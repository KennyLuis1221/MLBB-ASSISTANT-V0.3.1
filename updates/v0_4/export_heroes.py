"""Converte a lista de personagens Android em catálogo JSON para o servidor.
Garante que a mesma lista base exista em Android/backend.
"""
import json
import re
from pathlib import Path

source = Path("android-app/app/src/main/java/com/mlbb/assistant/HeroCatalog.kt")
text = source.read_text(encoding="utf-8")
groups = re.findall(r'^\s*"(Tanque|Lutador|Assassino|Mago|Atirador|Suporte)" to "([^"]+)"', text, re.M)
if len(groups) != 6:
    raise RuntimeError("Catálogo Kotlin não encontrado ou incompleto")
catalog = {}
for role, names in groups:
    for name in names.split(", "):
        catalog.setdefault(name.strip(), []).append(role)
if len(catalog) < 120:
    raise RuntimeError("Catálogo de heróis insuficiente: " + str(len(catalog)))
dst = Path("server/heroes_catalog.json")
dst.write_text(json.dumps(catalog, ensure_ascii=False, sort_keys=True, indent=2), encoding="utf-8")
print("Catálogo exportado:", len(catalog), "heróis")
