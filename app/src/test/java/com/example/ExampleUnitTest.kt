package com.example

import com.example.download.FileIntegrityVerifier
import com.example.engine.DefaultLocalInferenceEngine
import com.example.engine.PromptEnricher
import com.example.engine.VideoEncoderManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class ExampleUnitTest {

    @Test
    fun testPromptEnricher_appendsCinematicAttributes() {
        val base = "Futuristic neon sports car speeding in rain"
        val enriched = PromptEnricher.enrichPrompt(base)

        assertTrue(enriched.startsWith(base))
        assertTrue(enriched.length > base.length)
    }

    @Test
    fun testModelSizeLimit_strictlyEnforcesOnePointFiveGb() {
        val maxAllowedBinaryBytes = 1_610_612_736L // 1.5 GiB
        val wanVideoBytes = 4_100_000_000L
        val ltxVideoBytes = 6_600_000_000L
        val cogVideoBytes = 4_840_000_000L
        val zeroScopeBytes = 3_700_000_000L

        assertTrue("Wan 2.1 must exceed 1.5 GB limit", wanVideoBytes > maxAllowedBinaryBytes)
        assertTrue("LTX-Video must exceed 1.5 GB limit", ltxVideoBytes > maxAllowedBinaryBytes)
        assertTrue("CogVideoX must exceed 1.5 GB limit", cogVideoBytes > maxAllowedBinaryBytes)
        assertTrue("ZeroScope must exceed 1.5 GB limit", zeroScopeBytes > maxAllowedBinaryBytes)
    }

    @Test
    fun testSha256Verifier_matchesKnownHashAndDetectsMismatch() {
        runBlocking {
            val tempFile = File.createTempFile("test_hash", ".txt").apply {
                writeText("LocalVideoAI-Integrity-Check")
            }
            val calculated = FileIntegrityVerifier.calculateSha256(tempFile)
            assertTrue(calculated.isNotBlank())
            assertEquals(64, calculated.length)

            val matches = FileIntegrityVerifier.verifyIntegrity(tempFile, calculated)
            assertTrue(matches)

            val mismatched = FileIntegrityVerifier.verifyIntegrity(tempFile, "badhash0000000000000000000000000000000000000000000000000000000000")
            assertFalse(mismatched)

            tempFile.delete()
        }
    }

    @Test
    fun testLocalInferenceEngine_detectsFormatsAndRejectsHtmlFiles() {
        val engine = DefaultLocalInferenceEngine()

        // 1. Non-existent file
        val missingFile = File("/tmp/non_existent_weights.bin")
        val resultMissing = engine.verifyModelFiles(missingFile)
        assertFalse(resultMissing.isValid)

        // 2. Empty file
        val emptyFile = File.createTempFile("empty_weights", ".bin")
        val resultEmpty = engine.verifyModelFiles(emptyFile)
        assertFalse(resultEmpty.isValid)
        emptyFile.delete()

        // 3. HTML webpage returned instead of binary weights (e.g. 404 or repo viewer HTML)
        val htmlFile = File.createTempFile("error_page", ".bin")
        FileOutputStream(htmlFile).use { fos ->
            fos.write("<!DOCTYPE html><html><head><title>404 Not Found</title></head></html>".toByteArray())
        }
        val resultHtml = engine.verifyModelFiles(htmlFile)
        assertFalse("Engine must reject HTML error pages as model weights", resultHtml.isValid)
        assertEquals("HTML Webpage", resultHtml.formatDetected)
        htmlFile.delete()

        // 4. Valid GGUF header
        val ggufFile = File.createTempFile("test_model", ".gguf")
        FileOutputStream(ggufFile).use { fos ->
            val ggufHeader = byteArrayOf('G'.code.toByte(), 'G'.code.toByte(), 'U'.code.toByte(), 'F'.code.toByte())
            val padding = ByteArray(60)
            fos.write(ggufHeader)
            fos.write(padding)
        }
        val resultGguf = engine.verifyModelFiles(ggufFile)
        assertTrue(resultGguf.isValid)
        assertEquals("GGUF Quantized", resultGguf.formatDetected)
        ggufFile.delete()
    }

    @Test
    fun testVideoEncoderManager_validatesMissingOrInvalidMp4() {
        val nonExistent = File("/tmp/non_existent_video.mp4")
        val validationResult = VideoEncoderManager.validateGeneratedMp4(nonExistent)
        assertFalse(validationResult.isSuccess)
    }

    @Test
    fun testModelCatalog_orderedFromLightestToHeaviestWithValidBreakdowns() {
        val catalog = com.example.data.model.ModelCatalog.RESEARCH_MODELS
        assertEquals(4, catalog.size)

        // Verify order from lightest to heaviest
        assertEquals("zeroscope-v2", catalog[0].id)
        assertEquals("wan-2.1-1.3b", catalog[1].id)
        assertEquals("cogvideox-2b", catalog[2].id)
        assertEquals("ltx-video-2b", catalog[3].id)

        assertTrue(catalog[0].totalSizeBytes < catalog[1].totalSizeBytes)
        assertTrue(catalog[1].totalSizeBytes < catalog[2].totalSizeBytes)
        assertTrue(catalog[2].totalSizeBytes < catalog[3].totalSizeBytes)

        // Verify all 4 have itemized multi-file components
        for (model in catalog) {
            assertTrue("Model ${model.name} must have multiple components", model.components.size >= 3)
            assertFalse("Model ${model.name} must not be compatible with 1.5 GB phone limit", model.isCompatibleWithDeviceLimit)
            assertTrue("Model ${model.name} must have verified Hugging Face repo URL", model.huggingFaceRepoUrl.startsWith("https://huggingface.co/"))
            assertTrue("Model ${model.name} must have direct download URL", !model.directDownloadUrl.isNullOrBlank())
        }
    }

    @Test
    fun testSafeTensorsHeaderParser_classifiesUnetCorrectly() {
        val json = """{"down_blocks.0.attentions.0.weight": {"dtype": "F16", "shape": [320, 320]}, "time_embed.0.weight": {"dtype": "F16", "shape": [320, 1280]}}"""
        val bytes = createSyntheticSafeTensorsBytes(json)
        val stream = java.io.ByteArrayInputStream(bytes)

        val result = com.example.engine.SafeTensorsHeaderParser.parseHeader(
            stream,
            com.example.engine.SafeTensorsRole.UNET
        )

        assertTrue("UNet header must be valid", result.isValid)
        assertEquals(com.example.engine.SafeTensorsRole.UNET, result.detectedRole)
        assertEquals(2, result.tensorCount)
    }

    @Test
    fun testSafeTensorsHeaderParser_classifiesTextEncoderCorrectly() {
        val json = """{"text_model.embeddings.token_embedding.weight": {"dtype": "F16", "shape": [49408, 1024]}, "text_model.encoder.layers.0.self_attn.weight": {"dtype": "F16", "shape": [1024, 1024]}}"""
        val bytes = createSyntheticSafeTensorsBytes(json)
        val stream = java.io.ByteArrayInputStream(bytes)

        val result = com.example.engine.SafeTensorsHeaderParser.parseHeader(
            stream,
            com.example.engine.SafeTensorsRole.TEXT_ENCODER
        )

        assertTrue("Text encoder header must be valid", result.isValid)
        assertEquals(com.example.engine.SafeTensorsRole.TEXT_ENCODER, result.detectedRole)
        assertEquals(2, result.tensorCount)
    }

    @Test
    fun testSafeTensorsHeaderParser_classifiesVaeCorrectly() {
        val json = """{"post_quant_conv.weight": {"dtype": "F16", "shape": [4, 4, 1, 1]}, "decoder.conv_in.weight": {"dtype": "F16", "shape": [512, 4, 3, 3]}}"""
        val bytes = createSyntheticSafeTensorsBytes(json)
        val stream = java.io.ByteArrayInputStream(bytes)

        val result = com.example.engine.SafeTensorsHeaderParser.parseHeader(
            stream,
            com.example.engine.SafeTensorsRole.VAE
        )

        assertTrue("VAE header must be valid", result.isValid)
        assertEquals(com.example.engine.SafeTensorsRole.VAE, result.detectedRole)
        assertEquals(2, result.tensorCount)
    }

    @Test
    fun testSafeTensorsHeaderParser_detectsComponentMismatch() {
        // Provide VAE keys when UNet is expected
        val json = """{"post_quant_conv.weight": {"dtype": "F16", "shape": [4, 4]}, "decoder.conv_in.weight": {"dtype": "F16", "shape": [512, 4]}}"""
        val bytes = createSyntheticSafeTensorsBytes(json)
        val stream = java.io.ByteArrayInputStream(bytes)

        val result = com.example.engine.SafeTensorsHeaderParser.parseHeader(
            stream,
            com.example.engine.SafeTensorsRole.UNET
        )

        assertFalse("Must reject VAE weights in UNet slot", result.isValid)
        assertEquals(com.example.engine.SafeTensorsRole.VAE, result.detectedRole)
        assertTrue(result.failureReasonAr?.contains("VAE") == true)
    }

    @Test
    fun testSafeTensorsHeaderParser_rejectsHtmlFile() {
        val htmlContent = "<!DOCTYPE html><html><body>404 Not Found from Hugging Face</body></html>".toByteArray()
        val stream = java.io.ByteArrayInputStream(htmlContent)

        val result = com.example.engine.SafeTensorsHeaderParser.parseHeader(
            stream,
            com.example.engine.SafeTensorsRole.UNET
        )

        assertFalse("Must reject HTML file as SafeTensors", result.isValid)
        assertTrue(result.failureReasonAr?.contains("HTML") == true)
    }

    private fun createSyntheticSafeTensorsBytes(json: String): ByteArray {
        val jsonBytes = json.toByteArray(Charsets.UTF_8)
        val headerLen = jsonBytes.size.toLong()

        val buf = java.nio.ByteBuffer.allocate(8 + jsonBytes.size)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buf.putLong(headerLen)
        buf.put(jsonBytes)
        return buf.array()
    }
}
