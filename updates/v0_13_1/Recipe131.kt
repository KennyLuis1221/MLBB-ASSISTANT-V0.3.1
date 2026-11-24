package com.mlbb.assistant

/** Exact allocation of up to six owned components to a recipe tree.
 * All prices are reference prices, no live sale value assumed.
 */
object Recipe131 {
 data class Quote(val cost:Int,val consumed:Int,val component:String?=null)
 private data class Node(val name:String,val price:Int,val children:List<Node>)
 private data class Allocation(val mask:Int,val cost:Int,val actions:List<Action>)
 private data class Action(val name:String,val cost:Int,val consumed:Int)
 fun quote(db:GameData13.Snapshot,target:String,owned:List<String>,budget:Int?):Quote?{
  if(owned.size>6)return null
  val stock=owned.map{db.canonicalItem(it)?:return null}
  fun tree(name:String,parents:Set<String>):Node?{
   if(name in parents)return null
   val item=db.items[name]?:return null
   val price=item.price?.takeIf{it>0}?:return null
   val children=item.recipe.map{tree(it,parents+name)?:return null}
   if(children.sumOf{it.price}>price)return null
   return Node(name,price,children)
  }
  val root=tree(db.canonicalItem(target)?:return null,emptySet())?:return null
  fun solve(n:Node,mask:Int,allowWhole:Boolean):List<Allocation>{
   val direct=if(allowWhole)stock.indices.filter{stock[it]==n.name&&mask and (1 shl it)==0}.map{Allocation(mask or (1 shl it),0,emptyList())}else emptyList()
   var paths=listOf(Allocation(mask,0,emptyList()))
   for(child in n.children){
    paths=paths.flatMap{p->solve(child,p.mask,true).map{q->Allocation(q.mask,p.cost+q.cost,p.actions+q.actions)}}
      .groupBy{it.mask}.values.map{group->group.minByOrNull{it.cost}!!}
   }
   val surcharge=n.price-n.children.sumOf{it.price}
   val assembled=paths.map{p->
    val cost=p.cost+surcharge
    val action=Action(n.name,cost,Integer.bitCount(p.mask xor mask))
    Allocation(p.mask,cost,p.actions+action)
   }
   return (direct+assembled).groupBy{it.mask}.values.map{it.minByOrNull{p->p.cost}!!}
  }
  val best=solve(root,0,false).sortedWith(compareBy<Allocation>{it.cost}.thenByDescending{Integer.bitCount(it.mask)}).firstOrNull()?:return null
  val free=budget?.takeIf{it>=0}
  val action=if(free!=null&&free<best.cost)best.actions.filter{it.name!=root.name&&it.cost in 1..free&&(stock.size<6||it.consumed>0)}.maxWithOrNull(compareBy<Action>{it.cost}.thenBy{it.consumed}.thenBy{it.name}) else null
  return Quote(best.cost,Integer.bitCount(best.mask),action?.name)
 }
}
