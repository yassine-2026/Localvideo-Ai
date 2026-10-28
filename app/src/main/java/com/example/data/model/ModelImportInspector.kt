package com.example.data.model

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.download.FileIntegrityVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ComponentRole {
    BACKBONE_DIT_UNET,
    TEXT_ENCODER,
    VAE_DECODER,
    CONFIG_OR_TOKENIZER,
    STANDALONE_NEURAL_GRAPH,
    UNKNOWN
}

data class InspectedFileItem(
    val uri: Uri,
    val fileName: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val role: ComponentRole,
    val roleLabelAr: String,
    val roleLabelEn: String,
    val format: String
)

data class ImportInspectionReport(
    val files: List<InspectedFileItem>,
    val totalSizeBytes: Long,
    val totalSizeFormatted: String,
    val hasBackbone: Boolean,
    val hasTextEncoder: Boolean,
    val hasVaeDecoder: Boolean,
    val isStandaloneGraph: Boolean,
    val isExceedingLimit: Boolean,
    val isValidAndImportable: Boolean,
    val statusTitleAr: String,
    val statusTitleEn: String,
    val rejectionReasonAr: String?,
    val rejectionReasonEn: String?
)

object ModelImportInspector {

    private const val MAX_ALLOWED_BYTES = 1_610_612_736L // 1.5 GiB

    fun inspectUris(context: Context, uris: List<Uri>): ImportInspectionReport {
        val resolver = context.contentResolver
        val inspectedItems = mutableListOf<InspectedFileItem>()
        var totalBytes = 0L

        for (uri in uris) {
            var fileName = "unknown_file.bin"
            var sizeBytes = 0L

            try {
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val queriedName = cursor.getString(nameIndex)
                            if (!queriedName.isNullOrBlank()) fileName = queriedName
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            sizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                fileName = uri.lastPathSegment ?: "unknown_file.bin"
            }

            // Fallback for size if cursor returned 0 or -1
            if (sizeBytes <= 0) {
                try {
                    resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        sizeBytes = pfd.statSize
                    }
                } catch (_: Exception) {}
            }

            totalBytes += sizeBytes.coerceAtLeast(0L)

            val role = classifyFileRole(fileName)
            val format = detectFileFormat(fileName)

            val roleLabelAr = when (role) {
                ComponentRole.BACKBONE_DIT_UNET -> "أوزان توليد الحركة (DiT / UNet)"
                ComponentRole.TEXT_ENCODER -> "مشفر النصوص (T5 / CLIP)"
                ComponentRole.VAE_DECODER -> "مفكك التشفير الصوري (3D VAE)"
                ComponentRole.CONFIG_OR_TOKENIZER -> "ملف إعدادات / ترميز (Config / Tokenizer)"
                ComponentRole.STANDALONE_NEURAL_GRAPH -> "رسم بياني مدمج مستقل (GGUF / TFLite / ONNX)"
                ComponentRole.UNKNOWN -> "ملف غير مصنف"
            }

            val roleLabelEn = when (role) {
                ComponentRole.BACKBONE_DIT_UNET -> "DiT / UNet Backbone"
                ComponentRole.TEXT_ENCODER -> "Text Encoder (T5 / CLIP)"
                ComponentRole.VAE_DECODER -> "3D VAE Decoder"
                ComponentRole.CONFIG_OR_TOKENIZER -> "Config / Tokenizer"
                ComponentRole.STANDALONE_NEURAL_GRAPH -> "Standalone Neural Graph (GGUF/TFLite/ONNX)"
                ComponentRole.UNKNOWN -> "Unclassified File"
            }

            inspectedItems.add(
                InspectedFileItem(
                    uri = uri,
                    fileName = fileName,
                    sizeBytes = sizeBytes,
                    sizeFormatted = formatBytes(sizeBytes),
                    role = role,
                    roleLabelAr = roleLabelAr,
                    roleLabelEn = roleLabelEn,
                    format = format
                )
            )
        }

        val hasBackbone = inspectedItems.any { it.role == ComponentRole.BACKBONE_DIT_UNET }
        val hasTextEncoder = inspectedItems.any { it.role == ComponentRole.TEXT_ENCODER }
        val hasVaeDecoder = inspectedItems.any { it.role == ComponentRole.VAE_DECODER }
        val isStandalone = inspectedItems.any { it.role == ComponentRole.STANDALONE_NEURAL_GRAPH }

        val isExceedingLimit = totalBytes > MAX_ALLOWED_BYTES
        val totalSizeFormatted = formatBytes(totalBytes)

        val hasRequiredComponents = isStandalone || (hasBackbone && (hasTextEncoder || hasVaeDecoder)) || (hasBackbone && inspectedItems.size == 1 && inspectedItems.first().format in listOf("GGUF", "TFLite", "ONNX"))

        val isValidAndImportable = !isExceedingLimit && totalBytes > 0 && (hasBackbone || isStandalone)

