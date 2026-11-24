# MLBB ASSISTANT 0.7 — integração experimental

O APK não é uma versão final certificada. V0.7 substitui o matcher 3x3 pelo descritor com gradientes 64 bits + 4 médias RGB, usando 92 retratos e 76 itens com recortes revisados. Cobertura visual parcial. Posições de placar ainda são hipóteses, exigem validação em partida. A leitura só aceita candidatos repetidos na mesma linha. Time próprio depende de herói informado ou destaque repetido; nenhum resultado deve ser tratado como certeza. Ouro é candidato OCR da linha; KDA só se houver separadores legíveis. O modo draft local usa nomes lidos e função, sem counters validados. O motor de builds continua heurístico, não certificado por patch.

O painel foi reconstruído inspirado na prévia (cartões azul-marinho, ciano e ouro, retratos e itens recortados de capturas reconhecidas, explicação, dados, idade e contadores); não reproduz o mockup pixel a pixel, nem desenha caixas sobre o jogo.

A captura preserva a proteção de primeiro plano. O diagnóstico informa se chegam quadros e se são analisados; se HyperOS impedir confirmação do jogo, não processa. A tela inicial registra o último status e abre MLBB após consentimento quando Android disponibiliza o pacote.

## Online
O backend possui contrato expandido para draft, jogadores, ouro, KDA, nível e tempo; sessão automática opcional com quotas em memória. Não há servidor publicado nem credencial de provedor. Para ativar: implantar o backend em conta Render, configurar chaves exclusivamente no servidor, ativar visão, configurar quotas e definir MLBB_DEFAULT_API_URL (HTTPS) como variável de repositório no GitHub. A emissão automática fica desligada por padrão. Exige consentimento de envio separado no app. Para produção, usar autenticação/attestation, quotas persistentes e proxy confiável. Não use chaves pessoais no APK.

## Validação
A compilação e os testes de backend não certificam a captura Android, OCR, classificação nem recomendações em partidas reais. Os vídeos fornecidos mostram catálogos; os prints reais disponíveis são resultados finais, não partidas em andamento. Necessário teste em Xiaomi e amostras reais de draft/placar/loja para medir precisão. Antes de uso em conta competitiva verificar regras atuais do MLBB sobre assistência externa. Nenhum controle automatizado, leitura de memória ou informação oculta.
