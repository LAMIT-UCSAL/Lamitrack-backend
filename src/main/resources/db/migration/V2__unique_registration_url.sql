-- Chave de idempotência do pipeline (issue #44): o registration_url contém o
-- slug/id do evento na Sympla, então é único por evento. A constraint impede
-- linha duplicada na tabela events quando o mesmo evento é raspado de novo
-- (o pipeline faz upsert por registration_url, ver SymplaPipeline).
-- SQL portável entre Postgres (testes/produção) e H2 em memória (esqueleto de
-- runtime, ver application.yml).

ALTER TABLE events
    ADD CONSTRAINT uq_events_registration_url UNIQUE (registration_url);
