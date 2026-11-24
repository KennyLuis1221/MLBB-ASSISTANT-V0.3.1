from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
def rep(n,a,z):
 p=b/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,z,1))
p="Combat132.kt"
rep(p,'val items:List<String>,val unmodeled:List<String>)','val items:List<String>,val unmodeled:List<String>,val baseHp:Double?=null)')
rep(p,'Total HP|Max HP|Extra HP|Total Mana','Total HP|Max HP|Extra Max HP|Extra HP|Total Mana')
rep(p,'Total Physical Defense|Total Magic Defense)\\s*\\)','Total Physical Defense|Total Magic Defense|target\\x27s HP|of the target\\x27s lost HP|of target\\x27s lost HP)\\s*\\)')
# Allow apostrophe in attribute names, and optional "as" before damage type.
rep(p,'(?:extra\\s+)?(Physical|Magic|True)','(?:(?:extra|as)\\s+)?(Physical|Magic|True)')
rep(p,'([A-Za-z ]+)""",RegexOption.IGNORE_CASE)','([A-Za-z\\x27 ]+)""",RegexOption.IGNORE_CASE)')
rep(p,'names,unresolved.distinct())','names,unresolved.distinct(),value(h,"hp",level))')
rep(p,'fun raw(e:Event,a:Stats,soulStacks:Int?=null):Double?{','fun raw(e:Event,a:Stats,soulStacks:Int?=null,targetHp:Double?=null,targetLostHp:Double?=null):Double?{')
rep(p,'"total hp","max hp"->a.hp','''"total hp","max hp"->a.hp
   "extra hp","extra max hp"->if(a.hp!=null&&a.baseHp!=null)(a.hp-a.baseHp).coerceAtLeast(0.0) else null
   "target's hp"->targetHp?.takeIf{it.isFinite()&&it>=0}
   "of the target's lost hp","of target's lost hp"->targetLostHp?.takeIf{it.isFinite()&&it>=0}''')
# Keep target-dependent formulas separate from automatic marginal damage calculations.
rep(p,'context.contains("if ")||context.contains("additional")||context.contains("after ")||context.contains("each ")||context.contains("when ")','context.contains("if ")||context.contains("additional")||context.contains("after ")||context.contains("each ")||context.contains("when ")||m.groupValues[2].contains("target",true)')
# Do not invent opponent levels for automatic damage model.
rep(p,'val p=foes.mapNotNull{stats(db,it.hero,it.level?:15,it.items.take(6))}','val p=foes.mapNotNull{foe->foe.level?.let{stats(db,foe.hero,it,foe.items.take(6))}}')
p="MainActivity.kt"
rep(p,'        val stacks=input("Stacks Soul Steal (Aldous, opcional)","")','''        val stacks=input("Stacks Soul Steal (Aldous, opcional)","")
        val targetHp=input("HP do alvo exigido pela fórmula (opcional)","")
        val targetLost=input("HP perdido do alvo (opcional)","")
        val crystal=input("Holy Crystal: bônus percentual conhecido (21–35, opcional)","")
        val axe=input("War Axe: stacks conhecidas (0–6, opcional)","")
        val wings=check("Blood Wings: escudo ativo no atacante")''')
rep(p,'hits,sequence,stacks,emblem,talents,spell,shield,rate,heal))','hits,sequence,stacks,targetHp,targetLost,crystal,axe,emblem,talents,spell,shield,rate,heal))')
rep(p,'ui.addView(anti);ui.addView(magicGuard)','ui.addView(anti);ui.addView(magicGuard);ui.addView(wings)')
rep(p,'val a=h?.let{Combat132.stats(db,it.name,level.text.toString().toIntOrNull()?:0,list(items.text.toString()),setup)}','''val base=h?.let{Combat132.stats(db,it.name,level.text.toString().toIntOrNull()?:0,list(items.text.toString()),setup)}
            val crystalText=crystal.text.toString().trim();val axeText=axe.text.toString().trim()
            val crystalPct=crystalText.toDoubleOrNull();val axeCount=axeText.toIntOrNull()
            if(crystalText.isNotEmpty()&&crystalPct==null||axeText.isNotEmpty()&&axeCount==null){out.addView(label("Parâmetros de passiva inválidos.",12,muted));return@button}
            val effects=if(base!=null&&h!=null)Effects16.apply(db,h.name,base,Effects16.Inputs(crystalPct,axeCount,wings.isChecked))else null
            val a=effects?.stats''')
rep(p,'val soul=stacks.text.toString().toIntOrNull()?.takeIf{it in 0..1000}','''val soul=stacks.text.toString().toIntOrNull()?.takeIf{it in 0..1000}
            val hpInput=targetHp.text.toString().trim().toDoubleOrNull()?.takeIf{it.isFinite()&&it>=0}
            val lostInput=targetLost.text.toString().trim().toDoubleOrNull()?.takeIf{it.isFinite()&&it>=0}
            if(targetHp.text.toString().isNotBlank()&&hpInput==null||targetLost.text.toString().isNotBlank()&&lostInput==null){
                out.addView(label("HP informado inválido.",12,muted));return@button
            }
            effects?.shield?.let{out.addView(label("Escudo condicional Blood Wings: "+fmt(it),12,white))}
            effects?.warAxeTrueFraction?.let{out.addView(label("War Axe: fração de dano verdadeiro condicional "+fmt(it*100)+"% (não adicionada automaticamente)",12,white))}
            effects?.warnings?.forEach{out.addView(label(it,11,muted))}''')
q=b/p;s=q.read_text();assert s.count("Combat132.raw(ev,a,soul)")==2;s=s.replace("Combat132.raw(ev,a,soul)","Combat132.raw(ev,a,soul,hpInput,lostInput)");q.write_text(s)
