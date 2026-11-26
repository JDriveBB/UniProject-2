package co.uniproject.nucleo.seguridad.permisos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.uniproject.nucleo.PostgresPrueba;
import co.uniproject.nucleo.comun.error.AccesoDenegadoException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.simple.JdbcClient;

class PermisosJdbcTest {

    private static final List<String> DATOS = List.of(
            "INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma) VALUES "
                    + "(1, 'Docente', 'docente@u.edu.co', '!', 'DOCENTE'), "
                    + "(2, 'Monitor', 'monitor@u.edu.co', '!', 'MONITOR'), "
                    + "(3, 'Lider', 'lider@u.edu.co', '!', 'ESTUDIANTE'), "
                    + "(4, 'Integrante', 'integrante@u.edu.co', '!', 'ESTUDIANTE'), "
                    + "(5, 'Otro equipo', 'otro@u.edu.co', '!', 'ESTUDIANTE'), "
                    + "(6, 'Admin', 'admin@u.edu.co', '!', 'ADMINISTRADOR'), "
                    + "(7, 'Otro docente', 'docente2@u.edu.co', '!', 'DOCENTE'), "
                    + "(8, 'Participante', 'participante@u.edu.co', '!', 'ESTUDIANTE')",
            "INSERT INTO proyecto (id_proyecto, id_docente, nombre, fecha_inicio, fecha_fin, usa_monitores) VALUES "
                    + "(10, 1, 'Con monitores', DATE '2026-08-03', DATE '2026-11-27', true), "
                    + "(11, 7, 'Sin monitores', DATE '2026-08-03', DATE '2026-11-27', false)",
            "INSERT INTO monitor_proyecto (id_proyecto, id_monitor) VALUES (10, 2), (11, 2)",
            "INSERT INTO equipo (id_equipo, id_proyecto, nombre_equipo) VALUES (20, 10, 'A'), (21, 10, 'B'), (22, 11, 'C')",
            "INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES "
                    + "(20, 3, 'LIDER'), (20, 4, 'INTEGRANTE'), (21, 5, 'LIDER')",
            "INSERT INTO hito (id_hito, id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) VALUES "
                    + "(30, 10, 'Hito 1', 1, DATE '2026-08-03', DATE '2026-09-04'), "
                    + "(31, 11, 'Hito 1', 1, DATE '2026-08-03', DATE '2026-09-04')",
            "INSERT INTO hito_equipo (id_hito_equipo, id_hito, id_equipo) VALUES (40, 30, 20), (41, 30, 21), (42, 31, 22)",
            "INSERT INTO tarea (id_tarea, id_hito_equipo, id_encargado, titulo, horas_estimadas) VALUES "
                    + "(50, 40, 4, 'T50', 2), (51, 40, 3, 'T51', 2), (52, 40, NULL, 'T52', 2), "
                    + "(53, 41, 5, 'T53', 2), (54, 40, 5, 'T54', 2)",
            "INSERT INTO curso (id_curso, id_docente, nombre, periodo) VALUES "
                    + "(60, 1, 'Curso 1', '2026-2'), (61, 7, 'Curso 2', '2026-2')",
            "INSERT INTO miembro_curso (id_curso, id_usuario) VALUES (60, 4), (60, 8)",
            "INSERT INTO curso_proyecto (id_curso, id_proyecto) VALUES (60, 10)",
            "INSERT INTO participante_proyecto (id_proyecto, id_usuario, id_curso) VALUES (10, 8, 60)");

    private static PermisosJdbc permisos;

    @BeforeAll
    static void preparar() {
        JdbcClient jdbc = JdbcClient.create(PostgresPrueba.baseLimpia("11"));
        DATOS.forEach(sql -> jdbc.sql(sql).update());
        permisos = new PermisosJdbc(jdbc);
    }

