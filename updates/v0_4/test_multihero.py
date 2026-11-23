"""Testes de regressão v0.4: sugestão por herói, classe e draft manual."""
import json
from pathlib import Path
from multihero_engine import CATALOG, recommend_multi, suggest_draft

def test_catalogo_amplo():
    assert len(CATALOG) >= 120
    assert "Hanzo" in CATALOG and "Layla" in CATALOG and "Tigreal" in CATALOG

def test_build_mago():
    items = recommend_multi("Kagura", [], False, False, False)["recommendations"]
    assert len(items) > 0 and all(row["source"] for row in items)

def test_build_atirador_defesa():
    ranked = recommend_multi("Layla", [], True, False, False)["recommendations"]
    assert ranked[0]["item"] == "Malefic Roar"

def test_anti_cura_sem_duplicacao():
    ranked = recommend_multi("Hanzo", ["Sea Halberd"], False, True, False)["recommendations"]
    assert all(x["item"] != "Sea Halberd" for x in ranked)

def test_tanque_anti_cura():
    ranked = recommend_multi("Tigreal", [], False, True, False)["recommendations"]
    assert ranked[0]["item"] == "Dominance Ice"

def test_draft_sem_banidos():
    picks = suggest_draft("Ouro", ["Tigreal"], ["Hanzo"], bans=["Beatrix", "Brody"])
    assert len(picks) == 3
    assert all(p["hero"] not in ("Beatrix", "Brody", "Hanzo") for p in picks)

def test_heroi_nao_reconhecido_nao_inventa():
    ranked = recommend_multi("Herói fictício", [], False, False, False)
    assert ranked["recommendations"] == []
