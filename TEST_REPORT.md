# Relatório de testes

| Ensaio | Resultado |
| --- | --- |
| Compilação de referência V0.17 | Reproduzida antes das alterações. 53 testes Kotlin em cada variante |
| Android Base 0.18 | 63 testes JVM, zero falhas e zero pulos |
| Android IA 0.18 | 63 testes JVM, zero falhas e zero pulos |
| Lint Android | Base e IA passaram. Avisos de APIs obsoletas e de lint permanecem registrados |
| Backend na árvore completa | 56 aprovados e um pulado. A suíte anterior dependia das imagens da conversa ausentes no pacote |
| Backend com o script de produção na árvore da `main` | 47 aprovados e um pulado pelo mesmo motivo. Essa árvore mantém apenas as revisões necessárias no servidor |
| Ferramentas | Seis testes aprovados para avaliador, evidências duplicadas, atribuição e catálogo |
| Build de validação | APKs Base e IA. Código 19, minSdk 29, target 35, assinatura debug, verificação de SHA-256 e INTERNET exclusivamente na edição IA |
| CI inicial da recuperação | Reprovou a compilação de teste por smart cast de propriedade de outro módulo. Corrigido em `620617e` e confirmado na execução seguinte |
| Android instrumentado e campo | Pendente. Sem ADB em aparelho físico e sem conjunto autorizado de partida |
| Backend externo | Verificação publicada no relatório de integração |

Os testes unitários exercitam regras com entradas controladas. A precisão visual e o desempenho dependem do ensaio em campo. [Execução CI que corrigiu a compilação](https://github.com/KennyLuis1221/MLBB-ASSISTANT-V0.3.1/actions/runs/45418636145) · [Integração](RELATORIO_INTEGRACAO.md) · [Precisão](RELATORIO_PRECISAO.md) · [Desempenho](RELATORIO_DESEMPENHO.md) · [Android](RELATORIO_ANDROID.md)
