# Linguagem do back-end: Java

**Status**: accepted

A linguagem do back-end estava indecisa desde o kick-off (issue #38), travando #9, #13 e #19. Os critérios registrados na própria issue apontavam para Python ou Node (conhecimento prévio do time, maturidade do ecossistema de scraping). Decidimos **Java** mesmo assim, não pelo que o time já sabe, mas pela reparabilidade do repositório no longo prazo: desenvolvimento orientado a testes (TDD) e documentação mantida a cada iteração (este `CONTEXT.md` e os ADRs em `docs/adr/`).

## Consequências

- O consumo de memória em repouso de uma JVM pesa mais na decisão de hospedagem (#33), ainda aberta e também travada pelo caminho do WhatsApp (#30).
- Fontes de eventos (`Scraper`s, ver `CONTEXT.md`) que exigirem automação de browser (JS pesado, sem dado embutido em HTML/JSON) ficam fora do alcance natural do Java e vão precisar de um componente em Python — ver ADR 0002.
