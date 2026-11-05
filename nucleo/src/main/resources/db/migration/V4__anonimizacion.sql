-- V4: derecho de supresión (Ley 1581 de 2012; bloque 27, decisiones 27-2, 27-3 y 27-16).
-- La fila de usuario no se borra: se anonimiza para no romper las FK de los registros académicos.
-- log_auditoria y su trigger no se tocan: solo guardan ids (restricción 4e).
ALTER TABLE usuario
    ADD COLUMN fecha_anonimizacion timestamptz NULL;

ALTER TABLE usuario
    ADD CONSTRAINT usuario_anonimizado_ck CHECK (
        fecha_anonimizacion IS NULL
        OR (nombre = 'Usuario anonimizado'
            AND email = 'anonimo-' || CAST(id_usuario AS text) || '@anonimo.invalid'
            AND password_hash = '!'
            AND telegram_chat_id IS NULL
            AND NOT autoriza_tratamiento_datos));

COMMENT ON COLUMN usuario.fecha_anonimizacion IS
    'Cuándo se suprimió la cuenta (Ley 1581). No nula = sin datos personales (usuario_anonimizado_ck): no inicia sesión y sus sesiones abiertas dejan de servir. Los registros académicos siguen ligados a este id.';
