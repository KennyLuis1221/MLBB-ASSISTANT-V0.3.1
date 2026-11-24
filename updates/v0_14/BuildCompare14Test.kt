package com.mlbb.assistant
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class BuildCompare14Test{
 private fun db()=listOf(File("src/main/assets/strategy13.tsv"),File("app/src/main/assets/strategy13.tsv")).first{it.isFile}.inputStream().use{GameData13.install(it)}
 @Test fun compare(){
  val d=db();val target=listOf(StrategicEngine.Player("Tigreal",level=15))
  val a=BuildCompare14.metrics(d,"Layla",15,emptyList(),target)!!
  val b=BuildCompare14.metrics(d,"Layla",15,listOf("Blade of Despair"),target)!!
  assertTrue(b.basicHit!!>a.basicHit!!)
  assertTrue(BuildCompare14.relative(d,"Layla",a,b)>0)
  assertFalse(BuildCompare14.legal(d,listOf("Swift Boots","Tough Boots")))
  assertFalse(BuildCompare14.legal(d,listOf("Golden Staff","Berserker's Fury")))
  assertFalse(BuildCompare14.legal(d,listOf("Malefic Gun","Malefic Roar")))
  val r=BuildCompare14.rank(d,"Layla",15,listOf(emptyList(),listOf("Blade of Despair")),target)
  assertEquals("Blade of Despair",r.first().items.single())
 }
 @Test fun swaps(){
  val d=db();val target=listOf(StrategicEngine.Player("Tigreal",level=15))
  val stock=listOf("Swift Boots","War Axe","Hunter Strike","Endless Battle","Sea Halberd","Immortality")
  val pool=listOf("Blade of Despair","Malefic Roar","Demon Hunter Sword")
  assertTrue(BuildCompare14.swaps(d,"Layla",15,stock,pool,emptyMap(),5000,target).isEmpty())
  assertTrue(BuildCompare14.swaps(d,"Layla",15,stock,pool,mapOf(1 to 1000),null,target).isEmpty())
  val r=BuildCompare14.swaps(d,"Layla",15,stock,pool,mapOf(1 to 1000,2 to 1000),5000,target)
  assertTrue(r.all{it.netCost<=5000&&it.slots.size in 1..2&&it.score>0})
 }
}
