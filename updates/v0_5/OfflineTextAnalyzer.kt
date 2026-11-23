package com.mlbb.assistant

import android.graphics.BitmapFactory
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.TimeUnit

/** OCR local. Nao identifica icones nem atribui itens a adversarios. */
class OfflineTextAnalyzer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val items = listOf(
        "Antique Cuirass", "Arcane Boots", "Ares Belt", "Athena's Shield",
        "Azure Blade", "Berserker's Fury", "Black Ice Shield", "Blade Armor",
        "Blade of Despair", "Blade of the Heptaseas", "Blood Wings", "Book of Sages",
        "Boots", "Brute Force Breastplate", "Chastise Pauldron", "Clock of Destiny",
        "Concentrated Energy", "Corrosion Scythe", "Cursed Helmet", "Dagger",
        "Demon Hunter Sword", "Demon Shoes", "Divine Glaive", "Dominance Ice",
        "Dreadnaught Armor", "Elegant Gem", "Enchanted Talisman", "Endless Battle",
        "Fleeting Time", "Fury Hammer", "Genius Wand", "Glowing Wand",
        "Golden Staff", "Great Dragon Spear", "Guardian Helmet", "Haas' Claws",
        "Healing Necklace", "Hero's Ring", "Holy Crystal", "Hunter Strike",
        "Ice Queen Wand", "Immortality", "Iron Hunting Bow", "Javelin",
        "Knife", "Leather Jerkin", "Legion Sword", "Lightning Truncheon",
        "Magic Blade", "Magic Necklace", "Magic Resist Cloak", "Magic Shoes",
        "Magic Wand", "Malefic Gun", "Malefic Roar", "Molten Essence",
        "Mystery Codex", "Mystic Container", "Ogre Tomahawk", "Oracle",
        "Power Crystal", "Queen's Wings", "Radiant Armor", "Rapid Boots",
        "Silence Robe", "Sky Piercer", "Starlium Scythe", "Steel Legplates",
        "Swift Boots", "Swift Crossbow", "Thunder Belt", "Tome of Evil",
        "Tough Boots", "Vampire Mallet", "Vitality Crystal", "War Axe",
        "Warrior Boots", "Wind of Nature", "Windtalker", "Winter Crown", "Wishing Lantern"
    )

    fun analyze(jpeg: ByteArray, heroHint: String): AnalysisResult {
        val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
            ?: return unavailable("Quadro de captura inválido")
        try {
            val raw = Tasks.await(
                recognizer.process(InputImage.fromBitmap(bitmap, 0)), 12L, TimeUnit.SECONDS
            ).text
            val heroes = HeroCatalog.heroes.filter { raw.contains(it, ignoreCase = true) }
            val itemNames = items.filter { raw.contains(it, ignoreCase = true) }
                .filterNot { shortName ->
                    items.any { longer -> longer.length > shortName.length &&
                        longer.contains(shortName, ignoreCase = true) &&
                        raw.contains(longer, ignoreCase = true) }
                }
            val hero = HeroCatalog.find(heroHint)
            val tip = if (hero == null) null else
                HeroCatalog.itemSuggestions(hero, false, false, false).firstOrNull()
            val state = if (heroes.isEmpty() && itemNames.isEmpty())
                "Nenhum nome de herói ou item legível nesta captura"
            else "Texto identificado: " + heroes.size + " heróis • " + itemNames.size + " itens"
            return AnalysisResult(
                status = state, item = tip?.item,
                reason = if (tip != null) tip.reason + " (dica de classe, não build inimiga validada)"
                    else "Selecione seu herói em Dicas de Build; texto isolado não confirma quem possui um item.",
                detections = heroes.size + itemNames.size,
                source = "OCR local • sem envio de imagem",
                hero = hero, enemies = 0, items = itemNames.size,
                visionStatus = "Heróis nomeados: " + heroes.take(4).joinToString(", ").ifEmpty { "nenhum" } +
                    " • Itens nomeados: " + itemNames.take(4).joinToString(", ").ifEmpty { "nenhum" }
            )
        } catch (error: Exception) {
            return unavailable("Leitura local indisponível: " + error.javaClass.simpleName)
        } finally { bitmap.recycle() }
    }

    private fun unavailable(message: String) = AnalysisResult(
        status = message, item = null,
        reason = "Abra a loja, o placar ou a seleção de heróis para tentar ler informações visíveis.",
        detections = 0, source = "OCR local", hero = null, enemies = 0,
        items = 0, visionStatus = "Sem identificação confiável"
    )
    fun close() { recognizer.close() }
}
