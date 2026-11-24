package com.mlbb.assistant

import java.text.Normalizer
import java.util.Locale

internal enum class ScreenEvidence {
    DRAFT, LOADING, MATCH, SHOP, INVENTORY, SCOREBOARD, DEATH, POST_MATCH, UNKNOWN;

    companion object {
        fun classify(lines: List<String>): ScreenEvidence {
            fun clean(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()
            val text = lines.map(::clean).filter { it.isNotBlank() }
            fun exact(vararg labels: String) = text.any { it in labels }
            fun contains(vararg labels: String) = text.any { line -> labels.any { it in line } }
            return when {
                exact("defeat", "victory", "derrota", "vitoria", "retornar ao lobby", "return to lobby") -> POST_MATCH
                contains("selecao de herois", "selecionar heroi", "escolha de herois", "hero selection", "pick a hero",
                    "ban hero", "banir heroi", "escolha seu heroi", "banimento", "your turn", "sua vez") -> DRAFT
                exact("carregando", "loading", "carregando partida", "loading match") -> LOADING
                contains("ressuscita em", "renasce em", "respawn in") -> DEATH
                exact("placar", "estatisticas", "statistics", "scoreboard", "desempenho da equipe") -> SCOREBOARD
                exact("inventario", "inventory") -> INVENTORY
                exact("equipamentos", "equipment", "loja", "shop") -> SHOP
                text.any { ObservationParser.clockSeconds(it) != null } -> MATCH
                else -> UNKNOWN
            }
        }
    }
}
