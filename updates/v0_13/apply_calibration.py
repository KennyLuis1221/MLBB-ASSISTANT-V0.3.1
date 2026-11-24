from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
def rep(n,a,z):
 p=b/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,z,1))
rep("DetailedMatcher.kt",'private val refs = mutableListOf<Ref>()','''private val refs = mutableListOf<Ref>()
    private val calibration=BoardCalibration.decode(context.getSharedPreferences("settings",Context.MODE_PRIVATE).getString("board_calibration",null))
    val calibrated get()=calibration!=null''')
rep("DetailedMatcher.kt",'        val w=b.width;val h=b.height\n        // Multiple hypotheses','''        val w=b.width;val h=b.height
        val custom=calibration?.takeIf{it.compatible(w,h)}
        if(custom!=null){
            val rows=mutableListOf<Row>()
            for(side in 0..1)for(row in 0..4){
                val p=custom.hero(side,row)
                val hit=match(b,(p.x*w).toInt(),(p.y*h).toInt(),(p.size*w).toInt().coerceAtLeast(12),'h')
                val items=if(hit==null)emptyList()else(0..5).mapNotNull{idx->
                    val i=custom.item(side,row,idx)
                    match(b,(i.x*w).toInt(),(i.y*h).toInt(),(i.size*w).toInt().coerceAtLeast(12),'i')
                }
                rows.add(Row(side,row,hit,items,(p.y*h).toInt()))
            }
            val duplicates=rows.mapNotNull{it.hero?.name}.groupingBy{it}.eachCount().filterValues{it>1}.keys
            val unique=rows.filter{it.hero?.name !in duplicates}
            val n=unique.count{it.hero!=null}
            return if(n>=4&&unique.count{it.side==0&&it.hero!=null}>=2&&unique.count{it.side==1&&it.hero!=null}>=2)Board(unique,n)else Board(emptyList(),n)
        }
        // Multiple hypotheses''')
rep("MainActivity.kt",'        ui.addView(button("Configurações avançadas  ⚙") { showAdvanced() })','''        ui.addView(button("CALIBRAR PLACAR COM PRINT") {
            val pick=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="image/*"}
            startActivityForResult(pick,7303)
        })
        ui.addView(button("Configurações avançadas  ⚙") { showAdvanced() })''')
rep("MainActivity.kt",'        if(requestCode==7302){','''        if(requestCode==7303){
            if(resultCode==RESULT_OK)data?.data?.let{showCalibration(it)}
            return
        }
        if(requestCode==7302){''')
rep("MainActivity.kt",'    private fun showReplay(uri:Uri){','''    private fun showCalibration(uri:Uri){
        page="calibration"
        val ui=root()
        back(ui,"CALIBRAÇÃO DO PLACAR")
        ui.addView(label("Selecione um print do placar aberto, sem o painel por cima. Marque o centro dos retratos e slots (mesmo vazios). A imagem não será armazenada.",13,white))
        val hint=label("Carregando imagem...",15,cyan,true)
        ui.addView(hint)
        ui.addView(button("APAGAR CALIBRAÇÃO SALVA"){
            prefs.edit().remove("board_calibration").apply()
            toast("Calibração apagada; reinicie a assistência.")
        })
        Thread{
            try{
                val opts=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true}
                contentResolver.openInputStream(uri)?.use{android.graphics.BitmapFactory.decodeStream(it,null,opts)}
                require(opts.outWidth>opts.outHeight&&opts.outHeight>0){"Escolha um print horizontal"}
                var sample=1
                while(maxOf(opts.outWidth,opts.outHeight)/sample>2200)sample*=2
                val bmp=contentResolver.openInputStream(uri)?.use{
                    android.graphics.BitmapFactory.decodeStream(it,null,android.graphics.BitmapFactory.Options().apply{inSampleSize=sample})
                }?:error("Imagem inválida")
                runOnUiThread{
                    if(page!="calibration"||isFinishing||isDestroyed){bmp.recycle();return@runOnUiThread}
                    var layout:BoardCalibration.Layout?=null
                    val aspect=bmp.width.toDouble()/bmp.height
                    hint.text="1/10 • "+BoardCalibration.instructions[0]
                    val marker=CalibrationView(this,bmp){points->
                        layout=BoardCalibration.create(aspect,points)
                        hint.text=if(points.size<10)(points.size+1).toString()+"/10 • "+BoardCalibration.instructions[points.size]
                            else if(layout!=null)"Confira as caixas azuis nos 10 jogadores e nos slots." else "Marcas incoerentes. Desfaça ou refaça."
                    }
                    ui.addView(marker,LinearLayout.LayoutParams(-1,dp(260)))
                    ui.addView(button("DESFAZER ÚLTIMA MARCA"){marker.undo()})
                    ui.addView(button("REFAZER MARCAÇÕES"){marker.reset()})
                    ui.addView(button("SALVAR CALIBRAÇÃO",true){
                        val x=layout
                        if(x==null){toast("Complete as 10 marcas válidas.");return@button}
                        prefs.edit().putString("board_calibration",x.encode()).apply()
                        toast("Calibração salva. Reinicie assistência e teste com print.")
                        showHome()
                    })
                    ui.addView(label("A calibração só define regiões. Não garante a identificação correta. Se o placar mudar, refaça.",11,muted))
                }
            }catch(e:Exception){runOnUiThread{hint.text="Não foi possível ler print horizontal: "+e.javaClass.simpleName}}
        }.start()
    }

    private fun showReplay(uri:Uri){''')
rep("OfflineTextAnalyzer.kt",'(statsLabel||numeric>=6)','(statsLabel||numeric>=6||matcher.calibrated&&numeric>=4)')
rep("OfflineTextAnalyzer.kt",'" candidatos"+','" candidatos • "+(if(matcher.calibrated)"calibração salva" else "layout estimado")+')
rep("MainActivity.kt",'Versão 0.8 experimental • motor estratégico local e modo I.A. opcional. Dados por patch e leitura ao vivo ainda exigem validação.','V0.13 experimental • motor local, banco de dados comunitário e calibração de placar. Leitura real ainda exige validação.')
