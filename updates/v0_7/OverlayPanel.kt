package com.mlbb.assistant

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Painel inspirado na prévia aprovada: dados reais em cartões separados.
 * Sem imagens fictícias, percentuais inventados nem toques no jogo. */
class OverlayPanel(private val context:Context,private val onPause:()->Unit,private val onStop:()->Unit) {
    private val wm=context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val navy=0xF50A1526.toInt();private val cyan=0xFF39CFFF.toInt()
    private val gold=0xFFECC16B.toInt();private val white=0xFFF0F6FF.toInt()
    private val muted=0xFFB6C9DE.toInt()
    private var attached=false;private var expanded=false;private var paused=false
    private var lastResult:AnalysisResult?=null
    private val surface=column().apply{setPadding(dp(7),dp(6),dp(7),dp(6));background=bg(navy,gold,15)}
    private val heading=label("M",18,gold,true)
    private val content=column()
    private val scroll=ScrollView(context).apply { isFillViewport=false;addView(content) }
    private val status=label("Aguardando captura",12,cyan,true)
    private val hero=label("Não identificado",16,white,true)
    private val heroPortrait=ImageView(context).apply{scaleType=ImageView.ScaleType.CENTER_CROP;background=bg(0xFF172D46.toInt(),cyan,24);clipToOutline=true}
    private val heroSource=label("Sem evidência visual",10,muted)
    private val title=label("PRÓXIMA COMPRA RECOMENDADA",11,cyan,true)
    private val item=label("Aguardando informações",19,gold,true)
    private val itemArt=ImageView(context).apply { scaleType=ImageView.ScaleType.CENTER_CROP;background=bg(0xFF132C49.toInt(),gold,9);clipToOutline=true }
    private val reason=label("Abra o placar durante a partida.",12,white)
    private val threats=label("Sem ameaças confirmadas",11,muted)
    private val playerRows=column()
    private val alternate=label("",11,muted)
    private val clock=label("Quadros analisados: 0",10,muted)
    private val diagnostic=label("",10,muted)
    private val p=WindowManager.LayoutParams(dp(62),WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_SECURE,
        android.graphics.PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.END;x=dp(8);y=dp(70) }

