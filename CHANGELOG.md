# Changelog

## 0.18.0 · validação

- Replay de até 300 imagens ou um trecho de vídeo de até cinco minutos, com ordem, timestamps, SHA-256 e a mesma análise local da captura.
- Placar por geometria e evidências de texto. Leitura de cada slot independe do retrato, e as linhas mantêm identidade temporal inclusive com heróis repetidos.
- Confirmação e expiração por campo, histórico de troca de equipamento, relógio com extrapolação limitada e saldo da loja por região calibrada.
- Importação validada, restauração do catálogo anterior e auditoria dos rótulos de herói e item. Fórmulas e descrições de Deter e Chastise compartilham seus valores.
- Validação de alternativas com a mesma simulação de compra do item principal. Orçamento desconhecido permanece explícito.
- Complemento da IA com contrato versionado, referência a fatos do placar, prazo de 12 s do provedor e fallback Base.
- Medição da análise e da entrega ao painel, pacotes de validação separados e verificação do certificado, hash e commit no GitHub Actions.

## 0.17.0

- Cadência local de 1 s e prazo de 1.000 ms por quadro em Base e IA, com descarte e contagem de resultados atrasados.
- Captura com tratamento de rotação, pausa, interrupção e quadros sem detalhes. Caminho local em bitmap.
- Leitura visual independente do OCR e busca de geometria alternativa quando a calibração não confirma o placar.
- Diagnóstico no aplicativo, exportação sem credenciais e replay por até dois prints.
- APKs Base e IA separados. Base sem permissão de internet.
- Complemento pelo Gemini com observações estruturadas e candidatos locais, consentimento próprio e fallback Base.
- Importação de atualizações do catálogo TSV compatível com as referências incluídas no APK.
