package com.mlbb.assistant

import android.content.Context
import android.graphics.BitmapFactory
import android.os.SystemClock
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.TimeUnit

/**
 * Analisa imagem de tela autorizada localmente. Distingue texto legível de
 * evidências visuais de ícones. NUNCA atribui item ao inimigo só porque
 * o nome foi encontrado no OCR da loja.
 */
class OfflineTextAnalyzer(context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val matcher = VideoIconMatcher(context)
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private var recentEnemies = emptyList<String>()
    private var recentEnemyItems = emptyList<String>()
    private var lastVisualAt = 0L
    private var lastScreen = ""
    private val expiresMs = 90_000L
    private val itemNames = listOf(
        "Antique Cuirass", "Arcane Boots", "Athena's Shield", "Berserker's Fury",
        "Blade Armor", "Blade of Despair", "Brute Force Breastplate", "Corrosion Scythe",
        "Demon Hunter Sword", "Divine Glaive", "Dominance Ice", "Enchanted Talisman",
        "Endless Battle", "Genius Wand", "Glowing Wand", "Golden Staff", "Holy Crystal",
        "Hunter Strike", "Immortality", "Malefic Gun", "Malefic Roar", "Oracle",
        "Queen's Wings", "Radiant Armor", "Sea Halberd", "Sky Piercer", "Thunder Belt",
        "War Axe", "Wind of Nature", "Windtalker", "Winter Crown"
    )

    fun analyze(jpeg: ByteArray, heroHint: String): AnalysisResult {
        val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
            ?: return unavailable("Imagem capturada inválida")
        try {
            val text = Tasks.await(
                recognizer.process(InputImage.fromBitmap(bitmap, 0)),
                12L, TimeUnit.SECONDS
            ).text
            val normalized = text.lowercase()
            val ended = listOf("defeat", "victory", "derrota", "vitória", "vitoria").any { it in normalized }
            if (ended) {
                clearMatch()
                return AnalysisResult(
                    status = "Partida encerrada: placar final não gera sugestões de compra",
                    item = null, reason = "As informações dessa partida foram descartadas.",
                    detections = 0, source = "OCR local • placar final", hero = null,
                    enemies = 0, items = 0, visionStatus = "Aguardando outra partida"
                )
            }
            val scoreboard = listOf("estatísticas", "estatisticas", "statistics", "atributos",
                "scoreboard", "desempenho da equipe").any { it in normalized }
            val draft = listOf("seleção de heróis", "selecionar herói", "escolha de heróis",
                "hero selection", "pick a hero", "ban hero").any { it in normalized }
            val shop = listOf("equipamentos", "equipment", "loja", "shop", "recomendado").any { it in normalized }
            val screen = when {
                scoreboard -> "Placar durante a partida"
                draft -> "Seleção de heróis"
                shop -> "Loja de itens"
                else -> "Gameplay ou tela desconhecida"
            }
            val now = SystemClock.elapsedRealtime()
            val catalogWords = HeroCatalog.heroes.filter { full ->
                text.split('\n').any { line ->
                    val compact = line.trim()
                    compact.equals(full, ignoreCase = true) ||
                    compact.startsWith(full + " ", ignoreCase = true) ||
                    compact.endsWith(" " + full, ignoreCase = true)
                }
            }
            val itemsAsText = itemNames.filter { item ->
                text.split('\n').any { line -> line.trim().equals(item, ignoreCase = true) }
            }
            var freshIcons = ScoreboardIcons()
            if (scoreboard) {
                // Testa duas geometrias distintas; só aceita ao menos dois matches
                // por conjunto. Skin, idioma e proporção podem impedir detecção.
                val a = matcher.probeScoreboard(bitmap, false)
                val b = matcher.probeScoreboard(bitmap, true)
                val scoreA = a.enemyHeroes.size + a.enemyItems.size
                val scoreB = b.enemyHeroes.size + b.enemyItems.size
                freshIcons = if (scoreB > scoreA) b else a
                if (freshIcons.enemyHeroes.isNotEmpty() || freshIcons.enemyItems.isNotEmpty()) {
                    if (freshIcons.enemyHeroes.isNotEmpty()) recentEnemies = freshIcons.enemyHeroes
                    if (freshIcons.enemyItems.isNotEmpty()) recentEnemyItems = freshIcons.enemyItems
                    lastVisualAt = now
                }
            }
            if (lastVisualAt != 0L && now - lastVisualAt > expiresMs) {
                recentEnemies = emptyList()
                recentEnemyItems = emptyList()
                lastVisualAt = 0L
            }
            lastScreen = screen
            val hero = HeroCatalog.find(heroHint.ifBlank { prefs.getString("hero_hint", "") ?: "" })
            val advice = LiveBuildEngine.recommend(hero, recentEnemies, recentEnemyItems)
            // Seleção preliminar por função, sem fingir counters detectados.
            val lane = prefs.getString("lane_hint", "Selva") ?: "Selva"
            val draftSuggestions = if (draft) HeroCatalog.suggest(lane, "", "", "").take(3) else emptyList()
            val pickText = draftSuggestions.map { it.hero }.joinToString(" / ")

            val observed = recentEnemies.size + recentEnemyItems.size
            val age = if (lastVisualAt != 0L) ((now - lastVisualAt) / 1000).toInt() else null
            val state = when {
                scoreboard && observed > 0 -> "Placar: ícones candidatos reconhecidos"
                scoreboard -> "Placar detectado, sem ícones confiáveis"
                draft -> "Seleção detectada; reconhecimento experimental"
                shop -> "Loja detectada; itens escritos não significam itens comprados"
                else -> "Analisando tela — abra o placar para detectar builds"
            }
            val samples = buildString {
                append("Referências locais: ${matcher.referenceCount()} • Tela: $screen")
                append("\nInimigos candidatos: ")
                append(recentEnemies.take(5).joinToString(", ").ifEmpty { "não confirmados" })
                append("\nEquipamentos no lado inimigo: ")
                append(recentEnemyItems.take(6).joinToString(", ").ifEmpty { "não confirmados" })
                append("\n")
                append(if (age == null) "Sem observação visual válida" else "Última observação: há ${age}s")
                append("\n")
                append(advice.context)
                if (advice.alternatives.isNotEmpty()) append("\nAlternativas: " + advice.alternatives)
                if (shop && itemsAsText.isNotEmpty()) append("\nItens escritos: " + itemsAsText.take(3).joinToString(", "))
                if (draft && catalogWords.isNotEmpty()) append("\nHeróis escritos: " + catalogWords.take(3).joinToString(", "))
            }
            return AnalysisResult(
                status = if (draft) "Seleção de heróis: sugestões preliminares" else state,
                item = if (draft && draftSuggestions.isNotEmpty()) pickText else advice.item,
                reason = if (draft && draftSuggestions.isNotEmpty())
                    "Pré-seleção por função. Aliados, inimigos e banimentos não confirmados."
                else advice.reason,
                detections = observed,
                source = "Detecção local experimental • não validada em partida real",
                hero = hero, enemies = recentEnemies.size, items = recentEnemyItems.size,
                visionStatus = samples
            )
        } catch (error: Exception) {
            return unavailable("Leitura local falhou: " + error.javaClass.simpleName)
        } finally {
            bitmap.recycle()
        }
    }
    private fun clearMatch() {
        recentEnemies = emptyList()
        recentEnemyItems = emptyList()
        lastVisualAt = 0L
    }
    private fun unavailable(message: String) = AnalysisResult(
        status = message, item = null,
        reason = "Verifique captura Android, abra o placar e selecione seu herói se necessário.",
        detections = 0, source = "Análise local", hero = null, enemies = 0,
        items = 0, visionStatus = "Sem leitura confiável"
    )
    fun close() { recognizer.close(); clearMatch() }
}
