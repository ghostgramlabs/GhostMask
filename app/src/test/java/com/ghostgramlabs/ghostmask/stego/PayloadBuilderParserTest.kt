package com.ghostgramlabs.ghostmask.stego

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [PayloadBuilder] and [PayloadParser].
 * Verifies all three payload modes and error handling.
 */
class PayloadBuilderParserTest {

    private val password = "test-password-123"

    @Test
    fun `text-only payload roundtrip`() {
        val text = "This is a hidden message"
        val payload = PayloadBuilder.build(text = text, imageBytes = null, password = password)
        val decoded = PayloadParser.parse(payload, password)

        assertThat(decoded.text).isEqualTo(text)
        assertThat(decoded.imageBytes).isNull()
    }

    @Test
    fun `image-only payload roundtrip`() {
        val imageBytes = ByteArray(500) { (it % 256).toByte() }
        val payload = PayloadBuilder.build(text = null, imageBytes = imageBytes, password = password)
        val decoded = PayloadParser.parse(payload, password)

        assertThat(decoded.text).isNull()
        assertThat(decoded.imageBytes).isEqualTo(imageBytes)
    }

    @Test
    fun `text and image payload roundtrip`() {
        val text = "Secret message with image"
        val imageBytes = ByteArray(1000) { (it % 256).toByte() }
        val payload = PayloadBuilder.build(text = text, imageBytes = imageBytes, password = password)
        val decoded = PayloadParser.parse(payload, password)

        assertThat(decoded.text).isEqualTo(text)
        assertThat(decoded.imageBytes).isEqualTo(imageBytes)
    }

    @Test
    fun `unicode text payload roundtrip`() {
        val text = "日本語テスト 🎭 Ŧħíŝ ïŝ ũñíçödé"
        val payload = PayloadBuilder.build(text = text, imageBytes = null, password = password)
        val decoded = PayloadParser.parse(payload, password)

        assertThat(decoded.text).isEqualTo(text)
    }

    @Test
    fun `empty text with image treats as image-only`() {
        val imageBytes = ByteArray(100) { 0xAB.toByte() }
        val payload = PayloadBuilder.build(text = "", imageBytes = imageBytes, password = password)
        val decoded = PayloadParser.parse(payload, password)

        assertThat(decoded.text).isNull()
        assertThat(decoded.imageBytes).isEqualTo(imageBytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `build with no text and no image throws`() {
        PayloadBuilder.build(text = null, imageBytes = null, password = password)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `build with empty text and null image throws`() {
        PayloadBuilder.build(text = "", imageBytes = null, password = password)
    }

    @Test(expected = InvalidPayloadException::class)
    fun `parse with invalid magic header throws`() {
        val garbage = ByteArray(100) { 0xFF.toByte() }
        PayloadParser.parse(garbage, password)
    }

    @Test(expected = InvalidPayloadException::class)
    fun `parse with too-short data throws`() {
        PayloadParser.parse(byteArrayOf(1, 2, 3), password)
    }

    @Test(expected = WrongPasswordException::class)
    fun `parse with wrong password throws`() {
        val payload = PayloadBuilder.build(text = "secret", imageBytes = null, password = "correct")
        PayloadParser.parse(payload, "wrong")
    }

    @Test
    fun `payload header is exactly 13 bytes`() {
        assertThat(PayloadBuilder.HEADER_SIZE).isEqualTo(13)
    }

    @Test
    fun `large text payload roundtrip`() {
        val text = "A".repeat(10_000)
        val payload = PayloadBuilder.build(text = text, imageBytes = null, password = password)
        val decoded = PayloadParser.parse(payload, password)

        assertThat(decoded.text).isEqualTo(text)
    }
}
