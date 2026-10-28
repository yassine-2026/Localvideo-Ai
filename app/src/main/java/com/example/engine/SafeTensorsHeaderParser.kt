package com.example.engine

import android.util.Log
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class SafeTensorsRole {
    UNET,
    TEXT_ENCODER,
    VAE,
    UNKNOWN
}

data class SafeTensorsValidationResult(
    val isValid: Boolean,
    val detectedRole: SafeTensorsRole,
    val tensorCount: Int = 0,
    val headerSizeBytes: Long = 0,
    val sampleKeys: List<String> = emptyList(),
    val failureReasonAr: String? = null,
    val failureReasonEn: String? = null,
    val technicalDetail: String? = null
)

object SafeTensorsHeaderParser {

    private const val TAG = "SafeTensorsParser"
    private const val MAX_HEADER_SIZE_BYTES = 25 * 1024 * 1024 // 25 MB safety limit for JSON header

    /**
     * Parses the SafeTensors binary header without reading the full tensor data.
     * SafeTensors binary format:
     * - First 8 bytes: unsigned 64-bit integer, little-endian (header size N in bytes).
     * - Next N bytes: UTF-8 encoded JSON string mapping tensor names to metadata.
     * - Remainder: Raw binary tensor data buffer.
     */
    fun parseHeader(
        inputStream: InputStream,
        expectedRole: SafeTensorsRole? = null
    ): SafeTensorsValidationResult {
        try {
            // Read 8-byte length prefix
            val lengthBytes = ByteArray(8)
            var bytesRead = 0
            while (bytesRead < 8) {
                val read = inputStream.read(lengthBytes, bytesRead, 8 - bytesRead)
                if (read == -1) break
                bytesRead += read
            }

            if (bytesRead < 8) {
                return SafeTensorsValidationResult(
                    isValid = false,
                    detectedRole = SafeTensorsRole.UNKNOWN,
                    failureReasonAr = "ملف غير مكتمل أو تالف: ترويسة SafeTensors الثنائية أصغر من 8 بايت.",
                    failureReasonEn = "Incomplete or corrupt file: header is smaller than 8 bytes.",
                    technicalDetail = "Read only $bytesRead bytes for header length prefix."
                )
            }

            // Check if file is HTML error page (e.g. HuggingFace 404 or login required)
            val asciiProbe = String(lengthBytes, Charsets.US_ASCII)
            if (asciiProbe.contains("<html", ignoreCase = true) ||
                asciiProbe.contains("<!doc", ignoreCase = true) ||
                asciiProbe.contains("<head", ignoreCase = true) ||
                asciiProbe.contains("error", ignoreCase = true)
            ) {
                return SafeTensorsValidationResult(
                    isValid = false,
                    detectedRole = SafeTensorsRole.UNKNOWN,
                    failureReasonAr = "الملف المختار هو صفحة ويب HTML وليس ملف أوزان SafeTensors ثنائي حقيقي. غالباً ما يكون صفحة خطأ 404 أو صفحة توثيق Hugging Face.",
                    failureReasonEn = "Selected file is an HTML webpage rather than a real binary SafeTensors weights file (e.g. 404 or auth prompt).",
                    technicalDetail = "Header starts with ASCII: $asciiProbe"
                )
            }

            val buffer = ByteBuffer.wrap(lengthBytes).order(ByteOrder.LITTLE_ENDIAN)
            val headerSize = buffer.long

            if (headerSize <= 0) {
                return SafeTensorsValidationResult(
                    isValid = false,
                    detectedRole = SafeTensorsRole.UNKNOWN,
                    failureReasonAr = "ترويسة SafeTensors غير صالحة: طول الترويسة ($headerSize) غير موجب.",
                    failureReasonEn = "Invalid SafeTensors header length ($headerSize).",
                    technicalDetail = "Header size reported as $headerSize bytes."
                )
            }

            if (headerSize > MAX_HEADER_SIZE_BYTES) {
                return SafeTensorsValidationResult(
                    isValid = false,
                    detectedRole = SafeTensorsRole.UNKNOWN,
                    failureReasonAr = "طول ترويسة SafeTensors (${headerSize / (1024 * 1024)} MB) يتجاوز الحد الآمن (25 MB). قد يكون الملف غير متوافق.",
                    failureReasonEn = "SafeTensors header length (${headerSize / (1024 * 1024)} MB) exceeds safety threshold.",
                    technicalDetail = "Header length: $headerSize bytes"
                )
            }

            // Read header bytes
            val headerBytes = ByteArray(headerSize.toInt())
            var jsonRead = 0
            while (jsonRead < headerBytes.size) {
                val read = inputStream.read(headerBytes, jsonRead, headerBytes.size - jsonRead)
                if (read == -1) break
                jsonRead += read
            }

            if (jsonRead < headerBytes.size) {
                return SafeTensorsValidationResult(
                    isValid = false,
                    detectedRole = SafeTensorsRole.UNKNOWN,
                    failureReasonAr = "انقطاع مفاجئ في الملف: تم قراءة $jsonRead بايت فقط من أصل $headerSize بايت للترويسة.",
                    failureReasonEn = "Unexpected end of file while reading header: read $jsonRead of $headerSize bytes.",
                    technicalDetail = "Incomplete header stream."
                )
            }

            val jsonString = String(headerBytes, Charsets.UTF_8)
            val keys = extractHeaderKeys(jsonString)

            if (keys.isEmpty()) {
                return SafeTensorsValidationResult(
                    isValid = false,
                    detectedRole = SafeTensorsRole.UNKNOWN,
                    failureReasonAr = "ملف SafeTensors فارغ أو تالف: لا يحتوي على أي مصفوفات أو أوزان (Tensors).",
                    failureReasonEn = "SafeTensors file contains no tensor weights or header is invalid.",
                    technicalDetail = "Zero keys extracted from header JSON."
                )
            }

            if (keys.isEmpty()) {
                return SafeTensorsValidationResult(
                    isValid = false,
                    detectedRole = SafeTensorsRole.UNKNOWN,
                    failureReasonAr = "ملف SafeTensors فارغ: لا يحتوي على أي مصفوفات أو أوزان (Tensors).",
                    failureReasonEn = "SafeTensors file contains no tensor weights.",
                    technicalDetail = "Zero keys in header JSON."
                )
            }

            val detectedRole = classifyKeys(keys)
            val sampleKeys = keys.take(5)

            // Validate against expected role if provided
            if (expectedRole != null && expectedRole != SafeTensorsRole.UNKNOWN) {
                if (detectedRole != expectedRole) {
                    val (reasonAr, reasonEn) = getMismatchExplanation(expectedRole, detectedRole)
                    return SafeTensorsValidationResult(
                        isValid = false,
                        detectedRole = detectedRole,
                        tensorCount = keys.size,
                        headerSizeBytes = headerSize,
                        sampleKeys = sampleKeys,
                        failureReasonAr = reasonAr,
                        failureReasonEn = reasonEn,
                        technicalDetail = "Expected $expectedRole, but keys match $detectedRole (sample: ${sampleKeys.firstOrNull()})."
                    )
                }
            }

            return SafeTensorsValidationResult(
                isValid = true,
                detectedRole = detectedRole,
                tensorCount = keys.size,
                headerSizeBytes = headerSize,
                sampleKeys = sampleKeys,
                failureReasonAr = null,
                failureReasonEn = null,
                technicalDetail = "Verified ${keys.size} tensors in SafeTensors header ($headerSize bytes)."
            )

        } catch (e: Exception) {
            try {
                Log.e(TAG, "Error parsing SafeTensors header", e)
            } catch (_: Throwable) {
                System.err.println("SafeTensorsHeaderParser: ${e.message}")
            }
            return SafeTensorsValidationResult(
                isValid = false,
                detectedRole = SafeTensorsRole.UNKNOWN,
                failureReasonAr = "خطأ أثناء قراءة الملف: ${e.localizedMessage ?: e.message}",
                failureReasonEn = "Error reading SafeTensors file: ${e.localizedMessage ?: e.message}",
                technicalDetail = e.stackTraceToString()
            )
        }
    }

