package com.example.data

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.ModelInfoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class ModelRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val modelDao = db.modelDao()

    val allModels: Flow<List<ModelInfoEntity>> = modelDao.getAllModelsFlow()

    suspend fun initializeCatalogIfEmpty() = withContext(Dispatchers.IO) {
        val initialList = listOf(
            ModelInfoEntity(
                id = "wan-2.1-1.3b",
                name = "Wan 2.1 (1.3B)",
                version = "v2.1",
                provider = "Wan-Video / Alibaba Research",
                license = "Apache 2.0",
                downloadUrl = "https://huggingface.co/Wan-AI/Wan2.1-T2V-1.3B",
                sizeBytes = 4_100_000_000L,
                sizeFormatted = "4.10 GB (DiT 1.3GB + UMT5 2.4GB + VAE 0.4GB)",
                minRamGb = 10.0,
                architecture = "Spatiotemporal Flow Transformer",
                expectedSha256 = "",
                status = "EXCEEDS_LIMIT",
                verificationNotes = "أصغر نموذج DiT حقيقي متاح عالمياً. يتجاوز حد 1.5 GB ويتطلب 10 GB RAM."
            ),
            ModelInfoEntity(
                id = "ltx-video-2b",
                name = "LTX-Video 2B",
                version = "v0.9.1",
                provider = "Lightricks Research",
                license = "OpenRAIL-M",
                downloadUrl = "https://huggingface.co/Lightricks/LTX-Video",
                sizeBytes = 6_600_000_000L,
                sizeFormatted = "6.60 GB (FP8 DiT 4.2GB + T5 2.4GB)",
                minRamGb = 12.0,
                architecture = "Diffusion Transformer (DiT)",
                expectedSha256 = "",
                status = "EXCEEDS_LIMIT",
                verificationNotes = "يتطلب بطاقة معالجة مكتبية بذاكرة 12 GB. يتجاوز حد الذاكرة المسموح به للهاتف."
            ),
            ModelInfoEntity(
                id = "cogvideox-2b",
                name = "CogVideoX-2B",
                version = "v1.5 INT4",
                provider = "THUDM / Zhipu AI",
                license = "Apache 2.0",
                downloadUrl = "https://huggingface.co/THUDM/CogVideoX-2b",
                sizeBytes = 5_200_000_000L,
                sizeFormatted = "4.84 GB",
                minRamGb = 14.0,
                architecture = "3D VAE + Expert DiT",
                expectedSha256 = "",
                status = "EXCEEDS_LIMIT",
                verificationNotes = "يتجاوز حد 1.5 GB. لا يمكن تشغيله على الهواتف دون تسريع سحابي."
            ),
            ModelInfoEntity(
                id = "zeroscope-v2",
                name = "ZeroScope v2 (576w)",
                version = "v2.0",
                provider = "Cerspense Research",
                license = "CC-BY-NC-4.0",
                downloadUrl = "https://huggingface.co/cerspense/zeroscope_v2_576w",
                sizeBytes = 3_700_000_000L,
                sizeFormatted = "3.70 GB (UNet 2.8GB + CLIP 0.6GB + VAE 0.3GB)",
                minRamGb = 8.0,
                architecture = "Temporal UNet + CLIP",
                expectedSha256 = "",
                status = "EXCEEDS_LIMIT",
                verificationNotes = "يتجاوز حد 1.5 GB ولا يمكن تحميله في ذاكرة الهاتف."
            )
        )

        // Clear any old fake models (e.g. microvideo-quantized or microvideo-proto)
        val oldFakeIds = listOf("microvideo-quantized", "microvideo-proto")
        for (fakeId in oldFakeIds) {
            val fake = modelDao.getModelById(fakeId)
            if (fake != null) {
                modelDao.deleteModel(fake)
            }
        }

        for (model in initialList) {
            val existing = modelDao.getModelById(model.id)
            if (existing == null) {
                modelDao.insertModel(model)
            }
        }
    }

    suspend fun getActiveModel(): ModelInfoEntity? = withContext(Dispatchers.IO) {
        modelDao.getActiveReadyModel()
    }

    suspend fun updateModel(model: ModelInfoEntity) = withContext(Dispatchers.IO) {
        modelDao.updateModel(model)
    }

    suspend fun registerCustomImport(
        name: String,
        file: File,
        sha256: String
    ): Result<ModelInfoEntity> = withContext(Dispatchers.IO) {
        val sizeBytes = file.length()
        val maxAllowedBytes = 1_610_612_736L // 1.5 GiB strict ceiling

        if (sizeBytes > maxAllowedBytes) {
            return@withContext Result.failure(
                IllegalArgumentException("الملف المختار (${sizeBytes / (1024 * 1024)} MB) يتجاوز الحد الصارم 1.5 GB (1536 MB).")
            )
        }

        val sizeFormatted = "${(sizeBytes / (1024.0 * 1024.0)).toInt()} MB"
        val customModel = ModelInfoEntity(
            id = "custom-${System.currentTimeMillis()}",
            name = name.ifBlank { file.nameWithoutExtension },
            version = "Custom Importer",
            provider = "Local User",
            license = "User Supplied",
            downloadUrl = "file://${file.absolutePath}",
            sizeBytes = sizeBytes,
            sizeFormatted = sizeFormatted,
            minRamGb = 4.0,
            architecture = "Custom ONNX / TFLite / GGUF Tensor Weights",
            expectedSha256 = sha256,
            status = "READY",
            localFilePath = file.absolutePath,
            downloadedBytes = sizeBytes,
            verificationNotes = "تم استيراد النموذج وفحصه محلياً ضمن سقف 1.5 GB.",
            isCustomImported = true
        )

        modelDao.insertModel(customModel)
        Result.success(customModel)
    }

    suspend fun deleteModel(model: ModelInfoEntity) = withContext(Dispatchers.IO) {
        model.localFilePath?.let { path ->
            val f = File(path)
            if (f.exists()) f.delete()
        }
        if (model.isCustomImported) {
            modelDao.deleteModel(model)
        } else {
            modelDao.updateDownloadState(model.id, "NOT_DOWNLOADED", 0L, null)
        }
    }
}
