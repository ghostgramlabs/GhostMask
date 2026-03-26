package com.ghostgramlabs.ghostmask.stego

import com.ghostgramlabs.ghostmask.domain.model.EmbeddingMetadata
import com.ghostgramlabs.ghostmask.domain.model.GhostMeta
import com.ghostgramlabs.ghostmask.domain.model.GhostPayload
import com.ghostgramlabs.ghostmask.domain.model.PayloadType
import com.ghostgramlabs.ghostmask.domain.model.RevealFlags
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PayloadBuilderParserTest {

    private val password = "test-password-123"

    private fun payload(
        text: String? = null,
        imageBytes: ByteArray? = null,
        compressionEnabled: Boolean = false
    ): GhostPayload {
        val payloadType = when {
            text != null && imageBytes != null -> PayloadType.BOTH
            text != null -> PayloadType.TEXT
            imageBytes != null -> PayloadType.IMAGE
            else -> throw IllegalArgumentException("Need content")
        }
        return GhostPayload(
            meta = GhostMeta(
                payloadType = payloadType,
                createdAtEpochMs = 123456789L,
                textLength = text?.toByteArray()?.size ?: 0,
                imageLength = imageBytes?.size ?: 0,
                compressionEnabled = compressionEnabled,
                expiryEpochMs = 987654321L,
                flags = RevealFlags(
                    secureView = true,
                    blockScreenshots = true,
                    clearOnClose = true,
                    selfDestructSeconds = 30,
                    oneTimeReveal = true,
                    requireBiometric = true
                ),
                senderLabel = "Ghost",
                embedding = EmbeddingMetadata(lsbBitsUsed = 2)
            ),
            textBytes = text?.toByteArray(),
            imageBytes = imageBytes
        )
    }

    @Test
    fun `text-only payload roundtrip`() {
        val encoded = PayloadBuilder.build(payload(text = "This is a hidden message"), password)
        val decoded = PayloadParser.parse(encoded, password)

        assertThat(decoded.text).isEqualTo("This is a hidden message")
        assertThat(decoded.imageBytes).isNull()
        assertThat(decoded.payload.meta.payloadType).isEqualTo(PayloadType.TEXT)
    }

    @Test
    fun `image-only payload roundtrip`() {
        val imageBytes = ByteArray(500) { (it % 256).toByte() }
        val encoded = PayloadBuilder.build(payload(imageBytes = imageBytes), password)
        val decoded = PayloadParser.parse(encoded, password)

        assertThat(decoded.text).isNull()
        assertThat(decoded.imageBytes).isEqualTo(imageBytes)
    }

    @Test
    fun `text and image payload roundtrip`() {
        val text = "Secret message with image"
        val imageBytes = ByteArray(1000) { (it % 256).toByte() }
        val encoded = PayloadBuilder.build(payload(text = text, imageBytes = imageBytes), password)
        val decoded = PayloadParser.parse(encoded, password)

        assertThat(decoded.text).isEqualTo(text)
        assertThat(decoded.imageBytes).isEqualTo(imageBytes)
    }

    @Test
    fun `unicode text payload roundtrip`() {
        val text = "日本語テスト 🎭 This is unicode"
        val encoded = PayloadBuilder.build(payload(text = text), password)
        val decoded = PayloadParser.parse(encoded, password)

        assertThat(decoded.text).isEqualTo(text)
    }

    @Test
    fun `compressed payload roundtrip`() {
        val text = "A".repeat(5_000)
        val encoded = PayloadBuilder.build(payload(text = text, compressionEnabled = true), password)
        val decoded = PayloadParser.parse(encoded, password)

        assertThat(decoded.text).isEqualTo(text)
        assertThat(decoded.payload.meta.compressionEnabled).isTrue()
    }

    @Test(expected = InvalidPayloadException::class)
    fun `parse with invalid magic header throws`() {
        PayloadParser.parse(ByteArray(100) { 0xFF.toByte() }, password)
    }

    @Test(expected = InvalidPayloadException::class)
    fun `parse with too-short data throws`() {
        PayloadParser.parse(byteArrayOf(1, 2, 3), password)
    }

    @Test(expected = WrongPasswordException::class)
    fun `parse with wrong password throws`() {
        val encoded = PayloadBuilder.build(payload(text = "secret"), "correct")
        PayloadParser.parse(encoded, "wrong")
    }

    @Test
    fun `payload header is exactly 9 bytes`() {
        assertThat(PayloadBuilder.HEADER_SIZE).isEqualTo(9)
    }

    @Test
    fun `metadata survives encryption roundtrip`() {
        val encoded = PayloadBuilder.build(payload(text = "Secret"), password)
        val decoded = PayloadParser.parse(encoded, password)

        assertThat(decoded.payload.meta.flags.blockScreenshots).isTrue()
        assertThat(decoded.payload.meta.flags.requireBiometric).isTrue()
        assertThat(decoded.payload.meta.embedding.lsbBitsUsed).isEqualTo(2)
        assertThat(decoded.payload.meta.expiryEpochMs).isEqualTo(987654321L)
    }
}
