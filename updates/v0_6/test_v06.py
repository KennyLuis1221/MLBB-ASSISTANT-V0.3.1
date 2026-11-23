"""Testes V0.6: cenários observados mudam candidatos, sem ler itens ocultos."""
import os
from multihero_engine import recommend_multi, suggest_draft

def test_recomendacao_muda_com_armadura_observada():
    normal = recommend_multi("Layla", [], False, False, False)["recommendations"][0]["item"]
    changed = recommend_multi("Layla", [], False, False, False,
        enemy_items=["Antique Cuirass", "Blade Armor"])["recommendations"][0]["item"]
    assert normal != changed and changed == "Malefic Roar"

def test_mago_responde_resistencia_magica_e_nao_armadura():
    mage = recommend_multi("Kagura", [], False, False, False,
        enemy_items=["Athena's Shield", "Radiant Armor"])["recommendations"][0]["item"]
    armor = recommend_multi("Kagura", [], False, False, False,
        enemy_items=["Antique Cuirass", "Blade Armor"])["recommendations"][0]["item"]
    assert mage == "Divine Glaive" and armor != "Divine Glaive"

def test_sea_halberd_responde_cura_e_nao_duplicacao():
    r = recommend_multi("Hanzo", [], False, False, False, enemies=["Estes"])
    assert r["recommendations"][0]["item"] == "Sea Halberd"
    r2 = recommend_multi("Hanzo", ["Sea Halberd"], False, False, False, enemies=["Estes"])
    assert all(x["item"] != "Sea Halberd" for x in r2["recommendations"])

def test_sem_heroi_confirmado_sem_build_inventada():
    assert recommend_multi("Herói desconhecido", [], False, True, True)["recommendations"] == []

def test_draft_ignora_banidos():
    options = suggest_draft("Ouro", ["Tigreal"], ["Hanzo"], ["Lesley", "Layla"])
    assert len(options) == 3
    assert not set(x["hero"] for x in options).intersection({"Lesley", "Layla", "Hanzo"})

def test_api_recommend_usa_evidencia():
    from fastapi.testclient import TestClient
    from app import app
    client = TestClient(app)
    payload = {"hero": "Layla", "owned_items": [], "enemies": [],
               "enemy_items": ["Antique Cuirass", "Blade Armor"]}
    res = client.post("/recommend", json=payload)
    assert res.status_code == 200
    assert res.json()["recommendations"][0]["item"] == "Malefic Roar"
