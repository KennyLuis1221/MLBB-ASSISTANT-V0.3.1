# Relatório de validação Android

As duas variantes usam o mesmo núcleo local. A Base não solicita INTERNET no manifesto mesclado. A IA usa a rede apenas para o complemento quando habilitado. As distribuições de validação usam Android mínimo 29, código de versão 19 e pacotes próprios. O certificado de debug é identificado nos artefatos.

Os testes Kotlin da recuperação passaram com 63 casos em cada edição. Eles cobrem fusão temporal, duplicidades, catálogo, recomendação, orçamento, correção de alternativas e contratos de dados. Lint e integridade dos APKs são registrados pelo GitHub Actions.

O ambiente não oferece aparelho Android físico nem USB. A tentativa anterior com emulador Android 15 por CPU não chegou a uma sessão ADB funcional e o ambiente continua sem KVM. Instalação, projeção real, overlay, comportamento em Xiaomi, comunicação APK → Render e ensaio de partida permanecem pendentes. [Procedimento](VALIDACAO_ANDROID.md) · [Testes](TEST_REPORT.md)

## Identificação dos APKs

| Arquivo | SHA-256 |
| --- | --- |
| `MLBB-Base-0.18.0-validacao.apk` | `ad9c32e4a561dca26f1c834eeecc4906a5bb491f992b5c6a266265fc13cee617` |
| `MLBB-IA-0.18.0-validacao.apk` | `9f048539629e32214a76a95952e7cb97b7b578cd3a07f97ca3357f4e60b6a446` |

O commit dentro dos dois APKs é `f54b658579d7d9e39fa450bc335db0b1413cca63`. A assinatura Android Debug v2 usa o certificado SHA-256 `5d9acccf5acb3fbc2f1b799b5afc6703abbc5318e1cf55f924d013c3fe778502`. Cada versão de validação pode ter um certificado debug diferente. [Execução verificada](https://github.com/KennyLuis1221/MLBB-ASSISTANT-V0.3.1/actions/runs/45419410410)
