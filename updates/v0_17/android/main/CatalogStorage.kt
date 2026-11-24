package com.mlbb.assistant

import android.content.Context
import android.util.AtomicFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.time.LocalDate

internal object CatalogStorage {
    private const val MAX_BYTES = 6_000_000
    private const val NAME = "strategy13.tsv"

    @Synchronized
    fun load(context: Context): GameData13.Snapshot? {
        GameData13.current?.let { return it }
        val file = AtomicFile(File(context.filesDir, NAME))
        return runCatching { file.openRead().use { GameData13.install(it) } }.getOrElse {
            runCatching { context.assets.open(NAME).use { GameData13.install(it) } }.getOrNull()
        }
    }

    @Synchronized
    fun update(context: Context, input: InputStream): String {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            require(bytes.size() + read <= MAX_BYTES)
            bytes.write(buffer, 0, read)
        }
        val content = bytes.toByteArray()
        require(content.size in 1..MAX_BYTES)
        val proposed = GameData13.parse(content.inputStream())
        val bundled = context.assets.open(NAME).use(GameData13::parse)
        val file = AtomicFile(File(context.filesDir, NAME))
        val current = runCatching { file.openRead().use(GameData13::parse) }.getOrDefault(bundled)
        require(proposed.heroes.keys == bundled.heroes.keys && proposed.items.keys.containsAll(bundled.items.keys))
        val date = LocalDate.parse(proposed.date)
        require(date >= LocalDate.parse(current.date))
        val output = file.startWrite()
        try {
            output.write(content)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
        return proposed.date
    }
}
