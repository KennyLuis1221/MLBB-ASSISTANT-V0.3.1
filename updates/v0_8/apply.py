from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
def rep(name,a,z):
 p=b/name;s=p.read_text();assert a in s,(name,a);p.write_text(s.replace(a,z,1))
rep("MainActivity.kt",'ui.addView(button("▶  INICIAR ASSISTÊNCIA AO VIVO", true) { startCapture() })','''ui.addView(button("▶  INICIAR COM I.A.", true) { startCapture(true) })
        ui.addView(label("Visão online opcional • Gemini gratuito • sujeito à cota",12,muted))
        ui.addView(space(9))
        ui.addView(button("▶  INICIAR COM APP BASE", true) { startCapture(false) })
        ui.addView(label("Motor estratégico local • nenhuma imagem enviada • sem cota de API",12,muted))''')
rep("MainActivity.kt",'private fun startCapture() {','private fun startCapture(useCloud:Boolean) {')
rep("MainActivity.kt",'sessionCloudAllowed=cloudConsent?.isChecked==true','''if(useCloud&&cloudConsent?.isChecked!=true)return toast("Autorize o envio opcional para iniciar com I.A.")
        sessionCloudAllowed=useCloud''')
rep("MainActivity.kt",'if (url.isNotBlank() && !(url.startsWith("https://") || (BuildConfig.DEBUG && url.startsWith("http://"))))','if (useCloud && url.isNotBlank() && !url.startsWith("https://"))')
rep("MainActivity.kt",'val connected = !prefs.getString("url", "").isNullOrBlank() && !prefs.getString("token", "").isNullOrBlank()','val connected = prefs.getString("url", "").orEmpty().ifBlank{BuildConfig.DEFAULT_API_URL}.startsWith("https://")')
rep("MainActivity.kt",'Versão 0.7 experimental. Observações repetidas, diagnóstico de captura e painel redesenhado. Não acessa informações ocultas.','Versão 0.8 experimental • motor estratégico local e modo I.A. opcional. Dados por patch e leitura ao vivo ainda exigem validação.')
rep("MainActivity.kt",'Apoio offline por classe; não substitui a análise visual da partida.','Simulador do motor local. Informe apenas dados conhecidos; ouro total não é orçamento de compra.')
rep("MainActivity.kt",'for (c in listOf(armor, healing, magic)) ui.addView(c)','''for (c in listOf(armor, healing, magic)) ui.addView(c)
        val rivals=input("Inimigos (nomes, vírgula)","")
        val enemyItems=input("Itens adversários agregados (nomes, vírgula)","")
        val owned=input("Itens já comprados (nomes, vírgula)","")
        val minute=input("Minuto da partida (opcional)","")
        val budget=input("Ouro disponível (opcional)","")
        for(f in listOf(rivals,enemyItems,owned,minute,budget)){ui.addView(f);ui.addView(space(6))}''')
rep("MainActivity.kt",'''            HeroCatalog.itemSuggestions(hero, armor.isChecked, healing.isChecked, magic.isChecked).forEach { tip ->
                result.addView(label(tip.item, 16, cyan, true))
                result.addView(label(tip.reason, 12, white))
                result.addView(space(10))
            }''','''            val foes=rivals.text.toString().split(",",";").mapNotNull{HeroCatalog.find(it)}.distinct().take(5)
            val items=StrategicEngine.parseItems(enemyItems.text.toString())
            // Manual items are aggregate observations, not assigned to invented owners.
            val advice=StrategicEngine.recommend(StrategicEngine.State(hero,
                owned.text.toString().split(",",";").map{it.trim()}.filter{it.isNotBlank()},
                foes.map{StrategicEngine.Player(it)},minute=minute.text.toString().toIntOrNull(),
                budget=budget.text.toString().toIntOrNull(),inventoryKnown=true,
                armorHint=armor.isChecked||items.any{it in setOf("Blade Armor","Antique Cuirass","Dominance Ice")},
                healingHint=healing.isChecked,magicHint=magic.isChecked))
            result.addView(label("PRÓXIMO: "+(advice.next?:"Aguardando"),18,cyan,true))
            result.addView(label(advice.reason,12,white))
            result.addView(label("Plano: "+advice.plan.joinToString(" → "),13,gold,true))
            result.addView(label("Alternativas: "+advice.alternatives.joinToString(", "),12,muted))
            result.addView(label(advice.threat,12,muted))
            result.addView(label(advice.purchase.orEmpty(),12,muted))
            result.addView(label("Combinações avaliadas: "+advice.evaluated,12,muted))''')
