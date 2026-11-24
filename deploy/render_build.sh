#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test -f MLBB_ASSISTANT_CODIGO_GITHUB.zip
unzip -q MLBB_ASSISTANT_CODIGO_GITHUB.zip
test -f android-app/app/build.gradle.kts

cp updates/v0_4/MainActivity.kt android-app/app/src/main/java/com/mlbb/assistant/MainActivity.kt
cp updates/v0_4/HeroCatalog.kt android-app/app/src/main/java/com/mlbb/assistant/HeroCatalog.kt
cp updates/v0_4/multihero_engine.py server/multihero_engine.py
cp updates/v0_4/test_multihero.py server/test_multihero.py
python3 updates/v0_4/export_heroes.py
python3 updates/v0_4/patch_server.py
python3 -m py_compile server/app.py server/multihero_engine.py server/vision_bridge.py
PYTHONPATH=server python3 - <<'PY'
from multihero_engine import CATALOG, recommend_multi, suggest_draft
assert len(CATALOG) >= 120, len(CATALOG)
assert recommend_multi("Layla", [], True, False, False)["recommendations"][0]["item"] == "Malefic Roar"
assert recommend_multi("Kagura", [], False, False, False)["recommendations"]
assert len(suggest_draft("Ouro", [], [], ["Beatrix", "Brody"])) == 3
print("Validado:", len(CATALOG), "herois")
PY
python3 - <<'PY'
from pathlib import Path
p = Path("android-app/app/build.gradle.kts")
s = p.read_text(encoding="utf-8").replace('versionCode = 4', 'versionCode = 5').replace('versionName = "0.3.1"', 'versionName = "0.4.0"')
p.write_text(s, encoding="utf-8")
PY

cp updates/v0_5/MainActivity.kt android-app/app/src/main/java/com/mlbb/assistant/MainActivity.kt
cp updates/v0_5/OfflineTextAnalyzer.kt android-app/app/src/main/java/com/mlbb/assistant/OfflineTextAnalyzer.kt
python3 - <<'PY'
from pathlib import Path
service = Path("android-app/app/src/main/java/com/mlbb/assistant/ProjectionService.kt")
s = service.read_text(encoding="utf-8")
original = "    private var projection: MediaProjection? = null"
replacement = original + "\n    private val offlineAnalyzer by lazy { OfflineTextAnalyzer() }"
if original not in s or "OfflineTextAnalyzer" in s:
    raise SystemExit("Fonte original inesperado para ProjectionService")
s = s.replace(original, replacement)
previous = "val result = UploadClient.analyze(url, token, jpeg, sessionId, heroHint, armor, healing, magic)"
replacement = (
    "val result = if (url.isBlank()) {\n"
    "                        offlineAnalyzer.analyze(jpeg, heroHint)\n"
    "                    } else {\n"
    "                        try {\n"
    "                            UploadClient.analyze(url, token, jpeg, sessionId, heroHint, armor, healing, magic)\n"
    "                        } catch (network: Exception) {\n"
    "                            offlineAnalyzer.analyze(jpeg, heroHint).copy(\n"
    "                                visionStatus = \"Servidor indisponível; OCR local experimental\"\n"
    "                            )\n"
    "                        }\n"
    "                    }"
)
if previous not in s:
    raise SystemExit("Chamada de rede antiga não encontrada")
s = s.replace(previous, replacement)
s = s.replace("        executor.shutdownNow()", "        executor.shutdownNow()\n        if (this::offlineAnalyzer.isInitialized) offlineAnalyzer.close()") if False else s
service.write_text(s, encoding="utf-8")
app = Path("android-app/app/build.gradle.kts")
gradle = app.read_text(encoding="utf-8")
if 'com.google.mlkit:text-recognition' not in gradle:
    gradle += '\n\ndependencies {\n    implementation("com.google.mlkit:text-recognition:16.0.1")\n}\n'
gradle = gradle.replace("versionCode = 5", "versionCode = 6").replace('versionName = "0.4.0"', 'versionName = "0.5.0"')
app.write_text(gradle, encoding="utf-8")
print("V0.5 aplicado: captura local sem servidor, ML Kit OCR incluído")
PY

mkdir -p android-app/app/src/main/assets
cp updates/v0_6/icon_sketches.tsv android-app/app/src/main/assets/icon_sketches.tsv
cp updates/v0_6/VideoIconMatcher.kt android-app/app/src/main/java/com/mlbb/assistant/
cp updates/v0_6/LiveBuildEngine.kt android-app/app/src/main/java/com/mlbb/assistant/
cp updates/v0_6/OfflineTextAnalyzer.kt android-app/app/src/main/java/com/mlbb/assistant/
cp updates/v0_6/multihero_engine.py server/multihero_engine.py
cp updates/v0_6/test_v06.py server/test_v06.py
cp updates/v0_6/Dockerfile server/Dockerfile
cp updates/v0_6/render.yaml render.yaml
python3 updates/v0_6/patch_backend.py
python3 - <<'PY'
from pathlib import Path
service=Path("android-app/app/src/main/java/com/mlbb/assistant/ProjectionService.kt")
x=service.read_text(encoding="utf-8")
assert "OfflineTextAnalyzer()" in x, "Construtor da V0.5 mudou"
x=x.replace("OfflineTextAnalyzer()", "OfflineTextAnalyzer(this)")
x=x.replace("private var intervalMs = 25_000L", "private var intervalMs = 12_000L")
x=x.replace('prefs.getInt("interval", 25)', 'prefs.getInt("interval", 12)')
x=x.replace('.coerceIn(15, 60)', '.coerceIn(10, 60)')
service.write_text(x,encoding="utf-8")
main=Path("android-app/app/src/main/java/com/mlbb/assistant/MainActivity.kt")
x=main.read_text(encoding="utf-8")
x=x.replace("Versão 0.5 de testes.", "Versão 0.6 de testes.")
x=x.replace("Análise local disponível • IA online não conectada",
  "Análise local automática • servidor online opcional")
