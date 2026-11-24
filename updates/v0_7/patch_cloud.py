"""Integrate structured online observations and optional bounded session enrollment."""
from pathlib import Path
import json
p=Path("server/vision_bridge.py")
s=p.read_text()
s=s.replace('class VisualState(BaseModel):','''class VisiblePlayer(BaseModel):
    model_config = ConfigDict(extra="forbid")
    hero: str | None = None
    team: Literal["ally", "enemy", "unknown"] = "unknown"
    items: list[str] = Field(default_factory=list,max_length=6)
    gold: int | None = Field(default=None,ge=0,le=100000)
    kda: str | None = None
    level: int | None = Field(default=None,ge=1,le=15)

class VisualState(BaseModel):''')
s=s.replace('"match_scoreboard", "match_gameplay", "shop", "post_match", "other"','"match_scoreboard", "match_gameplay", "shop", "draft", "post_match", "other"')
s=s.replace('    note: str = Field(max_length=180)','''    note: str = Field(max_length=180)
    players: list[VisiblePlayer] = Field(default_factory=list,max_length=10)
    allies: list[str] = Field(default_factory=list,max_length=5)
    bans: list[str] = Field(default_factory=list,max_length=10)
    match_time: str | None = None''')
old='        "note": {"type": "string"},'
new='''        "note": {"type": "string"},
        "players": {"type": "array", "items": {"type":"object","additionalProperties":False,
            "properties":{"hero":{"type":["string","null"]},"team":{"type":"string","enum":["ally","enemy","unknown"]},
            "items":{"type":"array","items":{"type":"string"}},"gold":{"type":["integer","null"]},
            "kda":{"type":["string","null"]},"level":{"type":["integer","null"]}},
            "required":["hero","team","items","gold","kda","level"]}},
        "allies":{"type":"array","items":{"type":"string"}},
        "bans":{"type":"array","items":{"type":"string"}},
        "match_time":{"type":["string","null"]},'''
assert old in s
s=s.replace(old,new)
s=s.replace('"enemy_items", "note"],','"enemy_items", "note", "players", "allies", "bans", "match_time"],')
s=s.replace('note: frase curta','''draft: seleção ou banimento. allies/enemies/bans somente escolhas ou banimentos explicitamente visíveis; não transcreva o catálogo de opções. Se time incerto, deixe vazio.
players: somente jogadores do placar DURANTE partida; um objeto por linha; associe o herói, time e itens apenas se visíveis na mesma linha. gold, kda e level devem ser null quando ilegíveis. Não leia nomes de conta. Time unknown se não distinguível.
match_time: tempo visível em mm:ss, null se indisponível.
note: frase curta''')
s=s.replace('"max_output_tokens": 850','"max_output_tokens": 2200')
p.write_text(s)
p=Path("server/app.py")
s=p.read_text()
s=s.replace('import vision_bridge','import vision_bridge\nimport cloud_sessions')
s=s.replace('version="0.6.0"','version="0.7.0"').replace('"version": "0.6.0"','"version": "0.7.0"')
s=s.replace('    expected = os.environ.get("MLBB_API_TOKEN", "")','''    if cloud_sessions.authorize(authorization):
        return
    expected = os.environ.get("MLBB_API_TOKEN", "")''')
s=s.replace('@app.post("/recommend", dependencies=[Depends(require_authorization)])','''@app.post("/session")
def issue_session(request: Request):
    if not vision_bridge.online_ready():
        raise HTTPException(503,"Visão não configurada")
    return cloud_sessions.issue(request.client.host if request.client else "unknown")

@app.post("/recommend", dependencies=[Depends(require_authorization)])''')
s=s.replace('    hero_hint: str = Form(default=""),','    hero_hint: str = Form(default=""),\n    lane_hint: str = Form(default="Selva"),')
s=s.replace('    screen_type = "unknown"','    screen_type = "unknown"\n    visible_players = []\n    match_time = None')
anchor='            # Telas de resultado final NÃO alimentam análise durante partida.'
assert anchor in s
s=s.replace(anchor,'''            match_time=visual.match_time
            if screen_type=="draft":
                update_session(session_id,{"hero":None,"enemies":[],"owned_items":[],"enemy_items":[],"visual_at":0})
                suggestions=suggest_draft(lane_hint,visual.allies,visual.enemies,visual.bans,[])
                return {"screen_type":"draft","vision_status":vision_status,"detections":[],
                    "draft_picks":suggestions,"recommendation":None,
                    "message":"Seleção observada; sugestões por classe ainda não validadas",
                    "extracted":{"hero":None,"enemies":visual.enemies,"owned_items":[],"enemy_items":[]}}
            if screen_type=="match_scoreboard":
                for player in visual.players:
                    if not player.hero or not roles_for(player.hero):continue
                    row=player.model_dump()
                    row["items"]=[v for v in player.items if v.casefold() in VERIFIED_ICON_NAMES]
                    visible_players.append(row)
            # Telas de resultado final NÃO alimentam análise durante partida.''')
s=s.replace('    "screen_type": screen_type, "vision_status": vision_status, "detections": detections,\n        "extracted": {"hero": hero or None','    "screen_type": screen_type, "vision_status": vision_status, "detections": detections,\n        "players":visible_players,"match_time":match_time,\n        "extracted": {"hero": hero or None')
# Validate names against known visual-reference catalog; these are not verified patch effects.
ref=Path("android-app/app/src/main/assets/references_v7.txt").read_text().splitlines()
Path("server/items_reference.json").write_text(json.dumps(ref[2].split("|"),ensure_ascii=False))
s=s.replace('TEMPLATES = ROOT / "templates"','TEMPLATES = ROOT / "templates"\nVERIFIED_ICON_NAMES={x.casefold() for x in json.loads((ROOT/"items_reference.json").read_text())}')
p.write_text(s)
# Keep automatic enrollment off by default.
r=Path("render.yaml")
s=r.read_text()
s += '      - key: MLBB_PUBLIC_SESSION_ENABLED\n        value: "0"\n      - key: MLBB_MAX_REQUESTS_DAY\n        value: "200"\n'
r.write_text(s)
