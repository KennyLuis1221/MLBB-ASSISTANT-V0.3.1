# Relatório de integração

O endpoint padrão do aplicativo é `https://mlbb-assistant-vision.onrender.com`. A linha de base respondeu com versão 0.7.2 e `POST /strategy` retornou HTTP 404.

A 0.18 expõe `POST /strategy` com autenticação e `strategy_contract` 1 em `/health`. O contrato aceita candidatos locais e evidências recentes do estado. O prazo do provedor é de 12 s e o cliente espera até 16 s pela resposta estratégica. As chamadas de abertura de sessão têm limites próprios. As falhas retornam ao motor Base e o provedor está configurado para o projeto gratuito. A cota local é por processo.

## Produção

O Render usa a branch `main`. A publicação manual incluiu apenas o backend. O script de produção rodou localmente na mesma árvore com 47 testes aprovados e um pulado pela ausência das imagens de teste visual. A árvore completa da recuperação passou em 56 testes de backend e seis testes de ferramentas.

| Verificação | Resultado |
| --- | --- |
| Primeiro deploy | `bab7769`, deploy `dep-dc2q8qk0916s73as37og`, versão 0.18.0. Rota presente e sessão disponível. A consulta ao provedor retornou HTTP 503 |
| Correção e segundo deploy | Esquema Gemini simplificado, limite de resposta ajustado e logs sanitizados. `ad46ce24b6a5751f03bd2e364b63ce9b4cd08857`, deploy `dep-dc2qb4ei0phs73bjkn5g`, estado `live` |
| Saúde | HTTP 200, versão 0.18.0, contrato 1, commit esperado, sessões habilitadas |
| Autenticação | `POST /strategy` sem token retornou HTTP 403. `POST /session` retornou HTTP 200 e a sessão foi aceita em `/strategy` |
| Provedor real | `POST /strategy` retornou HTTP 200 em 1.092 ms. O Gemini escolheu Malefic Roar e citou o item Blade Armor do Tigreal. O backend preservou o identificador e a revisão da requisição e retornou a condição de inventário parcial |

A chamada real ao provedor usou um caso controlado para verificar transporte, autenticação, contrato e validação de evidência. A comunicação de um APK instalado, o fallback visto no celular, a latência local e a qualidade em partidas reais seguem para o ensaio físico.

[Saúde pública](https://mlbb-assistant-vision.onrender.com/health) · [Commit em produção](https://github.com/KennyLuis1221/MLBB-ASSISTANT-V0.3.1/commit/ad46ce24b6a5751f03bd2e364b63ce9b4cd08857) · [Prazo e diagnóstico](DIAGNOSTICS.md) · [Modelo e plano oficial](https://ai.google.dev/gemini-api/docs/pricing/)
