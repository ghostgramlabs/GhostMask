package com.ghostgramlabs.ghostmask.stego

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles AES-256-GCM encryption/decryption with PBKDF2 key derivation.
 *
 * Payload layout of [EncryptedPackage]:
 *   [16 bytes salt][12 bytes IV][N bytes ciphertext+authTag]
 */
object CryptoEngine {

    private const val KEY_LENGTH_BITS = 256
    private const val PBKDF2_ITERATIONS = 100_000
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256"

    /**
     * Derives a 256-bit AES key from a password and salt using PBKDF2.
     */
    fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts [data] using AES-GCM with a key derived from [password].
     * Returns an [EncryptedPackage] containing salt, IV, and ciphertext.
     */
    fun encrypt(data: ByteArray, password: String): EncryptedPackage {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(data)

        return EncryptedPackage(salt = salt, iv = iv, ciphertext = ciphertext)
    }

    /**
     * Decrypts an [EncryptedPackage] using the given [password].
     * @throws WrongPasswordException if authentication fails (wrong password or corrupt data).
     */
    fun decrypt(pkg: EncryptedPackage, password: String): ByteArray {
        return try {
            val key = deriveKey(password, pkg.salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, pkg.iv))
            cipher.doFinal(pkg.ciphertext)
        } catch (e: Exception) {
            throw WrongPasswordException("Decryption failed. Wrong password or corrupted data.", e)
        }
    }
}

/**
 * Holds the components of an AES-GCM encrypted payload.
 * Supports serialization to/from a contiguous byte array.
 */
data class EncryptedPackage(
    val salt: ByteArray,
    val iv: ByteArray,
    val ciphertext: ByteArray
) {
    /**
     * Serializes to: [salt][iv][ciphertext]
     */
    fun toByteArray(): ByteArray {
        return ByteBuffer.allocate(salt.size + iv.size + ciphertext.size)
            .put(salt)
            .put(iv)
            .put(ciphertext)
            .array()
    }

    companion object {
        private const val SALT_LEN = 16
        private const val IV_LEN = 12

        /**
         * Deserializes from contiguous bytes: [16 salt][12 iv][rest ciphertext]
         */
        fun fromByteArray(data: ByteArray): EncryptedPackage {
            require(data.size > SALT_LEN + IV_LEN) { "Encrypted data too short" }
            val buf = ByteBuffer.wrap(data)
            val salt = ByteArray(SALT_LEN).also { buf.get(it) }
            val iv = ByteArray(IV_LEN).also { buf.get(it) }
            val ciphertext = ByteArray(buf.remaining()).also { buf.get(it) }
            return EncryptedPackage(salt, iv, ciphertext)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedPackage) return false
        return salt.contentEquals(other.salt) &&
                iv.contentEquals(other.iv) &&
                ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int {
        var result = salt.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }
}

/**
 * Thrown when decryption fails due to wrong password or data corruption.
 */
class WrongPasswordException(message: String, cause: Throwable? = null) : Exception(message, cause)
