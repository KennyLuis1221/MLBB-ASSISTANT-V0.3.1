"""Build-time frozen snapshot, no network at app runtime. Import numeric facts only.
Source: achedon12/mlbb, wiki-derived CC BY-SA data, pinned revision.
Statistics source: arena.rone.dev via source repo. Missing rows stay unknown.
"""
from pathlib import Path
import json, urllib.request, re, collections, math
root=Path("android-app/app/src/main/assets")
p=root/"strategy13.tsv"
base=Path("updates/v0_13/catalog.tsv").read_text(encoding="utf-8")
rows=[r.split("\t") for r in base.splitlines()]
slugs={r[2]:r[1] for r in rows if r[0]=="H"}
items={r[1] for r in rows if r[0]=="I"}
lookup={n.casefold():n for n in items}
lookup["athena's shield"]="Valkyrie's Shield"
lookup["magic boots"]="Magic Shoes"
lookup["demon boots"]="Demon Shoes"
lookup["haas's claws"]="Haas' Claws"
pin="d0d96e8ecdd62553bfee83a7f75f8dbac4ad86c0"
url="https://raw.githubusercontent.com/achedon12/mlbb/"+pin+"/src/data/game/statistics.json"
with urllib.request.urlopen(url,timeout=60) as r:
    raw=r.read(8_000_001)
assert len(raw)<8_000_001
d=json.loads(raw)
def norm(s):
    s=re.sub(r"^(Ice|Flame|Behemoth) Hunter's\s+","",str(s),flags=re.I)
    s=re.sub(r"\s*-\s*(Favor|Encourage|Conceal|Dire Hit)\s*$","",s,flags=re.I)
    return lookup.get(s.strip().casefold())
def add(*r):
    rows.append([str(v).replace("\t"," ").replace("\n"," ").replace("\r"," ") for v in r])
def finite(v):
    return isinstance(v,(float,int)) and math.isfinite(v) and abs(v)<=100
counts=collections.Counter()
# Explicit rank buckets; never merge data of different ranks.
for slug,v in d["counters"].items():
    if slug not in slugs:continue
    if "strong" in v:v={"all":v}
    for rank,x in v.items():
        if rank not in ("all","epic","legend","mythic","honor","glory") or not isinstance(x,dict):continue
        for e in x.get("strong",[])+x.get("weak",[]):
            target=e.get("slug");gap=e.get("advantage")
            if target in slugs and target!=slug and finite(gap):
                add("C",slugs[slug],slugs[target],rank,gap);counts["counter_rows"]+=1
for slug,v in d.get("teammates",{}).items():
    if slug not in slugs or not isinstance(v,dict):continue
    for rank,arr in v.items():
        if rank not in ("all","epic","legend","mythic","honor","glory") or not isinstance(arr,list):continue
        for e in arr:
            target=e.get("slug");gap=e.get("advantage")
            if target in slugs and target!=slug and finite(gap):
                add("Y",slugs[slug],slugs[target],rank,gap);counts["synergy_rows"]+=1
for slug,lanes in d["builds"].items():
    if slug not in slugs:continue
    for lane,byrank in lanes.items():
        for rank,arr in byrank.items():
            if rank not in ("all","epic","legend","mythic","honor","glory") or not isinstance(arr,list):continue
            for b in arr[:3]:
                names=list(dict.fromkeys(n for i in b.get("items",[]) if (n:=norm(i))))
                if names:
                    add("B",slugs[slug],lane,rank,"played",b.get("pickRate") or 0,"|".join(names),b.get("emblem") or "","|".join(map(str,b.get("talents") or [])),b.get("spell") or "",b.get("winRate") if finite(b.get("winRate")) else "");counts["played_builds"]+=1
for slug,lanes in d.get("guides",{}).items():
    if slug not in slugs or not isinstance(lanes,dict):continue
    for lane,byrank in lanes.items():
        if not isinstance(byrank,dict):continue
        for rank,b in byrank.items():
            if rank not in ("all","epic","legend","mythic","honor","glory") or not isinstance(b,dict):continue
            names=list(dict.fromkeys(n for i in b.get("items",[]) if (n:=norm(i))))
            if names:
                add("B",slugs[slug],lane,rank,"guide",0,"|".join(names),b.get("emblem") or "","|".join(map(str,b.get("talents") or [])),b.get("spell") or "","");counts["guide_builds"]+=1
assert len(slugs)==133 and len(items)>=100
assert counts["counter_rows"]>0 and counts["played_builds"]>0,counts
p.write_text("\n".join("\t".join(r) for r in rows)+"\n"+Path("updates/v0_13_1/effects.tsv").read_text(encoding="utf-8"),encoding="utf-8")
print("SOURCE DATA",len(slugs),len(items),dict(counts),"skills",sum(r[0]=="S" for r in rows))
print("HERO BUILD COVERAGE",len({r[1] for r in rows if r[0]=="B"}))
