# Situação da recuperação

A **0.18.0 de validação** está na branch `codex/mlbb-recuperacao-v3`, sobre a referência `1a08dde`. A aprovação operacional está pendente de Android físico, corpus visual autorizado e cobertura dos detectores restantes.

| Módulo | Implementação | Automatizados | Integração | Operação |
| --- | --- | --- | --- | --- |
| Avaliador e replay | 🟡 Replay extenso e hashes exatos. Similaridade visual pendente | 🟢 Avaliador e auditoria | 🟡 Decodificação e exportação em Android físico pendentes | 🟡 Sem corpus |
| Captura, placar e estado | 🟢 Observações independentes, confirmação, expiração e histórico | 🟢 63 testes Kotlin por edição no total | 🟡 Build e lint aprovados. MediaProjection em aparelho pendente | 🟡 Identificação própria, inventários, qualidade e prazo ao vivo |
| Catálogo e Base | 🟡 Importação e recomendações corrigidas. Patch parcial | 🟢 Receitas, saldo, inventário cheio e catálogo | 🟢 Base compila sem permissão INTERNET | 🟡 Offline em partida física |
| IA | 🟢 Contrato 1, validação e fallback | 🟢 Backend e contrato passaram | 🟢 Endpoint autenticado e chamada real ao Gemini em produção | 🟡 Comunicação APK e análise de partida |
| Cobertura complementar | 🟡 Sinais de tela e saldo calibrável | 🟡 Classificação textual e regras | 🟡 HUD e telas complementares pendentes | 🔴 Minimapa, torres, Lorde e Tartaruga sem detectores dedicados |
| Diagnóstico | 🟢 Contagem e medidas por estágio | 🟢 Prazo e cadência com regressões | 🟡 Entrega ao painel medida no código | 🟡 Desempenho e ciclo de vida físicos pendentes |

[Próximos passos](NEXT_ACTIONS.md) · [Matriz](ISSUE_MATRIX.md) · [Evidências](EVIDENCE_INDEX.md)
