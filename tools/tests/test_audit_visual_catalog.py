import base64
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from audit_visual_catalog import audit


def test_audit_normalizes_shield_and_exposes_missing_active_labels(tmp_path):
    (tmp_path / "strategy13.tsv").write_text(
        "V\t2026-11-23\tref\nH\tLayla\nH\tNana\nI\tValkyrie's Shield\tDefense\t1\t\n"
        "I\tDagger\tAttack\t1\t\nI\tLantern of Hope\tMagic\t\t\n", encoding="utf-8"
    )
    descriptor = base64.b64encode(bytes(20)).decode()
    (tmp_path / "references_v7.txt").write_text(f"Layla\n{descriptor}\nAthena's Shield\n{descriptor}\n", encoding="utf-8")
    result = audit(tmp_path)
    assert result["herois_sem_referencia"] == ["Nana"]
    assert result["loja_sem_referencia"] == ["Dagger"]
    assert {row["nome"] for row in result["itens_sem_referencia"]} == {"Dagger", "Lantern of Hope"}
    assert result["item_ref_fora_catalogo"] == []
