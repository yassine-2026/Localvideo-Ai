package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedVideoDao {
    @Query("SELECT * FROM generated_videos ORDER BY createdAtMillis DESC")
    fun getAllVideosFlow(): Flow<List<GeneratedVideoEntity>>

    @Query("SELECT * FROM generated_videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: Long): GeneratedVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: GeneratedVideoEntity): Long

    @Delete
    suspend fun deleteVideo(video: GeneratedVideoEntity)

    @Query("DELETE FROM generated_videos WHERE id = :id")
    suspend fun deleteVideoById(id: Long)
}

@Dao
interface ModelInfoDao {
    @Query("SELECT * FROM ai_models ORDER BY sizeBytes ASC")
    fun getAllModelsFlow(): Flow<List<ModelInfoEntity>>

    @Query("SELECT * FROM ai_models WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: String): ModelInfoEntity?

    @Query("SELECT * FROM ai_models WHERE status = 'READY' LIMIT 1")
    suspend fun getActiveReadyModel(): ModelInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelInfoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelInfoEntity)

    @Update
    suspend fun updateModel(model: ModelInfoEntity)

    @Query("UPDATE ai_models SET status = :status, downloadedBytes = :downloadedBytes, localFilePath = :localPath WHERE id = :id")
    suspend fun updateDownloadState(id: String, status: String, downloadedBytes: Long, localPath: String?)

    @Delete
    suspend fun deleteModel(model: ModelInfoEntity)
}
