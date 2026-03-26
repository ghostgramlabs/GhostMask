package com.ghostgramlabs.ghostmask.crypto

import com.ghostgramlabs.ghostmask.domain.model.GhostPayload
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor

@OptIn(ExperimentalSerializationApi::class)
object PayloadSerializer {

    private val cbor = Cbor {
        ignoreUnknownKeys = true
    }

    fun serialize(payload: GhostPayload): ByteArray = cbor.encodeToByteArray(GhostPayload.serializer(), payload)

    fun deserialize(bytes: ByteArray): GhostPayload = cbor.decodeFromByteArray(GhostPayload.serializer(), bytes)
}