    @ParameterizedTest(name = "{0}({1}, {2}) = {3}")
    @CsvSource({
        "esDocenteDelProyecto, 1, 10, true",
        "esDocenteDelProyecto, 7, 10, false",
        "esDocenteDelProyecto, 7, 11, true",
        "esDocenteDelProyecto, 1, 999, false",
        "esDocenteDelCurso, 1, 60, true",
        "esDocenteDelCurso, 7, 60, false",
        "esDocenteDelCurso, 4, 60, false",
        "esDocenteDelCurso, 1, 999, false",
        "puedeVerCurso, 1, 60, true",
        "puedeVerCurso, 4, 60, true",
        "puedeVerCurso, 5, 60, false",
        "puedeVerCurso, 7, 60, false",
        "puedeVerCurso, 1, 999, false",
        "esMonitorDelProyecto, 2, 10, true",
        "esMonitorDelProyecto, 2, 11, false",
        "esMonitorDelProyecto, 3, 10, false",
        "esParticipanteDelProyecto, 8, 10, true",
        "esParticipanteDelProyecto, 8, 11, false",
        "esParticipanteDelProyecto, 4, 10, false",
        "esParticipanteDelProyecto, 1, 999, false",
        "esIntegranteDelEquipo, 3, 20, true",
        "esIntegranteDelEquipo, 4, 20, true",
        "esIntegranteDelEquipo, 5, 20, false",
        "esIntegranteDelEquipo, 1, 20, false",
        "esLiderDelEquipo, 3, 20, true",
        "esLiderDelEquipo, 4, 20, false",
        "esLiderDelEquipo, 5, 21, true",
        "puedeVerProyecto, 1, 10, true",
        "puedeVerProyecto, 2, 10, true",
        "puedeVerProyecto, 4, 10, true",
        "puedeVerProyecto, 5, 10, true",
        "puedeVerProyecto, 8, 10, true",
        "puedeVerProyecto, 8, 11, false",
        "puedeVerProyecto, 2, 11, false",
        "puedeVerProyecto, 6, 10, false",
        "puedeVerProyecto, 7, 10, false",
        "puedeVerProyecto, 1, 999, false",
        "puedeVerEquipo, 1, 20, true",
        "puedeVerEquipo, 2, 20, true",
        "puedeVerEquipo, 4, 20, true",
        "puedeVerEquipo, 5, 20, false",
        "puedeVerEquipo, 8, 20, false",
        "puedeVerEquipo, 6, 20, false",
        "puedeVerEquipo, 7, 20, false",
        "puedeVerEquipo, 2, 22, false",
        "puedeVerEquipo, 7, 22, true",
        "puedeVerEquipo, 1, 999, false",
        "puedeVerHistorialEquipo, 1, 20, true",
        "puedeVerHistorialEquipo, 2, 20, false",
        "puedeVerHistorialEquipo, 3, 20, true",
        "puedeVerHistorialEquipo, 4, 20, true",
        "puedeVerHistorialEquipo, 5, 20, false",
        "puedeVerHistorialEquipo, 6, 20, false",
        "puedeVerHistorialEquipo, 7, 20, false",
        "puedeVerHistorialEquipo, 7, 22, true",
        "puedeVerHistorialEquipo, 1, 999, false",
        "puedeVerTarea, 4, 50, true",
        "puedeVerTarea, 2, 50, true",
        "puedeVerTarea, 1, 53, true",
        "puedeVerTarea, 5, 53, true",
        "puedeVerTarea, 5, 50, false",
        "puedeVerTarea, 5, 54, false",
        "puedeVerTarea, 6, 50, false",
        "puedeVerTarea, 1, 999, false",
        "puedeGestionarTarea, 3, 50, true",
        "puedeGestionarTarea, 3, 52, true",
        "puedeGestionarTarea, 4, 50, false",
        "puedeGestionarTarea, 1, 50, false",
        "puedeGestionarTarea, 2, 50, false",
        "puedeGestionarTarea, 3, 53, false",
        "puedeGestionarTarea, 5, 53, true",
        "puedeActualizarAvance, 4, 50, true",
        "puedeActualizarAvance, 3, 50, true",
        "puedeActualizarAvance, 4, 51, false",
        "puedeActualizarAvance, 4, 52, false",
        "puedeActualizarAvance, 3, 52, true",
        "puedeActualizarAvance, 5, 54, false",
        "puedeActualizarAvance, 2, 50, false",
        "puedeActualizarAvance, 1, 50, false",
        "puedeActualizarAvance, 6, 50, false",
        "puedeActualizarAvance, 4, 999, false",
        "puedeAdjuntarEvidencia, 4, 50, true",
        "puedeAdjuntarEvidencia, 5, 50, false",
        "puedeAdjuntarEvidencia, 2, 50, false",
        "puedeSolicitarApoyo, 4, 50, true",
        "puedeSolicitarApoyo, 3, 51, true",
        "puedeSolicitarApoyo, 3, 50, false",
        "puedeSolicitarApoyo, 5, 54, false",
        "puedeComentarTarea, 2, 50, true",
        "puedeComentarTarea, 1, 50, true",
        "puedeComentarTarea, 4, 51, true",
        "puedeComentarTarea, 5, 50, false",
        "puedeComentarTarea, 6, 50, false",
        "puedeVerHitoEquipo, 4, 40, true",
        "puedeVerHitoEquipo, 4, 41, false",
        "puedeVerHitoEquipo, 1, 41, true",
        "puedeVerHitoEquipo, 2, 41, true",
        "puedeVerHitoEquipo, 7, 42, true",
        "puedeVerHitoEquipo, 2, 42, false",
        "puedeVerHitoEquipo, 4, 999, false",
        "puedeGestionarHitoEquipo, 3, 40, true",
        "puedeGestionarHitoEquipo, 4, 40, false",
        "puedeGestionarHitoEquipo, 1, 40, false",
        "puedeGestionarHitoEquipo, 5, 41, true",
        "puedeEnviarHito, 4, 40, true",
        "puedeEnviarHito, 3, 40, true",
        "puedeEnviarHito, 5, 40, false",
        "puedeEnviarHito, 1, 40, false",
        "puedeEnviarHito, 2, 40, false",
        "puedeEvaluarHito, 1, 40, true",
        "puedeEvaluarHito, 1, 41, true",
        "puedeEvaluarHito, 7, 40, false",
        "puedeEvaluarHito, 7, 42, true",
        "puedeEvaluarHito, 3, 40, false",
        "puedeEvaluarHito, 1, 999, false"
    })
    void matriz(String metodo, long idUsuario, long id, boolean esperado) {
        assertThat(consultar(metodo, idUsuario, id)).isEqualTo(esperado);
    }

