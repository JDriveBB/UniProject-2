package co.uniproject.nucleo.seguridad.permisos;

import co.uniproject.nucleo.comun.error.AccesoDenegadoException;

/**
 * Reglas de acceso del dominio (entrega1 §2.1 restricción 4a y 4b, RF13, RF14). Un id inexistente responde false
 * (403, no 404: no se revela qué ids existen). El administrador no tiene acceso académico.
 */
public interface Permisos {

    /** Dueño del proyecto. */
    boolean esDocenteDelProyecto(long idUsuario, long idProyecto);

    /** Monitor asignado y el proyecto trabaja con monitores (usa_monitores). */
    boolean esMonitorDelProyecto(long idUsuario, long idProyecto);

    /** Miembro del equipo, líder o integrante. */
    boolean esIntegranteDelEquipo(long idUsuario, long idEquipo);

    boolean esLiderDelEquipo(long idUsuario, long idEquipo);

    /** Docente dueño, monitor activo o miembro de algún equipo del proyecto. */
    boolean puedeVerProyecto(long idUsuario, long idProyecto);

    /** Miembro del equipo, docente dueño del proyecto o monitor activo del proyecto. */
    boolean puedeVerEquipo(long idUsuario, long idEquipo);

    /** Integrante del equipo o docente dueño del proyecto (el monitor no). */
    boolean puedeVerHistorialEquipo(long idUsuario, long idEquipo);

    /** puedeVerEquipo del equipo dueño de la tarea. */
    boolean puedeVerTarea(long idUsuario, long idTarea);

    /** Líder del equipo de la tarea: editar, asignar, reasignar, dependencias, revisar evidencias, pedir corrección. */
    boolean puedeGestionarTarea(long idUsuario, long idTarea);

    /** Encargado que sigue en el equipo, o líder del equipo (4a, RF13). Monitor y docente no (4b). */
    boolean puedeActualizarAvance(long idUsuario, long idTarea);

    /** Mismas reglas que puedeActualizarAvance (4a: adjuntos). */
    boolean puedeAdjuntarEvidencia(long idUsuario, long idTarea);

    /** Solo el encargado que sigue en el equipo. */
    boolean puedeSolicitarApoyo(long idUsuario, long idTarea);

    /** Quien puede ver la tarea (RF14: monitor incluido). Que la tarea esté en curso lo valida el servicio. */
    boolean puedeComentarTarea(long idUsuario, long idTarea);

    /** puedeVerEquipo del equipo del hito. */
    boolean puedeVerHitoEquipo(long idUsuario, long idHitoEquipo);

    /** Líder del equipo: crear tareas del hito y marcar prerrequisitos críticos. */
    boolean puedeGestionarHitoEquipo(long idUsuario, long idHitoEquipo);

    /** Cualquier miembro del equipo (entrega1 §2.2). RF7 lo valida el servicio. */
    boolean puedeEnviarHito(long idUsuario, long idHitoEquipo);

    /** Docente dueño del proyecto del hito (RF8). */
    boolean puedeEvaluarHito(long idUsuario, long idHitoEquipo);

    /** Lanza AccesoDenegadoException (403, auditado por ManejadorErrores) si no está permitido. */
    default void exigir(boolean permitido) {
        if (!permitido) {
            throw new AccesoDenegadoException();
        }
    }
}
