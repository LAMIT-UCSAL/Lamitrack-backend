# Arquitetura

Dois diagramas em [PlantUML](https://plantuml.com), fonte de verdade do desenho da v1:

- **[topologia.puml](topologia.puml)** — as peças e como se ligam: fontes → ingestão →
  banco → API → site e bot.
- **[pipeline.puml](pipeline.puml)** — o que acontece a cada rodada diária, na ordem,
  com os invariantes que não podem ser quebrados.

Versão desenhada, para olhar rápido:
https://miro.com/app/board/uXjVH16FzhA=/

## Por que PlantUML e não só a imagem

O desenho do Miro é bonito e serve para explicar em reunião. O `.puml` é texto:
entra no diff da PR, dá para discutir uma linha na revisão e não desatualiza em
silêncio. As **notas de invariante** — o que não pode ser quebrado e por quê — só
existem aqui.

Para visualizar sem instalar nada: cole o conteúdo em https://www.plantuml.com/plantuml.
No VS Code, a extensão *PlantUML* mostra o preview lado a lado.

## Os dois invariantes que valem mais que o resto

1. **Uma fonte quebrada não derruba a coleta inteira.** Se o Sympla mudar uma classe
   de CSS, o scraper daquela fonte falha, registra no log e o job segue para a
   próxima. Sem isso, o dia inteiro se perde por causa de um site.

2. **Marcar como divulgado só depois do envio confirmado.** Se falhar entre o envio
   e a marcação, o pior caso é repetir a mensagem amanhã — visível e inofensivo.
   Na ordem inversa, o evento sumiria em silêncio.

## O que ainda não está decidido

- **Gateway do WhatsApp** (tarefa 30) — está no diagrama como decisão pendente.
  A API oficial da Meta aparentemente não envia para grupos; confirmar antes de fechar.
- **Onde o back-end roda** (tarefa 33).
- **A linguagem do back-end** — a ata decidiu React + TypeScript só para o front.
