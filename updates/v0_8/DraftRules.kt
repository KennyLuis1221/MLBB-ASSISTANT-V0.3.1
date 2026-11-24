package com.mlbb.assistant

/** Sparse qualitative interaction hypotheses, not a matchup win-rate matrix. */
object DraftRules {
    fun reasons(hero:String,allies:List<String>,enemies:List<String>):List<String> {
        val e=enemies.toSet();val a=allies.toSet()
        val r=mutableListOf<String>()
        if(hero=="Diggie"&&e.any{it in setOf("Tigreal","Atlas","Minotaur")})r.add("avaliar proteção contra controle em área")
        if(hero=="Lolita"&&e.any{it in setOf("Chang'e","Layla","Granger")})r.add("avaliar bloqueio de projéteis")
        if(hero=="Karrie"&&e.any{it in setOf("Hylos","Belerick","Fredrinn")})r.add("avaliar dano contra alvos resistentes")
        if(hero=="Valir"&&e.any{it in setOf("Tigreal","Atlas","Hylos")})r.add("avaliar afastamento de iniciadores")
        if(hero=="Estes"&&a.any{it in setOf("Tigreal","Fredrinn","Belerick")})r.add("possível sustentação com linha de frente")
        if(hero=="Angela"&&a.any{it in setOf("Alpha","Yu Zhong","Lapu-Lapu")})r.add("avaliar suporte a lutadores")
        if(hero=="Khufra"&&e.any{it in setOf("Fanny","Ling","Lancelot")})r.add("avaliar controle de mobilidade")
        return r
    }
}
