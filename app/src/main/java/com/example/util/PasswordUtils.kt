package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

object PasswordUtils {

    /**
     * Hashes password using SHA-256 with a secure random salt.
     * Format: saltBase64:hashBase64
     */
    fun hashPassword(password: String): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))

        val saltStr = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashStr = Base64.encodeToString(hash, Base64.NO_WRAP)
        return "$saltStr:$hashStr"
    }

    /**
     * Verifies entered password against stored salt:hash string.
     */
    fun verifyPassword(password: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrBlank()) return false
        val parts = storedHash.split(":")
        if (parts.size != 2) return false

        return try {
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expectedHash = Base64.decode(parts[1], Base64.NO_WRAP)

            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(salt)
            val computedHash = digest.digest(password.toByteArray(Charsets.UTF_8))

            MessageDigest.isEqual(expectedHash, computedHash)
        } catch (_: Exception) {
            false
        }
    }
}
