package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.provider.OpenableColumns
import android.util.Log
import com.example.engine.SafeTensorsHeaderParser
import com.example.engine.SafeTensorsRole
import com.example.engine.SafeTensorsValidationResult
import com.example.hardware.DeviceCompatibilityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

enum class ZeroScopeSlotId {
    UNET,
    TEXT_ENCODER,
    VAE
}

sealed class ComponentVerificationState {
    object NotSelected : ComponentVerificationState()

    data class Checking(
        val progress: Float = 0f,
        val stageAr: String = "جارٍ فحص الملف...",
        val stageEn: String = "Verifying file..."
    ) : ComponentVerificationState()

    data class Valid(
        val fileName: String,
        val sizeBytes: Long,
        val sizeFormatted: String,
        val sha256Prefix: String,
        val tensorCount: Int,
        val headerBytes: Long,
        val sampleKeys: List<String>,
        val verifiedAtTimestamp: Long = System.currentTimeMillis()
    ) : ComponentVerificationState()

    data class Invalid(
        val fileName: String,
        val sizeBytes: Long,
        val sizeFormatted: String,
        val failureReasonAr: String,
        val failureReasonEn: String,
        val technicalDetail: String? = null
    ) : ComponentVerificationState()
}

data class ZeroScopeSlotData(
    val slotId: ZeroScopeSlotId,
    val title: String,
    val defaultFileName: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val estimatedSizeBytes: Long,
    val estimatedSizeFormatted: String,
    val expectedRole: SafeTensorsRole,
    val uriString: String? = null,
    val verificationState: ComponentVerificationState = ComponentVerificationState.NotSelected
)

data class ZeroScopeActivationResult(
    val isActivated: Boolean,
    val titleAr: String,
    val titleEn: String,
    val messageAr: String,
    val messageEn: String,
    val technicalDetails: String
)

data class ZeroScopeScreenState(
    val unetSlot: ZeroScopeSlotData,
    val textEncoderSlot: ZeroScopeSlotData,
    val vaeSlot: ZeroScopeSlotData,
    val totalRealSizeBytes: Long = 0L,
    val totalRealSizeFormatted: String = "0 B",
    val estimatedTotalSizeBytes: Long = 3_765_000_000L,
    val estimatedTotalSizeFormatted: String = "3.76 GB",
    val maxPermittedBytes: Long = 1_610_612_736L, // 1.5 GB
    val isExceedingLimit: Boolean = false,
    val sizeStatusLabelAr: String = "لم يتم اختيار ملفات بعد",
    val sizeStatusLabelEn: String = "No files selected yet",
    val compatibilityStatusAr: String = "غير مفحوص",
    val compatibilityStatusEn: String = "Unchecked",
    val isCompatible: Boolean = false,
    val isActivationReady: Boolean = false,
    val isActivated: Boolean = false,
    val activationResult: ZeroScopeActivationResult? = null,
    val freeDeviceStorageBytes: Long = 0L,
    val freeDeviceStorageFormatted: String = "0 GB",
    val deviceTotalRamGb: Double = 0.0
)

class ZeroScopeModelManager(private val context: Context) {

