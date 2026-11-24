package com.mlbb.assistant
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class Team15Test{
 private fun db()=listOf(File("src/main/assets/strategy13.tsv"),File("app/src/main/assets/strategy13.tsv")).first{it.isFile}.inputStream().use{GameData13.install(it)}
 @Test fun assignment(){
  val d=db()
  val team=listOf("Hanzo","Tigreal","Layla","Eudora","Alpha")
  val a=Team15.assign(d,team,mapOf("Jungle" to "Hanzo"))!!
  assertEquals(5,a.byLane.size)
  assertEquals("Hanzo",a.byLane["Jungle"])
  assertTrue(a.unassigned.isEmpty())
  assertNull(Team15.assign(d,team,mapOf("Jungle" to "Eudora")))
 }
 @Test fun completeComposition(){
  val d=db()
  val a=listOf("Hanzo","Tigreal","Layla","Eudora","Alpha")
  val e=listOf("Saber","Atlas","Miya","Kagura","Terizla")
  val r=Team15.report(d,a,e)!!
  assertEquals(25,r.counters.measured+r.counters.estimated)
  assertEquals(10,r.allySynergy.measured+r.allySynergy.estimated)
  assertEquals(10,r.enemySynergy.measured+r.enemySynergy.estimated)
  assertTrue(r.allyConditions.isNotEmpty())
  assertNull(Team15.report(d,a,e.dropLast(1)+"Hanzo"))
  assertTrue(Team15.responses(d,a.dropLast(1),e,emptyList(),"Alpha").isEmpty())
  val picks=Team15.responses(d,a.dropLast(1),e.take(3),listOf("Diggie"),"Alpha")
  assertTrue(picks.all{it.hero !in a+e.take(3)+listOf("Diggie")})
  assertEquals(picks,Team15.responses(d,a.dropLast(1),e.take(3),listOf("Diggie"),"Alpha"))
 }
}
