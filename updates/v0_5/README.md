# MLBB ASSISTANT V0.5 — teste de captura local (Android)

Esta versão usa a base V0.4 que compilou anteriormente e acrescenta o módulo de OCR local com Google ML Kit para reconhecer nomes de heróis e equipamentos quando aparecem como texto nas telas do jogo.

**Mudança principal:** o botão Iniciar Assistência não exige URL ou token. Com autorização de captura e acesso ao app em foco, realiza leitura local de texto, sem transmitir imagens. Se o usuário configurar um servidor em Avançadas, o envio online é opcional.

## Vídeos de referência
Foram examinados dois vídeos enviados pelo usuário em 23 de novembro de 2026:
- vídeo 1 (42,2 segundos): catálogo de heróis com nomes e retratos.
- vídeo 2 (37,9 segundos): catálogo de equipamentos com nomes e ícones.

A lista do OCR inclui 81 nomes de equipamentos transcritos dos vídeos. O catálogo geral do app continua com 133 heróis. Fora do APK compilado, foram extraídas 117 imagens de heróis e 76 imagens de itens para futura calibração. Essas imagens NÃO integram o modelo de OCR deste APK.

## Limitações OBRIGATÓRIAS de informar
- O reconhecimento local lê **texto visível**, não identifica de modo confiável os ícones dos equipamentos na tabela de placar durante a partida. Não infere compras dos cinco inimigos.
- A seleção automática dos três heróis ainda usa dados informados manualmente no app, não OCR da tela de draft.
- As dicas de itens são por classes e situações manuais, não há build otimizada por matchup validada no patch atual.
- A análise com IA visual completa exige backend HTTPS implantado e chave de provedor de visão armazenada no servidor. Nenhum servidor público padrão foi publicado e não há chaves no APK.
- O APK foi compilado no GitHub Actions, mas não testado em um aparelho real; desempenho, funcionamento do overlay e precisão devem ser avaliados pelo usuário.
- Não há garantia de compatibilidade com regras do Mobile Legends; evitar uso em conta importante antes de verificação das políticas de terceiros.

## Como instalar
No workflow verde, abra a seção Artifacts e baixe `MLBB-ASSISTANT-V0.5-APK`, extraia o `app-debug.apk` e instale. O GitHub Actions também disponibiliza as fontes com as mesmas alterações.

## Como testar
1. Abra o app e conceda permissão de sobreposição e acesso de uso.
2. Marque autorização de captura, toque em Iniciar Assistência e conceda permissão do Android.
3. Abra o Mobile Legends, toque na bolha flutuante e abra a loja / draft; se houver nomes em texto, o OCR poderá identificá-los.
4. Se não reconhecer texto, confira se o jogo está em primeiro plano e se a sessão de captura continua ativa. Não presuma detecção de ícones ou compra confirmada.
5. Na tela Dicas de Build selecione seu herói manualmente para que as heurísticas por classe exibam sugestões iniciais.

## Integridade
O app não injeta comandos, não lê memória do jogo, não altera partidas e não inclui anticheat bypass. O vídeo usado como referência é do usuário. Este projeto não é afiliado à Moonton.
