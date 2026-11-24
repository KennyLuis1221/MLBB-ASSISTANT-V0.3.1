package com.mlbb.assistant
import org.junit.Assert.*
import org.junit.Test
class StrategicEngineTest {
    private fun r(h:String,items:List<String> = emptyList(),foes:List<StrategicEngine.Player> = emptyList())=
        StrategicEngine.recommend(StrategicEngine.State(h,items,foes,inventoryKnown=true))
    @Test fun catalogCoverage(){
        assertEquals(133,HeroCatalog.heroes.size)
        for(h in HeroCatalog.heroes){
            val x=r(h);assertNotNull(h,x.next);assertEquals(h,6,x.plan.size);assertEquals(h,6,x.plan.distinct().size)
        }
    }
    @Test fun contextualArmor(){
        val x=r("Hanzo",listOf("Swift Boots","Demon Hunter Sword"),
          listOf(StrategicEngine.Player("Tigreal",listOf("Blade Armor")),StrategicEngine.Player("Hylos",listOf("Antique Cuirass"))))
        assertEquals("Malefic Roar",x.next)
        assertTrue(x.reason.contains("Defesa física"))
    }
    @Test fun magicNotPhysical(){
        val x=r("Karina",emptyList(),listOf(StrategicEngine.Player("Tigreal",listOf("Blade Armor","Athena's Shield"))))
        assertFalse(x.plan.contains("Malefic Roar"))
    }
    @Test fun fullInventory(){
        val x=r("Hanzo");assertNull(r("Hanzo",x.plan).next)
    }
    @Test fun noRepeatAndBootConflict(){
        val x=r("Hanzo",listOf("Tough Boots","Demon Hunter Sword"));assertFalse(x.plan.contains("Swift Boots"))
        assertNotEquals("Demon Hunter Sword",x.next)
    }
    @Test fun noCritWithGoldenStaff(){
        val x=r("Layla",listOf("Swift Boots","Golden Staff"))
        assertFalse(x.plan.contains("Berserker's Fury"));assertFalse(x.plan.contains("Windtalker"))
    }
    @Test fun noDuplicateAntiheal(){
        val x=r("Hanzo",listOf("Sea Halberd"),listOf(StrategicEngine.Player("Estes")))
        assertFalse(x.plan.contains("Glowing Wand"));assertFalse(x.plan.contains("Dominance Ice"))
    }
    @Test fun budgetNotTotalGold(){
        val x=StrategicEngine.recommend(StrategicEngine.State("Hanzo",ownGold=5000))
        assertTrue(x.purchase!!.contains("não confirmado"))
        val y=StrategicEngine.recommend(StrategicEngine.State("Hanzo",budget=10,prices=mapOf(x.next!! to 100)))
        assertTrue(y.purchase!!.contains("90"))
    }
    @Test fun draftExclusions(){
        val picks=HeroCatalog.suggest("Roam","Tigreal","Atlas","Diggie","Lolita")
        assertTrue(picks.none{it.hero in setOf("Tigreal","Atlas","Lolita")})
        assertTrue(picks.any{it.hero=="Diggie"&&it.reason.contains("controle")})
    }
    @Test fun missingHero(){assertNull(StrategicEngine.recommend(StrategicEngine.State(null)).next)}
    @Test fun deterministicStress(){
        var count=0
        val rnd=kotlin.random.Random(804)
        repeat(300){
            val hero=HeroCatalog.heroes.random(rnd)
            val foes=HeroCatalog.heroes.filter{it!=hero}.shuffled(rnd).take(5).map{h->
                StrategicEngine.Player(h,listOf("Blade Armor","Athena's Shield","Guardian Helmet").shuffled(rnd).take(rnd.nextInt(4)),rnd.nextInt(1000,18000),rnd.nextInt(15),rnd.nextInt(10))
            }
            val state=StrategicEngine.State(hero,enemies=foes,ownGold=7000,minute=rnd.nextInt(1,25))
            val x=StrategicEngine.recommend(state)
            assertEquals(x,StrategicEngine.recommend(state));assertTrue(x.plan.size<=6)
            assertEquals(x.plan.size,x.plan.distinct().size);assertTrue(x.evaluated<=24*50*6)
            count++
        }
        println("300 deterministic synthetic scenarios: "+count)
    }
}
