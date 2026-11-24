package com.mlbb.assistant
import org.junit.Assert.*
import org.junit.Test
class Evidence14Test {
 @Test fun stableSlotsAndRepeatedItems(){
  val a=listOf(Evidence14.Slot(0,"Dagger"),Evidence14.Slot(1,"Dagger"),Evidence14.Slot(2,"Boots"))
  val b=listOf(Evidence14.Slot(0,"Dagger"),Evidence14.Slot(1,"Knife"),Evidence14.Slot(2,null))
  assertEquals(listOf(Evidence14.Slot(0,"Dagger")),Evidence14.stableSlots(a,b))
  assertEquals(a,Evidence14.stableSlots(a,a))
  assertTrue(Evidence14.stableSlots(a,listOf(Evidence14.Slot(0,"Dagger"),Evidence14.Slot(0,"Dagger"))).isEmpty())
 }
 @Test fun history(){
  val h=Evidence14.History()
  fun p(gold:Int,slots:List<Evidence14.Slot>)=Evidence14.Player("Hanzo",0,1,slots,gold,"1/0/2")
  val a=Evidence14.Frame(1000,100,listOf(p(2000,listOf(Evidence14.Slot(0,"Boots"),Evidence14.Slot(1,"Dagger")))))
  assertTrue(h.record(a).accepted)
  val b=Evidence14.Frame(5000,104,listOf(p(2400,listOf(Evidence14.Slot(0,"Swift Boots")))))
  val u=h.record(b);assertEquals(2,u.frames);assertEquals(400,u.changes.single().goldDelta)
  assertEquals(listOf("slot 1: Boots → Swift Boots"),u.changes.single().replacements)
  assertFalse(h.record(a).accepted)
  val rollback=h.record(b.copy(observedAt=10000,clockSeconds=10))
  assertTrue(rollback.reset);assertEquals(1,rollback.frames)
  assertTrue(h.record(b.copy(observedAt=60000,clockSeconds=60)).reset)
  assertFalse(Evidence14.fresh(46001,1000))
  assertFalse(Evidence14.pairFresh(21001,1000))
  assertFalse(Evidence14.fresh(0,1000))
 }
}
