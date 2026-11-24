from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant/MainActivity.kt")
s=b.read_text()
needle='                for(v in suggestions){result.addView(label(v.hero,15,gold,true));result.addView(label(v.reason,12,white))}'
assert needle in s
s=s.replace(needle,needle+'''
                val ownNames=parse(allies.text.toString())
                val enemyNames=parse(enemies.text.toString())
                val banned=parse(bans.text.toString())
                val rank=ranks.selectedItem.toString()
                val chosen=picks.firstOrNull()?.hero
                if(chosen!=null){
                    val laneKey=mapOf("Selva" to "Jungle","EXP" to "Exp","Meio" to "Mid","Ouro" to "Gold","Roam" to "Roam")[lane.selectedItem.toString()]
                    val lock=if(laneKey!=null)mapOf(laneKey to chosen)else emptyMap()
                    val scenario=Team15.report(db,ownNames+chosen,enemyNames,rank,lock)
                    if(scenario!=null){
                        result.addView(label("CENÁRIO DE COMPOSIÇÃO COM "+chosen,13,cyan,true))
                        result.addView(label("Posições possíveis: "+scenario.allies.byLane.entries.joinToString(" • "){it.key+": "+it.value},12,white))
                        result.addView(label(scenario.allyConditions.joinToString(" • "),12,white))
                        result.addView(label("Adversário: "+scenario.enemyConditions.joinToString(" • "),12,muted))
                        result.addView(label("Pares de confronto medidos: "+scenario.counters.measured+"; por regra: "+scenario.counters.estimated,12,muted))
                        for(r in scenario.risks)result.addView(label("Risco: "+r.hero+" contra "+r.against+" • "+(r.measured?.let{it.toString()+" p.p. observados"}?:r.reasons.joinToString(", ")),12,white))
                        val responses=Team15.responses(db,ownNames,enemyNames,banned,chosen,rank)
                        if(responses.isNotEmpty()){
                            result.addView(label("RESPOSTAS ADVERSÁRIAS POSSÍVEIS • CENÁRIOS, NÃO PREVISÃO",12,cyan,true))
                            for(r in responses)result.addView(label(r.hero+" pode ameaçar "+r.threatened+" • "+(r.measured?.let{it.toString()+" p.p. observados"}?:"estimativa por características"),12,white))
                        }
                    }
                }
''',1)
b.write_text(s)
