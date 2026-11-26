package co.uniproject.nucleo.seguridad.permisos;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Consultas EXISTS sobre las tablas de V3; cada una se resuelve por claves primarias e índices. */
@Component
public class PermisosJdbc implements Permisos {

    private static final String MONITOR_ACTIVO = "(p.usa_monitores AND EXISTS (SELECT 1 FROM monitor_proyecto mp "
            + "WHERE mp.id_proyecto = p.id_proyecto AND mp.id_monitor = :usuario))";

    private static final String VER_PROYECTO = "SELECT EXISTS (SELECT 1 FROM proyecto p WHERE p.id_proyecto = :id "
            + "AND (p.id_docente = :usuario OR " + MONITOR_ACTIVO + " OR EXISTS (SELECT 1 FROM equipo e "
            + "JOIN miembro_equipo m ON m.id_equipo = e.id_equipo WHERE e.id_proyecto = p.id_proyecto "
            + "AND m.id_usuario = :usuario)))";

    private static final String VER_EQUIPO = "SELECT EXISTS (SELECT 1 FROM equipo e "
            + "JOIN proyecto p ON p.id_proyecto = e.id_proyecto WHERE e.id_equipo = :id "
            + "AND (p.id_docente = :usuario OR " + MONITOR_ACTIVO + " OR EXISTS (SELECT 1 FROM miembro_equipo m "
            + "WHERE m.id_equipo = e.id_equipo AND m.id_usuario = :usuario)))";

    private static final String DOCENTE_DE_EQUIPO = "SELECT EXISTS (SELECT 1 FROM equipo e "
            + "JOIN proyecto p ON p.id_proyecto = e.id_proyecto WHERE e.id_equipo = :id AND p.id_docente = :usuario)";

    private static final String TAREA_DE_MIEMBRO = "SELECT EXISTS (SELECT 1 FROM tarea t "
            + "JOIN hito_equipo he ON he.id_hito_equipo = t.id_hito_equipo "
            + "JOIN miembro_equipo m ON m.id_equipo = he.id_equipo AND m.id_usuario = :usuario "
            + "WHERE t.id_tarea = :id AND ";

    private final JdbcClient jdbc;

    public PermisosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean esDocenteDelProyecto(long idUsuario, long idProyecto) {
        return existe(
                "SELECT EXISTS (SELECT 1 FROM proyecto WHERE id_proyecto = :id AND id_docente = :usuario)",
                idUsuario,
                idProyecto);
    }

    @Override
    public boolean esDocenteDelCurso(long idUsuario, long idCurso) {
        return existe(
                "SELECT EXISTS (SELECT 1 FROM curso WHERE id_curso = :id AND id_docente = :usuario)",
                idUsuario,
                idCurso);
    }

    @Override
    public boolean puedeVerCurso(long idUsuario, long idCurso) {
        return existe(
                "SELECT EXISTS (SELECT 1 FROM curso c WHERE c.id_curso = :id AND (c.id_docente = :usuario "
                        + "OR EXISTS (SELECT 1 FROM miembro_curso mc WHERE mc.id_curso = c.id_curso "
                        + "AND mc.id_usuario = :usuario)))",
                idUsuario,
                idCurso);
    }

    @Override
    public boolean esMonitorDelProyecto(long idUsuario, long idProyecto) {
        return existe(
                "SELECT EXISTS (SELECT 1 FROM proyecto p WHERE p.id_proyecto = :id AND " + MONITOR_ACTIVO + ")",
                idUsuario,
                idProyecto);
    }

    @Override
    public boolean esIntegranteDelEquipo(long idUsuario, long idEquipo) {
        return existe(
                "SELECT EXISTS (SELECT 1 FROM miembro_equipo WHERE id_equipo = :id AND id_usuario = :usuario)",
                idUsuario,
                idEquipo);
    }

    @Override
    public boolean esLiderDelEquipo(long idUsuario, long idEquipo) {
        return existe(
                "SELECT EXISTS (SELECT 1 FROM miembro_equipo WHERE id_equipo = :id AND id_usuario = :usuario "
                        + "AND rol_equipo = 'LIDER')",
                idUsuario,
                idEquipo);
    }

