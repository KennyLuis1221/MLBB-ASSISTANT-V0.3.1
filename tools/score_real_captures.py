#!/usr/bin/env python3
"""Evaluate independently annotated MLBB replay frames."""
import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import re

ABSTAIN = "<abstenção>"
EMPTY = "<vazio>"
MISATTRIBUTED = "<atribuição incorreta>"
GROUPS = (
    "herois", "itens", "itens_atribuidos", "slots", "ocr", "equipe",
    "identidade", "inventario_completo", "heroi_proprio", "placar",
)
SLOT_STATES = {
    "NOT_OBSERVED", "UNREADABLE", "EMPTY_CONFIRMED",
    "ITEM_CANDIDATE", "ITEM_CONFIRMED", "ITEM_UNKNOWN",
}
CONFIRMED_STATES = {"ITEM_CONFIRMED", "EMPTY_CONFIRMED"}
SPLITS = {"treino", "calibracao", "avaliacao"}


class Scores:
    def __init__(self):
        self.pairs = Counter()

    def add(self, expected, observed):
        truth = str(expected)
        prediction = ABSTAIN if observed is None else str(observed)
        self.pairs[truth, prediction] += 1

    def report(self):
        def ratio(n, d):
            return round(n / d, 4) if d else None

        classes = sorted(
            {a for a, _ in self.pairs} | {b for _, b in self.pairs} - {ABSTAIN, MISATTRIBUTED}
        )
        per_class = {}
        for label in classes:
            tp = self.pairs[label, label]
            fp = sum(n for (a, b), n in self.pairs.items() if a != label and b == label)
            fn = sum(n for (a, b), n in self.pairs.items() if a == label and b != label)
            per_class[label] = {
                "tp": tp, "fp": fp, "fn": fn, "amostras": tp + fn,
                "precisao": ratio(tp, tp + fp), "recall": ratio(tp, tp + fn),
                "f1": ratio(2 * tp, 2 * tp + fp + fn),
            }
        total = sum(self.pairs.values())
        covered = sum(n for (_, b), n in self.pairs.items() if b != ABSTAIN)
        tp = sum(n for (a, b), n in self.pairs.items() if a == b)
        fp = covered - tp
        fn = total - tp
        macro = [entry["f1"] for entry in per_class.values() if entry["amostras"]]

        return {
            "amostras": total, "tp": tp, "fp": fp, "fn": fn,
            "precisao": ratio(tp, tp + fp), "recall": ratio(tp, tp + fn),
            "f1": ratio(2 * tp, 2 * tp + fp + fn),
            "f1_macro": ratio(sum(macro), len(macro)),
            "cobertura": ratio(covered, total), "abstencao": ratio(total - covered, total),
            "classes": per_class,
            "confusao": [{"esperado": a, "observado": b, "n": n} for (a, b), n in sorted(self.pairs.items())],
        }


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def index_rows(rows):
    result = {}
    for row in rows:
        side, line = row["lado_indice"], row["linha_indice"]
        if side not in (0, 1) or line not in range(5) or (side, line) in result:
            raise ValueError("Equipas e linhas devem identificar uma posição única")
        result[side, line] = row
    return result


def slots_of(row):
    slots = {}
    for slot in row.get("slots", []):
        index = slot["slot"]
        state = slot.get("estado")
        if index not in range(1, 7) or index in slots or state not in SLOT_STATES:
            raise ValueError("Slots devem ter índice único de 1 a 6 e estado explícito")
        if state in {"ITEM_CANDIDATE", "ITEM_CONFIRMED"} and not slot.get("item"):
            raise ValueError("Estado de item exige rótulo")
        if state == "EMPTY_CONFIRMED" and slot.get("item") is not None:
            raise ValueError("Slot vazio não pode conter item")
        slots[index] = (state, slot.get("item"))
    return slots


def visible_item(slot):
    state, item = slot
    return item if state == "ITEM_CONFIRMED" else EMPTY if state == "EMPTY_CONFIRMED" else None


def frame_for(export, number):
    frames = export.get("quadros")
    if not isinstance(frames, list):
        raise ValueError("Use o JSON de replay que inclui quadros, estados e hashes")
    hits = [frame for frame in frames if frame.get("quadro") == number]
    if len(hits) != 1:
        raise ValueError(f"Quadro {number} ausente ou duplicado no replay")
    return hits[0]


