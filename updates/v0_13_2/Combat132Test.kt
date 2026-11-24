package com.mlbb.assistant
import org.junit.Test
import org.junit.Assert.*
import java.io.File
class Combat132Test {
 private fun db()=listOf(File("src/main/assets/strategy13.tsv"),File("app/src/main/assets/strategy13.tsv")).first{it.isFile}.inputStream().use{GameData13.install(it)}
 @Test fun formula(){
  assertEquals(500.0,Combat132.mitigate(1000.0,Combat132.Damage.PHYSICAL,120.0),1e-8)
  assertEquals(2000.0,Combat132.mitigate(1000.0,Combat132.Damage.MAGIC,-500.0),1e-8)
  assertEquals(50.0,Combat132.effectiveDefense(100.0,40.0,10.0),1e-8)
  assertEquals(1000.0,Combat132.mitigate(1000.0,Combat132.Damage.TRUE,999.0),1e-8)
  assertEquals(600.0,Combat132.heal(1000.0,lifebane=true),1e-8)
  assertEquals(200.0,Combat132.healthAfter(500.0,1000.0,300.0).healthDamage,1e-8)
 }
 @Test fun attributes(){
  val d=db()
  val a=Combat132.stats(d,"Layla",15,listOf("Blade of Despair"))!!
  val b=Combat132.stats(d,"Layla",15,emptyList())!!
  assertEquals(160.0,a.physical!!-b.physical!!,1e-8)
  val c=Combat132.stats(d,"Layla",15,listOf("Golden Staff","Berserker's Fury"))!!
  assertEquals(0.0,c.crit,1e-8)
  assertEquals(40.0,c.speedBonus,1e-8)
  assertEquals(30.0,Combat132.stats(d,"Layla",15,listOf("Malefic Gun","Malefic Roar"))!!.pPct,1e-8)
  assertNull(Combat132.stats(d,"Layla",16,emptyList()))
  assertNull(Combat132.stats(d,"Layla",15,listOf("unknown")))
 }
 @Test fun skillEvents(){
  val d=db()
  val a=Combat132.stats(d,"Eudora",15,listOf("Holy Crystal"))!!
  val e=Combat132.events(d,"Eudora").first{it.rawBase==275.0&&it.type==Combat132.Damage.MAGIC}
  assertEquals(440.0,Combat132.raw(e,a)!!,1e-8)
  val target=Combat132.stats(d,"Tigreal",15,emptyList())!!
  val x=Combat132.combo(listOf(Combat132.Hit(100.0,Combat132.Damage.TRUE,3)),a,target)!!
  assertEquals(300.0,x.damage,1e-8)
  val count=d.heroes.keys.count{Combat132.events(d,it).isNotEmpty()}
  println("COMBAT132_EVENT_HEROES="+count+" / "+d.heroes.size+" EVENTS="+d.heroes.keys.sumOf{Combat132.events(d,it).size})
  assertTrue(count>80)
 }
 @Test fun teamsAndLoadouts(){
  val d=db()
  val fit=Tactics132.team(d,listOf("Hanzo","Tigreal","Layla","Eudora","Alpha"))
  assertTrue(fit.score>0)
  for(h in d.heroes.keys)assertTrue(Tactics132.loadouts(d,h,null,"all").isNotEmpty())
  for(a in d.heroes.keys)for(b in d.heroes.keys)if(a!=b){
   val x=Tactics132.interaction(d,a,b,false);val y=Tactics132.interaction(d,b,a,false)
   assertEquals(x.score,-y.score,1e-8)
   assertEquals(Tactics132.interaction(d,a,b,true).score,Tactics132.interaction(d,b,a,true).score,1e-8)
  }
 }
 @Test fun everyHeroStats(){
  val d=db();var missing=0
  for(h in d.heroes.keys)for(l in listOf(1,8,15)){
   val a=Combat132.stats(d,h,l,emptyList())!!
   if(a.hp==null||a.physical==null||a.pdef==null||a.mdef==null)missing++
  }
  println("COMBAT132_MISSING_STAT_CASES="+missing+" / 399")
 }
}
