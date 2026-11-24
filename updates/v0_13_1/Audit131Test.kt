package com.mlbb.assistant
import org.junit.Test
import org.junit.Assert.*
import java.io.File
class Audit131Test {
 private fun db():GameData13.Snapshot=listOf(File("src/main/assets/strategy13.tsv"),File("app/src/main/assets/strategy13.tsv"),File("android-app/app/src/main/assets/strategy13.tsv")).first{it.isFile}.inputStream().use{GameData13.install(it)}
 @Test fun referenceIntegrity(){
  val d=db()
  assertEquals(133,d.heroes.size);assertEquals(133,d.builds.size)
  assertEquals(532,d.skills.values.sumOf{it.size})
  assertTrue(d.skills.values.flatten().all{it.description.isNotBlank()})
  assertTrue(d.items.values.all{i->i.recipe.all{it in d.items}})
  assertTrue(d.counters.keys.map{it.third}.containsAll(listOf("all","epic","legend","mythic","honor","glory")))
  assertTrue(Strategy13.eligible(d,d.items.getValue("Twilight Armor")))
  for(i in d.items.values.filter{it.price!=null&&it.price>0}){
   val q=Recipe131.quote(d,i.name,emptyList(),null)
   assertEquals(i.name,i.price,q?.cost)
  }
 }
 @Test fun exactEconomy(){
  val d=db()
  assertEquals(470,Recipe131.quote(d,"Swift Boots",listOf("Boots"),300)?.cost)
  val p=d.items.getValue("Golden Staff").price!!
  val dagger=d.items.getValue("Dagger").price!!
  assertEquals(p-2*dagger,Recipe131.quote(d,"Golden Staff",listOf("Dagger","Dagger"),null)?.cost)
  val owned=listOf("Boots","Immortality","Oracle","Radiant Armor","Thunder Belt","Guardian Helmet")
  assertEquals(1,Recipe131.quote(d,"Swift Boots",owned,500)?.consumed)
  assertNull(Recipe131.quote(d,"Swift Boots",listOf("bad name"),500))
  assertNull(Strategy13.buy(d,"Swift Boots",listOf("Boots"),1000,false).remaining)
  val r=Strategy13.recommend(StrategicEngine.State("Hanzo",listOf("Immortality","Immortality","Oracle","Radiant Armor","Thunder Belt","Guardian Helmet"),inventoryKnown=true))
  assertNull(r.next)
  assertEquals(6,r.plan.size)
 }
 @Test fun matchupsSymmetry(){
  val d=db()
  for(rank in listOf("all","epic","legend","mythic","honor","glory"))for(a in d.heroes.keys)for(b in d.heroes.keys)if(a!=b){
   val f=d.counter(a,b,rank);val r=d.counter(b,a,rank)
   if(f==null)assertNull(r)else assertEquals(f,-r!!,1e-8)
   assertEquals(d.teammate(a,b,rank),d.teammate(b,a,rank))
  }
 }
 @Test fun manualPlayers(){
  val d=db();val p=Manual131.players("Tigreal|12000|15|2/3/8|Athena's Shield+Oracle;Layla|8000|12|1/2/3|Swift Boots",d,5)
  assertTrue(p.errors.isEmpty());assertEquals(2,p.players.size);assertEquals("Valkyrie's Shield",p.players[0].items[0])
  assertEquals(15,p.players[0].level);assertEquals(8,p.players[0].assists)
  assertTrue(Manual131.players("Tigreal|x|17|a/b/c|unknown",d,5).errors.isNotEmpty())
 }
 @Test fun rankLane(){
  val d=db();val p=Strategy13.draft("Roam","Tigreal","Atlas","Diggie","Lolita","legend")
  assertTrue(p.all{"Roam" in d.heroes.getValue(it.hero).lanes&&it.hero !in listOf("Tigreal","Atlas","Lolita")})
  assertTrue(p.all{it.reason.contains("legend")})
  val r=Strategy13.recommend(StrategicEngine.State("Hanzo",lane="Selva",rank="mythic"))
  assertNotNull(r.next);assertTrue(r.profile.contains("mythic/Jungle"))
 }
}
