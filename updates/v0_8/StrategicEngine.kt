package com.mlbb.assistant

/** Pure Kotlin, no Android, network, API, or model. Experimental qualitative weights.
 * No exact item stats, prices, kit coefficients or patch claims are invented.
 */
object StrategicEngine {
    enum class Kind { PHYSICAL, MAGIC, TANK, SUPPORT, MIXED }
    data class Player(val hero:String,val items:List<String> = emptyList(),val totalGold:Int?=null,
                      val kills:Int?=null,val deaths:Int?=null,val assists:Int?=null,val level:Int?=null)
    data class State(val hero:String?,val owned:List<String> = emptyList(),
                     val enemies:List<Player> = emptyList(),val allies:List<Player> = emptyList(),
                     val ownGold:Int?=null,val ownLevel:Int?=null,val minute:Int?=null,
                     val budget:Int?=null,val prices:Map<String,Int> = emptyMap(),
                     val inventoryKnown:Boolean=false,val armorHint:Boolean=false,
                     val healingHint:Boolean=false,val magicHint:Boolean=false)
    data class Choice(val item:String,val score:Double,val why:List<String>)
    data class Result(val next:String?,val reason:String,val alternatives:List<String>,val plan:List<String>,
                      val threat:String,val profile:String,val purchase:String?,val evaluated:Int)
    private val armor=setOf("Antique Cuirass","Blade Armor","Brute Force Breastplate","Dominance Ice","Dreadnaught Armor","Steel Legplates","Leather Jerkin")
    private val resist=setOf("Athena's Shield","Radiant Armor","Oracle","Magic Resist Cloak","Silence Robe")
    private val hp=setOf("Guardian Helmet","Cursed Helmet","Ares Belt","Vitality Crystal")
    private val heals=setOf("Estes","Floryn","Angela","Rafaela","Uranus","Ruby","Alucard","Yu Zhong","Esmeralda","Alpha")
    private val magicBurst=setOf("Eudora","Aurora","Kadita","Gusion","Harley","Vale","Kagura","Aamon","Karina")
    private val physicalBurst=setOf("Saber","Hayabusa","Fanny","Ling","Lancelot","Natalia","Helcurt","Nolan")
    private val basicAttackers=setOf("Hanzo","Layla","Karrie","Claude","Miya","Melissa","Moskov","Irithel","Hanabi","Wanwan","Sun","Zilong","Argus","Freya","Aulus","Bruno")
    private val onHit=setOf("Hanzo","Karrie","Claude","Miya","Melissa","Moskov","Sun")
    private val magic=setOf("Aamon","Alice","Esmeralda","Guinevere","Gusion","Harith","Harley","Joy","Julian","Kadita","Kagura","Karina","Kimmy","Natan","Selena","Silvanna","Gatotkaca")
    private val mixed=setOf("Bane","Edith","Roger","X.Borg","Lesley")
    private val antiHeal=setOf("Sea Halberd","Glowing Wand","Dominance Ice")
    private val boots=setOf("Tough Boots","Swift Boots","Arcane Boots","Demon Shoes","Magic Shoes","Warrior Boots","Rapid Boots","Boots")
    private val crit=setOf("Berserker's Fury","Haas' Claws","Windtalker","Great Dragon Spear","Javelin")
    private val physical=setOf("Demon Hunter Sword","Corrosion Scythe","Golden Staff","Blade of Despair","Malefic Roar","Sea Halberd","Hunter Strike","War Axe","Endless Battle","Berserker's Fury","Haas' Claws","Windtalker","Wind of Nature","Malefic Gun","Great Dragon Spear")
    private val magical=setOf("Genius Wand","Holy Crystal","Divine Glaive","Glowing Wand","Lightning Truncheon","Feather of Heaven","Starlium Scythe","Enchanted Talisman","Ice Queen Wand","Concentrated Energy","Clock of Destiny")
    private val defense=setOf("Athena's Shield","Radiant Armor","Antique Cuirass","Blade Armor","Immortality","Oracle","Dominance Ice","Queen's Wings","Thunder Belt","Winter Crown","Guardian Helmet","Brute Force Breastplate")
    private val components=setOf("Boots","Dagger","Knife","Javelin","Iron Hunting Bow","Swift Crossbow","Fury Hammer","Legion Sword","Vampire Mallet","Leather Jerkin","Steel Legplates","Dreadnaught Armor","Magic Resist Cloak","Silence Robe","Vitality Crystal","Ares Belt","Magic Wand","Mystery Codex","Magic Necklace","Power Crystal","Azure Blade","Elegant Gem","Tome of Evil","Hero's Ring","Black Ice Shield","Mystic Container","Magic Blade","Ogre Tomahawk","Chastise Pauldron","Molten Essence","Healing Necklace")
    // Qualitative profiles from V0.7.2; new archetypes are not claimed individual validated builds.
    private val profiles=mapOf(
        "Hanzo" to "Swift Boots|Demon Hunter Sword|Corrosion Scythe|Golden Staff|Malefic Roar|Immortality",
        "Layla" to "Swift Boots|Berserker's Fury|Windtalker|Haas' Claws|Malefic Roar|Wind of Nature",
        "Kagura" to "Arcane Boots|Genius Wand|Lightning Truncheon|Holy Crystal|Divine Glaive|Winter Crown",
        "Tigreal" to "Tough Boots|Dominance Ice|Antique Cuirass|Athena's Shield|Immortality|Radiant Armor",
        "Karrie" to "Swift Boots|Demon Hunter Sword|Golden Staff|Corrosion Scythe|Wind of Nature|Immortality",
        "Claude" to "Swift Boots|Demon Hunter Sword|Golden Staff|Corrosion Scythe|Wind of Nature|Malefic Roar",
        "Eudora" to "Arcane Boots|Genius Wand|Holy Crystal|Lightning Truncheon|Divine Glaive|Winter Crown",
        "Natan" to "Arcane Boots|Feather of Heaven|Genius Wand|Holy Crystal|Divine Glaive|Winter Crown",
        "Karina" to "Arcane Boots|Starlium Scythe|Genius Wand|Holy Crystal|Divine Glaive|Winter Crown",
        "Alpha" to "Tough Boots|War Axe|Hunter Strike|Queen's Wings|Oracle|Immortality",
        "Estes" to "Demon Shoes|Enchanted Talisman|Fleeting Time|Oracle|Athena's Shield|Immortality",
        "Saber" to "Tough Boots|Hunter Strike|Blade of Despair|Malefic Roar|Endless Battle|Immortality")
    private val bases=mapOf(
        "magic" to "Arcane Boots|Genius Wand|Holy Crystal|Divine Glaive|Glowing Wand|Winter Crown",
        "tank" to "Tough Boots|Dominance Ice|Antique Cuirass|Athena's Shield|Radiant Armor|Immortality",
        "support" to "Demon Shoes|Enchanted Talisman|Flask of the Oasis|Fleeting Time|Oracle|Immortality",
        "marksman" to "Swift Boots|Windtalker|Berserker's Fury|Malefic Roar|Wind of Nature|Immortality",
        "onhit" to "Swift Boots|Demon Hunter Sword|Corrosion Scythe|Golden Staff|Malefic Roar|Immortality",
        "fighter" to "Tough Boots|War Axe|Hunter Strike|Queen's Wings|Oracle|Immortality",
        "assassin" to "Tough Boots|Hunter Strike|Blade of Despair|Malefic Roar|Endless Battle|Immortality")
    val itemNames=(physical+magical+defense+boots+components+antiHeal+profiles.values.flatMap{it.split("|")}+bases.values.flatMap{it.split("|")}).sorted()
    fun canonicalItem(s:String)=itemNames.firstOrNull{it.equals(s.trim(),true)}
    fun parseItems(s:String)=s.split(",",";","\n").mapNotNull(::canonicalItem).take(30)
    fun kind(hero:String):Kind {
        val r=HeroCatalog.rolesOf(hero)
        return when {
            hero in mixed -> Kind.MIXED
            hero in magic -> Kind.MAGIC
            "Suporte" in r && "Mago" !in r && "Tanque" !in r -> Kind.SUPPORT
            "Tanque" in r && "Atirador" !in r -> Kind.TANK
            "Mago" in r -> Kind.MAGIC
            else -> Kind.PHYSICAL
        }
    }
    private fun base(hero:String,k:Kind):List<String> {
        val r=HeroCatalog.rolesOf(hero)
        val key=when{
            k==Kind.MAGIC->"magic";k==Kind.TANK->"tank";k==Kind.SUPPORT->"support"
            hero in onHit->"onhit";"Atirador" in r->"marksman";"Lutador" in r->"fighter";else->"assassin"
        }
        return (profiles[hero]?:bases.getValue(key)).split("|")
    }
    private fun weight(p:Player,s:State):Double{
        val gold=if(p.totalGold!=null&&s.ownGold!=null&&s.ownGold>0)
            ((p.totalGold.toDouble()/s.ownGold)-1).coerceIn(-.35,1.0) else 0.0
        val level=if(p.level!=null&&s.ownLevel!=null) ((p.level-s.ownLevel)*.08).coerceIn(-.25,.4) else 0.0
        val kda=if(p.kills!=null&&p.deaths!=null) ((p.kills-p.deaths)*.035).coerceIn(-.25,.4) else 0.0
        return (1+gold+level+kda).coerceIn(.5,2.5)
    }
    private fun valid(next:String,owned:List<String>,k:Kind):Boolean {
        if(next in owned||next in components)return false
        if(next in boots && owned.any{it in boots&&it!="Boots"})return false
        if((next=="Golden Staff"&&owned.any{it in crit})||(next in crit&&"Golden Staff" in owned))return false
        if(next in antiHeal&&owned.any{it in antiHeal})return false
        if(k==Kind.MAGIC&&next in physical)return false
        if(k==Kind.PHYSICAL&&next in magical)return false
        return true
    }
    fun recommend(s:State):Result{
        val h=HeroCatalog.find(s.hero.orEmpty())?:return Result(null,"Herói desconhecido; aguarde leitura ou selecione no simulador.",emptyList(),emptyList(),"Sem dados","desconhecido",null,0)
        val k=kind(h);val base=base(h,k)
        val profile=if(h in profiles)"Perfil individual experimental" else "Arquétipo experimental por função"
        val owned=s.owned.mapNotNull(::canonicalItem).distinct()
        val complete=owned.filter{it !in components}
        val unknownInventory=s.owned.any{canonicalItem(it)==null}
        val slots=(6-complete.size).coerceAtLeast(0)
        if(slots==0||s.owned.size>=6&&unknownInventory)return Result(null,"Inventário completo ou incerto; não sugerir compra adicional. Substituições exigem confirmação.",emptyList(),complete.take(6),"","$profile",null,0)
        val enemies=s.enemies.filter{HeroCatalog.find(it.hero)!=null}.distinctBy{it.hero}.take(5)
        fun evidence(pred:(Player)->Boolean)=enemies.filter(pred).sumOf{weight(it,s)}
        val a=evidence{p->p.items.any{it in armor}}.coerceAtLeast(if(s.armorHint)2.0 else 0.0)
        val m=evidence{p->p.items.any{it in resist}}
        val heal=evidence{it.hero in heals}.coerceAtLeast(if(s.healingHint)2.0 else 0.0)
        val mb=evidence{it.hero in magicBurst}.coerceAtLeast(if(s.magicHint)2.0 else 0.0)
        val pb=evidence{it.hero in physicalBurst}
        val tank=evidence{kind(it.hero)==Kind.TANK||it.items.any{v->v in hp}}
        val allyAnti=s.allies.any{p->p.items.any{it in antiHeal}}
        val choices=linkedMapOf<String,Choice>()
        fun add(item:String,score:Double,reason:String) {
            if(!valid(item,owned,k))return
            val old=choices[item]
            choices[item]=Choice(item,(old?.score?:0.0)+score,(old?.why?:emptyList())+reason)
        }
        base.forEachIndexed{i,it->add(it,45.0-i*5,"$profile • posição inicial "+(i+1))}
        if(a>0&&(k==Kind.PHYSICAL||k==Kind.MIXED))add("Malefic Roar",18+14*a,"Defesa física observada/informada; penetração candidata")
        if(m>0&&(k==Kind.MAGIC||k==Kind.MIXED))add("Divine Glaive",18+14*m,"Defesa mágica observada; penetração candidata")
        if(heal>0){
            val item=when(k){Kind.MAGIC->"Glowing Wand";Kind.TANK,Kind.SUPPORT->"Dominance Ice";else->"Sea Halberd"}
            add(item, (if(allyAnti)8 else 23)+14*heal,"Potencial de cura adversária"+if(allyAnti)"; aliado já possui anti-cura" else "")
        }
        if(mb>0)add("Athena's Shield",16+16*mb,"Ameaça de explosão mágica; proteção situacional")
        if(pb>0)add(if("Atirador" in HeroCatalog.rolesOf(h))"Wind of Nature" else "Antique Cuirass",16+16*pb,"Ameaça física; proteção situacional")
        if(tank>0&&h in basicAttackers&&k==Kind.PHYSICAL)add("Demon Hunter Sword",16+12*tank,"Alvos potencialmente resistentes; sinergia experimental com ataques")
        if(s.minute!=null&&s.minute<5&&complete.isEmpty())base.firstOrNull()?.let{add(it,20.0,"Fase inicial: mobilidade/desenvolvimento")}
        if(s.minute!=null&&s.minute>=15)add("Immortality",13.0,"Fase avançada: avaliar sobrevivência")
        if(s.ownGold!=null&&enemies.any{it.totalGold!=null&&it.totalGold>s.ownGold*1.4})add("Immortality",15.0,"Desvantagem de ouro total observada; proteção candidata")
        val pool=choices.values.sortedWith(compareByDescending<Choice>{it.score}.thenBy{it.item})
        data class Path(val added:List<String>,val score:Double)
        var beam=listOf(Path(emptyList(),0.0));var evaluated=0
        for(depth in 0 until slots){
            val expanded=beam.flatMap{path->
                pool.filter{valid(it.item,owned+path.added,k)}.map{candidate->
                    evaluated++
                    val synergy=if(h in onHit&&candidate.item in setOf("Golden Staff","Demon Hunter Sword","Corrosion Scythe")&&(owned+path.added).any{it in setOf("Golden Staff","Demon Hunter Sword","Corrosion Scythe")})8.0 else 0.0
                    // Prefer immediately useful purchases, then complementary later slots.
                    Path(path.added+candidate.item,path.score+(candidate.score+synergy)/(1+depth*.3))
                }
            }
            if(expanded.isEmpty())break
            beam=expanded.sortedWith(compareByDescending<Path>{it.score}.thenBy{it.added.joinToString("|")}).take(24)
        }
        val plan=beam.firstOrNull()?.added.orEmpty()
        val next=plan.firstOrNull()
        val item=pool.firstOrNull{it.item==next}
        val price=next?.let{s.prices[it]}?.takeIf{it>0}
        val purchase=when {
            next==null->null
            s.budget==null||price==null->"Preço restante/ouro disponível não confirmado; consulte a loja."
            s.budget>=price->"Compra candidata cabe no orçamento informado."
            else->"Faltam "+(price-s.budget)+" de ouro pelo preço restante informado; confirme componentes na loja."
        }
        val top=enemies.sortedByDescending{weight(it,s)}.take(3).joinToString(", "){it.hero}
        val threats=listOfNotNull(if(a>0)"Armadura" else null,if(m>0)"Resistência mágica" else null,
            if(heal>0)"Cura potencial" else null,if(mb>0)"Explosão mágica" else null,if(pb>0)"Explosão física" else null,
            if(top.isNotBlank())"Prioridade por dados disponíveis: "+top else null).joinToString(" • ").ifBlank{"Sem ameaças confirmadas"}
        val warning=if(!s.inventoryKnown||unknownInventory)" Inventário incompleto: confirme antes de comprar." else ""
        return Result(next,(item?.why?.joinToString("; ")?: "Sem candidato válido")+"."+warning+
            " Pesos experimentais; patch não validado.",pool.filter{it.item!=next}.take(3).map{it.item},
            (complete+plan).take(6),threats,profile,purchase,evaluated)
    }
}
