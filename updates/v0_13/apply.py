from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
def rep(n,a,z):
 p=b/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,z,1))
def allrep(n,a,z):
 p=b/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,z))
rep("MainActivity.kt","        super.onCreate(savedInstanceState)","        super.onCreate(savedInstanceState)\n        runCatching { assets.open(\"strategy13.tsv\").use { GameData13.install(it) } }")
allrep("MainActivity.kt","HeroCatalog.suggest(","Strategy13.draft(")
# initial draft call with old four args: add empty bans
rep("MainActivity.kt",'Strategy13.draft(lane.selectedItem.toString(), "", "", "")','Strategy13.draft(lane.selectedItem.toString(), "", "", "", "")')
allrep("MainActivity.kt","StrategicEngine.recommend(","Strategy13.recommend(")
rep("MainActivity.kt",'ui.addView(button("⚔  DICAS DE BUILD PARA MEU HERÓI") { showBuild() })','''ui.addView(button("⚔  DICAS DE BUILD PARA MEU HERÓI") { showBuild() })
        ui.addView(button("BANCO E CONFRONTOS OFFLINE") { showDatabase() })''')
rep("MainActivity.kt",'    private fun showBuild() {','''    private fun showDatabase() {
        page="database"
        val ui=root()
        back(ui,"BANCO E CONFRONTOS")
        val db=GameData13.current
        if(db==null){ui.addView(label("Banco indisponível; modo anterior ativo.",14,muted));return}
        ui.addView(label("Snapshot comunitário "+db.date,17,gold,true))
        ui.addView(label(db.heroes.size.toString()+" heróis • "+db.items.size+" equipamentos • "+db.skills.values.sumOf{it.size}+" habilidades registradas",13,white))
        ui.addView(label("Confrontos e sinergias: medidas parciais; ausências são estimativas por características, não taxas de vitória.",12,muted))
        val a=input("Seu herói","Hanzo")
        val e=input("Outro herói","Tigreal")
        ui.addView(a);ui.addView(e)
        val out=card()
        ui.addView(button("ANALISAR CONFRONTO") {
            out.removeAllViews()
            val hero=db.hero(a.text.toString())
            val rival=db.hero(e.text.toString())
            if(hero==null||rival==null||hero.name==rival.name){out.addView(label("Informe dois heróis distintos do catálogo.",12,muted));return@button}
            out.addView(label(hero.name+" • "+hero.damage+" • "+hero.lanes.joinToString(", "),16,gold,true))
            out.addView(label("Atributos de referência: "+hero.stats.entries.joinToString(" • "){it.key+"="+it.value},11,white))
            for(skill in db.skills[hero.name].orEmpty()){
                out.addView(label(skill.name,13,cyan,true))
                out.addView(label("CD: "+skill.cooldown.joinToString("/")+" • custo: "+skill.cost.joinToString("/")+" • efeitos detectados: "+skill.tags.joinToString(", ")+" • escalamentos: "+skill.scaling.joinToString(", "),11,muted))
            }
            val counter=Strategy13.matchup(db,hero.name,rival.name)
            val synergy=Strategy13.matchup(db,hero.name,rival.name,true)
            out.addView(label("Confronto: "+(counter.measured?.let{it.toString()+" p.p. observados"}?:"sem medida; hipótese "+counter.estimate),14,cyan,true))
            out.addView(label(counter.reasons.joinToString(" • "),12,white))
            out.addView(label("Sinergia: "+(synergy.measured?.let{it.toString()+" p.p. observados"}?:"sem medida; hipótese "+synergy.estimate),14,cyan,true))
            out.addView(label(synergy.reasons.joinToString(" • "),12,white))
        })
        ui.addView(out)
        val itemName=input("Nome do equipamento","Malefic Roar")
        val itemCard=card()
        ui.addView(itemName)
        ui.addView(button("CONSULTAR EQUIPAMENTO"){
            itemCard.removeAllViews()
            val v=db.canonicalItem(itemName.text.toString())?.let{db.items[it]}
            itemCard.addView(label(v?.name?:"Não identificado",16,gold,true))
            if(v!=null){
                itemCard.addView(label("Preço referência: "+v.price+" • "+v.category+" • "+v.bonus,12,white))
                itemCard.addView(label("Receita: "+v.recipe.joinToString(" + ")+" • passiva/ativa: "+v.passive+" "+v.active,12,muted))
            }
        })
        ui.addView(itemCard)
        ui.addView(label("Fonte: wiki Mobile Legends / achedon12/mlbb / arena.rone.dev. Consulte DATA_ATTRIBUTION no código. Não é dado oficial do patch.",11,muted))
    }

    private fun showBuild() {''')
