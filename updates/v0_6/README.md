# MLBB ASSISTANT V0.6 — desenvolvimento e status

## Três componentes desta atualização

**1. Identificação visual experimental.** O APK inclui 193 referências compactas de ícones (117 heróis, 76 equipamentos) extraídas dos vídeos fornecidos. Combina reconhecimento de texto ML Kit com comparação visual de imagens pequenas, com limite de erro e rejeição de resultados ambíguos. Só tenta associar ícones ao lado inimigo se a tela tiver indícios de ser o placar durante a partida. Não lê itens ocultos nem deduz que um item observado na loja foi comprado. Layouts de scoreboard e skins diferentes podem não ser detectados; sem calibração com partidas reais, a precisão é **desconhecida**.

**2. Builds adaptativas.** Motor local Kotlin e backend Python analisam herói selecionado, personagens adversários identificados e equipamentos do lado inimigo efetivamente observados; recalculam candidatos a cada atualização de evidências. Evitam recomendar item já informado como comprado. Não são builds validadas individualmente para cada herói; pesos são heurísticas e não probabilidades. A recomendação é apenas uma sugestão e não controla o jogo.

**3. Servidor online.** Foi preparado Dockerfile e render.yaml para implantar FastAPI HTTPS em uma conta do operador, com chave de visão somente no servidor e autenticação por token. **Não há serviço público implantado, nem endpoint confirmado.** Por padrão o APK utiliza análise **local automática** sem URL ou token; se houver backend online próprio, ele pode ser configurado em Avançadas. Um serviço pronto e totalmente automático ainda exige hospedagem, credenciais, autenticação de usuários e políticas de privacidade — não é tecnicamente correto dizer que isso já está funcionando.

## Instalação

1. Baixe o APK V0.6 da execução GitHub Actions.
2. Instale no Android. Se a assinatura debug for diferente do APK anterior, desinstale o anterior após salvar dados.
3. Autorize o painel flutuante e o reconhecimento de app em uso.
4. Escolha seu herói em Dicas de Build (identificação automática do herói do próprio jogador ainda não é confiável).
5. Na home, marque a autorização de captura e inicie a sessão. Conceda a permissão de captura do Android.
6. Abra MLBB e, durante a partida, abra o placar. Use a bolha flutuante para observar se foram detectados heróis/itens.
7. Se constar 'sem ícones confiáveis', o sistema não deve inventar dados.

## Implantação opcional de backend

- Crie uma conta de hospedagem com suporte a contêineres (por exemplo, Render).
- Use o `render.yaml` e o diretório `server/` deste projeto. O arquivo `OPENAI_API_KEY` deve ser configurado **somente na hospedagem**. Não coloque essa chave no APK ou no repositório.
- Defina `MLBB_API_TOKEN` como segredo exclusivo do servidor e autorize clientes por mecanismos seguros. Para testes privados, o operador pode configurar URL/token em Avançadas. Não publique um token compartilhado no APK.
- Configure `MLBB_VISION_ENABLED=1` somente após revisar custos, retenção, consentimento e limites de uso.
- O processamento visual online envia capturas autorizadas ao backend e, quando habilitado, ao provedor de IA.

## Limitações importantes

- Somente código compilado + testes unitários/automáticos do backend podem ser confirmados no GitHub; sem aparelho de laboratório não se pode declarar estabilidade em MLBB/Xiaomi.
- O dataset de vídeo é um catálogo ilustrativo; não demonstra leitura de scoreboard real em Android, nem performance quantitativa.
- A seleção de heróis permanece baseada em informações inseridas manualmente, sem counters específicos validados.
- Não oferece, nem promete, invisibilidade no mapa, leitura de memória ou automação do jogo. Verifique conformidade com regras do jogo.
- Um jogo competitivo pode restringir o uso de overlays e assistentes; teste preferencialmente em partida personalizada e em uma conta de teste.
