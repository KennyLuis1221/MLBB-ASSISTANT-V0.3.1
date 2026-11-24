# Procedimento de validação em Android real

## Preparação e critérios

Use pelo menos dois aparelhos com proporções diferentes. Inclua Android 14 ou posterior e, para validar a rota antiga de rotação, um aparelho Android 10 a 13. Registre modelo, API, resolução, build do APK, versão do jogo e idioma. Use partidas de treino ou replays que você tenha autorização para capturar. Utilize prints diferentes para a calibração e para a avaliação.

| Métrica | Critério de aceitação antes do ensaio |
| --- | --- |
| Latência local | Máximo de 1.000 ms, zero quadros descartados por prazo. Grave também os intervalos reais entre tentativas enquanto a tela do jogo fornece quadros |
| Retratos | Precisão ≥95% e recall ≥90% nos retratos visíveis, por posição de placar. Confirme a associação do herói próprio aos aliados |
| Equipamentos | Precisão ≥95% e recall ≥85% nos slots visíveis, com componentes, botas, trocas e slots vazios anotados |
| OCR | ≥90% de leituras exatas por campo legível avaliado. Reporte tempo, ouro, KDA e nível separadamente quando amostras estiverem disponíveis |
| Falsos positivos | Nenhum herói ou item confirmado em telas sem placar na amostra de regressão. Registre precisão, recall, totais de erros e tamanho das amostras |
| Usabilidade e estabilidade | Sem crash ou ANR em 30 min por aparelho e edição. Orçamento de triagem de 250 MiB PSS e 25% de um núcleo em média, sujeito à medição em modo compacto |
| Base e IA | Base atualiza em modo avião. Na IA ativa, uma mudança relevante que permaneça visível deve produzir complemento fundamentado. Ausência de rede, erro HTTP e perda de foco mantêm ou retomam Base conforme o estado da captura |

O limiar elevado de precisão reduz a possibilidade de recomendar compra por uma ameaça atribuída incorretamente. Uma pequena diferença no recall de itens acomoda slots com ícones pequenos e permite registrar campos desconhecidos. Os limites de recursos são critérios de triagem para um painel que roda simultaneamente ao jogo.

## Execução

1. Registre um mínimo de 60 pares de prints independentes e autorizados. Distribua a coleta entre dois aparelhos, placar, HUD, seleção, loja e resultado final. Inclua troca de um item no mesmo slot entre pares, diferentes heróis, composições e skins.
2. Anote os valores visíveis manualmente antes de executar o aplicativo para evitar viés. Para tempo, KDA, nível e ouro, anote apenas valores legíveis. Use os menus de replay e de exportação. Verifique se as recomendações mudam quando o item de defesa adversário muda, indicando a ameaça observada.
3. Em cada edição, negue a primeira solicitação de captura, depois autorize. Teste botão de pausa, rotação, troca de aplicativo, interrupção pela notificação, retomada, retorno ao lobby e tela sem detalhes. Exporte o diagnóstico imediatamente após cada reprodução.
4. Deixe o placar aberto por pelo menos 30 s com cada combinação. Exporte a latência, intervalos e atrasos. Registre PSS com `adb shell dumpsys meminfo com.mlbb.assistant.debug` ou o pacote IA. Registre CPU com `adb shell top -b -d 1 -n 30` e anote uso relativo a um núcleo. Verifique crashes com `adb logcat -d -b crash`.
5. Na Base, ative modo avião antes da captura. Na IA, compare uma consulta autorizada a um servidor atualizado com modo avião e com o servidor indisponível. Valide visualmente o complemento remoto e confirme que a Base continua tendo leituras enquanto a consulta está em trânsito.

## Formato da pontuação

Crie um manifesto JSON com uma lista de registros. Cada registro tem `origem` (aparelho, versão do jogo e sessão anonimizada), `esperado` (arquivo da anotação) e `observado` (JSON exportado pelo app). Use caminhos relativos ao manifesto.

A anotação tem `jogadores` como lista de linhas. Cada linha traz `lado_indice` de 0 a 1, `linha_indice` de 0 a 4 e, quando observáveis, `heroi`, `ouro` e `kda` como texto e `nivel` como número. `slots` é um objeto com chaves de `1` a `6`. Informe o nome canônico do equipamento ou `null` para um slot comprovadamente vazio. Omita campos ilegíveis. Informe `tempo_partida` no nível superior quando visível. Para uma imagem sem placar, use `sem_placar` igual a `true` e lista de jogadores vazia.

```bash
python tools/score_real_captures.py manifesto.json > metricas.json
```

Avalie categorias de OCR separadamente com anotações filtradas por campo. Em hardware sem gravação de imagem, registre o estado pelo diagnóstico. O replay mede reconhecimento e a análise local. O ensaio ao vivo mede também a captura, os tempos, a sobreposição e a integração IA.
