# Relatório de desempenho

A meta é processar um quadro em até 1.000 ms e seguir uma cadência local de 1 s enquanto o MLBB fornece novos quadros. A análise Base roda separadamente da rede. Se o quadro não cumprir o prazo ou a entrega não puder ser medida, o diagnóstico registra a ocorrência.

A suíte Kotlin cobre o limite de 1.000 ms, o atraso de 1.001 ms e o cálculo do próximo intervalo sem somar o tempo de análise à cadência. A entrega do painel usa a confirmação de submissão de frame oferecida pelo Android. Essa medição local não inclui uma medição óptica de apresentação no visor.

| Medida em partida real | Resultado |
| --- | --- |
| Latência captura → painel e intervalo entre inícios | Pendente, sem Android físico |
| Quadros descartados por prazo na partida | Pendente |
| CPU, PSS, bateria, aquecimento e estabilidade | Pendentes |
| Casos de captura, interrupção e rotação por fabricante | Pendentes |

Consulte o [diagnóstico](DIAGNOSTICS.md) e o [ensaio Android](VALIDACAO_ANDROID.md).