    private fun extractHeaderKeys(jsonString: String): List<String> {
        try {
            val jsonObject = JSONObject(jsonString)
            val keys = mutableListOf<String>()
            val keyIterator = jsonObject.keys()
            while (keyIterator.hasNext()) {
                val key = keyIterator.next()
                if (key != "__metadata__") {
                    keys.add(key)
                }
            }
            if (keys.isNotEmpty()) return keys
        } catch (_: Throwable) {}

        val keys = mutableListOf<String>()
        val regex = Regex("\"([^\"]+)\"\\s*:\\s*\\{")
        for (match in regex.findAll(jsonString)) {
            val k = match.groupValues[1]
            if (k != "__metadata__" && !keys.contains(k)) {
                keys.add(k)
            }
        }
        return keys
    }

    private fun classifyKeys(keys: List<String>): SafeTensorsRole {
        var unetScore = 0
        var textEncoderScore = 0
        var vaeScore = 0

        for (k in keys) {
            val keyLower = k.lowercase()
            // UNet keys in diffusers: down_blocks, up_blocks, mid_block, time_embed, conv_in, temporal_transformer_blocks
            if (keyLower.contains("down_blocks") ||
                keyLower.contains("up_blocks") ||
                keyLower.contains("mid_block") ||
                keyLower.contains("time_embed") ||
                keyLower.contains("time_embedding") ||
                keyLower.contains("temporal_transformer") ||
                keyLower.contains("temp_conv")
            ) {
                unetScore++
            }

            // Text Encoder keys: text_model, encoder.layers, token_embedding, position_embedding, text_projection
            if (keyLower.contains("text_model") ||
                keyLower.contains("token_embedding") ||
                keyLower.contains("position_embedding") ||
                keyLower.contains("text_projection") ||
                (keyLower.contains("encoder.layers") && !keyLower.contains("down_blocks"))
            ) {
                textEncoderScore++
            }

            // VAE keys: decoder, encoder, post_quant_conv, quant_conv
            if (keyLower.contains("post_quant_conv") ||
                keyLower.contains("quant_conv") ||
                (keyLower.contains("decoder.") && !keyLower.contains("text_model")) ||
                (keyLower.contains("encoder.conv_in") && !keyLower.contains("down_blocks"))
            ) {
                vaeScore++
            }
        }

        return when {
            unetScore > textEncoderScore && unetScore > vaeScore -> SafeTensorsRole.UNET
            textEncoderScore > unetScore && textEncoderScore > vaeScore -> SafeTensorsRole.TEXT_ENCODER
            vaeScore > unetScore && vaeScore > textEncoderScore -> SafeTensorsRole.VAE
            unetScore > 0 -> SafeTensorsRole.UNET
            textEncoderScore > 0 -> SafeTensorsRole.TEXT_ENCODER
            vaeScore > 0 -> SafeTensorsRole.VAE
            else -> SafeTensorsRole.UNKNOWN
        }
    }

