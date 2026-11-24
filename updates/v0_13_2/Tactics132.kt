package com.mlbb.assistant

/** Symmetric team heuristics and directional interactions; no inferred win rates. */
object Tactics132 {
 data class Fit(val score:Double,val reasons:List<String>)
 data class Loadout(val emblem:String,val talents:List<String>,val spell:String,val support:Int,val source:String)
 private val lanes=listOf("Jungle","Roam","Exp","Mid","Gold")
 fun team(db:GameData13.Snapshot,names:List<String>):Fit{
  val h=names.distinct().mapNotNull(db::hero).take(5)
  // Maximize unique lane assignment; no greedy double-counting.
  fun assign(i:Int,used:Set<String>):Int{
   if(i==h.size)return 0
   return maxOf(assign(i+1,used),h[i].lanes.filter{it in lanes&&it !in used}.maxOfOrNull{1+assign(i+1,used+it)}?:0)
  }
  val slots=assign(0,emptySet())
  val score=slots*2.0+(if(h.any{"Tank" in it.roles})2 else 0)+(if(h.any{it.damage=="Magic"}&&h.any{it.damage=="Physical"})2 else 0)+(if(h.any{"Crowd Control" in it.traits||"Initiator" in it.traits})2 else 0)
  return Fit(score,listOf("posições distintas possíveis: "+slots+"/"+h.size,"linha de frente: "+h.any{"Tank" in it.roles},"dano físico/mágico diversificado: "+(h.any{it.damage=="Magic"}&&h.any{it.damage=="Physical"})))
 }
 fun interaction(db:GameData13.Snapshot,a:String,b:String,ally:Boolean):Fit{
  val h=db.hero(a)?:return Fit(0.0,emptyList());val e=db.hero(b)?:return Fit(0.0,emptyList())
  if(h.name==e.name)return Fit(0.0,emptyList())
  fun tags(n:String)=db.skills[n].orEmpty().flatMap{it.tags}.toSet()
  val x=tags(h.name);val y=tags(e.name)
  val reasons=mutableListOf<String>();var score=0.0
  fun add(v:Double,r:String){score+=v;reasons.add(r)}
  fun directional(p:GameData13.Hero,q:GameData13.Hero,pt:Set<String>,qt:Set<String>,sign:Double){
   if("suppress" in pt&&("dash" in qt||"blink" in qt))add(sign*1.5,p.name+": supressão contra mobilidade")
   if("untargetable" in pt&&"Burst" in q.traits)add(sign*1.0,p.name+": janela de evasão contra explosão")
   if("true damage" in pt&&"Tank" in q.roles)add(sign*1.0,p.name+": dano verdadeiro contra defesa")
   if("shield" in qt&&p.name=="Esmeralda")add(sign*2,p.name+": interação com escudos")
   if(p.name=="Baxia"&&("heal" in qt||"Regen" in q.traits))add(sign*2,p.name+": redução de sustentação")
   if(p.name=="Minsitthar"&&("dash" in qt||"blink" in qt))add(sign*2,p.name+": bloqueio de deslocamentos elegíveis")
   if(p.name=="Phoveus"&&("dash" in qt||"blink" in qt))add(sign*1.5,p.name+": pressão sobre deslocamentos")
   if(p.name=="Valentina"&&q.name in setOf("Atlas","Tigreal","Minotaur"))add(sign*1.0,p.name+": potencial de ultimate copiada")
  }
  if(ally){
   if(("heal" in x&&"Fighter" in e.roles)||("heal" in y&&"Fighter" in h.roles))add(1.5,"cura + lutador")
   if(("shield" in x&&"Marksman" in e.roles)||("shield" in y&&"Marksman" in h.roles))add(1.5,"escudo + atirador")
   if(("stun" in x&&"Burst" in e.traits)||("stun" in y&&"Burst" in h.traits))add(1.5,"controle + explosão")
  }else{directional(h,e,x,y,1.0);directional(e,h,y,x,-1.0)}
  return Fit(score.coerceIn(-5.0,5.0),reasons)
 }
 fun loadouts(db:GameData13.Snapshot,hero:String,lane:String?,rank:String):List<Loadout>{
  val h=db.hero(hero)?:return emptyList()
  val ls=mapOf("Selva" to "Jungle","EXP" to "Exp","Meio" to "Mid","Ouro" to "Gold","Roam" to "Roam")[lane]?:lane
  val all=db.builds[h.name].orEmpty()
  val selected=all.filter{it.rank==rank&&(ls==null||it.lane==ls)}.ifEmpty{all.filter{it.rank=="all"&&(ls==null||it.lane==ls)}}
  return selected.filter{it.emblem.isNotBlank()&&it.spell.isNotBlank()}.groupBy{Triple(it.emblem,it.talents,it.spell)}.map{(k,v)->
   Loadout(k.first,k.second,k.third,v.size,v.first().rank+"/"+v.first().lane+" • "+if(v.any{it.type=="played"})"registros observados/guia" else "guia")
  }.sortedWith(compareByDescending<Loadout>{if(ls=="Jungle"&&it.spell.contains("Retribution",true))1 else 0}.thenByDescending{it.support}.thenBy{it.spell}.thenBy{it.emblem}).take(3)
 }
}
