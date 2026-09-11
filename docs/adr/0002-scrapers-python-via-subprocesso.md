# Scrapers em Python entram via subprocesso, não como serviço separado

**Status**: accepted

Fontes que exigirem automação de browser (JS pesado, sem dado embutido em HTML/JSON) vão precisar de Python, fora do alcance natural do back-end em Java (ADR 0001). Em vez de um serviço Python separado com API própria, o Java invoca o script como subprocesso e lê a saída no formato de Evento (issue #9) — o mesmo contrato de qualquer `Scraper` (ver `CONTEXT.md`), independente da linguagem.

## Consequências

- Um único processo deployável, importante enquanto a hospedagem (#33) segue travada pelo caminho do WhatsApp (#30) e a pressão de memória em repouso é um critério real.
- Se o número de scrapers em Python crescer a ponto de precisar de um ciclo de deploy próprio, essa decisão vale revisitar — mas hoje não há alternativa real melhor que justifique a complexidade de dois runtimes sempre vivos.
