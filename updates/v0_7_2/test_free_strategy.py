import json
import pytest
import gemini_free as g
import vision_bridge as v
from multihero_engine import recommend_multi,suggest_draft,CATALOG

def test_free_production_does_not_use_openai(monkeypatch):
    monkeypatch.setenv("MLBB_DEPLOYMENT","production")
    monkeypatch.setenv("MLBB_VISION_ENABLED","1")
    monkeypatch.setenv("OPENAI_API_KEY","test-only")
    monkeypatch.delenv("GEMINI_API_KEY",raising=False)
    assert not v.online_ready()

@pytest.mark.asyncio
async def test_gemini_contract(monkeypatch):
    monkeypatch.setenv("MLBB_VISION_PROVIDER","gemini")
    monkeypatch.setenv("MLBB_VISION_ENABLED","1")
    monkeypatch.setenv("MLBB_FREE_PROJECT_CONFIRMED","1")
    monkeypatch.setenv("GEMINI_API_KEY","unit-test")
    monkeypatch.setattr(g,"_calls",0);monkeypatch.setattr(g,"_block_until",0)
    class R:
        status_code=200
        def json(self):
            return {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":json.dumps({"screen_type":"post_match","self_hero":None,"enemies":[],"owned_items":[],"enemy_items":[],"note":"Resultado final"})}]}}]}
    class C:
        def __init__(self,**kw):pass
        async def __aenter__(self):return self
        async def __aexit__(self,*a):pass
        async def post(self,url,**kw):
            assert url.endswith("gemini-3.5-flash-lite:generateContent")
            assert kw["headers"]["x-goog-api-key"]=="unit-test"
            assert kw["json"]["contents"][0]["parts"][1]["inlineData"]["data"]
            return R()
    monkeypatch.setattr(g.httpx,"AsyncClient",C)
    assert (await v.read_visual_state(b"test")).screen_type=="post_match"

def test_quota(monkeypatch):
    monkeypatch.setenv("MLBB_FREE_CALLS_DAY","1")
    monkeypatch.setattr(g,"_calls",0);monkeypatch.setattr(g,"_block_until",0)
    g.reserve()
    with pytest.raises(RuntimeError):g.reserve()

def test_all_catalog_plans():
    for hero in CATALOG:
        r=recommend_multi(hero,[],False,False,False)
        assert len(r["build_plan"])==6 and len(set(r["build_plan"]))==6
        assert r["recommendations"]
    assert recommend_multi("Hanzo",[],False,False,False)["build_plan"]!=recommend_multi("Saber",[],False,False,False)["build_plan"]

def test_full_inventory_no_purchase():
    plan=recommend_multi("Hanzo",[],False,False,False)["build_plan"]
    assert not recommend_multi("Hanzo",plan,False,False,False)["recommendations"]

def test_magic_assassin_not_physical():
    r=recommend_multi("Karina",[],True,False,False,enemy_items=["Antique Cuirass","Blade Armor"])
    assert all(x["item"]!="Malefic Roar" for x in r["recommendations"])
    assert recommend_multi("Karina",[],False,False,False,enemy_items=["Athena's Shield","Radiant Armor"])["recommendations"][0]["item"]=="Divine Glaive"

def test_draft_excludes_and_explains():
    r=suggest_draft("Roam",["Tigreal"],["Atlas"],["Lolita"])
    assert not {"Tigreal","Atlas","Lolita"}.intersection(x["hero"] for x in r)
    assert any(x["hero"]=="Diggie" and "controle" in x["reason"] for x in r)
