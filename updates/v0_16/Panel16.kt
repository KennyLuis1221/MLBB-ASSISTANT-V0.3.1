package com.mlbb.assistant

/** Presentation semantics only; evidence counts are not probabilities of recognition. */
object Panel16 {
 data class Row(val hero:String,val side:String,val items:List<String>,val gold:String?,val kda:String?,val level:Int?)
 data class Group(val label:String,val expected:Int?,val rows:List<Row>)
 fun groups(rows:List<Row>,own:String?):List<Group>{
  val unique=rows.filter{it.hero.isNotBlank()}.groupBy{it.hero.lowercase(java.util.Locale.ROOT)}.values.mapNotNull{it.singleOrNull()}
  val allies=unique.filter{it.side=="Aliado"&&!it.hero.equals(own,true)}
  val enemies=unique.filter{it.side=="Inimigo"}
  val self=unique.filter{it.side=="Aliado"&&it.hero.equals(own,true)}
  val rest=unique.filter{it.side !in setOf("Aliado","Inimigo")}
  return listOf(Group("Seu herói",1,self),Group("Aliados",if(self.isNotEmpty())4 else null,allies),Group("Inimigos",5,enemies),Group("Lado incerto",null,rest))
 }
 fun coverage(row:Row):String{
  val info=listOfNotNull(row.gold?.let{"ouro provável "+it},row.kda?.let{"KDA "+it},row.level?.takeIf{it in 1..15}?.let{"nível provável "+it})
  return row.items.size.coerceAtMost(6).toString()+"/6 slots com item reconhecido • slots restantes não confirmados"+
    (if(info.isEmpty())" • estatísticas não identificadas" else " • "+info.joinToString(" • "))
 }
 fun threats(db:GameData13.Snapshot?,row:Row,own:Row?):List<String>{
  if(row.side!="Inimigo")return emptyList()
  val items=row.items.mapNotNull{db?.canonicalItem(it)}.toSet()
  val risks=mutableListOf<String>()
  val armor=items.intersect(setOf("Blade Armor","Antique Cuirass","Dominance Ice","Twilight Armor","Steel Legplates"))
  val magic=items.intersect(setOf("Valkyrie's Shield","Radiant Armor","Oracle","Silence Robe"))
  if(armor.isNotEmpty())risks.add("defesa física: "+armor.joinToString(", "))
  if(magic.isNotEmpty())risks.add("defesa mágica: "+magic.joinToString(", "))
  val lifebane=items.intersect(setOf("Sea Halberd","Glowing Wand","Dominance Ice"))
  if(lifebane.isNotEmpty())risks.add("anti-cura: "+lifebane.joinToString(", "))
  val a=own?.gold?.toIntOrNull();val b=row.gold?.toIntOrNull()
  if(a!=null&&b!=null&&b>a)risks.add("ouro total provável +"+(b-a))
  if(own?.level!=null&&row.level!=null&&row.level>own.level)risks.add("nível provável +"+(row.level-own.level))
  return risks
 }
}
