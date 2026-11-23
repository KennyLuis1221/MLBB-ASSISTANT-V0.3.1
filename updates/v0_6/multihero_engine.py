"""Motor adaptativo V0.6 baseado em evidências visíveis.
Cobertura de nomes/classes para 133 heróis não equivale a builds individualmente validadas.
Sem heurística inferir itens ocultos ou números de confiança.
"""
from __future__ import annotations
import json
from pathlib import Path

CATALOG = json.loads(Path(__file__).with_name("heroes_catalog.json").read_text(encoding="utf-8"))
NAMES = {name.casefold(): name for name in CATALOG}

def roles_for(hero: str) -> list[str]:
    return CATALOG.get(NAMES.get(hero.casefold().strip(), ""), [])

ARMOR = {"antique cuirass", "blade armor", "brute force breastplate",
         "dominance ice", "immortality", "queen's wings", "thunder belt"}
MAGIC_DEFENSE = {"athena's shield", "radiant armor", "oracle"}
HEALING_HEROES = {"estes", "floryn", "angela", "uranus", "ruby",
                  "rafaela", "alucard", "yu zhong", "esmeralda"}
MAGIC_BURST = {"eudora", "aurora", "kadita", "gusion", "harley", "vale", "kagura"}
PHYSICAL_ASSASSINS = {"saber", "hayabusa", "fanny", "ling", "lancelot"}

def recommend_multi(hero: str, owned: list[str], armor: bool, healing: bool, magic: bool,
                    *, enemy_items: list[str] | None = None, enemies: list[str] | None = None) -> dict:
    canonical = NAMES.get((hero or "").strip().casefold())
    if not canonical:
        return {"hero": hero, "recommendations": [], "note": "Herói não identificado; sem recomendação específica"}
    classes = set(CATALOG[canonical])
    mage = "Mago" in classes and "Atirador" not in classes and "Lutador" not in classes
    tank = "Tanque" in classes
    support = "Suporte" in classes and "Mago" not in classes
    physical = not (mage or tank or support)
    observed_items = [s.casefold().strip() for s in (enemy_items or [])]
    seen_heroes = [s.casefold().strip() for s in (enemies or [])]
    armor_count = sum(s in ARMOR for s in observed_items)
    magic_resist_count = sum(s in MAGIC_DEFENSE for s in observed_items)
    heal_count = sum(s in HEALING_HEROES for s in seen_heroes)
    burst_count = sum(s in MAGIC_BURST for s in seen_heroes)
    assassin_count = sum(s in PHYSICAL_ASSASSINS for s in seen_heroes)
    ownership = {i.casefold().strip() for i in owned}
    ranked: dict[str, tuple[int, str]] = {}

    def add(item: str, value: int, reason: str):
        if item.casefold() in ownership:
            return
        if item not in ranked or ranked[item][0] < value:
            ranked[item] = (value, reason)

    if (heal_count or healing) and not any(a in ownership for a in (
        "sea halberd", "dominance ice", "glowing wand")):
        item = "Glowing Wand" if mage else "Dominance Ice" if tank or support else "Sea Halberd"
        why = (f"{heal_count} herói(s) visto(s) com potencial de cura" if heal_count
               else "contexto de cura informado manualmente")
        add(item, 12 + heal_count, "Candidato anti-cura: " + why)

    if (armor_count >= 2 or armor) and physical:
        add("Malefic Roar", 16 + armor_count, "Considerar penetração: " +
            (f"{armor_count} itens de armadura vistos no time adversário" if armor_count
             else "defesa física informada manualmente"))

    if magic_resist_count >= 2 and mage:
        add("Divine Glaive", 17 + magic_resist_count,
            f"Considerar penetração mágica: {magic_resist_count} itens de resistência mágica visíveis")

    if burst_count or magic:
        if mage:
            add("Winter Crown", 10 + burst_count, "Considerar proteção situacional contra burst mágico")
        elif tank or "Lutador" in classes:
            add("Athena's Shield", 10 + burst_count, "Considerar resistência a burst mágico")
        else:
            add("Rose Gold Meteor", 10 + burst_count, "Considerar defesa contra dano mágico")

    if assassin_count >= 2 and (tank or support):
        add("Antique Cuirass", 11 + assassin_count,
            f"Composição inimiga com {assassin_count} ameaças de assassinos físicos")

    generic = ("Holy Crystal" if mage else
               "Immortality" if tank or support else
               "Windtalker" if "Atirador" in classes else
               "War Axe" if "Lutador" in classes else
               "Blade of Despair" if "Assassino" in classes else "Immortality")
    add(generic, 1, "Sugestão geral por classe, sem contexto confirmado.")
    candidates = sorted(ranked.items(), key=lambda kv: (-kv[1][0], kv[0]))
    return {
        "hero": canonical,
        "recommendations": [
            {"item": item, "score": score, "reason": why,
             "source": "heurística baseada no contexto visível; verificar sinergia e patch"}
            for item, (score, why) in candidates[:3]
        ],
        "evidence": {"enemy_armor_items": armor_count,
                     "enemy_magic_defense_items": magic_resist_count,
                     "enemy_healers": heal_count, "enemy_magic_burst": burst_count},
        "note": "Recomendação adaptativa experimental. Scores não são probabilidades."
    }

LANE_ROLES = {
    "Selva": {"Assassino", "Lutador"}, "EXP": {"Lutador", "Tanque"},
    "Meio": {"Mago"}, "Ouro": {"Atirador"}, "Roam": {"Suporte", "Tanque"}
}

def suggest_draft(lane: str, allies: list[str], enemies: list[str],
                  bans: list[str] | None = None, favorites: list[str] | None = None) -> list[dict]:
    excluded = {x.casefold().strip() for x in allies + enemies + (bans or [])}
    favorites_set = {x.casefold().strip() for x in (favorites or [])}
    required = LANE_ROLES.get(lane, set().union(*LANE_ROLES.values()))
    allied_roles = {r for h in allies for r in roles_for(h)}
    assassins = sum("Assassino" in roles_for(h) for h in enemies)
    result = []
    for hero, classes in CATALOG.items():
        roles = set(classes)
        if hero.casefold() in excluded or not roles.intersection(required):
            continue
        points = 8 + (12 if hero.casefold() in favorites_set else 0)
        if "Tanque" in roles and "Tanque" not in allied_roles: points += 3
        if "Mago" in roles and "Mago" not in allied_roles: points += 1
        if "Suporte" in roles and "Suporte" not in allied_roles: points += 2
        if assassins >= 2 and roles.intersection({"Lutador", "Tanque"}): points += 2
        reasons = ["função adequada para " + lane]
        if hero.casefold() in favorites_set: reasons.append("é um dos seus favoritos")
        if "Tanque" in roles and "Tanque" not in allied_roles: reasons.append("oferece linha de frente")
        result.append({"hero": hero, "roles": sorted(roles), "score": points,
                       "reason": "; ".join(reasons),
                       "source": "critério por classe/composição, counters não validados"})
    return sorted(result, key=lambda v: (-v["score"], v["hero"]))[:3]
