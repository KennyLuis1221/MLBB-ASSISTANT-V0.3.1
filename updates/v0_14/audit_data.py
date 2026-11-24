#!/usr/bin/env python3
"""Audit the bundled snapshot. A successful structural check is not a release certificate."""
from pathlib import Path
import json, hashlib, collections, sys
p=Path("android-app/app/src/main/assets/strategy13.tsv")
raw=p.read_bytes()
rows=[v.split("\t") for v in raw.decode("utf-8").splitlines()]
by=collections.defaultdict(list)
for r in rows:by[r[0]].append(r)
heroes={r[1]:r for r in by["H"]}
items={r[1]:r for r in by["I"]}
assert len(heroes)==len(by["H"])==133
assert len(items)==len(by["I"])
assert all(r[1] in heroes for key in ("S","D","Q","B","C","Y") for r in by[key])
assert all(r[2] in heroes for key in ("C","Y") for r in by[key])
assert all(x in items for r in by["B"] for x in r[6].split("|"))
assert all(x in items for r in by["I"] for x in r[4].split("|") if x)
def visit(name,seen):
    if name in seen:raise ValueError("circular recipe "+name)
    for child in items[name][4].split("|"):
        if child:visit(child,seen|{name})
for n in items:visit(n,set())
stats={}
for n,r in heroes.items():
    v=dict(x.split("=",1) for x in r[8].split("|") if "=" in x)
    missing=[k for k in ("hp1","hp15","physical_atk1","physical_atk15","physical_def1","physical_def15","magic_def1","magic_def15") if not v.get(k)]
    if missing:stats[n]=missing
skills=collections.defaultdict(set)
for r in by["S"]:skills[r[1]].add(r[2])
unknown=[{"hero":r[1],"combo":r[2],"step":s} for r in by["Q"] for s in r[3].split("|") if s!="@BASIC" and s not in skills[r[1]]]
counter={}
synergy={}
for rank in ("all","epic","legend","mythic","honor","glory"):
    for key,out in (("C",counter),("Y",synergy)):
        observed={tuple(sorted((r[1],r[2]))) for r in by[key] if r[3]==rank and r[1]!=r[2]}
        out[rank]={"measured_pairs":len(observed),"unmeasured_pairs":len(heroes)*(len(heroes)-1)//2-len(observed)}
report={"date":by["V"][0][1],"sha256":hashlib.sha256(raw).hexdigest(),"rows":{k:len(v) for k,v in by.items()},
        "missing_attributes":stats,"unlinked_combo_steps":unknown,"counter_coverage":counter,"synergy_coverage":synergy,
        "release_ready":False,"reason":"Structural audit only; no full per-skill/per-effect oracle, no patch verification, no real independent capture corpus, no approved Android/device checks."}
Path("development-data-audit.json").write_text(json.dumps(report,ensure_ascii=False,indent=2))
print(json.dumps({k:v for k,v in report.items() if k!="unlinked_combo_steps"},ensure_ascii=False))
if "--release" in sys.argv:
    raise SystemExit("RELEASE BLOCKED: full acceptance evidence not present.")
