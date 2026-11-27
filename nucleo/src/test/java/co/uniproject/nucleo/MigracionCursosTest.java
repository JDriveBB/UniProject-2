package co.uniproject.nucleo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** V11: tablas de cursos, participantes, rondas y ámbito de correo_habilitado por rol. */
class MigracionCursosTest {

    private static JdbcClient base() {
        DataSource ds = PostgresPrueba.baseLimpia("11");
        JdbcClient jdbc = JdbcClient.create(ds);
        jdbc.sql("INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma) VALUES "
                        + "(1, 'Docente', 'docente@u.edu.co', '!', 'DOCENTE')")
                .update();
        jdbc.sql("INSERT INTO proyecto (id_proyecto, id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (10, 1, 'Proyecto', DATE '2026-08-03', DATE '2026-11-27')")
                .update();
        jdbc.sql("INSERT INTO curso (id_curso, id_docente, nombre, periodo) VALUES "
                        + "(20, 1, 'Curso A', '2026-2'), (21, 1, 'Curso B', '2026-2')")
                .update();
        return jdbc;
    }

    @Test
    void ambitoDeLaHabilitacionPorRol() {
        JdbcClient jdbc = base();
        jdbc.sql("INSERT INTO correo_habilitado (email, rol_plataforma, id_habilitado_por) "
                        + "VALUES ('doc@u.edu.co', 'DOCENTE', 1)")
                .update();
        jdbc.sql("INSERT INTO correo_habilitado (id_proyecto, email, rol_plataforma, id_habilitado_por) "
                        + "VALUES (10, 'mon@u.edu.co', 'MONITOR', 1)")
                .update();
        jdbc.sql("INSERT INTO correo_habilitado (id_curso, email, rol_plataforma, id_habilitado_por) "
                        + "VALUES (20, 'est@u.edu.co', 'ESTUDIANTE', 1)")
                .update();

        assertThatThrownBy(() -> jdbc.sql(
                                "INSERT INTO correo_habilitado (id_proyecto, email, rol_plataforma, id_habilitado_por) "
                                        + "VALUES (10, 'mal1@u.edu.co', 'ESTUDIANTE', 1)")
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO correo_habilitado (email, rol_plataforma, id_habilitado_por) "
                                + "VALUES ('mal2@u.edu.co', 'ESTUDIANTE', 1)")
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql(
                                "INSERT INTO correo_habilitado (id_curso, email, rol_plataforma, id_habilitado_por) "
                                        + "VALUES (20, 'mal3@u.edu.co', 'MONITOR', 1)")
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql(
                                "INSERT INTO correo_habilitado (id_curso, email, rol_plataforma, id_habilitado_por) "
                                        + "VALUES (20, 'mal4@u.edu.co', 'DOCENTE', 1)")
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unicidadPorAmbito() {
        JdbcClient jdbc = base();
        jdbc.sql("INSERT INTO correo_habilitado (id_curso, email, rol_plataforma, id_habilitado_por) "
                        + "VALUES (20, 'est@u.edu.co', 'ESTUDIANTE', 1), (21, 'est@u.edu.co', 'ESTUDIANTE', 1)")
                .update();
        assertThatThrownBy(() -> jdbc.sql(
                                "INSERT INTO correo_habilitado (id_curso, email, rol_plataforma, id_habilitado_por) "
                                        + "VALUES (20, 'est@u.edu.co', 'ESTUDIANTE', 1)")
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbc.sql("INSERT INTO correo_habilitado (email, rol_plataforma, id_habilitado_por) "
                        + "VALUES ('doc@u.edu.co', 'DOCENTE', 1)")
                .update();
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO correo_habilitado (email, rol_plataforma, id_habilitado_por) "
                                + "VALUES ('doc@u.edu.co', 'DOCENTE', 1)")
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laInvitacionAEquipoYaNoExiste() {
        JdbcClient jdbc = base();
        int columnas = jdbc.sql("SELECT count(*) FROM information_schema.columns WHERE table_schema = 'public' "
                        + "AND table_name = 'correo_habilitado' AND column_name = 'id_equipo_invitado'")
                .query(Integer.class)
                .single();
        assertThat(columnas).isZero();
    }

    @Test
    void tablasYColumnasNuevas() {
        JdbcClient jdbc = base();
        int tablas = jdbc.sql("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name IN ('curso', 'miembro_curso', 'curso_proyecto', "
                        + "'participante_proyecto', 'ronda_lider', 'voto_lider')")
                .query(Integer.class)
                .single();
        assertThat(tablas).isEqualTo(6);

        assertThat(jdbc.sql("SELECT equipos_mixtos || '|' || horas_ronda FROM proyecto WHERE id_proyecto = 10")
                        .query(String.class)
                        .single())
                .isEqualTo("false|48");

        assertThatThrownBy(() -> jdbc.sql(
                                "INSERT INTO equipo (id_proyecto, nombre_equipo, cupo) VALUES (10, 'Sin Cupo', 0)")
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);

        long idEquipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo, cupo) VALUES (10, 'Con Cupo', 4) "
                        + "RETURNING id_equipo")
                .query(Long.class)
                .single();
        jdbc.sql("INSERT INTO ronda_lider (id_equipo, origen, abre_en, cierra_en) "
                        + "VALUES (:e, 'PROGRAMADA', now(), now() + interval '48 hours')")
                .param("e", idEquipo)
                .update();
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO ronda_lider (id_equipo, origen, abre_en, cierra_en) "
                                + "VALUES (:e, 'DEMANDA', now(), now() + interval '24 hours')")
                        .param("e", idEquipo)
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
