package com.mlbb.assistant
import org.junit.Test
import org.junit.Assert.*
import java.io.File
class Panel16Test{
 @Test fun groupsAndCoverage(){
  val a=Panel16.Row("Hanzo","Aliado",listOf("Dagger","Dagger"),"2000","1/0/1",5)
  val b=Panel16.Row("Tigreal","Inimigo",listOf("Blade Armor"),"2500",null,6)
  val u=Panel16.Row("Layla","Incerto",emptyList(),null,null,null)
  val result=Panel16.groups(listOf(a,b,u),"Hanzo")
  assertEquals(listOf(a),result[0].rows)
  assertEquals(4,result[1].expected)
  assertEquals(listOf(b),result[2].rows)
  assertEquals(listOf(u),result[3].rows)
  assertTrue(Panel16.coverage(a).contains("2/6"))
  assertTrue(Panel16.coverage(u).contains("estatísticas não identificadas"))
  assertTrue(Panel16.groups(listOf(a,a.copy(side="Inimigo")),a.hero).all{it.rows.isEmpty()})
  assertNull(Panel16.groups(listOf(a),null)[1].expected)
 }
 @Test fun observedThreats(){
  val d=listOf(File("src/main/assets/strategy13.tsv"),File("app/src/main/assets/strategy13.tsv")).first{it.isFile}.inputStream().use{GameData13.install(it)}
  val a=Panel16.Row("Hanzo","Aliado",emptyList(),"2000",null,5)
  val b=Panel16.Row("Tigreal","Inimigo",listOf("Blade Armor","Oracle"),"2500",null,6)
  val t=Panel16.threats(d,b,a)
  assertTrue(t.any{it.contains("Blade Armor")});assertTrue(t.any{it.contains("Oracle")})
  assertTrue(t.any{it.contains("+500")});assertTrue(t.any{it.contains("+1")})
  assertTrue(Panel16.threats(d,b.copy(items=emptyList(),gold=null,level=null),a).isEmpty())
 }
}
