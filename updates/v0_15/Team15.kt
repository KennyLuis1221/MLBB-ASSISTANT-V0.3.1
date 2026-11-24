package com.mlbb.assistant

/** Composition scenarios using deterministic assignments and explicit matchup evidence.
 * Scores are heuristic priorities, not estimated probabilities or outcome simulations.
 */
object Team15 {
 val lanes=listOf("Jungle","Roam","Exp","Mid","Gold")
 data class Assignment(val byLane:Map<String,String>,val unassigned:List<String>,val missing:List<String>)
 data class Evidence(val measured:Int,val estimated:Int,val meanMeasured:Double?,val meanEstimate:Double?)
 data class Risk(val hero:String,val against:String,val measured:Double?,val estimate:Double,val reasons:List<String>)
 data class Report(val allies:Assignment,val enemies:Assignment,val counters:Evidence,val allySynergy:Evidence,val enemySynergy:Evidence,
  val allyConditions:List<String>,val enemyConditions:List<String>,val risks:List<Risk>)
 data class Response(val hero:String,val threatened:String,val score:Double,val measured:Double?,val estimated:Double)
 fun roster(db:GameData13.Snapshot,names:List<String>)=names.mapNotNull{db.hero(it)?.name}.distinct().take(5)
 fun assign(db:GameData13.Snapshot,names:List<String>,locked:Map<String,String> = emptyMap()):Assignment?{
  val n=roster(db,names)
  if(locked.keys.any{it !in lanes}||locked.values.distinct().size!=locked.size||
    locked.any{(lane,hero)->hero !in n||lane !in db.heroes.getValue(hero).lanes})return null
  val remaining=n.filter{it !in locked.values}
  var best=locked.toMap()
  fun visit(i:Int,m:Map<String,String>){
   if(i==remaining.size){if(m.size>best.size)best=m.toMap();return}
   val hero=db.heroes.getValue(remaining[i])
   for(l in lanes.filter{it in hero.lanes&&it !in m})visit(i+1,m+(l to hero.name))
   visit(i+1,m)
  }
  visit(0,locked.toMap())
  return Assignment(best,n.filter{it !in best.values},lanes.filter{it !in best.keys})
 }
 private fun evidence(rows:List<Strategy13.Matchup>):Evidence{
  val measured=rows.mapNotNull{it.measured}
  val estimated=rows.filter{it.measured==null}.map{it.estimate}
  return Evidence(measured.size,estimated.size,measured.takeIf{it.isNotEmpty()}?.average(),estimated.takeIf{it.isNotEmpty()}?.average())
 }
 private fun conditions(db:GameData13.Snapshot,n:List<String>,a:Assignment):List<String>{
  val h=n.map{db.heroes.getValue(it)}
  if(h.isEmpty())return listOf("Composição ainda desconhecida")
  val tags=n.associateWith{hero->db.skills[hero].orEmpty().flatMap{it.tags}.toSet()}
  val r=mutableListOf<String>()
  val frontline=h.filter{"Tank" in it.roles||"Initiator" in it.traits}
  val sustained=h.filter{"Marksman" in it.roles||"Damage" in it.traits&&"Fighter" in it.roles}
  val burst=h.filter{"Burst" in it.traits}
  val control=h.filter{p->tags[p.name].orEmpty().any{it in setOf("stun","suppress","knock","immobil")}}
  val healing=h.filter{p->"heal" in tags[p.name].orEmpty()||"shield" in tags[p.name].orEmpty()}
  if(frontline.isEmpty())r.add("Risco: sem linha de frente identificada no perfil")
  else if(sustained.isNotEmpty())r.add("Hipótese: proteger dano sustentado ("+sustained.joinToString{it.name}+") com "+frontline.joinToString{it.name})
  if(burst.isNotEmpty()&&control.isNotEmpty())r.add("Hipótese: combinar controle e explosão")
  if(healing.isNotEmpty()&&frontline.isNotEmpty())r.add("Hipótese: luta prolongada com cura/escudo")
  val damage=h.map{it.damage}.toSet()
  if(damage.size==1&&damage.first() in setOf("Physical","Magic"))r.add("Risco: concentração em um único tipo de dano")
  if(a.missing.isNotEmpty())r.add("Posições ainda sem atribuição: "+a.missing.joinToString(", "))
  if(a.unassigned.isNotEmpty())r.add("Conflito de posições possíveis: "+a.unassigned.joinToString(", "))
  return r.ifEmpty{listOf("Não há condição específica inferida pelos perfis")}
 }
 fun report(db:GameData13.Snapshot,allies:List<String>,enemies:List<String>,rank:String="all",locked:Map<String,String> = emptyMap()):Report?{
  val a=roster(db,allies);val e=roster(db,enemies)
  if(a.size!=allies.size||e.size!=enemies.size||a.any{it in e})return null
  val aa=assign(db,a,locked)?:return null;val ee=assign(db,e)?:return null
  val counter=a.flatMap{x->e.map{y->Triple(x,y,Strategy13.matchup(db,x,y,rank=rank))}}
  fun synergy(n:List<String>)=n.indices.flatMap{i->(i+1 until n.size).map{j->Strategy13.matchup(db,n[i],n[j],true,rank)}}
  val risks=counter.filter{(_,_,m)->(m.measured?:m.estimate*.7)<0}.sortedBy{(_,_,m)->m.measured?:m.estimate*.7}.take(5).map{(x,y,m)->Risk(x,y,m.measured,m.estimate,m.reasons)}
  return Report(aa,ee,evidence(counter.map{it.third}),evidence(synergy(a)),evidence(synergy(e)),conditions(db,a,aa),conditions(db,e,ee),risks)
 }
 /** Stress test one legal opposing reply, restricted to a position that can still be filled.
  * These are counterexamples for planning, not forecasts of actual selection.
  */
 fun responses(db:GameData13.Snapshot,allies:List<String>,enemies:List<String>,bans:List<String>,candidate:String,rank:String="all"):List<Response>{
  val a=roster(db,allies+candidate);val e=roster(db,enemies)
  if(a.size!=allies.size+1||e.size!=enemies.size||a.any{it in e}||e.size>=5)return emptyList()
  val excluded=(a+e+roster(db,bans)).toSet()
  val before=assign(db,e)?.byLane?.size?:0
  return db.heroes.values.asSequence().filter{it.name !in excluded}.mapNotNull{h->
   if((assign(db,e+h.name)?.byLane?.size?:0)<=before)return@mapNotNull null
   val pairs=a.map{p->p to Strategy13.matchup(db,h.name,p,rank=rank)}
   val hardest=pairs.maxByOrNull{(_,m)->m.measured?:m.estimate*.7}?:return@mapNotNull null
   val score=hardest.second.measured?:hardest.second.estimate*.7
   if(score<=0)return@mapNotNull null
   Response(h.name,hardest.first,score,hardest.second.measured,hardest.second.estimate)
  }.sortedWith(compareByDescending<Response>{it.score}.thenBy{it.hero}).take(3).toList()
 }
}
