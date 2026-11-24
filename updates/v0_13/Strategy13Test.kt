package com.mlbb.assistant
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Strategy13Test {
 @Test fun integratedDataAndScenarios(){
  val file=listOf(File("src/main/assets/strategy13.tsv"),File("app/src/main/assets/strategy13.tsv"),File("android-app/app/src/main/assets/strategy13.tsv")).first{it.isFile}
  val d=file.inputStream().use{GameData13.install(it)}
  assertEquals(133,d.heroes.size)
  assertEquals(133,d.skills.size)
  assertTrue(d.items.size>=100)
  assertTrue(d.builds.size>=120)
  assertTrue(d.counters.isNotEmpty())
  assertTrue(d.heroes.keys.containsAll(HeroCatalog.heroes))
  var measured=0;var estimated=0;var synergies=0;var syEst=0
  for(a in d.heroes.keys)for(b in d.heroes.keys)if(a!=b){
   val m=Strategy13.matchup(d,a,b)
   if(m.measured!=null)measured++ else estimated++
   val y=Strategy13.matchup(d,a,b,true)
   if(y.measured!=null)synergies++ else syEst++
  }
  assertEquals(17556,measured+estimated)
  assertEquals(17556,synergies+syEst)
  for(h in d.heroes.keys){
   val s=StrategicEngine.State(h)
   val r=Strategy13.recommend(s)
   assertNotNull(h,r.next);assertEquals(h,6,r.plan.size);assertEquals(h,6,r.plan.distinct().size)
   assertEquals(r,Strategy13.recommend(s))
  }
  val a=Strategy13.buy(d,"Swift Boots",listOf("Boots"),300,true)
  assertEquals(470,a.remaining)
  assertNotNull(a.component)
  val no=Strategy13.buy(d,"Swift Boots",listOf("Boots"),10000,false)
  assertNull(no.remaining)
  val selected=Strategy13.draft("Roam","Tigreal","Atlas","Diggie","Lolita")
  assertTrue(selected.none{it.hero in setOf("Tigreal","Atlas","Lolita")})
  assertTrue(selected.all{ "Roam" in d.heroes.getValue(it.hero).lanes})
  val r=Strategy13.recommend(StrategicEngine.State("Hanzo",listOf("Swift Boots","Demon Hunter Sword","Corrosion Scythe","Golden Staff","Malefic Roar","Immortality"),inventoryKnown=true))
  assertNull(r.next)
  val rnd=kotlin.random.Random(1313)
  repeat(300){
   val hero=d.heroes.keys.random(rnd)
   val enemies=d.heroes.keys.filter{it!=hero}.shuffled(rnd).take(5).map{StrategicEngine.Player(it,listOf("Blade Armor","Radiant Armor").shuffled(rnd).take(rnd.nextInt(3)),rnd.nextInt(1000,18000))}
   val s=StrategicEngine.State(hero,enemies=enemies,ownGold=8000,minute=rnd.nextInt(1,22))
   val p=Strategy13.recommend(s)
   assertNotNull(p.next);assertTrue(p.plan.size<=6);assertEquals(p.plan.size,p.plan.distinct().size)
  }
  println("V013_COUNTS heroes="+d.heroes.size+" items="+d.items.size+" skills="+d.skills.values.sumOf{it.size}+" buildHeroes="+d.builds.size+" measuredCounters="+measured+" estimatedCounters="+estimated+" measuredSynergies="+synergies+" estimatedSynergies="+syEst)
 }
}
