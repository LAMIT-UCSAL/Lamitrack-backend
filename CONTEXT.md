# Lamitrack-backend

Scraper, API e persistência de eventos do LamiTrack: descobre eventos em fontes externas (Sympla e outras), normaliza para um formato comum e expõe para o front.

## Language

**Scraper**:
Um componente que busca eventos em uma fonte externa e devolve dados no formato de Evento (issue #9), independente da linguagem em que for implementado. A #14 exige que uma segunda fonte funcione reusando a mesma interface do Scraper da #13 (Sympla).
_Avoid_: Scrapper (grafia usada hoje nas pastas `scrapper/app-java`, `scrapper/app-python` dos branches POC — errada em inglês, vale corrigir para `scraper/` quando esse código for promovido para a `main`)

**Fonte**:
Um site, RSS ou outra origem externa de onde um Scraper extrai eventos. Sympla é a primeira (#13); a segunda (#14) ainda não foi escolhida.
