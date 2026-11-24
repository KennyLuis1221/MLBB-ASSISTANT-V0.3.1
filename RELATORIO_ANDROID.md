# Relatório de validação Android

As duas variantes usam o mesmo núcleo local. A Base não solicita INTERNET no manifesto mesclado. A IA usa a rede apenas para o complemento quando habilitado. As distribuições de validação usam Android mínimo 29, código de versão 19 e pacotes próprios. O certificado de debug é identificado nos artefatos.

Os testes Kotlin da recuperação passaram com 63 casos em cada edição. Eles cobrem fusão temporal, duplicidades, catálogo, recomendação, orçamento, correção de alternativas e contratos de dados. Lint e integridade dos APKs são registrados pelo GitHub Actions.

O ambiente não oferece aparelho Android físico nem USB. A tentativa anterior com emulador Android 15 por CPU não chegou a uma sessão ADB funcional e o ambiente continua sem KVM. Instalação, projeção real, overlay, comportamento em Xiaomi, comunicação APK → Render e ensaio de partida permanecem pendentes. [Procedimento](VALIDACAO_ANDROID.md) · [Testes](TEST_REPORT.md)
