package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.db.AppDatabase
import com.example.data.db.GeneratedVideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class VideoRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val videoDao = db.videoDao()

    val allVideos: Flow<List<GeneratedVideoEntity>> = videoDao.getAllVideosFlow()

    suspend fun saveGeneratedVideo(
        title: String,
        prompt: String,
        modelName: String,
        modelVersion: String,
        resolution: String,
        durationSeconds: Double,
        fps: Int,
        frameCount: Int,
        file: File
    ): GeneratedVideoEntity = withContext(Dispatchers.IO) {
        val entity = GeneratedVideoEntity(
            title = title,
            prompt = prompt,
            modelName = modelName,
            modelVersion = modelVersion,
            resolution = resolution,
            durationSeconds = durationSeconds,
            fps = fps,
            frameCount = frameCount,
            fileSizeBytes = file.length(),
            filePath = file.absolutePath,
            createdAtMillis = System.currentTimeMillis(),
            isVerifiedPlayable = true
        )
        val id = videoDao.insertVideo(entity)
        entity.copy(id = id)
    }

    suspend fun deleteVideo(video: GeneratedVideoEntity) = withContext(Dispatchers.IO) {
        val file = File(video.filePath)
        if (file.exists()) {
            file.delete()
        }
        videoDao.deleteVideo(video)
    }

    /**
     * Exports video into the device's shared Movies directory using MediaStore on Android 10+
     * or standard external storage.
     */
    suspend fun exportVideoToGallery(video: GeneratedVideoEntity): Result<Uri> = withContext(Dispatchers.IO) {
        val sourceFile = File(video.filePath)
        if (!sourceFile.exists()) {
            return@withContext Result.failure(IllegalStateException("Source video file not found."))
        }

        try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "LocalVideo_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/LocalVideoAI")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = contentResolver.insert(collection, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            contentResolver.openOutputStream(itemUri).use { out ->
                if (out == null) throw Exception("Output stream is null")
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                contentResolver.update(itemUri, contentValues, null, null)
            }

            Result.success(itemUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates an Intent to share the video with external apps using FileProvider.
     */
    fun createShareIntent(video: GeneratedVideoEntity): Intent {
        val file = File(video.filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Generated with LocalVideo AI:\n\"${video.prompt}\"")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
