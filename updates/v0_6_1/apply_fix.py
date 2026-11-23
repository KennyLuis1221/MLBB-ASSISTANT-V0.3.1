"""Patch incremental V0.6.1 — diagnósticos e recomendações visíveis.
Não atribui falsamente dados ao vivo quando não há detecção confirmada.
"""
from pathlib import Path

base = Path("android-app/app/src/main/java/com/mlbb/assistant")
overlay = base / "OverlayPanel.kt"
src = overlay.read_text(encoding="utf-8")
def replace_required(old, new):
    global src
    if old not in src:
        raise RuntimeError("OverlayPanel inesperado: " + old[:70])
    src = src.replace(old, new, 1)

replace_required(
    '    private val item = line("Ainda sem recomendação", 17, 0xFFEBCB82.toInt(), true)',
    '    private val recommendationTitle = line("PRÓXIMO ITEM", 11, 0xFFB5C8D8.toInt(), true)\n'
    '    private val item = line("Ainda sem recomendação", 17, 0xFFEBCB82.toInt(), true)'
)
replace_required(
    '        content.addView(line("PRÓXIMO ITEM", 11, 0xFFB5C8D8.toInt(), true))',
    '        content.addView(recommendationTitle)'
)
replace_required(
    '    fun showResult(result: AnalysisResult) {\n        status.text = result.status',
    '    fun showResult(result: AnalysisResult) {\n'
    '        recommendationTitle.text = if (result.status.contains("Seleção", true)) '
    '"SUGESTÕES DE HERÓIS" else "PRÓXIMO ITEM"\n'
    '        status.text = result.status'
)
overlay.write_text(src, encoding="utf-8")

service = base / "ProjectionService.kt"
src = service.read_text(encoding="utf-8")
old = '            showStatus("Aguardando MLBB em primeiro plano")'
new = '''            showStatus("Aguardando MLBB em primeiro plano")
            val chosenHero = HeroCatalog.find(heroHint)
            if (chosenHero != null) {
                val initial = LiveBuildEngine.recommend(chosenHero, emptyList(), emptyList())
                overlay?.showResult(AnalysisResult(
                    status = "Aguardando placar • dica geral por classe",
                    item = initial.item,
                    reason = initial.reason,
                    detections = 0,
                    source = "Recomendação genérica, NÃO baseada em partida observada",
                    hero = chosenHero, enemies = 0, items = 0,
                    visionStatus = "Dados inimigos não detectados. Abra o placar durante o MLBB."
                ))
            } else {
                showStatus("Herói não identificado. Para teste, selecione-o na tela inicial.")
            }'''
if old not in src:
    raise RuntimeError("Ponto de entrada ProjectionService não encontrado")
src = src.replace(old, new, 1)
if '        executor.shutdownNow()' not in src:
    raise RuntimeError("Shutdown não encontrado")
src = src.replace('        executor.shutdownNow()', '        executor.shutdownNow()\n        offlineAnalyzer.close()', 1)
service.write_text(src, encoding="utf-8")

build = Path("android-app/app/build.gradle.kts")
s = build.read_text(encoding="utf-8")
if 'versionCode = 7' not in s or 'versionName = "0.6.0"' not in s:
    raise RuntimeError("Versão base do Gradle inesperada")
s = s.replace('versionCode = 7', 'versionCode = 8')
s = s.replace('versionName = "0.6.0"', 'versionName = "0.6.1"')
build.write_text(s, encoding="utf-8")
print("Patch diagnóstico V0.6.1 aplicado: início com dica por classe, rótulo do draft, release ML Kit")
