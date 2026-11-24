# Relatório de testes

Referência Android experimental V0.16 na branch `development-v023`, commit `a554185ea74461edc3c97d52f07f31e22f0f08f7`. A publicação anterior gerava um único APK que alternava Base e IA pela interface. [Execução V0.16](https://github.com/KennyLuis1221/MLBB-ASSISTANT-V0.3.1/actions/runs/45399109046)

| Ensaio | Observação |
| --- | --- |
| Pipeline anterior | Auditoria do código confirmou intervalo de 60 s no caminho IA e exigência de par em até 20 s na análise local |
| Backend anterior | 48 passaram e 1 foi pulado na preparação |
| Backend atualizado | 54 passaram e 1 foi pulado por ausência das imagens previstas na suíte anterior. Casos novos cobrem autenticação, dados desconhecidos, falha do provedor e resposta fora do catálogo observado |
| Materialização independente | A preparação do ZIP e a aplicação sequencial das revisões reproduziram o código Android e servidor |
| Android JVM | 53 passaram na Base e 53 na IA, sem falhas ou pulos. O teste de regressão verifica que 1.000 ms é aceito e 1.001 ms excede o limite |
| APKs de teste | Base e IA 0.17.0, código 18, Android mínimo 29, target 35. O ZIP interno e a assinatura v2 de debug foram verificados. Apenas IA declara permissão de internet |
| Servidor configurado no app anterior | `GET /health` respondeu com versão 0.7.2, visão marcada online, 0 templates e sem capacidade `strategy` |
| Capturas reais autorizadas | 0 pares fornecidos. Precisão, recall, falsos positivos e negativos e acurácia do OCR sem medição |
| Aparelho físico com MLBB | Sem sessão de teste disponível. Latência real, CPU, PSS e estabilidade de partida pendentes |
| Gemini real | Respostas simuladas foram usadas nos testes de contrato. Sem invocação autorizada com estado real de partida |

Os critérios de aceitação de campo foram definidos em [VALIDACAO_ANDROID.md](VALIDACAO_ANDROID.md).
