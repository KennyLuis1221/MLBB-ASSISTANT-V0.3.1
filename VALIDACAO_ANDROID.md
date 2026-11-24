# Validação em Android real

Use dois aparelhos com proporções diferentes, um com Android 10 a 13 e outro com Android 14 ou superior. Registre modelo, API, resolução, build, commit, patch do MLBB e idioma. Colete 60 sequências autorizadas com ao menos três quadros úteis por sessão. Reserve partidas distintas para treino, calibração e avaliação. Execute 30 minutos por edição e dispositivo. Inclua modos, fases, skins e equipamentos diferentes e informe as classes sem amostra. Os limiares completos estão nos [critérios de aceitação](ACCEPTANCE_CRITERIA.md).

1. Instale os APKs de validação com `adb install -r arquivo.apk` ou abra cada arquivo no Android. Os identificadores são `com.mlbb.assistant.validation` e `com.mlbb.assistant.ia.validation`.
2. Autorize sobreposição e acesso de uso. Recuse a primeira solicitação de captura e confirme o aviso. Tente novamente e autorize. No Android 14 ou superior, selecione apenas MLBB quando essa opção for oferecida.
3. Abra o placar em treino ou replay autorizado. Deixe o painel compacto fora dos retratos e itens. Calibre placar, slot vazio e saldo da loja com prints da sessão de calibração quando necessário. Verifique primeiro o herói próprio automático. Registre separadamente a tentativa com indicação manual de herói.
4. Colete o placar com posições diferentes, retrato ilegível, duplicatas de herói, slots vazios e ilegíveis, compra, venda e troca de equipamentos. Inclua os casos de seis slots ocupados, saldo baixo, evolução e ausência temporária do placar. Abra o HUD e a loja para verificar a atualização de relógio e saldo.
5. Teste pausa, rotação, saída e retorno do MLBB, parada pela notificação, interrupção inesperada, retorno ao lobby, overlay encobrindo uma área e reinício de partida. Exporte o diagnóstico de cada transição. Sinalize explicitamente quando o sistema ocultar conteúdo.
6. Teste a Base em modo avião. Na IA habilitada, teste com rede, sem rede, com resposta atrasada e durante mudança de partida. Confira os estados exibidos e a continuidade das leituras locais. Mantenha o consentimento do modo IA separado da captura.
7. Registre a tela e exporte o diagnóstico após cada sessão. Compare latência e cadência a 1.000 ms. Registre PSS com `adb shell dumpsys meminfo com.mlbb.assistant.validation`, CPU com `adb shell top -b -d 1 -n 30` e crashes com `adb logcat -d -b crash`. Anote consumo, temperatura, ANR, dispositivo e condições da partida. Uma execução sem medição de renderização deixa o prazo pendente.

## Replay e manifesto

Em **Testar reconhecimento com print**, selecione as imagens na ordem temporal ou um vídeo de até cinco minutos. Exporte o JSON de replay. A numeração dos quadros começa em 1. Para imagens selecionadas, o replay usa intervalos de 1.000 ms. Use vídeo para preservar o tempo decodificado. Rejeite uma alegação de diversidade baseada em quadros visualmente equivalentes.

O manifesto é uma lista de registros. Os caminhos são relativos ao manifesto. O JSON anotado precisa usar a mesma partida e quadro declarados no registro.

```json
[
  {
    "partida": "sessao-01",
    "quadro": 3,
    "conjunto": "avaliacao",
    "origem": "aparelho-A e patch anotado",
    "esperado": "anotacoes/sessao-01-q3.json",
    "observado": "replays/sessao-01.json"
  }
]
```

Use `treino`, `calibracao` ou `avaliacao` em `conjunto`. O JSON anotado contém `partida`, `quadro`, `placar` booleano e `jogadores` como lista de linhas. Cada entrada descreve índices `lado_indice` de 0 a 1 e `linha_indice` de 0 a 4, `jogador_id` anônimo e constante na partida, `equipe`, `heroi`, `ouro`, `kda`, `nivel` e `slots` quando observáveis. Os slots são uma lista com `slot` de 1 a 6, `estado` e `item` quando confirmado. Use `ITEM_CONFIRMED` com o nome canônico, `EMPTY_CONFIRMED` quando vazio comprovado e `UNREADABLE` quando ilegível. Use os tipos exportados pelo aplicativo e preencha `tempo_partida` e `heroi_proprio` no nível superior quando legíveis. Uma tela sem placar tem `placar` igual a falso e lista de jogadores vazia.

```bash
python tools/score_real_captures.py manifesto.json > metricas.json
```

Envie os JSONs, o manifesto e as amostras autorizadas relevantes. O replay exercita reconhecimento local. O teste ao vivo mede adicionalmente captura, entrega ao painel, estabilidade e integração do APK.
