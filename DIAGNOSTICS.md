# Diagnóstico

Abra **Diagnóstico da captura**, toque em **Atualizar** e, para guardar a evidência, toque em **Exportar JSON**. Use o arquivo exportado logo após reproduzir a falha. O relógio do Android é registrado em UTC para os horários absolutos. As latências usam relógio monotônico.

| Campo | Interpretação |
| --- | --- |
| `captura`, `servico_ativo`, `permissao_*` | Estado do serviço e autorizações vigentes |
| `resolucao`, `ultimo_quadro_utc`, `quadros_recebidos` | Resolução da superfície de entrada, horário do último quadro amostrado pelo diagnóstico e contador recebido |
| `quadros_analisados_tentativas`, `quadros_processados`, `quadros_fora_do_prazo` | Tentativas com MLBB em foco, resultados locais publicados e leituras descartadas por terem mais de 5 s na publicação |
| `latencia_entrega_ms`, `entregas_acima_da_meta`, `entrega_visual_sem_medicao` | Latência da última confirmação gráfica, confirmações acima de 1 s e publicações sem confirmação medida. O contador de análises avança quando o resultado é publicado. |
| `latencia_local_ms`, `latencia_maxima_ms`, `ultima_latencia_fora_do_prazo_ms` | Latência de publicação local, máximo dessa latência e idade da última leitura descartada por atraso, em milissegundos |
| `ultimo_intervalo_local_ms`, `maior_intervalo_local_ms` | Intervalos observados entre inícios de análises na mesma sequência |
| `processamento_ms`, `entrega_visual_sem_medicao` | Tempo da etapa local e quantidade de entregas em que o Android não ofereceu a confirmação de renderização |
| `quadros_validos`, `quadros_descartados`, `quadros_duplicados` | Validade do conteúdo, descarte e duplicação. Um quadro válido também pode ser descartado por atraso |
| `reconhecimentos_completos`, `reconhecimentos_parciais`, `invalidacoes`, `erros` | Cobertura do placar entregue, invalidações e erros da captura/análise |
| `ocr`, `placar_atual`, `idade_observacao_ms` | Volumes de elementos/texto lido, confirmação no quadro atual e idade da observação |
| `jogadores`, `slots`, `distancia_retrato`, `margem_retrato` | Estado reconhecido. Distância menor e margem maior favorecem um candidato no detector. São escores do algoritmo |
| `base`, `justificativa_base`, `ia`, `ultimo_erro` | Última recomendação, justificação local, estado remoto e erro registrado |

O diagnóstico copia apenas campos selecionados. A exportação exclui imagens, OCR bruto, nomes de conta, identificadores de sessão, URLs e credenciais. A área **Testar reconhecimento com print** exporta uma sequência de leituras do mesmo analisador local que pode ser comparada com a verdade de campo usando o script de pontuação.
