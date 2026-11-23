# MLBB ASSISTANT — versão 0.6.1 de diagnóstico

Este pacote corrige dois problemas observados na V0.6: ausência de qualquer sugestão quando o herói não foi identificado e sugestões de draft escondidas atrás de preenchimento manual.

## O que foi corrigido
- Tela principal mostra a escolha opcional de um herói e função para TESTE (salvos localmente).
- Tela de escolha de heróis mostra três candidatos logo ao abrir, identificados como sugestões gerais por função, sem counters verificados.
- Enquanto a captura procura dados visíveis, o painel mostra a dica de classe se um herói de teste tiver sido selecionado, com aviso explícito de que não é análise da partida.
- Quando a OCR classifica uma tela como draft, o painel apresenta sugestões preliminares de personagem para a função selecionada; esses resultados não usam aliados/inimigos sem confirmação.
- O painel identifica corretamente quando exibe personagens em vez de item.
- Tratamento de limpeza de recursos do reconhecedor local.
- Versão do código atualizada para 0.6.1.

## O que ainda NÃO foi corrigido
- Reconhecimento completo de heróis/equipamentos dentro do placar real.
- Reconhecimento confiável do herói controlado pelo jogador.
- Identificação automática de aliados, bans e inimigos durante draft real.
- Sugestões confiáveis por matchup para todos os heróis.
- Servidor público de IA funcionando automaticamente.
- Validação em aparelho Xiaomi.

A base atual usa referências simplificadas 3x3 pixels dos vídeos de catálogo, ainda sem conjunto validado de capturas durante partidas. Não é correto prometer que funcionará automaticamente com essas referências.

## Como testar sem confundir dados de teste e observações reais
1. Na tela inicial, escolha um herói de teste (por exemplo, Layla), salve e escolha uma função.
2. Abra "Sugestão de personagem": devem aparecer três nomes sem exigir digitação.
3. Inicie a assistência, conceda a captura e abra MLBB.
4. Abra a bolha. Uma dica genérica deve ser identificada como tal; ela não é a análise do jogo.
5. Durante uma partida personalizada, abra o placar durante o jogo. Observe status, inimigos detectados e atualização.
6. Capture e envie prints do painel aberto e do placar REAL durante a partida, sem nomes ou identificadores pessoais quando possível.

Se aparecer "Envio bloqueado: abra o MLBB", o acesso de uso ou a identificação do jogo precisa ser investigado com o diagnóstico do telefone.

## Próxima correção para funcionalidade final
Precisamos de capturas reais do draft, loja e placar em uma partida em andamento para avaliar o detector; também de backend com HTTPS e credenciais de IA antes de declarar análise online funcional.
