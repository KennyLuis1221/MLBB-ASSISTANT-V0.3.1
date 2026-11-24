from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
def rep(n,a,z):
 p=b/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,z,1))
p="GameData13.kt"
rep(p,'val scaling:List<String>)','val scaling:List<String>,val description:String="")')
rep(p,'val active:String)','val active:String,val passiveText:String="",val activeText:String="",val summary:String="")')
rep(p,'fun counter(a:String,b:String,rank:String="all"):Double?=counters[Triple(a,b,rank)]?:counters[Triple(b,a,rank)]?.let{-it}','''fun counter(a:String,b:String,rank:String="all"):Double?{
   if(a==b)return null
   val values=listOfNotNull(counters[Triple(a,b,rank)],counters[Triple(b,a,rank)]?.let{-it})
   return if(values.isEmpty())null else values.average()
  }''')
rep(p,'val v=normalizeItem(s)','val v=normalizeItem(s)')
rep(p,'     "C","Y"->','''     "D"->{val skills=sk[a[1]]?:error("Unknown skill hero");val idx=a[2].toInt();skills[idx]=skills[idx].copy(description=a[3])}
     "E"->{val item=it[a[1]]?:error("Unknown item");it[a[1]]=item.copy(passiveText=a[2],activeText=a[3],unique=a[4],summary=a[5])}
     "C","Y"->''')
rep(p,'val result=Snapshot(date,h,sk,it,bs,c,y);','''require(h.keys==sk.keys&&bs.keys.containsAll(h.keys))
  require(sk.values.flatten().all{it.description.isNotBlank()})
  require(it.values.all{i->i.recipe.all{it.containsKey(it)} }.not() == false) // replaced below
  val result=Snapshot(date,h,sk,it,bs,c,y);''')
# Avoid shadowing the item map.
rep(p,'require(it.values.all{i->i.recipe.all{it.containsKey(it)} }.not() == false) // replaced below','require(it.values.all{i->i.recipe.all{part->it.containsKey(part)}})')
p="StrategicEngine.kt"
rep(p,'val enemyItemsAggregate:List<String> = emptyList())','val enemyItemsAggregate:List<String> = emptyList(),val lane:String?=null,val rank:String="all")')
p="Strategy13.kt"
rep(p,'a:String,b:String,ally:Boolean=false):Matchup','a:String,b:String,ally:Boolean=false,rank:String="all"):Matchup')
rep(p,'val measured=if(ally)db.teammate(h.name,e.name)else db.counter(h.name,e.name)','''if(h.name==e.name)return Matchup(null,0.0,listOf("mesmo herói"))
  val measured=if(ally)db.teammate(h.name,e.name,rank)else db.counter(h.name,e.name,rank)''')
rep(p,'DraftRules.reasons(h.name,emptyList(),listOf(e.name)).forEach{add(3.0,it)}','''DraftRules.reasons(h.name,emptyList(),listOf(e.name)).forEach{add(3.0,it)}
    DraftRules.reasons(e.name,emptyList(),listOf(h.name)).forEach{add(-3.0,"risco: "+it)}
    val hs=db.skills[h.name].orEmpty().flatMap{it.tags}.toSet()
    val es=db.skills[e.name].orEmpty().flatMap{it.tags}.toSet()
    if("true damage" in hs&&"Tank" in e.roles)add(1.5,"dano verdadeiro contra resistência")
    if("true damage" in es&&"Tank" in h.roles)add(-1.5,"risco de dano verdadeiro")
    if("stun" in hs&&("dash" in es||"blink" in es))add(1.0,"controle contra deslocamento")
    if("stun" in es&&("dash" in hs||"blink" in hs))add(-1.0,"risco de controle")''')
rep(p,'favoritesInput:String,bansInput:String):List<HeroCatalog.Pick>','favoritesInput:String,bansInput:String,rank:String="all"):List<HeroCatalog.Pick>')
rep(p,'it to matchup(db,h.name,it)}','it to matchup(db,h.name,it,rank=rank)}')
rep(p,'it to matchup(db,h.name,it,true)}','it to matchup(db,h.name,it,true,rank)}')
rep(p,'val score=es.sumOf','''val team=db.heroes.values.filter{it.name in a}
    val balance=(if(team.isNotEmpty()&&team.none{"Tank" in it.roles}&&"Tank" in h.roles)3.0 else 0.0)+
      (if(team.isNotEmpty()&&team.none{it.damage=="Magic"}&&h.damage=="Magic")2.0 else 0.0)+
      (if(team.isNotEmpty()&&team.none{it.damage=="Physical"}&&h.damage=="Physical")2.0 else 0.0)
    val score=balance+es.sumOf''')
