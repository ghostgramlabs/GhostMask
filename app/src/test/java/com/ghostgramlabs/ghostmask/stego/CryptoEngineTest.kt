package com.ghostgramlabs.ghostmask.stego

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [CryptoEngine].
 * Verifies encrypt/decrypt roundtrip, wrong password detection, and edge cases.
 */
class CryptoEngineTest {

    @Test
    fun `encrypt and decrypt roundtrip returns original data`() {
        val original = "Hello, GhostMask! This is a secret message.".toByteArray()
        val password = "MyStrongPassword123!"

        val encrypted = CryptoEngine.encrypt(original, password)
        val decrypted = CryptoEngine.decrypt(encrypted, password)

        assertThat(decrypted).isEqualTo(original)
    }

    @Test
    fun `encrypt and decrypt large data roundtrip`() {
        val original = ByteArray(50_000) { (it % 256).toByte() }
        val password = "test-password"

        val encrypted = CryptoEngine.encrypt(original, password)
        val decrypted = CryptoEngine.decrypt(encrypted, password)

        assertThat(decrypted).isEqualTo(original)
    }

    @Test(expected = WrongPasswordException::class)
    fun `decrypt with wrong password throws WrongPasswordException`() {
        val original = "Secret data".toByteArray()
        val encrypted = CryptoEngine.encrypt(original, "correct-password")

        CryptoEngine.decrypt(encrypted, "wrong-password")
    }

    @Test
    fun `encrypt produces different ciphertext each time due to random salt and IV`() {
        val original = "Same data".toByteArray()
        val password = "same-password"

        val encrypted1 = CryptoEngine.encrypt(original, password)
        val encrypted2 = CryptoEngine.encrypt(original, password)

        // Salt and IV should differ, making ciphertext different
        assertThat(encrypted1.toByteArray()).isNotEqualTo(encrypted2.toByteArray())

        // But both should decrypt to the same result
        assertThat(CryptoEngine.decrypt(encrypted1, password)).isEqualTo(original)
        assertThat(CryptoEngine.decrypt(encrypted2, password)).isEqualTo(original)
    }

    @Test
    fun `encrypt single byte data`() {
        val original = byteArrayOf(42)
        val password = "pass"

        val encrypted = CryptoEngine.encrypt(original, password)
        val decrypted = CryptoEngine.decrypt(encrypted, password)

        assertThat(decrypted).isEqualTo(original)
    }

    @Test
    fun `EncryptedPackage serialization roundtrip`() {
        val original = "Test data for serialization".toByteArray()
        val password = "serialize-test"

        val encrypted = CryptoEngine.encrypt(original, password)
        val serialized = encrypted.toByteArray()
        val deserialized = EncryptedPackage.fromByteArray(serialized)

        assertThat(deserialized.salt).isEqualTo(encrypted.salt)
        assertThat(deserialized.iv).isEqualTo(encrypted.iv)
        assertThat(deserialized.ciphertext).isEqualTo(encrypted.ciphertext)

        // And should still decrypt correctly
        val decrypted = CryptoEngine.decrypt(deserialized, password)
        assertThat(decrypted).isEqualTo(original)
    }
}
