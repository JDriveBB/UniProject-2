-- V1 del agente: idempotencia por (evento, destinatario) (plan §8.4; decisiones-tecnicas-java §3).
-- Esquema agente (Flyway default-schema). Sin llave foránea hacia public: el rol del agente no tiene acceso a ese esquema.
CREATE TABLE evento_procesado (
    evento_id  uuid        NOT NULL,
    usuario_id bigint      NOT NULL,
    enviado_en timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT evento_procesado_pk PRIMARY KEY (evento_id, usuario_id)
);
COMMENT ON TABLE evento_procesado IS
    'Avisos ya enviados por Telegram. INSERT ... ON CONFLICT DO NOTHING tras cada envío exitoso.';
