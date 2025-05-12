package com.example.multinav.utils

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utility class for handling byte array conversions
 */
object ByteUtils {
    /**
     * Convert a byte array to a hex string for debugging
     */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { byte ->
            String.format("%02X", byte)
        }
    }

    /**
     * Convert two bytes to a short value (little-endian)
     */
    fun bytesToShort(byte1: Byte, byte2: Byte): Short {
        return (((byte2.toInt() and 0xFF) shl 8) or (byte1.toInt() and 0xFF)).toShort()
    }

    /**
     * Convert three bytes to an integer value (little-endian)
     */
    fun bytesToInt(byte1: Byte, byte2: Byte, byte3: Byte): Int {
        return ((byte3.toInt() and 0xFF) shl 16) or ((byte2.toInt() and 0xFF) shl 8) or (byte1.toInt() and 0xFF)
    }

    /**
     * Convert byte array to a list of floats by processing in pairs
     * (Used for custom float encoding)
     */
    fun bytesToFloats(bytes: ByteArray): List<Float> {
        if (bytes.size % 2 != 0) {
            Log.w("ByteUtils", "Byte array length is not a multiple of 2, padding with 0")
            // Pad with a zero byte if the length is odd
            val paddedBytes = bytes + byteArrayOf(0)
            return processBytePairs(paddedBytes)
        }
        return processBytePairs(bytes)
    }

    /**
     * Convert byte array to IEEE 754 floats
     * (Used for standard float encoding)
     */
    fun bytesToIEEEFloats(value: ByteArray): List<Float> {
        val result = mutableListOf<Float>()
        var index = 0

        while (index + 3 < value.size) {
            val bytes = ByteBuffer.wrap(value, index, 4).order(ByteOrder.LITTLE_ENDIAN)
            result.add(bytes.float)
            index += 4
        }

        return result
    }

    /**
     * Process byte array as pairs of bytes to convert to floats
     */
    private fun processBytePairs(bytes: ByteArray): List<Float> {
        val floats = mutableListOf<Float>()
        for (i in bytes.indices step 2) {
            // Ensure we have at least 2 bytes to process
            if (i + 1 < bytes.size) {
                // Combine 2 bytes into a short (16-bit integer)
                val shortValue = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)

                // Convert the short to a float
                val floatValue = shortValue.toFloat()
                floats.add(floatValue)
            }
        }
        return floats
    }
}