# MLBB Assistant 0.17.0

Aplicativo Android para analisar as informações visíveis de Mobile Legends: Bang Bang. A edição Base roda localmente. A edição IA também roda o motor Base e pode consultar um servidor HTTPS compatível que usa Gemini. Ambas exigem Android 10 ou superior.

## Compilação

Requisitos de compilação incluem JDK 17, Gradle 8.9, plataforma Android 35, Build Tools 35.0.0, Python 3, Git, Bash e unzip. Com as fontes já materializadas, execute as tarefas Gradle no diretório `android-app/`. No checkout Git, crie primeiro uma árvore materializada em um diretório vazio.

```bash
python3 -m venv .venv
. .venv/bin/activate
python -m pip install PyYAML Pillow
python tools/materialize_android.py ../mlbb-build
cd ../mlbb-build/android-app
gradle --no-daemon :app:testBaseDebugUnitTest :app:testIaDebugUnitTest :app:assembleBaseDebug :app:assembleIaDebug
```

Os APKs saem em `app/build/outputs/apk/base/debug/app-base-debug.apk` e `app/build/outputs/apk/ia/debug/app-ia-debug.apk`. O GitHub Actions disponibiliza os dois APKs, o código materializado e os relatórios automatizados. A assinatura é de **debug**. Compare o SHA-256 do APK ao de `SHA256SUMS.txt` e verifique a assinatura com `apksigner verify --verbose --print-certs arquivo.apk`.

## Instalação e uso

1. Instale os APKs desejados pelo Android ou com `adb install -r arquivo.apk`. Os pacotes de teste são `com.mlbb.assistant.debug` e `com.mlbb.assistant.ia.debug`. O Android exige a mesma assinatura para atualizar um pacote já instalado. Se houver conflito, preserve as configurações que precisar antes de instalar no lugar do build anterior.
2. Autorize a sobreposição e o acesso de uso. Confirme a captura no diálogo do Android. No Android 14 ou superior, selecione apenas o MLBB quando o sistema oferecer a escolha.
3. Em uma partida de treino ou replay autorizado, abra o placar. Mantenha o painel compacto e fora dos retratos e slots. Se necessário, use **Calibrar placar com print** e indique seu herói nas configurações avançadas.
4. Na edição IA, autorize o envio dos dados estruturados se desejar usar Gemini. O servidor precisa oferecer `/strategy` e provisionar sessão ou aceitar um token de acesso do aplicativo. Chaves do Gemini ficam no servidor. O endereço padrão observado ainda informa versão 0.7.2 sem o recurso estruturado.

Consulte [procedimento de validação](VALIDACAO_ANDROID.md), [diagnósticos](DIAGNOSTICS.md) e [painel de situação](STATUS_REPORT.md).
