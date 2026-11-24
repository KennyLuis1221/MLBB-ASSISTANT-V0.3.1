package com.mlbb.assistant

/** Explicit formulas from the 2.2.23 community patch mirror. No implicit proc activation. */
object Patch15 {
 const val version="2.2.23"
 const val date="2026-11-11"
 const val source="https://mlbbhub.com/patch-notes/original/2.2.23"
 val changedHeroes=setOf("Popol and Kupa","Xavier","Lunox","Freya","Aldous","Kaja","Melissa","Gloo","Masha","Atlas","Lukas","Karrie","Kagura","Uranus")
 val changedItems=setOf("Chastise Pauldron","Antique Cuirass")
 fun atlasDefense(extraHp:Double):Double?=extraHp.takeIf{it.isFinite()&&it>=0}?.let{20+.007*it}
 fun concussiveBlast(maxHp:Double,minion:Boolean):Double?=maxHp.takeIf{it.isFinite()&&it>=0}?.let{(100+.08*it)*if(minion).5 else 1.0}
 fun chastiseSpeed(attackSpeed:Double,cap:Double,active:Boolean):Pair<Double,Double>?{
  if(listOf(attackSpeed,cap).any{!it.isFinite()||it<0})return null
  val c=cap*(if(active).8 else 1.0)
  return minOf(c,attackSpeed*(if(active).8 else 1.0)) to c
 }
 // Caller supplies confirmed stack count; only per-stack multiplier checked, stack limit remains external.
 fun deterPhysical(raw:Double,stacks:Int,maxStacks:Int):Double?{
  if(!raw.isFinite()||raw<0||maxStacks !in 1..10||stacks !in 0..maxStacks)return null
  return raw*(1-.08*stacks).coerceAtLeast(0.0)
 }
 fun isSnapshotCertified(installed:String?)=false // evidence for whole snapshot is absent, even when version matches
}
