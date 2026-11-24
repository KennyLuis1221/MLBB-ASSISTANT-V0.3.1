#!/usr/bin/env python3
"""Compare packaged catalog identities with descriptor references."""
import argparse
import base64
import json
from pathlib import Path

ALIASES = {"Athena's Shield": "Valkyrie's Shield"}
LEGACY = {
    "Bloodlust Axe", "Calamity Reaper", "Necklace of Durance", "Scarlet Phantom",
    "Shadow Twinblades", "Magic Potion", "Power Potion", "Rock Potion",
}
SPECIAL_COST = {"Bud of Hope", "Flower of Hope", "Lantern of Hope", "Shimmering Lantern"}


def read_refs(path):
    lines = path.read_text(encoding="utf-8").splitlines()
    if len(lines) != 4:
        raise ValueError("Formato de descritores inválido")
    groups = []
    for index in (0, 2):
        labels = lines[index].split("|")
        if any(not label for label in labels) or len(labels) != len(set(labels)):
            raise ValueError("Rótulo visual repetido ou vazio")
        if len(base64.b64decode(lines[index + 1], validate=True)) != len(labels) * 20:
            raise ValueError("Quantidade ou bloco de descritores inválido")
        groups.append(set(labels))
    return groups


def audit(assets):
    hero_refs, item_refs = read_refs(assets / "references_v7.txt")
    rows = [line.split("\t") for line in (assets / "strategy13.tsv").read_text(encoding="utf-8").splitlines()]
    heroes = {r[1] for r in rows if r[0] == "H"}
    items = {r[1]: r for r in rows if r[0] == "I"}
    item_ids = {ALIASES.get(name, name) for name in item_refs}
    report = {
        "data": rows[0][1], "patch_revisado_parcialmente": "2.2.23",
        "herois_catalogo": len(heroes), "herois_referenciados": len(hero_refs),
        "itens_catalogo": len(items), "rotulos_item": len(item_refs),
        "alias": ALIASES, "herois_sem_referencia": sorted(heroes - hero_refs),
        "hero_ref_fora_catalogo": sorted(hero_refs - heroes), "item_ref_fora_catalogo": sorted(item_ids - set(items)),
        "itens": [],
    }
    components = {name for row in items.values() for name in row[4].split("|") if name}
    for name in sorted(items):
        status = "legado" if name in LEGACY else "especial_ou_legado_a_confirmar" if name in SPECIAL_COST else "loja_referencia"
        report["itens"].append({
            "nome": name, "grupo": status,
            "tipo": "componente" if name in components else "equipamento_final_ou_especial",
            "referencia_visual": name in item_ids,
        })
    report["itens_sem_referencia"] = [entry for entry in report["itens"] if not entry["referencia_visual"]]
    report["loja_sem_referencia"] = [entry["nome"] for entry in report["itens_sem_referencia"] if entry["grupo"] == "loja_referencia"]
    report["cobertura_todos_modos_certificada"] = False
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("assets", type=Path)
    args = parser.parse_args()
    report = audit(args.assets)
    print(json.dumps(report, indent=2, ensure_ascii=False))
    if any(report[label] for label in ("herois_sem_referencia", "hero_ref_fora_catalogo", "item_ref_fora_catalogo", "loja_sem_referencia")):
        parser.exit(1, "Há um herói normal ou item de loja sem descritor compatível\n")


if __name__ == "__main__":
    main()