rep(p,'add("Função "+lane+"; dados "+db.date)','add("Função "+lane+"; ranque "+rank+"; dados "+db.date)\n     if(balance>0)add("complementa a composição")')
rep(p,'i.name=="Valkyrie\\'s Shield"||ItemReference.byName[i.name]?.tier==3','i.name in setOf("Valkyrie\\'s Shield","Twilight Armor")||ItemReference.byName[i.name]?.tier==3')
# Replace old greedy economy with exact allocation and slot-safe actions.
q=b/p;s=q.read_text();start=s.index('  val item=db.items[target]');end=s.index('\n fun recommend(',start)
s=s[:start]+'''  val name=db.canonicalItem(target)?:return Buy(target,null,null,"Equipamento desconhecido.")
  val item=db.items.getValue(name)
  val price=item.price?.takeIf{it>0}
  if(!inventoryKnown||price==null)return Buy(name,null,null,"Preço integral de referência: "+(price?.toString()?:"?")+" • inventário/preço restante não confirmado.")
  val quote=Recipe131.quote(db,name,owned,budget)?:return Buy(name,null,null,"Receita ou inventário inconsistente; confirme na loja.")
  val remaining=explicitPrice?.takeIf{it>=0}?:quote.cost
  val free=budget?.takeIf{it>=0}
  val fits=owned.size<6||quote.consumed>0
  val component=quote.component.takeIf{free!=null&&free<remaining}
  val detail="Custo restante "+(if(explicitPrice!=null)"informado" else "estimado pela receita comunitária")+": "+remaining+
    " ouro"+(if(!fits)" • sem espaço; libere um slot" else if(free==null)" • saldo desconhecido" else if(free>=remaining)" • cabe no saldo informado" else " • faltam "+(remaining-free))+
    (component?.let{" • componente candidato: "+it}?:"")+"; confirme no jogo."
  return Buy(name,remaining,component,detail)
 }
''' +s[end:];q.write_text(s)
rep(p,'val full=owned.filter{db.items[it]?.let{v->eligible(db,v)}==true}.distinct()','val full=owned.filter{db.items[it]?.let{v->eligible(db,v)}==true}')
rep(p,'val unknown=s.owned.size!=owned.size','val unknown=s.owned.size!=owned.size||s.owned.size>6')
rep(p,'val builds=db.builds[h.name].orEmpty().filter{it.rank=="all"||it.rank=="mythic"}','''val allBuilds=db.builds[h.name].orEmpty()
  val lane=laneMap[s.lane]?:s.lane
  val ranked=allBuilds.filter{it.rank==s.rank&& (lane==null||it.lane==lane)}
  val sameLane=allBuilds.filter{it.rank=="all"&&(lane==null||it.lane==lane)}
  val builds=ranked.ifEmpty{sameLane}.ifEmpty{allBuilds.filter{it.rank=="all"}}''')
rep(p,'val seedScore=seeds.mapNotNull{b->b.items.indexOf(i.name).takeIf{it>=0}?.let{pos->(if(b.type=="played")24.0 else 18.0)-pos*2}}.maxOrNull()','''val seedScore=seeds.mapNotNull{b->b.items.indexOf(i.name).takeIf{it>=0}?.let{pos->
     (if(b.type=="played")24.0 else 18.0)-pos*2+b.frequency.coerceIn(0.0,100.0)*.06}}.maxOrNull()''')
rep(p,'(s.minute?:0)<6','s.minute!=null&&s.minute in 0..5')
rep(p,'if(full.size>=6||s.owned.size>=6&&unknown){','if(full.size>=6||s.owned.size>=6&&unknown){')
rep(p,'candidates.filter{valid(it.name,full-old)}','candidates.filter{it.name!=old&&valid(it.name,full-old)&&(db.items[old]?.category!="Movement"||db.items[it.name]?.category=="Movement")}')
rep(p,'val next=beam.flatMap{path->candidates.filter{valid(it.name,full+path.items)}.map{c->','''val next=beam.flatMap{path->candidates.filter{valid(it.name,full+path.items)&&
    (depth!=0||owned.size<6||(!unknown&&Recipe131.quote(db,it.name,owned,null)?.consumed?.let{n->n>0}==true))}.map{c->''')
rep(p,'val purchase=next?.let{buy(db,it,owned,s.budget,s.inventoryKnown&&!unknown,s.prices[it]).detail}','''val purchase=next?.let{buy(db,it,owned,s.budget,s.inventoryKnown&&!unknown,s.prices.entries.firstOrNull{e->db.canonicalItem(e.key)==it}?.value).detail}
     ?:if(owned.size>=6)"Sem compra segura: inventário cheio; confirme componentes ou libere espaço." else null''')
rep(p,'if(seeds.isEmpty())"Perfil factual + regras, sem build observada" else "Perfil factual + "+seeds.size+" builds/guia de referência"','if(seeds.isEmpty())"Perfil factual + regras, sem build observada" else "Perfil factual + "+seeds.size+" builds de referência; "+seeds.first().rank+"/"+seeds.first().lane')
# Expose source descriptions, rank filter, manual players and explicit inventory confirmation.
p="MainActivity.kt"
rep(p,'ui.addView(label("Sugestões preliminares por função, composição e favoritos. Não são counters validados por IA.", 12, muted))','ui.addView(label("Dados comunitários e regras locais, sem IA. Ausências de medida não são taxas de vitória.", 12, muted))')
rep(p,'        val allies = input("Aliados selecionados','''        val ranks=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("all","epic","legend","mythic","honor","glory"))}
        ui.addView(label("Ranque da referência",12,white));ui.addView(ranks)
        val allies = input("Aliados selecionados''')
