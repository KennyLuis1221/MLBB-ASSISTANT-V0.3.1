"""Production contract: no external AI calls; all credentials below are test-only."""
import uuid, time
import cv2
import numpy as np
import pytest
from fastapi.testclient import TestClient
import app as m
import cloud_sessions as cs
import vision_bridge as vb

@pytest.fixture(autouse=True)
def clean(monkeypatch):
    monkeypatch.setenv("MLBB_DEPLOYMENT","production")
    monkeypatch.delenv("MLBB_API_TOKEN",raising=False)
    monkeypatch.delenv("OPENAI_API_KEY",raising=False)
    monkeypatch.setenv("MLBB_PUBLIC_SESSION_ENABLED","0")
    monkeypatch.setenv("MLBB_VISION_ENABLED","0")
    cs.TOKENS.clear();cs.ISSUANCE.clear();cs.issued=0;cs.used=0
    m.SESSIONS.clear()
    yield
    m.SESSIONS.clear()

client=TestClient(m.app)
def image():
    return cv2.imencode(".png",np.zeros((50,80,3),dtype=np.uint8))[1].tobytes()
def analyze(s,auth):
    return client.post("/analyze",data={"session_id":s},headers={"Authorization":auth},
                       files={"image":("sample.png",image(),"image/png")})
def state(hero="Hanzo",screen="match_scoreboard",items=None):
    return vb.VisualState(screen_type=screen,self_hero=hero,enemies=[],owned_items=items or [],enemy_items=[],note="fixture")

@pytest.mark.parametrize("endpoint",["/recommend","/draft","/analyze"])
def test_production_rejects_anonymous(endpoint):
    assert client.post(endpoint,json={}).status_code==403

def test_wrong_token(monkeypatch):
    monkeypatch.setenv("MLBB_API_TOKEN","unit-test-admin")
    assert client.post("/recommend",json={"hero":"Hanzo"},headers={"Authorization":"Bearer wrong"}).status_code==401
    assert client.post("/recommend",json={"hero":"Hanzo"},headers={"Authorization":"Bearer unit-test-admin"}).status_code==200

def test_no_provider_no_session():
    assert client.get("/health").json()["vision"]=="disabled"
    assert client.post("/session").status_code==503

def test_oversize_before_decode(monkeypatch):
    monkeypatch.setenv("MLBB_API_TOKEN","unit-test-admin")
    r=client.post("/analyze",content=b"x"*2_600_001,headers={"Authorization":"Bearer unit-test-admin"})
    assert r.status_code==413

def test_session_isolation_same_uuid(monkeypatch):
    monkeypatch.setenv("MLBB_PUBLIC_SESSION_ENABLED","1")
    monkeypatch.setattr(vb,"online_ready",lambda:True)
    queue=[state(items=["Sea Halberd"]),state(hero=None,screen="match_gameplay")]
    async def fake(*a):return queue.pop(0)
    monkeypatch.setattr(vb,"read_visual_state",fake)
    a=cs.issue("test-a")["token"];b=cs.issue("test-b")["token"]
    sid=str(uuid.uuid4())
    one=analyze(sid,"Bearer "+a)
    two=analyze(sid,"Bearer "+b)
    assert one.status_code==200 and two.status_code==200
    assert one.json()["extracted"]["owned_items"]==["Sea Halberd"]
    assert two.json()["extracted"]["owned_items"]==[]

def test_quota_and_expiry(monkeypatch):
    monkeypatch.setenv("MLBB_PUBLIC_SESSION_ENABLED","1")
    monkeypatch.setenv("MLBB_MAX_REQUESTS_SESSION","1")
    token=cs.issue("test")["token"]
    assert cs.authorize("Bearer "+token)
    with pytest.raises(Exception) as e:cs.authorize("Bearer "+token)
    assert e.value.status_code==429
    cs.TOKENS[token][0]=time.time()-1
    assert not cs.authorize("Bearer "+token)

def test_old_scoreboard_not_refreshed(monkeypatch):
    monkeypatch.setenv("MLBB_API_TOKEN","unit-test-admin")
    monkeypatch.setattr(vb,"online_ready",lambda:True)
    queue=[state(items=["Sea Halberd"]),state(screen="match_gameplay"),state(screen="match_gameplay")]
    async def fake(*a):return queue.pop(0)
    monkeypatch.setattr(vb,"read_visual_state",fake)
    sid=str(uuid.uuid4());auth="Bearer unit-test-admin"
    assert analyze(sid,auth).json()["extracted"]["owned_items"]==["Sea Halberd"]
    entry=next(iter(m.SESSIONS.values()))
    entry["visual_at"]=time.monotonic()-60
    age=entry["visual_at"]
    assert analyze(sid,auth).status_code==200
    assert entry["visual_at"]==age
    entry["visual_at"]=time.monotonic()-91
    assert analyze(sid,auth).json()["extracted"]["owned_items"]==[]
