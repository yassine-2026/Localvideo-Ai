package com.example.download

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

data class DownloadProgress(
    val modelId: String = "",
    val isDownloading: Boolean = false,
    val isPaused: Boolean = false,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val percentage: Int = 0,
    val speedKbPerSec: Double = 0.0,
    val etaSeconds: Long = 0,
    val errorMessage: String? = null
)

class ModelDownloadManager(private val context: Context) {

    private val _downloadState = MutableStateFlow(DownloadProgress())
    val downloadState: StateFlow<DownloadProgress> = _downloadState.asStateFlow()

    @Volatile
    private var isCancelled = false

    @Volatile
    private var isPausedRequested = false

    fun getModelsDir(): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Checks if there is sufficient disk space for the expected download plus 200MB buffer.
     */
    fun checkStorageAvailable(requiredBytes: Long): Boolean {
        return try {
            val stat = StatFs(getModelsDir().absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes >= (requiredBytes + 200 * 1024 * 1024L)
        } catch (_: Exception) {
            true
        }
    }

    suspend fun startDownload(
        modelId: String,
        urlString: String,
        fileName: String,
        expectedSizeBytes: Long,
        expectedSha256: String
    ): Result<File> = withContext(Dispatchers.IO) {
        isCancelled = false
        isPausedRequested = false

        if (urlString.isBlank() || !urlString.startsWith("http")) {
            val errorMsg = "Invalid or empty model download URL."
            _downloadState.value = DownloadProgress(modelId = modelId, errorMessage = errorMsg)
            return@withContext Result.failure(IllegalArgumentException(errorMsg))
        }

        // Check storage before starting
        if (expectedSizeBytes > 0 && !checkStorageAvailable(expectedSizeBytes)) {
            val errorMsg = "Insufficient storage space on device for this model (${expectedSizeBytes / (1024 * 1024)} MB required)."
            _downloadState.value = DownloadProgress(modelId = modelId, errorMessage = errorMsg)
            return@withContext Result.failure(IllegalStateException(errorMsg))
        }

        val targetFile = File(getModelsDir(), fileName)
        val partFile = File(getModelsDir(), "$fileName.part")

        var existingBytes = if (partFile.exists()) partFile.length() else 0L

        _downloadState.value = DownloadProgress(
            modelId = modelId,
            isDownloading = true,
            bytesDownloaded = existingBytes,
            totalBytes = expectedSizeBytes
        )

        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "LocalVideoAI-Android/1.0")

            if (existingBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$existingBytes-")
            }

            connection.connect()
            val responseCode = connection.responseCode

            // Handle HTTP errors cleanly
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                _downloadState.value = DownloadProgress(modelId = modelId, errorMessage = "HTTP Error 404: The model weights file was not found on remote server.")
                return@withContext Result.failure(Exception("HTTP Error 404: Remote file not found at $urlString"))
            }

            if (responseCode == HttpURLConnection.HTTP_FORBIDDEN || responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                _downloadState.value = DownloadProgress(modelId = modelId, errorMessage = "HTTP Error $responseCode: Access denied. Repository may require authentication or license agreement.")
                return@withContext Result.failure(Exception("HTTP Error $responseCode: Access forbidden for $urlString"))
            }

            if (responseCode !in 200..299) {
                _downloadState.value = DownloadProgress(modelId = modelId, errorMessage = "HTTP Error $responseCode: ${connection.responseMessage}")
                return@withContext Result.failure(Exception("HTTP Error: $responseCode ${connection.responseMessage}"))
            }

            // Verify Content-Type: Reject HTML error/login pages masquerading as weights
            val contentType = connection.contentType ?: ""
            if (contentType.contains("text/html", ignoreCase = true) || contentType.contains("application/xhtml", ignoreCase = true)) {
                _downloadState.value = DownloadProgress(
                    modelId = modelId,
                    errorMessage = "Server returned an HTML webpage instead of binary model weights (URL may be a repository viewer rather than direct raw download)."
                )
                return@withContext Result.failure(IllegalStateException("Server returned HTML page instead of binary weights."))
            }

