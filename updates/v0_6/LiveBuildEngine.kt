package com.mlbb.assistant

/** Recomendações de itens guiadas por evidências observadas.
 * Os pesos são heurísticos, não são 'confiança da IA' ou winrates.
 * Não atribui um item a um herói específico se o reconhecimento não forneceu a linha.
 */
object LiveBuildEngine {
    private val physicalDefense = setOf(
        "Antique Cuirass", "Blade Armor", "Brute Force Breastplate",
        "Dominance Ice", "Immortality", "Queen's Wings", "Thunder Belt"
    )
    private val magicDefense = setOf("Athena's Shield", "Radiant Armor", "Oracle")
    private val healingHeroes = setOf(
        "Estes", "Floryn", "Angela", "Uranus", "Ruby", "Rafaela", "Alucard", "Yu Zhong", "Esmeralda"
    )
    private val magicBurstHeroes = setOf(
        "Eudora", "Aurora", "Kadita", "Gusion", "Harley", "Vale", "Kagura"
    )
    private val antiHealOptions = setOf("Sea Halberd", "Dominance Ice", "Glowing Wand")
    private val severePhysicalHeroes = setOf("Saber", "Hayabusa", "Fanny", "Ling", "Lancelot")
    data class Advice(val item: String?, val reason: String, val context: String, val alternatives: String)

    fun recommend(hero: String?, opponents: List<String>, enemyItems: List<String>,
                  ownedItems: List<String> = emptyList()): Advice {
        val canonical = HeroCatalog.find(hero ?: "")
            ?: return Advice(null,
                "Herói do jogador ainda não confirmado. Escolha seu herói em Dicas de Build.",
                "Sem dados suficientes", "")
        val roles = HeroCatalog.rolesOf(canonical)
        val isMage = "Mago" in roles && "Atirador" !in roles && "Lutador" !in roles
        val isTank = "Tanque" in roles
        val isSupport = "Suporte" in roles && "Mago" !in roles
        val isPhysicalCarry = !isTank && !isSupport && !isMage
        val owned = ownedItems.map { it.lowercase() }.toSet()
        val armorEvidence = enemyItems.count { it in physicalDefense }
        val magicDefenseEvidence = enemyItems.count { it in magicDefense }
        val healingEvidence = opponents.count { it in healingHeroes }
        val burstEvidence = opponents.count { it in magicBurstHeroes }
        val physicalThreat = opponents.count { it in severePhysicalHeroes }
        data class Candidate(val item: String, val score: Int, val reason: String)
        val candidates = mutableListOf<Candidate>()
        fun offer(item: String, score: Int, reason: String) {
            if (item.lowercase() !in owned) candidates.add(Candidate(item, score, reason))
        }

        // Risco de erro de classificação: usar sugestões situacionais apenas com evidência.
        if (healingEvidence > 0 && antiHealOptions.none { it.lowercase() in owned }) {
            val anti = if (isMage) "Glowing Wand" else if (isTank || isSupport) "Dominance Ice" else "Sea Halberd"
            offer(anti, 13 + healingEvidence, "Inimigos vistos com potencial de cura: " +
                opponents.filter { it in healingHeroes }.joinToString(", ") + ".")
        }
        if (armorEvidence >= 2 && isPhysicalCarry) {
            offer("Malefic Roar", 18 + armorEvidence,
                "Foram observados $armorEvidence equipamentos ligados à defesa física no lado inimigo.")
        }
        if (magicDefenseEvidence >= 2 && isMage) {
            offer("Divine Glaive", 18 + magicDefenseEvidence,
                "Foram observados $magicDefenseEvidence itens de defesa mágica adversária.")
        }
        if (burstEvidence > 0) {
            if (isMage) offer("Winter Crown", 11 + burstEvidence,
                "Possível ameaça de explosão mágica: " + opponents.filter { it in magicBurstHeroes }.joinToString(", ") + ".")
            else offer("Athena's Shield", 11 + burstEvidence,
                "Possível ameaça de explosão mágica: " + opponents.filter { it in magicBurstHeroes }.joinToString(", ") + ".")
        }
        if (physicalThreat >= 2 && (isTank || isSupport)) {
            offer("Antique Cuirass", 10 + physicalThreat,
                "Múltiplos assassinos físicos identificados. Considerar proteção física.")
        }

        // Opção genérica sempre identificada como tal, nunca como resposta a um item não observado.
        val generic = when {
            isMage -> "Holy Crystal"
            isTank || isSupport -> "Immortality"
            "Atirador" in roles -> "Windtalker"
            "Lutador" in roles -> "War Axe"
            "Assassino" in roles -> "Blade of Despair"
            else -> "Immortality"
        }
        offer(generic, 1, "Opção genérica da classe; validar sinergia e patch antes de comprar.")
        val ranked = candidates.sortedWith(compareByDescending<Candidate> { it.score }.thenBy { it.item })
        val best = ranked.firstOrNull()
        val context = buildList {
            if (armorEvidence > 0) add("Defesa física visível: $armorEvidence")
            if (magicDefenseEvidence > 0) add("Defesa mágica visível: $magicDefenseEvidence")
            if (healingEvidence > 0) add("Cura potencial: $healingEvidence")
            if (burstEvidence > 0) add("Burst mágico potencial: $burstEvidence")
        }.joinToString(" • ").ifBlank { "Ainda sem ameaças confirmadas" }
        val alternatives = ranked.drop(1).take(2).joinToString(" / ") { it.item }
        return Advice(
            item = best?.item,
            reason = (best?.reason ?: "Nenhum item compatível confirmado.") +
                " Sugestão experimental, não compra obrigatória.",
            context = context,
            alternatives = alternatives
        )
    }
}
