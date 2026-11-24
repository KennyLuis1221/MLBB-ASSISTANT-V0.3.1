from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
def rep(n,a,z):
 p=b/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,z,1))
p="CalibrationView.kt"
rep(p,'private val bitmap:Bitmap,private val changed:', 'private val bitmap:Bitmap,private val pointLimit:Int=10,private val changed:')
rep(p,'points.size<10&&bounds.contains','points.size<pointLimit&&bounds.contains')
rep(p,'  paint.style=Paint.Style.FILL\n }','''  if(pointLimit!=10){
   paint.color=Color.CYAN;paint.style=Paint.Style.STROKE
   for(pair in points.chunked(2))if(pair.size==2){
    val a=pair[0];val b=pair[1]
    c.drawRect(bounds.left+minOf(a.x,b.x).toFloat()*bounds.width(),bounds.top+minOf(a.y,b.y).toFloat()*bounds.height(),
      bounds.left+maxOf(a.x,b.x).toFloat()*bounds.width(),bounds.top+maxOf(a.y,b.y).toFloat()*bounds.height(),paint)
   }
  }
  paint.style=Paint.Style.FILL
 }''')
p="MainActivity.kt"
rep(p,'        ui.addView(button("Configurações avançadas  ⚙") { showAdvanced() })','''        ui.addView(button("CALIBRAR NÍVEL NO PLACAR"){
            if(BoardCalibration.decode(prefs.getString("board_calibration",null))==null){toast("Calibre primeiro os retratos e itens do placar.");return@button}
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="image/*"},7304)
        })
        ui.addView(button("CALIBRAR SALDO NA LOJA"){
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="image/*"},7305)
        })
        ui.addView(button("Configurações avançadas  ⚙") { showAdvanced() })''')
rep(p,'        if(requestCode==7303){','''        if(requestCode in 7304..7305){
            if(resultCode==RESULT_OK)data?.data?.let{showFieldCalibration(it,requestCode==7304)}
            return
        }
        if(requestCode==7303){''')
rep(p,'    private fun showReplay(uri:Uri){','''    private fun showFieldCalibration(uri:Uri,levels:Boolean){
        page="fields";val ui=root();back(ui,if(levels)"NÍVEL NO PLACAR" else "SALDO NA LOJA")
        val board=if(levels)BoardCalibration.decode(prefs.getString("board_calibration",null))else null
        if(levels&&board==null){ui.addView(label("Calibre o placar primeiro.",14,muted));return}
        val key=if(levels)"levels_calibration" else "budget_calibration"
        val instructions=if(levels)listOf("Canto superior esquerdo do número de nível da primeira linha esquerda","Canto inferior direito do mesmo número","Canto superior esquerdo do nível da primeira linha direita","Canto inferior direito do mesmo número")
          else listOf("Canto superior esquerdo do SALDO disponível na loja, não o preço de item","Canto inferior direito do SALDO")
        val hint=label("Carregando captura...",14,cyan,true)
        ui.addView(label("Use um print verdadeiro da tela correspondente, na mesma resolução da captura. Delimite apenas o número. A imagem não será armazenada. Resultado precisa repetir em dois quadros.",12,muted))
        ui.addView(hint)
        ui.addView(button("APAGAR CALIBRAÇÃO DESTE CAMPO"){prefs.edit().remove(key).apply();toast("Calibração removida; reinicie assistência.")})
        Thread{
            try{
                val opts=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true}
                contentResolver.openInputStream(uri)?.use{android.graphics.BitmapFactory.decodeStream(it,null,opts)}
                require(opts.outWidth>opts.outHeight&&opts.outHeight>0)
                var sample=1
                while(maxOf(opts.outWidth,opts.outHeight)/sample>2200)sample*=2
                val bmp=contentResolver.openInputStream(uri)?.use{android.graphics.BitmapFactory.decodeStream(it,null,android.graphics.BitmapFactory.Options().apply{inSampleSize=sample})}?:error("Print inválido")
                runOnUiThread{
                    if(page!="fields"||isFinishing||isDestroyed){bmp.recycle();return@runOnUiThread}
                    var profile:Fields15.Profile?=null
                    hint.text=instructions.first()
                    val marker=CalibrationView(this,bmp,instructions.size){points->
                        profile=Fields15.create(bmp.width.toDouble()/bmp.height,points,board)
                        hint.text=if(points.size<instructions.size)instructions[points.size] else if(profile!=null)"Confira a região marcada." else "Região inválida; refaça as marcas."
                    }
                    ui.addView(marker,LinearLayout.LayoutParams(-1,dp(280)))
                    ui.addView(button("DESFAZER"){marker.undo()})
                    ui.addView(button("REFAZER"){marker.reset()})
                    ui.addView(button("SALVAR",true){
                        val p=profile
                        if(p==null){toast("Marque as regiões válidas.");return@button}
                        prefs.edit().putString(key,p.encode()).apply();toast("Salvo. Reinicie a assistência.");showHome()
                    })
                }
            }catch(e:Exception){runOnUiThread{hint.text="Não foi possível abrir a captura horizontal."}}
        }.start()
    }

    private fun showReplay(uri:Uri){''')
