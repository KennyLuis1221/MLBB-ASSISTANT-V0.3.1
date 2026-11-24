package com.mlbb.assistant
/** Manual test input is independent of screenshot recognition. Reject ambiguous rows. */
object Manual131 {
 data class Parsed(val players:List<StrategicEngine.Player>,val errors:List<String>)
 fun players(text:String,db:GameData13.Snapshot,max:Int):Parsed{
  val result=mutableListOf<StrategicEngine.Player>();val errors=mutableListOf<String>()
  for(row in text.split(';','\n').map{it.trim()}.filter{it.isNotEmpty()}){
   val v=row.split('|').map{it.trim()}
   val h=db.hero(v[0])
   if(h==null||v.size>5||result.any{it.hero==h.name}||result.size>=max){errors.add("Linha inválida: "+row);continue}
   fun num(i:Int,range:IntRange):Int?=v.getOrNull(i)?.takeIf{it.isNotBlank()}?.toIntOrNull()?.takeIf{it in range}
   val gold=num(1,0..999999);val level=num(2,1..15)
   val k=v.getOrNull(3).orEmpty()
   val kda=if(k.isBlank())emptyList()else k.split('/').mapNotNull{it.toIntOrNull()?.takeIf{n->n in 0..99}}
   val raw=v.getOrNull(4).orEmpty().split('+').map{it.trim()}.filter{it.isNotBlank()}
   val items=raw.mapNotNull(db::canonicalItem)
   if(v.getOrNull(1).orEmpty().isNotBlank()&&gold==null||v.getOrNull(2).orEmpty().isNotBlank()&&level==null||k.isNotBlank()&&(!k.matches(Regex("[0-9]{1,2}/[0-9]{1,2}/[0-9]{1,2}"))||kda.size!=3)||items.size!=raw.size||items.size>6){errors.add("Dados inválidos: "+row);continue}
   result.add(StrategicEngine.Player(h.name,items,gold,kda.getOrNull(0),kda.getOrNull(1),kda.getOrNull(2),level))
  }
  return Parsed(result,errors)
 }
}
