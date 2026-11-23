# MLBB ASSISTANT V0.6 — relatório de verificação (2026-11-23)

## Build Android
- GitHub Actions run: https://github.com/KennyLuis1221/MLBB-ASSISTANT-V0.3.1/actions/runs/45185861211
- Status final: **success**.
- Artefato APK: `MLBB-ASSISTANT-V0.6-APK`.
- Verificação do ZIP de saída: aprovada; APK extraído de `app-debug.apk`.
- O build não substitui testes em dispositivo físico.

## Backend
- Executado `PYTHONPATH=. pytest -q --disable-warnings --maxfail=1` em `server/` nos fontes exatos do artefato da build: **33 testes passaram**.
- Inclui testes de mudança de recomendação quando são informados itens de armadura, resistência mágica, cura e bans.

## Referências e CV local
- Banco compacto com 193 referências derivadas dos dois vídeos do usuário: 117 heróis e 76 itens.
- Autoteste com as próprias assinaturas: 185/193 foram distinguíveis pelo limiar escolhido; oito foram rejeitadas por proximidade excessiva.
- **Esse número não mede precisão em imagens reais do jogo.**
- Teste exploratório com capturas de placar final fornecidas anteriormente: comparações conservadoras rejeitaram candidatos; OCR deve descartar a tela final pelo texto VICTORY/DEFEAT.
- Não há validação quantitativa em placar durante partida (ainda faltam capturas/vídeos desse contexto).

## Integração e restrições
- Local: MediaProjection + ML Kit OCR + comparação de ícones + motor de itens, sem URL/token.
- Online: backend FastAPI integrado a motor adaptativo, Dockerfile e Render blueprint preparados; **não houve implantação de endpoint público**, portanto não existe servidor de IA conectado por padrão.
- Análise visual para todos os ícones/skins, atribuição confiável a cada jogador, counters de draft e builds por patch ainda precisam de dados/testes adicionais.
- Recomendação e percentuais não foram apresentados como confiança estatística.
