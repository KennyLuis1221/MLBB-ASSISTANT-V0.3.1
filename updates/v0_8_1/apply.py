from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
def rep(n,a,z):
 p=b/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,z,1))
p="StrategicEngine.kt"
rep(p,'val healingHint:Boolean=false,val magicHint:Boolean=false)','val healingHint:Boolean=false,val magicHint:Boolean=false,\n                     val enemyItemsAggregate:List<String> = emptyList())')
rep(p,'private val components=setOf(', 'private val oldComponents=setOf(')
rep(p,'    val itemNames=','    private val components=(oldComponents.filter{ItemReference.byName[it]?.tier!=3}+ItemReference.all.filter{it.tier<3}.map{it.name}).toSet()\n    val itemNames=')
rep(p,'val itemNames=(physical+','val itemNames=(ItemReference.byName.keys+physical+')
rep(p,'it.equals(s.trim(),true)','it.equals(ItemReference.normalize(s),true)')
rep(p,'"Saber" to "Tough Boots|Hunter Strike|Blade of Despair|Malefic Roar|Endless Battle|Immortality")','''"Saber" to "Tough Boots|Hunter Strike|Blade of Despair|Malefic Roar|Endless Battle|Immortality",
        "Miya" to "Swift Boots|Windtalker|Berserker's Fury|Haas' Claws|Malefic Gun|Sea Halberd",
        "Moskov" to "Warrior Boots|Corrosion Scythe|Demon Hunter Sword|Golden Staff|Malefic Gun|Rose Gold Meteor",
        "Bruno" to "Swift Boots|Haas' Claws|Berserker's Fury|Malefic Gun|Windtalker|Blade of Despair",
        "Angela" to "Demon Shoes|Flask of the Oasis|Fleeting Time|Glowing Wand|Ice Queen Wand|Immortality")''')
rep(p,'val enemies=s.enemies.filter{HeroCatalog.find(it.hero)!=null}.distinctBy{it.hero}.take(5)','''val enemies=s.enemies.mapNotNull{p->HeroCatalog.find(p.hero)?.let{p.copy(hero=it,items=p.items.mapNotNull(::canonicalItem))}}.distinctBy{it.hero}.take(5)
        val aggregate=s.enemyItemsAggregate.mapNotNull(::canonicalItem)
        val aggregateArmor=aggregate.count{it in armor}.coerceAtMost(5).toDouble()
        val aggregateMagic=aggregate.count{it in resist}.coerceAtMost(5).toDouble()
        val aggregateHp=aggregate.count{it in hp}.coerceAtMost(5).toDouble()''')
rep(p,'val a=evidence{p->p.items.any{it in armor}}.coerceAtLeast(if(s.armorHint)2.0 else 0.0)','val a=evidence{p->p.items.any{it in armor}}.coerceAtLeast(aggregateArmor).coerceAtLeast(if(s.armorHint)2.0 else 0.0)')
rep(p,'val m=evidence{p->p.items.any{it in resist}}','val m=evidence{p->p.items.any{it in resist}}.coerceAtLeast(aggregateMagic)')
rep(p,'val tank=evidence{kind(it.hero)==Kind.TANK||it.items.any{v->v in hp}}','val tank=evidence{kind(it.hero)==Kind.TANK||it.items.any{v->v in hp}}.coerceAtLeast(aggregateHp)')
rep(p,'val allyAnti=s.allies.any{p->p.items.any{it in antiHeal}}','val allyAnti=s.allies.any{p->p.items.mapNotNull(::canonicalItem).any{it in antiHeal}}')
rep(p,'if(next in antiHeal&&owned.any{it in antiHeal})return false','''if(next in antiHeal&&owned.any{it in antiHeal})return false
        if(next in setOf("Malefic Gun","Malefic Roar")&&owned.any{it in setOf("Malefic Gun","Malefic Roar")})return false''')
rep(p,'base.forEachIndexed{i,it->add(it,45.0-i*5,"$profile • posição inicial "+(i+1))}','''base.forEachIndexed{i,it->add(it,45.0-i*5,"$profile • posição inicial "+(i+1))}
        (when(k){Kind.MAGIC->listOf("Blood Wings","Wishing Lantern","Winter Crown");Kind.TANK,Kind.SUPPORT->listOf("Immortality","Oracle","Radiant Armor");else->listOf("Rose Gold Meteor","Immortality","Blade of Despair")}).forEach{if(it !in choices)add(it,5.0,"Reserva situacional")}''')
rep(p,'val price=next?.let{s.prices[it]}?.takeIf{it>0}','''val price=next?.let{s.prices[it]}?.takeIf{it>0}
        val listPrice=next?.let{ItemReference.byName[it]?.price}''')
rep(p,'s.budget==null||price==null->"Preço restante/ouro disponível não confirmado; consulte a loja."','''s.budget==null||price==null->"Preço restante/ouro disponível não confirmado; "+(listPrice?.let{"preço integral de referência: "+it+" ouro ("+ItemReference.source+"). "}?: "")+"Consulte a loja."''')
p="MainActivity.kt"
rep(p,'val budget=input("Ouro disponível (opcional)","")','''val budget=input("Ouro disponível (opcional)","")
        val knownPrice=input("Preço restante (opcional: nome=valor)","")''')
