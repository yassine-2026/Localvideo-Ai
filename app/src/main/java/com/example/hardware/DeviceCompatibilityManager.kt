package com.example.hardware

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File
import kotlin.math.roundToInt

data class CompatibilityReport(
    val isOverallCompatible: Boolean,
    val score: Int, // 0 to 100
    val androidVersion: String,
    val isAndroidVersionSupported: Boolean,
    val cpuAbi: String,
    val is64BitSupported: Boolean,
    val totalRamGb: Double,
    val availableRamGb: Double,
    val isRamSufficient: Boolean,
    val freeStorageGb: Double,
    val isStorageSufficient: Boolean,
    val hasHardwareH264Encoder: Boolean,
    val encoderName: String,
    val maxSupportedResolution: String,
    val issues: List<String>,
    val recommendations: List<String>
)

object DeviceCompatibilityManager {

    // Thresholds for on-device AI video generation
    private const val MIN_ANDROID_API = Build.VERSION_CODES.O // Android 8.0+
    private const val MIN_TOTAL_RAM_GB = 4.0 // Minimum 4GB RAM required for model load + frame pipeline
    private const val RECOMMENDED_RAM_GB = 8.0
    private const val MIN_FREE_STORAGE_GB = 3.0 // Model file (~1.5GB) + scratch frames + video outputs

    fun checkCompatibility(context: Context): CompatibilityReport {
        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        var score = 100

        // 1. Android Version Check
        val apiLevel = Build.VERSION.SDK_INT
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API $apiLevel)"
        val isAndroidSupported = apiLevel >= MIN_ANDROID_API
        if (!isAndroidSupported) {
            issues.add("Android version is too old. Requires API 26 (Android 8.0) or newer.")
            score -= 30
        }

        // 2. CPU Architecture Check (64-bit ARM required for modern AI tensor operators)
        val supportedAbis = Build.SUPPORTED_ABIS
        val primaryAbi = supportedAbis.firstOrNull() ?: "unknown"
        val is64Bit = supportedAbis.any { it.contains("arm64") || it.contains("x86_64") }
        if (!is64Bit) {
            issues.add("32-bit architecture detected ($primaryAbi). On-device neural network execution strictly requires 64-bit (arm64-v8a).")
            score -= 40
        }

        // 3. RAM Memory Check
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalRamBytes = memoryInfo.totalMem
        val availRamBytes = memoryInfo.availMem
        val totalRamGb = ((totalRamBytes / (1024.0 * 1024.0 * 1024.0)) * 10.0).roundToInt() / 10.0
        val availRamGb = ((availRamBytes / (1024.0 * 1024.0 * 1024.0)) * 10.0).roundToInt() / 10.0

        val isRamSufficient = totalRamGb >= MIN_TOTAL_RAM_GB
        if (!isRamSufficient) {
            issues.add("Total RAM ($totalRamGb GB) is below the minimum required ($MIN_TOTAL_RAM_GB GB). Model loading will trigger OutOfMemory.")
            score -= 35
        } else if (totalRamGb < RECOMMENDED_RAM_GB) {
            recommendations.add("Your device has $totalRamGb GB RAM. Recommended is $RECOMMENDED_RAM_GB GB+. Consider generating videos at 256x256 resolution.")
            score -= 10
        }

        if (memoryInfo.lowMemory) {
            issues.add("System reports low memory pressure. Close background applications before running generation.")
            score -= 15
        }

        // 4. Free Storage Check
        val storageDir = context.getExternalFilesDir(null) ?: context.filesDir
        val statFs = StatFs(storageDir.absolutePath)
        val freeStorageBytes = statFs.availableBlocksLong * statFs.blockSizeLong
        val freeStorageGb = ((freeStorageBytes / (1024.0 * 1024.0 * 1024.0)) * 10.0).roundToInt() / 10.0
        val isStorageSufficient = freeStorageGb >= MIN_FREE_STORAGE_GB

        if (!isStorageSufficient) {
            issues.add("Free storage ($freeStorageGb GB) is below minimum $MIN_FREE_STORAGE_GB GB required for model files and MP4 exports.")
            score -= 25
        }

        // 5. Hardware Video Encoder Check (MediaCodec H.264)
        var hasH264 = false
        var encoderName = "None"
        var maxResolution = "Unknown"

        try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (info in codecList.codecInfos) {
                if (info.isEncoder) {
                    val types = info.supportedTypes
                    if (types.any { it.equals("video/avc", ignoreCase = true) }) {
                        hasH264 = true
                        encoderName = info.name
                        try {
                            val caps = info.getCapabilitiesForType("video/avc")
                            val videoCaps = caps.videoCapabilities
                            if (videoCaps != null) {
                                maxResolution = "${videoCaps.supportedWidths.upper}x${videoCaps.supportedHeights.upper}"
                            }
                        } catch (_: Exception) {
                            maxResolution = "1920x1080"
                        }
                        break
                    }
                }
            }
        } catch (e: Exception) {
            hasH264 = false
        }

        if (!hasH264) {
            issues.add("No hardware-accelerated H.264 (video/avc) video encoder found on this device.")
            score -= 30
        }

        score = score.coerceIn(0, 100)
        val isOverallCompatible = isAndroidSupported && is64Bit && isRamSufficient && hasH264 && (score >= 50)

        return CompatibilityReport(
            isOverallCompatible = isOverallCompatible,
            score = score,
            androidVersion = androidVersion,
            isAndroidVersionSupported = isAndroidSupported,
            cpuAbi = primaryAbi,
            is64BitSupported = is64Bit,
            totalRamGb = totalRamGb,
            availableRamGb = availRamGb,
            isRamSufficient = isRamSufficient,
            freeStorageGb = freeStorageGb,
            isStorageSufficient = isStorageSufficient,
            hasHardwareH264Encoder = hasH264,
            encoderName = encoderName,
            maxSupportedResolution = maxResolution,
            issues = issues,
            recommendations = recommendations
        )
    }
}
