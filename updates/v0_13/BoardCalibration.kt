package com.mlbb.assistant

/** A user-supplied scoreboard geometry. Only relative coordinates are persisted. */
object BoardCalibration {
 data class Point(val x:Double,val y:Double)
 data class Slot(val x:Double,val y:Double,val size:Double)
 data class Layout(val aspect:Double,val points:List<Point>){
  fun encode()=(listOf(aspect.toString())+points.flatMap{listOf(it.x.toString(),it.y.toString())}).joinToString(";")
  fun compatible(w:Int,h:Int)=w>h&&h>0&&kotlin.math.abs(w.toDouble()/h/aspect-1)<.045
  fun hero(side:Int,row:Int):Slot{
   val start=points[side*2];val end=points[side*2+1]
   val f=row/4.0
   return Slot(start.x+(end.x-start.x)*f,start.y+(end.y-start.y)*f,2*kotlin.math.abs(points[8].x-points[0].x))
  }
  fun item(side:Int,row:Int,index:Int):Slot{
   val start=points[4+side*2];val end=points[5+side*2]
   val h=hero(side,row);val first=hero(side,0)
   val f=index/5.0
   return Slot(start.x+(end.x-start.x)*f+h.x-first.x,start.y+(end.y-start.y)*f+h.y-first.y,2*kotlin.math.abs(points[9].x-points[4].x))
  }
 }
 val instructions=listOf("Centro do retrato do 1º jogador à esquerda","Centro do retrato do 5º jogador à esquerda",
  "Centro do retrato do 1º jogador à direita","Centro do retrato do 5º jogador à direita",
  "Centro do slot 1 de equipamento da primeira linha esquerda","Centro do slot 6 da primeira linha esquerda",
  "Centro do slot 1 da primeira linha direita","Centro do slot 6 da primeira linha direita",
  "Borda direita do primeiro retrato esquerdo (mesma altura do centro)","Borda direita do primeiro slot de equipamento esquerdo (mesma altura do centro)")
 fun create(aspect:Double,p:List<Point>):Layout?{
  if(!aspect.isFinite()||aspect !in 1.3..3.0||p.size!=10||p.any{!it.x.isFinite()||!it.y.isFinite()||it.x !in 0.0..1.0||it.y !in 0.0..1.0})return null
  if(p[0].x>=.5||p[2].x<=.5||p[1].x>=.5||p[3].x<=.5)return null
  for(s in 0..1){
   if(p[2*s+1].y-p[2*s].y !in .18.. .78||kotlin.math.abs(p[2*s+1].x-p[2*s].x)>.06)return null
   val a=p[4+2*s];val b=p[5+2*s]
   if(kotlin.math.abs(a.x-b.x) !in .07.. .45||kotlin.math.abs(a.y-b.y)>.035)return null
   if(s==0&&(a.x>.52||b.x>.52)||s==1&&(a.x<.48||b.x<.48))return null
  }
  val hs=2*(p[8].x-p[0].x);val its=2*(p[9].x-p[4].x)
  if(hs !in .012.. .095||its !in .008.. .075||kotlin.math.abs(p[8].y-p[0].y)>.025||kotlin.math.abs(p[9].y-p[4].y)>.025)return null
  val l=Layout(aspect,p.toList())
  for(side in 0..1)for(row in 0..4){
    val slots=listOf(l.hero(side,row))+(0..5).map{l.item(side,row,it)}
    if(slots.any{it.x-it.size/2<0||it.x+it.size/2>1||it.y-it.size*aspect/2<0||it.y+it.size*aspect/2>1})return null
  }
  return l
 }
 fun decode(s:String?):Layout?{
  val a=s?.split(";")?.map{it.toDoubleOrNull()?:return null}?:return null
  if(a.size!=21)return null
  return create(a[0],a.drop(1).chunked(2).map{Point(it[0],it[1])})
 }
}
