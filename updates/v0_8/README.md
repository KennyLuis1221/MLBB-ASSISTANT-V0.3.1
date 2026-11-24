# V0.8.0 — App Base experimental
- Dois botões de início; modo base força cloud_allowed=false, sem chamadas ao Render/Gemini.
- StrategicEngine: motor Kotlin puro, busca em feixe limitada a 24 caminhos por etapa. Planeja até 6 itens e próxima compra; não enumera todas as combinações. 133 nomes herdados, 12 perfis individuais experimentais, demais arquétipos; dados não verificados por patch. Propriedades qualitativas de subconjunto de itens, sem inventar preços/coeficientes.
- Evidências: herói, itens próprios e adversários, aliados com anti-cura, ouro total, KDA e fase; nível opcional no contrato mas não extraído localmente. Ouro total nunca usado como saldo. Orçamento/preço restante podem ser fornecidos ao motor; interface ainda não fornece preço nem árvore de receitas. Não autoriza venda/substituição automática. Restrições conservadoras de botas/crit/anti-cura, precisam de revisão por patch.
- Simulador manual no app. Draft usa exclusões/favoritos/composição e 7 hipóteses de interações, não matriz completa. Não classifica automaticamente bans/aliados/inimigos do draft local. 
- Painel mostra próximo item, plano, alternativas, justificativa, ameaças, diagnóstico. Mantém detector local experimental (92 retratos/76 ícones; sem validação em gameplay). Sem novos dados reais, não certificamos captura/precisão/UI/Xiaomi/FPS.
- IA online mantém backend V0.7.2, recebe build_plan quando disponível. Não modifica servidor, configurações ou assinaturas.
- Testes Kotlin JVM em dados sintéticos e 133 heróis, não certificam meta ou precisão visual.
