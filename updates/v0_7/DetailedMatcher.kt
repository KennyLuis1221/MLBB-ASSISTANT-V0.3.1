package com.mlbb.assistant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Visual candidates, not a trained skin classifier. 64 gradient bits and 4 RGB regions. */
class DetailedMatcher(context: Context) {
    data class Hit(val name: String, val distance: Double, val x: Int, val y: Int, val size: Int)
    private data class Ref(val name: String, val type: Char, val bytes: ByteArray)
    private val refs = mutableListOf<Ref>()
    init {
        runCatching {
            val lines = context.assets.open("references_v7.txt").bufferedReader().use { it.readLines() }
            for (n in listOf(0,2)) {
                val names=lines[n].split('|')
                val bytes=Base64.decode(lines[n+1], Base64.DEFAULT)
                require(bytes.size == names.size*20)
                names.forEachIndexed { j,name -> refs.add(Ref(name, if(n==0) 'h' else 'i',
                    bytes.copyOfRange(j*20,j*20+20))) }
            }
        }
    }
    val count get() = refs.size
    private fun luminance(c:Int)=.299*Color.red(c)+.587*Color.green(c)+.114*Color.blue(c)
    private fun feature(b: Bitmap, x:Int,y:Int,s:Int): ByteArray? {
        val half=(s*.375).toInt().coerceAtLeast(5)
        val x0=x-half; val y0=y-half; val d=half*2
        if(x0<0||y0<0||x0+d>b.width||y0+d>b.height) return null
        val patch=Bitmap.createBitmap(b,x0,y0,d,d)
        val a=Bitmap.createScaledBitmap(patch,9,8,true)
        val out=ByteArray(20)
        var contrast=0.0
        for (r in 0..7) for(c in 0..7) {
            val diff=luminance(a.getPixel(c+1,r))-luminance(a.getPixel(c,r))
            contrast+=abs(diff)
            if(diff>0) out[r]=(out[r].toInt() or (1 shl (7-c))).toByte()
        }
        if(a!==patch) a.recycle()
        if(contrast/64<5) { patch.recycle();return null }
        // Mean color of each quadrant, not a single sampled pixel.
        val colors=IntArray(12); val totals=IntArray(4)
        for (yy in 0 until d) for(xx in 0 until d) {
            val p=patch.getPixel(xx,yy);val q=(if(yy>=half)2 else 0)+(if(xx>=half)1 else 0)
            colors[q*3]+=Color.red(p);colors[q*3+1]+=Color.green(p);colors[q*3+2]+=Color.blue(p);totals[q]++
        }
        patch.recycle()
        for(k in 0..11) out[k+8]=(colors[k]/totals[k/3]).toByte()
        return out
    }
    fun match(b:Bitmap,x:Int,y:Int,size:Int,type:Char):Hit? {
        val v=feature(b,x,y,size)?:return null
        var best:Ref?=null;var score=1e9;var second=1e9
        for(r in refs) {
            if(r.type!=type)continue
            var bits=0
            for(j in 0..7) bits+=Integer.bitCount((v[j].toInt() xor r.bytes[j].toInt()) and 255)
            var rgb=0
            for(j in 8..19) rgb+=abs((v[j].toInt() and 255)-(r.bytes[j].toInt() and 255))
            val dist=bits/64.0*.64+rgb/(12.0*255.0)*.36
            if(dist<score){second=score;score=dist;best=r}else if(dist<second)second=dist
        }
        // Conservative rejection; thresholds require in-game calibration.
        if(best==null||score>.205||second-score<.032)return null
        return Hit(best.name,score,x,y,size)
    }
    data class Row(val side:Int,val index:Int,val hero:Hit?,val items:List<Hit>,val centerY:Int)
    data class Board(val rows:List<Row>,val support:Int)
    private data class Grid(val start:Double,val step:Double,val left:Double,val right:Double,
        val leftItems:Double,val rightItems:Double,val itemStep:Double,val dy:Double,val heroSize:Double,val itemSize:Double)
    fun board(b:Bitmap):Board {
        val w=b.width;val h=b.height
        // Multiple hypotheses: full-width scoreboard, inset board and alternate row positions.
        // No hypothesis is trusted without at least three distinct portrait candidates.
        val grids=listOf(
            Grid(.265,.128,.165,.835,.208,.643,.030,.026,.044,.027),
            Grid(.35,.087,.135,.87,.18,.66,.028,.027,.045,.028),
            Grid(.35,.087,.135,.72,.18,.55,.025,.028,.043,.023),
            Grid(.275,.11,.15,.85,.19,.635,.027,.025,.043,.024))
        var chosen:Grid?=null;var selected=emptyList<Row>();var best=0
        for(g in grids) {
            val rows=mutableListOf<Row>()
            for(side in 0..1)for(k in 0..4) {
                val y=((g.start+k*g.step)*h).toInt()
                val x=((if(side==0)g.left else g.right)*w).toInt()
                val size=(g.heroSize*w).toInt()
                val hit=match(b,x,y,size,'h')
                rows.add(Row(side,k,hit,emptyList(),y))
            }
            val n=rows.mapNotNull{it.hero?.name}.distinct().size
            if(n>best){chosen=g;selected=rows;best=n}
        }
        val g=chosen?:return Board(emptyList(),0)
        if(best<3||selected.count{it.side==0&&it.hero!=null}<1||selected.count{it.side==1&&it.hero!=null}<1)
            return Board(emptyList(),best)
        val final=selected.map { row ->
            if(row.hero==null) row else {
                val start=if(row.side==0)g.leftItems else g.rightItems
                val items=(0..5).mapNotNull{k->
                    match(b,((start+g.itemStep*k)*w).toInt(),row.centerY+(g.dy*h).toInt(),
                        (g.itemSize*w).toInt(),'i')
                }
                row.copy(items=items)
            }
        }
        return Board(final,best)
    }
    fun thumbnail(b:Bitmap,hit:Hit):Bitmap? {
        val s=hit.size; val x=hit.x-s/2;val y=hit.y-s/2
        if(x<0||y<0||x+s>b.width||y+s>b.height)return null
        val crop=Bitmap.createBitmap(b,x,y,s,s)
        val thumb=Bitmap.createScaledBitmap(crop,48,48,true)
        if(crop!==thumb)crop.recycle()
        return thumb
    }
    /** Self highlight candidate: one distinctly brighter blue strip than all other rows.
     * Never turns this visual hypothesis into a confirmed identity without repeat evidence. */
    fun highlight(b:Bitmap,rows:List<Row>):Row? {
        val scores=rows.filter{it.hero!=null}.map { r ->
            val h=r.hero!!;var sum=0.0
            val w=b.width
            val start=if(r.side==0) .26 else .55
            for(i in 0..8) {
                val x=((start+i*.022)*w).toInt().coerceIn(0,w-1)
                val y=(r.centerY+h.size*.46).toInt().coerceIn(0,b.height-1)
                val c=b.getPixel(x,y)
                sum+=Color.blue(c)*.6+Color.green(c)*.4-Color.red(c)*.45
            }
            r to sum/9
        }.sortedByDescending{it.second}
        val first=scores.firstOrNull()?:return null
        val second=scores.getOrNull(1)?:return null
        return if(first.second>65&&first.second-second.second>25)first.first else null
    }
}
