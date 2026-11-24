package com.mlbb.assistant
import org.junit.Assert.*
import org.junit.Test
class Review081Test {
 @Test fun catalogFacts(){
  assertTrue(ItemReference.all.size>=80)
  assertEquals(ItemReference.all.size,ItemReference.byName.size)
  assertEquals(2100,ItemReference.byName["Chastise Pauldron"]!!.price)
  assertEquals(3,ItemReference.byName["Chastise Pauldron"]!!.tier)
  assertEquals("Haas' Claws",StrategicEngine.canonicalItem("Haas's Claws"))
  assertEquals("Demon Shoes",StrategicEngine.canonicalItem("Demon Boots"))
 }
 @Test fun aggregateArmor(){
  val x=StrategicEngine.recommend(StrategicEngine.State("Hanzo",listOf("Swift Boots","Demon Hunter Sword"),enemyItemsAggregate=listOf("Blade Armor","Antique Cuirass")))
  assertEquals("Malefic Roar",x.next)
 }
 @Test fun aggregateMagic(){
  val x=StrategicEngine.recommend(StrategicEngine.State("Karina",listOf("Arcane Boots","Starlium Scythe"),enemyItemsAggregate=listOf("Athena's Shield","Radiant Armor")))
  assertEquals("Divine Glaive",x.next)
 }
 @Test fun inventoryTier(){
  val x=StrategicEngine.recommend(StrategicEngine.State("Tigreal",listOf("Tough Boots","Chastise Pauldron","Athena's Shield","Radiant Armor","Immortality","Oracle")))
  assertNull(x.next)
 }
 @Test fun individualProfiles(){
  for(h in listOf("Miya","Moskov","Bruno","Angela")){
   val x=StrategicEngine.recommend(StrategicEngine.State(h))
   assertTrue(h,x.profile.contains("individual"))
   assertEquals(h,6,x.plan.size)
  }
 }
 @Test fun parsing(){
  assertEquals("2/3/7",ObservationParser.kda(listOf("2","/","3","/","7")))
  assertNull(ObservationParser.kda(listOf("2/3/7","4/2/1")))
  assertEquals("3456",ObservationParser.gold(listOf("3,456","2/3/7")))
  assertNull(ObservationParser.gold(listOf("3456","7890")))
  assertEquals("3600",ObservationParser.compatibleGold("3456","3600"))
  assertNull(ObservationParser.compatibleGold("3456","1000"))
  assertEquals(615,ObservationParser.clockSeconds("10:15"))
  assertNull(ObservationParser.clockSeconds("10:77"))
 }
 @Test fun stalePriceNotAffordability(){
  val r=StrategicEngine.recommend(StrategicEngine.State("Hanzo",budget=10000))
  assertTrue(r.purchase!!.contains("não confirmado"))
  assertTrue(r.purchase!!.contains("referência"))
 }
}
