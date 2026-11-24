from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
def rep(n,a,z):
 p=b/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,z,1))
p="DetailedMatcher.kt"
rep(p,'val size: Int)','val size: Int,val margin:Double=0.0,val slot:Int=-1)')
rep(p,'return Hit(best.name,score,x,y,size)','return Hit(best.name,score,x,y,size,second-score)')
rep(p,"match(b,(i.x*w).toInt(),(i.y*h).toInt(),(i.size*w).toInt().coerceAtLeast(12),'i')","match(b,(i.x*w).toInt(),(i.y*h).toInt(),(i.size*w).toInt().coerceAtLeast(12),'i')?.copy(slot=idx)")
rep(p,"(g.itemSize*w).toInt(),'i')","(g.itemSize*w).toInt(),'i')?.copy(slot=k)")
p="OfflineTextAnalyzer.kt"
rep(p,'private var previousBoard=emptyList<DetailedMatcher.Row>()','''private var previousBoard=emptyList<DetailedMatcher.Row>()
    private var previousBoardAt=0L
    private val history=Evidence14.History()
    private var historyText=""
    private var cachedState:StrategicEngine.State?=null
    private var cachedAdvice:StrategicEngine.Result?=null''')
rep(p,'previousBoard=emptyList();matchClock=null;statsCandidates=emptyMap();clockCandidate=null','previousBoard=emptyList();previousBoardAt=0L;matchClock=null;statsCandidates=emptyMap();clockCandidate=null;history.clear();historyText="";cachedState=null;cachedAdvice=null')
# Keep stale observations separate from new candidates.
rep(p,'if(lastSeen>0&&now-lastSeen>90_000){reset()}','if(lastSeen>0&&!Evidence14.fresh(now,lastSeen)){reset()}')
rep(p,'if(board.rows.isNotEmpty()) {\n                val highlighted','''if(board.rows.isNotEmpty()) {
                if(!Evidence14.pairFresh(now,previousBoardAt)){
                    previousBoard=emptyList();previousHighlight=null;statsCandidates=emptyMap();clockCandidate=null
                }
                val highlighted''')
rep(p,'self=null; recent=emptyList(); lastSeen=0L','self=null; recent=emptyList(); lastSeen=0L;history.clear();historyText="";cachedState=null;cachedAdvice=null')
rep(p,'val items=r.items.filter{x->old.items.any{it.name==x.name}}','''val stable=Evidence14.stableSlots(old.items.map{Evidence14.Slot(it.slot,it.name,it.distance,it.margin)},r.items.map{Evidence14.Slot(it.slot,it.name,it.distance,it.margin)})
                        val items=r.items.filter{x->stable.any{it.index==x.slot&&it.name==x.name}}''')
rep(p,'                    statsCandidates=nextStats\n                    lastSeen=now','''                    statsCandidates=nextStats
                    val frame=Evidence14.Frame(now,ObservationParser.clockSeconds(matchClock),accepted.mapNotNull{r->
                        val h=r.hero?.name?:return@mapNotNull null
                        val previous=previousBoard.singleOrNull{it.side==r.side&&it.index==r.index}?:return@mapNotNull null
                        val observed=recent.singleOrNull{it.hero==h}?:return@mapNotNull null
                        val stable=Evidence14.stableSlots(previous.items.map{Evidence14.Slot(it.slot,it.name,it.distance,it.margin)},r.items.map{Evidence14.Slot(it.slot,it.name,it.distance,it.margin)})
                        Evidence14.Player(h,r.side,r.index,stable,observed.gold?.toIntOrNull(),observed.kda)
                    })
                    val update=history.record(frame)
                    historyText="Histórico local: "+update.frames+" quadros confirmados"+(if(update.reset)" • novo segmento" else "")
                    if(update.changes.isNotEmpty())historyText+=" • "+update.changes.take(3).joinToString("; "){c->c.hero+" "+(c.replacements+listOfNotNull(c.goldDelta?.takeIf{it>0}?.let{"ouro total +"+it},if(c.kdaChanged)"KDA alterado" else null)).joinToString(", ")}
                    lastSeen=now''')
rep(p,'                previousBoard=board.rows','                previousBoard=board.rows;previousBoardAt=now')
rep(p,'                previousBoard=emptyList()\n                previousHighlight=null','                previousBoard=emptyList();previousBoardAt=0L\n                previousHighlight=null')
rep(p,'if(lastSeen>0&&now-lastSeen>90_000){reset()}','if(lastSeen>0&&!Evidence14.fresh(now,lastSeen)){reset()}')
rep(p,'val advice=Strategy13.recommend(StrategicEngine.State(hero,owned,','val strategyState=StrategicEngine.State(hero,owned,')
rep(p,'rank=prefs.getString("strategy_rank","all")?:"all"))','''rank=prefs.getString("strategy_rank","all")?:"all")
            val advice=if(cachedState==strategyState&&cachedAdvice!=null)cachedAdvice!! else Strategy13.recommend(strategyState).also{cachedState=strategyState;cachedAdvice=it}''')
rep(p,'(if(candidateSelf!=null)" • destaque: "+candidateSelf else "")+(if(shopNames.isNotEmpty())" • nomes na loja: "+shopNames.joinToString(", ") else ""),','(if(candidateSelf!=null)" • destaque: "+candidateSelf else "")+(if(shopNames.isNotEmpty())" • nomes na loja: "+shopNames.joinToString(", ") else "")+(if(historyText.isNotBlank())" • "+historyText else ""),')
# If game foreground is lost after processing, do not show an old result. Keep existing explicit consent.
rep("ProjectionService.kt",'                    completedFrames++\n                    val enriched=','''                    if(paused||!isGameForeground()){
                        showStatus("Quadro descartado: MLBB não confirmado em foco após análise");return@execute
                    }
                    completedFrames++
                    val enriched=''')
p="OverlayPanel.kt"
rep(p,'if(result.observedAt>0 && SystemClock.elapsedRealtime()-result.observedAt>90_000){','if(result.observedAt>0 && !Evidence14.fresh(SystemClock.elapsedRealtime(),result.observedAt)){')
# Never present stale item images as live once focus is lost. This does not mutate match evidence.
rep(p,'clock.text="Captura/observações interrompidas • dados podem estar antigos"','''clock.text="Captura/observações interrompidas • dados podem estar antigos"
            lastResult=null
            item.text="Aguardando leitura atual";itemArt.visibility=View.GONE
            reason.text="Análise interrompida; não usar a sugestão anterior como atual."
            plan.text="";economy.text="";alternate.text="";threats.text="Sem leitura atual"
            playerRows.removeAllViews();heroPortrait.setImageBitmap(null);heroPortrait.visibility=View.GONE''')
