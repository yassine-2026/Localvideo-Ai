package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.GeneratedVideoEntity
import com.example.hardware.DeviceCompatibilityManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
            modelName = "MicroVideo-T2V",
            modelVersion = "v0.9.4",
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
    }
}
