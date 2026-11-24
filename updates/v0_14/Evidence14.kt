package com.mlbb.assistant

/** Only compares independently repeated slot values. Unknown is never empty or purchased. */
object Evidence14 {
 const val MAX_AGE_MS=45_000L
 const val MAX_FRAME_GAP_MS=20_000L
 data class Slot(val index:Int,val name:String?,val distance:Double?=null,val margin:Double?=null)
 data class Player(val hero:String,val side:Int,val row:Int,val slots:List<Slot>,val gold:Int?,val kda:String?)
 data class Frame(val observedAt:Long,val clockSeconds:Int?,val players:List<Player>)
 data class Delta(val hero:String,val replacements:List<String>,val goldDelta:Int?,val kdaChanged:Boolean)
 data class Update(val accepted:Boolean,val reset:Boolean,val changes:List<Delta>,val frames:Int)
 fun stableSlots(previous:List<Slot>,current:List<Slot>):List<Slot>{
  val old=previous.groupBy{it.index}
  return current.groupBy{it.index}.mapNotNull{(index,values)->
   if(index !in 0..5||values.size!=1)return@mapNotNull null
   val v=values[0];val p=old[index]?.singleOrNull()
   if(v.name!=null&&p?.name==v.name)v else null
  }.sortedBy{it.index}
 }
 fun age(now:Long,observedAt:Long):Long?=if(observedAt>0&&now>=observedAt)now-observedAt else null
 fun fresh(now:Long,observedAt:Long)=age(now,observedAt)?.let{it<=MAX_AGE_MS}==true
 fun pairFresh(now:Long,previousAt:Long)=age(now,previousAt)?.let{it<=MAX_FRAME_GAP_MS}==true
 class History {
  private val frames=ArrayDeque<Frame>()
  fun clear(){frames.clear()}
  fun snapshot():List<Frame> = frames.toList()
  fun record(frame:Frame):Update{
   if(frame.observedAt<=0||frame.players.isEmpty()||frame.players.size>10||
      frame.players.map{it.hero}.distinct().size!=frame.players.size||
      frame.players.map{it.side to it.row}.distinct().size!=frame.players.size||
      frame.players.any{it.hero.isBlank()||it.side !in 0..1||it.row !in 0..4||it.slots.map{v->v.index}.distinct().size!=it.slots.size||it.slots.any{v->v.index !in 0..5}}||
      frame.clockSeconds?.let{it<0}==true)return Update(false,false,emptyList(),frames.size)
   val previous=frames.lastOrNull()
   if(previous!=null&&frame.observedAt<=previous.observedAt)return Update(false,false,emptyList(),frames.size)
   // Do not merge roster changes into old match history. A clock rollback is already confirmed by the OCR gate.
   val overlap=previous?.players?.map{it.hero}?.toSet()?.intersect(frame.players.map{it.hero}.toSet())?.size?:0
   val reset=previous!=null&&(!fresh(frame.observedAt,previous.observedAt)||
       previous.clockSeconds!=null&&frame.clockSeconds!=null&&previous.clockSeconds-frame.clockSeconds>30||
       previous.players.size>=6&&frame.players.size>=6&&overlap<=2)
   if(reset)clear()
   val changes=if(previous==null||reset)emptyList()else frame.players.mapNotNull{p->
    val q=previous.players.singleOrNull{it.hero==p.hero&&it.side==p.side}?:return@mapNotNull null
    val slots=q.slots.associateBy{it.index}
    val swaps=p.slots.mapNotNull{s->
     val old=slots[s.index]?.name
     if(s.name!=null&&old!=null&&s.name!=old)"slot "+(s.index+1)+": "+old+" → "+s.name else null
    }
    val gold=if(p.gold!=null&&q.gold!=null&&p.gold>=q.gold)p.gold-q.gold else null
    val k=p.kda!=null&&q.kda!=null&&p.kda!=q.kda
    if(swaps.isEmpty()&&(gold==null||gold==0)&&!k)null else Delta(p.hero,swaps,gold,k)
   }
   // Store only structured observations in session memory, never images.
   frames.addLast(frame.copy(players=frame.players.map{it.copy(slots=it.slots.toList())}))
   while(frames.size>64)frames.removeFirst()
   return Update(true,reset,changes,frames.size)
  }
 }
}
