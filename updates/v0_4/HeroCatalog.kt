package com.mlbb.assistant

/**
 * Catalogo offline de herois, agrupado por classes. Referencia de nomes/roles:
 * mobilelegends.com/hero/list e mlbbhub.com/heroes (consulta 2026-11-23).
 * Cobertura de catalogo NAO significa que counters/builds especificos foram validados.
 */
object HeroCatalog {
    private val groups: Map<String, String> = linkedMapOf(
        "Tanque" to "Akai, Alice, Atlas, Barats, Baxia, Belerick, Carmilla, Chip, Edith, Esmeralda, Franco, Fredrinn, Gatotkaca, Gloo, Grock, Hilda, Hylos, Johnson, Khufra, Lolita, Masha, Minotaur, Tigreal, Uranus",
        "Lutador" to "Aldous, Alpha, Alucard, Argus, Arlott, Aulus, Badang, Balmond, Bane, Barats, Benedetta, Chou, Cici, Dyrroth, Fredrinn, Freya, Gatotkaca, Grock, Guinevere, Hilda, Jawhead, Julian, Kalea, Khaleed, Lapu-Lapu, Leomord, Lukas, Martis, Masha, Minsitthar, Paquito, Phoveus, Roger, Ruby, Silvanna, Sora, Sun, Suyou, Terizla, Thamuz, X.Borg, Yin, Yu Zhong, Zilong",
        "Assassino" to "Aamon, Alucard, Arlott, Benedetta, Fanny, Gusion, Hanzo, Harley, Hayabusa, Helcurt, Hirara, Joy, Julian, Kadita, Karina, Lancelot, Lesley, Ling, Mathilda, Natalia, Nolan, Paquito, Saber, Selena, Sora, Suyou, Yi Sun-shin, Yin, Zilong",
        "Mago" to "Alice, Aurora, Bane, Cecilion, Chang'e, Cyclops, Esmeralda, Eudora, Faramis, Gord, Harith, Harley, Kadita, Kagura, Kimmy, Lunox, Luo Yi, Lylia, Nana, Novaria, Odette, Pharsa, Selena, Vale, Valentina, Valir, Vexana, Xavier, Yve, Zetian, Zhask, Zhuxin",
        "Atirador" to "Beatrix, Brody, Bruno, Claude, Clint, Edith, Granger, Hanabi, Irithel, Ixia, Karrie, Kimmy, Layla, Lesley, Melissa, Miya, Moskov, Natan, Obsidia, Popol and Kupa, Roger, Wanwan, Yi Sun-shin",
        "Suporte" to "Angela, Carmilla, Chip, Diggie, Estes, Faramis, Floryn, Johnson, Kaja, Kalea, Lolita, Marcel, Mathilda, Minotaur, Rafaela"
    )
    val heroes: List<String> = groups.values.flatMap { it.split(", ").map(String::trim) }.distinct().sorted()
    private val roles: Map<String, Set<String>> = heroes.associateWith { hero ->
        groups.filterValues { it.split(", ").contains(hero) }.keys.toSet()
    }
    fun rolesOf(hero: String): Set<String> {
        val canonical = heroes.firstOrNull { it.equals(hero.trim(), ignoreCase = true) } ?: return emptySet()
        return roles[canonical] ?: emptySet()
    }
    fun find(hero: String): String? = heroes.firstOrNull { it.equals(hero.trim(), ignoreCase = true) }

    private val laneRoles = mapOf(
        "Selva" to setOf("Assassino", "Lutador"),
        "EXP" to setOf("Lutador", "Tanque"),
        "Meio" to setOf("Mago"),
        "Ouro" to setOf("Atirador"),
        "Roam" to setOf("Suporte", "Tanque")
    )
    private fun parseHeroes(text: String): List<String> =
        text.split(',', ';', '\n').mapNotNull { find(it) }.distinct().take(10)

    data class Pick(val hero: String, val reason: String)
    fun suggest(lane: String, alliesInput: String, enemiesInput: String, favoritesInput: String): List<Pick> {
        val allies = parseHeroes(alliesInput)
        val enemies = parseHeroes(enemiesInput)
        val excluded = (allies + enemies).toSet()
        val favorites = parseHeroes(favoritesInput).toSet()
        val needed = laneRoles[lane] ?: setOf("Lutador", "Mago", "Atirador", "Tanque", "Assassino", "Suporte")
        val allyRoles = allies.flatMap { rolesOf(it) }.toSet()
        val enemyAssassins = enemies.count { "Assassino" in rolesOf(it) }
        val candidateScore = heroes.asSequence()
            .filter { it !in excluded && rolesOf(it).any(needed::contains) }
            .map { h ->
                val hRoles = rolesOf(h)
                var score = if (hRoles.any(needed::contains)) 8 else 0
                if (h in favorites) score += 12
                if ("Tanque" in hRoles && "Tanque" !in allyRoles) score += 3
                if ("Suporte" in hRoles && "Suporte" !in allyRoles) score += 2
                if (enemyAssassins >= 2 && ("Tanque" in hRoles || "Lutador" in hRoles)) score += 2
                if ("Mago" in hRoles && "Mago" !in allyRoles) score += 1
                h to score
            }.sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first }).take(3).toList()
        return candidateScore.map { (hero, _) ->
            val rationale = mutableListOf("perfil compatível com " + lane)
            if (hero in favorites) rationale.add("está entre seus favoritos")
            if ("Tanque" in rolesOf(hero) && "Tanque" !in allyRoles) rationale.add("adiciona resistência à equipe")
            if ("Mago" in rolesOf(hero) && "Mago" !in allyRoles) rationale.add("diversifica as classes aliadas")
            Pick(hero, rationale.joinToString("; ") + ".")
        }
    }

    data class ItemTip(val item: String, val reason: String)
    fun itemSuggestions(heroInput: String, armor: Boolean, healing: Boolean, magic: Boolean): List<ItemTip> {
        val hero = find(heroInput) ?: return emptyList()
        val type = rolesOf(hero)
        val magicDamage = "Mago" in type && "Atirador" !in type
        val tanky = "Tanque" in type || "Suporte" in type
        val picks = mutableListOf<ItemTip>()
        if (armor && !magicDamage && !tanky)
            picks.add(ItemTip("Malefic Roar", "Considerar penetração física contra armadura."))
        if (armor && magicDamage)
            picks.add(ItemTip("Divine Glaive", "Considerar penetração mágica contra defesa mágica; armadura física por si só não justifica este item."))
        if (healing) {
            val antiheal = if (tanky) "Dominance Ice" else if (magicDamage) "Glowing Wand" else "Sea Halberd"
            picks.add(ItemTip(antiheal, "Investigar anti-cura conforme tipo de dano e função."))
        }
        if (magic)
            picks.add(ItemTip(if (tanky || "Lutador" in type) "Athena's Shield" else "Rose Gold Meteor",
                "Considerar sobrevivência contra dano mágico explosivo; verificar adequação ao herói."))
        if (picks.isEmpty()) {
            val standard = when {
                "Atirador" in type -> "Malefic Roar"
                "Mago" in type -> "Holy Crystal"
                "Assassino" in type -> "Blade of Despair"
                "Lutador" in type -> "War Axe"
                "Suporte" in type -> "Flask of the Oasis"
                else -> "Immortality"
            }
            picks.add(ItemTip(standard, "Candidato genérico pela classe; não é uma build específica validada."))
        }
        return picks.take(3)
    }
}
