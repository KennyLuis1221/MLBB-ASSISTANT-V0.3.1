"""Aplica integração da V0.6 ao backend sem recriar a API existente."""
from pathlib import Path

p = Path("server/app.py")
s = p.read_text(encoding="utf-8")
old = """    return recommend_multi(state.hero, state.owned_items, state.enemy_armor_stacking,
                           state.enemy_healing_high, state.enemy_magic_burst)"""
new = """    return recommend_multi(state.hero, state.owned_items, state.enemy_armor_stacking,
                           state.enemy_healing_high, state.enemy_magic_burst,
                           enemy_items=state.enemy_items, enemies=state.enemies)"""
if old not in s:
    raise RuntimeError("Contrato recommend() legado mudou; abortando patch")
s = s.replace(old, new, 1)
old_fields = """    owned_items: list[str] = Field(default_factory=list)


def require_authorization"""
new_fields = """    owned_items: list[str] = Field(default_factory=list)
    enemy_items: list[str] = Field(default_factory=list)


def require_authorization"""
if old_fields not in s:
    raise RuntimeError("Contrato MatchState mudou; abortando patch")
s = s.replace(old_fields, new_fields, 1)
old_rank = "    ranked = recommend(state)\n"
new_rank = """    ranked = recommend_multi(state.hero, equipped, state.enemy_armor_stacking,
                             state.enemy_healing_high, state.enemy_magic_burst,
                             enemy_items=enemy_equipped, enemies=enemy_heroes)
"""
if old_rank not in s:
    raise RuntimeError("Não localizou recomendação dentro de /analyze")
s = s.replace(old_rank, new_rank, 1)
s = s.replace('version="0.4.0"', 'version="0.6.0"')
s = s.replace('"version": "0.4.0"', '"version": "0.6.0"')
p.write_text(s, encoding="utf-8")
print("V0.6: backend ligado ao contexto inimigo")
