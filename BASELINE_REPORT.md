# Linha de base

A referência Git é `1a08dde`, com código dos APKs publicados no commit `990bd3d`. O repositório remoto foi atualizado antes de abrir a branch de recuperação. O ZIP original, as revisões Kotlin e Python e o workflow de desenvolvimento reproduzem as duas variantes. A edição Base tem o pacote `com.mlbb.assistant.debug`; IA, `com.mlbb.assistant.ia.debug`.

| Área | Estado inicial |
| --- | --- |
| Compilação | Artefatos V0.17.0 com assinatura debug v2 e SHA-256 verificados. 53 testes Kotlin por edição. O certificado da Base difere do APK experimental 0.16 |
| Android | API mínima 29 e alvo 35. MediaProjection, ImageReader, VirtualDisplay, sobreposição, acesso de uso e serviço em primeiro plano |
| Detecção | OCR local do ML Kit e 4 geometrias fixas mais calibração opcional. 133 descritores de retratos, 90 rótulos de itens e nenhum estado explícito de slot vazio |
| Estado temporal | Linhas descartadas com retrato ilegível ou repetido, evidência parcial sobrescreve observações, troca A → B → B desaparece do histórico |
| Estratégia | Catálogo TSV com 133 heróis e 102 itens, sugestões dinâmicas e receitas existentes. Inventário ao vivo fica incompleto e o saldo obtido da loja é desconsiderado |
| Backend | FastAPI/Pydantic e provedor Gemini gratuito. 54 testes passaram e 1 foi pulado. O servidor padrão informa versão 0.7.2 e retorna 404 para `/strategy` |
| Avaliação | O avaliador da V0.17 aceita item associado ao herói errado e conta ausência de leitura como slot vazio. Replay aceita no máximo duas URIs |
| Operação | Ausência de corpus anotado autorizado, dispositivo físico acessível e validação ao vivo. Latência, precisão por classe e estabilidade ainda sem medição de campo |

[Execução original e relatórios](https://github.com/KennyLuis1221/MLBB-ASSISTANT-V0.3.1/actions/runs/45410533740)
