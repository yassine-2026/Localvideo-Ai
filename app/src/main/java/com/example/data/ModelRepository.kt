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
            // 1. ZeroScope v2 (576w) - Lightest (~3.76 GB)
            ModelInfoEntity(
                id = "zeroscope-v2",
                name = "ZeroScope v2 (576w)",
                version = "v2.0 FP16",
                provider = "Cerspense Research",
                license = "CC-BY-NC-4.0",
                downloadUrl = "https://huggingface.co/cerspense/zeroscope_v2_576w",
                sizeBytes = 3_765_000_000L,
                sizeFormatted = "3.76 GB (UNet 2.75GB + OpenCLIP 680MB + VAE 335MB)",
                minRamGb = 8.0,
                architecture = "Temporal UNet + OpenCLIP",
                expectedSha256 = "",
                status = "EXCEEDS_LIMIT",
                verificationNotes = "أخف نموذج مفتوح المصدر (3.76 GB إجمالي الملفات). يتجاوز سقف 1.5 GB الصارم للهاتف."
            ),
            // 2. Wan 2.1 (1.3B) (~4.10 - 5.6 GB)
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
                architecture = "Spatiotemporal Flow DiT + UMT5-XXL",
                expectedSha256 = "",
                status = "EXCEEDS_LIMIT",
                verificationNotes = "أصغر معمارية DiT حقيقية في العالم. تتجاوز حد 1.5 GB وتتطلب 10 GB RAM."
            ),
            // 3. CogVideoX-2B (~4.84 - 9.6 GB)
            ModelInfoEntity(
                id = "cogvideox-2b",
                name = "CogVideoX-2B",
                version = "v1.5 INT4",
                provider = "THUDM / Zhipu AI",
                license = "Apache 2.0",
                downloadUrl = "https://huggingface.co/THUDM/CogVideoX-2b",
                sizeBytes = 4_840_000_000L,
                sizeFormatted = "4.84 GB (DiT 2.2GB + T5 4.8GB + VAE 0.4GB)",
                minRamGb = 12.0,
                architecture = "3D Causal VAE + Expert DiT",
                expectedSha256 = "",
                status = "EXCEEDS_LIMIT",
                verificationNotes = "محول انتشار ثلاثي الأبعاد سينمائي. يتجاوز حد 1.5 GB ويتطلب 12-14 GB RAM."
            ),
            // 4. LTX-Video (2B) - Heaviest (~6.94 GB)
            ModelInfoEntity(
                id = "ltx-video-2b",
                name = "LTX-Video (2B)",
                version = "v0.9.1 FP8",
                provider = "Lightricks Research",
                license = "OpenRAIL-M",
                downloadUrl = "https://huggingface.co/Lightricks/LTX-Video",
                sizeBytes = 6_940_000_000L,
                sizeFormatted = "6.94 GB (DiT 4.19GB + T5 2.4GB + VAE 0.35GB)",
                minRamGb = 12.0,
                architecture = "Diffusion Transformer (DiT 2B) + T5-XXL",
                expectedSha256 = "",
                status = "EXCEEDS_LIMIT",
                verificationNotes = "محول انتشار عالي السرعة (6.94 GB حزمة الملفات). يتطلب 12 GB ذاكرة رسومية."
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
            } else {
                // Update metadata if needed
                modelDao.updateModel(
                    existing.copy(
                        name = model.name,
                        version = model.version,
                        sizeBytes = model.sizeBytes,
                        sizeFormatted = model.sizeFormatted,
                        verificationNotes = model.verificationNotes
                    )
                )
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
