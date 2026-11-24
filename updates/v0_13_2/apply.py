from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
def rep(n,a,z):
 p=b/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,z,1))
p="Strategy13.kt"
rep(p,'if(reasons.isEmpty())reasons.add("sem evidência específica")','''val interaction=Tactics132.interaction(db,h.name,e.name,ally)
  score+=interaction.score;reasons.addAll(interaction.reasons)
  if(reasons.isEmpty())reasons.add("sem evidência específica")''')
rep(p,'val score=balance+es.sumOf','val teamFit=Tactics132.team(db,a+h.name)\n    val score=balance+teamFit.score+es.sumOf')
rep(p,'if(balance>0)add("complementa a composição")','if(balance>0)add("complementa a composição")\n     addAll(teamFit.reasons)')
rep(p,'Candidate(i.name,score,why.ifEmpty{listOf("candidato situacional")})','''val gain=Combat132.marginal(db,h.name,owned,i.name,s.ownLevel,s.enemies)
   if(gain>0.01)bonus(gain,"ganho numérico estimado de atributos/hit/EHP (modelo parcial)")
   Candidate(i.name,score,why.ifEmpty{listOf("candidato situacional")})''')
# Full inventory replacement uses numerical gain for each legal swap as well as contextual score.
rep(p,'it.score-(scores[old]?.score?:10.0)','it.score-(scores[old]?.score?:10.0)+Combat132.marginal(db,h.name,full-old,it.name,s.ownLevel,s.enemies)-Combat132.marginal(db,h.name,full-old,old,s.ownLevel,s.enemies)')
p="MainActivity.kt"
rep(p,'ui.addView(button("BANCO E CONFRONTOS OFFLINE") { showDatabase() })','''ui.addView(button("BANCO E CONFRONTOS OFFLINE") { showDatabase() })
        ui.addView(button("SIMULADOR DE DANO E SOBREVIVÊNCIA") { showCombat() })''')
rep(p,'    private fun showDatabase() {','''    private fun showCombat() {
        page="combat"
        val ui=root();back(ui,"COMBATE OFFLINE")
        val db=GameData13.current?:return
        ui.addView(label("Modelo parcial: nível interpolado, atributos estáticos e eventos explícitos. Não simula automaticamente todas as passivas, ranks de skill ou combos.",12,muted))
        val hero=input("Herói atacante","Eudora");val enemy=input("Alvo","Tigreal")
        val level=input("Nível atacante","15");val enemyLevel=input("Nível alvo","15")
        val items=input("Itens atacante, separados por vírgula","Holy Crystal, Divine Glaive")
        val enemyItems=input("Itens alvo, separados por vírgula","Valkyrie's Shield")
        val physical=input("Dano físico bruto por hit (opcional)","")
        val magic=input("Dano mágico bruto por hit (opcional)","1000")
        val trueDmg=input("Dano verdadeiro bruto (opcional)","")
        val hits=input("Número de hits de cada tipo","1")
        val sequence=input("Combo de eventos: índices separados por vírgula (opcional)","")
        val stacks=input("Stacks Soul Steal (Aldous, opcional)","")
        val magicGuard=check("Ativar redução mágica temporária de Valkyrie's Shield no alvo")
        val shield=input("Escudo inicial do alvo","0")
        val rate=input("Ataques por segundo conhecidos (opcional)","")
        val heal=input("Cura bruta recebida (opcional)","")
        val anti=check("Lifebane ativo no alvo")
        for(v in listOf(hero,enemy,level,enemyLevel,items,enemyItems,physical,magic,trueDmg,hits,sequence,stacks,shield,rate,heal))ui.addView(v)
        ui.addView(anti);ui.addView(magicGuard)
        val out=card()
        ui.addView(button("CALCULAR"){
            out.removeAllViews()
            fun list(s:String)=s.split(",",";").map{it.trim()}.filter{it.isNotBlank()}
            val h=db.hero(hero.text.toString());val e=db.hero(enemy.text.toString())
            val a=h?.let{Combat132.stats(db,it.name,level.text.toString().toIntOrNull()?:0,list(items.text.toString()))}
            val d=e?.let{Combat132.stats(db,it.name,enemyLevel.text.toString().toIntOrNull()?:0,list(enemyItems.text.toString()))}
            val n=hits.text.toString().toIntOrNull()
            val sh=shield.text.toString().toDoubleOrNull()
            if(a==null||d==null||n==null||n !in 1..100||sh==null||!sh.isFinite()||sh<0){
                out.addView(label("Confira heróis, níveis, nomes e quantidade de itens.",13,muted));return@button
            }
            val events=mutableListOf<Combat132.Hit>()
            for((v,t) in listOf(physical to Combat132.Damage.PHYSICAL,magic to Combat132.Damage.MAGIC,trueDmg to Combat132.Damage.TRUE)){
                val text=v.text.toString().trim()
                if(text.isNotEmpty()){
                    val x=text.toDoubleOrNull()
                    if(x==null||!x.isFinite()||x<0){out.addView(label("Dano bruto inválido.",12,muted));return@button}
                    events.add(Combat132.Hit(x,t,n))
                }
            }
            fun fmt(v:Double?)=v?.let{String.format(java.util.Locale.US,"%.1f",it)}?:"não disponível"
            val extracted=Combat132.events(db,h!!.name)
            val soul=stacks.text.toString().toIntOrNull()?.takeIf{it in 0..1000}
            val sequenceText=sequence.text.toString().trim()
            if(sequenceText.isNotBlank()){
                val indices=sequenceText.split(",",";").map{it.trim().toIntOrNull()}
                if(indices.size>30||indices.any{it==null||it !in 1..extracted.size}){
                    out.addView(label("Confira os índices da lista de eventos; máximo 30.",12,muted))
                }else{
                    val chosen=indices.map{extracted[it!!-1]}
                    val rawEvents=chosen.map{ev->Combat132.raw(ev,a,soul)?.let{Combat132.Hit(it,ev.type)}}
                    if(rawEvents.any{it==null})out.addView(label("Combo indisponível: atributo de escalamento ausente.",12,muted))
                    else {
                        val combo=Combat132.combo(rawEvents.filterNotNull(),a,d,sh,magicGuard.isChecked)
                        out.addView(label("Sequência condicional informada: "+fmt(combo?.damage)+" de dano • "+chosen.joinToString(" → "){it.skill},13,cyan,true))
                    }
                }
            }
            val result=Combat132.combo(events,a,d,sh,magicGuard.isChecked)
            out.addView(label("Dano total: "+fmt(result?.damage)+" • HP retirado: "+fmt(result?.healthDamage)+" • HP restante: "+fmt(result?.hpLeft),17,gold,true))
            out.addView(label("Ataque físico "+fmt(a.physical)+" • poder mágico "+fmt(a.magic)+" • crítico "+fmt(a.crit)+"% • CDR "+fmt(a.cooldown)+"%",13,white))
            out.addView(label("HP alvo "+fmt(d.hp)+" • defesa física "+fmt(d.pdef)+" • defesa mágica "+fmt(d.mdef),13,white))
            out.addView(label("HP efetivo físico "+fmt(Combat132.effectiveHP(d,Combat132.Damage.PHYSICAL))+" • mágico "+fmt(Combat132.effectiveHP(d,Combat132.Damage.MAGIC)),13,white))
            rate.text.toString().toDoubleOrNull()?.takeIf{it in 0.0..10.0}?.let{r->
                a.physical?.let{out.addView(label("DPS físico básico hipotético (taxa informada): "+fmt(Combat132.dps(it,r,a,d,Combat132.Damage.PHYSICAL)),13,white))}
            }
            heal.text.toString().toDoubleOrNull()?.takeIf{it.isFinite()&&it>=0}?.let{out.addView(label("Cura recebida no cenário: "+fmt(Combat132.shieldOrHeal(it,d,anti.isChecked)),13,white))}
            out.addView(label("EVENTOS DE HABILIDADE EXTRAÍDOS • valores da descrição, não ranks calculados",12,cyan,true))
            for((idx,ev) in extracted.withIndex()){
                val raw=Combat132.raw(ev,a,soul)
                out.addView(label((idx+1).toString()+". "+ev.skill+" • "+ev.type+" • bruto "+fmt(raw)+" • mitigado "+fmt(raw?.let{Combat132.hit(Combat132.Hit(it,ev.type),a,d)})+(if(ev.conditional)" • condicional" else " • evento isolado"),11,white))
            }
            out.addView(label("Efeitos não simulados: "+(a.unmodeled+d.unmodeled).distinct().joinToString("; "),11,muted))
        })
        ui.addView(out)
    }

    private fun showDatabase() {''')
