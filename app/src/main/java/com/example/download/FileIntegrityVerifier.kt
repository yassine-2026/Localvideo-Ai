package com.example.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object FileIntegrityVerifier {

    suspend fun calculateSha256(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.canRead()) {
            return@withContext ""
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(1024 * 1024) // 1MB buffer
        FileInputStream(file).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        val hexString = StringBuilder()
        for (b in hashBytes) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        hexString.toString()
    }

    suspend fun verifyIntegrity(file: File, expectedSha256: String): Boolean {
        if (expectedSha256.isBlank()) return true
        val calculated = calculateSha256(file)
        return calculated.equals(expectedSha256.trim(), ignoreCase = true)
    }
}
