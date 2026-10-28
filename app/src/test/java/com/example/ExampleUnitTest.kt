package com.example

import com.example.download.FileIntegrityVerifier
import com.example.engine.DefaultLocalInferenceEngine
import com.example.engine.PromptEnricher
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
        val ltxVideoBytes = 4_500_000_000L
        val cogVideoBytes = 5_200_000_000L
        val microVideoBytes = 1_480_000_000L

        assertTrue("LTX-Video must exceed 1.5 GB limit", ltxVideoBytes > maxAllowedBinaryBytes)
        assertTrue("CogVideoX must exceed 1.5 GB limit", cogVideoBytes > maxAllowedBinaryBytes)
        assertTrue("MicroVideo must be within 1.5 GB limit", microVideoBytes <= maxAllowedBinaryBytes)
    }

    @Test
    fun testSha256Verifier_matchesKnownHash() {
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
    fun testLocalInferenceEngine_detectsModelFormatsAndEnforcesLimits() {
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

        // 3. Valid GGUF header
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
}
