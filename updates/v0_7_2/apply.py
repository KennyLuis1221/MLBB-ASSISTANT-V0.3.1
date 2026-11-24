"""V0.7.2: free-only production adapter and shared candidate profiles."""
from pathlib import Path
import json
p=Path("server/vision_bridge.py")
s=p.read_text()
s=s.replace("import httpx","import httpx\nimport gemini_free",1)
s=s.replace('    return os.getenv("MLBB_VISION_ENABLED", "0") == "1" and bool(os.getenv("OPENAI_API_KEY", ""))','''    if os.getenv("MLBB_DEPLOYMENT")=="production" or os.getenv("MLBB_VISION_PROVIDER")=="gemini":
        return gemini_free.ready()
    return os.getenv("MLBB_VISION_ENABLED", "0") == "1" and bool(os.getenv("OPENAI_API_KEY", ""))''')
s=s.replace('    model = os.getenv("MLBB_VISION_MODEL", "gpt-4.1-mini")','''    if os.getenv("MLBB_DEPLOYMENT")=="production" or os.getenv("MLBB_VISION_PROVIDER")=="gemini":
        return await gemini_free.read(image,content_type,PROMPT,OUTPUT_SCHEMA,VisualState)
    model = os.getenv("MLBB_VISION_MODEL", "gpt-4.1-mini")''')
p.write_text(s)
p=Path("server/app.py");s=p.read_text()
s=s.replace('version="0.7.1"','version="0.7.2"').replace('"version": "0.7.1"','"version": "0.7.2"')
s=s.replace('"vision_verified": False,','"vision_verified": False, "provider": "gemini-free-project" if os.getenv("MLBB_DEPLOYMENT")=="production" else "development",')
s=s.replace('"recommendation": best, "recommendations": ranked["recommendations"],','"recommendation": best, "recommendations": ranked["recommendations"], "build_plan":ranked.get("build_plan",[]),')
s=s.replace('"data_patch": "cobertura por classe; não validado por herói ou patch"','"data_patch": "perfis experimentais e fallback por classe; patch não validado"')
p.write_text(s)
p=Path("server/multihero_engine.py");s=p.read_text()
s=s.replace("from pathlib import Path","from pathlib import Path\nfrom strategy_profiles import PROFILES, BASES, MAGIC, PHYSICAL, draft_bonus")
s=s.replace('    mage = "Mago" in classes and "Atirador" not in classes and "Lutador" not in classes','    mage = canonical in MAGIC or (canonical not in PHYSICAL and "Mago" in classes and "Atirador" not in classes and "Lutador" not in classes)')
s=s.replace('    add(generic, 1, "Sugestão geral por classe, sem contexto confirmado.")','''    profile = PROFILES.get(canonical)
    base = profile or BASES.get("magic" if mage else "tank" if tank or support else "marksman" if "Atirador" in classes else "fighter" if "Lutador" in classes else "assassin")
    for idx,item in enumerate(base):
        add(item, max(1,8-idx), ("Perfil experimental de "+canonical if profile else "Plano geral por função")+"; ordem e patch não validados.")
    if len(ownership)>=6:
        ranked.clear()
    plan=list(dict.fromkeys([i for i in owned if i.strip()]+[item for item,_ in sorted(ranked.items(),key=lambda kv:(-kv[1][0],kv[0]))]))[:6]''')
s=s.replace('"hero": canonical,\n        "recommendations":','"hero": canonical,\n        "build_plan": plan, "profile_scope": "individual_experimental" if profile else "class_fallback",\n        "recommendations":')
s=s.replace('        reasons = ["função adequada para " + lane]','        bonus, extra = draft_bonus(hero, allies, enemies)\n        points += bonus\n        reasons = ["função adequada para " + lane] + extra')
s=s.replace('"source": "critério por classe/composição, counters não validados"','"source": "critérios por função, composição e interações hipotéticas; sem winrates ou counters validados"')
p.write_text(s)
# Same item sequence in Kotlin, plus exclusions/favorites and honest disclosure.
b=Path("android-app/app/src/main/java/com/mlbb/assistant")
from sys import path
path.insert(0,"server")
from strategy_profiles import PROFILES,BASES,MAGIC,PHYSICAL
def ktmap(name,d):
    return "    private val "+name+" = mapOf(\n"+",\n".join("        "+json.dumps(k,ensure_ascii=False)+" to listOf("+", ".join(json.dumps(x,ensure_ascii=False) for x in v)+")" for k,v in d.items())+"\n    )\n"
