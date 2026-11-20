-- V9, T-040 (decisión D-15 5A): la cuenta solo se crea con un enlace de invitación de un solo uso (30 días) generado
-- por el docente o el administrador para esa habilitación. El código no se guarda: solo selector (8 bytes) y SHA-256
-- del verificador (32 bytes), con el mismo formato de CodigosRecuperacion (V5). No toca el log de auditoría ni su trigger.
ALTER TABLE correo_habilitado
    ADD COLUMN enlace_selector bytea NULL,
    ADD COLUMN enlace_verificador_hash bytea NULL,
    ADD COLUMN enlace_vence_en timestamptz NULL;

ALTER TABLE correo_habilitado
    ADD CONSTRAINT correo_habilitado_enlace_ck CHECK (
        (enlace_selector IS NULL AND enlace_verificador_hash IS NULL AND enlace_vence_en IS NULL)
        OR (octet_length(enlace_selector) = 8
            AND octet_length(enlace_verificador_hash) = 32
            AND enlace_vence_en IS NOT NULL));

ALTER TABLE correo_habilitado
    ADD CONSTRAINT correo_habilitado_enlace_uso_ck CHECK (
        fecha_uso IS NULL
        OR (enlace_selector IS NULL AND enlace_verificador_hash IS NULL AND enlace_vence_en IS NULL));

CREATE UNIQUE INDEX correo_habilitado_enlace_selector_uk
    ON correo_habilitado (enlace_selector) WHERE enlace_selector IS NOT NULL;

COMMENT ON COLUMN correo_habilitado.enlace_selector IS
    'Primeros 8 bytes del código del enlace de invitación (T-040, D-15 5A). NULL = sin enlace o habilitación ya usada.';
COMMENT ON COLUMN correo_habilitado.enlace_verificador_hash IS
    'SHA-256 (32 bytes) de los 24 bytes del verificador del enlace de invitación; el código en texto nunca se guarda.';
COMMENT ON COLUMN correo_habilitado.enlace_vence_en IS
    'Instante en que vence el enlace de invitación vigente (30 días desde que se genera). NULL = sin enlace o ya usada.';
