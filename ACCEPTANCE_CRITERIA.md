# Critérios de aceitação

| Indicador | Critério |
| --- | --- |
| Referências visuais | 100% dos heróis e equipamentos ativos aplicáveis ao patch, com alias reconciliado e fonte rastreável |
| Herói próprio | Precisão ≥99% e cobertura e abstenção publicadas |
| Heróis | Precisão e recall ≥98%, com relatório por classe |
| Itens | Precisão e recall ≥97%, com relatório por classe e posição |
| Associação de itens | Precisão ≥98% para time, jogador, herói e slot |
| Captura e painel ao vivo | No máximo 1.000 ms de latência local e cadência acompanhada por relógio monotônico. Registrar intervalos, descartes e distribuição das latências |
| Estratégia Base | Funciona offline. Zero compras impossíveis nos casos de regressão. Recomendações baseadas em dados confirmados, com saldo desconhecido sinalizado |
| IA | Zero afirmações factuais sem evidências nos testes. Fallback em todas as falhas previstas. Autenticação, backend e provedor verificados separadamente |
| Regressões | Zero falhas nos casos conhecidos |
| Operação | Android físico com partidas autorizadas, medidas de latência, memória, CPU e estabilidade |

O ensaio começa com pelo menos 60 sequências independentes de dois aparelhos, com Android 10 a 13 e Android 14 ou superior representados, proporções distintas, mínimo de três quadros úteis por sequência e 30 minutos por edição. O conjunto precisa abranger os modos e telas aplicáveis e deve informar as classes que ficaram sem amostra. Calibração e avaliação usam partidas separadas. O avaliador informa amostras por classe, precisão, recall, F1, cobertura, abstenção e erros. A amostra mínima permite triagem e não certifica classes com pouca ou nenhuma observação.
