# MLBB ASSISTANT — V0.4 (build de testes)

Esta versão é uma evolução direta da V0.3.1. O código compilado usa a pasta `updates/v0_4/` do repositório, aplicada ao projeto Android que está no ZIP legado.

## Entregas implementadas

- Nova interface Android: tela inicial limpa, com IA/status, permissões, privacidade e configurações técnicas ocultas em **Avançadas**.
- Tela **Sugestão de personagem** por lane (Selva, EXP, Meio, Ouro, Roam), composição informada manualmente e heróis favoritos.
- Catálogo de nomes/classes de mais de 120 heróis, exportado para Android e servidor.
- Tela **Dicas de build** por herói/classe e ameaças assinaladas manualmente; não é build otimizada individualmente.
- Backend FastAPI anterior preservado, com análise visual online opcional, memória da partida e motor genérico multi-classe; novo `POST /draft`.
- Sem envio de telas sem autorização. Uso do MediaProjection e checagem do jogo em foco preservados.

## O que ainda NÃO está entregue

**Esta não é uma versão final com IA automática implantada.**

- Não existe um servidor de IA publicado e operando no endereço padrão do APK. O botão de análise ao vivo explica a dependência até a configuração de um backend real em Avançadas.
- Não há chaves de provedor de IA embutidas (por segurança). A implantação requer servidor HTTPS, credenciais, custos e política de privacidade.
- O reconhecimento visual para todos os heróis/itens e seleção automática durante o draft não foi treinado/validado em partidas reais.
- A tela Smart Hero Pick usa informações preenchidas pelo jogador e critérios de função/composição; **não comprova counters ou meta atual**.
- Dicas de builds são heurísticas genéricas por classe, sem calibração de patch 2026 ou validação de matchups.
- Sobreposição/captura só foram testadas até o nível de compilação; precisam ser testadas em aparelhos Xiaomi e em jogo real.
- Esta é uma build debug: a assinatura pode ser diferente da APK V0.3.1. Se o Android informar conflito de assinatura, será necessário desinstalar a versão antiga (fazendo backup dos dados importantes antes).

## Como baixar e instalar

Na execução verde do GitHub Actions, baixe o artefato `MLBB-ASSISTANT-APK`. Extraia `app-debug.apk` no celular e instale. Também será publicado o artefato `MLBB-ASSISTANT-V0.4-FONTES`, contendo os fontes realmente usados para compilação.

## Para habilitar a análise online real

1. Hospede o backend `server/` com HTTPS e autenticação; não exponha uma instância pública sem controle de acesso.
2. Defina a chave de visão no **servidor**, jamais no APK.
3. Configure `MLBB_VISION_ENABLED=1` após validar custos, segurança, política de dados e conformidade com o jogo.
4. Configure o endereço do backend em **Avançadas**, até que um servidor padrão próprio seja implantado.
5. Autorize as permissões Android e a captura no começo de cada sessão.

## Referência de catálogo

Nomes e classes baseados nas páginas `https://www.mobilelegends.com/hero/list` e `https://mlbbhub.com/heroes`, consultadas em 2026-11-23. O catálogo é um ponto de partida; deve ser revisado a cada atualização. Não usar indicadores especulativos como porcentagens de confiança.

## Nota

Não há vínculo ou endosso da Moonton. A compatibilidade com as regras do jogo ainda precisa ser avaliada antes de distribuir ou usar em conta importante.
