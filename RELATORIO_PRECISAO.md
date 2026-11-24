# Relatório de precisão visual

| Escopo | Evidência | Resultado |
| --- | --- | --- |
| Identidades no catálogo | Auditoria automatizada de `strategy13.tsv` e `references_v7.txt` | 133 heróis e 133 referências correspondentes, sem heróis órfãos |
| Rótulos de equipamento | Auditoria nominal e alias Athena’s Shield → Valkyrie’s Shield | 102 registros de catálogo, 90 rótulos, 90 identidades referenciadas. A lista nominal fica no artefato do workflow |
| Equipamentos sem descritor | Registro de auditoria | 12 registros sem descritor. O artefato `visual-coverage.json` lista os nomes e a classificação |
| Avaliador | Testes unitários de associação, slot ilegível, duplicatas e separação por partida | Acerto visual e atribuição são medidos separadamente |
| Campo | Capturas anotadas independentes fornecidas nesta execução | 0 partidas, 0 imagens e 0 vídeos. Sem taxa de precisão, recall, F1, associação ou identificação própria |

A auditoria rastreia descritores e aliases do snapshot empacotado. Para validar o patch e os modos, obtenha referências legítimas dos especiais em falta e capture uma amostra independente para a calibração e a avaliação. Lantern of Hope é associado a Floryn e segue sem descritor. [Auditoria automatizada](tools/audit_visual_catalog.py) · [Registro de Lantern of Hope](https://mlbbdex.com/en/items/lantern-of-hope)
