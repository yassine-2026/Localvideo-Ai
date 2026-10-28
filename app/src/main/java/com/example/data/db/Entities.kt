package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_videos")
data class GeneratedVideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val prompt: String,
    val modelName: String,
    val modelVersion: String,
    val resolution: String,
    val durationSeconds: Double,
    val fps: Int,
    val frameCount: Int,
    val fileSizeBytes: Long,
    val filePath: String,
    val createdAtMillis: Long,
    val isVerifiedPlayable: Boolean = true
)

@Entity(tableName = "ai_models")
data class ModelInfoEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val version: String,
    val provider: String,
    val license: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val minRamGb: Double,
    val architecture: String,
    val expectedSha256: String,
    val status: String, // "NOT_DOWNLOADED", "DOWNLOADING", "READY", "EXCEEDS_LIMIT", "PENDING_VERIFICATION"
    val localFilePath: String? = null,
    val downloadedBytes: Long = 0,
    val verificationNotes: String = "",
    val isCustomImported: Boolean = false
)
