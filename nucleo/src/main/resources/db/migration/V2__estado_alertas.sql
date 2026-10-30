-- V2: estado para no repetir alertas del programador interno (plan §8.7).
-- Sin llaves foráneas todavía: tarea, proyecto y estudiante llegan con el bloque del dominio,
-- que agrega las FK con ALTER TABLE (no recrea estas tablas).

CREATE TABLE estado_riesgo_tarea (
    tarea_id    bigint      PRIMARY KEY,
    en_riesgo   boolean     NOT NULL,
    cambiado_en timestamptz NOT NULL
);
COMMENT ON TABLE estado_riesgo_tarea IS
    'Último estado de riesgo de cada tarea de la ruta crítica. tarea.ruta_critica.riesgo se publica solo al pasar de sin fila/false a true.';

CREATE TABLE alerta_inactividad (
    proyecto_id      bigint      NOT NULL,
    estudiante_id    bigint      NOT NULL,
    ultima_alerta_en timestamptz NOT NULL,
    CONSTRAINT alerta_inactividad_pk PRIMARY KEY (proyecto_id, estudiante_id)
);
COMMENT ON TABLE alerta_inactividad IS
    'Fecha de la última alerta equipo.inactividad por estudiante y proyecto. Se vuelve a alertar solo si hubo actividad después de esa fecha.';
