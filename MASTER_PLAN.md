# Recuperação do MLBB Assistant

A base desta recuperação é a branch `codex/mlbb-auditoria` no commit `1a08dde`. A branch de trabalho `codex/mlbb-recuperacao-v3` preserva o histórico e as revisões que materializam o projeto a partir do ZIP original.

| Etapa | Pré-requisito | Evidência exigida |
| --- | --- | --- |
| 0. Avaliação e replay | Identificar a compilação e as falhas de partida | Regressões para associação incorreta, abstenção e quadros duplicados. Manifesto de sessões com separação entre calibração e avaliação |
| 1. Captura e visão | Avaliador corrigido | Linhas preservadas com retratos ausentes, slots explícitos, perfis e auditoria de referências |
| 2. Estado temporal | Observações independentes por linha | Transições, reordenação, expiração, herói próprio e reinício testados |
| 3. Catálogo | Dados identificados por patch | Validação de esquema e receitas, histórico de importações, lacunas rastreadas |
| 4. Base | Estado e catálogo válidos | Compras e alternativas consistentes com inventário e saldo. Nenhuma compra impossível nos casos testados |
| 5. IA | Contrato com estado consolidado | Fatos derivados do estado, prazos coerentes, fallback e prova de ambiente real quando disponível |
| 6. Telas | Evidências anotadas de cada tela | Cobertura por tela, campos desconhecidos explícitos e métricas por classe |
| 7. Diagnóstico e desempenho | Captura e fusão integradas | Tempos por estágio, métricas por tentativa, transições de foco e dados expirados |
| 8. Validação | APKs assinados e corpus independente | Compilação rastreável, regressões, integração e testes físicos conforme os critérios |

[Requisitos de aceitação](ACCEPTANCE_CRITERIA.md) · [Matriz de defeitos](ISSUE_MATRIX.md) · [Situação](PROJECT_STATUS.md) · [Evidências](EVIDENCE_INDEX.md)

## Ponto de retomada

A infraestrutura de avaliação foi implementada em `41d6342`, seguida da recuperação dos módulos em `85b9a7d` e das correções de CI e invalidação em `620617e`. A integração Gemini e o deploy de produção têm rastreabilidade própria. O ensaio de replay com corpus, os detectores complementares, a calibração e a validação física ainda condicionam a conclusão integral. [Integração](RELATORIO_INTEGRACAO.md) · [Continuidade](NEXT_ACTIONS.md)
