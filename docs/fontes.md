# Catálogo de fontes de eventos

Lista das **fontes** de onde o LamiTrack pode tirar eventos (ver "Fonte" e "Scraper" no `CONTEXT.md`), com o endereço, o que dá para extrair e como. Serve para escolher a segunda fonte (#14), planejar a deduplicação (#15) e decidir o que fica para o cadastro manual (#17).

Levantamento feito em **2026-11-03**. Sites mudam: antes de implementar um scraper, confira de novo a linha da fonte.

---

## 1. Critérios do que entra no LamiTrack (decisão do time)

O scraper traz tudo o que a fonte mostra. Os "principais eventos" dependem destes critérios. Os marcados como **Decidido** valem; os demais ainda são proposta.

| Critério | Pergunta | Situação |
|---|---|---|
| Região e tema | Só Salvador? Bahia? Online? | **Decidido (2026-11-03):** presencial na Bahia **e** online de qualquer lugar, desde que dentro do contexto de **inovação, tecnologia e empreendedorismo** |
| Tipo | Só as 5 categorias (Hackathon, Ideathon, Maratona, Edital, Eventos de Inovação)? | Proposta: sim, só as 5 |
| Público | Estudantes, startups, pesquisadores? | Estudantes e comunidade tech |
| Preço | Gratuitos e pagos? | Os dois (a API ainda não guarda preço) |
| Prazo | Só eventos futuros ou com inscrição aberta? | Proposta: só com data futura |
| Curadoria | Tudo o que o scraper achar entra, ou alguém aprova? | Proposta: aprovação humana antes de publicar (#17) |
| Data de um edital | `startsAt` é a publicação ou o prazo? | **Decidido (2026-11-03):** o **prazo de inscrição** (encerramento) |

> **Importante para o front:** a categoria do evento é lida das `tags`. Para o card mostrar a categoria, o scraper precisa gravar **uma tag com o nome exato** de uma das 5 categorias: `Hackathon`, `Ideathon`, `Maratona`, `Edital` ou `Eventos de Inovação`.

---

## 2. Resumo

Legenda de acesso: **API** = dados em JSON por endereço público · **HTML** = dados no HTML da página · **JS** = a página só mostra os dados depois de rodar JavaScript (exige navegador automatizado, ADR 0002) · **Manual** = sem coleta automática.

| Fonte | Tipo | Acesso | Categorias que traz | Prioridade |
|---|---|---|---|---|
| [Sympla](#sympla) | Plataforma de ingressos | HTML (página do evento); JS (busca) | Hackathon, Ideathon, Maratona, Eventos de Inovação | **Já implementada** (só por URL) |
| [FAPESB](#fapesb) | Fundação de amparo à pesquisa (BA) | **API** (WordPress REST) | Edital | **Alta — sugerida para a #14** |
| [Parque Tecnológico da Bahia](#parque-tecnológico-da-bahia) | Polo de inovação (Salvador) | HTML | Eventos de Inovação | Média |
| [Devpost](#devpost) | Plataforma de hackathons | **API** | Hackathon (online) | **Alta** (online entra no critério) |
| [Even3](#even3) | Plataforma de eventos acadêmicos | JS | Eventos de Inovação, Maratona | Baixa |
| [Doity](#doity) | Plataforma de eventos | A conferir | Eventos de Inovação | Baixa |
| [Eventbrite](#eventbrite) | Plataforma de ingressos | HTML (busca) | Eventos de Inovação | Baixa |
| [Outros órgãos e instituições](#outros-órgãos-e-instituições) | Sites próprios | Variado | Edital, Hackathon | A levantar |
| [Instagram, LinkedIn, Telegram](#redes-sociais) | Redes sociais | **Manual** | Todas | Via cadastro manual (#17); perfis na seção Redes sociais |
| [Index Bahia](#index-bahia) | **Um evento**, não uma fonte | Manual | Eventos de Inovação | Cadastro manual 1x/ano |

---

## 3. Detalhe por fonte

### Sympla

- **URL:** https://www.sympla.com.br · busca por cidade: `https://www.sympla.com.br/eventos/salvador-ba` (padrão `/eventos/{cidade}-{uf}`)
- **robots.txt:** `Allow: /` para qualquer robô.
- **Termos de uso:** **pendente.** A página (`termos-e-politicas.sympla.com.br`) bloqueia leitura automática (403). Alguém precisa ler no navegador e procurar cláusulas sobre robôs, coleta automatizada ou reprodução de conteúdo.
- **Como extrair:**
  - **Página de um evento:** é o que o `SymplaScraper` já faz. Título, data, local, cidade/UF e banner vêm da página.
  - **Busca e listagem:** carregam por JavaScript; o HTML inicial não traz os cards. Para descobrir eventos sem receber a URL pronta, é preciso: (a) achar a chamada que a página faz para buscar os resultados e usá-la, ou (b) navegador automatizado (Python, ADR 0002).
- **Não vem pronto:** categoria do evento (o menu "Categorias" é do site, não do evento) e se o evento é hackathon/ideathon. Dá para inferir por palavras no título e na descrição.
- **API oficial:** a API pública da Sympla é para o organizador consultar os próprios eventos (token por conta), não para buscar eventos de terceiros.
- **Situação no código:** `SymplaPipeline` processa **uma** URL por vez (`lamitrack.pipeline.url`). Falta a etapa de descoberta (busca → lista de URLs).

### FAPESB

- **URL:** https://www.fapesb.ba.gov.br · editais: https://www.fapesb.ba.gov.br/category/editais/
- **robots.txt:** sem restrições.
- **Como extrair — API do WordPress (recomendado):**
  `GET https://www.fapesb.ba.gov.br/wp-json/wp/v2/posts?search=edital&per_page=20`
  Responde JSON com `title.rendered`, `date` (publicação), `link`, `slug`, `content.rendered`, `excerpt.rendered`, `categories` (IDs; nomes em `/wp-json/wp/v2/categories`). Conferido: devolveu, por exemplo, "EDITAL FAPESB/SECTI – Nº 010/2026 – Universal" (publicado em 2026-07-03).
- **Atenção:**
  - O campo `date` é a **data de publicação**, não o prazo de inscrição. **Decidido: `startsAt` = prazo de inscrição.** Ele está no texto do post (`content`) ou no PDF do edital e precisa ser extraído (ex.: procurar "inscrições até", "prazo", "data limite"). Edital sem prazo encontrado não entra, ou vai para revisão manual.
  - A seção "Comunicado" mistura editais com erratas e prorrogações. Filtrar títulos que começam com "EDITAL".
  - A página de categoria em HTML às vezes mostra "Nada encontrado"; a API é mais confiável.
- **Mapeamento:** `title` → `title`; `link` → `registrationUrl`; `city` = Salvador, `uf` = BA (ou nulos: edital não tem local); tag `Edital`.
- **Por que é boa candidata para a #14:** JSON estável, sem JavaScript, sem login e de uma fonte pública. Prova que a interface do `Scraper` aceita uma fonte bem diferente da Sympla.

### Parque Tecnológico da Bahia

- **URL:** https://parquetecnologico-ba.org.br · agenda: https://parquetecnologico-ba.org.br/calendario-de-eventos/
- **robots.txt:** sem restrições.
- **Como extrair:** a agenda está no HTML (data, título, descrição curta). Os itens **não têm link individual** e quase nunca têm local. Há também a página "Cadastre seu evento", que pode virar parceria (eles mandam os eventos para o LamiTrack).
- **Conteúdo:** mistura eventos de inovação (Darwin on the Road, Open meetup, Masterclass da Validação) com eventos internos (corrida, festa junina). Precisa de curadoria.
- **Observação:** o rodapé do site tem um link com cara de spam injetado (`.../vitejte-v-novem-svete-zabavy-casino-point-cz/`). Não seguir links do rodapé no scraper.

### Devpost

- **URL:** https://devpost.com/hackathons · API: `https://devpost.com/api/hackathons?search=...&status[]=open&status[]=upcoming`
- **robots.txt:** liberado para robôs em geral (só alguns robôs específicos são bloqueados).
- **Como extrair:** a API responde JSON com `hackathons[]` e `meta.total_count`/`per_page` (paginação).
- **Atenção:** a busca por "brazil" voltou **0 resultados** em 2026-11-03. Como online agora entra no critério, vale buscar hackathons **online** (de qualquer país) e filtrar pelos temas de inovação, tecnologia e empreendedorismo (o Devpost marca temas por hackathon). Conferir idioma: muitos são só em inglês.

### Even3

- **URL:** https://www.even3.com.br/eventos
- **robots.txt:** proíbe só áreas internas (`/documentos/`, `/streaming/`, `/public/` etc.); a listagem é permitida.
- **Como extrair:** a listagem é montada por JavaScript (o HTML vem com marcadores `{{item.titulo}}`). Tem filtros de local, data, preço, categoria e formato; a busca exige 4 caracteres. Exige achar a chamada de dados da página ou navegador automatizado.
- **Conteúdo:** forte em eventos acadêmicos (congressos, semanas acadêmicas, maratonas de programação de universidades).

### Doity

- **URL:** https://doity.com.br
- **robots.txt:** `Allow: /`.
- **Como extrair:** não conferido em detalhe. Pendente.

### Eventbrite

- **URL:** https://www.eventbrite.com.br · busca por cidade: `/d/brazil--salvador/{termo}/`
- **robots.txt:** bloqueia RSS e Atom (`/rss/`, `/events/rss/`) e áreas internas; a busca não aparece bloqueada.
- **API:** a busca pública de eventos da API foi desativada pela Eventbrite há anos; a API atual é para o organizador. Coleta só pelas páginas.
- **Prioridade baixa:** pouco evento de tecnologia de Salvador lá, comparado à Sympla.

### Outros órgãos e instituições

Candidatos levantados por busca, **sem conferência técnica ainda**:

| Instituição | O que publica | Onde olhar |
|---|---|---|
| SECTI Bahia | Editais e chamadas de CT&I | https://www.ba.gov.br/secti |
| Sebrae Bahia | Editais (ex.: Centelha), eventos para startups | site do Sebrae BA |
| SENAI CIMATEC | Eventos e hackathons de inovação em Salvador | site do CIMATEC |
| Universidades (UFBA, UNEB, UCSal, IFBA) | Semanas acadêmicas, hackathons (ex.: SEMCOMP) | páginas de eventos de cada uma |
| TJBA e órgãos públicos | Hackathons pontuais (ex.: Hackathon+ TJBA) | portais de notícias dos órgãos |
| CNPq, Finep | Chamadas nacionais de inovação | portais oficiais |

Muitos desses publicam pouco e de forma irregular. Fazem mais sentido no **cadastro manual** (#17) do que num scraper próprio.

### Redes sociais

- **Instagram:** **não será raspado** (decisão já registrada em `docs/arquitetura/topologia.puml`: risco alto de bloqueio, e os termos proíbem coleta automática). Eventos que só aparecem lá entram pelo cadastro manual (#17).
- **LinkedIn e Telegram:** mesma regra, manual.
- **Perfis acompanhados** (cadastro manual, #17). A lista cresce com indicações do time:

  | Perfil | Rede | O que publica |
  |---|---|---|
  | [@hackathonbrasil_oficial](https://www.instagram.com/hackathonbrasil_oficial/) | Instagram | Hackathons pelo Brasil (presenciais e online). Não achamos site próprio com agenda (busca em 2026-11-03), então segue manual |

### Index Bahia

O diagrama de topologia cita "Index Bahia" como fonte, mas **não é um agregador**: é a **feira anual da indústria baiana** (FIEB e Sebrae Bahia, Centro de Convenções de Salvador). É um evento por ano, que entra pelo cadastro manual. Vale corrigir o `topologia.puml`.

---

## 4. Como cada fonte vira um Evento

Todas devolvem o mesmo formato (tabela `events`, ver `V1__criar_tabela_events.sql`):

| Campo | Obrigatório | Observação |
|---|---|---|
| `title` | sim | |
| `startsAt` | sim | data e hora locais, sem fuso. **Edital: prazo de inscrição** |
| `venue`, `city`, `uf` | não | nulos para online e para editais |
| `registrationUrl` | não (mas é a chave de deduplicação hoje) | link para a página original |
| `source` | sim | `sympla`, `fapesb`, `parque-tecnologico-ba`, `devpost`… |
| `bannerUrl` | não | |
| `tags` | não | **incluir a categoria com o nome exato** (ver seção 1) |

Deduplicação (#15): a mesma competição pode estar na Sympla e no site do organizador. Hoje a chave é `registrationUrl`; entre fontes diferentes, vai precisar comparar título e data.

---

## 5. Recomendações

1. **Decidir os critérios da seção 1** antes de escrever novo scraper.
2. **Segunda fonte (#14): FAPESB**, pela API do WordPress. É a de menor risco técnico e cobre a categoria Edital, que a Sympla não cobre. O ponto mais trabalhoso é extrair o prazo de inscrição do texto.
3. **Terceira fonte: Devpost**, pela API, para hackathons online (permitidos pelo critério de região).
4. **Sympla: implementar a descoberta** (busca por cidade e palavra-chave → lista de URLs) para o pipeline existente processar. Antes, **ler os termos de uso** (pendente).
5. **Cadastro manual (#17)** para Instagram, órgãos públicos, Index Bahia e eventos pontuais. É o que garante os "principais eventos" mesmo quando nenhuma fonte automática os tem.

## 6. Pendências

- [ ] Ler os termos de uso da Sympla (bloqueiam leitura automática).
- [x] Primeiro perfil: @hackathonbrasil_oficial. Ampliar a lista com o time.
- [ ] Decidir os critérios ainda em proposta (tipo, prazo, curadoria).
- [ ] Conferir Doity, SECTI, Sebrae BA, SENAI CIMATEC e páginas de eventos das universidades.
- [x] Decidir `startsAt` para editais → prazo de inscrição.
- [x] Decidir região → Bahia presencial + online, no contexto de inovação, tecnologia e empreendedorismo.
- [ ] Corrigir "Index Bahia" em `docs/arquitetura/topologia.puml`.