rep("HeroCatalog.kt",'                h to score','                score += DraftRules.reasons(h,allies,enemies).size*5\n                h to score')
rep("HeroCatalog.kt",'            Pick(hero, rationale.joinToString("; ") + ".")','            rationale.addAll(DraftRules.reasons(hero,allies,enemies))\n            Pick(hero, rationale.joinToString("; ") + ". Hipóteses experimentais.")')
rep("UploadClient.kt",'    val frameCount: Int = 0','    val frameCount: Int = 0,\n    val buildPlan: List<String> = emptyList(),\n    val purchaseDetail: String = ""')
rep("OfflineTextAnalyzer.kt",'val selected=manual?:self','val selected=self?:manual')
rep("OfflineTextAnalyzer.kt",'val hero=manual?:self','val hero=self?:manual')
rep("OfflineTextAnalyzer.kt",'            val advice=LiveBuildEngine.recommend(hero,foes,enemyItems,owned)','''            fun strategic(p:ObservedPlayer):StrategicEngine.Player {
                val k=p.kda?.split("/")?.mapNotNull{it.toIntOrNull()}.orEmpty()
                return StrategicEngine.Player(p.hero,p.items,p.gold?.toIntOrNull(),k.getOrNull(0),k.getOrNull(1),k.getOrNull(2))
            }
            val advice=StrategicEngine.recommend(StrategicEngine.State(hero,owned,
                rivals.map(::strategic),recent.filter{it.team=="Aliado"&&it.hero!=hero}.map(::strategic),
                ownGold=own?.gold?.toIntOrNull(),minute=matchClock?.substringBefore(":")?.toIntOrNull(),
                inventoryKnown=false))''')
rep("OfflineTextAnalyzer.kt",'return AnalysisResult(state,advice.item,advice.reason,observed,','return AnalysisResult(state,advice.next,advice.reason,observed,')
rep("OfflineTextAnalyzer.kt",'players=recent, alternatives=advice.alternatives.split(" / ").filter{it.isNotBlank()},\n                threatSummary=advice.context,matchTime=matchClock,observedAt=lastSeen,frameCount=frames)','''players=recent, alternatives=advice.alternatives,
                threatSummary=advice.threat,matchTime=matchClock,observedAt=lastSeen,frameCount=frames,
                buildPlan=advice.plan,purchaseDetail=advice.purchase.orEmpty())''')
rep("OverlayPanel.kt",'private val alternate=label("",11,muted)','private val alternate=label("",11,muted)\n    private val plan=label("",11,gold)\n    private val economy=label("",10,muted)')
rep("OverlayPanel.kt",'            addView(alternate)','            addView(alternate);addView(plan);addView(economy)')
rep("OverlayPanel.kt",'        threats.text=result.threatSummary.ifBlank','        plan.text=if(result.buildPlan.isEmpty())"" else "PLANO CANDIDATO\\n"+result.buildPlan.joinToString(" → ")\n        economy.text=result.purchaseDetail\n        threats.text=result.threatSummary.ifBlank')
rep("OverlayPanel.kt",'            threats.text="Observações vencidas"','            threats.text="Observações vencidas"\n            plan.text="";economy.text="";alternate.text=""')
# Guard model selection and show local results without cloud calls in App Base.
rep("ProjectionService.kt",'val initial = LiveBuildEngine.recommend(chosenHero, emptyList(), emptyList())','val initial = StrategicEngine.recommend(StrategicEngine.State(chosenHero))')
rep("ProjectionService.kt",'item = initial.item,','item = initial.next,')
rep("ProjectionService.kt",'visionStatus = "Dados inimigos não detectados. Abra o placar durante o MLBB.", frameCount=0','visionStatus = "Dados inimigos não detectados. Abra o placar durante o MLBB.", frameCount=0,buildPlan=initial.plan,purchaseDetail=initial.purchase.orEmpty()')
rep("ProjectionService.kt",'visionStatus = "Servidor indisponível; OCR local experimental"','visionStatus = "Modo I.A. indisponível; App Base local experimental"')
# Keep cloud recommendations and add the server's plan, without pretending new local analysis from stale data.
rep("UploadClient.kt",'            val readSource =','            val plan=response.optJSONArray("build_plan")\n            val readSource =')
rep("UploadClient.kt",'players=players,draftPicks=draft,','players=players,draftPicks=draft,buildPlan=if(plan==null)emptyList()else(0 until plan.length()).map{plan.optString(it)},')
g=Path("android-app/app/build.gradle.kts");s=g.read_text().replace("versionCode = 11","versionCode = 12").replace('versionName = "0.7.2"','versionName = "0.8.0"')
s=s.replace('dependencies {','dependencies {\n    testImplementation("junit:junit:4.13.2")')
g.write_text(s)