def score(expected, actual, totals, identities):
    actual_board = bool(actual["placar_atual"])
    truth_board = bool(expected["placar"])
    totals["placar"].add(truth_board, actual_board)
    rows = index_rows(expected["jogadores"])
    predictions = index_rows(actual["jogadores"]) if actual_board else {}
    if not truth_board:
        for prediction in predictions.values():
            if prediction.get("heroi"):
                totals["herois"].add(EMPTY, prediction["heroi"])
            for slot in slots_of(prediction).values():
                label = visible_item(slot)
                if label not in (None, EMPTY):
                    totals["itens"].add(EMPTY, label)
    for coords, truth in rows.items():
        prediction = predictions.get(coords, {})
        truth_id = truth.get("jogador_id")
        observed_id = prediction.get("jogador_id")
        expected_hero = truth.get("heroi")
        observed_hero = prediction.get("heroi")
        expected_side = truth.get("equipe")
        observed_side = prediction.get("equipe")
        correct_hero = expected_hero is not None and expected_hero == observed_hero
        if expected_hero is not None:
            totals["herois"].add(expected_hero, observed_hero)
        if expected_side is not None:
            totals["equipe"].add(expected_side, observed_side)
        wrong_side = expected_side is not None and expected_side != observed_side
        if truth_id and observed_id and truth_id not in identities and correct_hero and not wrong_side:
            if observed_id not in identities.values():
                identities[truth_id] = observed_id
        proper_owner = correct_hero and not wrong_side and (
            truth_id is None or observed_id is not None and identities.get(truth_id) == observed_id
        )
        if truth_id is not None:
            totals["identidade"].add("correta", "correta" if proper_owner else "incorreta" if observed_id else None)
        truth_slots = slots_of(truth)
        observed_slots = slots_of(prediction)
        for slot_id, truth_slot in truth_slots.items():
            observed_slot = observed_slots.get(slot_id, ("NOT_OBSERVED", None))
            totals["slots"].add(truth_slot[0], observed_slot[0] if slot_id in observed_slots else None)
            truth_item = visible_item(truth_slot)
            observation = visible_item(observed_slot)
            if truth_item is None:
                continue
            totals["itens"].add(truth_item, observation)
            totals[f"slot_{slot_id}"].add(truth_item, observation)
            if expected_hero is not None:
                attributed = observation if proper_owner else MISATTRIBUTED if observation is not None else None
                totals["itens_atribuidos"].add(truth_item, attributed)
        if len(truth_slots) == 6 and all(visible_item(slot) is not None for slot in truth_slots.values()):
            complete = len(observed_slots) == 6 and all(visible_item(slot) is not None for slot in observed_slots.values())
            correct = proper_owner and all(
                visible_item(observed_slots[i]) == visible_item(truth_slots[i]) for i in range(1, 7)
            ) if complete else False
            totals["inventario_completo"].add("correto", "correto" if correct else "incorreto" if complete else None)
        for field in ("ouro", "kda", "nivel"):
            if truth.get(field) is not None:
                totals["ocr"].add(truth[field], prediction.get(field))
                totals[f"ocr_{field}"].add(truth[field], prediction.get(field))
    for field, group in (("tempo_partida", "ocr"), ("heroi_proprio", "heroi_proprio")):
        if expected.get(field) is not None:
            totals[group].add(expected[field], actual.get(field))
            if field == "tempo_partida":
                totals["ocr_tempo"].add(expected[field], actual.get(field))


def evaluate(pairs, directory):
    totals = defaultdict(Scores, {label: Scores() for label in GROUPS})
    seen = set()
    content_splits = {}
    splits = {}
    eligible = []
    for sample in pairs:
        match = sample["partida"]
        split = sample["conjunto"]
        frame = sample["quadro"]
        if not isinstance(match, str) or not match.strip() or split not in SPLITS or not isinstance(frame, int) or frame < 1:
            raise ValueError("Cada caso exige partida, quadro positivo e conjunto válido")
        if splits.setdefault(match, split) != split:
            raise ValueError(f"Partida presente em mais de um conjunto: {match}")
        if (match, frame) in seen:
            raise ValueError(f"Quadro repetido no manifesto: {match}/{frame}")
        seen.add((match, frame))
        actual = frame_for(load(directory / sample["observado"]), frame)
        digest = actual.get("sha256")
        if not isinstance(digest, str) or not re.fullmatch(r"[0-9a-f]{64}", digest):
            raise ValueError(f"Quadro {match}/{frame}: SHA-256 ausente ou inválido")
        if content_splits.setdefault(digest, split) != split:
            raise ValueError("Mesmo conteúdo presente em mais de um conjunto")
        if split == "avaliacao":
            eligible.append((match, frame, digest, sample, actual))

    scored = set()
    origins = Counter()
    ids = defaultdict(dict)
    duplicates = 0
    for match, frame, digest, sample, observed in sorted(eligible, key=lambda case: case[:2]):
        if observed.get("duplicado") or digest in scored:
            duplicates += 1
            continue
        truth = load(directory / sample["esperado"])
        if truth.get("partida") != match or truth.get("quadro") != frame:
            raise ValueError(f"Anotação aponta para outro quadro: {match}/{frame}")
        origins[sample["origem"]] += 1
        score(truth, observed, totals, ids[match])
        scored.add(digest)
    return {
        "amostras": len(scored), "partidas": len(ids), "duplicatas_excluidas": duplicates, "origens": origins,
        "metricas": {group: metric.report() for group, metric in sorted(totals.items())},
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path)
    args = parser.parse_args()
    try:
        pairs = load(args.manifest)
        if not isinstance(pairs, list) or not pairs:
            raise ValueError("Manifesto sem capturas")
        report = evaluate(pairs, args.manifest.parent)
    except (OSError, ValueError, KeyError, TypeError) as error:
        parser.error(str(error))
    if report["amostras"] == 0:
        parser.error("Conjunto sem quadros independentes de avaliação")
    print(json.dumps(report, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