            // Handle Range support: If server returned 200 OK instead of 206 Partial Content, reset stream to 0
            val totalLength = if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                existingBytes + connection.contentLengthLong
            } else {
                existingBytes = 0L // Server ignored Range header, restart download from beginning
                connection.contentLengthLong
            }

            val finalTotalBytes = if (totalLength > 0) totalLength else expectedSizeBytes
            var downloaded = existingBytes

            val inputStream: InputStream = connection.inputStream
            val output = RandomAccessFile(partFile, "rw")
            if (existingBytes == 0L) {
                output.setLength(0) // Truncate file if restarting from beginning
            }
            output.seek(existingBytes)

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var lastTime = System.currentTimeMillis()
            var bytesSinceLastTime = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isCancelled) {
                    output.close()
                    inputStream.close()
                    connection.disconnect()
                    partFile.delete()
                    _downloadState.value = DownloadProgress()
                    return@withContext Result.failure(Exception("Download cancelled by user."))
                }

                if (isPausedRequested) {
                    output.close()
                    inputStream.close()
                    connection.disconnect()
                    _downloadState.value = _downloadState.value.copy(
                        isDownloading = false,
                        isPaused = true
                    )
                    return@withContext Result.failure(Exception("Download paused."))
                }

                output.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                bytesSinceLastTime += bytesRead

                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - lastTime
                if (elapsed >= 500) {
                    val speed = (bytesSinceLastTime / 1024.0) / (elapsed / 1000.0)
                    val remainingBytes = (finalTotalBytes - downloaded).coerceAtLeast(0)
                    val eta = if (speed > 0) (remainingBytes / (speed * 1024)).toLong() else 0L
                    val percent = if (finalTotalBytes > 0) ((downloaded * 100) / finalTotalBytes).toInt().coerceIn(0, 100) else 0

                    _downloadState.value = DownloadProgress(
                        modelId = modelId,
                        isDownloading = true,
                        isPaused = false,
                        bytesDownloaded = downloaded,
                        totalBytes = finalTotalBytes,
                        percentage = percent,
                        speedKbPerSec = speed,
                        etaSeconds = eta
                    )

                    lastTime = currentTime
                    bytesSinceLastTime = 0L
                }
            }

            output.close()
            inputStream.close()
            connection.disconnect()

            // Verify downloaded file size
            if (partFile.length() < 1024) {
                partFile.delete()
                _downloadState.value = DownloadProgress(modelId = modelId, errorMessage = "Downloaded file is incomplete or corrupted (< 1 KB).")
                return@withContext Result.failure(IllegalStateException("Downloaded file is incomplete."))
            }

            // Verify cryptographic SHA-256 integrity if specified
            if (expectedSha256.isNotBlank()) {
                val matches = FileIntegrityVerifier.verifyIntegrity(partFile, expectedSha256)
                if (!matches) {
                    partFile.delete()
                    _downloadState.value = DownloadProgress(modelId = modelId, errorMessage = "SHA-256 checksum mismatch. Downloaded file is corrupted or modified.")
                    return@withContext Result.failure(Exception("SHA-256 integrity verification failed."))
                }
            }

            // Finalize target file
            if (targetFile.exists()) targetFile.delete()
            if (!partFile.renameTo(targetFile)) {
                _downloadState.value = DownloadProgress(modelId = modelId, errorMessage = "Failed to finalize downloaded model file on disk.")
                return@withContext Result.failure(Exception("Failed to finalize downloaded model file."))
            }

            _downloadState.value = DownloadProgress(
                modelId = modelId,
                isDownloading = false,
                bytesDownloaded = targetFile.length(),
                totalBytes = targetFile.length(),
                percentage = 100
            )

            Result.success(targetFile)
        } catch (e: Exception) {
            _downloadState.value = DownloadProgress(modelId = modelId, errorMessage = e.message ?: "Network error during download.")
            Result.failure(e)
        }
    }

    fun pauseDownload() {
        isPausedRequested = true
    }

    fun cancelDownload() {
        isCancelled = true
    }

    fun clearError() {
        _downloadState.value = _downloadState.value.copy(errorMessage = null)
    }
}
