"""Provide an optional Gemini interpretation anchored to the game's recognized state."""
from __future__ import annotations

from pathlib import Path
from typing import Annotated, Literal

from fastapi import HTTPException
from pydantic import BaseModel, ConfigDict, Field, StringConstraints, model_validator

import gemini_free

Name = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=70)]
catalog = Path(__file__).with_name("live_catalog.tsv")
HEROES: set[str] = set()
ITEMS: set[str] = {"Athena's Shield"}
for line in catalog.read_text(encoding="utf-8").splitlines():
    if line.startswith(("H\t", "I\t")):
        category, name, *_ = line.split("\t", 2)
        (HEROES if category == "H" else ITEMS).add(name)


class Player(BaseModel):
    model_config = ConfigDict(extra="forbid")
    hero: Name
    team: Literal["ally", "enemy"]
    items: list[Name] = Field(max_length=6)
    gold: int | None = Field(default=None, ge=0, le=100_000)
    level: int | None = Field(default=None, ge=1, le=15)


class Evidence(BaseModel):
    model_config = ConfigDict(extra="forbid")
    hero: Name
    players: list[Player] = Field(min_length=2, max_length=10)
    candidates: list[Name] = Field(min_length=1, max_length=8)
    minute: int | None = Field(default=None, ge=0, le=99)

    @model_validator(mode="after")
    def observed_match(self):
        heroes = {p.hero for p in self.players}
        if not heroes <= HEROES or len(heroes) != len(self.players):
            raise ValueError("Heróis fora do catálogo ou repetidos")
        for player in self.players:
            if not set(player.items) <= ITEMS:
                raise ValueError("Item observado fora do catálogo")
        if not set(self.candidates) <= ITEMS or len(set(self.candidates)) != len(self.candidates):
            raise ValueError("Candidatos inválidos")
        if not any(p.hero == self.hero and p.team == "ally" for p in self.players):
            raise ValueError("Herói próprio sem observação no lado aliado")
        if not 1 <= sum(p.team == "enemy" for p in self.players) <= 5:
            raise ValueError("Lado inimigo sem evidência ou acima do máximo")
        if sum(p.team == "ally" for p in self.players) > 5:
            raise ValueError("Lado aliado acima do máximo")
        return self


class Supplement(BaseModel):
    model_config = ConfigDict(extra="forbid")
    item: Name | None
    advice: str | None = Field(min_length=12, max_length=400)
    evidence_heroes: list[Name] = Field(max_length=5)


PROMPT = """Você complementa a estratégia local de Mobile Legends: Bang Bang.
Use somente as observações do JSON fornecido. Listas de itens incompletas não provam slots vazios.
Ouro no placar é ouro total. Não presuma orçamento, resultado, objetivos, habilidades ativas ou dados ausentes.
Explique em português uma adaptação ou prioridade verificável a partir dos dados.
Se citar uma compra, escolha entre os candidatos locais e evite itens já observados no próprio herói.
Liste os heróis observados que fundamentam a explicação. Se faltar evidência, devolva item e advice nulos e evidence_heroes vazio.
"""


async def analyze(evidence: Evidence) -> Supplement:
    if not gemini_free.ready():
        raise HTTPException(status_code=503, detail="Provedor de estratégia indisponível")
    try:
        suggestion = await gemini_free.generate(
            [{"text": PROMPT}, {"text": evidence.model_dump_json()}],
            Supplement.model_json_schema(), Supplement, max_tokens=700,
        )
        if suggestion.advice is None:
            if suggestion.item is not None or suggestion.evidence_heroes:
                raise ValueError("Resposta contraditória")
            return suggestion
        own = next(player for player in evidence.players if player.hero == evidence.hero)
        if suggestion.item is not None and (suggestion.item not in evidence.candidates or suggestion.item in own.items):
            raise ValueError("Item fora dos candidatos ou já observado")
        if not set(suggestion.evidence_heroes) <= {player.hero for player in evidence.players} or not suggestion.evidence_heroes:
            raise ValueError("Justificativa sem referência observada")
        return suggestion
    except Exception as error:
        raise HTTPException(status_code=503, detail="Estratégia online temporariamente indisponível") from error
