-- V5: cambio y recuperación de contraseña por Telegram (bloque 28; D6 sin correos).
-- El código de un solo uso no se guarda: solo el selector y el SHA-256 del verificador (decisión 28-3).
-- No toca el log de auditoría ni su trigger.

ALTER TABLE usuario
    ADD COLUMN version_credenciales integer NOT NULL DEFAULT 0,
    ADD CONSTRAINT usuario_version_credenciales_ck CHECK (version_credenciales >= 0);
COMMENT ON COLUMN usuario.version_credenciales IS
    'Sube en 1 con cada cambio o restablecimiento de contraseña. La sesión guarda la del login; si ya no coincide, FiltroCuentaSuprimida responde 401 (decisión 28-9).';

CREATE TABLE recuperacion_contrasena (
    id                uuid        PRIMARY KEY,
    usuario_id        bigint      NOT NULL REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    creada_en         timestamptz NOT NULL DEFAULT now(),
    vence_en          timestamptz NOT NULL,
    selector          bytea       NULL,
    verificador_hash  bytea       NULL,
    entregas          integer     NOT NULL DEFAULT 0,
    intentos_fallidos integer     NOT NULL DEFAULT 0,
    usada_en          timestamptz NULL,
    anulada_en        timestamptz NULL,
    CONSTRAINT recuperacion_contrasena_vigencia_ck CHECK (vence_en > creada_en),
    CONSTRAINT recuperacion_contrasena_codigo_ck CHECK (
        (selector IS NULL AND verificador_hash IS NULL)
        OR (octet_length(selector) = 8 AND octet_length(verificador_hash) = 32)),
    CONSTRAINT recuperacion_contrasena_contadores_ck CHECK (entregas >= 0 AND intentos_fallidos >= 0)
);
CREATE UNIQUE INDEX recuperacion_contrasena_selector_uk
    ON recuperacion_contrasena (selector) WHERE selector IS NOT NULL;
CREATE INDEX recuperacion_contrasena_usuario_idx ON recuperacion_contrasena (usuario_id);
COMMENT ON TABLE recuperacion_contrasena IS
    'Olvidé mi contraseña (bloque 28): 15 min, un uso, anulada al pedir otra o al cambiar la contraseña. selector + SHA-256 del verificador; el código no se guarda. Se borra un día después de vencer.';

CREATE TABLE limite_recuperacion (
    clave          varchar(80) PRIMARY KEY,
    ventana_inicio timestamptz NOT NULL,
    cuenta         integer     NOT NULL,
    CONSTRAINT limite_recuperacion_clave_ck CHECK (clave ~ '^(correo|ip|canje|cambio):[0-9a-f]{64}$'),
    CONSTRAINT limite_recuperacion_cuenta_ck CHECK (cuenta >= 1)
);
COMMENT ON TABLE limite_recuperacion IS
    'Contadores de ventana fija (decisión 28-6). La clave lleva el SHA-256 del correo, de la IP o del id, nunca el valor. Se borran al día.';

ALTER TABLE evento_saliente DROP CONSTRAINT evento_saliente_tipo_ck;
ALTER TABLE evento_saliente ADD CONSTRAINT evento_saliente_tipo_ck CHECK (tipo IN (
    'tarea.ruta_critica.riesgo',
    'equipo.inactividad',
    'hito.enviado',
    'hito.evaluado',
    'tarea.reasignada',
    'tarea.correccion',
    'cuenta.recuperacion.solicitada',
    'cuenta.contrasena.cambiada'
));
