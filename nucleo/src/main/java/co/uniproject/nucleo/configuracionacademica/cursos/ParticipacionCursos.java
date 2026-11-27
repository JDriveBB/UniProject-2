package co.uniproject.nucleo.configuracionacademica.cursos;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fuente de verdad de la pertenencia a cursos y su propagación a participante_proyecto. No revisa permisos: los exige
 * quien lo llama. Todos los métodos corren dentro de una transacción abierta (MANDATORY).
 *
 * <p>Orden único de candados para evitar interbloqueos:
 * <ol>
 *   <li>filas de correo_habilitado (solo el registro);</li>
 *   <li>curso con FOR NO KEY UPDATE, en orden ascendente de id_curso;</li>
 *   <li>proyecto con FOR NO KEY UPDATE, en orden ascendente de id_proyecto;</li>
 *   <li>equipo con FOR NO KEY UPDATE (quitarMiembro, cambiarLider);</li>
 *   <li>filas de miembro_curso, participante_proyecto, miembro_equipo y la auditoría.</li>
 * </ol>
 */
@Component
public class ParticipacionCursos {

    private final JdbcClient jdbc;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;

    public ParticipacionCursos(JdbcClient jdbc, RegistroAuditoria auditoria, Clock reloj) {
        this.jdbc = jdbc;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    /** Toma el candado del curso en el orden único de candados. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void bloquearCurso(long idCurso) {
        jdbc.sql("SELECT id_curso FROM curso WHERE id_curso = :c FOR NO KEY UPDATE")
                .param("c", idCurso)
                .query(Long.class)
                .single();
    }

    /**
     * Agrega al estudiante al curso y sincroniza su participación en todos los proyectos del curso. Devuelve false si
     * ya era miembro.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean incorporar(long idQuienAgrega, long idCurso, long idUsuario) {
        bloquearCurso(idCurso);
        int insertadas = jdbc.sql("INSERT INTO miembro_curso (id_curso, id_usuario, fecha_ingreso) VALUES (:c, :u, :f) "
                        + "ON CONFLICT DO NOTHING")
                .param("c", idCurso)
                .param("u", idUsuario)
                .param("f", OffsetDateTime.ofInstant(Instant.now(reloj), ZoneOffset.UTC))
                .update();
        if (insertadas == 0) {
            return false;
        }
        auditoria.registrar(EntradaAuditoria.cambio(
                idQuienAgrega,
                "miembro_curso",
                idCurso,
                AccionAuditoria.CREAR,
                "id_usuario",
                null,
                String.valueOf(idUsuario)));
        for (long idProyecto : proyectosDelCurso(idCurso)) {
            sincronizar(idProyecto, idUsuario);
        }
        return true;
    }

    /** Quita al estudiante del curso y sincroniza su participación en todos los proyectos del curso. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void retirar(long idQuienQuita, long idCurso, long idUsuario) {
        bloquearCurso(idCurso);
        int borradas = jdbc.sql("DELETE FROM miembro_curso WHERE id_curso = :c AND id_usuario = :u")
                .param("c", idCurso)
                .param("u", idUsuario)
                .update();
        if (borradas == 0) {
            throw new RecursoNoEncontradoException("Ese estudiante no está en el curso");
        }
        auditoria.registrar(EntradaAuditoria.cambio(
                idQuienQuita,
                "miembro_curso",
                idCurso,
                AccionAuditoria.ELIMINAR,
                "id_usuario",
                String.valueOf(idUsuario),
                null));
        for (long idProyecto : proyectosDelCurso(idCurso)) {
            sincronizar(idProyecto, idUsuario);
        }
    }

    /** Cursos asignados al proyecto en los que el usuario es miembro, en orden ascendente de id_curso. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> cursosValidos(long idProyecto, long idUsuario) {
        return jdbc.sql("SELECT cp.id_curso FROM curso_proyecto cp "
                        + "JOIN miembro_curso mc ON mc.id_curso = cp.id_curso AND mc.id_usuario = :u "
                        + "WHERE cp.id_proyecto = :p ORDER BY cp.id_curso")
                .param("p", idProyecto)
                .param("u", idUsuario)
                .query(Long.class)
                .list();
    }

    /**
     * Regla única de participación en el proyecto: decide alta, cambio de curso o baja según los cursos válidos y si
     * el estudiante tiene equipo. Si pertenece a un equipo con curso asignado, la participación conserva el curso de
     * ese equipo hasta que salga de él. No audita: es un dato derivado.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void sincronizar(long idProyecto, long idUsuario) {
        jdbc.sql("SELECT id_proyecto FROM proyecto WHERE id_proyecto = :p FOR NO KEY UPDATE")
                .param("p", idProyecto)
                .query(Long.class)
                .single();
        List<Long> validos = cursosValidos(idProyecto, idUsuario);
        Optional<Long> actual = jdbc.sql(
                        "SELECT id_curso FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u FOR UPDATE")
                .param("p", idProyecto)
                .param("u", idUsuario)
                .query(Long.class)
                .optional();
        Optional<Long> cursoEquipo = jdbc.sql(
                        "SELECT e.id_curso FROM miembro_equipo m JOIN equipo e ON e.id_equipo = m.id_equipo "
                                + "WHERE e.id_proyecto = :p AND m.id_usuario = :u AND e.id_curso IS NOT NULL LIMIT 1")
                .param("p", idProyecto)
                .param("u", idUsuario)
                .query(Long.class)
                .optional();
        if (cursoEquipo.isPresent()) {
            long c = cursoEquipo.get();
            if (actual.isEmpty()) {
                jdbc.sql("INSERT INTO participante_proyecto (id_proyecto, id_usuario, id_curso) VALUES (:p, :u, :c)")
                        .param("p", idProyecto)
                        .param("u", idUsuario)
                        .param("c", c)
                        .update();
            } else if (actual.get() != c) {
                jdbc.sql("UPDATE participante_proyecto SET id_curso = :c WHERE id_proyecto = :p AND id_usuario = :u")
                        .param("c", c)
                        .param("p", idProyecto)
                        .param("u", idUsuario)
                        .update();
            }
            return;
        }
        if (!validos.isEmpty()) {
            if (actual.isEmpty()) {
                jdbc.sql("INSERT INTO participante_proyecto (id_proyecto, id_usuario, id_curso) VALUES (:p, :u, :c)")
                        .param("p", idProyecto)
                        .param("u", idUsuario)
                        .param("c", validos.getFirst())
                        .update();
            } else if (!validos.contains(actual.get())) {
                jdbc.sql("UPDATE participante_proyecto SET id_curso = :c WHERE id_proyecto = :p AND id_usuario = :u")
                        .param("c", validos.getFirst())
                        .param("p", idProyecto)
                        .param("u", idUsuario)
                        .update();
            }
            return;
        }
        if (actual.isPresent() && !tieneEquipoEnProyecto(idProyecto, idUsuario)) {
            jdbc.sql("DELETE FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u")
                    .param("p", idProyecto)
                    .param("u", idUsuario)
                    .update();
        }
    }

    /** Sincroniza todos los participantes actuales y miembros de los cursos asignados al proyecto. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void sincronizarProyecto(long idProyecto) {
        List<Long> usuarios = jdbc.sql("SELECT id_usuario FROM participante_proyecto WHERE id_proyecto = :p "
                        + "UNION "
                        + "SELECT mc.id_usuario FROM miembro_curso mc JOIN curso_proyecto cp ON cp.id_curso = mc.id_curso "
                        + "WHERE cp.id_proyecto = :p ORDER BY 1")
                .param("p", idProyecto)
                .query(Long.class)
                .list();
        for (long idUsuario : usuarios) {
            sincronizar(idProyecto, idUsuario);
        }
    }

    private List<Long> proyectosDelCurso(long idCurso) {
        return jdbc.sql("SELECT id_proyecto FROM curso_proyecto WHERE id_curso = :c ORDER BY id_proyecto")
                .param("c", idCurso)
                .query(Long.class)
                .list();
    }

    private boolean tieneEquipoEnProyecto(long idProyecto, long idUsuario) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM miembro_equipo m JOIN equipo e "
                        + "ON e.id_equipo = m.id_equipo WHERE e.id_proyecto = :proyecto AND m.id_usuario = :usuario)")
                .param("proyecto", idProyecto)
                .param("usuario", idUsuario)
                .query(Boolean.class)
                .single();
    }
}
