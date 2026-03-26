package com.ghostgramlabs.ghostmask.crypto

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object CompressionService {

    fun compress(input: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(input) }
        return output.toByteArray()
    }

    fun decompress(input: ByteArray): ByteArray {
        GZIPInputStream(ByteArrayInputStream(input)).use { gzip ->
            return gzip.readBytes()
        }
    }
}