rep(p,'listOf(rivals,enemyItems,owned,minute,budget)','listOf(rivals,enemyItems,owned,minute,budget,knownPrice)')
rep(p,'budget=budget.text.toString().toIntOrNull(),inventoryKnown=true,','''budget=budget.text.toString().toIntOrNull()?.takeIf{it>=0},
                prices=knownPrice.text.toString().split("=",limit=2).let{v->
                    val name=StrategicEngine.canonicalItem(v.firstOrNull().orEmpty())
                    val value=v.getOrNull(1)?.trim()?.toIntOrNull()
                    if(name!=null&&value!=null&&value>0)mapOf(name to value)else emptyMap()},
                inventoryKnown=true,enemyItemsAggregate=items,''')
p="OfflineTextAnalyzer.kt"
rep(p,'private var matchClock:String?=null','''private var matchClock:String?=null
    private var statsCandidates=emptyMap<String,Pair<String?,String?>>()
    private var clockCandidate:String?=null''')
rep(p,'previousBoard=emptyList();matchClock=null','previousBoard=emptyList();matchClock=null;statsCandidates=emptyMap();clockCandidate=null')
rep(p,'if(accepted.isNotEmpty()) {\n                    recent=accepted.map{r->','''if(accepted.isNotEmpty()) {
                    val nextStats=mutableMapOf<String,Pair<String?,String?>>()
                    recent=accepted.map{r->''')
rep(p,'val goldCandidates=region.filter{it.matches(Regex("[0-9]{4,5}"))}\n                        val gold=goldCandidates.singleOrNull()\n                        val kda=region.firstOrNull{it.matches(Regex("[0-9]{1,2}/[0-9]{1,2}/[0-9]{1,2}"))}','''val key=r.side.toString()+":"+r.index+":"+h.name
                        val goldCandidate=ObservationParser.gold(region)
                        val kdaCandidate=ObservationParser.kda(region)
                        val previous=statsCandidates[key]
                        nextStats[key]=goldCandidate to kdaCandidate
                        val gold=ObservationParser.compatibleGold(previous?.first,goldCandidate)
                        val kda=kdaCandidate?.takeIf{it==previous?.second}''')
rep(p,'                    lastSeen=now','                    statsCandidates=nextStats\n                    lastSeen=now')
rep(p,'if(clocks.size==1)matchClock=clocks[0]','''if(clocks.size==1){
                    val c=clocks[0];val old=ObservationParser.clockSeconds(clockCandidate)
                    val current=ObservationParser.clockSeconds(c)
                    if(old!=null&&current!=null&&current-old in 0..25)matchClock=c
                    clockCandidate=c
                }''')
rep(p,'previousHighlight=null\n            }','previousHighlight=null\n                statsCandidates=emptyMap();clockCandidate=null\n            }')
rep(p,'shop->"Loja • itens escritos não são compras"','shop->"Loja • nomes visíveis não são compras"')
rep(p,'val observed=recent.size+recent.sumOf{it.items.size}','''val shopNames=if(shop)ocr.text.lines().mapNotNull{StrategicEngine.canonicalItem(it.trim())}.distinct().take(8)else emptyList()
            val observed=recent.size+recent.sumOf{it.items.size}''')
rep(p,'(if(candidateSelf!=null)" • destaque: "+candidateSelf else ""),','(if(candidateSelf!=null)" • destaque: "+candidateSelf else "")+(if(shopNames.isNotEmpty())" • nomes na loja: "+shopNames.joinToString(", ") else ""),')
rep("OverlayPanel.kt",'heroPortrait.setImageBitmap(own?.portrait)','heroPortrait.setImageBitmap(own?.portrait)\n        heroPortrait.visibility=if(own?.portrait!=null)View.VISIBLE else View.GONE')
rep("OverlayPanel.kt",'itemArt.setImageBitmap(result.players.asSequence().mapNotNull{it.itemImages[result.item]}.firstOrNull())\n        itemArt.visibility=if(result.item!=null&&result.draftPicks.isEmpty())View.VISIBLE else View.GONE','''val art=result.players.asSequence().mapNotNull{it.itemImages[result.item]}.firstOrNull()
        itemArt.setImageBitmap(art)
        itemArt.visibility=if(art!=null&&result.draftPicks.isEmpty())View.VISIBLE else View.GONE''')
rep("OverlayPanel.kt",'addView(iv,LinearLayout.LayoutParams(dp(28),dp(28)))','if(r.portrait!=null)addView(iv,LinearLayout.LayoutParams(dp(28),dp(28)))')
rep("OverlayPanel.kt",'val pic=ImageView(context).apply{setImageBitmap(r.itemImages[name]);contentDescription=name}\n                            addView(pic,LinearLayout.LayoutParams(dp(21),dp(21)))','''val art=r.itemImages[name]
                            if(art!=null){
                                val pic=ImageView(context).apply{setImageBitmap(art);contentDescription=name}
                                addView(pic,LinearLayout.LayoutParams(dp(21),dp(21)))
                            }''')
g=Path("android-app/app/build.gradle.kts");s=g.read_text().replace("versionCode = 12","versionCode = 13").replace('versionName = "0.8.0"','versionName = "0.8.1"');g.write_text(s)