    private fun getMismatchExplanation(expected: SafeTensorsRole, detected: SafeTensorsRole): Pair<String, String> {
        return when (expected) {
            SafeTensorsRole.UNET -> {
                when (detected) {
                    SafeTensorsRole.TEXT_ENCODER -> Pair(
                        "تم تحديد ملف مشفر النصوص (Text Encoder / CLIP) في حقل UNet. يرجى اختيار ملف unet/diffusion_pytorch_model.safetensors.",
                        "Text Encoder file selected in UNet slot. Please select unet/diffusion_pytorch_model.safetensors."
                    )
                    SafeTensorsRole.VAE -> Pair(
                        "تم تحديد ملف مفكك VAE الصوري في حقل UNet. يرجى اختيار ملف unet/diffusion_pytorch_model.safetensors.",
                        "VAE Decoder file selected in UNet slot. Please select unet/diffusion_pytorch_model.safetensors."
                    )
                    else -> Pair(
                        "الملف المختار لا يحتوي على معمارية UNet لإزالة الضجيج (Spatiotemporal UNet).",
                        "Selected file does not contain UNet spatiotemporal denoising architecture."
                    )
                }
            }
            SafeTensorsRole.TEXT_ENCODER -> {
                when (detected) {
                    SafeTensorsRole.UNET -> Pair(
                        "تم تحديد ملف أوزان UNet في حقل مشفر النصوص. يرجى اختيار ملف text_encoder/model.safetensors.",
                        "UNet weights selected in Text Encoder slot. Please select text_encoder/model.safetensors."
                    )
                    SafeTensorsRole.VAE -> Pair(
                        "تم تحديد ملف مفكك VAE في حقل مشفر النصوص. يرجى اختيار ملف text_encoder/model.safetensors.",
                        "VAE weights selected in Text Encoder slot. Please select text_encoder/model.safetensors."
                    )
                    else -> Pair(
                        "الملف المختار لا يحتوي على معمارية ترميز النصوص (CLIP / OpenCLIP Text Transformer).",
                        "Selected file does not contain CLIP text transformer architecture."
                    )
                }
            }
            SafeTensorsRole.VAE -> {
                when (detected) {
                    SafeTensorsRole.UNET -> Pair(
                        "تم تحديد ملف أوزان UNet في حقل مفكك VAE. يرجى اختيار ملف vae/diffusion_pytorch_model.safetensors.",
                        "UNet weights selected in VAE slot. Please select vae/diffusion_pytorch_model.safetensors."
                    )
                    SafeTensorsRole.TEXT_ENCODER -> Pair(
                        "تم تحديد ملف مشفر النصوص في حقل مفكك VAE. يرجى اختيار ملف vae/diffusion_pytorch_model.safetensors.",
                        "Text Encoder weights selected in VAE slot. Please select vae/diffusion_pytorch_model.safetensors."
                    )
                    else -> Pair(
                        "الملف المختار لا يحتوي على معمارية مفكك الترميز الصوري (AutoencoderKL Decoder).",
                        "Selected file does not contain AutoencoderKL Decoder architecture."
                    )
                }
            }
            else -> Pair(
                "نوع الملف المختار ($detected) غير متطابق مع المكوّن المطلوب.",
                "Selected file role ($detected) does not match required component."
            )
        }
    }
}
