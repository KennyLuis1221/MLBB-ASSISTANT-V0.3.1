#!/usr/bin/env python3
"""Score exported MLBB readings against independently annotated, authorized frames."""
import argparse
from collections import Counter
import json
from pathlib import Path


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def compare(totals, truth, prediction):
    if truth is None:
        totals["tn" if prediction is None else "fp"] += 1
    elif truth == prediction:
        totals["tp"] += 1
    else:
        totals["fn"] += 1
        if prediction is not None:
            totals["fp"] += 1


def coords(row):
    return row["lado_indice"], row["linha_indice"]


def score(expected, actual, totals):
    predicted = {coords(row): row for row in actual["jogadores"]}
    rows = expected["jogadores"]
    if expected.get("sem_placar") is True:
        totals["herois"]["fp"] += len(predicted)
        totals["itens"]["fp"] += sum(len(row["slots"]) for row in predicted.values())
        totals["quadros_sem_placar"]["fp" if predicted else "tn"] += 1
    for row in rows:
        result = predicted.get(coords(row), {})
        if "heroi" in row:
            compare(totals["herois"], row["heroi"], result.get("heroi"))
        slots = {str(slot["slot"]): slot["item"] for slot in result.get("slots", [])}
        for position, item in row.get("slots", {}).items():
            compare(totals["itens"], item, slots.get(position))
        for field in ("ouro", "kda", "nivel"):
            if field in row:
                compare(totals["ocr"], row[field], result.get(field))
    if "tempo_partida" in expected:
        compare(totals["ocr"], expected["tempo_partida"], actual.get("tempo_partida"))


def metrics(counter):
    tp, fp, fn, tn = [counter[label] for label in ("tp", "fp", "fn", "tn")]
    def ratio(n, d):
        return round(n / d, 4) if d else None
    return dict(tp=tp, fp=fp, fn=fn, tn=tn, precision=ratio(tp, tp + fp),
                recall=ratio(tp, tp + fn), false_negative_rate=ratio(fn, tp + fn))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path, help="JSON array of pairs with origem, esperado and observado")
    args = parser.parse_args()
    pairs = load(args.manifest)
    if not isinstance(pairs, list) or not pairs:
        parser.error("manifesto sem capturas")
    groups = ("herois", "itens", "ocr", "quadros_sem_placar")
    totals = {label: Counter() for label in groups}
    origins = Counter()
    for pair in pairs:
        origins[pair["origem"]] += 1
        expected = load(args.manifest.parent / pair["esperado"])
        observed = load(args.manifest.parent / pair["observado"])
        score(expected, observed, totals)
    summary = {label: metrics(totals[label]) for label in groups}
    negatives = totals["quadros_sem_placar"]
    n = negatives["fp"] + negatives["tn"]
    summary["quadros_sem_placar"]["false_positive_rate"] = round(negatives["fp"] / n, 4) if n else None
    print(json.dumps({"amostras": len(pairs), "origens": origins, "metricas": summary}, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
