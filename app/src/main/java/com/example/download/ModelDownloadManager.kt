package com.example.download

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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

    suspend fun startDownload(
        modelId: String,
        urlString: String,
        fileName: String,
        expectedSizeBytes: Long,
        expectedSha256: String
    ): Result<File> = withContext(Dispatchers.IO) {
        isCancelled = false
        isPausedRequested = false

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

            if (existingBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$existingBytes-")
            }

            connection.connect()
            val responseCode = connection.responseCode

            val totalLength = if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                existingBytes + connection.contentLengthLong
            } else if (responseCode == HttpURLConnection.HTTP_OK) {
                existingBytes = 0L
                connection.contentLengthLong
            } else {
                return@withContext Result.failure(Exception("HTTP Error: $responseCode ${connection.responseMessage}"))
            }

            val finalTotalBytes = if (totalLength > 0) totalLength else expectedSizeBytes
            var downloaded = existingBytes

            val inputStream: InputStream = connection.inputStream
            val output = RandomAccessFile(partFile, "rw")
            output.seek(existingBytes)

            val buffer = ByteArray(64 * 1024) // 64KB buffer
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
                    return@withContext Result.failure(Exception("Download cancelled by user"))
                }

                if (isPausedRequested) {
                    output.close()
                    inputStream.close()
                    connection.disconnect()
                    _downloadState.value = _downloadState.value.copy(
                        isDownloading = false,
                        isPaused = true
                    )
                    return@withContext Result.failure(Exception("Download paused"))
                }

                output.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                bytesSinceLastTime += bytesRead

                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - lastTime
                if (elapsed >= 500) { // Update stats every 500ms
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

            // Rename .part to final targetFile
            if (targetFile.exists()) targetFile.delete()
            if (!partFile.renameTo(targetFile)) {
                return@withContext Result.failure(Exception("Failed to finalize downloaded model file"))
            }

            // Verify integrity
            if (expectedSha256.isNotBlank()) {
                val matches = FileIntegrityVerifier.verifyIntegrity(targetFile, expectedSha256)
                if (!matches) {
                    targetFile.delete()
                    _downloadState.value = DownloadProgress(errorMessage = "SHA-256 verification failed. File corrupted.")
                    return@withContext Result.failure(Exception("SHA-256 integrity verification mismatch."))
                }
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
            _downloadState.value = DownloadProgress(errorMessage = e.message ?: "Network error during download")
            Result.failure(e)
        }
    }

    fun pauseDownload() {
        isPausedRequested = true
    }

    fun cancelDownload() {
        isCancelled = true
    }
}
