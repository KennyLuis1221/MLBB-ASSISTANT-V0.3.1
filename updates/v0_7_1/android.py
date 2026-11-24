"""Fix stale overlay state, add local image replay, set real endpoint, no secrets."""
from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
def patch(name,a,c):
    p=b/name;s=p.read_text();assert a in s,(name,a);p.write_text(s.replace(a,c,1))
p="OfflineTextAnalyzer.kt"
patch(p,'            if(board.rows.isNotEmpty()) {','''            if(lastSeen>0&&now-lastSeen>90_000){reset()}
            if(board.rows.isNotEmpty()) {''')
patch(p,'                val highlighted=matcher.highlight(b,board.rows)?.hero?.name','''                val highlighted=matcher.highlight(b,board.rows)?.hero?.name
                if(self!=null && highlighted!=null && self!=highlighted) {
                    self=null; recent=emptyList(); lastSeen=0L
                }''')
patch(p,'                previousBoard=board.rows','''                previousBoard=board.rows''')
# Reject non-consecutive matches: a non-board frame invalidates previous candidate.
patch(p,'                if(clocks.size==1)matchClock=clocks[0]\n            }','''                if(clocks.size==1)matchClock=clocks[0]
            } else {
                previousBoard=emptyList()
                previousHighlight=null
            }''')
p="OverlayPanel.kt"
patch(p,'    private var lastResult:AnalysisResult?=null','''    private var lastResult:AnalysisResult?=null
    private val timer=android.os.Handler(android.os.Looper.getMainLooper())
    private val tick=object:Runnable{
        override fun run(){
            if(!attached)return
            lastResult?.let { refreshAge(it) }
            timer.postDelayed(this,1000)
        }
    }''')
patch(p,'        wm.addView(surface,p);attached=true','        wm.addView(surface,p);attached=true;timer.post(tick)')
patch(p,'    fun remove(){if(attached){runCatching{wm.removeView(surface)};attached=false}}','    fun remove(){timer.removeCallbacks(tick);if(attached){runCatching{wm.removeView(surface)};attached=false}}')
patch(p,'        val age=if(result.observedAt>0)','''        diagnostic.text=result.visionStatus
        refreshAge(result)
    }
    private fun refreshAge(result:AnalysisResult) {
        val age=if(result.observedAt>0)''')
patch(p,'        diagnostic.text=result.visionStatus\n    }\n    fun remove()', '''        if(result.observedAt>0 && SystemClock.elapsedRealtime()-result.observedAt>90_000){
            status.text="Dados antigos • abra o placar"
            item.text="Aguardando leitura atual"
            reason.text="As observações venceram. Não use esta sugestão como análise atual."
            itemArt.visibility=View.GONE
            threats.text="Observações vencidas"
            playerRows.removeAllViews()
            playerRows.addView(label("Abra o placar para atualizar os jogadores.",11,muted))
        }
    }
    fun remove()''')
patch(p,'p.y=p.y.coerceIn(0,(m.heightPixels-dp(65)).coerceAtLeast(0))','p.y=p.y.coerceIn(0,(m.heightPixels-(if(expanded) scroll.layoutParams.height+dp(45) else dp(65))).coerceAtLeast(0))')
patch("UploadClient.kt",'            conn.requestMethod = "POST"','            conn.instanceFollowRedirects=false\n            conn.requestMethod = "POST"')
patch("ProjectionService.kt",'        token = prefs.getString("token", "") ?: ""','        token = prefs.getString("token", "") ?: ""\n        if(url.isNotBlank()&&!url.startsWith("https://")){url="";token=""}')
patch("ProjectionService.kt",'                    showStatus("Falha na análise: $detail")','                    showStatus("Falha na análise: "+error.javaClass.simpleName)')
patch("ProjectionService.kt",'                showStatus("Herói não identificado. Para teste, selecione-o na tela inicial.")','                showStatus("Herói não identificado. Abra o placar; teste local disponível na tela inicial.")')
# Never retain diagnostic screenshots or send them to server.
p="MainActivity.kt"
patch(p,'        ui.addView(button("Configurações avançadas  ⚙") { showAdvanced() })','''        ui.addView(button("TESTAR RECONHECIMENTO COM PRINT") {
            val pick=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
                addCategory(Intent.CATEGORY_OPENABLE);type="image/*"
            }
            startActivityForResult(pick,7302)
        })
        ui.addView(button("Configurações avançadas  ⚙") { showAdvanced() })''')
patch(p,'        super.onActivityResult(requestCode, resultCode, data)','''        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==7302){
            if(resultCode==RESULT_OK)data?.data?.let{ uri->showReplay(uri) }
            return
        }''')
patch(p,'    private fun back(ui: LinearLayout, name: String) {','''    private fun showReplay(uri:Uri){
        page="replay"
        val ui=root()
        back(ui,"DIAGNÓSTICO LOCAL")
        ui.addView(label("Leitura de captura salva",23,gold,true))
        ui.addView(label("Sem internet. Uma imagem estática é processada duas vezes para testar o detector, não a captura ao vivo.",12,muted))
        val state=label("Processando imagem...",14,cyan,true)
        ui.addView(state)
        val result=card()
        ui.addView(result)
        Thread{
            var analyzer:OfflineTextAnalyzer?=null
            try{
                val opts=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true}
                contentResolver.openInputStream(uri)?.use{android.graphics.BitmapFactory.decodeStream(it,null,opts)}
                require(opts.outWidth>0&&opts.outHeight>0){"Imagem inválida"}
                var sample=1
                while(maxOf(opts.outWidth,opts.outHeight)/sample>1800)sample*=2
                val bmp=contentResolver.openInputStream(uri)?.use{
                    android.graphics.BitmapFactory.decodeStream(it,null,android.graphics.BitmapFactory.Options().apply{inSampleSize=sample})
                }?:error("Não foi possível abrir a imagem")
                val bytes=java.io.ByteArrayOutputStream().use{out->
                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG,90,out);out.toByteArray()
                }
                bmp.recycle()
                val reader=OfflineTextAnalyzer(this)
                analyzer=reader
                val first=reader.analyze(bytes,"")
                val second=reader.analyze(bytes,"")
                runOnUiThread{
                    if(page!="replay"||isFinishing||isDestroyed)return@runOnUiThread
                    state.text="Teste local concluído"
                    result.addView(label("1ª leitura: "+first.status,12,cyan,true))
                    result.addView(label("2ª leitura: "+second.status,12,cyan,true))
                    result.addView(label(second.visionStatus,12,muted))
                    result.addView(label("Herói: "+(second.hero?:"não identificado"),15,gold,true))
                    result.addView(label("Item: "+(second.item?:"nenhum"),15,gold,true))
                    result.addView(label(second.reason,12,white))
                    for(r in second.players){
                        result.addView(label(r.hero+" • "+r.team+" • "+r.items.joinToString(", "),12,white))
                    }
                    result.addView(label("Candidatos reproduzidos em uma imagem não são acurácia comprovada.",11,muted))
                }
            }catch(e:Exception){
                runOnUiThread{state.text="Falha no teste: "+e.javaClass.simpleName}
            }finally{analyzer?.close()}
        }.start()
    }

    private fun back(ui: LinearLayout, name: String) {''')
g=Path("android-app/app/build.gradle.kts")
s=g.read_text().replace("versionCode = 9","versionCode = 10").replace('versionName = "0.7.0"','versionName = "0.7.1"')
g.write_text(s)
