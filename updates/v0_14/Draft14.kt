package com.mlbb.assistant

/** Ban suggestions use only declared planned/selected heroes. No fabricated meta pick rate. */
object Draft14 {
 data class Ban(val hero:String,val measured:Int,val estimated:Int,val score:Double,val reason:String)
 fun bans(db:GameData13.Snapshot,allies:List<String>,planned:List<String>,enemies:List<String>,already:List<String>,rank:String):List<Ban>{
  val team=(allies+planned).mapNotNull{db.hero(it)?.name}.distinct().take(5)
  if(team.isEmpty())return emptyList()
  val excluded=(team+enemies+already).mapNotNull{db.hero(it)?.name}.toSet()
  return db.heroes.keys.filter{it !in excluded}.mapNotNull{hero->
   val match=team.map{it to Strategy13.matchup(db,hero,it,rank=rank)}
   val score=match.sumOf{(_,m)->m.measured?.coerceIn(-12.0,12.0)?:m.estimate*.7}/team.size
   if(score<=0)return@mapNotNull null
   val measured=match.count{it.second.measured!=null}
   val risk=match.maxByOrNull{(_,m)->m.measured?:m.estimate*.7}!!
   Ban(hero,measured,team.size-measured,score,"Risco para "+risk.first+": "+(risk.second.measured?.let{it.toString()+" p.p. observados"}?:risk.second.reasons.joinToString(", "))+" • medidos "+measured+"/"+team.size)
  }.sortedWith(compareByDescending<Ban>{it.score}.thenByDescending{it.measured}.thenBy{it.hero}).take(5)
 }
}