rep(p,'favorites.text.toString(), bans.text.toString())','favorites.text.toString(), bans.text.toString(),ranks.selectedItem.toString())')
rep(p,'val e=input("Outro herói","Tigreal")','''val e=input("Outro herói","Tigreal")
        val rank=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("all","epic","legend","mythic","honor","glory"))}
        ui.addView(rank)''')
rep(p,'out.addView(label("CD: "+skill.cooldown','out.addView(label(skill.description,12,white))\n                out.addView(label("CD: "+skill.cooldown')
rep(p,'Strategy13.matchup(db,hero.name,rival.name)','Strategy13.matchup(db,hero.name,rival.name,rank=rank.selectedItem.toString())')
rep(p,'Strategy13.matchup(db,hero.name,rival.name,true)','Strategy13.matchup(db,hero.name,rival.name,true,rank.selectedItem.toString())')
rep(p,'itemCard.addView(label("Receita: "+v.recipe.joinToString(" + ")+" • passiva/ativa: "+v.passive+" "+v.active,12,muted))','''itemCard.addView(label("Receita: "+v.recipe.joinToString(" + ")+" • passiva/ativa: "+v.passive+" "+v.active,12,muted))
                itemCard.addView(label(listOf(v.unique,v.passiveText,v.activeText).filter{it.isNotBlank()}.joinToString("\\n"),12,white))
                itemCard.addView(label(if(Strategy13.eligible(db,v))"Candidato final" else "Componente, especial ou histórico; não candidato final",11,muted))''')
rep(p,'val rivals=input("Inimigos (nomes, vírgula)","")','''val rivals=input("Inimigos: nome|ouro total|nível|K/D/A|item+item; próximo herói","")
        val team=input("Aliados: mesmo formato; separe com ponto e vírgula","")
        val total=input("Meu ouro total (opcional)","")
        val ownLevel=input("Meu nível (1–15)","")
        val complete=check("Confirme que todos os itens próprios foram informados (ou inventário vazio)")
        ui.addView(complete)
        val pos=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Automática","Selva","EXP","Meio","Ouro","Roam"))}
        val ranks=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("all","epic","legend","mythic","honor","glory"))}
        ui.addView(pos);ui.addView(ranks)''')
rep(p,'listOf(rivals,enemyItems,owned,minute,budget,knownPrice)','listOf(rivals,team,total,ownLevel,enemyItems,owned,minute,budget,knownPrice)')
rep(p,'val foes=rivals.text.toString().split(",",";").mapNotNull{HeroCatalog.find(it)}.distinct().take(5)\n            val items=StrategicEngine.parseItems(enemyItems.text.toString())','''val db=GameData13.current?:run{toast("Banco indisponível");return@button}
            fun playerText(s:String)=if("|" !in s)s.replace(",",";") else s
            val foes=Manual131.players(playerText(rivals.text.toString()),db,5)
            val allies=Manual131.players(playerText(team.text.toString()),db,4)
            val rawItems=enemyItems.text.toString().split(",",";").map{it.trim()}.filter{it.isNotBlank()}
            val items=rawItems.mapNotNull(db::canonicalItem)
            if(foes.errors.isNotEmpty()||allies.errors.isNotEmpty()||items.size!=rawItems.size||
                (foes.players+allies.players).map{it.hero}.let{it.size!=it.distinct().size||hero in it}){
                result.addView(label("Confira os nomes e campos: "+(foes.errors+allies.errors).joinToString("; "),12,muted));return@button
            }''')
rep(p,'foes.map{StrategicEngine.Player(it)},minute=minute.text.toString().toIntOrNull(),','''foes.players,allies.players,ownGold=total.text.toString().toIntOrNull()?.takeIf{it>=0},
                ownLevel=ownLevel.text.toString().toIntOrNull()?.takeIf{it in 1..15},
                minute=minute.text.toString().toIntOrNull()?.takeIf{it in 0..99},
                lane=pos.selectedItem.toString().takeUnless{it=="Automática"},rank=ranks.selectedItem.toString(),''')
rep(p,'val name=StrategicEngine.canonicalItem(v.firstOrNull().orEmpty())','val name=db.canonicalItem(v.firstOrNull().orEmpty())')
rep(p,'value>0)mapOf','value>=0)mapOf')
rep(p,'inventoryKnown=true,enemyItemsAggregate=items,','inventoryKnown=complete.isChecked,enemyItemsAggregate=items,')
rep(p,'result.addView(label(advice.reason,12,white))','result.addView(label(advice.reason,12,white))\n            result.addView(label(advice.profile,12,muted))')
# Prevent stale snapshot fallback being silent in base mode.
rep(p,'private fun startCapture(useCloud:Boolean) {','''private fun startCapture(useCloud:Boolean) {
        if(!useCloud&&GameData13.current==null){toast("Banco offline inválido. Reinstale o APK.");return}''')
g=Path("android-app/app/build.gradle.kts");s=g.read_text().replace("versionCode = 14","versionCode = 15").replace('versionName = "0.13.0"','versionName = "0.13.1"');g.write_text(s)
