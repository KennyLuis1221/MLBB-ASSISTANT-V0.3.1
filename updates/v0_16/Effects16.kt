package com.mlbb.assistant

/** Conditional effects from the bundled item descriptions.
 * All activation, duration and stack assumptions are supplied by the caller.
 * The numerical results are scenarios, not a certification of the installed patch.
 */
object Effects16 {
 data class Inputs(val holyCrystalPercent:Double?=null,val warAxeStacks:Int?=null,val bloodWingsShieldActive:Boolean=false)
 data class Result(val stats:Combat132.Stats,val shield:Double?,val warAxeTrueFraction:Double?,val warnings:List<String>)
 fun apply(db:GameData13.Snapshot,hero:String,base:Combat132.Stats,input:Inputs):Result?{
  val h=db.hero(hero)?:return null
  val items=base.items
  val percent=input.holyCrystalPercent
  val stacks=input.warAxeStacks
  if(percent!=null&&(!percent.isFinite()||percent !in 21.0..35.0||"Holy Crystal" !in items))return null
  if(stacks!=null&&(stacks !in 0..6||"War Axe" !in items))return null
  if(input.bloodWingsShieldActive&&"Blood Wings" !in items)return null
  var stats=base
  val warnings=mutableListOf<String>()
  if(percent!=null)stats=stats.copy(magic=stats.magic*(1+percent/100))
  else if("Holy Crystal" in items)warnings.add("Holy Crystal: percentual depende do nível; não presumido")
  var extra:Double?=null
  if(stacks!=null){
   val factor=if(h.roles.any{it in setOf("Marksman","Mage","Support")}) .5 else 1.0
   stats=stats.copy(physical=stats.physical?.plus(12.0*stacks*factor))
   extra=if(stacks==6).10*factor else 0.0
  }else if("War Axe" in items)warnings.add("War Axe: stacks e dano verdadeiro não presumidos")
  val shield=if(input.bloodWingsShieldActive)800+stats.magic else null
  if(input.bloodWingsShieldActive)stats=stats.copy(movement=stats.movement?.plus(30))
  return Result(stats,shield,extra,warnings)
 }
 /** Raw source value: current HP is supplied separately and capped for minions. */
 fun demonHunterRaw(currentHp:Double,minion:Boolean):Double?{
  if(!currentHp.isFinite()||currentHp<0)return null
  val raw=currentHp*.08
  return if(minion)raw.coerceAtMost(60.0) else raw
 }
 fun demonHunterRecovery(level:Int,minion:Boolean):Double?=
  if(level in 1..15)(10+4.0*level)*(if(minion).5 else 1.0) else null
}
