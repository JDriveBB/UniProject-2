-- Bloque 50: eventos de cursos, equipos y votación; la bandeja apunta también a cursos y proyectos.

ALTER TABLE evento_saliente DROP CONSTRAINT evento_saliente_tipo_ck;
ALTER TABLE evento_saliente ADD CONSTRAINT evento_saliente_tipo_ck CHECK (tipo IN (
    'tarea.ruta_critica.riesgo',
    'equipo.inactividad',
    'hito.enviado',
    'hito.evaluado',
    'tarea.reasignada',
    'tarea.correccion',
    'cuenta.recuperacion.solicitada',
    'cuenta.contrasena.cambiada',
    'curso.estudiante_agregado',
    'equipo.votacion_abierta',
    'equipo.lider_elegido',
    'equipo.sin_lider',
    'equipo.miembro_cambio',
    'equipo.inscripcion_por_cerrar',
    'equipo.inscripcion_resumen'
));

ALTER TABLE notificacion DROP CONSTRAINT notificacion_tipo_ck;
ALTER TABLE notificacion ADD CONSTRAINT notificacion_tipo_ck CHECK (tipo IN (
    'tarea.ruta_critica.riesgo',
    'equipo.inactividad',
    'hito.enviado',
    'hito.evaluado',
    'tarea.reasignada',
    'tarea.correccion',
    'curso.estudiante_agregado',
    'equipo.votacion_abierta',
    'equipo.lider_elegido',
    'equipo.sin_lider',
    'equipo.miembro_cambio',
    'equipo.inscripcion_por_cerrar',
    'equipo.inscripcion_resumen'
));

ALTER TABLE notificacion DROP CONSTRAINT notificacion_entidad_ck;
ALTER TABLE notificacion ADD CONSTRAINT notificacion_entidad_ck CHECK (entidad_referencia IN (
    'tarea', 'hito_equipo', 'equipo', 'curso', 'proyecto'));
