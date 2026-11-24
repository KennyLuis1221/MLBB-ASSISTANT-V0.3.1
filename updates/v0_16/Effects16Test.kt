package com.mlbb.assistant
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class Effects16Test {
 private fun db()=listOf(File("src/main/assets/strategy13.tsv"),File("app/src/main/assets/strategy13.tsv")).first{it.isFile}.inputStream().use{GameData13.install(it)}
 @Test fun targetDependentSkills(){
  val d=db()
  val a=Combat132.stats(d,"Balmond",15,emptyList())!!
  val e=Combat132.events(d,"Balmond").first{v->v.terms.any{it.attribute=="of the target's lost hp"}}
  assertNull(Combat132.raw(e,a))
  val low=Combat132.raw(e,a,targetLostHp=1000.0)!!
  val high=Combat132.raw(e,a,targetLostHp=2000.0)!!
  assertEquals(300.0,high-low,1e-8)
  val l=Combat132.stats(d,"Lunox",15,emptyList())!!
  val le=Combat132.events(d,"Lunox").first{v->v.terms.any{it.attribute=="target's hp"}}
  assertNull(Combat132.raw(le,l))
  assertEquals(70.0,Combat132.raw(le,l,targetHp=4000.0)!!-Combat132.raw(le,l,targetHp=2000.0)!!,1e-8)
  val g=Combat132.stats(d,"Gloo",15,listOf("Guardian Helmet"))!!
  val ge=Combat132.events(d,"Gloo").first{v->v.terms.any{it.attribute=="extra max hp"}}
  assertNotNull(Combat132.raw(ge,g))
  assertEquals(230.0+.11*(g.hp!!-g.baseHp!!),Combat132.raw(ge,g)!!,1e-8)
 }
 @Test fun explicitEffects(){
  val d=db()
  val base=Combat132.stats(d,"Eudora",15,listOf("Holy Crystal","Blood Wings"))!!
  val r=Effects16.apply(d,"Eudora",base,Effects16.Inputs(35.0,null,true))!!
  assertEquals(base.magic*1.35,r.stats.magic,1e-8)
  assertEquals(800+r.stats.magic,r.shield!!,1e-8)
  assertNull(Effects16.apply(d,"Eudora",base,Effects16.Inputs(99.0)))
  val b=Combat132.stats(d,"Balmond",15,listOf("War Axe"))!!
  val full=Effects16.apply(d,"Balmond",b,Effects16.Inputs(warAxeStacks=6))!!
  assertEquals(72.0,full.stats.physical!!-b.physical!!,1e-8)
  assertEquals(.1,full.warAxeTrueFraction!!,1e-8)
  assertNull(Effects16.apply(d,"Balmond",b,Effects16.Inputs(warAxeStacks=7)))
  assertEquals(80.0,Effects16.demonHunterRaw(1000.0,false)!!,1e-8)
  assertEquals(60.0,Effects16.demonHunterRaw(1000.0,true)!!,1e-8)
  assertEquals(35.0,Effects16.demonHunterRecovery(15,true)!!,1e-8)
 }
}
