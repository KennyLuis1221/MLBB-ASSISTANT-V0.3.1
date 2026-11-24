package com.mlbb.assistant
object ObservationParser {
    fun kda(tokens:List<String>):String? {
        val matches=Regex("(?<![0-9])([0-9]{1,2})\\s*/\\s*([0-9]{1,2})\\s*/\\s*([0-9]{1,2})(?![0-9])")
            .findAll(tokens.joinToString(" ")).map{m->m.groupValues.drop(1).joinToString("/")}.distinct().toList()
        return matches.singleOrNull()
    }
    fun gold(tokens:List<String>):String? {
        return tokens.map{it.replace(",","").replace(".","")}
            .filter{it.matches(Regex("[0-9]{3,5}"))&&(it.toIntOrNull()?:-1) in 100..99999}.distinct().singleOrNull()
    }
    fun clockSeconds(s:String?):Int?{
        val parts=s?.split(":")?:return null
        if(parts.size!=2)return null
        val m=parts[0].toIntOrNull()?:return null;val sec=parts[1].toIntOrNull()?:return null
        return if(m in 0..99&&sec in 0..59)m*60+sec else null
    }
    fun compatibleGold(old:String?,new:String?):String? {
        val a=old?.toIntOrNull()?:return null;val b=new?.toIntOrNull()?:return null
        return new.takeIf{b>=a&&b-a<=2500}
    }
}
