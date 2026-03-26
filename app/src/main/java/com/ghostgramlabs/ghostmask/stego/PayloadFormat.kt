package com.ghostgramlabs.ghostmask.stego

import com.ghostgramlabs.ghostmask.crypto.CompressionService
import com.ghostgramlabs.ghostmask.crypto.PayloadSerializer
import com.ghostgramlabs.ghostmask.domain.model.GhostPayload
import java.nio.ByteBuffer
import java.nio.ByteOrder

object PayloadBuilder {

    private val MAGIC = byteArrayOf('G'.code.toByte(), 'M'.code.toByte(), 'K'.code.toByte(), '1'.code.toByte())
    private const val VERSION: Byte = 0x02
    const val HEADER_SIZE = 4 + 1 + 4

    fun build(payload: GhostPayload, password: String): ByteArray {
        val serialized = PayloadSerializer.serialize(payload)
        val prepared = if (payload.meta.compressionEnabled) {
            CompressionService.compress(serialized)
        } else {
            serialized
        }

        val decryptedEnvelope = byteArrayOf(if (payload.meta.compressionEnabled) 1 else 0) + prepared
        val encrypted = CryptoEngine.encrypt(decryptedEnvelope, password).toByteArray()

        return ByteBuffer.allocate(HEADER_SIZE + encrypted.size)
            .order(ByteOrder.BIG_ENDIAN)
            .put(MAGIC)
            .put(VERSION)
            .putInt(encrypted.size)
            .put(encrypted)
            .array()
    }

    fun hasMagicPrefix(payload: ByteArray): Boolean {
        if (payload.size < MAGIC.size) return false
        return payload.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
    }
}

object PayloadParser {

    private val MAGIC = byteArrayOf('G'.code.toByte(), 'M'.code.toByte(), 'K'.code.toByte(), '1'.code.toByte())

    fun parse(payload: ByteArray, password: String): DecodedPayload {
        if (payload.size < PayloadBuilder.HEADER_SIZE) {
            throw InvalidPayloadException("Payload too short to contain a valid header")
        }

        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN)
        val magic = ByteArray(4)
        buffer.get(magic)
        if (!magic.contentEquals(MAGIC)) {
            throw InvalidPayloadException("This image does not contain a GhostMask secret.")
        }

        val version = buffer.get()
        if (version != 0x02.toByte()) {
            throw InvalidPayloadException("Unsupported GhostMask version: $version")
        }

        val encryptedLength = buffer.getInt()
        if (encryptedLength <= 0 || encryptedLength > buffer.remaining()) {
            throw InvalidPayloadException("Invalid encrypted payload length.")
        }

        val encryptedBytes = ByteArray(encryptedLength)
        buffer.get(encryptedBytes)

        val encryptedPackage = EncryptedPackage.fromByteArray(encryptedBytes)
        val decryptedEnvelope = CryptoEngine.decrypt(encryptedPackage, password)
        if (decryptedEnvelope.isEmpty()) {
            throw InvalidPayloadException("Hidden payload is empty.")
        }

        val compressionEnabled = decryptedEnvelope.first().toInt() == 1
        val contentBytes = decryptedEnvelope.copyOfRange(1, decryptedEnvelope.size)
        val serialized = if (compressionEnabled) {
            CompressionService.decompress(contentBytes)
        } else {
            contentBytes
        }

        val ghostPayload = PayloadSerializer.deserialize(serialized)
        return DecodedPayload(
            payload = ghostPayload,
            text = ghostPayload.textBytes?.toString(Charsets.UTF_8),
            imageBytes = ghostPayload.imageBytes
        )
    }
}

data class DecodedPayload(
    val payload: GhostPayload,
    val text: String?,
    val imageBytes: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecodedPayload) return false
        return payload == other.payload &&
            text == other.text &&
            imageBytes.contentEquals(other.imageBytes)
    }

    override fun hashCode(): Int {
        var result = payload.hashCode()
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        return result
    }
}

class InvalidPayloadException(message: String) : Exception(message)
