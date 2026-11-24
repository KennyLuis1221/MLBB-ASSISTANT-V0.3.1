from pathlib import Path
p=Path("android-app/app/src/main/java/com/mlbb/assistant/OverlayPanel.kt")
s=p.read_text()
a='        for(r in result.players.take(10)){'
z='''        fun asEvidence(v:ObservedPlayer)=Panel16.Row(v.hero,v.team,v.items,v.gold,v.kda,v.level)
        val evidence=result.players.take(10).map(::asEvidence)
        val groups=Panel16.groups(evidence,result.hero)
        val ownEvidence=groups.first().rows.singleOrNull()
        for(group in groups){
            if(group.rows.isEmpty()&&group.expected==null)continue
            val count=group.expected?.let{"/"+it}?: ""
            playerRows.addView(label(group.label.uppercase(java.util.Locale.ROOT)+" • "+group.rows.size+count+" reconhecidos",11,cyan,true))
            if(group.rows.isEmpty())playerRows.addView(label("Sem observação suficiente.",10,muted))
            for(er in group.rows){
            val r=result.players.singleOrNull{it.hero==er.hero&&it.team==er.side}?:continue'''
assert a in s;s=s.replace(a,z,1)
a='                    if(stats.isNotBlank())addView(label(stats,10,muted))'
z='''                    if(stats.isNotBlank())addView(label(stats,10,muted))
                    addView(label(Panel16.coverage(er),9,muted))
                    for(risk in Panel16.threats(GameData13.current,er,ownEvidence))addView(label(risk,10,gold))'''
assert a in s;s=s.replace(a,z,1)
a='        diagnostic.text=result.visionStatus'
assert a in s;s=s.replace(a,'        }\n'+a,1)
p.write_text(s)
