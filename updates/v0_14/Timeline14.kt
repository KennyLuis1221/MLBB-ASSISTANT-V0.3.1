package com.mlbb.assistant

/** Reusable deterministic combat timeline. Timings and values are explicit scenario inputs. */
object Timeline14 {
 enum class Kind { HIT, HEAL, SHIELD, IMMUNE, REDUCTION, CAST, PERIODIC }
 data class Action(val time:Double,val kind:Kind,val amount:Double=0.0,val duration:Double=0.0,
   val type:Combat132.Damage=Combat132.Damage.PHYSICAL,val id:String="",val cooldown:Double=0.0,
   val cost:Double=0.0,val interval:Double=0.0,val count:Int=1,val crit:Boolean=false,
   val ignoreShield:Boolean=false,val onHit:Boolean=false,val lifebane:Boolean=false)
 data class Trace(val time:Double,val kind:Kind,val hp:Double,val shield:Double,val resource:Double?,val detail:String)
 data class Result(val hp:Double,val shield:Double,val resource:Double?,val killTime:Double?,val rows:List<Trace>)
 private data class Shield(var amount:Double,val until:Double)
 fun run(a:Combat132.Stats,d:Combat132.Stats,actions:List<Action>,initialResource:Double?=a.mana,
         maxResource:Double?=a.mana,resourceRegenPerSec:Double=0.0,initialShield:Double=0.0):Result?{
  val maxHp=d.hp?.takeIf{it.isFinite()&&it>0}?:return null
  if(actions.size>120||!initialShield.isFinite()||initialShield<0||!resourceRegenPerSec.isFinite()||resourceRegenPerSec<0)return null
  if(initialResource!=null&&(!initialResource.isFinite()||initialResource<0)||maxResource!=null&&(!maxResource.isFinite()||maxResource<0)||
      initialResource!=null&&maxResource!=null&&initialResource>maxResource)return null
  val list=mutableListOf<Action>()
  for(x in actions){
   if(listOf(x.time,x.amount,x.duration,x.cooldown,x.cost,x.interval).any{!it.isFinite()||it<0}||x.time>180||x.duration>180||x.count !in 1..100)return null
   if(x.kind==Kind.PERIODIC){
    if(x.count>1&&x.interval<=0)return null
    repeat(x.count){i->val at=x.time+i*x.interval;if(at>180)return null;list.add(x.copy(time=at,kind=Kind.HIT,count=1))}
   }else list.add(x)
  }
  if(list.size>1000)return null
  var hp=maxHp;var time=0.0;var resource=initialResource;var kill:Double?=null
  var immunityUntil=-1.0
  val shields=mutableListOf<Shield>();if(initialShield>0)shields.add(Shield(initialShield,Double.POSITIVE_INFINITY))
  val reductions=mutableListOf<Pair<Double,Double>>()
  val ready=mutableMapOf<String,Double>();val failedCasts=mutableSetOf<String>();val trace=mutableListOf<Trace>()
  for(x in list.sortedBy{it.time}){
   val dt=x.time-time
   resource=resource?.let{(it+dt*resourceRegenPerSec).let{v->if(maxResource!=null)v.coerceAtMost(maxResource)else v}}
   time=x.time
   shields.removeAll{it.until<=time||it.amount<=0}
   reductions.removeAll{it.second<=time}
   if(kill!=null)break
   var note=""
   when(x.kind){
    Kind.CAST->{
      if(x.id.isBlank())return null
      if(time+1e-9<(ready[x.id]?:0.0)){failedCasts.add(x.id);note="recarga"}
      else if(x.cost>0&&(resource==null||resource!!<x.cost)){failedCasts.add(x.id);note="recurso insuficiente/desconhecido"}
      else{resource=resource?.minus(x.cost);ready[x.id]=time+Combat132.cooldown(x.cooldown,a);failedCasts.remove(x.id);note="habilidade utilizada"}
    }
    Kind.IMMUNE->{immunityUntil=maxOf(immunityUntil,time+x.duration);note="imunidade de cenário"}
    Kind.REDUCTION->{reductions.add(x.amount.coerceIn(0.0,100.0) to time+x.duration);note="redução explícita"}
    Kind.HEAL->{hp=(hp+Combat132.shieldOrHeal(x.amount,d,x.lifebane)).coerceAtMost(maxHp);note="cura"}
    Kind.SHIELD->{shields.add(Shield(Combat132.shieldOrHeal(x.amount,d,x.lifebane),time+x.duration));note="escudo"}
    Kind.HIT->{
      if(x.id.isNotBlank()&&(x.id in failedCasts||x.id !in ready)){note="evento sem cast válido"}
      else if(time<immunityUntil){note="imune"}
      else{
       var total=Combat132.hit(Combat132.Hit(x.amount,x.type,critAllowed=x.crit),a,d)?:return null
       if(x.onHit){total+=Combat132.demonHunterOnHit(hp,a,d)?:return null}
       // Conditional reductions multiply, unless true damage; no hidden proc assumptions.
       if(x.type!=Combat132.Damage.TRUE)for(r in reductions)total*=1-r.first/100.0
       var rem=total
       if(!x.ignoreShield)for(s in shields.sortedBy{it.until}){
        val used=minOf(rem,s.amount);s.amount-=used;rem-=used;if(rem<=0)break
       }
       hp=(hp-rem).coerceAtLeast(0.0);if(hp==0.0)kill=time
       note="dano mitigado "+total
      }
    }
    Kind.PERIODIC->error("expanded")
   }
   trace.add(Trace(time,x.kind,hp,shields.sumOf{it.amount},resource,note))
  }
  return Result(hp,shields.sumOf{it.amount},resource,kill,trace)
 }
}
