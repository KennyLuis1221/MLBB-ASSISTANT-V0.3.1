from pathlib import Path
import json
p=Path("android-app/app/src/main/assets/strategy13.tsv")
rows=[s.split("\t") for s in p.read_text(encoding="utf-8").splitlines()]
found=[]
for r in rows:
    if r[0]=="S" and r[1]=="Aldous" and r[2]=="Contract: Explosion":
        assert r[3]=="9.5"
        r[3]="8"
        found.append("Aldous cooldown")
    if r[0]=="D" and r[1]=="Atlas" and "20 (+0.5% Extra Max HP)" in r[3]:
        r[3]=r[3].replace("20 (+0.5% Extra Max HP)","20 (+0.7% Extra Max HP)")
        found.append("Atlas defense")
assert len(found)==2,found
p.write_text("\n".join("\t".join(r) for r in rows)+"\n",encoding="utf-8")
meta={"target_patch":"2.2.23","patch_date":"2026-11-11","snapshot_certified":False,"source":"https://mlbbhub.com/patch-notes/original/2.2.23","corrections":found+["Sprint cooldown 75s"],"conflicts":{"Vale.magic_def15":[15,50],"Zetian.physical_atk15":["unknown",165]},"notes":"Community sources disagree; never fill missing values silently. All other changed heroes and effects still require full audit."}
Path("android-app/app/src/main/assets/patch-review.json").write_text(json.dumps(meta,indent=2),encoding="utf-8")
b=Path("android-app/app/src/main/java/com/mlbb/assistant/Loadout14.kt")
s=b.read_text();assert 'Spell("Sprint",100.0' in s;b.write_text(s.replace('Spell("Sprint",100.0','Spell("Sprint",75.0'))
