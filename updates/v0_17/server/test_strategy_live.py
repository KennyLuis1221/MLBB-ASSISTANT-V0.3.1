import pytest
from fastapi.testclient import TestClient

import strategy_live
from app import app

client = TestClient(app)
PAYLOAD = {
    "hero": "Layla",
    "players": [
        {"hero": "Layla", "team": "ally", "items": ["Swift Boots"], "level": 8},
        {"hero": "Tigreal", "team": "enemy", "items": ["Blade Armor"], "gold": 4900},
    ],
    "candidates": ["Malefic Roar"],
    "minute": 7,
}


def test_rejects_unknown_hero_and_fields():
    assert client.post("/strategy", json={**PAYLOAD, "hero": "Inventado"}).status_code == 422
    assert client.post("/strategy", json={**PAYLOAD, "image": "private"}).status_code == 422


def test_requires_token_before_body_processing(monkeypatch):
    monkeypatch.setenv("MLBB_DEPLOYMENT", "production")
    monkeypatch.setenv("MLBB_API_TOKEN", "test-token")
    assert client.post("/strategy", json=PAYLOAD).status_code == 401


def test_reports_provider_unavailable(monkeypatch):
    monkeypatch.setattr(strategy_live.gemini_free, "ready", lambda: False)
    assert client.post("/strategy", json=PAYLOAD).status_code == 503


@pytest.mark.parametrize("item, cited, status", [
    ("Malefic Roar", ["Tigreal"], 200),
    ("Blade Armor", ["Tigreal"], 503),
    ("Malefic Roar", ["Beatrix"], 503),
])
def test_checks_provider_contract(monkeypatch, item, cited, status):
    async def generate(parts, *_args, **_kwargs):
        assert len(parts) == 2 and all(set(part) == {"text"} for part in parts)
        assert "Tigreal" in parts[1]["text"]
        return strategy_live.Supplement(
            item=item, advice="Adapte a penetração com a armadura observada.", evidence_heroes=cited,
        )
    monkeypatch.setattr(strategy_live.gemini_free, "ready", lambda: True)
    monkeypatch.setattr(strategy_live.gemini_free, "generate", generate)
    assert client.post("/strategy", json=PAYLOAD).status_code == status
