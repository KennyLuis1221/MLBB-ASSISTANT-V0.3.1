package com.mlbb.assistant

import android.graphics.Bitmap
import android.graphics.Color

internal object FrameContent {
    fun hasDetail(frame: Bitmap): Boolean {
        var minimum = 255
        var maximum = 0
        for (y in 1..12) for (x in 1..20) {
            val color = frame.getPixel(x * frame.width / 21, y * frame.height / 13)
            val luma = (Color.red(color) * 3 + Color.green(color) * 6 + Color.blue(color)) / 10
            minimum = minOf(minimum, luma)
            maximum = maxOf(maximum, luma)
        }
        return maximum - minimum >= 12
    }
}
