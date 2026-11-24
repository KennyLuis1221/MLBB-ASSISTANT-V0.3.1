package com.mlbb.assistant
import org.junit.Assert.*
import org.junit.Test
class Fields15Test{
 @Test fun parse(){
  assertEquals(15,Fields15.level(listOf("Lv.","15")))
  assertEquals(1,Fields15.level(listOf("1")))
  assertNull(Fields15.level(listOf("16")))
  assertNull(Fields15.level(listOf("3","4")))
  assertNull(Fields15.level(listOf("1/2/3")))
  assertEquals(12345,Fields15.budget(listOf("Gold","12,345")))
  assertEquals(0,Fields15.budget(listOf("0")))
  assertNull(Fields15.budget(listOf("1.2")))
  assertNull(Fields15.budget(listOf("120","140")))
 }
 @Test fun boxes(){
  val p=listOf(.12 to .25,.12 to .81,.88 to .25,.88 to .81,.19 to .27,.39 to .27,.81 to .27,.61 to .27,.14 to .25,.205 to .27).map{BoardCalibration.Point(it.first,it.second)}
  val board=BoardCalibration.create(2.0,p)!!
  val f=Fields15.create(2.0,listOf(.13 to .24,.15 to .26,.85 to .24,.87 to .26).map{BoardCalibration.Point(it.first,it.second)},board)!!
  assertEquals(f,Fields15.decode(f.encode(),board))
  assertEquals(.52,f.levelBox(board,0,2)!!.y0,1e-8)
  assertFalse(f.compatible(1600,900))
  assertNull(Fields15.decode(f.encode()))
  val gold=Fields15.create(2.0,listOf(BoardCalibration.Point(.80,.10),BoardCalibration.Point(.9,.14)))!!
  assertEquals(gold,Fields15.decode(gold.encode()))
 }
 @Test fun repeats(){
  val r=Fields15.Repeated(12000)
  assertNull(r.next(100,1000))
  assertEquals(100,r.next(100,2000))
  assertNull(r.next(101,3000))
  assertNull(r.next(101,16000))
  assertEquals(101,r.next(101,17000))
  assertNull(r.next(null,18000))
  assertNull(r.next(101,19000))
 }
}
