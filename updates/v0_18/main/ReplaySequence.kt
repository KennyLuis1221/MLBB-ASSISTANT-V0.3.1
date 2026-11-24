package com.mlbb.assistant

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.nio.ByteBuffer
import java.security.MessageDigest

/** Reads one bounded sequence without keeping source images in memory. */
internal class ReplaySequence(private val resolver: ContentResolver) {
    companion object {
        const val MAX_FRAMES = 300
        private const val STEP_MS = 1000L
        private const val MAX_EDGE = 1920

        fun contentHash(bitmap: Bitmap): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val pixels = IntArray(bitmap.width)
            val row = ByteBuffer.allocate(bitmap.width * Int.SIZE_BYTES)
            val integers = row.asIntBuffer()
            digest.update(ByteBuffer.allocate(8).putInt(bitmap.width).putInt(bitmap.height).array())
            for (y in 0 until bitmap.height) {
                bitmap.getPixels(pixels, 0, bitmap.width, 0, y, bitmap.width, 1)
                integers.clear()
                integers.put(pixels)
                digest.update(row.array())
            }
            return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        }
    }

    data class Frame(val index: Int, val timeMs: Long, val bitmap: Bitmap)

    fun read(uris: List<Uri>, consume: (Frame) -> Unit) {
        require(uris.size in 1..MAX_FRAMES) { "Selecione de 1 a $MAX_FRAMES quadros" }
        val videos = uris.filter { resolver.getType(it)?.startsWith("video/") == true }
        if (videos.isNotEmpty()) {
            require(uris.size == 1) { "Selecione um vídeo ou uma sequência de imagens" }
            video(videos.single(), consume)
            return
        }
        for ((i, uri) in uris.withIndex()) {
            val bitmap = image(uri)
            try { consume(Frame(i + 1, i * STEP_MS, bitmap)) } finally { bitmap.recycle() }
        }
    }

    private fun image(uri: Uri): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        requireNotNull(resolver.openInputStream(uri)).use { BitmapFactory.decodeStream(it, null, options) }
        require(options.outWidth > 0 && options.outHeight > 0) { "Imagem ilegível" }
        var sample = 1
        while (maxOf(options.outWidth, options.outHeight) / sample > MAX_EDGE * 2) sample *= 2
        val decoded = requireNotNull(resolver.openInputStream(uri)).use {
            requireNotNull(BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }))
        }
        val (width, height) = outputSize(decoded.width, decoded.height)
        return if (width == decoded.width && height == decoded.height) decoded else {
            Bitmap.createScaledBitmap(decoded, width, height, true).also { decoded.recycle() }
        }
    }

    private fun video(uri: Uri, consume: (Frame) -> Unit) {
        val retriever = MediaMetadataRetriever()
        try {
            requireNotNull(resolver.openAssetFileDescriptor(uri, "r")).use { asset ->
                if (asset.length >= 0) retriever.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
                else retriever.setDataSource(asset.fileDescriptor)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
                require(duration != null && duration in 1..(STEP_MS * MAX_FRAMES) && width != null && height != null && width > 0 && height > 0) {
                    "Use um vídeo de até cinco minutos, com metadados de duração e resolução"
                }
                val (w, h) = if (rotation == 90 || rotation == 270) outputSize(height, width) else outputSize(width, height)
                for ((i, time) in (0 until duration step STEP_MS).withIndex()) {
                    val frame = requireNotNull(retriever.getScaledFrameAtTime(
                        time * 1000, MediaMetadataRetriever.OPTION_CLOSEST, w, h
                    )) { "Não foi possível ler o quadro ${i + 1}" }
                    try { consume(Frame(i + 1, time, frame)) } finally { frame.recycle() }
                }
            }
        } finally {
            retriever.release()
        }
    }

    private fun outputSize(width: Int, height: Int): Pair<Int, Int> {
        val ratio = (MAX_EDGE.toDouble() / maxOf(width, height)).coerceAtMost(1.0)
        return (width * ratio).toInt().coerceAtLeast(1) to (height * ratio).toInt().coerceAtLeast(1)
    }
}