x=x.replace("Intervalo entre capturas (15 a 60 segundos)",
  "Intervalo entre capturas (10 a 60 segundos)")
x=x.replace('prefs.getInt("interval", 25)', 'prefs.getInt("interval", 12)')
x=x.replace('intValue !in 15..60', 'intValue !in 10..60')
x=x.replace('Use um intervalo de 15 a 60 segundos',
  'Use um intervalo de 10 a 60 segundos')
main.write_text(x,encoding="utf-8")
overlay=Path("android-app/app/src/main/java/com/mlbb/assistant/OverlayPanel.kt")
x=overlay.read_text(encoding="utf-8").replace("VISÃO ONLINE • TESTE",
  "VISÃO LOCAL • EXPERIMENTAL")
overlay.write_text(x,encoding="utf-8")
gradle=Path("android-app/app/build.gradle.kts")
x=gradle.read_text(encoding="utf-8")
assert "versionCode = 6" in x and 'versionName = "0.5.0"' in x
x=x.replace("versionCode = 6","versionCode = 7")
x=x.replace('versionName = "0.5.0"','versionName = "0.6.0"')
gradle.write_text(x,encoding="utf-8")
PY
python3 -m py_compile server/app.py server/multihero_engine.py server/vision_bridge.py
PYTHONPATH=server python3 - <<'PY'
from multihero_engine import CATALOG,recommend_multi,suggest_draft
assert len(CATALOG) >= 120
x = recommend_multi("Layla", [], False, False, False,
    enemy_items=["Antique Cuirass","Blade Armor"])
assert x["recommendations"][0]["item"] == "Malefic Roar"
m = recommend_multi("Kagura", [], False, False, False,
    enemy_items=["Athena's Shield","Radiant Armor"])
assert m["recommendations"][0]["item"] == "Divine Glaive"
assert len(suggest_draft("Roam",[],[],[],[])) == 3
print("Motor de recomendacao V0.6 validado")
PY
python3 - <<'PY'
import base64
from pathlib import Path
lines=Path("android-app/app/src/main/assets/icon_sketches.tsv").read_text().splitlines()
assert len(lines) >= 190
for s in lines:
    p=s.split("|")
    assert len(p)==3 and p[0] in ("i","h") and len(base64.b64decode(p[2]))==27
print(len(lines),"esbocos de icones validados")
PY

cp updates/v0_6_1/MainActivity.kt android-app/app/src/main/java/com/mlbb/assistant/MainActivity.kt
cp updates/v0_6_1/OfflineTextAnalyzer.kt android-app/app/src/main/java/com/mlbb/assistant/OfflineTextAnalyzer.kt
python3 updates/v0_6_1/apply_fix.py
python3 - <<'PY'
from pathlib import Path
base = Path("android-app/app/src/main/java/com/mlbb/assistant")
main = (base / "MainActivity.kt").read_text()
overlay = (base / "OverlayPanel.kt").read_text()
local = (base / "OfflineTextAnalyzer.kt").read_text()
assert "Salvar meu herói de teste" in main
assert "SUGESTÕES INICIAIS POR FUNÇÃO" in main
assert "SUGESTÕES DE HERÓIS" in overlay
assert "draftSuggestions" in local
assert 'versionName = "0.6.1"' in Path("android-app/app/build.gradle.kts").read_text()
print("Validações de presença V0.6.1: OK")
PY

cp updates/v0_7/*.kt android-app/app/src/main/java/com/mlbb/assistant/
cp updates/v0_7/references_v7.txt android-app/app/src/main/assets/
cp updates/v0_7/cloud_sessions.py server/
python3 updates/v0_7/apply_v7.py
python3 updates/v0_7/patch_cloud.py
python3 -m py_compile server/*.py
python3 - <<'PY'
from pathlib import Path
import base64
rows=Path("android-app/app/src/main/assets/references_v7.txt").read_text().splitlines()
assert len(rows)==4
for a,b in [(0,1),(2,3)]:
    names=rows[a].split("|")
    assert len(names)==len(set(names))
    assert len(base64.b64decode(rows[b],validate=True))==20*len(names)
    print("Referências:",len(names))
PY
python3 deploy/harden_backend.py
python3 -m pip install -r server/requirements.txt
