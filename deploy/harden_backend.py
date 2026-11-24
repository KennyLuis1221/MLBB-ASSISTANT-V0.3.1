"""V0.7.1: fail-closed production authentication, health and observation hygiene."""
from pathlib import Path
p=Path("server/app.py")
s=p.read_text(encoding="utf-8")
def req(old,new):
    global s
    if old not in s: raise RuntimeError("Patch anchor missing: "+old[:80])
    s=s.replace(old,new,1)
req("import hmac","import hmac\nimport hashlib\nimport re")
req('app = FastAPI(title="MLBB Assistant", version="0.7.0")','app = FastAPI(title="MLBB Assistant", version="0.7.1", docs_url=None if os.getenv("MLBB_DEPLOYMENT")=="production" else "/docs", redoc_url=None if os.getenv("MLBB_DEPLOYMENT")=="production" else "/redoc")')
req('elif not request.client or request.client.host not in {"127.0.0.1", "::1", "testclient"}:',
    'elif os.getenv("MLBB_DEPLOYMENT")=="production" or not request.client or request.client.host not in {"127.0.0.1", "::1", "testclient"}:')
req('"version": "0.7.0", "vision": "online" if vision_bridge.online_ready() else "templates",',
    '"version": "0.7.1", "vision": "online" if vision_bridge.online_ready() else "disabled",\n            "vision_verified": False, "sessions_enabled": cloud_sessions.enabled() and vision_bridge.online_ready(),')
# Scope a caller's session identifier to its credential to prevent cross-session reads.
req("    image: UploadFile = File(...),","    request: Request,\n    image: UploadFile = File(...),")
req("    now = time.monotonic()\n    previous = get_session(session_id, now)",
    """    now = time.monotonic()
    if session_id:
        try:
            UUID(session_id)
            from uuid import uuid5, NAMESPACE_URL
            scope=hashlib.sha256(request.headers.get("authorization","local-test").encode()).hexdigest()
            session_id=str(uuid5(NAMESPACE_URL,scope+":"+session_id))
        except (ValueError, AttributeError):
            session_id=""
    previous = get_session(session_id, now)""")
# A new draft must not inherit cached observations from another match.
req('                update_session(session_id,{"hero":None,"enemies":[],"owned_items":[],"enemy_items":[],"visual_at":0})',
    '                update_session(session_id,{"hero":None,"enemies":[],"owned_items":[],"enemy_items":[],"visual_at":0})')
# Validate provider-reported labels and clear unknowns rather than inventing evidence.
req('                if visual.self_hero:\n                    observations["hero"] = visual.self_hero.strip().lower()[:60]',
    '                if visual.self_hero and roles_for(visual.self_hero):\n                    observations["hero"] = visual.self_hero.strip().lower()[:60]')
req('observations["enemies"] = normal_names(visual.enemies, 5)',
    'observations["enemies"] = normal_names([h for h in visual.enemies if roles_for(h)], 5)')
req('observations["enemy_items"] = normal_names(visual.enemy_items, 30)',
    'observations["enemy_items"] = [i for i in visual.enemy_items if i.casefold() in VERIFIED_ICON_NAMES][:30]')
s=s.replace('normal_names(visual.owned_items, 6)','normal_names([i for i in visual.owned_items if i.casefold() in VERIFIED_ICON_NAMES], 6)')
# Discard previous match's observations after 90s; do not refresh their timestamp with unrelated data.
req('    if visual_age is not None and visual_age > SESSION_TTL:', '    if visual_age is not None and visual_age > 90:')
p.write_text(s,encoding="utf-8")
