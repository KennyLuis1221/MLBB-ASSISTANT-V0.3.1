"""Candidate plans, NOT verified meta or patch data. No fake winrates.
Items chosen as illustrative archetypes; users should verify current effects.
"""
PROFILES={
 "Hanzo":["Swift Boots","Demon Hunter Sword","Corrosion Scythe","Golden Staff","Malefic Roar","Immortality"],
 "Layla":["Swift Boots","Berserker's Fury","Windtalker","Haas' Claws","Malefic Roar","Wind of Nature"],
 "Kagura":["Arcane Boots","Genius Wand","Lightning Truncheon","Holy Crystal","Divine Glaive","Winter Crown"],
 "Tigreal":["Tough Boots","Dominance Ice","Antique Cuirass","Athena's Shield","Immortality","Radiant Armor"],
 "Karrie":["Swift Boots","Demon Hunter Sword","Golden Staff","Corrosion Scythe","Wind of Nature","Immortality"],
 "Claude":["Swift Boots","Demon Hunter Sword","Golden Staff","Corrosion Scythe","Wind of Nature","Malefic Roar"],
 "Eudora":["Arcane Boots","Genius Wand","Holy Crystal","Lightning Truncheon","Divine Glaive","Winter Crown"],
 "Natan":["Arcane Boots","Feather of Heaven","Genius Wand","Holy Crystal","Divine Glaive","Winter Crown"],
 "Karina":["Arcane Boots","Starlium Scythe","Genius Wand","Holy Crystal","Divine Glaive","Winter Crown"],
 "Alpha":["Tough Boots","War Axe","Hunter Strike","Queen's Wings","Oracle","Immortality"],
 "Estes":["Demon Shoes","Enchanted Talisman","Fleeting Time","Oracle","Athena's Shield","Immortality"],
 "Saber":["Tough Boots","Hunter Strike","Blade of Despair","Malefic Roar","Endless Battle","Immortality"]
}
BASES={
 "magic":["Arcane Boots","Genius Wand","Holy Crystal","Divine Glaive","Glowing Wand","Winter Crown"],
 "tank":["Tough Boots","Dominance Ice","Antique Cuirass","Athena's Shield","Radiant Armor","Immortality"],
 "marksman":["Swift Boots","Windtalker","Berserker's Fury","Malefic Roar","Wind of Nature","Immortality"],
 "fighter":["Tough Boots","War Axe","Hunter Strike","Queen's Wings","Oracle","Immortality"],
 "assassin":["Tough Boots","Hunter Strike","Blade of Despair","Malefic Roar","Endless Battle","Immortality"]
}
MAGIC={"Natan","Karina","Aamon","Gusion","Harley","Joy","Guinevere","Silvanna","Julian","Harith","Kimmy","Kagura","Selena","Kadita","Esmeralda","Alice"}
PHYSICAL={"Hanzo","Bane","Roger","Lesley"}
def draft_bonus(hero,allies,enemies):
    """Small explainable ability-interaction hypotheses, not verified matchup statistics."""
    foes={x.casefold().strip() for x in enemies}
    friends={x.casefold().strip() for x in allies}
    points=0; reasons=[]
    if hero=="Diggie" and foes & {"tigreal","atlas","minotaur"}:
        points+=5;reasons.append("avaliar proteção contra controle em área")
    if hero=="Lolita" and foes & {"chang'e","layla","granger"}:
        points+=3;reasons.append("avaliar proteção contra projéteis bloqueáveis")
    if hero=="Estes" and friends & {"tigreal","fredrinn","belerick"}:
        points+=3;reasons.append("possível sustentação com linha de frente")
    if hero=="Karrie" and foes & {"hylos","belerick","fredrinn"}:
        points+=4;reasons.append("avaliar dano contra alvos resistentes")
    if hero=="Valir" and foes & {"tigreal","atlas","hylos"}:
        points+=3;reasons.append("avaliar afastamento de iniciadores")
    return points,reasons