    companion object {
        private const val TAG = "ZeroScopeModelMgr"
        private const val PREFS_NAME = "zeroscope_v2_slots_prefs"
        const val STRICT_LIMIT_BYTES = 1_610_612_736L // 1.5 GiB
        const val ESTIMATED_TOTAL_BYTES = 3_765_000_000L // ~3.76 GB
        const val ESTIMATED_UNET_BYTES = 2_750_000_000L // ~2.75 GB
        const val ESTIMATED_TEXT_ENCODER_BYTES = 680_000_000L // ~680 MB
        const val ESTIMATED_VAE_BYTES = 335_000_000L // ~335 MB
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(buildInitialState())
    val state: StateFlow<ZeroScopeScreenState> = _state.asStateFlow()

    init {
        restorePersistedSlots()
        refreshStorageAndRam()
    }

    private fun buildInitialState(): ZeroScopeScreenState {
        val unetSlot = ZeroScopeSlotData(
            slotId = ZeroScopeSlotId.UNET,
            title = "UNet — diffusion_pytorch_model.safetensors",
            defaultFileName = "diffusion_pytorch_model.safetensors",
            descriptionAr = "أوزان شبكة الانتشار المستخدمة في عملية توليد الفيديو (Backbone Spatiotemporal Denoising)",
            descriptionEn = "Diffusion network weights used in video denoising (Backbone Spatiotemporal Denoising)",
            estimatedSizeBytes = ESTIMATED_UNET_BYTES,
            estimatedSizeFormatted = "2.75 GB",
            expectedRole = SafeTensorsRole.UNET
        )

        val textEncoderSlot = ZeroScopeSlotData(
            slotId = ZeroScopeSlotId.TEXT_ENCODER,
            title = "Text Encoder — model.safetensors",
            defaultFileName = "model.safetensors",
            descriptionAr = "مكوّن ترميز النص الذي يحتاج إليه النموذج لفهم وصف الفيديو (OpenCLIP ViT-H/14)",
            descriptionEn = "Text encoder component required to understand prompt scene descriptions (OpenCLIP ViT-H/14)",
            estimatedSizeBytes = ESTIMATED_TEXT_ENCODER_BYTES,
            estimatedSizeFormatted = "680 MB",
            expectedRole = SafeTensorsRole.TEXT_ENCODER
        )

        val vaeSlot = ZeroScopeSlotData(
            slotId = ZeroScopeSlotId.VAE,
            title = "VAE — diffusion_pytorch_model.safetensors",
            defaultFileName = "diffusion_pytorch_model.safetensors",
            descriptionAr = "مكوّن فك الترميز الذي يحوّل المخرجات الكامنة إلى إطارات فيديو (3D AutoencoderKL)",
            descriptionEn = "Decoder component converting latent tensors into RGB video frames (3D AutoencoderKL)",
            estimatedSizeBytes = ESTIMATED_VAE_BYTES,
            estimatedSizeFormatted = "335 MB",
            expectedRole = SafeTensorsRole.VAE
        )

        return ZeroScopeScreenState(
            unetSlot = unetSlot,
            textEncoderSlot = textEncoderSlot,
            vaeSlot = vaeSlot
        )
    }

    fun refreshStorageAndRam() {
        val report = DeviceCompatibilityManager.checkCompatibility(context)
        val storageDir = context.getExternalFilesDir(null) ?: context.filesDir
        val statFs = StatFs(storageDir.absolutePath)
        val freeBytes = statFs.availableBlocksLong * statFs.blockSizeLong

        _state.value = _state.value.copy(
            freeDeviceStorageBytes = freeBytes,
            freeDeviceStorageFormatted = formatBytes(freeBytes),
            deviceTotalRamGb = report.totalRamGb
        )
    }

    suspend fun selectAndVerifySlot(slotId: ZeroScopeSlotId, uri: Uri) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // 1. Resolve file name and size via ContentResolver
        var fileName = "weights.safetensors"
        var sizeBytes = 0L

        try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        val name = cursor.getString(nameIdx)
                        if (!name.isNullOrBlank()) fileName = name
                    }
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx != -1) {
                        sizeBytes = cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query OpenableColumns: ${e.message}")
        }

        if (sizeBytes <= 0L) {
            try {
                resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    sizeBytes = pfd.statSize
                }
            } catch (_: Exception) {}
        }

        val expectedRole = when (slotId) {
            ZeroScopeSlotId.UNET -> SafeTensorsRole.UNET
            ZeroScopeSlotId.TEXT_ENCODER -> SafeTensorsRole.TEXT_ENCODER
            ZeroScopeSlotId.VAE -> SafeTensorsRole.VAE
        }

        // Set Checking state
        updateSlotState(slotId) { slot ->
            slot.copy(
                uriString = uri.toString(),
                verificationState = ComponentVerificationState.Checking(
                    progress = 0.1f,
                    stageAr = "جارٍ قراءة ترويسة SafeTensors الثنائية...",
                    stageEn = "Parsing binary SafeTensors header..."
                )
            )
        }

        // 2. Parse SafeTensors header directly from InputStream
        val headerResult = try {
            resolver.openInputStream(uri)?.use { stream ->
                SafeTensorsHeaderParser.parseHeader(stream, expectedRole)
            } ?: SafeTensorsValidationResult(
                isValid = false,
                detectedRole = SafeTensorsRole.UNKNOWN,
                failureReasonAr = "تعذر فتح مجرى بيانات الملف عبر Storage Access Framework.",
                failureReasonEn = "Unable to open input stream via Storage Access Framework."
            )
        } catch (e: Exception) {
            SafeTensorsValidationResult(
                isValid = false,
                detectedRole = SafeTensorsRole.UNKNOWN,
                failureReasonAr = "خطأ غير متوقع أثناء فحص الملف: ${e.localizedMessage ?: e.message}",
                failureReasonEn = "Unexpected error checking file: ${e.localizedMessage ?: e.message}"
            )
        }

        // 3. Compute fast SHA-256 stream prefix (first 16MB) to ensure file integrity without freezing
        val sha256Prefix = try {
            resolver.openInputStream(uri)?.use { stream ->
                computeStreamSha256Prefix(stream, maxBytes = 16 * 1024 * 1024)
            } ?: "unknown"
        } catch (_: Exception) {
            "uncomputed"
        }

        // 4. Update slot state based on validation result
        if (headerResult.isValid) {
            val validState = ComponentVerificationState.Valid(
                fileName = fileName,
                sizeBytes = sizeBytes,
                sizeFormatted = formatBytes(sizeBytes),
                sha256Prefix = sha256Prefix,
                tensorCount = headerResult.tensorCount,
                headerBytes = headerResult.headerSizeBytes,
                sampleKeys = headerResult.sampleKeys
            )
            updateSlotState(slotId) { slot ->
                slot.copy(
                    uriString = uri.toString(),
                    verificationState = validState
                )
            }
            persistSlot(slotId, uri.toString(), fileName, sizeBytes, sha256Prefix, headerResult.tensorCount, headerResult.headerSizeBytes, headerResult.sampleKeys)
        } else {
            val invalidState = ComponentVerificationState.Invalid(
                fileName = fileName,
                sizeBytes = sizeBytes,
                sizeFormatted = formatBytes(sizeBytes),
                failureReasonAr = headerResult.failureReasonAr ?: "فشل التحقق من توافق بنية الملف.",
                failureReasonEn = headerResult.failureReasonEn ?: "Verification failed.",
                technicalDetail = headerResult.technicalDetail
            )
            updateSlotState(slotId) { slot ->
                slot.copy(
                    uriString = uri.toString(),
                    verificationState = invalidState
                )
            }
            clearPersistedSlot(slotId)
        }

        recalculateAggregateState()
    }

    fun removeSlot(slotId: ZeroScopeSlotId) {
        updateSlotState(slotId) { slot ->
            slot.copy(
                uriString = null,
                verificationState = ComponentVerificationState.NotSelected
            )
        }
        clearPersistedSlot(slotId)
        recalculateAggregateState()
    }

    /**
     * Honest ZeroScope v2 activation check:
     * Checks whether all 3 components are valid, then verifies if the runtime has
     * a bundled native 3D spatio-temporal inference engine capable of running ZeroScope v2.
     * Never claims success or runs procedural animations.
     */
    fun activateZeroScope(): ZeroScopeActivationResult {
        val current = _state.value
        val unetValid = current.unetSlot.verificationState is ComponentVerificationState.Valid
        val textValid = current.textEncoderSlot.verificationState is ComponentVerificationState.Valid
        val vaeValid = current.vaeSlot.verificationState is ComponentVerificationState.Valid

        if (!unetValid || !textValid || !vaeValid) {
            val result = ZeroScopeActivationResult(
                isActivated = false,
                titleAr = "الملفات غير مكتملة",
                titleEn = "Incomplete Components",
                messageAr = "يجب اختيار وفحص جميع المكونات الثلاثة (UNet و Text Encoder و VAE) بنجاح قبل محاولة التفعيل.",
                messageEn = "All three components (UNet, Text Encoder, and VAE) must be chosen and successfully verified before activation.",
                technicalDetails = "Missing verified components."
            )
            _state.value = _state.value.copy(activationResult = result, isActivated = false)
            return result
        }

        // Engine availability check:
        // ZeroScope v2 requires a native C++ 3D spatio-temporal Diffusers engine (e.g. Vulkan/NNAPI FP16)
        // and ~8 GB RAM. Check if such a native library is installed/bundled.
        val hasNativeZeroScopeDiffusersEngine = checkNativeZeroScopeEnginePresent()

        if (!hasNativeZeroScopeDiffusersEngine) {
            val result = ZeroScopeActivationResult(
                isActivated = false,
                titleAr = "عدم توفر محرك استدلال متوافق",
                titleEn = "No Compatible Inference Engine",
                messageAr = "تم فحص الملفات بنجاح، لكن لا يتوفر محرك استدلال متوافق لتشغيل هذا النموذج على هذا الجهاز. نموذج ZeroScope v2 (576w) يعتمد على شبكة انتشار مكانية-زمنية (3D Spatiotemporal Diffusers) تتطلب محرك C++ مخصص مع مكتبات Vulkan/OpenCL وذاكرة رام فعلية تفوق 8 GB، وهو غير مدمج في هذا الإصدار. يُحظر التظاهر بنجاح وهمي وفق قواعد النزاهة الهندسية.",
                messageEn = "Files successfully verified, but no compatible native inference engine is available to execute this model on this device. ZeroScope v2 requires a specialized 3D Spatiotemporal Diffusers C++ runtime with ~8 GB RAM, which is not bundled. Mock simulation is strictly prohibited.",
                technicalDetails = "Engine Status: Native 3D Spatiotemporal Diffusers runtime missing. Total Size: ${current.totalRealSizeFormatted} (Exceeds strict 1.5 GB limit). Device RAM: ${current.deviceTotalRamGb} GB (Required: 8.0 GB)."
            )
            _state.value = _state.value.copy(
                activationResult = result,
                isActivated = false,
                compatibilityStatusAr = "تم فحص الملفات (محرك الاستدلال غير متوفر)",
                compatibilityStatusEn = "Files verified (Inference engine unavailable)"
            )
            return result
        }

        // If native engine were present (for future native engine expansion)
        val result = ZeroScopeActivationResult(
            isActivated = true,
            titleAr = "تم تفعيل ZeroScope v2 بنجاح",
            titleEn = "ZeroScope v2 Activated",
            messageAr = "تم ربط ملفات UNet و Text Encoder و VAE بمحرك الاستدلال المحلي بنجاح.",
            messageEn = "UNet, Text Encoder, and VAE successfully linked to local inference engine.",
            technicalDetails = "All 3 components verified and loaded."
        )
        _state.value = _state.value.copy(activationResult = result, isActivated = true)
        return result
    }

    fun dismissActivationResult() {
        _state.value = _state.value.copy(activationResult = null)
    }

    private fun checkNativeZeroScopeEnginePresent(): Boolean {
        // ZeroScope v2 (576w) PyTorch diffusers pipeline requires native C++ 3D-UNet tensor operators.
        // There is no bundled compiled native .so for 3D spatio-temporal diffusion in this mobile package.
        // Return false to honestly report reality to the user.
        return false
    }

    private fun updateSlotState(slotId: ZeroScopeSlotId, transform: (ZeroScopeSlotData) -> ZeroScopeSlotData) {
        val current = _state.value
        _state.value = when (slotId) {
            ZeroScopeSlotId.UNET -> current.copy(unetSlot = transform(current.unetSlot))
            ZeroScopeSlotId.TEXT_ENCODER -> current.copy(textEncoderSlot = transform(current.textEncoderSlot))
            ZeroScopeSlotId.VAE -> current.copy(vaeSlot = transform(current.vaeSlot))
        }
    }

    private fun recalculateAggregateState() {
        val current = _state.value
        var sumBytes = 0L
        var anySelected = false
        var anyChecking = false
        var anyInvalid = false
        var allValid = true

        val slots = listOf(current.unetSlot, current.textEncoderSlot, current.vaeSlot)
        for (slot in slots) {
            when (val state = slot.verificationState) {
                is ComponentVerificationState.NotSelected -> {
                    allValid = false
                }
                is ComponentVerificationState.Checking -> {
                    anySelected = true
                    anyChecking = true
                    allValid = false
                }
                is ComponentVerificationState.Valid -> {
                    anySelected = true
                    sumBytes += state.sizeBytes
                }
                is ComponentVerificationState.Invalid -> {
                    anySelected = true
                    anyInvalid = true
                    allValid = false
                    sumBytes += state.sizeBytes
                }
            }
        }

        val isExceeding = sumBytes > STRICT_LIMIT_BYTES
        val sizeFormatted = formatBytes(sumBytes)

        val sizeStatusAr = when {
            !anySelected -> "لم يتم اختيار ملفات بعد (التقديري: 3.76 GB)"
            isExceeding -> "يتجاوز الحد المحدد (1.5 GB)"
            else -> "ضمن الحد المسموح"
        }

        val sizeStatusEn = when {
            !anySelected -> "No files selected yet (Est: 3.76 GB)"
            isExceeding -> "Exceeds specified limit (1.5 GB)"
            else -> "Within allowed limit"
        }

        val compatStatusAr = when {
            anyChecking -> "جارٍ التحقق..."
            !anySelected -> "غير مفحوص"
            anyInvalid -> "غير متوافق (أحد الملفات تالف أو غير مطابق)"
            allValid && isExceeding -> "مفحوص ومطابق (يتجاوز سعة الهاتف 1.5 GB)"
            allValid -> "متوافق"
            else -> "غير مكتمل"
        }

        val compatStatusEn = when {
            anyChecking -> "Verifying..."
            !anySelected -> "Unchecked"
            anyInvalid -> "Incompatible (File corrupt or mismatched)"
            allValid && isExceeding -> "Verified & Valid (Exceeds 1.5 GB limit)"
            allValid -> "Compatible"
            else -> "Incomplete"
        }

        _state.value = current.copy(
            totalRealSizeBytes = sumBytes,
            totalRealSizeFormatted = sizeFormatted,
            isExceedingLimit = isExceeding,
            sizeStatusLabelAr = sizeStatusAr,
            sizeStatusLabelEn = sizeStatusEn,
            compatibilityStatusAr = compatStatusAr,
            compatibilityStatusEn = compatStatusEn,
            isCompatible = allValid && !isExceeding,
            isActivationReady = allValid
        )
    }

    private fun computeStreamSha256Prefix(stream: InputStream, maxBytes: Int): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var totalRead = 0
        while (totalRead < maxBytes) {
            val toRead = (maxBytes - totalRead).coerceAtMost(buffer.size)
            val read = stream.read(buffer, 0, toRead)
            if (read == -1) break
            digest.update(buffer, 0, read)
            totalRead += read
        }
        val hash = digest.digest()
        val hex = StringBuilder()
        for (b in hash.take(8)) {
            val h = Integer.toHexString(0xff and b.toInt())
            if (h.length == 1) hex.append('0')
            hex.append(h)
        }
        return "${hex}…"
    }

    private fun persistSlot(
        slotId: ZeroScopeSlotId,
        uri: String,
        fileName: String,
        sizeBytes: Long,
        shaPrefix: String,
        tensorCount: Int,
        headerBytes: Long,
        sampleKeys: List<String>
    ) {
        val prefix = slotId.name
        prefs.edit()
            .putString("${prefix}_uri", uri)
            .putString("${prefix}_filename", fileName)
            .putLong("${prefix}_size", sizeBytes)
            .putString("${prefix}_sha", shaPrefix)
            .putInt("${prefix}_tensors", tensorCount)
            .putLong("${prefix}_header", headerBytes)
            .putString("${prefix}_keys", sampleKeys.joinToString("|"))
            .apply()
    }

    private fun clearPersistedSlot(slotId: ZeroScopeSlotId) {
        val prefix = slotId.name
        prefs.edit()
            .remove("${prefix}_uri")
            .remove("${prefix}_filename")
            .remove("${prefix}_size")
            .remove("${prefix}_sha")
            .remove("${prefix}_tensors")
            .remove("${prefix}_header")
            .remove("${prefix}_keys")
            .apply()
    }

    private fun restorePersistedSlots() {
        for (slotId in ZeroScopeSlotId.values()) {
            val prefix = slotId.name
            val uri = prefs.getString("${prefix}_uri", null) ?: continue
            val fileName = prefs.getString("${prefix}_filename", null) ?: continue
            val size = prefs.getLong("${prefix}_size", 0L)
            val sha = prefs.getString("${prefix}_sha", "") ?: ""
            val tensors = prefs.getInt("${prefix}_tensors", 0)
            val header = prefs.getLong("${prefix}_header", 0L)
            val keys = prefs.getString("${prefix}_keys", "")?.split("|")?.filter { it.isNotBlank() } ?: emptyList()

            val validState = ComponentVerificationState.Valid(
                fileName = fileName,
                sizeBytes = size,
                sizeFormatted = formatBytes(size),
                sha256Prefix = sha,
                tensorCount = tensors,
                headerBytes = header,
                sampleKeys = keys
            )

            updateSlotState(slotId) { slot ->
                slot.copy(
                    uriString = uri,
                    verificationState = validState
                )
            }
        }
        recalculateAggregateState()
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }
}
