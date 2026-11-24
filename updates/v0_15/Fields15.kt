package com.mlbb.assistant

/** Explicitly calibrated fields; no use of a generic scoreboard number as level or cash. */
object Fields15 {
 data class Box(val x0:Double,val y0:Double,val x1:Double,val y1:Double){
  fun contains(x:Double,y:Double)=x in x0..x1&&y in y0..y1
  fun move(dx:Double,dy:Double)=Box(x0+dx,y0+dy,x1+dx,y1+dy)
  fun valid()=listOf(x0,y0,x1,y1).all{it.isFinite()&&it in 0.0..1.0}&&x1-x0 in .006.. .18&&y1-y0 in .008.. .12
 }
 data class Profile(val aspect:Double,val boxes:List<Box>,val boardId:String?){
  fun compatible(w:Int,h:Int)=w>h&&h>0&&kotlin.math.abs(w.toDouble()/h/aspect-1)<.025
  fun encode()=(listOf(aspect.toString(),boardId?:"-")+boxes.flatMap{listOf(it.x0.toString(),it.y0.toString(),it.x1.toString(),it.y1.toString())}).joinToString("|")
  fun levelBox(board:BoardCalibration.Layout,side:Int,row:Int):Box?{
   if(board.encode()!=boardId||boxes.size!=2||side !in 0..1||row !in 0..4)return null
   val first=board.hero(side,0);val slot=board.hero(side,row)
   return boxes[side].move(slot.x-first.x,slot.y-first.y).takeIf{it.valid()}
  }
 }
 fun create(aspect:Double,points:List<BoardCalibration.Point>,board:BoardCalibration.Layout?=null):Profile?{
  if(!aspect.isFinite()||aspect !in 1.3..3.0||points.size!=(if(board==null)2 else 4))return null
  val boxes=points.chunked(2).map{(a,b)->Box(minOf(a.x,b.x),minOf(a.y,b.y),maxOf(a.x,b.x),maxOf(a.y,b.y))}
  if(boxes.any{!it.valid()})return null
  if(board!=null){
   if(kotlin.math.abs(aspect/board.aspect-1)>=.025||boxes[0].x1>.51||boxes[1].x0<.49)return null
   for(side in 0..1){
    val h=board.hero(side,0)
    if(kotlin.math.abs((boxes[side].y0+boxes[side].y1)/2-h.y)>.065)return null
   }
  }
  val result=Profile(aspect,boxes,board?.encode())
  if(board!=null&&(0..1).any{s->(0..4).any{result.levelBox(board,s,it)==null}})return null
  return result
 }
 fun decode(s:String?,board:BoardCalibration.Layout?=null):Profile?{
  val v=s?.split("|")?:return null
  if(v.size!=(if(board==null)6 else 10)||v[1]!=(board?.encode()?:"-"))return null
  val aspect=v[0].toDoubleOrNull()?:return null
  val nums=v.drop(2).map{it.toDoubleOrNull()?:return null}
  return create(aspect,nums.chunked(2).map{BoardCalibration.Point(it[0],it[1])},board)
 }
 fun level(tokens:List<String>):Int?{
  val s=tokens.joinToString(" ").trim()
  val m=Regex("(?i)^(?:(?:lv\\.?|lvl\\.?|nivel|nível)\\s*)?([0-9]{1,2})$").matchEntire(s)?:return null
  return m.groupValues[1].toIntOrNull()?.takeIf{it in 1..15}
 }
 fun budget(tokens:List<String>):Int?{
  val s=tokens.joinToString(" ").trim()
  val m=Regex("(?i)^(?:(?:ouro|gold)\\s*:?\\s*)?([0-9]{1,5}|[0-9]{1,2}(?:[.,][0-9]{3}))$").matchEntire(s)?:return null
  return m.groupValues[1].replace(".","").replace(",","").toIntOrNull()?.takeIf{it in 0..99999}
 }
 class Repeated(private val ttl:Long){
  private var value:Int?=null;private var at=0L
  fun clear(){value=null;at=0}
  fun next(candidate:Int?,now:Long):Int?{
   if(now<=0||candidate==null){clear();return null}
   val confirmed=candidate==value&&at>0&&now>at&&now-at<=ttl
   value=candidate;at=now
   return candidate.takeIf{confirmed}
  }
 }
}
