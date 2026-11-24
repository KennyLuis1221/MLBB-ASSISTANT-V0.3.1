package com.mlbb.assistant

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import kotlin.math.abs

internal class EmptySlotReference(private val aspect: Double, private val colors: ByteArray) {
    fun matches(bitmap: Bitmap, x: Int, y: Int, width: Int): Boolean {
        if (abs(bitmap.width.toDouble() / bitmap.height / aspect - 1) > .025) return false
        val half = (width * .6).toInt().coerceAtLeast(5) / 2
        val candidate = sample(bitmap, x - half, y - half, x + half, y + half) ?: return false
        val average = colors.indices.sumOf { abs((colors[it].toInt() and 255) - (candidate[it].toInt() and 255)) }.toDouble() / colors.size
        return average <= 7.0
    }

    fun encode() = "$aspect|${Base64.encodeToString(colors, Base64.NO_WRAP)}"

    companion object {
        fun create(bitmap: Bitmap, first: BoardCalibration.Point, second: BoardCalibration.Point): EmptySlotReference? {
            val left = (minOf(first.x, second.x) * bitmap.width).toInt()
            val right = (maxOf(first.x, second.x) * bitmap.width).toInt()
            val top = (minOf(first.y, second.y) * bitmap.height).toInt()
            val bottom = (maxOf(first.y, second.y) * bitmap.height).toInt()
            if (right - left > bitmap.width * .06 || bottom - top > bitmap.height * .10) return null
            val colors = sample(bitmap, left, top, right, bottom) ?: return null
            return EmptySlotReference(bitmap.width.toDouble() / bitmap.height, colors)
        }

        fun decode(encoded: String?): EmptySlotReference? = runCatching {
            val parts = encoded.orEmpty().split('|')
            require(parts.size == 2)
            val aspect = parts[0].toDouble()
            val colors = Base64.decode(parts[1], Base64.NO_WRAP)
            require(aspect in 1.3..3.0 && colors.size == 48)
            EmptySlotReference(aspect, colors)
        }.getOrNull()

        private fun sample(bitmap: Bitmap, left: Int, top: Int, right: Int, bottom: Int): ByteArray? {
            if (left < 0 || top < 0 || right > bitmap.width || bottom > bitmap.height || right-left < 4 || bottom-top < 4) return null
            val out = ByteArray(48)
            var offset = 0
            for (row in 0..3) for (column in 0..3) {
                val pixel = bitmap.getPixel(left + (2 * column + 1) * (right - left) / 8,
                    top + (2 * row + 1) * (bottom - top) / 8)
                out[offset++] = Color.red(pixel).toByte()
                out[offset++] = Color.green(pixel).toByte()
                out[offset++] = Color.blue(pixel).toByte()
            }
            return out
        }
    }
}
