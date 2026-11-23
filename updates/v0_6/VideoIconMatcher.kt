package com.mlbb.assistant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * Catálogo de referências 3x3 RGB extraídas dos dois vídeos fornecidos pelo usuário.
 * É um classificador experimental por distância de cor, NÃO um modelo treinado/validado
 * de reconhecimento de skins. Exige distância baixa E margem ao segundo colocado;
 * quando incerto retorna null, sem inventar a detecção.
 */
class VideoIconMatcher(context: Context) {
    data class Hit(val name: String, val category: Char, val error: Double)
    private data class Ref(val name: String, val category: Char, val rgb: ByteArray)
    private val refs: List<Ref> = try {
        context.assets.open("icon_sketches.tsv").bufferedReader(Charsets.UTF_8).useLines { seq ->
            seq.mapNotNull { line ->
                val f = line.split('|')
                if (f.size != 3 || f[0].length != 1) null
                else try {
                    val bytes = Base64.decode(f[2], Base64.DEFAULT)
                    if (bytes.size != 27) null else Ref(f[1], f[0][0], bytes)
                } catch (_: Exception) { null }
            }.toList()
        }
    } catch (_: Exception) { emptyList() }

    fun referenceCount() = refs.size

    fun match(bitmap: Bitmap, centerX: Int, centerY: Int, size: Int, kind: Char): Hit? {
        if (refs.isEmpty() || size < 13) return null
        val x = centerX - size / 2
        val y = centerY - size / 2
        if (x < 0 || y < 0 || x + size > bitmap.width || y + size > bitmap.height) return null
        val patch = Bitmap.createBitmap(bitmap, x, y, size, size)
        val tiny = Bitmap.createScaledBitmap(patch, 3, 3, true)
        if (patch !== tiny) patch.recycle()
        val pix = IntArray(9)
        tiny.getPixels(pix, 0, 3, 0, 0, 3, 3)
        tiny.recycle()
        val source = IntArray(27)
        for (i in pix.indices) {
            source[i * 3] = Color.red(pix[i])
            source[i * 3 + 1] = Color.green(pix[i])
            source[i * 3 + 2] = Color.blue(pix[i])
        }
        var best: Ref? = null
        var bestError = Double.POSITIVE_INFINITY
        var runnerUp = Double.POSITIVE_INFINITY
        for (ref in refs) {
            if (ref.category != kind) continue
            var squared = 0.0
            for (i in 0 until 27) {
                val delta = source[i] - (ref.rgb[i].toInt() and 255)
                squared += delta * delta
            }
            val rms = sqrt(squared / 27.0)
            if (rms < bestError) {
                runnerUp = bestError
                bestError = rms
                best = ref
            } else if (rms < runnerUp) runnerUp = rms
        }
        // Margens conservadoras; não são porcentagens de precisão calibradas.
        if (best == null || bestError > 27.0 || runnerUp - bestError < 7.5) return null
        return Hit(best.name, best.category, bestError)
    }

    /**
     * Geometrias iniciais: placar largo e placar sobreposto.
     * Não extrapola para linhas fora da zona da tabela; apenas resultados com
     * duas ou mais detecções concordantes são utilizados como evidência.
     */
    fun probeScoreboard(bitmap: Bitmap, overlay: Boolean): ScoreboardIcons {
        val w = bitmap.width
        val h = bitmap.height
        val reds = linkedSetOf<String>()
        val items = linkedSetOf<String>()
        val blues = linkedSetOf<String>()
        val rows = if (overlay)
            doubleArrayOf(.35, .43, .51, .59, .67)
        else doubleArrayOf(.265, .37, .475, .58, .685)
        val blueX = if (overlay) .135 else .15
        val redX = if (overlay) .72 else .845
        val itemStart = if (overlay) .535 else .634
        val itemStep = if (overlay) .025 else .027
        val heroSize = (w * .043).toInt().coerceIn(22, 100)
        val itemSize = (w * .023).toInt().coerceIn(19, 80)
        for (yRatio in rows) {
            val yy = (h * yRatio).toInt()
            val red = match(bitmap, (w * redX).toInt(), yy, heroSize, 'h')
            val blue = match(bitmap, (w * blueX).toInt(), yy, heroSize, 'h')
            if (red != null) reds.add(red.name)
            if (blue != null) blues.add(blue.name)
            for (slot in 0..5) {
                val ix = (w * (itemStart + slot * itemStep)).toInt()
                val found = match(bitmap, ix, yy, itemSize, 'i')
                if (found != null) items.add(found.name)
            }
        }
        return ScoreboardIcons(
            enemyHeroes = if (reds.size >= 2) reds.toList() else emptyList(),
            alliedHeroes = if (blues.size >= 2) blues.toList() else emptyList(),
            enemyItems = if (items.size >= 2) items.toList() else emptyList()
        )
    }
}

data class ScoreboardIcons(
    val enemyHeroes: List<String> = emptyList(),
    val alliedHeroes: List<String> = emptyList(),
    val enemyItems: List<String> = emptyList()
)