    fun show(){
        if(attached)return
        val header=row().apply{
            gravity=Gravity.CENTER_VERTICAL;addView(heading,LinearLayout.LayoutParams(0,-2,1f))
            setOnTouchListener(Drag())
        }
        surface.addView(header)
        surface.addView(scroll)
        content.addView(card(cyan).apply{addView(status)})
        content.addView(card(0xFF345776.toInt()).apply{
            addView(label("SEU HERÓI",10,muted,true))
            addView(row().apply{gravity=Gravity.CENTER_VERTICAL;addView(heroPortrait,LinearLayout.LayoutParams(dp(42),dp(42)))
                addView(column().apply{setPadding(dp(7),0,0,0);addView(hero);addView(heroSource)},LinearLayout.LayoutParams(0,-2,1f))})
        })
        content.addView(card(cyan).apply{
            addView(title)
            addView(row().apply{gravity=Gravity.CENTER_VERTICAL;addView(itemArt,LinearLayout.LayoutParams(dp(40),dp(40)))
                addView(item,LinearLayout.LayoutParams(0,-2,1f))})
            addView(label("JUSTIFICATIVA ESTRATÉGICA",10,cyan,true));addView(reason)
            addView(alternate)
        })
        content.addView(card(0xFF345776.toInt()).apply{
            addView(label("AMEAÇAS DETECTADAS",11,cyan,true));addView(threats)
        })
        content.addView(card(0xFF345776.toInt()).apply{
            addView(label("JOGADORES E EQUIPAMENTOS OBSERVADOS",10,cyan,true));addView(playerRows)
        })
        content.addView(card(0xFF345776.toInt()).apply{
            addView(clock);addView(diagnostic)
        })
        val pause=action("Pausar"){paused=!paused;onPause()}
        pause.setOnClickListener{paused=!paused;pause.text=if(paused)"Retomar" else "Pausar";onPause()}
        content.addView(row().apply{
            addView(pause,LinearLayout.LayoutParams(0,dp(36),1f))
            addView(action("Encerrar"){onStop()},LinearLayout.LayoutParams(0,dp(36),1f))
        })
        scroll.visibility=View.GONE
        wm.addView(surface,p);attached=true
    }
    private fun toggle(){
        expanded=!expanded
        heading.text=if(expanded)"◈  MLBB ASSISTANT   −" else if(lastResult!=null)"M •" else "M"
        scroll.visibility=if(expanded)View.VISIBLE else View.GONE
        val metrics=context.resources.displayMetrics
        p.width=if(expanded) minOf(dp(320), (metrics.widthPixels*.44).toInt().coerceAtLeast(dp(220))) else dp(62)
        scroll.layoutParams=LinearLayout.LayoutParams(-1,if(expanded) minOf(dp(460),(metrics.heightPixels*.75).toInt()) else 0)
        clamp();if(attached)wm.updateViewLayout(surface,p)
    }
    fun showStatus(message:String){
        status.text=message.take(180)
        diagnostic.text=message.take(180)
        if(message.contains("bloque",true)||message.contains("paus",true)||message.contains("erro",true)){
            clock.text="Captura/observações interrompidas • dados podem estar antigos"
        }
    }
    fun showResult(result:AnalysisResult){
        lastResult=result
        status.text=result.status
        if(!expanded)heading.text="M •"
        hero.text=result.hero?:"Não identificado"
        heroSource.text=result.source
        val own=result.players.singleOrNull{it.hero.equals(result.hero,true)}
        heroPortrait.setImageBitmap(own?.portrait)
        title.text=if(result.draftPicks.isNotEmpty())"SMART HERO PICK" else "PRÓXIMA COMPRA RECOMENDADA"
        item.text=if(result.draftPicks.isNotEmpty())result.draftPicks.mapIndexed{i,s->"${i+1}. $s"}.joinToString("\n") else result.item?:"Aguardando dados"
        itemArt.setImageBitmap(result.players.asSequence().mapNotNull{it.itemImages[result.item]}.firstOrNull())
        itemArt.visibility=if(result.item!=null&&result.draftPicks.isEmpty())View.VISIBLE else View.GONE
        reason.text=result.reason
        alternate.text=if(result.alternatives.isEmpty())"" else result.alternatives.take(3).joinToString("\n")
        threats.text=result.threatSummary.ifBlank{"Sem evidência de ameaças"}
        playerRows.removeAllViews()
        if(result.players.isEmpty())playerRows.addView(label("Nenhum jogador confirmado por leituras repetidas.",11,muted))
        for(r in result.players.take(10)){
            playerRows.addView(row().apply{
                gravity=Gravity.CENTER_VERTICAL
                val iv=ImageView(context).apply{setImageBitmap(r.portrait);scaleType=ImageView.ScaleType.CENTER_CROP}
                addView(iv,LinearLayout.LayoutParams(dp(28),dp(28)))
                addView(column().apply{
                    addView(label(r.hero+" • "+r.team,11,if(r.team=="Inimigo")0xFFFF9A9A.toInt() else white,true))
                    val stats=listOfNotNull(r.gold?.let{"Ouro provável $it"},r.kda?.let{"KDA $it"}).joinToString("  |  ")
                    if(stats.isNotBlank())addView(label(stats,10,muted))
                    if(r.items.isNotEmpty()){
                        addView(row().apply{for(name in r.items.take(6)){
                            val pic=ImageView(context).apply{setImageBitmap(r.itemImages[name]);contentDescription=name}
                            addView(pic,LinearLayout.LayoutParams(dp(21),dp(21)))
                        }})
                        addView(label(r.items.joinToString(", "),9,muted))
                    }
                },LinearLayout.LayoutParams(0,-2,1f))
            })
        }
        val age=if(result.observedAt>0) ((SystemClock.elapsedRealtime()-result.observedAt).coerceAtLeast(0)/1000).toString()+"s" else "sem observação"
        clock.text="Quadros: ${result.frameCount}  •  Última leitura: $age"+
            (result.matchTime?.let{"  •  Tempo $it"}?:"")
        diagnostic.text=result.visionStatus
    }
    fun remove(){if(attached){runCatching{wm.removeView(surface)};attached=false}}
    private fun clamp(){
        val m=context.resources.displayMetrics
        p.x=p.x.coerceIn(0,(m.widthPixels-p.width).coerceAtLeast(0))
        p.y=p.y.coerceIn(0,(m.heightPixels-dp(65)).coerceAtLeast(0))
    }
    private inner class Drag:View.OnTouchListener{
        var ix=0;var iy=0;var dx=0f;var dy=0f;var moved=false
        override fun onTouch(v:View,e:MotionEvent):Boolean{
            when(e.actionMasked){
                MotionEvent.ACTION_DOWN->{ix=p.x;iy=p.y;dx=e.rawX;dy=e.rawY;moved=false;return true}
                MotionEvent.ACTION_MOVE->{
                    val x=(e.rawX-dx).toInt();val y=(e.rawY-dy).toInt()
                    if(kotlin.math.abs(x)>dp(6)||kotlin.math.abs(y)>dp(6))moved=true
                    if(moved){p.x=ix-x;p.y=iy+y;clamp();if(attached)wm.updateViewLayout(surface,p)}
                    return true
                }
                MotionEvent.ACTION_UP->{if(!moved)toggle();return true}
            };return false
        }
    }
    private fun column()=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL}
    private fun row()=LinearLayout(context).apply{orientation=LinearLayout.HORIZONTAL}
    private fun card(stroke:Int)=column().apply{
        background=bg(0xFF11223B.toInt(),stroke,9);setPadding(dp(6),dp(5),dp(6),dp(5))
        layoutParams=LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)}
    }
    private fun label(s:String,size:Int,color:Int,bold:Boolean=false)=TextView(context).apply{
        text=s;textSize=size.toFloat();setTextColor(color);setPadding(dp(2),dp(2),dp(2),dp(2))
        if(bold)setTypeface(null,Typeface.BOLD)
    }
    private fun action(s:String,click:()->Unit)=label(s,12,white,true).apply{
        gravity=Gravity.CENTER;background=bg(0xFF1B3957.toInt(),0xFF315D83.toInt(),8);setOnClickListener{click()}
    }
    private fun bg(fill:Int,stroke:Int,radius:Int)=GradientDrawable().apply{setColor(fill);setStroke(dp(1),stroke);cornerRadius=dp(radius).toFloat()}
    private fun dp(n:Int)=(n*context.resources.displayMetrics.density).toInt()
}
