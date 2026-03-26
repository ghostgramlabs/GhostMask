package com.ghostgramlabs.ghostmask.stego

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ghostgramlabs.ghostmask.domain.model.EmbeddingMetadata
import com.ghostgramlabs.ghostmask.domain.model.GhostMeta
import com.ghostgramlabs.ghostmask.domain.model.GhostPayload
import com.ghostgramlabs.ghostmask.domain.model.PayloadType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StegoRoundtripTest {

    private fun createTestBitmap(width: Int = 200, height: Int = 200): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, Color.rgb((x * 3) % 256, (y * 7) % 256, ((x + y) * 5) % 256))
            }
        }
        return bitmap
    }

    private fun payload(text: String? = null, imageBytes: ByteArray? = null, lsbBits: Int = 1): GhostPayload {
        val type = when {
            text != null && imageBytes != null -> PayloadType.BOTH
            text != null -> PayloadType.TEXT
            imageBytes != null -> PayloadType.IMAGE
            else -> throw IllegalArgumentException("Need content")
        }
        return GhostPayload(
            meta = GhostMeta(
                payloadType = type,
                createdAtEpochMs = 1L,
                compressionEnabled = false,
                embedding = EmbeddingMetadata(lsbBits)
            ),
            textBytes = text?.toByteArray(),
            imageBytes = imageBytes
        )
    }

    @Test
    fun textOnly_roundtrip() {
        val coverBitmap = createTestBitmap()
        val encodedPayload = PayloadBuilder.build(payload(text = "Hello GhostMask! This is a text-only secret."), "test123")
        val encoded = StegoEncoder.encode(coverBitmap, encodedPayload, 1)

        val extracted = StegoDecoder.decodeWithAutoDetect(encoded)
        val decoded = PayloadParser.parse(extracted.payload, "test123")

        assertThat(decoded.text).isEqualTo("Hello GhostMask! This is a text-only secret.")
        assertThat(decoded.imageBytes).isNull()
        assertThat(extracted.lsbBits).isEqualTo(1)
    }

    @Test
    fun imageOnly_roundtrip() {
        val coverBitmap = createTestBitmap()
        val imageBytes = ByteArray(500) { (it % 256).toByte() }
        val encodedPayload = PayloadBuilder.build(payload(imageBytes = imageBytes), "img-pass")
        val encoded = StegoEncoder.encode(coverBitmap, encodedPayload, 1)

        val extracted = StegoDecoder.decodeWithAutoDetect(encoded)
        val decoded = PayloadParser.parse(extracted.payload, "img-pass")

        assertThat(decoded.text).isNull()
        assertThat(decoded.imageBytes).isEqualTo(imageBytes)
    }

    @Test
    fun textAndImage_roundtrip_withTwoBitMode() {
        val coverBitmap = createTestBitmap()
        val imageBytes = ByteArray(300) { (it % 256).toByte() }
        val encodedPayload = PayloadBuilder.build(
            payload(text = "Both text and image present", imageBytes = imageBytes, lsbBits = 2),
            "both-pass"
        )
        val encoded = StegoEncoder.encode(coverBitmap, encodedPayload, 2)

        val extracted = StegoDecoder.decodeWithAutoDetect(encoded)
        val decoded = PayloadParser.parse(extracted.payload, "both-pass")

        assertThat(decoded.text).isEqualTo("Both text and image present")
        assertThat(decoded.imageBytes).isEqualTo(imageBytes)
        assertThat(extracted.lsbBits).isEqualTo(2)
    }

    @Test(expected = WrongPasswordException::class)
    fun wrongPassword_throwsException() {
        val coverBitmap = createTestBitmap()
        val encodedPayload = PayloadBuilder.build(payload(text = "secret"), "correct")
        val encoded = StegoEncoder.encode(coverBitmap, encodedPayload, 1)

        val extracted = StegoDecoder.decodeWithAutoDetect(encoded)
        PayloadParser.parse(extracted.payload, "wrong")
    }

    @Test(expected = PayloadTooLargeException::class)
    fun tooLargePayload_throwsException() {
        StegoEncoder.encode(createTestBitmap(5, 5), ByteArray(1000) { 0x42 }, 1)
    }

    @Test
    fun capacityCheck_matches() {
        val bitmap = createTestBitmap(100, 100)
        val capacity = CapacityCalculator.calculateCapacity(bitmap.width, bitmap.height, 1)
        val testPayload = ByteArray(capacity)
        val encoded = StegoEncoder.encode(bitmap, testPayload, 1)
        val extracted = StegoDecoder.decode(encoded, 1)

        assertThat(extracted).isEqualTo(testPayload)
    }
}
