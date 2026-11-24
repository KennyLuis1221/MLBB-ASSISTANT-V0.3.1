package com.mlbb.assistant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.TimeUnit
import java.text.Normalizer
import kotlin.math.abs

/** Pipeline local com evidências separadas de hipóteses e memória limitada à sessão. */
class OfflineTextAnalyzer(context: Context) {
    private val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val matcher=DetailedMatcher(context)
    private val prefs=context.getSharedPreferences("settings",Context.MODE_PRIVATE)
    private var recent=emptyList<ObservedPlayer>()
    private var lastSeen=0L
    private var candidateSelf:String?=null
    private var previousHighlight:String?=null
    private var self:String?=null
    private var frames=0
    private var previousBoard=emptyList<DetailedMatcher.Row>()
    private var matchClock:String?=null
    private fun norm(s:String)=Normalizer.normalize(s,Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"),"").lowercase()
    private fun reset() {
        recent=emptyList();lastSeen=0L;candidateSelf=null;previousHighlight=null;self=null
        previousBoard=emptyList();matchClock=null
    }
    fun analyze(jpeg:ByteArray,heroHint:String):AnalysisResult {
        val b=BitmapFactory.decodeByteArray(jpeg,0,jpeg.size)?:return failure("Quadro inválido")
        try {
            frames++
            val ocr=Tasks.await(recognizer.process(InputImage.fromBitmap(b,0)),8,TimeUnit.SECONDS)
            val t=norm(ocr.text)
            val now=SystemClock.elapsedRealtime()
            val tokens=ocr.textBlocks.flatMap{it.lines}.flatMap{it.elements}
            val post=listOf("defeat","victory","derrota","vitoria","return to lobby","retornar ao lobby").any{
                Regex("\\b"+it+"\\b").containsMatchIn(t)}
            if(post) {
                reset()
                return AnalysisResult("Resultado final",null,"Partida encerrada; dados descartados.",0,
                    "OCR local",null,0,0,"Nenhuma compra durante resultado final",frameCount=frames)
            }
            val draft=listOf("selecao de herois","selecionar heroi","escolha de herois","hero selection",
                "pick a hero","ban hero","banir heroi","escolha seu heroi","banimento","your turn","sua vez")
                .any{it in t}
            val shop=listOf("equipamentos","equipment","loja","shop","recomendado").any{it in t}
            val statsLabel=listOf("estatisticas","statistics","atributos","attributes","scoreboard","desempenho da equipe").any{it in t}
            val clocks=tokens.filter{el->Regex("^[0-9]{1,2}:[0-5][0-9]$").matches(el.text)&&
                (el.boundingBox?.centerY()?:b.height)<b.height*.24}.map{it.text}
            val numeric=tokens.count{it.text.matches(Regex("[0-9]{3,5}"))}
            val board=if(!draft && b.width>b.height && (statsLabel||numeric>=6))
                matcher.board(b) else DetailedMatcher.Board(emptyList(),0)
            val manual=HeroCatalog.find(heroHint.ifBlank{prefs.getString("hero_hint","").orEmpty()})
            val named=HeroCatalog.heroes.filter{h->ocr.text.lines().any{norm(it.trim())==norm(h)}}
            if(draft) {
                reset()
                val lane=prefs.getString("lane_hint","Selva")?:"Selva"
                val left=mutableListOf<String>();val right=mutableListOf<String>()
                for (line in ocr.textBlocks.flatMap{it.lines}) {
                    val name=HeroCatalog.find(line.text.trim())?:continue
                    val x=line.boundingBox?.centerX()?:continue
                    if(x<b.width*.28)left.add(name) else if(x>b.width*.72)right.add(name)
                }
                // Exclude visible names conservatively; side association remains a hypothesis.
                val excluded=(left+right+named).distinct().joinToString(",")
                val picks=HeroCatalog.suggest(lane,"",excluded,"").take(3)
                return AnalysisResult("Seleção de personagens",null,
                    "Função: $lane. Nomes visíveis excluídos por cautela; bans/counters ainda não validados.",
                    named.size,"OCR local • sugestões preliminares",null,0,0,
                    "Nomes lidos: "+named.joinToString(", ").ifBlank{"nenhum confirmado"},
                    draftPicks=picks.map{it.hero}, alternatives=picks.map{it.reason},frameCount=frames,
                    observedAt=now)
            }
            if(board.rows.isNotEmpty()) {
                val highlighted=matcher.highlight(b,board.rows)?.hero?.name
                candidateSelf=highlighted
                if(highlighted!=null&&highlighted==previousHighlight)self=highlighted
                previousHighlight=highlighted
                val accepted=board.rows.filter{r->r.hero!=null&&previousBoard.any{
                    it.side==r.side&&it.index==r.index&&it.hero?.name==r.hero?.name}}
                val selected=manual?:self
                val ownRow=accepted.singleOrNull{it.hero?.name==selected}
                val side=ownRow?.side
                if(accepted.isNotEmpty()) {
                    recent=accepted.map{r->
                        val h=r.hero!!
                        val old=previousBoard.first{it.side==r.side&&it.index==r.index}
                        val items=r.items.filter{x->old.items.any{it.name==x.name}}
                        val region=tokens.filter{el->
                            val q=el.boundingBox?:return@filter false
                            abs(q.centerY()-r.centerY)<b.height*.055 &&
                                (if(r.side==0)q.centerX()<b.width*.5 else q.centerX()>b.width*.5)
                        }.map{it.text}
                        val goldCandidates=region.filter{it.matches(Regex("[0-9]{4,5}"))}
                        val gold=goldCandidates.singleOrNull()
                        val kda=region.firstOrNull{it.matches(Regex("[0-9]{1,2}/[0-9]{1,2}/[0-9]{1,2}"))}
                        ObservedPlayer(h.name,if(side==null)"Lado "+(r.side+1)
                            else if(r.side==side)"Aliado" else "Inimigo",items.map{it.name},
                            gold,kda,matcher.thumbnail(b,h),
                            items.mapNotNull{it2->matcher.thumbnail(b,it2)?.let{img->it2.name to img}}.toMap())
                    }
                    lastSeen=now
                }
                previousBoard=board.rows
                if(clocks.size==1)matchClock=clocks[0]
            }
            if(lastSeen>0&&now-lastSeen>90_000){reset()}
            val hero=manual?:self
            val rivals=recent.filter{it.team=="Inimigo"}
            val own=recent.singleOrNull{it.hero==hero}
            val foes=rivals.map{it.hero}
            val enemyItems=rivals.flatMap{it.items}
            val owned=own?.items?:emptyList()
            val advice=LiveBuildEngine.recommend(hero,foes,enemyItems,owned)
            val state=when {
                board.rows.isNotEmpty()&&recent.isEmpty()->"Placar candidato • confirmando próximo quadro"
                board.rows.isNotEmpty()->"Placar • observações repetidas"
                statsLabel->"Placar lido, ícones insuficientes"
                shop->"Loja • itens escritos não são compras"
                else->"Captura ativa • abra o placar"
            }
            val observed=recent.size+recent.sumOf{it.items.size}
            val explanation=if(manual!=null&&self==null)"Herói informado manualmente"
                else if(self!=null)"Herói provável por destaque repetido" else "Herói não identificado"
            return AnalysisResult(state,advice.item,advice.reason,observed,
                "Visão local experimental • "+explanation,hero,rivals.size,enemyItems.size,
                "OCR: "+tokens.size+" elementos • "+matcher.count+" referências • "+board.support+" candidatos"+
                    (if(candidateSelf!=null)" • destaque: "+candidateSelf else ""),
                players=recent, alternatives=advice.alternatives.split(" / ").filter{it.isNotBlank()},
                threatSummary=advice.context,matchTime=matchClock,observedAt=lastSeen,frameCount=frames)
        }catch(e:Exception){return failure("Falha de leitura: "+e.javaClass.simpleName)}
        finally{b.recycle()}
    }
    private fun failure(message:String)=AnalysisResult(message,null,"Sem leitura suficiente.",0,
        "Visão local",null,0,0,"Verifique o contador de quadros e a permissão de captura.",frameCount=frames)
    fun close(){recognizer.close();reset()}
}