p="UploadClient.kt"
rep(p,'val itemImages:Map<String,android.graphics.Bitmap> = emptyMap()','val itemImages:Map<String,android.graphics.Bitmap> = emptyMap(),val level:Int?=null')
p="OfflineTextAnalyzer.kt"
rep(p,'    private var historyText=""','''    private var historyText=""
    private val boardLayout=BoardCalibration.decode(prefs.getString("board_calibration",null))
    private val levelProfile=boardLayout?.let{Fields15.decode(prefs.getString("levels_calibration",null),it)}
    private val cashProfile=Fields15.decode(prefs.getString("budget_calibration",null))
    private val cashRepeat=Fields15.Repeated(12_000)
    private var confirmedCash:Int?=null
    private var previousLevels=emptyMap<String,Int>()''')
rep(p,'history.clear();historyText="";cachedState=null;cachedAdvice=null','history.clear();historyText="";cachedState=null;cachedAdvice=null;previousLevels=emptyMap();cashRepeat.clear();confirmedCash=null')
rep(p,'            val numeric=tokens.count','''            fun inBox(box:Fields15.Box)=tokens.filter{el->el.boundingBox?.let{q->
                box.contains(q.centerX().toDouble()/b.width,q.centerY().toDouble()/b.height)
            }==true}.sortedBy{it.boundingBox?.left?:0}.map{it.text}
            val strongShop=listOf("equipamentos","equipment","loja","shop").any{Regex("\\\\b"+it+"\\\\b").containsMatchIn(t)}
            val numeric=tokens.count''')
rep(p,'                    previousBoard=emptyList();previousHighlight=null;statsCandidates=emptyMap();clockCandidate=null','                    previousBoard=emptyList();previousHighlight=null;statsCandidates=emptyMap();clockCandidate=null;previousLevels=emptyMap()')
rep(p,'                    val nextStats=mutableMapOf<String,Pair<String?,String?>>()','''                    val nextStats=mutableMapOf<String,Pair<String?,String?>>()
                    val nextLevels=mutableMapOf<String,Int>()''')
rep(p,'                        val previous=statsCandidates[key]','''                        val levelCandidate=if(boardLayout!=null&&levelProfile?.compatible(b.width,b.height)==true)
                            levelProfile.levelBox(boardLayout,r.side,r.index)?.let{Fields15.level(inBox(it))} else null
                        if(levelCandidate!=null)nextLevels[key]=levelCandidate
                        val confirmedLevel=levelCandidate?.takeIf{it==previousLevels[key]}
                        val previous=statsCandidates[key]''')
rep(p,'items.mapNotNull{it2->matcher.thumbnail(b,it2)?.let{img->it2.name to img}}.toMap())','items.mapNotNull{it2->matcher.thumbnail(b,it2)?.let{img->it2.name to img}}.toMap(),level=confirmedLevel)')
rep(p,'                    statsCandidates=nextStats','                    statsCandidates=nextStats;previousLevels=nextLevels')
rep(p,'                previousHighlight=null\n                statsCandidates=emptyMap();clockCandidate=null','                previousHighlight=null\n                statsCandidates=emptyMap();clockCandidate=null;previousLevels=emptyMap()')
rep(p,'            val owned=own?.items?:emptyList()','''            val owned=own?.items?:emptyList()
            val cash=if(strongShop&&board.rows.isEmpty()&&own!=null&&lastSeen>0&&Evidence14.fresh(now,lastSeen)&&cashProfile?.compatible(b.width,b.height)==true)
                cashProfile.boxes.singleOrNull()?.let{Fields15.budget(inBox(it))} else null
            confirmedCash=cashRepeat.next(cash,now)''')
rep(p,'k.getOrNull(0),k.getOrNull(1),k.getOrNull(2))','k.getOrNull(0),k.getOrNull(1),k.getOrNull(2),p.level)')
rep(p,'ownGold=own?.gold?.toIntOrNull(),minute=','ownGold=own?.gold?.toIntOrNull(),ownLevel=own?.level,budget=confirmedCash,minute=')
rep(p,'(if(historyText.isNotBlank())" • "+historyText else ""),','(if(historyText.isNotBlank())" • "+historyText else "")+(confirmedCash?.let{" • saldo provável $it (loja)"}?:""),')
p="OverlayPanel.kt"
rep(p,'r.kda?.let{"KDA $it"}','r.kda?.let{"KDA $it"},r.level?.let{"Nível provável $it"}')
