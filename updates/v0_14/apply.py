from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
def rep(n,a,z):
 p=b/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,z,1))
p="GameData13.kt"
rep(p,' data class Gap(', ' data class ComboRef(val hero:String,val type:String,val steps:List<String>)\n data class Gap(')
rep(p,'val synergy:Map<Triple<String,String,String>,Double>)','val synergy:Map<Triple<String,String,String>,Double>,val combos:Map<String,List<ComboRef>> = emptyMap())')
rep(p,'var date=""','var date="";val combos=linkedMapOf<String,MutableList<ComboRef>>()')
rep(p,'     "C","Y"->','     "Q"->combos.getOrPut(a[1]){mutableListOf()}.add(ComboRef(a[1],a[2],list(a[3])))\n     "C","Y"->')
rep(p,'val result=Snapshot(date,h,sk,it,bs,c,y)','require(combos.keys==h.keys&&combos.values.sumOf{it.size}==266)\n  val result=Snapshot(date,h,sk,it,bs,c,y,combos)')
p="Combat132.kt"
rep(p,'fun stats(db:GameData13.Snapshot,hero:String,level:Int,items:List<String>):Stats?{','fun stats(db:GameData13.Snapshot,hero:String,level:Int,items:List<String>,setup:Loadout14.Selection?=null,weaponMasterPercent:Double?=null):Stats?{')
rep(p,'if(isUnique&&!unique.add(source+":"+key))continue','''if(key=="adaptive penetration"||key=="adaptive penetration%"){
      val suffix=if(key.endsWith("%"))"%" else ""
      key=if(h.damage=="Magic")"magic penetration"+suffix else if(h.damage=="Physical")"physical penetration"+suffix else{unresolved.add(source+": penetração adaptativa mista");continue}
    }
    if(isUnique&&!unique.add(source+":"+key))continue''')
rep(p,'  fun t(k:String)=totals[k]?:0.0','''  val loadout=setup?.let{Loadout14.resolve(it,weaponMasterPercent)?:return null}
  if(loadout!=null){
    for(v in loadout.attributes)parse(v,"Emblema/talento",false)
    unresolved.addAll(loadout.warnings)
    loadout.weaponMasterPercent?.let{p->for(key in listOf("physical attack","magic power"))totals[key]=(totals[key]?:0.0)*(1+p/100)}
  }
  fun t(k:String)=totals[k]?:0.0''')
rep(p,'plus(t("physical defense")),value(h,"magic_def",level)?.plus(t("magic defense"))','plus(t("physical defense")+t("hybrid defense")),value(h,"magic_def",level)?.plus(t("magic defense")+t("hybrid defense"))')
rep(p,'h.stats["movement_spd"]?.toDoubleOrNull()?.plus(t("movement speed"))','h.stats["movement_spd"]?.toDoubleOrNull()?.plus(t("movement speed"))?.times(1+t("movement speed%")/100)')
rep(p,'heal(amount,if("Oracle" in recipient.items)25.0 else 0.0,lifebane)','heal(amount,if("Oracle" in recipient.items)25.0 else 0.0,lifebane)')
# Received healing is not healing dealt: support emblem healing effect should not be applied to incoming healing.
p="MainActivity.kt"
rep(p,'        val magicGuard=check("Ativar redução mágica temporária de Valkyrie\\\'s Shield no alvo")'.replace("\\'","'"),'''        val emblem=input("Emblema atacante (opcional, nível 60)","")
        val talents=input("Três talentos, separados por vírgula (opcional)","")
        val spell=input("Feitiço atacante (opcional)","")
        val magicGuard=check("Ativar redução mágica temporária de Valkyrie's Shield no alvo")''')
rep(p,'hits,sequence,stacks,shield,rate,heal))','hits,sequence,stacks,emblem,talents,spell,shield,rate,heal))')
rep(p,'val a=h?.let{Combat132.stats(db,it.name,level.text.toString().toIntOrNull()?:0,list(items.text.toString()))}','''val setup=if(listOf(emblem,talents,spell).any{it.text.toString().isNotBlank()})Loadout14.Selection(emblem.text.toString(),60,list(talents.text.toString()),spell.text.toString()) else null
            val a=h?.let{Combat132.stats(db,it.name,level.text.toString().toIntOrNull()?:0,list(items.text.toString()),setup)}''')
rep(p,'val extracted=Combat132.events(db,h!!.name)','''val extracted=Combat132.events(db,h!!.name)
            out.addView(label("COMBOS DE REFERÊNCIA • NÃO SÃO SIMULAÇÕES COMPLETAS",12,cyan,true))
            for(c in db.combos[h.name].orEmpty()){
                val skills=db.skills[h.name].orEmpty().map{it.name}.toSet()
                out.addView(label(c.type+": "+c.steps.joinToString(" → "){step->when(step){"@BASIC"->"Ataque básico";"@UNKNOWN"->"etapa desconhecida";else->step+if(step !in skills)" (forma/efeito não modelado)" else ""}},12,white))
            }''')
rep(p,'if (picks.isEmpty()) result.addView(label("Nenhuma escolha válida encontrada.", 13, muted))','''if (picks.isEmpty()) result.addView(label("Nenhuma escolha válida encontrada.", 13, muted))
            val db=GameData13.current
            if(db!=null){
                fun parse(s:String)=s.split(",",";").map{it.trim()}.filter{it.isNotBlank()}
                val suggestions=Draft14.bans(db,parse(allies.text.toString()),parse(favorites.text.toString()),parse(enemies.text.toString()),parse(bans.text.toString()),ranks.selectedItem.toString())
                result.addView(label("BANIMENTOS SUGERIDOS • CONTRA A COMPOSIÇÃO INFORMADA",12,cyan,true))
                if(suggestions.isEmpty())result.addView(label("Informe aliados ou personagens planejados para avaliar riscos.",12,muted))
                for(v in suggestions){result.addView(label(v.hero,15,gold,true));result.addView(label(v.reason,12,white))}
            }''')
asset=Path("android-app/app/src/main/assets/strategy13.tsv")
rows=asset.read_text(encoding="utf-8")
asset.write_text(rows+Path("updates/v0_14/combos.tsv").read_text(encoding="utf-8"),encoding="utf-8")