    @Override
    public boolean puedeVerProyecto(long idUsuario, long idProyecto) {
        return existe(VER_PROYECTO, idUsuario, idProyecto);
    }

    @Override
    public boolean puedeVerEquipo(long idUsuario, long idEquipo) {
        return existe(VER_EQUIPO, idUsuario, idEquipo);
    }

    @Override
    public boolean puedeVerHistorialEquipo(long idUsuario, long idEquipo) {
        return esIntegranteDelEquipo(idUsuario, idEquipo) || existe(DOCENTE_DE_EQUIPO, idUsuario, idEquipo);
    }

    @Override
    public boolean puedeVerTarea(long idUsuario, long idTarea) {
        return equipoDeTarea(idTarea).filter(e -> puedeVerEquipo(idUsuario, e)).isPresent();
    }

    @Override
    public boolean puedeGestionarTarea(long idUsuario, long idTarea) {
        return equipoDeTarea(idTarea)
                .filter(e -> esLiderDelEquipo(idUsuario, e))
                .isPresent();
    }

    @Override
    public boolean puedeActualizarAvance(long idUsuario, long idTarea) {
        return existe(TAREA_DE_MIEMBRO + "(t.id_encargado = :usuario OR m.rol_equipo = 'LIDER'))", idUsuario, idTarea);
    }

    @Override
    public boolean puedeAdjuntarEvidencia(long idUsuario, long idTarea) {
        return puedeActualizarAvance(idUsuario, idTarea);
    }

    @Override
    public boolean puedeSolicitarApoyo(long idUsuario, long idTarea) {
        return existe(TAREA_DE_MIEMBRO + "t.id_encargado = :usuario)", idUsuario, idTarea);
    }

    @Override
    public boolean puedeComentarTarea(long idUsuario, long idTarea) {
        return puedeVerTarea(idUsuario, idTarea);
    }

    @Override
    public boolean puedeVerHitoEquipo(long idUsuario, long idHitoEquipo) {
        return equipoDeHitoEquipo(idHitoEquipo)
                .filter(e -> puedeVerEquipo(idUsuario, e))
                .isPresent();
    }

    @Override
    public boolean puedeGestionarHitoEquipo(long idUsuario, long idHitoEquipo) {
        return equipoDeHitoEquipo(idHitoEquipo)
                .filter(e -> esLiderDelEquipo(idUsuario, e))
                .isPresent();
    }

    @Override
    public boolean puedeEnviarHito(long idUsuario, long idHitoEquipo) {
        return equipoDeHitoEquipo(idHitoEquipo)
                .filter(e -> esIntegranteDelEquipo(idUsuario, e))
                .isPresent();
    }

    @Override
    public boolean puedeEvaluarHito(long idUsuario, long idHitoEquipo) {
        return existe(
                "SELECT EXISTS (SELECT 1 FROM hito_equipo he JOIN hito h ON h.id_hito = he.id_hito "
                        + "JOIN proyecto p ON p.id_proyecto = h.id_proyecto "
                        + "WHERE he.id_hito_equipo = :id AND p.id_docente = :usuario)",
                idUsuario,
                idHitoEquipo);
    }

    private boolean existe(String sql, long idUsuario, long id) {
        return jdbc.sql(sql)
                .param("usuario", idUsuario)
                .param("id", id)
                .query(Boolean.class)
                .single();
    }

    private Optional<Long> equipoDeTarea(long idTarea) {
        return jdbc.sql("SELECT he.id_equipo FROM tarea t JOIN hito_equipo he ON he.id_hito_equipo = t.id_hito_equipo "
                        + "WHERE t.id_tarea = :id")
                .param("id", idTarea)
                .query(Long.class)
                .optional();
    }

    private Optional<Long> equipoDeHitoEquipo(long idHitoEquipo) {
        return jdbc.sql("SELECT id_equipo FROM hito_equipo WHERE id_hito_equipo = :id")
                .param("id", idHitoEquipo)
                .query(Long.class)
                .optional();
    }
}
