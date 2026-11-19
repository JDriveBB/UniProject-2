-- V8, T-027 (decisión D-12 20A): el docente puede poner en un equipo a un estudiante habilitado que aún no tiene
-- cuenta. La invitación vive en su habilitación pendiente; al registrarse entra al equipo y la invitación se borra
-- (se consume). Por eso solo vale en habilitaciones de ESTUDIANTE sin usar.
ALTER TABLE correo_habilitado
    ADD COLUMN id_equipo_invitado bigint NULL REFERENCES equipo (id_equipo);

ALTER TABLE correo_habilitado
    ADD CONSTRAINT correo_habilitado_invitacion_ck
        CHECK (id_equipo_invitado IS NULL OR (rol_plataforma = 'ESTUDIANTE' AND fecha_uso IS NULL));

CREATE INDEX correo_habilitado_equipo_invitado_idx ON correo_habilitado (id_equipo_invitado)
    WHERE id_equipo_invitado IS NOT NULL;

COMMENT ON COLUMN correo_habilitado.id_equipo_invitado IS
    'Equipo del mismo proyecto al que entra el estudiante cuando cree su cuenta (D-12 20A). NULL = sin invitación. Se borra al registrarse, se cumpla o no.';
