package com.mlbb.assistant
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/** Marking coordinates in a user-selected screenshot. No image saved or uploaded. */
class CalibrationView(context:Context,private val bitmap:Bitmap,private val changed:(List<BoardCalibration.Point>)->Unit):View(context) {
 private val points=mutableListOf<BoardCalibration.Point>()
 private val bounds=RectF()
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
 override fun onDraw(c:Canvas){
  super.onDraw(c)
  val scale=minOf(width.toFloat()/bitmap.width,height.toFloat()/bitmap.height)
  val w=bitmap.width*scale;val h=bitmap.height*scale
  bounds.set((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2)
  c.drawBitmap(bitmap,null,bounds,paint)
  paint.strokeWidth=2f;paint.color=Color.YELLOW;paint.style=Paint.Style.STROKE
  points.forEachIndexed{i,p->
   val x=bounds.left+p.x.toFloat()*bounds.width();val y=bounds.top+p.y.toFloat()*bounds.height()
   c.drawCircle(x,y,8f,paint);c.drawLine(x-12,y,x+12,y,paint);c.drawLine(x,y-12,x,y+12,paint)
   paint.style=Paint.Style.FILL;paint.textSize=21f;c.drawText((i+1).toString(),x+10,y-10,paint);paint.style=Paint.Style.STROKE
  }
  BoardCalibration.create(bitmap.width.toDouble()/bitmap.height,points)?.let{layout->
   paint.color=Color.CYAN
   for(s in 0..1)for(r in 0..4){
    val slots=listOf(layout.hero(s,r))+(0..5).map{layout.item(s,r,it)}
    slots.forEach{p->val x=bounds.left+p.x.toFloat()*bounds.width();val y=bounds.top+p.y.toFloat()*bounds.height();val size=p.size.toFloat()*bounds.width()/2;c.drawRect(x-size,y-size,x+size,y+size,paint)}
   }
  }
  paint.style=Paint.Style.FILL
 }
 override fun onTouchEvent(e:MotionEvent):Boolean{
  if(e.actionMasked==MotionEvent.ACTION_DOWN){parent?.requestDisallowInterceptTouchEvent(true);return true}
  if(e.actionMasked==MotionEvent.ACTION_UP){
   parent?.requestDisallowInterceptTouchEvent(false)
   if(points.size<10&&bounds.contains(e.x,e.y)){
    points.add(BoardCalibration.Point(((e.x-bounds.left)/bounds.width()).toDouble(),((e.y-bounds.top)/bounds.height()).toDouble()))
    invalidate();changed(points.toList())
   }
   performClick();return true
  }
  return true
 }
 override fun performClick():Boolean{super.performClick();return true}
 fun undo(){if(points.isNotEmpty()){points.removeAt(points.lastIndex);invalidate();changed(points.toList())}}
 fun reset(){points.clear();invalidate();changed(emptyList())}
}