rep(p,'result.addView(label(advice.profile,12,muted))','''result.addView(label(advice.profile,12,muted))
            for(loadout in Tactics132.loadouts(db,hero,pos.selectedItem.toString().takeUnless{it=="Automática"},ranks.selectedItem.toString())){
                result.addView(label("CONFIGURAÇÃO DE REFERÊNCIA • "+loadout.source,12,cyan,true))
                result.addView(label(loadout.emblem+" • "+loadout.talents.joinToString(", ")+" • "+loadout.spell+" • "+loadout.support+" registros (não partidas)",12,white))
            }''')
g=Path("android-app/app/build.gradle.kts");s=g.read_text().replace("versionCode = 15","versionCode = 16").replace('versionName = "0.13.1"','versionName = "0.13.2"');g.write_text(s)

# Supplement the only hero lacking a complete six-item guide; never call it measured.
asset=Path("android-app/app/src/main/assets/strategy13.tsv")
rows=asset.read_text(encoding="utf-8").splitlines()
hero_names={r.split("\t")[1] for r in rows if r.startswith("H\t")}
complete={a[1] for r in rows if (a:=r.split("\t"))[0]=="B" and a[4] in ("guide","curated") and len(a[6].split("|"))==6}
if "Barats" not in complete:
    rows.append("\t".join(["B","Barats","Jungle","all","curated","0","Tough Boots|War Axe|Thunder Belt|Brute Force Breastplate|Guardian Helmet|Immortality","","","",""]))
    complete.add("Barats")
assert hero_names==complete,hero_names-complete
asset.write_text("\n".join(rows)+"\n",encoding="utf-8")
print("FULL SIX-ITEM REFERENCE COVERAGE",len(complete),"/",len(hero_names))
rep("Strategy13.kt",'builds.filter{it.type=="guide"}.take(4)','builds.filter{it.type in setOf("guide","curated")}.take(4)')
rep("MainActivity.kt",'val guide=ref.firstOrNull{it.type=="guide"}','val guide=ref.firstOrNull{it.type in setOf("guide","curated")}')
rep("MainActivity.kt",'if(b.type=="guide")"GUIA DE JOGADOR • NÃO É TAXA MEDIDA" else "ITENS-CHAVE OBSERVADOS"','if(b.type=="curated")"GUIA EDITORIAL • MLBBGuides" else if(b.type=="guide")"GUIA DE JOGADOR • NÃO É TAXA MEDIDA" else "ITENS-CHAVE OBSERVADOS"')
