package com.mlbb.assistant
import org.junit.Test
import org.junit.Assert.*
class BoardCalibrationTest{
 @Test fun validGeometry(){
  val p=listOf(.12 to .25,.12 to .81,.88 to .25,.88 to .81,.19 to .27,.39 to .27,.81 to .27,.61 to .27,.14 to .25,.205 to .27).map{BoardCalibration.Point(it.first,it.second)}
  val l=BoardCalibration.create(2.0,p)!!
  assertEquals(l,BoardCalibration.decode(l.encode()))
  assertEquals(.53,l.hero(0,2).y,1e-8)
  assertEquals(.73,l.item(1,2,2).x,1e-8)
  assertEquals(.55,l.item(0,2,1).y,1e-8)
  assertTrue(l.compatible(1600,800))
  assertFalse(l.compatible(1600,900))
  assertNull(BoardCalibration.create(2.0,p.reversed()))
  assertNull(BoardCalibration.decode("invalid"))
 }
}
