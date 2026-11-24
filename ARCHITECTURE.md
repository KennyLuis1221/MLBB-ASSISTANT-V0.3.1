# Arquitetura e auditoria da V0.16

Referência `a554185ea74461edc3c97d52f07f31e22f0f08f7` da branch `development-v023`. O repositório preserva o ZIP de origem e aplica revisões pela ordem do workflow. A auditoria verificou o resultado materializado do Android e do backend, inclusive mudanças que sobrescrevem versões anteriores.

## Fluxo atual

`MediaProjection → VirtualDisplay → ImageReader RGBA → OfflineTextAnalyzer → observações temporais → Strategy13 → painel`

A rotina de captura descarta quadros da superfície anterior quando a dimensão muda. A apresentação de resultado usa a geração da captura para filtrar retornos de operações preexistentes. Android 14+ fornece mudanças na geometria através do callback da projeção. Nas versões anteriores, o serviço observa mudanças na tela padrão. A captura depende da autorização Android, da permissão de sobreposição e do acesso de uso que indique o jogo em foco. O serviço preserva o mesmo VirtualDisplay até encerrar a sessão.

ML Kit lê texto latino localmente. O detector usa descritores visuais de retratos e equipamentos com corte central, contraste, distância e margem. O placar é buscado por geometria calibrada ou por hipóteses proporcionais à tela. Candidatos repetidos na mesma linha e no mesmo slot ganham confirmação temporal. A posição do herói próprio associa os lados. O destaque visual é uma hipótese. A escolha manual só confirma o lado quando o herói aparece entre as observações repetidas. Slots sem classificação permanecem desconhecidos.

O motor `Strategy13` usa a observação disponível e o catálogo versionado. Ouro total do placar e saldo calibrado da loja são campos diferentes. A edição IA usa as mesmas regras e acrescenta uma consulta HTTPS em executor separado quando existe placar recente, o herói próprio está no lado aliado e há ao menos um inimigo. O backend valida os nomes e candidatos antes de chamar Gemini e valida a resposta antes de devolver um complemento. O painel mantém a análise Base utilizável durante falhas de rede e de provedor.

## Causas verificadas no código anterior

| Causa | Consequência | Correção |
| --- | --- | --- |
| O serviço escolhia análise local **ou** remota pela disponibilidade de URL. Intervalo com URL de 60 s para uma janela temporal de 20 s | Uma falha remota disparava OCR local tarde demais para confirmar os quadros. O modo online substituía o resultado local | Mesmo fluxo local em ambas as edições e consulta assíncrona opcional |
| O detector de retratos dependia de rótulos do placar ou de contagem de números extraídos pelo OCR | Um placar visível com texto reduzido podia nunca chegar ao detector | Classificação visual independente de rótulos, com suporte geométrico e repetição |
| Uma exceção ou espera do OCR interrompia a análise do quadro | Resultado sem retratos e builds | Espera local limitada para OCR. O detector visual ainda pode executar |
| Geometria calibrada inconclusiva bloqueava a busca em outras geometrias | Uma calibração incompatível ou mal posicionada podia suprimir todos os resultados | Busca alternativa, com diagnóstico da geometria usada |
| O modo replay processava o mesmo print duas vezes | Um arquivo repetido podia ser apresentado como confirmação em dois quadros | O fluxo aceita até dois prints distintos e mostra cada leitura |
| Redimensionamento dependia somente do callback novo e o bitmap intermediário era copiado com padding e recomprimido para JPEG | Orientações mais antigas sem atualização e perda potencial de detalhes | Observação da rotação nas APIs antigas, conversão considerando o stride de cada linha e bitmap direto para análise local |

As causas acima foram identificadas no código. A contribuição de cada uma para a falha relatada no aparelho permanece sem reprodução.
