package co.uniproject.nucleo.trazabilidad.auditoria;

import java.util.Objects;

/**
 * Una fila de log_auditoria. entidadAfectada: nombre de tabla en minúscula ("tarea", "hito_equipo"…) o "http".
 * Los valores van como texto y nunca llevan contraseñas, tokens, códigos de vinculación ni chat_id (Ley 1581).
 */
public record EntradaAuditoria(Long idUsuarioAccion, String entidadAfectada, Long idRegistroAfectado,
                               AccionAuditoria accion, String campoModificado, String valorAnterior,
                               String valorNuevo) {

    public EntradaAuditoria {
        Objects.requireNonNull(entidadAfectada, "entidadAfectada");
        Objects.requireNonNull(accion, "accion");
    }

    /** Acción sobre un registro, sin detalle de campo. */
    public static EntradaAuditoria de(Long idUsuarioAccion, String entidad, Long idRegistro, AccionAuditoria accion) {
        return new EntradaAuditoria(idUsuarioAccion, entidad, idRegistro, accion, null, null, null);
    }

    /** Cambio de un campo con su valor anterior y nuevo. */
    public static EntradaAuditoria cambio(Long idUsuarioAccion, String entidad, Long idRegistro, AccionAuditoria accion,
                                          String campo, String anterior, String nuevo) {
        return new EntradaAuditoria(idUsuarioAccion, entidad, idRegistro, accion, campo, anterior, nuevo);
    }

    /** 403: método y ruta, sin parámetros de consulta. */
    public static EntradaAuditoria accesoDenegado(Long idUsuarioAccion, String metodo, String ruta) {
        return new EntradaAuditoria(idUsuarioAccion, "http", null, AccionAuditoria.ACCESO_DENEGADO, null, null,
                metodo + " " + ruta);
    }
}
