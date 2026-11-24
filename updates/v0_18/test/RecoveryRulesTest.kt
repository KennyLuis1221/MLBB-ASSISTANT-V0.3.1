package com.mlbb.assistant

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class RecoveryRulesTest {
    private fun data() = listOf(File("src/main/assets/strategy13.tsv"), File("app/src/main/assets/strategy13.tsv"))
        .first { it.isFile }.readText()

    @Test fun catalog_rejects_negative_price_duplicate_ids_and_cycles() {
        val source = data()
        val row = source.lineSequence().first { it.startsWith("I\tDagger\t") }
        fun altered(column: Int, value: String): String {
            val fields = row.split('\t').toMutableList()
            fields[column] = value
            return source.replaceFirst(row, fields.joinToString("\t"))
        }
        for (bad in listOf(altered(3, "-280"), source.replaceFirst(row, "$row\n$row"),
            altered(4, "Dagger"), altered(4, "Componente desconhecido"),
            source.lineSequence().first().let { source.replaceFirst(it, "$it\t999") })) {
            assertThrows(IllegalArgumentException::class.java) { GameData13.parse(bad.byteInputStream()) }
        }
    }

    @Test fun documented_patch_numbers_match_the_shared_effect_constants() {
        val db = GameData13.parse(data().byteInputStream())
        assertTrue(db.items.getValue("Antique Cuirass").passiveText.contains("by 8%"))
        assertTrue(db.items.getValue("Chastise Pauldron").passiveText.contains("to 80%"))
        assertEquals(92.0, Patch15.deterPhysical(100.0, 1, 3)!!, 0.00001)
        assertEquals(80.0, Patch15.chastiseSpeed(100.0, 120.0, true)!!.first, 0.00001)
    }

    @Test fun six_slot_alternatives_only_offer_items_that_consume_a_component() {
        val db = GameData13.install(data().byteInputStream())
        val stock = List(6) { "Magic Necklace" }
        val result = Strategy13.recommend(StrategicEngine.State("Layla", stock, inventoryKnown = true, budget = 200))
        val prohibited = setOf("Windtalker", "Haas' Claws", "Berserker's Fury")
        assertTrue(result.alternatives.none { it in prohibited })
        assertTrue(result.futureAlternatives.containsAll(prohibited))
        (result.alternatives + listOfNotNull(result.next)).forEach {
            assertTrue((Recipe131.quote(db, it, stock, 200)?.consumed ?: 0) > 0)
        }
        assertEquals(false, result.recommendation?.buyableNow)
    }

    @Test fun budget_is_used_only_with_confirmed_inventory() {
        val db = GameData13.install(data().byteInputStream())
        val stock = listOf("Dagger")
        assertNull(Strategy13.buy(db, "Windtalker", stock, 500, false).remaining)
        val quote = Strategy13.buy(db, "Windtalker", stock, 500, true)
        assertTrue(quote.remaining != null && quote.remaining > 500)
        val result = Strategy13.recommend(StrategicEngine.State("Layla", stock, inventoryKnown = true, budget = 50))
        assertNotNull(result.recommendation?.remainingCost)
        assertEquals(false, result.recommendation?.buyableNow)
    }
}
