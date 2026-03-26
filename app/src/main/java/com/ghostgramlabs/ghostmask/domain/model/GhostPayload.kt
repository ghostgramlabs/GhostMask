package com.ghostgramlabs.ghostmask.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GhostPayload(
    val meta: GhostMeta,
    val textBytes: ByteArray? = null,
    val imageBytes: ByteArray? = null
)

@Serializable
data class GhostMeta(
    val version: Int = CURRENT_VERSION,
    val payloadType: PayloadType,
    val createdAtEpochMs: Long,
    val textLength: Int = 0,
    val imageLength: Int = 0,
    val compressionEnabled: Boolean,
    val expiryEpochMs: Long? = null,
    val flags: RevealFlags = RevealFlags(),
    val senderLabel: String? = null,
    val embedding: EmbeddingMetadata = EmbeddingMetadata()
) {
    companion object {
        const val CURRENT_VERSION = 2
    }
}

@Serializable
enum class PayloadType {
    TEXT,
    IMAGE,
    BOTH
}

@Serializable
data class RevealFlags(
    val secureView: Boolean = false,
    val blockScreenshots: Boolean = false,
    val hideFromRecents: Boolean = false,
    val clearOnClose: Boolean = false,
    val clearOnBackground: Boolean = false,
    val selfDestructSeconds: Int? = null,
    val oneTimeReveal: Boolean = false,
    val requireBiometric: Boolean = false,
    val deleteEncodedAfterReveal: Boolean = false
)

@Serializable
data class EmbeddingMetadata(
    val lsbBitsUsed: Int = 1
)

enum class EmbeddingMode(val lsbBits: Int, val label: String, val warning: String?) {
    STEALTH(1, "Stealth", null),
    BALANCED(2, "Balanced", "Balanced mode increases capacity while keeping the image visually similar."),
    HIGH_CAPACITY(3, "High Capacity", "High capacity mode may slightly alter image quality. Use only if needed.");

    companion object {
        fun fromBits(bits: Int): EmbeddingMode = entries.firstOrNull { it.lsbBits == bits } ?: STEALTH
    }
}
