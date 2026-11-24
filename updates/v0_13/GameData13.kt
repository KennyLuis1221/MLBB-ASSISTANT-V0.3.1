package com.mlbb.assistant

import java.io.InputStream

/** Data is bundled as UTF-8 TSV; no internet permission required for these lookups. */
object GameData13 {
 data class Hero(val name:String,val slug:String,val roles:Set<String>,val lanes:Set<String>,val damage:String,val attack:String,val traits:Set<String>,val stats:Map<String,String>,val offense:Int?,val durability:Int?)
 data class Skill(val name:String,val cooldown:List<String>,val cost:List<String>,val tags:Set<String>,val scaling:List<String>)
 data class Item(val name:String,val category:String,val price:Int?,val recipe:List<String>,val bonus:String,val unique:String,val passive:String,val active:String)
 data class Build(val hero:String,val lane:String,val rank:String,val type:String,val frequency:Double,val items:List<String>)
 data class Gap(val from:String,val to:String,val rank:String,val value:Double)
 data class Snapshot(val date:String,val heroes:Map<String,Hero>,val skills:Map<String,List<Skill>>,val items:Map<String,Item>,val builds:Map<String,List<Build>>,val counters:Map<Triple<String,String,String>,Double>,val synergy:Map<Triple<String,String,String>,Double>) {
  fun hero(s:String?)=heroes.values.firstOrNull{it.name.equals(s?.trim(),true)}
  fun canonicalItem(s:String):String?{
   val v=normalizeItem(s)
   return items.keys.firstOrNull{it.equals(v,true)}
  }
  fun counter(a:String,b:String,rank:String="all"):Double?=counters[Triple(a,b,rank)]?:counters[Triple(b,a,rank)]?.let{-it}
  fun teammate(a:String,b:String,rank:String="all"):Double?{
   val values=listOfNotNull(synergy[Triple(a,b,rank)],synergy[Triple(b,a,rank)])
   return if(values.isEmpty())null else values.average()
  }
 }
 @Volatile var current:Snapshot?=null
   private set
 fun normalizeItem(s:String):String{
  val x=s.trim().replace('’','\'').replace(Regex("^(Ice|Flame|Behemoth) Hunter's\\s+",RegexOption.IGNORE_CASE),"").replace(Regex("\\s*-\\s*(Favor|Encourage|Conceal|Dire Hit)\\s*$",RegexOption.IGNORE_CASE),"")
  return when(x.lowercase(java.util.Locale.ROOT)){
    "athena's shield"->"Valkyrie's Shield";"magic boots"->"Magic Shoes";"demon boots"->"Demon Shoes";"haas's claws"->"Haas' Claws";else->x
  }
 }
 @Synchronized fun install(input:InputStream):Snapshot {
  val h=linkedMapOf<String,Hero>();val sk=linkedMapOf<String,MutableList<Skill>>();val it=linkedMapOf<String,Item>();val bs=linkedMapOf<String,MutableList<Build>>()
  val c=linkedMapOf<Triple<String,String,String>,Double>();val y=linkedMapOf<Triple<String,String,String>,Double>();var date=""
  fun list(s:String)=s.split("|").filter{it.isNotBlank()}
  input.bufferedReader(Charsets.UTF_8).useLines{lines->lines.forEach{line->
    val a=line.split('\t');when(a[0]){
     "V"->date=a[1]
     "H"->h[a[1]]=Hero(a[1],a[2],list(a[3]).toSet(),list(a[4]).toSet(),a[5],a[6],list(a[7]).toSet(),list(a[8]).mapNotNull{v->v.split("=",limit=2).let{if(it.size==2)it[0] to it[1] else null}}.toMap(),a[9].toIntOrNull(),a[10].toIntOrNull())
     "S"->sk.getOrPut(a[1]){mutableListOf()}.add(Skill(a[2],list(a[3]),list(a[4]),list(a[5]).toSet(),list(a[6])))
     "I"->it[a[1]]=Item(a[1],a[2],a[3].toIntOrNull(),list(a[4]),a[5],a[6],a[7],a[8])
     "B"->bs.getOrPut(a[1]){mutableListOf()}.add(Build(a[1],a[2],a[3],a[4],a[5].toDoubleOrNull()?:0.0,list(a[6])))
     "C","Y"->{val dest=if(a[0]=="C")c else y;val v=a[4].toDoubleOrNull();if(v!=null&&v.isFinite())dest[Triple(a[1],a[2],a[3])]=v}
    }
  }}
  require(date.isNotBlank()&&h.size==133&&it.size>=100&&sk.keys.containsAll(h.keys)&&c.isNotEmpty()&&bs.isNotEmpty())
  val result=Snapshot(date,h,sk,it,bs,c,y);current=result;return result
 }
}
