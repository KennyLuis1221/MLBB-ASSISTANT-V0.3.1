"""Motor multi-heroi V0.4. Regras genéricas e explicáveis; NÃO são meta/picks validados.
Catálogo de nomes/classes exportado do app. Sem dependência de chaves secretas.
"""
from __future__ import annotations
import json
from pathlib import Path

CATALOG_PATH = Path(__file__).with_name("heroes_catalog.json")
CATALOG = json.loads(CATALOG_PATH.read_text(encoding="utf-8")) if CATALOG_PATH.exists() else {}
NAMES = {name.casefold(): name for name in CATALOG}


def roles_for(hero: str) -> list[str]:
    name = NAMES.get(hero.strip().casefold())
    return CATALOG.get(name, []) if name else []


def recommend_multi(hero: str, owned: list[str], armor: bool, healing: bool, magic: bool) -> dict:
    name = NAMES.get(hero.strip().casefold())
    if not name:
        return {"hero": hero, "recommendations": [],
                "note": "Herói não confirmado no catálogo; nenhuma build inventada"}
    classes = set(CATALOG[name])
    mage = "Mago" in classes and "Atirador" not in classes and "Lutador" not in classes
    tank = "Tanque" in classes
    support = "Suporte" in classes
    fighter = "Lutador" in classes
    mm = "Atirador" in classes
    assassin = "Assassino" in classes
    # Atributos/efeitos e custos de patch NAO verificados. Somente candidatos, não ordem final de build.
    candidates = {}
    def add(item, points, why):
        best = candidates.get(item)
        if best is None or best[0] < points:
            candidates[item] = (points, why)
    if mage:
        add("Holy Crystal", 3, "candidato inicial da classe mago; validar escalamento do herói")
        if healing:
            add("Glowing Wand", 12, "investigar redução de cura em build mágica")
    elif tank or support:
        add("Immortality", 3, "candidato genérico de sobrevivência para linha de frente")
        if healing:
            add("Dominance Ice", 12, "considerar controle de regeneração conforme efeitos atuais")
        if magic:
            add("Athena's Shield", 13, "considerar proteção contra dano mágico explosivo")
    else:
        if mm or assassin:
            add("Blade of Despair", 2, "opção genérica de dano físico; validar sinergia")
        if fighter:
            add("War Axe", 3, "opção de lutador; validar sinergia e estilo do herói")
        if armor:
            add("Malefic Roar", 13, "defesa física adversária elevada; considerar penetração física")
        if healing:
            add("Sea Halberd", 12, "adversários com muita cura; considerar resposta anti-cura")
        if magic:
            add("Rose Gold Meteor", 14, "ameaça de dano mágico explosivo; verificar alternativa defensiva")
    if magic and not tank and not support and not mage and fighter:
        add("Athena's Shield", 10, "alternativa de defesa mágica para lutador")
    if magic and mage:
        add("Winter Crown", 11, "considerar uma opção defensiva situacional para mago")
    if armor and tank:
        add("Blade Armor", 5, "avaliar resistência a dano físico, não penetração")
    if mage and armor:
        # Armadura física nao justifica penetração mágica.
        pass
    owned_lower = {item.casefold().strip() for item in owned}
    ranked = [
        {"item": item, "score": score, "reason": reason,
         "source": "heurística multi-herói por classe; sem validação de patch"}
        for item, (score, reason) in candidates.items()
        if item.casefold() not in owned_lower
    ]
    ranked.sort(key=lambda r: (-r["score"], r["item"]))
    return {"hero": name, "recommendations": ranked[:3],
            "note": "Prévia por classe. Score é peso heurístico, não confiança. Verificar compatibilidade."}


LANE_ROLES = {
    "Selva": {"Assassino", "Lutador"}, "EXP": {"Lutador", "Tanque"},
    "Meio": {"Mago"}, "Ouro": {"Atirador"}, "Roam": {"Suporte", "Tanque"},
}


def suggest_draft(lane: str, allies: list[str], enemies: list[str],
                  bans: list[str] | None = None, favorites: list[str] | None = None) -> list[dict]:
    excluded = {s.casefold().strip() for s in allies + enemies + (bans or [])}
    favs = {s.casefold().strip() for s in (favorites or [])}
    roles_needed = LANE_ROLES.get(lane, set().union(*LANE_ROLES.values()))
    ally_classes = {r for h in allies for r in roles_for(h)}
    threats = sum(1 for h in enemies if "Assassino" in roles_for(h))
    scored = []
    for hero, hero_roles in CATALOG.items():
        role_set = set(hero_roles)
        if hero.casefold() in excluded or not (role_set & roles_needed):
            continue
        points = 8 + (12 if hero.casefold() in favs else 0)
        if "Tanque" in role_set and "Tanque" not in ally_classes:
            points += 3
        if "Mago" in role_set and "Mago" not in ally_classes:
            points += 1
        if "Suporte" in role_set and "Suporte" not in ally_classes:
            points += 2
        if threats >= 2 and role_set & {"Tanque", "Lutador"}:
            points += 2
        why = ["combina com a função " + lane]
        if hero.casefold() in favs:
            why.append("está nos seus favoritos")
        if "Tanque" in role_set and "Tanque" not in ally_classes:
            why.append("ajuda a linha de frente")
        scored.append({"hero": hero, "roles": sorted(hero_roles), "score": points,
                       "reason": "; ".join(why),
                       "source": "sugestão por classe e composição, sem counters validados"})
    scored.sort(key=lambda row: (-row["score"], row["hero"]))
    return scored[:3]