        val (titleAr, titleEn, reasonAr, reasonEn) = when {
            inspectedItems.isEmpty() -> {
                Quadruple(
                    "لم يتم تحديد أي ملفات",
                    "No Files Selected",
                    "يرجى تحديد ملف أو مجلد ملفات النموذج عبر نافذة الاختيار.",
                    "Please select one or more model files via the system picker."
                )
            }
            isExceedingLimit -> {
                val totalMb = totalBytes / (1024 * 1024)
                Quadruple(
                    "مرفوض: الحجم يتجاوز حد 1.5 GB الصارم",
                    "Rejected: Total Size Exceeds 1.5 GB Limit",
                    "الحجم الإجمالي المقاس ($totalMb MB) يتجاوز الحد الصارم المسموح به للأجهزة المحمولة (1.5 GB / 1536 MB). للحفاظ على استقرار النظام يُمنع استيراد نماذج تتجاوز هذا الحد.",
                    "Total measured size ($totalMb MB) strictly exceeds the 1.5 GB mobile device ceiling. To prevent OOM crashes, models exceeding this threshold are blocked."
                )
            }
            !hasBackbone && !isStandalone -> {
                Quadruple(
                    "حزمة غير مكتملة: مفقود شبكة التوليد الأساسية",
                    "Incomplete Bundle: Missing Denoising Backbone",
                    "تم تحديد ملفات إضافية (مثل VAE أو مشفر النص أو ملفات Config)، ولكنها تفتقر إلى شبكة التوليد الأساسية (DiT أو UNet). لا يمكن تشغيل النموذج بدونها.",
                    "Selected files contain auxiliary components (e.g. VAE, text encoder, or config), but lack the core generative backbone (DiT / UNet). The model cannot function without it."
                )
            }
            !hasRequiredComponents -> {
                Quadruple(
                    "تنبيه: حزمة النموذج مجزأة جزئياً",
                    "Warning: Partially Incomplete Bundle",
                    "تم اكتشاف أوزان التوليد، لكن يفضل توفير مشفر النص (T5/CLIP) ومفكك VAE لتحقيق نتائج مرئية كاملة.",
                    "Backbone detected, but text encoder (T5/CLIP) and 3D VAE are recommended for full visual output."
                )
            }
            else -> {
                Quadruple(
                    "حزمة النموذج متوافقة وجاهزة للتثبيت",
                    "Model Bundle Validated & Ready for Import",
                    null,
                    null
                )
            }
        }

        return ImportInspectionReport(
            files = inspectedItems,
            totalSizeBytes = totalBytes,
            totalSizeFormatted = totalSizeFormatted,
            hasBackbone = hasBackbone,
            hasTextEncoder = hasTextEncoder,
            hasVaeDecoder = hasVaeDecoder,
            isStandaloneGraph = isStandalone,
            isExceedingLimit = isExceedingLimit,
            isValidAndImportable = isValidAndImportable,
            statusTitleAr = titleAr,
            statusTitleEn = titleEn,
            rejectionReasonAr = reasonAr,
            rejectionReasonEn = reasonEn
        )
    }

    suspend fun executeImport(
        context: Context,
        report: ImportInspectionReport,
        targetDir: File
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!report.isValidAndImportable) {
            return@withContext Result.failure(
                IllegalArgumentException(report.rejectionReasonAr ?: "ملفات النموذج غير متوافقة.")
            )
        }

        targetDir.mkdirs()
        val bundleFolder = File(targetDir, "imported_bundle_${System.currentTimeMillis()}").apply { mkdirs() }

        var primaryFile: File? = null

        for (item in report.files) {
            val destination = File(bundleFolder, item.fileName)
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة الملف: ${item.fileName}"))

            if (item.role == ComponentRole.BACKBONE_DIT_UNET || item.role == ComponentRole.STANDALONE_NEURAL_GRAPH || primaryFile == null) {
                primaryFile = destination
            }
        }

        val finalTarget = primaryFile ?: bundleFolder.listFiles()?.firstOrNull()
        if (finalTarget == null || !finalTarget.exists()) {
            return@withContext Result.failure(IllegalStateException("فشل نسخ ملفات النموذج."))
        }

        Result.success(finalTarget)
    }

    private fun classifyFileRole(name: String): ComponentRole {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".gguf") || lower.endsWith(".tflite") || (lower.endsWith(".onnx") && !lower.contains("encoder") && !lower.contains("vae")) -> {
                ComponentRole.STANDALONE_NEURAL_GRAPH
            }
            lower.contains("vae") || lower.contains("autoencoder") -> {
                ComponentRole.VAE_DECODER
            }
            lower.contains("text_encoder") || lower.contains("clip") || lower.contains("t5") || lower.contains("umt5") || lower.contains("tokenizer") -> {
                ComponentRole.TEXT_ENCODER
            }
            lower.endsWith(".json") || lower.contains("config") -> {
                ComponentRole.CONFIG_OR_TOKENIZER
            }
            lower.contains("diffusion") || lower.contains("unet") || lower.contains("dit") || lower.contains("transformer") || lower.endsWith(".safetensors") || lower.endsWith(".bin") || lower.endsWith(".pth") -> {
                ComponentRole.BACKBONE_DIT_UNET
            }
            else -> ComponentRole.UNKNOWN
        }
    }

    private fun detectFileFormat(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".safetensors") -> "SafeTensors"
            lower.endsWith(".gguf") -> "GGUF"
            lower.endsWith(".tflite") -> "TFLite"
            lower.endsWith(".onnx") -> "ONNX"
            lower.endsWith(".pth") || lower.endsWith(".pt") -> "PyTorch"
            lower.endsWith(".bin") -> "Binary Tensor"
            lower.endsWith(".json") -> "JSON Config"
            else -> "Unknown Binary"
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824L -> String.format("%.2f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1024L -> "${bytes / 1024} KB"
            else -> "$bytes B"
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
