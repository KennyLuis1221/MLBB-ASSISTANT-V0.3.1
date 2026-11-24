package com.mlbb.assistant

import kotlin.math.abs

/** Deterministic candidate optimization with explicit data provenance and unknowns. */
object Strategy13 {
 data class Matchup(val measured:Double?,val estimate:Double,val reasons:List<String>)
 data class Buy(val target:String,val remaining:Int?,val component:String?,val detail:String)
 private val retired=setOf("Bloodlust Axe","Calamity Reaper","Necklace of Durance","Scarlet Phantom","Shadow Twinblades","Magic Potion","Power Potion","Rock Potion")
 private val armor=setOf("Antique Cuirass","Blade Armor","Chastise Pauldron","Dominance Ice","Brute Force Breastplate","Dreadnaught Armor","Steel Legplates","Leather Jerkin")
 private val resist=setOf("Valkyrie's Shield","Radiant Armor","Oracle","Silence Robe","Magic Resist Cloak")
 private val penetration=setOf("Malefic Roar","Malefic Gun")
 private val antiHeal=setOf("Glowing Wand","Sea Halberd","Dominance Ice")
 private val crit=setOf("Berserker's Fury","Haas' Claws","Windtalker","Great Dragon Spear")
 private val control=setOf("Crowd Control","Control","Initiator")
 private val laneMap=mapOf("Selva" to "Jungle","EXP" to "Exp","Meio" to "Mid","Ouro" to "Gold","Roam" to "Roam")
 private fun has(h:GameData13.Hero,t:Set<String>)=h.traits.any{it in t}
 fun matchup(db:GameData13.Snapshot,a:String,b:String,ally:Boolean=false):Matchup {
  val h=db.hero(a)?:return Matchup(null,0.0,listOf("perfil ausente"))
  val e=db.hero(b)?:return Matchup(null,0.0,listOf("perfil ausente"))
  val measured=if(ally)db.teammate(h.name,e.name)else db.counter(h.name,e.name)
  val reasons=mutableListOf<String>();var score=0.0
  fun add(n:Double,s:String){score+=n;reasons.add(s)}
  if(ally){
    if(has(h,control)&&("Marksman" in e.roles||"Mage" in e.roles))add(2.0,"controle + dano")
    if(has(e,control)&&("Marksman" in h.roles||"Mage" in h.roles))add(2.0,"dano + controle")
    if("Tank" in h.roles&&"Marksman" in e.roles||"Tank" in e.roles&&"Marksman" in h.roles)add(2.0,"linha de frente + dano")
    if(h.damage!=e.damage&&h.damage!="Mixed"&&e.damage!="Mixed")add(1.0,"diversidade de dano")
    if("Support" in h.roles&&"Fighter" in e.roles||"Support" in e.roles&&"Fighter" in h.roles)add(1.0,"suporte + luta prolongada")
  }else{
    if(has(h,control)&&has(e,setOf("Chase","Charge")))add(2.0,"controle contra mobilidade (hipótese)")
    if(has(h,setOf("Burst"))&&"Marksman" in e.roles)add(1.5,"explosão contra atirador (hipótese)")
    if(has(e,setOf("Burst"))&&"Marksman" in h.roles)add(-1.5,"risco de explosão adversária")
    if(has(h,setOf("Poke"))&&e.attack=="Melee")add(1.0,"alcance contra corpo a corpo")
    if(has(e,setOf("Poke"))&&h.attack=="Melee")add(-1.0,"risco de alcance")
    DraftRules.reasons(h.name,emptyList(),listOf(e.name)).forEach{add(3.0,it)}
  }
  if(reasons.isEmpty())reasons.add("sem evidência específica")
  return Matchup(measured,score.coerceIn(-5.0,5.0),reasons)
 }
 fun draft(lane:String,alliesInput:String,enemiesInput:String,favoritesInput:String,bansInput:String):List<HeroCatalog.Pick>{
  val db=GameData13.current?:return HeroCatalog.suggest(lane,alliesInput,enemiesInput,favoritesInput,bansInput)
  fun parse(s:String)=s.split(",",";","\n").mapNotNull{db.hero(it)?.name}.distinct()
  val a=parse(alliesInput).take(4);val e=parse(enemiesInput).take(5);val f=parse(favoritesInput);val bans=parse(bansInput)
  val excluded=(a+e+bans).toSet();val wanted=laneMap[lane]
  data class Candidate(val h:String,val score:Double,val why:String)
  return db.heroes.values.filter{it.name !in excluded&&(wanted==null||wanted in it.lanes)}.map{h->
    val es=e.map{it to matchup(db,h.name,it)}
    val asy=a.map{it to matchup(db,h.name,it,true)}
    // Measured gap is a community observation, not a probability of winning the draft.
    val score=es.sumOf{(_,m)->m.measured?.coerceIn(-12.0,12.0)?:m.estimate*.7}+asy.sumOf{(_,m)->m.measured?.coerceIn(-10.0,10.0)?:m.estimate*.7}+(if(h.name in f)4.0 else 0.0)
    val best=es.sortedByDescending{it.second.measured?:it.second.estimate}.firstOrNull()
    val worst=es.minByOrNull{it.second.measured?:it.second.estimate}
    val why=buildList{
     add("Função "+lane+"; dados "+db.date)
     add("Confrontos com dado comunitário: "+es.count{it.second.measured!=null}+"/"+es.size)
     add("Sinergias com dado comunitário: "+asy.count{it.second.measured!=null}+"/"+asy.size)
     best?.let{(n,m)->add(n+": "+(m.measured?.let{"diferença observada "+it+" p.p."}?:m.reasons.joinToString(", ")))}
     worst?.takeIf{it!=best}?.let{(n,m)->add("Atenção a "+n+": "+(m.measured?.let{it.toString()+" p.p."}?:m.reasons.joinToString(", ")))}
     if(h.name in f)add("favorito")
    }.joinToString(" • ")
    Candidate(h.name,score,why)
  }.sortedWith(compareByDescending<Candidate>{it.score}.thenBy{it.h}).take(3).map{HeroCatalog.Pick(it.h,it.why)}
 }
 fun eligible(db:GameData13.Snapshot,i:GameData13.Item):Boolean{
  // Legacy wiki entries exist. Shop-reference intersection is conservative; new shield is an alias.
  return i.name !in retired&&(i.name=="Valkyrie's Shield"||ItemReference.byName[i.name]?.tier==3)
 }
 fun buy(db:GameData13.Snapshot,target:String,owned:List<String>,budget:Int?,inventoryKnown:Boolean,explicitPrice:Int?=null):Buy{
  val item=db.items[target]?:return Buy(target,null,null,"Preço/receita desconhecidos.")
  val price=item.price?.takeIf{it>0}
  if(!inventoryKnown||price==null)return Buy(target,null,null,"Preço integral de referência: "+(price?.toString()?:"?")+" • inventário/preço restante não confirmado.")
  val stock=owned.mapNotNull(db::canonicalItem).toMutableList()
  // Reserve each owned component once; nested recipe credits capped at parent price.
  fun credit(n:String,seen:Set<String>):Int{
    if(n in seen)return 0
    val x=db.items[n]?:return 0;val p=x.price?:return 0
    if(stock.remove(n))return p
    return x.recipe.sumOf{credit(it,seen+n)}.coerceIn(0,p)
  }
  val credits=item.recipe.sumOf{credit(it,setOf(target))}
  val remaining=explicitPrice?.takeIf{it>=0}?: (price-credits).coerceAtLeast(0)
  val free=budget?.takeIf{it>=0}
  var component:String?=null
  if(free!=null&&remaining>free&&owned.size<6){
    val unspent=owned.mapNotNull(db::canonicalItem).toMutableList()
    // A duplicate recipe part is required twice, not automatically owned twice.
    val candidates=item.recipe.filter{!unspent.remove(it)}.mapNotNull{db.items[it]}.filter{it.price!=null&&it.price<=free&&it.price>0}
    component=candidates.maxByOrNull{it.price?:0}?.name
    if(component==null){
      component=item.recipe.flatMap{db.items[it]?.recipe.orEmpty()}.filter{it !in owned}
        .mapNotNull{db.items[it]}.filter{it.price!=null&&it.price<=free&&it.price>0}.maxByOrNull{it.price?:0}?.name
    }
  }
  val detail="Custo restante "+(if(explicitPrice!=null)"informado" else "estimado pela receita comunitária")+": "+remaining+
    " ouro"+(if(free==null)" • saldo desconhecido" else if(free>=remaining)" • cabe no saldo informado" else " • faltam "+(remaining-free))+
    (component?.let{" • componente candidato: "+it}?:"")+"; confirme no jogo."
  return Buy(target,remaining,component,detail)
 }
 fun recommend(s:StrategicEngine.State):StrategicEngine.Result {
  val db=GameData13.current?:return StrategicEngine.recommend(s)
  val h=db.hero(s.hero)?:return StrategicEngine.recommend(s)
  val legacy=StrategicEngine.recommend(s)
  val owned=s.owned.mapNotNull(db::canonicalItem)
  val full=owned.filter{db.items[it]?.let{v->eligible(db,v)}==true}.distinct()
  val unknown=s.owned.size!=owned.size
  val foes=s.enemies.mapNotNull{p->db.hero(p.hero)?.let{it to p}}.distinctBy{it.first.name}.take(5)
  val allies=s.allies.mapNotNull{db.hero(it.hero)}
  val builds=db.builds[h.name].orEmpty().filter{it.rank=="all"||it.rank=="mythic"}
  val seeds=builds.sortedWith(compareBy<GameData13.Build>{if(it.rank=="all")0 else 1}.thenBy{if(it.type=="played")0 else 1}.thenByDescending{it.frequency}).take(8)
  val legacyNames=legacy.plan.mapNotNull(db::canonicalItem)
  val skillTags=db.skills[h.name].orEmpty().flatMap{it.tags}.toSet()
  val aggregate=s.enemyItemsAggregate.mapNotNull(db::canonicalItem)
  fun weight(p:StrategicEngine.Player):Double{
   val g=if(s.ownGold!=null&&s.ownGold>0&&p.totalGold!=null)((p.totalGold.toDouble()/s.ownGold)-1).coerceIn(-.3,1.0)else 0.0
   val l=if(p.level!=null&&s.ownLevel!=null)((p.level-s.ownLevel)*.07).coerceIn(-.2,.4)else 0.0
   val k=if(p.kills!=null&&p.deaths!=null)((p.kills-p.deaths)*.04).coerceIn(-.2,.4)else 0.0
   return (1+g+l+k).coerceIn(.5,2.5)
  }
  fun threat(pred:(GameData13.Hero,StrategicEngine.Player)->Boolean)=foes.filter{pred(it.first,it.second)}.sumOf{weight(it.second)}
  val a=threat{_,p->p.items.mapNotNull(db::canonicalItem).any{it in armor}}.coerceAtLeast(aggregate.count{it in armor}.coerceAtMost(5).toDouble()).coerceAtLeast(if(s.armorHint)2.0 else 0.0)
  val m=threat{_,p->p.items.mapNotNull(db::canonicalItem).any{it in resist}}.coerceAtLeast(aggregate.count{it in resist}.coerceAtMost(5).toDouble())
  val heal=threat{v,p->"Regen" in v.traits||v.name in setOf("Estes","Floryn","Angela","Rafaela")||p.items.mapNotNull(db::canonicalItem).any{db.items[it]?.bonus?.contains("Vamp",true)==true||db.items[it]?.bonus?.contains("Lifesteal",true)==true}}.coerceAtLeast(if(s.healingHint)2.0 else 0.0)
  val magic=threat{v,_->v.damage in setOf("Magic","Mixed")&&"Burst" in v.traits}.coerceAtLeast(if(s.magicHint)2.0 else 0.0)
  val physical=threat{v,_->v.damage in setOf("Physical","Mixed")&&("Burst" in v.traits||"Marksman" in v.roles)}
  val tank=threat{v,_->"Tank" in v.roles}
  val cc=threat{v,_->has(v,control)}
  val allyAnti=s.allies.any{p->p.items.mapNotNull(db::canonicalItem).any{it in antiHeal}}
  data class Candidate(val name:String,val score:Double,val why:List<String>)
  val candidates=db.items.values.filter{eligible(db,it)}.mapNotNull{i->
   val pureMagic=h.damage=="Magic";val purePhysical=h.damage=="Physical"
   val tankRole="Tank" in h.roles&&"Marksman" !in h.roles
   val support="Support" in h.roles
   if((pureMagic&&i.category=="Attack"&&"Physical Attack" in i.bonus&&"Adaptive" !in i.bonus)||(purePhysical&&!support&&i.category=="Magic"&&"Magic Power" in i.bonus))return@mapNotNull null
   var score=3.0;val why=mutableListOf<String>()
   fun bonus(v:Double,r:String){score+=v;why.add(r)}
   val seedScore=seeds.mapNotNull{b->b.items.indexOf(i.name).takeIf{it>=0}?.let{pos->(if(b.type=="played")24.0 else 18.0)-pos*2}}.maxOrNull()
   if(seedScore!=null)bonus(seedScore,"equipamento em build comunitária de referência")
   val old=legacyNames.indexOf(i.name);if(old>=0)bonus(17.0-old*1.8,"candidato do perfil anterior")
   if("Attack Speed" in i.bonus&&"basic attack" in skillTags&&h.damage!="Magic")bonus(7.0,"sinergia possível com ataques básicos")
   if("Cooldown" in i.bonus&&("Mage" in h.roles||support))bonus(5.0,"recarga de habilidades")
   if(i.category=="Defense"&&tankRole)bonus(8.0,"função de linha de frente")
   if(i.name in penetration&&h.damage!="Magic"&&a>0)bonus(15+14*a,"armadura adversária")
   if(i.name=="Divine Glaive"&&h.damage!="Physical"&&m>0)bonus(15+14*m,"resistência mágica adversária")
   if(i.name in antiHeal&&heal>0)bonus((if(allyAnti)7 else 18)+12*heal,"cura/sustentação"+if(allyAnti)" (aliado já tem anti-cura)" else "")
   if(i.name=="Valkyrie's Shield"&&magic>0)bonus(14+13*magic,"explosão mágica")
   if(i.name in setOf("Antique Cuirass","Blade Armor")&&physical>0)bonus(10+9*physical,"ameaça física")
   if(i.name=="Tough Boots"&&cc>0)bonus(6+4*cc,"controle adversário")
   if(i.name=="Demon Hunter Sword"&&tank>0&&"basic attack" in skillTags&&h.damage!="Magic")bonus(8+10*tank,"alvos resistentes e ataques básicos")
   if(i.category=="Movement"&&full.none{db.items[it]?.category=="Movement"}&&(s.minute?:0)<6)bonus(15.0,"fase inicial")
   if(i.name=="Immortality"&&(s.minute?:0)>=15)bonus(14.0,"fase avançada")
   if(i.name=="Sky Piercer"&&s.ownGold!=null&&foes.isNotEmpty()&&foes.all{p->p.second.totalGold?.let{g->s.ownGold>g}==true})bonus(8.0,"vantagem econômica observada")
   Candidate(i.name,score,why.ifEmpty{listOf("candidato situacional")})
  }.sortedWith(compareByDescending<Candidate>{it.score}.thenBy{it.name})
  fun valid(n:String,inv:List<String>):Boolean{
   if(n in inv)return false
   val i=db.items[n]?:return false
   if(i.category=="Movement"&&inv.any{db.items[it]?.category=="Movement"})return false
   if(n in antiHeal&&inv.any{it in antiHeal})return false
   if(n in penetration&&inv.any{it in penetration})return false
   if(n=="Golden Staff"&&inv.any{it in crit}||n in crit&&"Golden Staff" in inv)return false
   return true
  }
  val threatText=listOfNotNull(if(a>0)"armadura" else null,if(m>0)"resistência mágica" else null,if(heal>0)"cura" else null,if(magic>0)"burst mágico" else null,if(physical>0)"dano físico" else null,if(cc>0)"controle" else null).joinToString(" • ").ifBlank{"sem ameaça específica confirmada"}+
   " | "+foes.size+"/5 inimigos • "+allies.size+"/4 aliados"
  if(full.size>=6||s.owned.size>=6&&unknown){
   val scores=candidates.associateBy{it.name}
   val replacements=if(s.inventoryKnown&&!unknown&&full.size==6)full.flatMap{old->
    candidates.filter{valid(it.name,full-old)}.map{Triple(old,it,it.score-(scores[old]?.score?:10.0))}
   }.filter{it.third>=22}.maxByOrNull{it.third}else null
   val detail=replacements?.let{"Avaliar troca: "+it.first+" → "+it.second.name+"; "+it.second.why.joinToString("; ")+"; custo de revenda não confirmado."}?:"Inventário cheio/incerto: não indicar venda sem evidência."
   return StrategicEngine.Result(null,detail,emptyList(),full.take(6),threatText,"Banco comunitário "+db.date,detail,0)
  }
  data class Path(val items:List<String>,val score:Double)
  var beam=listOf(Path(emptyList(),0.0));var evaluated=0
  repeat((6-full.size).coerceAtLeast(0)){
   val depth=it
   val next=beam.flatMap{path->candidates.filter{valid(it.name,full+path.items)}.map{c->
    evaluated++
    val combo=if(c.name in setOf("Golden Staff","Demon Hunter Sword","Corrosion Scythe")&&(full+path.items).any{it in setOf("Golden Staff","Demon Hunter Sword","Corrosion Scythe")}&&"basic attack" in skillTags)6.0 else 0.0
    Path(path.items+c.name,path.score+(c.score+combo)/(1+depth*.28))
   }}
   if(next.isNotEmpty())beam=next.sortedWith(compareByDescending<Path>{it.score}.thenBy{it.items.joinToString("|")}).take(24)
  }
  val path=beam.first().items
  val next=path.firstOrNull();val choice=candidates.firstOrNull{it.name==next}
  val purchase=next?.let{buy(db,it,owned,s.budget,s.inventoryKnown&&!unknown,s.prices[it]).detail}
  val reason=(choice?.why?.joinToString("; ")?: "Sem compra válida")+
   (if(!s.inventoryKnown||unknown)". Inventário parcial: sugestão provisória" else "")+". Dados comunitários "+db.date+"; não certificados pelo patch."
  return StrategicEngine.Result(next,reason,candidates.filter{valid(it.name,full)&&it.name!=next}.take(3).map{it.name},(full+path).take(6),threatText,
    if(seeds.isEmpty())"Perfil factual + regras, sem build observada" else "Perfil factual + "+seeds.size+" builds/guia de referência",purchase,evaluated)
 }
}
