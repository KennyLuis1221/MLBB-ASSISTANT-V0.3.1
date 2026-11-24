# Índice de evidências

- Referência 0.17.0 com APKs, fonte e testes publicados. [Execução GitHub Actions](https://github.com/KennyLuis1221/MLBB-ASSISTANT-V0.3.1/actions/runs/45410533740)
- Inventário da compilação anterior, evidências da auditoria e pré-requisitos. [Linha de base](BASELINE_REPORT.md)
- Avaliador recuperado no commit `41d6342`. Os testes ficam em `tools/tests` e exercitam atribuição, abstenção, slots e divisão por partida. [Avaliador](tools/score_real_captures.py)
- APKs Base e IA 0.18.0 de validação, fontes materializadas e relatórios. A primeira execução aprovada usou o commit `620617e` e conferiu assinatura, permissões e hash. [Execução 45418636145](https://github.com/KennyLuis1221/MLBB-ASSISTANT-V0.3.1/actions/runs/45418636145)
- A execução 45419410410 aprovou o commit `f54b658579d7d9e39fa450bc335db0b1413cca63`. Os APKs foram baixados e conferidos independentemente com assinatura v2, SHA-256 e o commit presente nos dois DEX. As fontes do provedor e da estratégia coincidem com as do deploy. [APKs](https://github.com/KennyLuis1221/MLBB-ASSISTANT-V0.3.1/actions/runs/45419410410/artifacts/14934051696) · [Fontes](https://github.com/KennyLuis1221/MLBB-ASSISTANT-V0.3.1/actions/runs/45419410410/artifacts/14933522978) · [Evidências automatizadas](https://github.com/KennyLuis1221/MLBB-ASSISTANT-V0.3.1/actions/runs/45419410410/artifacts/14933742551)
- Backend e chamada real ao Gemini no Render. O commit `ad46ce2` foi observado no endpoint publicado. [Integração](RELATORIO_INTEGRACAO.md) · [Saúde](https://mlbb-assistant-vision.onrender.com/health)
- Catálogo, precisão, desempenho e limites físicos. [Precisão](RELATORIO_PRECISAO.md) · [Desempenho](RELATORIO_DESEMPENHO.md) · [Android](RELATORIO_ANDROID.md) · [Testes](TEST_REPORT.md)