rep("OfflineTextAnalyzer.kt",'private val recognizer=','private val dataset=runCatching { context.assets.open("strategy13.tsv").use { GameData13.install(it) } }.getOrNull()\n    private val recognizer=')
allrep("OfflineTextAnalyzer.kt","StrategicEngine.recommend(","Strategy13.recommend(")
allrep("OfflineTextAnalyzer.kt","HeroCatalog.suggest(","Strategy13.draft(")
# Reduce false identification: no draft unless keyword plus names or explicit strong draft header; no catalog names assumed picks.
rep("OfflineTextAnalyzer.kt",'val excluded=(left+right+named).distinct().joinToString(",")','val excluded=(left+right).distinct().joinToString(",")')
rep("OfflineTextAnalyzer.kt",'Nomes visíveis excluídos por cautela; bans/counters ainda não validados.','Nomes laterais excluídos por cautela; lados e bans não confirmados. Use o draft manual para composição precisa.')
# Prefer explicit chosen hero only when repeated rows contain it; avoid old hint as confirmed observation.
rep("OfflineTextAnalyzer.kt",'val hero=self?:manual','val hero=self?:manual?.takeIf{hint->recent.any{it.hero==hint}}')
# Validate repeated layout from same grid + use exact OCR text in rows only where portrait agrees if any.
rep("OfflineTextAnalyzer.kt",'it.side==r.side&&it.index==r.index&&it.hero?.name==r.hero?.name','it.side==r.side&&it.index==r.index&&it.hero?.name==r.hero?.name&&kotlin.math.abs(it.centerY-r.centerY)<b.height*.035')
rep("OfflineTextAnalyzer.kt",'val selected=self?:manual','val selected=self?:manual?.takeIf{hint->accepted.any{it.hero?.name==hint}}')
rep("OfflineTextAnalyzer.kt",'inventoryKnown=false))','inventoryKnown=false))')
# Preserve mode and avoid false live updates from cloud when no fresh data.
allrep("ProjectionService.kt","StrategicEngine.recommend(","Strategy13.recommend(")
rep("ProjectionService.kt",'status = "Aguardando placar • dica geral por classe"','status = "Aguardando placar • candidato offline"')
rep("ProjectionService.kt",'source = "Recomendação genérica, NÃO baseada em partida observada"','source = "Plano inicial, NÃO baseado em partida observada"')
# Local screen result freshness. Mark mode visibly. Cloud keeps original provider contract.
rep("ProjectionService.kt",'val enriched=result.copy(frameCount=completedFrames)','val enriched=result.copy(frameCount=completedFrames,source=(if(url.isBlank())"APP BASE • " else "MODO I.A. • ")+result.source)')
# Better data source recognition and plan row split in overlay. No art invented.
rep("OverlayPanel.kt",'plan.text=if(result.buildPlan.isEmpty())"" else "PLANO CANDIDATO\\n"+result.buildPlan.joinToString(" → ")','''plan.text=if(result.buildPlan.isEmpty())"" else "PLANO CANDIDATO\\n"+result.buildPlan.mapIndexed { i,n ->
            val owned=own?.items.orEmpty().any{GameData13.normalizeItem(it).equals(GameData13.normalizeItem(n),true)}
            (if(owned)"✓ " else if(n==result.item)"▶ " else (i+1).toString()+". ")+n
        }.joinToString("\\n")''')
g=Path("android-app/app/build.gradle.kts");s=g.read_text().replace("versionCode = 13","versionCode = 14").replace('versionName = "0.8.1"','versionName = "0.13.0"');g.write_text(s)
