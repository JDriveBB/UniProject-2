package co.uniproject.nucleo.ejecucion.servicio;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Candado por hito de equipo (decisión 19-2): toda escritura sobre las tareas de un hito, el envío y la evaluación
 * toman primero {@code hito_equipo FOR UPDATE}.
 *
 * <p>Orden de adquisición dentro de una transacción:
 * <ol>
 *   <li>{@code proyecto} ({@code FOR NO KEY UPDATE}, solo {@code agregarMiembro});</li>
 *   <li>{@code hito_equipo} ({@code FOR UPDATE}): la PRIMERA sentencia que bloquea, antes de leer con JPA o escribir
 *       filas de {@code tarea}, {@code dependencia_tarea}, {@code evidencia}, {@code solicitud_correccion},
 *       {@code registro_avance}, {@code comentario_tarea} o {@code solicitud_apoyo};</li>
 *   <li>filas de esas tablas (implícitas en UPDATE/INSERT);</li>
 *   <li>{@code evento_saliente} / {@code notificacion} (inserts del outbox).</li>
 * </ol>
 * Nunca se toman dos {@code hito_equipo} en una transacción. Como todos los escritores de tareas pasan primero por el
 * mismo candado, no hay espera circular. La supresión bloquea {@code usuario} y luego eventos/textos del titular; no
 * toma {@code hito_equipo}, así que tampoco forma ciclo.
 */
public final class BloqueoHito {

    private BloqueoHito() {
    }

    public static void bloquear(JdbcClient jdbc, long idHitoEquipo) {
        jdbc.sql("SELECT id_hito_equipo FROM hito_equipo WHERE id_hito_equipo = :he FOR UPDATE")
                .param("he", idHitoEquipo)
                .query(Long.class)
                .single();
    }

    /** Bloquea el hito de la tarea con una sola sentencia JDBC (antes de cargar la tarea con JPA) y devuelve su id. */
    public static long bloquearPorTarea(JdbcClient jdbc, long idTarea) {
        return jdbc.sql("SELECT he.id_hito_equipo FROM tarea t JOIN hito_equipo he "
                        + "ON he.id_hito_equipo = t.id_hito_equipo WHERE t.id_tarea = :tarea FOR UPDATE OF he")
                .param("tarea", idTarea)
                .query(Long.class)
                .single();
    }
}
