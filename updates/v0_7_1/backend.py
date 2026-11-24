"""Correções adicionais após harden_backend.py; execução reproduzível."""
from pathlib import Path
p=Path("server/app.py")
s=p.read_text()
def rep(a,b):
    global s
    assert a in s,a
    s=s.replace(a,b,1)
rep("from fastapi import Depends", "from fastapi.responses import JSONResponse\nfrom fastapi import Depends")
rep('redoc_url=None if os.getenv("MLBB_DEPLOYMENT")=="production" else "/redoc")','redoc_url=None if os.getenv("MLBB_DEPLOYMENT")=="production" else "/redoc", openapi_url=None if os.getenv("MLBB_DEPLOYMENT")=="production" else "/openapi.json")')
rep('    if cloud_sessions.authorize(authorization):','    if getattr(request.state,"authorized",False):\n        return\n    if cloud_sessions.authorize(authorization):')
rep('def recommend(state: MatchState) -> dict:', '''@app.middleware("http")
async def production_guard(request: Request, call_next):
    if os.getenv("MLBB_DEPLOYMENT")=="production" and request.method=="POST":
        path=request.url.path.rstrip("/")
        if path in {"/analyze","/recommend","/draft","/session"}:
            if path!="/session":
                try:
                    require_authorization(request,request.headers.get("authorization"))
                    request.state.authorized=True
                except HTTPException as exc:
                    return JSONResponse({"detail":exc.detail},status_code=exc.status_code)
            try:
                length=int(request.headers.get("content-length","-1"))
            except ValueError:
                length=-1
            limit=2_600_000 if path=="/analyze" else 32_768
            if not 0<=length<=limit:
                return JSONResponse({"detail":"Tamanho do corpo inválido"},status_code=413)
    return await call_next(request)

def recommend(state: MatchState) -> dict:''')
rep('VERIFIED_ICON_NAMES={x.casefold() for x in json.loads((ROOT/"items_reference.json").read_text())}', 'VERIFIED_ICON_NAMES={x.casefold() for x in json.loads((ROOT/"items_reference.json").read_text())}|{x.casefold() for x in DB["items"]}|{"blade of despair"}')
rep('    previous = get_session(session_id, now)\n    observations = {}','''    previous = get_session(session_id, now)
    if previous.get("visual_at") and now-previous["visual_at"]>90:
        update_session(session_id,{"hero":None,"enemies":[],"owned_items":[],"enemy_items":[],"visual_at":0})
        previous={}
    observations = {}''')
rep('                if observations:\n                    observations["visual_at"] = now','''                # A gameplay frame with a hero name must not renew old scoreboard evidence.
                if observations and (screen_type=="match_scoreboard" or not previous.get("visual_at")):
                    observations["visual_at"] = now''')
# Reset cached opposing-team inventory when the confirmed hero changes.
rep('    update_session(session_id, observations)\n    merged = {**previous, **observations}', '''    if observations.get("hero") and previous.get("hero") and observations["hero"]!=previous["hero"]:
        cleared={"enemies":[],"owned_items":[],"enemy_items":[],"visual_at":0}
        previous={**previous,**cleared}
        observations={**cleared,**observations,"visual_at":now}
    update_session(session_id, observations)
    merged = {**previous, **observations}''')
p.write_text(s)