    @Test
    void exigirLanzaAccesoDenegado() {
        assertThatCode(() -> permisos.exigir(true)).doesNotThrowAnyException();
        assertThatThrownBy(() -> permisos.exigir(false)).isInstanceOf(AccesoDenegadoException.class);
    }

    private static boolean consultar(String metodo, long u, long id) {
        return switch (metodo) {
            case "esDocenteDelProyecto" -> permisos.esDocenteDelProyecto(u, id);
            case "esDocenteDelCurso" -> permisos.esDocenteDelCurso(u, id);
            case "puedeVerCurso" -> permisos.puedeVerCurso(u, id);
            case "esMonitorDelProyecto" -> permisos.esMonitorDelProyecto(u, id);
            case "esParticipanteDelProyecto" -> permisos.esParticipanteDelProyecto(u, id);
            case "esIntegranteDelEquipo" -> permisos.esIntegranteDelEquipo(u, id);
            case "esLiderDelEquipo" -> permisos.esLiderDelEquipo(u, id);
            case "puedeVerProyecto" -> permisos.puedeVerProyecto(u, id);
            case "puedeVerEquipo" -> permisos.puedeVerEquipo(u, id);
            case "puedeVerHistorialEquipo" -> permisos.puedeVerHistorialEquipo(u, id);
            case "puedeVerTarea" -> permisos.puedeVerTarea(u, id);
            case "puedeGestionarTarea" -> permisos.puedeGestionarTarea(u, id);
            case "puedeActualizarAvance" -> permisos.puedeActualizarAvance(u, id);
            case "puedeAdjuntarEvidencia" -> permisos.puedeAdjuntarEvidencia(u, id);
            case "puedeSolicitarApoyo" -> permisos.puedeSolicitarApoyo(u, id);
            case "puedeComentarTarea" -> permisos.puedeComentarTarea(u, id);
            case "puedeVerHitoEquipo" -> permisos.puedeVerHitoEquipo(u, id);
            case "puedeGestionarHitoEquipo" -> permisos.puedeGestionarHitoEquipo(u, id);
            case "puedeEnviarHito" -> permisos.puedeEnviarHito(u, id);
            case "puedeEvaluarHito" -> permisos.puedeEvaluarHito(u, id);
            default -> throw new IllegalArgumentException(metodo);
        };
    }
}
