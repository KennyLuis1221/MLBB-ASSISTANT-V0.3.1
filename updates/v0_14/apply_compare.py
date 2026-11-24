from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
def rep(n,a,z):
 p=b/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,z,1))
p="Strategy13.kt"
# Complete-candidate comparison is limited to the current beam; known opponent level required.
rep(p,'val path=beam.first().items','''val comparisonTargets=s.enemies.filter{it.level!=null}.take(5)
  val evaluatedBuilds=if(comparisonTargets.isNotEmpty()&&s.ownLevel!=null&&beam.first().items.isNotEmpty())
      BuildCompare14.rank(db,h.name,s.ownLevel,beam.map{full+it.items},comparisonTargets).associateBy{it.items}
      else emptyMap()
  val path=beam.maxWithOrNull(compareBy<Path>{it.score+(evaluatedBuilds[full+it.items]?.score?.coerceIn(-10.0,20.0)?:0.0)}.thenByDescending{it.items.joinToString("|")})!!.items''')
# Fix panel invalidation on all focus-loss messages.
rep("OverlayPanel.kt",'message.contains("bloque",true)||message.contains("paus",true)||message.contains("erro",true)','message.contains("bloque",true)||message.contains("paus",true)||message.contains("erro",true)||message.contains("descartado",true)||message.contains("falha",true)')
# Expose compare and swaps in a local manual screen.
p="MainActivity.kt"
rep(p,'ui.addView(button("SIMULADOR DE DANO E SOBREVIVÊNCIA") { showCombat() })','''ui.addView(button("SIMULADOR DE DANO E SOBREVIVÊNCIA") { showCombat() })
        ui.addView(button("COMPARAR BUILDS E TROCAS") { showBuildCompare() })''')
rep(p,'    private fun showCombat() {','''    private fun showBuildCompare(){
        page="compare"
        val ui=root();back(ui,"COMPARAR BUILDS")
        val db=GameData13.current?:return
        ui.addView(label("Comparação de atributos e eventos modelados. Não considera todos os efeitos condicionais nem afirma melhor build global.",12,muted))
        val hero=input("Seu herói","Layla")
        val level=input("Seu nível","15")
        val targets=input("Alvos: nome|ouro|nível|K/D/A|item+item; próximo","Tigreal||15||Valkyrie's Shield")
        val first=input("Build A: itens separados por vírgula","Swift Boots, Berserker's Fury, Windtalker")
        val second=input("Build B: itens separados por vírgula","Swift Boots, Demon Hunter Sword, Corrosion Scythe")
        val sale=input("Valores de venda reais por slot: 1=ouro,2=ouro (opcional)","")
        val budget=input("Saldo atual real (opcional)","")
        val pool=input("Itens candidatos a troca (opcional)","")
        for(v in listOf(hero,level,targets,first,second,sale,budget,pool))ui.addView(v)
        val out=card()
        ui.addView(button("COMPARAR"){
            out.removeAllViews()
            val h=db.hero(hero.text.toString());val l=level.text.toString().toIntOrNull()
            val players=Manual131.players(targets.text.toString(),db,5)
            fun list(s:String)=s.split(",",";").map{it.trim()}.filter{it.isNotBlank()}
            if(h==null||l==null||l !in 1..15||players.errors.isNotEmpty()||players.players.any{it.level==null}){out.addView(label("Confira herói, nível e alvos com nível informado.",12,muted));return@button}
            val a=list(first.text.toString());val b=list(second.text.toString())
            val results=BuildCompare14.rank(db,h.name,l,listOf(a,b),players.players)
            fun fmt(v:Double?)=v?.let{String.format(java.util.Locale.US,"%.1f",it)}?:"indisponível"
            if(results.size!=listOf(a,b).distinct().size)out.addView(label("Build inválida ou com conflito de equipamento.",12,muted))
            for(r in results){
                val m=r.metrics
                out.addView(label(r.items.joinToString(" → "),14,gold,true))
                out.addView(label("Preço de referência: "+m.price+" • Pontuação do modelo: "+fmt(r.score)+" • Hit básico "+fmt(m.basicHit)+" • média de eventos "+fmt(m.sampleSkillDamage)+" ("+m.skillSamples+" amostras) • HP efetivo físico "+fmt(m.physicalEhp)+" • mágico "+fmt(m.magicEhp),12,white))
            }
            val pairs=sale.text.toString().split(",",";").mapNotNull{v->v.split("=",limit=2).takeIf{it.size==2}?.let{parts->val idx=parts[0].trim().toIntOrNull();val value=parts[1].trim().toIntOrNull();if(idx!=null&&value!=null)(idx-1) to value else null}}.toMap()
            if(pairs.isNotEmpty()){
                val swaps=BuildCompare14.swaps(db,h.name,l,a,list(pool.text.toString()),pairs,budget.text.toString().toIntOrNull(),players.players)
                out.addView(label("TROCAS CONDICIONAIS (1 OU 2 SLOTS)",12,cyan,true))
                if(swaps.isEmpty())out.addView(label("Nenhuma troca positiva e acessível no modelo. Informe seis itens e valores reais.",12,muted))
                for(s in swaps)out.addView(label(s.oldItems.joinToString(" + ")+" → "+s.newItems.joinToString(" + ")+" • custo líquido "+s.netCost+" ouro • ganho "+fmt(s.score),12,white))
            }
        })
        ui.addView(out)
    }

    private fun showCombat() {''')
