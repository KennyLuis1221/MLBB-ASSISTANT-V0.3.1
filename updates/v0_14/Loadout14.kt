package com.mlbb.assistant

/** Community reference dated 2026-09-11. Full emblem level only.
 * No automatic stacking or disputed current-patch percentages.
 */
object Loadout14 {
 data class Emblem(val name:String,val attributes:String)
 data class Talent(val name:String,val tier:Int,val attributes:String="",val notes:String="")
 data class Spell(val name:String,val cooldown:Double,val notes:String)
 data class Selection(val emblem:String,val level:Int,val talents:List<String>,val spell:String)
 data class Resolved(val attributes:List<String>,val warnings:List<String>,val weaponMasterPercent:Double?,val bargain:Boolean)
 val emblems=listOf(
  Emblem("Common","+12 Hybrid Regen, +275 HP, +22 Adaptive Attack"),
  Emblem("Tank","+500 HP, +10 Hybrid Defense, +4 HP Regen"),
  Emblem("Assassin","+14 Adaptive Penetration, +10 Adaptive Attack, +3% Movement Speed"),
  Emblem("Mage","+30 Magic Power, +5% Cooldown Reduction, +8 Magic Penetration"),
  Emblem("Fighter","+10% Hybrid Lifesteal, +16 Adaptive Attack, +8 Hybrid Defense"),
  Emblem("Support","+12% Healing Effect, +10% Cooldown Reduction, +6% Movement Speed"),
  Emblem("Marksman","+15% Attack Speed, +16 Adaptive Attack, +10% Adaptive Penetration")
 )
 val talents=listOf(
  Talent("Thrill",1,"+16 Adaptive Attack"),Talent("Swift",1,"+10% Attack Speed"),Talent("Vitality",1,"+225 HP"),
  Talent("Rupture",1,"+5 Adaptive Penetration"),Talent("Inspire",1,"+5% Cooldown Reduction, +2 Mana Regen"),Talent("Firmness",1,"+8 Hybrid Defense"),
  Talent("Agility",1,"+4% Movement Speed"),Talent("Fatal",1,"+5% Crit Chance, +5% Crit Damage"),
  Talent("Wilderness Blessing",2,notes="velocidade depende da região"),Talent("Seasoned Hunter",2,notes="dano de objetivo; não aplicado a heróis"),
  Talent("Tenacity",2,notes="defesa em HP baixo; valor divergente"),Talent("Master Assassin",2,notes="dano contra alvo isolado"),
  Talent("Bargain Hunter",2,notes="preço de referência reduzido em 5%; arredondamento da loja não confirmado"),
  Talent("Festival of Blood",2,"+6% Spell Vamp","stacks de abates/assistências não incluídas"),
  Talent("Pull Yourself Together",2,notes="redução de recarga de ativos/feitiço; valor divergente"),
  Talent("Weapon Master",2,notes="amplificação de ataque/poder de bônus; fontes divergem entre 5 e 8%"),
  Talent("Impure Rage",3,notes="proc de dano adaptativo e mana"),Talent("Quantum Charge",3,notes="proc de cura e movimento"),
  Talent("War Cry",3,notes="amplificação após golpes"),Talent("Temporal Reign",3,notes="recarga condicional após ultimate"),
  Talent("Concussive Blast",3,notes="dano mágico por HP"),Talent("Killing Spree",3,notes="cura/movimento após abate"),
  Talent("Lethal Ignition",3,notes="proc de dano adaptativo por sequência"),Talent("Brave Smite",3,notes="cura após dano de habilidade"),
  Talent("Focusing Mark",3,notes="amplificação de dano aliado"),Talent("Weakness Finder",3,notes="redução de mobilidade e ataque")
 )
 val spells=listOf(
  Spell("Execute",90.0,"dano verdadeiro em função do HP perdido; ignora escudo"),
  Spell("Retribution",35.0,"dano verdadeiro em monstros/súditos; bênçãos exigem estado"),
  Spell("Inspire",75.0,"janela de ataques básicos; quantidade de ataques diverge nas fontes"),
  Spell("Sprint",100.0,"velocidade e imunidade a lentidão"),
  Spell("Revitalize",75.0,"cura periódica em área"),
  Spell("Aegis",75.0,"escudo temporário"),
  Spell("Petrify",90.0,"dano mágico e controle; recarga deve ser conferida"),
  Spell("Purify",90.0,"limpeza de controles, exceto supressão"),
  Spell("Flameshot",50.0,"dano mágico dependente da distância"),
  Spell("Flicker",120.0,"deslocamento e defesa temporária"),
  Spell("Arrival",75.0,"teleporte com canalização; recarga deve ser conferida"),
  Spell("Vengeance",75.0,"redução de dano e reflexão; percentual de reflexão divergente")
 )
 private fun key(s:String)=s.trim().lowercase(java.util.Locale.ROOT).replace(Regex("[^a-z0-9]"),"")
 fun emblem(s:String):Emblem?{
  val k=key(s).removePrefix("custom").removePrefix("basic").removeSuffix("emblem")
  return emblems.firstOrNull{key(it.name)==k}
 }
 fun talent(s:String):Talent?{
  val k=key(s).let{if(it=="weaponsmaster")"weaponmaster" else it}
  return talents.firstOrNull{key(it.name)==k}
 }
 fun spell(s:String)=spells.firstOrNull{key(it.name)==key(s)}
 fun resolve(s:Selection,weaponMasterPercent:Double?=null):Resolved?{
  // Source contains level-60 totals only. No invented interpolation of emblem levels.
  if(s.level!=60)return null
  val e=emblem(s.emblem)?:return null
  val t=s.talents.map{talent(it)?:return null}
  if(t.size!=3||t.map{it.tier}.toSet()!=setOf(1,2,3)||spell(s.spell)==null)return null
  val hasMaster=t.any{it.name=="Weapon Master"}
  val wm=weaponMasterPercent?.takeIf{it in setOf(5.0,8.0)&&hasMaster}
  val notes=mutableListOf("Emblema de nível 60; snapshot comunitário de 11/09/2026, não certificado pelo patch atual.")
  notes+=t.filter{it.notes.isNotBlank()}.map{it.name+": "+it.notes}
  return Resolved(listOf(e.attributes)+t.map{it.attributes}.filter{it.isNotBlank()},notes,wm,t.any{it.name=="Bargain Hunter"})
 }
 /** Conditional spell values, not cooldown/target availability predictions. */
 fun execute(level:Int,targetLostHp:Double):Double?=if(level in 1..15&&targetLostHp.isFinite()&&targetLostHp>=0)100+10.0*level+.13*targetLostHp else null
 fun retribution(level:Int,creepOrMinion:Boolean):Double?=if(level in 1..15&&creepOrMinion)520+80.0*level else null
 fun aegis(level:Int):Double?=if(level in 1..15)750+90.0*level else null
 fun petrify(level:Int):Double?=if(level in 1..15)100+15.0*level else null
 fun flameshot(magic:Double,near:Boolean):Double?=if(magic.isFinite()&&magic>=0){if(near)200+.5*magic else 680+1.7*magic}else null
 fun revitalize(maxHp:Double,ticks:Int):Double?=if(maxHp.isFinite()&&maxHp>=0&&ticks in 0..10)maxHp*.025*ticks else null
 fun purifyRemoves(suppression:Boolean)=!suppression
}
