package com.mlbb.assistant

import kotlin.math.*

/** Offline, deterministic conditional model. Not a game emulator.
 * Source: MLBB Wiki defense/penetration, snapshot 2026-11-23; MLBBHub defense check patch 2.2.16.
 */
object Combat132 {
 enum class Damage { PHYSICAL,MAGIC,TRUE }
 data class Stats(val hp:Double?,val physical:Double?,val magic:Double,val pdef:Double?,val mdef:Double?,val basePhysical:Double?,
  val speedBonus:Double,val crit:Double,val critDamage:Double,val cooldown:Double,val pPen:Double,val mPen:Double,val pPct:Double,val mPct:Double,
  val mana:Double?,val regen:Double,val life:Double,val vamp:Double,val healEffect:Double,val movement:Double?,val items:List<String>,val unmodeled:List<String>)
 data class Hit(val raw:Double,val type:Damage,val hits:Int=1,val critAllowed:Boolean=false)
 data class Event(val skill:String,val rawBase:Double,val terms:List<Term>,val type:Damage,val conditional:Boolean)
 data class Term(val coefficient:Double,val attribute:String)
 data class Estimate(val damage:Double,val healthDamage:Double,val shieldLeft:Double,val hpLeft:Double)
 private val number=Regex("""\+([0-9]+(?:\.[0-9]+)?)(%?)\s+([A-Za-z ]+)""")
 private val scaling=Regex("""(?<![\d.\-])(\d+(?:\.\d+)?)\s*((?:\(\s*\+?\s*\d+(?:\.\d+)?%\s+(?:Total Physical Attack|Extra Physical Attack|Total Magic Power|Magic Power|Total HP|Max HP|Extra HP|Total Mana|Max Mana|Total Physical Defense|Total Magic Defense)\s*\)\s*)+)(?:extra\s+)?(Physical|Magic|True)\s+Damage""",RegexOption.IGNORE_CASE)
 private val term=Regex("""(\d+(?:\.\d+)?)%\s+([A-Za-z ]+)""",RegexOption.IGNORE_CASE)
 private fun value(h:GameData13.Hero,key:String,level:Int):Double?{
  val a=h.stats[key+"1"]?.toDoubleOrNull()?:return null
  if(level==1)return a
  val b=h.stats[key+"15"]?.toDoubleOrNull()?:return null
  return a+(b-a)*(level-1)/14.0
 }
 fun stats(db:GameData13.Snapshot,hero:String,level:Int,items:List<String>):Stats?{
  if(level !in 1..15||items.size>6)return null
  val h=db.hero(hero)?:return null
  val names=items.map{db.canonicalItem(it)?:return null}
  val totals=mutableMapOf<String,Double>();val unresolved=mutableListOf<String>();val unique=mutableSetOf<String>()
  fun parse(text:String,source:String,isUnique:Boolean){
   for(part in text.split(",").map{it.trim()}.filter{it.isNotBlank()}){
    val m=number.matchEntire(part)
    if(m==null){unresolved.add(source+": "+part);continue}
    var key=m.groupValues[3].trim().lowercase(java.util.Locale.ROOT)+(if(m.groupValues[2]=="%")"%" else "")
    var v=m.groupValues[1].toDouble()
    if(key=="adaptive attack"){key=if(h.damage=="Magic")"magic power" else if(h.damage=="Physical")"physical attack" else {unresolved.add(source+": ataque adaptativo misto");continue}}
    if(isUnique&&!unique.add(source+":"+key))continue
    totals[key]=(totals[key]?:0.0)+v
   }
  }
  for(n in names){
   val i=db.items.getValue(n)
   parse(i.bonus,n,false);parse(i.unique,n,true)
   if(i.passiveText.isNotBlank()||i.activeText.isNotBlank())unresolved.add(n+": efeitos condicionais não ativados")
  }
  fun t(k:String)=totals[k]?:0.0
  val golden="Golden Staff" in names
  val p=if(names.any{it in setOf("Malefic Roar","Malefic Gun")})30.0 else 0.0
  val base=value(h,"physical_atk",level)
  val hp=value(h,"hp",level)?.plus(t("hp"))
  val magic=t("magic power")+if(h.name=="Kimmy")t("attack speed%") else 0.0
  val crit=t("crit chance%").coerceIn(0.0,100.0)
  val cdCap=if("Enchanted Talisman" in names)45.0 else 40.0
  return Stats(hp,base?.plus(t("physical attack")),magic,value(h,"physical_def",level)?.plus(t("physical defense")),value(h,"magic_def",level)?.plus(t("magic defense")),base,
   (if(h.name=="Kimmy")0.0 else t("attack speed%")+if(golden)crit else 0.0),if(golden)0.0 else crit,200+t("crit damage%"),t("cooldown reduction%").coerceIn(0.0,cdCap),t("physical penetration"),t("magic penetration"),(t("physical penetration%")+p).coerceIn(0.0,100.0),t("magic penetration%").coerceIn(0.0,100.0),value(h,"mana",level)?.plus(t("mana")),t("hp regen"),t("lifesteal%")+t("hybrid lifesteal%"),t("spell vamp%")+t("hybrid lifesteal%"),t("healing effect%"),h.stats["movement_spd"]?.toDoubleOrNull()?.plus(t("movement speed")),names,unresolved.distinct())
 }
 fun effectiveDefense(def:Double,percent:Double,flat:Double,flatReduction:Double=0.0,percentReduction:Double=0.0):Double{
  require(listOf(def,percent,flat,flatReduction,percentReduction).all{it.isFinite()})
  return ((def-flatReduction)*(1-percentReduction.coerceIn(0.0,100.0)/100)*(1-percent.coerceIn(0.0,100.0)/100)-flat).coerceAtLeast(-60.0)
 }
 fun mitigate(raw:Double,type:Damage,def:Double=0.0,pct:Double=0.0,flat:Double=0.0,reduction:Double=0.0):Double{
  require(raw.isFinite()&&raw>=0&&reduction.isFinite())
  if(type==Damage.TRUE)return raw
  return raw*120/(120+effectiveDefense(def,pct,flat))*(1-reduction.coerceIn(0.0,100.0)/100)
 }
 fun healthAfter(damage:Double,hp:Double,shield:Double):Estimate{
  require(listOf(damage,hp,shield).all{it.isFinite()&&it>=0})
  val healthDamage=(damage-shield).coerceIn(0.0,hp)
  return Estimate(damage,healthDamage,(shield-damage).coerceAtLeast(0.0),hp-healthDamage)
 }
 fun heal(amount:Double,bonus:Double=0.0,lifebane:Boolean=false)=amount.coerceAtLeast(0.0)*(1+bonus.coerceAtLeast(0.0)/100)*(if(lifebane).6 else 1.0)
 /** Only apply timed effects when the caller explicitly marks them active. */
 fun shieldOrHeal(amount:Double,recipient:Stats,lifebane:Boolean):Double=heal(amount,if("Oracle" in recipient.items)25.0 else 0.0,lifebane)
 fun activeMagicShield(damage:Double,recipient:Stats,active:Boolean)=damage*(if(active&&"Valkyrie's Shield" in recipient.items).75 else 1.0)
 fun demonHunterOnHit(currentHp:Double,attacker:Stats,defender:Stats):Double? =
  if("Demon Hunter Sword" !in attacker.items)0.0 else hit(Hit(currentHp.coerceAtLeast(0.0)*.08,Damage.PHYSICAL),attacker,defender)
 fun cooldown(seconds:Double,stats:Stats)=seconds.coerceAtLeast(0.0)*(1-stats.cooldown/100)
 fun expectedCrit(raw:Double,stats:Stats)=raw*(1+stats.crit/100*(stats.critDamage/100-1))
 fun hit(h:Hit,a:Stats,d:Stats):Double?{
  if(h.hits !in 1..100||h.raw<0||!h.raw.isFinite())return null
  val raw=if(h.critAllowed)expectedCrit(h.raw,a)else h.raw
  val def=when(h.type){Damage.PHYSICAL->d.pdef;Damage.MAGIC->d.mdef;Damage.TRUE->0.0}?:return null
  val pct=when(h.type){
   Damage.PHYSICAL->a.pPct+if("Malefic Roar" in a.items)(def*.1).coerceIn(0.0,30.0)else 0.0
   Damage.MAGIC->a.mPct+if("Divine Glaive" in a.items)(def*.1).coerceIn(0.0,20.0)else 0.0
   Damage.TRUE->0.0
  }
  val flat=when(h.type){Damage.PHYSICAL->a.pPen;Damage.MAGIC->a.mPen;Damage.TRUE->0.0}
  return mitigate(raw,h.type,def,pct,flat)*h.hits
 }
 /** Extract only explicit single-hit base + one coefficient; never infer hit counts, ranks or proc uptime. */
 fun events(db:GameData13.Snapshot,hero:String):List<Event> = db.skills[hero].orEmpty().flatMap{s->
  scaling.findAll(s.description).map{m->
   val context=s.description.substring(0,m.range.first).takeLast(150).lowercase(java.util.Locale.ROOT)
   Event(s.name,m.groupValues[1].toDouble(),term.findAll(m.groupValues[2]).map{Term(it.groupValues[1].toDouble()/100,it.groupValues[2].trim().lowercase(java.util.Locale.ROOT))}.toList(),Damage.valueOf(m.groupValues[3].uppercase(java.util.Locale.ROOT)),context.contains("if ")||context.contains("additional")||context.contains("after ")||context.contains("each ")||context.contains("when "))
  }.toList()
 }
 fun raw(e:Event,a:Stats):Double?{
  var total=e.rawBase
  for(term in e.terms){
  val v=when(term.attribute){
   "total physical attack"->a.physical
   "extra physical attack"->if(a.physical!=null&&a.basePhysical!=null)a.physical-a.basePhysical else null
   "total magic power","magic power"->a.magic
   "total hp","max hp"->a.hp
   "total mana","max mana"->a.mana
   "total physical defense"->a.pdef
   "total magic defense"->a.mdef
   else->null
  }?:return null
  total+=term.coefficient*v
  }
  return total
 }
 fun effectiveHP(s:Stats,type:Damage):Double?{
  val hp=s.hp?:return null
  val d=when(type){Damage.PHYSICAL->s.pdef;Damage.MAGIC->s.mdef;Damage.TRUE->0.0}?:return null
  return hp/mitigate(1.0,type,d)
 }
 fun combo(hits:List<Hit>,a:Stats,d:Stats,shield:Double=0.0,magicShieldActive:Boolean=false):Estimate?{
  val hp=d.hp?:return null
  val damages=hits.map{h->val v=hit(h,a,d)?:return null;if(h.type==Damage.MAGIC)activeMagicShield(v,d,magicShieldActive)else v}
  return healthAfter(damages.sum(),hp,shield)
 }
 /** Conditional deterministic basic-attack DPS: caller supplies actual attacks/sec. */
 fun dps(raw:Double,attackRate:Double,a:Stats,d:Stats,type:Damage):Double?{
  if(attackRate !in 0.0..10.0)return null
  return hit(Hit(raw,type,critAllowed=true),a,d)?.times(attackRate)
 }
 /** A modest numerical marginal signal, not a simulated skill rotation. Missing level uses explicit level-15 reference. */
 fun marginal(db:GameData13.Snapshot,hero:String,owned:List<String>,candidate:String,level:Int?,foes:List<StrategicEngine.Player>):Double{
  if(owned.size>=6||candidate in owned)return 0.0
  val before=stats(db,hero,level?:15,owned)?:return 0.0
  val after=stats(db,hero,level?:15,owned+candidate)?:return 0.0
  val h=db.hero(hero)?:return 0.0
  val p=foes.mapNotNull{stats(db,it.hero,it.level?:15,it.items.take(6))}
  val events=events(db,hero).filter{!it.conditional}.distinctBy{it.skill}
  val damageGains=p.flatMap{d->events.mapNotNull{e->
   val a=raw(e,before)?.let{hit(Hit(it,e.type),before,d)}
   val b=raw(e,after)?.let{hit(Hit(it,e.type),after,d)}
   if(a!=null&&b!=null&&a>0)(b-a)/a else null
  }}
  val ehp=listOf(Damage.PHYSICAL,Damage.MAGIC).mapNotNull{t->
   val a=effectiveHP(before,t);val b=effectiveHP(after,t)
   if(a!=null&&b!=null&&a>0)(b-a)/a else null
  }
  val attack=if(h.damage=="Physical"&&before.physical!=null&&after.physical!=null&&before.physical>0)(expectedCrit(after.physical,after)-expectedCrit(before.physical,before))/before.physical else 0.0
  val off=if(damageGains.isEmpty())attack else damageGains.average()
  val def=if(ehp.isEmpty())0.0 else ehp.average()
  val tank="Tank" in h.roles||"Support" in h.roles
  return (if(tank)off*3+def*12 else off*12+def*3).coerceIn(-5.0,12.0)
 }
}
