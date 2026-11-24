package com.mlbb.assistant

/** Numerical comparison of explicit candidate builds, not a complete game simulator.
 * Unknown fields stay null; conditional passives do not get guessed activation.
 */
object BuildCompare14 {
 data class Metrics(val items:List<String>,val price:Int?,val basicHit:Double?,val sampleSkillDamage:Double?,val skillSamples:Int,
   val physicalEhp:Double?,val magicEhp:Double?,val cooldown:Double,val warnings:List<String>)
 data class Rated(val items:List<String>,val score:Double,val metrics:Metrics)
 data class Swap(val slots:List<Int>,val oldItems:List<String>,val newItems:List<String>,val netCost:Int,val score:Double)
 private val anti=setOf("Sea Halberd","Dominance Ice","Glowing Wand")
 private val pen=setOf("Malefic Gun","Malefic Roar")
 private val crit=setOf("Berserker's Fury","Haas' Claws","Windtalker","Great Dragon Spear")
 fun legal(db:GameData13.Snapshot,items:List<String>):Boolean{
  if(items.size>6)return false
  val names=items.map{db.canonicalItem(it)?:return false}
  if(names.distinct().size!=names.size||names.any{!Strategy13.eligible(db,db.items.getValue(it))})return false
  if(names.count{db.items[it]?.category=="Movement"}>1||names.count{it in anti}>1||names.count{it in pen}>1)return false
  return !("Golden Staff" in names&&names.any{it in crit})
 }
 fun metrics(db:GameData13.Snapshot,hero:String,level:Int,items:List<String>,targets:List<StrategicEngine.Player>,setup:Loadout14.Selection?=null):Metrics?{
  val h=db.hero(hero)?:return null
  val a=Combat132.stats(db,h.name,level,items,setup)?:return null
  val ds=targets.take(5).mapNotNull{p->p.level?.let{Combat132.stats(db,p.hero,it,p.items)}}
  val basic=if(h.damage=="Physical"&&a.physical!=null&&ds.isNotEmpty()){
   ds.mapNotNull{Combat132.hit(Combat132.Hit(a.physical,Combat132.Damage.PHYSICAL,critAllowed=true),a,it)}.takeIf{it.size==ds.size}?.average()
  }else null
  // Compare identical extracted events, not sum of a real rotation. Conditional events excluded.
  val events=Combat132.events(db,h.name).filter{!it.conditional}.distinctBy{it.skill}
  val samples=ds.flatMap{d->events.mapNotNull{e->Combat132.raw(e,a)?.let{Combat132.hit(Combat132.Hit(it,e.type),a,d)}}}
  val skill=samples.takeIf{it.isNotEmpty()&&it.size==events.size*ds.size}?.average()
  val prices=items.map{db.canonicalItem(it)?.let{n->db.items[n]?.price}}
  val price=if(prices.any{it==null})null else prices.filterNotNull().sum()
  val warnings=a.unmodeled+(if(ds.size<targets.take(5).size)listOf("alvos sem nível/atributos suficientes não comparados")else emptyList())+
   listOf("dano por evento, não DPS de rotação; passivas condicionais parcialmente modeladas")
  return Metrics(items,price,basic,skill,samples.size,Combat132.effectiveHP(a,Combat132.Damage.PHYSICAL),Combat132.effectiveHP(a,Combat132.Damage.MAGIC),a.cooldown,warnings)
 }
 fun relative(db:GameData13.Snapshot,hero:String,base:Metrics,next:Metrics):Double{
  fun ratio(a:Double?,b:Double?)=if(a!=null&&b!=null&&a>0) ((b-a)/a).coerceIn(-.9,4.0) else null
  val h=db.hero(hero)?:return 0.0
  val offense=listOfNotNull(ratio(base.basicHit,next.basicHit),ratio(base.sampleSkillDamage,next.sampleSkillDamage))
  val defense=listOfNotNull(ratio(base.physicalEhp,next.physicalEhp),ratio(base.magicEhp,next.magicEhp))
  val off=if(offense.isEmpty())0.0 else offense.average()
  val def=if(defense.isEmpty())0.0 else defense.average()
  val tank="Tank" in h.roles||"Support" in h.roles
  val cdr=(next.cooldown-base.cooldown)/100.0
  return if(tank)3*off+10*def+3*cdr else 10*off+3*def+3*cdr
 }
 /** Rerank provided candidates only; never claim global optimum. */
 fun rank(db:GameData13.Snapshot,hero:String,level:Int,candidates:List<List<String>>,targets:List<StrategicEngine.Player>,setup:Loadout14.Selection?=null):List<Rated>{
  val baseline=metrics(db,hero,level,emptyList(),targets,setup)?:return emptyList()
  return candidates.take(80).filter{legal(db,it)}.distinct().mapNotNull{items->
   val m=metrics(db,hero,level,items,targets,setup)?:return@mapNotNull null
   Rated(items,relative(db,hero,baseline,m),m)
  }.sortedWith(compareByDescending<Rated>{it.score}.thenBy{it.metrics.price?:Int.MAX_VALUE}.thenBy{it.items.joinToString("|")})
 }
 /** Compare single and double swaps using explicit in-game sale values by slot.
  * No unknown sale ratio is filled in; recommendations are not purchases.
  */
 fun swaps(db:GameData13.Snapshot,hero:String,level:Int,stock:List<String>,available:List<String>,saleBySlot:Map<Int,Int>,budget:Int?,targets:List<StrategicEngine.Player>):List<Swap>{
  if(stock.size!=6||budget==null||budget<0)return emptyList()
  val inventory=stock.map{db.canonicalItem(it)?:return emptyList()}
  val pool=available.mapNotNull(db::canonicalItem).distinct().filter{it !in inventory&&Strategy13.eligible(db,db.items.getValue(it))}.take(18)
  val slots=saleBySlot.keys.filter{it in inventory.indices&&saleBySlot.getValue(it)>=0&&saleBySlot.getValue(it)<=(db.items[inventory[it]]?.price?:-1)}.sorted()
  val base=metrics(db,hero,level,inventory,targets)?:return emptyList()
  val out=mutableListOf<Swap>()
  val cache=mutableMapOf<List<String>,Metrics?>()
  fun test(ids:List<Int>,new:List<String>){
   val changed=inventory.toMutableList();ids.zip(new).forEach{(i,n)->changed[i]=n}
   if(!legal(db,changed))return
   val cost=new.map{db.items[it]?.price?:return}.sum()-ids.sumOf{saleBySlot.getValue(it)}
   if(cost>budget)return
   val m=cache.getOrPut(changed.sorted()){metrics(db,hero,level,changed,targets)}?:return
   val score=relative(db,hero,base,m)
   if(score>0)out.add(Swap(ids,ids.map{inventory[it]},new,cost,score))
  }
  for(i in slots)for(n in pool)test(listOf(i),listOf(n))
  for(i in slots.indices)for(j in i+1 until slots.size)for(a in pool.indices)for(b in a+1 until pool.size){
   // Final inventories are unordered; no need to duplicate equivalent assignments.
   test(listOf(slots[i],slots[j]),listOf(pool[a],pool[b]))
  }
  return out.sortedWith(compareByDescending<Swap>{it.score}.thenBy{it.netCost}.thenBy{it.newItems.joinToString("|")}).take(5)
 }
}
