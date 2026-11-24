package com.mlbb.assistant

/** Reference prices and tiers, community catalog 2026-11-15 (mlbbhub.com/items).
 * Not live prices or remaining recipe costs. No official patch certification.
 */
object ItemReference {
    const val source="MLBBHub • 15/11/2026 • referência comunitária"
    private val rows="""Antique Cuirass|2170|3
Arcane Boots|720|3
Ares Belt|700|2
Athena's Shield|2150|3
Azure Blade|600|2
Berserker's Fury|2390|3
Black Ice Shield|780|2
Blade Armor|1910|3
Blade of Despair|3010|3
Blade of the Heptaseas|1950|3
Blood Wings|2100|3
Book of Sages|450|1
Boots|250|1
Brute Force Breastplate|2070|3
Chastise Pauldron|2100|3
Clock of Destiny|2030|3
Concentrated Energy|2020|3
Corrosion Scythe|2050|3
Cursed Helmet|1910|3
Dagger|250|1
Demon Shoes|720|3
Demon Hunter Sword|2180|3
Divine Glaive|1970|3
Dominance Ice|2010|3
Dreadnaught Armor|730|2
Elegant Gem|700|2
Enchanted Talisman|2070|3
Endless Battle|2330|3
Exotic Veil|600|2
Expert Gloves|500|1
Feather of Heaven|2030|3
Flask of the Oasis|1850|3
Fleeting Time|2050|3
Fury Hammer|830|2
Genius Wand|2000|3
Glowing Wand|2050|3
Golden Staff|2000|3
Great Dragon Spear|2140|3
Guardian Helmet|2500|3
Haas' Claws|2020|3
Healing Necklace|140|1
Hero's Ring|450|1
Holy Crystal|3000|3
Hunter Strike|2010|3
Ice Queen Wand|2040|3
Immortality|2120|3
Iron Hunting Bow|450|1
Javelin|320|1
Knife|280|1
Leather Jerkin|220|1
Legion Sword|910|2
Lightning Truncheon|2250|3
Magic Blade|1050|2
Magic Shoes|720|3
Magic Necklace|120|1
Magic Resist Cloak|220|1
Magic Wand|820|1
Malefic Gun|2120|3
Malefic Roar|2060|3
Molten Essence|800|2
Mystery Codex|300|1
Mystic Container|500|2
Ogre Tomahawk|650|2
Oracle|1860|3
Power Crystal|220|1
Queen's Wings|2250|3
Radiant Armor|1880|3
Rapid Boots|720|3
Regular Spear|600|2
Rogue Meteor|900|2
Rose Gold Meteor|2030|3
Sea Halberd|2050|3
Silence Robe|1020|2
Sky Piercer|1500|3
Starlium Scythe|2120|3
Steel Legplates|630|2
Swift Boots|720|3
Swift Crossbow|1000|2
Thunder Belt|1820|3
Tome of Evil|950|2
Tough Boots|720|3
Vampire Mallet|400|1
Vitality Crystal|300|1
War Axe|2100|3
Warrior Boots|720|3
Wind of Nature|1910|3
Windtalker|1880|3
Winter Crown|1910|3
Wishing Lantern|2250|3"""
    data class Item(val name:String,val price:Int,val tier:Int)
    val all=rows.lines().map{val a=it.split("|");Item(a[0],a[1].toInt(),a[2].toInt())}
    val byName=all.associateBy{it.name}
    fun normalize(s:String)=s.trim().replace('’','\'').let{
        when(it.lowercase(java.util.Locale.ROOT)){
            "haas's claws"->"Haas' Claws";"demon boots"->"Demon Shoes";"magic boots"->"Magic Shoes";else->it
        }
    }
}
