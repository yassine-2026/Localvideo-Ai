package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.GeneratedVideoEntity
import com.example.download.ModelDownloadManager
import com.example.engine.DefaultLocalInferenceEngine
import com.example.engine.InferenceConfig
import com.example.engine.InferenceState
import com.example.hardware.DeviceCompatibilityManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAppNameString() {
        val appName = context.getString(R.string.app_name)
        assertEquals("LocalVideo AI", appName)
    }

    @Test
    fun testDeviceCompatibilityCheck() {
        val report = DeviceCompatibilityManager.checkCompatibility(context)
        assertNotNull(report)
        assertTrue(report.androidVersion.isNotBlank())
        assertTrue(report.totalRamGb >= 0.0)
        assertTrue(report.freeStorageGb >= 0.0)
    }

    @Test
    fun testRoomDatabase_insertAndRetrieveVideo() = runBlocking {
        val dao = db.videoDao()
        val video = GeneratedVideoEntity(
            title = "Cinematic Waves",
            prompt = "Dramatic cinematic waves crashing against black basalt cliffs",
            modelName = "Wan 2.1",
            modelVersion = "v2.1",
            resolution = "256x256",
            durationSeconds = 2.0,
            fps = 12,
            frameCount = 24,
            fileSizeBytes = 409600L,
            filePath = "/data/user/0/com.example/files/videos/test.mp4",
            createdAtMillis = System.currentTimeMillis(),
            isVerifiedPlayable = true
        )

        val id = dao.insertVideo(video)
        val retrieved = dao.getVideoById(id)

        assertNotNull(retrieved)
        assertEquals("Cinematic Waves", retrieved?.title)
        assertEquals(24, retrieved?.frameCount)

        val allVideos = dao.getAllVideosFlow().first()
        assertEquals(1, allVideos.size)

        // Delete video
        dao.deleteVideoById(id)
        val afterDelete = dao.getVideoById(id)
        assertEquals(null, afterDelete)
    }

    @Test
    fun testLocalInferenceEngine_preventsExecutionWithoutModel() = runBlocking {
        val engine = DefaultLocalInferenceEngine()
        assertFalse("Engine must not be ready before loading verified model", engine.isModelReady())

        val states = engine.executeInference(
            context = context,
            config = InferenceConfig(prompt = "Dramatic cinematic sunset")
        ).toList()

        assertTrue(states.isNotEmpty())
        val firstState = states.first()
        assertTrue("Must emit Error state when model is not initialized", firstState is InferenceState.Error)
    }

    @Test
    fun testModelDownloadManager_handlesInvalidUrl() = runBlocking {
        val downloadManager = ModelDownloadManager(context)
        val result = downloadManager.startDownload(
            modelId = "invalid_test",
            urlString = "https://invalid.domain.example/non_existent.bin",
            fileName = "invalid.bin",
            expectedSizeBytes = 1000L,
            expectedSha256 = ""
        )

        assertTrue("Invalid URL must return failure result", result.isFailure)
        assertNotNull(downloadManager.downloadState.value.errorMessage)
    }

    @Test
    fun testZeroScopeModelManager_initialStateAndHonestActivation() {
        val manager = com.example.data.ZeroScopeModelManager(context)
        val state = manager.state.value

        assertEquals("UNet — diffusion_pytorch_model.safetensors", state.unetSlot.title)
        assertEquals("Text Encoder — model.safetensors", state.textEncoderSlot.title)
        assertEquals("VAE — diffusion_pytorch_model.safetensors", state.vaeSlot.title)

        assertEquals(com.example.data.ZeroScopeModelManager.ESTIMATED_TOTAL_BYTES, state.estimatedTotalSizeBytes)
        assertEquals(com.example.data.ZeroScopeModelManager.STRICT_LIMIT_BYTES, state.maxPermittedBytes)

        // Activation attempt before files are selected must be rejected
        val activationResult = manager.activateZeroScope()
        assertFalse("Activation must fail when slots are empty", activationResult.isActivated)
        assertTrue(activationResult.messageAr.contains("المكونات الثلاثة"))
    }
}
