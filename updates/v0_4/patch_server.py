"""Atualiza o backend da V0.3 preservando captura, autenticação e memória de sessão."""
from pathlib import Path

server = Path("server")
p = server / "app.py"
src = p.read_text(encoding="utf-8")
start = src.find("def recommend(state: MatchState) -> dict:")
stop = src.find("\ndef safe_parse_list", start)
if start < 0 or stop < 0:
    raise RuntimeError("Não foi possível localizar o motor legado com segurança")
src = src[:start] + (
    "def recommend(state: MatchState) -> dict:\n"
    "    return recommend_multi(state.hero, state.owned_items, state.enemy_armor_stacking,\n"
    "                           state.enemy_healing_high, state.enemy_magic_burst)\n\n"
) + src[stop:]
needle = "import vision_bridge\n"
if needle not in src:
    raise RuntimeError("Importação original ausente")
src = src.replace(needle, needle + "from multihero_engine import recommend_multi, roles_for, suggest_draft\n", 1)
src = src.replace('version="0.3.0"', 'version="0.4.0"')
src = src.replace('"version": "0.3.0"', '"version": "0.4.0"')
src = src.replace(
    'hero and hero not in DB["heroes"]',
    'hero and not roles_for(hero)'
)
src = src.replace(
    '"data_patch": "demonstrativo — NÃO verificado"',
    '"data_patch": "cobertura por classe; não validado por herói ou patch"'
)
src += '''

class DraftRequest(BaseModel):
    lane: str = "Selva"
    allies: list[str] = Field(default_factory=list, max_length=5)
    enemies: list[str] = Field(default_factory=list, max_length=5)
    bans: list[str] = Field(default_factory=list, max_length=10)
    favorites: list[str] = Field(default_factory=list, max_length=10)


@app.post("/draft", dependencies=[Depends(require_authorization)])
def draft_endpoint(body: DraftRequest):
    return {
        "suggestions": suggest_draft(body.lane, body.allies, body.enemies,
                                     body.bans, body.favorites),
        "warning": "Escolhas heurísticas por função e composição; não são counters verificados."
    }
'''
p.write_text(src, encoding="utf-8")
print("Backend V0.4 aplicado")
