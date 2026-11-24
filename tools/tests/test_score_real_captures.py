import json
import sys
from collections import defaultdict
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from score_real_captures import Scores, evaluate, score


def row(hero="Layla", player="1", state="ITEM_CONFIRMED", item="Swift Boots", line=0):
    return {
        "lado_indice": 0, "linha_indice": line, "heroi": hero, "jogador_id": player,
        "equipe": "Aliado", "slots": [{"slot": 1, "estado": state, "item": item}],
    }


def evaluate_row(truth, observation):
    totals = defaultdict(Scores)
    score({"placar": True, "jogadores": [truth]},
          {"placar_atual": True, "jogadores": [observation]}, totals, {})
    return {field: value.report() for field, value in totals.items()}


def test_correct_item_wrong_hero_is_not_an_attribution_hit():
    result = evaluate_row(row(), row(hero="Nana"))
    assert result["itens"]["tp"] == 1
    assert result["itens_atribuidos"]["tp"] == 0
    assert result["itens_atribuidos"]["fp"] == result["itens_atribuidos"]["fn"] == 1


def test_unreadable_is_not_a_confirmed_empty_slot():
    result = evaluate_row(row(state="EMPTY_CONFIRMED", item=None), row(state="UNREADABLE", item=None))
    assert result["itens"]["tp"] == 0
    assert result["itens"]["fn"] == 1
    assert result["itens"]["abstencao"] == 1


def test_shared_identity_is_checked_across_reordered_rows():
    scores = defaultdict(Scores)
    ids = {}
    score({"placar": True, "jogadores": [row(player="source")]},
          {"placar_atual": True, "jogadores": [row(player="tracked")]}, scores, ids)
    score({"placar": True, "jogadores": [row(player="source", line=1)]},
          {"placar_atual": True, "jogadores": [row(player="tracked", line=1)]}, scores, ids)
    score({"placar": True, "jogadores": [row(player="source", line=2)]},
          {"placar_atual": True, "jogadores": [row(player="different", line=2)]}, scores, ids)
    assert scores["itens_atribuidos"].report()["tp"] == 2


def inputs(tmp, match, split, number, digest):
    observation = tmp / f"{match}-{number}-obs.json"
    truth = tmp / f"{match}-{number}-truth.json"
    observation.write_text(json.dumps({"quadros": [
        {"quadro": number, "sha256": digest, "placar_atual": True, "jogadores": [row()]}
    ]}))
    truth.write_text(json.dumps({"partida": match, "quadro": number, "placar": True, "jogadores": [row()]}))
    return {"origem": "regressão", "partida": match, "conjunto": split, "quadro": number,
            "esperado": truth.name, "observado": observation.name}


def test_partition_is_checked_for_session_and_identical_frames(tmp_path):
    first = inputs(tmp_path, "m1", "calibracao", 1, "a" * 64)
    second = inputs(tmp_path, "m1", "avaliacao", 2, "b" * 64)
    with pytest.raises(ValueError, match="mais de um conjunto"):
        evaluate([first, second], tmp_path)
    third = inputs(tmp_path, "m2", "avaliacao", 2, "a" * 64)
    with pytest.raises(ValueError, match="Mesmo conteúdo"):
        evaluate([first, third], tmp_path)


def test_duplicate_content_does_not_raise_sample_size(tmp_path):
    first = inputs(tmp_path, "m1", "avaliacao", 1, "a" * 64)
    second = inputs(tmp_path, "m1", "avaliacao", 2, "a" * 64)
    third = inputs(tmp_path, "m1", "avaliacao", 3, "b" * 64)
    result = evaluate([third, second, first], tmp_path)
    assert result["amostras"] == 2
    assert result["duplicatas_excluidas"] == 1
    assert result["metricas"]["itens_atribuidos"]["f1"] == 1