def ktset(name,values):
    return "    private val "+name+" = setOf("+", ".join(json.dumps(x) for x in sorted(values))+")\n"
p=b/"LiveBuildEngine.kt";s=p.read_text()
s=s.replace("object LiveBuildEngine {","object LiveBuildEngine {\n"+ktmap("profiles",PROFILES)+ktmap("bases",BASES)+ktset("magicHeroes",MAGIC)+ktset("physicalHeroes",PHYSICAL),1)
s=s.replace('val isMage = "Mago" in roles && "Atirador" !in roles && "Lutador" !in roles','val isMage = canonical in magicHeroes || (canonical !in physicalHeroes && "Mago" in roles && "Atirador" !in roles && "Lutador" !in roles)')
s=s.replace('        offer(generic, 1, "Opção genérica da classe; validar sinergia e patch antes de comprar.")','''        val profile=profiles[canonical]
        val base=profile ?: bases[when { isMage->"magic"; isTank||isSupport->"tank"; "Atirador" in roles->"marksman"; "Lutador" in roles->"fighter"; else->"assassin" }].orEmpty()
        base.forEachIndexed { i, item -> offer(item, maxOf(1,8-i), (if(profile!=null) "Perfil experimental de "+canonical else "Plano geral por classe")+"; patch não validado.") }
        if(owned.size>=6) candidates.clear()''')
s=s.replace('val ranked = candidates.sortedWith','val ranked = candidates.distinctBy { it.item }.sortedWith')
p.write_text(s)
p=b/"HeroCatalog.kt";s=p.read_text()
s=s.replace('favoritesInput: String): List<Pick>','favoritesInput: String, bansInput: String = ""): List<Pick>')
s=s.replace('val excluded = (allies + enemies).toSet()','val excluded = (allies + enemies + parseHeroes(bansInput)).toSet()')
s=s.replace('        if (armor && magicDamage)\n            picks.add(ItemTip("Divine Glaive", "Considerar penetração mágica contra defesa mágica; armadura física por si só não justifica este item."))','')
p.write_text(s)
p=b/"MainActivity.kt";s=p.read_text()
s=s.replace('val favorites = input("Seus heróis favoritos (opcional)", "")','val favorites = input("Seus heróis favoritos (opcional)", prefs.getString("draft_favorites","").orEmpty())\n        val bans = input("Banidos (separe por vírgulas)", "")')
s=s.replace('listOf(allies, enemies, favorites)','listOf(allies, enemies, favorites, bans)')
s=s.replace('putString("lane_hint",lane.selectedItem.toString()).apply()','putString("lane_hint",lane.selectedItem.toString()).putString("draft_favorites",favorites.text.toString()).apply()')
s=s.replace('enemies.text.toString(), favorites.text.toString())','enemies.text.toString(), favorites.text.toString(), bans.text.toString())')
s=s.replace('Permito enviar quadros do MLBB ao servidor configurado e ao provedor de visão. Opcional.','Permito enviar quadros ao Render e Gemini. O plano gratuito pode usar dados para melhorar produtos. Não envie dados pessoais. Opcional.')
p.write_text(s)
p=b/"OfflineTextAnalyzer.kt";s=p.read_text()
s=s.replace('HeroCatalog.suggest(lane,"",excluded,"")','HeroCatalog.suggest(lane,"","",prefs.getString("draft_favorites","").orEmpty(),excluded)')
p.write_text(s)
p=b/"ProjectionService.kt";s=p.read_text()
s=s.replace('if(url.isBlank()) 4_000L else 12_000L','if(url.isBlank()) 4_000L else 60_000L')
p.write_text(s)
p=Path("android-app/app/build.gradle.kts");s=p.read_text().replace('versionCode = 10','versionCode = 11').replace('versionName = "0.7.1"','versionName = "0.7.2"');p.write_text(s)
