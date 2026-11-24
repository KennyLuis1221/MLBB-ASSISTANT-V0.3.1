package com.mlbb.assistant
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class Development14Test{
 private fun db()=listOf(File("src/main/assets/strategy13.tsv"),File("app/src/main/assets/strategy13.tsv")).first{it.isFile}.inputStream().use{GameData13.install(it)}
 @Test fun comboCoverage(){
  val d=db()
  assertEquals(133,d.combos.size);assertEquals(266,d.combos.values.sumOf{it.size})
  assertTrue(d.combos.values.flatten().all{it.steps.isNotEmpty()})
  val unknown=d.combos.values.flatten().sumOf{v->v.steps.count{it!="@BASIC"&&it !in d.skills.getValue(v.hero).map{s->s.name}}}
  assertEquals(41,unknown)
  println("COMBO_REFERENCE_COVERAGE=133/133; unmodeled steps="+unknown)
 }
 @Test fun loadoutAttributes(){
  val d=db()
  assertEquals(7,Loadout14.emblems.size);assertEquals(26,Loadout14.talents.size);assertEquals(12,Loadout14.spells.size)
  val l=Loadout14.Selection("Tank",60,listOf("Firmness","Festival of Blood","Brave Smite"),"Flicker")
  val a=Combat132.stats(d,"Tigreal",15,emptyList(),l)!!
  val b=Combat132.stats(d,"Tigreal",15,emptyList())!!
  assertEquals(500.0,a.hp!!-b.hp!!,1e-8);assertEquals(18.0,a.pdef!!-b.pdef!!,1e-8);assertEquals(18.0,a.mdef!!-b.mdef!!,1e-8)
  assertEquals(6.0,a.vamp,1e-8)
  assertNull(Loadout14.resolve(l.copy(level=40)))
  assertNull(Loadout14.resolve(l.copy(talents=listOf("Swift","Firmness","Brave Smite"))))
  assertNull(Loadout14.resolve(l.copy(spell="unknown")))
  assertEquals(380.0,Loadout14.execute(15,1000.0)!!,1e-8)
  assertEquals(1720.0,Loadout14.retribution(15,true)!!,1e-8)
  assertNull(Loadout14.retribution(15,false))
  assertEquals(2100.0,Loadout14.aegis(15)!!,1e-8)
  assertFalse(Loadout14.purifyRemoves(true))
 }
 @Test fun timeline(){
  val d=db();val a=Combat132.stats(d,"Eudora",15,emptyList())!!;val target=Combat132.stats(d,"Tigreal",15,emptyList())!!
  val hp=target.hp!!
  val rows=listOf(
    Timeline14.Action(0.0,Timeline14.Kind.CAST,id="s1",cooldown=5.0,cost=40.0),
    Timeline14.Action(0.0,Timeline14.Kind.SHIELD,amount=100.0,duration=1.0),
    Timeline14.Action(0.0,Timeline14.Kind.HIT,amount=200.0,type=Combat132.Damage.TRUE,id="s1"),
    Timeline14.Action(1.0,Timeline14.Kind.CAST,id="s1",cooldown=5.0,cost=40.0),
    Timeline14.Action(1.0,Timeline14.Kind.HIT,amount=900.0,type=Combat132.Damage.TRUE,id="s1"),
    Timeline14.Action(2.0,Timeline14.Kind.IMMUNE,duration=1.0),
    Timeline14.Action(2.0,Timeline14.Kind.HIT,amount=500.0,type=Combat132.Damage.TRUE),
    Timeline14.Action(3.0,Timeline14.Kind.PERIODIC,amount=50.0,type=Combat132.Damage.TRUE,interval=1.0,count=3),
    Timeline14.Action(6.0,Timeline14.Kind.HEAL,amount=30.0)
  )
  val r=Timeline14.run(a,target,rows,initialResource=100.0,maxResource=100.0)!!
  assertEquals(hp-220,r.hp,1e-8);assertEquals(60.0,r.resource!!,1e-8)
  val dead=Timeline14.run(a,target,listOf(Timeline14.Action(3.0,Timeline14.Kind.HIT,amount=hp+10,type=Combat132.Damage.TRUE)))!!
  assertEquals(3.0,dead.killTime!!,1e-8)
  assertNull(Timeline14.run(a,target,listOf(Timeline14.Action(-1.0,Timeline14.Kind.HIT,amount=10.0))))
 }
 @Test fun banCandidates(){
  val d=db()
  val result=Draft14.bans(d,listOf("Layla"),listOf("Tigreal"),listOf("Hanzo"),listOf("Saber"),"all")
  assertTrue(result.isNotEmpty())
  assertTrue(result.all{it.hero !in listOf("Layla","Tigreal","Hanzo","Saber")&&it.measured+it.estimated==2})
  assertEquals(result,Draft14.bans(d,listOf("Layla"),listOf("Tigreal"),listOf("Hanzo"),listOf("Saber"),"all"))
  assertTrue(Draft14.bans(d,emptyList(),emptyList(),emptyList(),emptyList(),"all").isEmpty())
 }
}
