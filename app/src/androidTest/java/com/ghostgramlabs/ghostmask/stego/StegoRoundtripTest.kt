package com.ghostgramlabs.ghostmask.stego

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for full steganography roundtrip.
 * Uses real Bitmap operations, so must run on device/emulator.
 */
@RunWith(AndroidJUnit4::class)
class StegoRoundtripTest {

    private fun createTestBitmap(width: Int = 200, height: Int = 200): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Fill with varied pixel data
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, Color.rgb((x * 3) % 256, (y * 7) % 256, ((x + y) * 5) % 256))
            }
        }
        return bitmap
    }

    @Test
    fun textOnly_roundtrip() {
        val coverBitmap = createTestBitmap()
        val text = "Hello GhostMask! This is a text-only secret."
        val password = "test123"

        // Encode
        val payload = PayloadBuilder.build(text = text, imageBytes = null, password = password)
        val encoded = StegoEncoder.encode(coverBitmap, payload)

        // Decode
        val extracted = StegoDecoder.decode(encoded)
        val decoded = PayloadParser.parse(extracted, password)

        assertThat(decoded.text).isEqualTo(text)
        assertThat(decoded.imageBytes).isNull()
    }

    @Test
    fun imageOnly_roundtrip() {
        val coverBitmap = createTestBitmap()
        val imageBytes = ByteArray(500) { (it % 256).toByte() }
        val password = "img-pass"

        val payload = PayloadBuilder.build(text = null, imageBytes = imageBytes, password = password)
        val encoded = StegoEncoder.encode(coverBitmap, payload)

        val extracted = StegoDecoder.decode(encoded)
        val decoded = PayloadParser.parse(extracted, password)

        assertThat(decoded.text).isNull()
        assertThat(decoded.imageBytes).isEqualTo(imageBytes)
    }

    @Test
    fun textAndImage_roundtrip() {
        val coverBitmap = createTestBitmap()
        val text = "Both text and image present"
        val imageBytes = ByteArray(300) { (it % 256).toByte() }
        val password = "both-pass"

        val payload = PayloadBuilder.build(text = text, imageBytes = imageBytes, password = password)
        val encoded = StegoEncoder.encode(coverBitmap, payload)

        val extracted = StegoDecoder.decode(encoded)
        val decoded = PayloadParser.parse(extracted, password)

        assertThat(decoded.text).isEqualTo(text)
        assertThat(decoded.imageBytes).isEqualTo(imageBytes)
    }

    @Test(expected = WrongPasswordException::class)
    fun wrongPassword_throwsException() {
        val coverBitmap = createTestBitmap()
        val payload = PayloadBuilder.build(text = "secret", imageBytes = null, password = "correct")
        val encoded = StegoEncoder.encode(coverBitmap, payload)

        val extracted = StegoDecoder.decode(encoded)
        PayloadParser.parse(extracted, "wrong")
    }

    @Test(expected = PayloadTooLargeException::class)
    fun tooLargePayload_throwsException() {
        // Tiny 5x5 image — capacity is very limited
        val tinyBitmap = createTestBitmap(5, 5)
        val hugePayload = ByteArray(1000) { 0x42 }

        // This should throw during encoding
        StegoEncoder.encode(tinyBitmap, hugePayload)
    }

    @Test
    fun capacityCheck_matches() {
        val bitmap = createTestBitmap(100, 100)
        val capacity = CapacityCalculator.calculateCapacity(bitmap.width, bitmap.height)

        // Should be able to encode payload up to capacity minus header overhead
        val testPayload = ByteArray(capacity)
        val encoded = StegoEncoder.encode(bitmap, testPayload)
        val extracted = StegoDecoder.decode(encoded)

        assertThat(extracted).isEqualTo(testPayload)
    }
}
