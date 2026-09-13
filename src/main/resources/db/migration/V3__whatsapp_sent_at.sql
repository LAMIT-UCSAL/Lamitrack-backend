-- Marca de envio do bot de WhatsApp (issue #57): quando o evento foi enviado
-- para o grupo do WhatsApp. NULL = evento ainda não enviado ("evento novo").
-- SQL portável entre Postgres (testes/produção) e H2 em memória (esqueleto de
-- runtime, ver application.yml).

ALTER TABLE events
    ADD COLUMN whatsapp_sent_at TIMESTAMP;
