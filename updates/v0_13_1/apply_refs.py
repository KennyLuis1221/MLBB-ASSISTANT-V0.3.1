from pathlib import Path
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
def rep(n,a,z):
 p=b/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,z,1))
rep("MainActivity.kt",'prefs.edit().putString("lane_hint",lane.selectedItem.toString()).putString("draft_favorites",favorites.text.toString()).apply()','prefs.edit().putString("lane_hint",lane.selectedItem.toString()).putString("draft_favorites",favorites.text.toString()).putString("strategy_rank",ranks.selectedItem.toString()).apply()')
rep("MainActivity.kt",'val advice=Strategy13.recommend(StrategicEngine.State(hero,','prefs.edit().putString("build_lane",pos.selectedItem.toString()).putString("strategy_rank",ranks.selectedItem.toString()).apply()\n            val advice=Strategy13.recommend(StrategicEngine.State(hero,')
rep("OfflineTextAnalyzer.kt",'prefs.getString("draft_favorites","").orEmpty(),excluded).take(3)','prefs.getString("draft_favorites","").orEmpty(),excluded,prefs.getString("strategy_rank","all")?:"all").take(3)')
rep("OfflineTextAnalyzer.kt",'inventoryKnown=false))','inventoryKnown=false,lane=prefs.getString("build_lane",null)?.takeUnless{it=="Automática"},rank=prefs.getString("strategy_rank","all")?:"all"))')
# Canonicalize recognized item names consistently across modes.
rep("OfflineTextAnalyzer.kt",'StrategicEngine.canonicalItem(it.trim())','GameData13.current?.canonicalItem(it.trim())?:StrategicEngine.canonicalItem(it.trim())')
# Reject duplicate portraits in fallback layouts as well as custom layout.
rep("DetailedMatcher.kt",'if(best<3||selected.count{it.side==0&&it.hero!=null}<1||selected.count{it.side==1&&it.hero!=null}<1)','''val duplicates=selected.mapNotNull{it.hero?.name}.groupingBy{it}.eachCount().filterValues{it>1}.keys
        selected=selected.filter{it.hero?.name !in duplicates}
        best=selected.count{it.hero!=null}
        if(best<4||selected.count{it.side==0&&it.hero!=null}<2||selected.count{it.side==1&&it.hero!=null}<2)''')
# Set a visible diagnostic for data load failures rather than silent fallback.
