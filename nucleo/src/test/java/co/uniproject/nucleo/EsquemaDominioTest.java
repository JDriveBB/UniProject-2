package co.uniproject.nucleo;

import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** V3 contra PostgreSQL 18.6 real: tablas, restricciones, trigger de auditoría y usuarios previos. */
class EsquemaDominioTest {

    private static final List<String> BASE = List.of(
            "INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma) VALUES "
                    + "(1, 'Docente', 'docente@u.edu.co', '!', 'DOCENTE'), "
                    + "(2, 'Lider', 'lider@u.edu.co', '!', 'ESTUDIANTE'), "
                    + "(3, 'Integrante', 'integrante@u.edu.co', '!', 'ESTUDIANTE')",
            "INSERT INTO proyecto (id_proyecto, id_docente, nombre, fecha_inicio, fecha_fin) "
                    + "VALUES (10, 1, 'Proyecto', DATE '2026-08-03', DATE '2026-11-27')",
            "INSERT INTO equipo (id_equipo, id_proyecto, nombre_equipo) VALUES (20, 10, 'Equipo A')",
            "INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES (20, 2, 'LIDER'), (20, 3, 'INTEGRANTE')",
            "INSERT INTO hito (id_hito, id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) "
                    + "VALUES (30, 10, 'Hito 1', 1, DATE '2026-08-03', DATE '2026-09-04')",
            "INSERT INTO hito_equipo (id_hito_equipo, id_hito, id_equipo) VALUES (40, 30, 20)",
            "INSERT INTO tarea (id_tarea, id_hito_equipo, id_encargado, titulo, horas_estimadas) "
                    + "VALUES (50, 40, 3, 'Tarea 1', 4), (51, 40, 2, 'Tarea 2', 2)");

    private static final String UUID_PRUEBA = "0f8fad5b-d9cb-469f-a165-70867728950e";

    private static JdbcClient base() {
        JdbcClient jdbc = JdbcClient.create(PostgresPrueba.baseLimpia("3"));
        BASE.forEach(sql -> jdbc.sql(sql).update());
        return jdbc;
    }

    private static int contar(JdbcClient jdbc, String sql) {
        return jdbc.sql(sql).query(Integer.class).single();
    }

    @Test
    void creaLasTablasDelDominio() {
        JdbcClient jdbc = JdbcClient.create(PostgresPrueba.baseLimpia("3"));
        int n = contar(jdbc, "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ("
                + "'usuario', 'proyecto', 'monitor_proyecto', 'equipo', 'miembro_equipo', 'hito', 'criterio_rubrica', "
                + "'hito_equipo', 'envio_hito', 'tarea', 'dependencia_tarea', 'registro_avance', 'evidencia', "
                + "'comentario_tarea', 'solicitud_correccion', 'evaluacion_hito', 'calificacion_criterio', "
                + "'nota_individual', 'notificacion', 'log_auditoria', 'correo_habilitado')");
        assertThat(n).isEqualTo(21);
    }

    @Test
    void completaLosUsuariosCreadosAntesDeV3() {
        DataSource ds = PostgresPrueba.baseLimpia("2");
        JdbcClient jdbc = JdbcClient.create(ds);
        jdbc.sql("INSERT INTO usuario (id_usuario, telegram_chat_id) VALUES (5, NULL), (6, 123)").update();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").target("3").load().migrate();
        assertThat(jdbc.sql("SELECT nombre || '|' || email || '|' || password_hash || '|' || rol_plataforma || '|' "
                + "|| autoriza_tratamiento_datos FROM usuario WHERE id_usuario = 5").query(String.class).single())
                .isEqualTo("Usuario 5|usuario5@sin-correo.invalid|!|ESTUDIANTE|false");
        assertThat(jdbc.sql("SELECT telegram_chat_id FROM usuario WHERE id_usuario = 6").query(Long.class).single())
                .isEqualTo(123L);
    }

    @Test
    void logAuditoriaEsSoloInsercion() {
        JdbcClient jdbc = base();
        jdbc.sql("INSERT INTO log_auditoria (id_usuario_accion, entidad_afectada, id_registro_afectado, accion) "
                + "VALUES (2, 'tarea', 50, 'CAMBIAR_ESTADO')").update();
        assertThatThrownBy(() -> jdbc.sql("UPDATE log_auditoria SET accion = 'OTRA'").update())
                .isInstanceOf(DataAccessException.class).hasMessageContaining("solo inserción");
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM log_auditoria").update())
                .isInstanceOf(DataAccessException.class).hasMessageContaining("solo inserción");
        assertThatThrownBy(() -> jdbc.sql("TRUNCATE log_auditoria").update())
                .isInstanceOf(DataAccessException.class).hasMessageContaining("solo inserción");
        assertThat(contar(jdbc, "SELECT count(*) FROM log_auditoria")).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO log_auditoria (entidad_afectada, accion) VALUES ('Tarea', 'CREAR')").update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reglasDeTareaYDependencias() {
        JdbcClient jdbc = base();
        assertThatThrownBy(() -> jdbc.sql("UPDATE tarea SET estado = 'COMPLETADA', porcentaje_avance = 50, "
                + "fecha_completada = now() WHERE id_tarea = 50").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql("UPDATE tarea SET porcentaje_avance = 100 WHERE id_tarea = 50").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.sql("UPDATE tarea SET estado = 'COMPLETADA', porcentaje_avance = 100, fecha_completada = now() WHERE id_tarea = 50").update();
        assertThatThrownBy(() -> jdbc.sql("UPDATE tarea SET bloqueada_por_dependencia = true WHERE id_tarea = 51").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.sql("UPDATE tarea SET bloqueada_por_dependencia = true, fecha_bloqueo = now() WHERE id_tarea = 51").update();
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO dependencia_tarea (id_tarea_predecesora, id_tarea_dependiente) VALUES (50, 50)").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.sql("INSERT INTO dependencia_tarea (id_tarea_predecesora, id_tarea_dependiente) VALUES (50, 51)").update();
        assertThatThrownBy(() -> jdbc.sql("UPDATE tarea SET nivel_riesgo = 'CRITICO' WHERE id_tarea = 51").update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unSoloLiderPorEquipo() {
        JdbcClient jdbc = base();
        jdbc.sql("INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma) "
                + "VALUES (4, 'Otro', 'otro@u.edu.co', '!', 'ESTUDIANTE')").update();
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES (20, 4, 'LIDER')").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario) VALUES (20, 4)").update();
        assertThat(contar(jdbc, "SELECT count(*) FROM miembro_equipo WHERE id_equipo = 20 AND rol_equipo = 'INTEGRANTE'"))
                .isEqualTo(2);
    }

    @Test
    void reglasDeCorreoHabilitadoYUsuario() {
        JdbcClient jdbc = base();
        jdbc.sql("INSERT INTO correo_habilitado (email, rol_plataforma, id_habilitado_por) VALUES ('nuevo@u.edu.co', 'DOCENTE', 1)").update();
        jdbc.sql("INSERT INTO correo_habilitado (id_proyecto, email, rol_plataforma, id_habilitado_por) "
                + "VALUES (10, 'est@u.edu.co', 'ESTUDIANTE', 1)").update();
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO correo_habilitado (email, rol_plataforma, id_habilitado_por) "
                + "VALUES ('nuevo@u.edu.co', 'DOCENTE', 1)").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO correo_habilitado (id_proyecto, email, rol_plataforma, id_habilitado_por) "
                + "VALUES (10, 'd2@u.edu.co', 'DOCENTE', 1)").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO correo_habilitado (email, rol_plataforma, id_habilitado_por) "
                + "VALUES ('e2@u.edu.co', 'ESTUDIANTE', 1)").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO correo_habilitado (id_proyecto, email, rol_plataforma, id_habilitado_por) "
                + "VALUES (10, 'Mayus@u.edu.co', 'ESTUDIANTE', 1)").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma) "
                + "VALUES (101, 'X', 'docente@u.edu.co', '!', 'DOCENTE')").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma, "
                + "autoriza_tratamiento_datos) VALUES (102, 'X', 'x@u.edu.co', '!', 'DOCENTE', true)").update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tablasDeV2QuedanEnlazadas() {
        JdbcClient jdbc = base();
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO estado_riesgo_tarea (tarea_id, en_riesgo, cambiado_en) "
                + "VALUES (999999, true, now())").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.sql("INSERT INTO estado_riesgo_tarea (tarea_id, en_riesgo, cambiado_en) VALUES (50, true, now())").update();
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO alerta_inactividad (proyecto_id, estudiante_id, ultima_alerta_en) "
                + "VALUES (10, 999999, now())").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.sql("INSERT INTO alerta_inactividad (proyecto_id, estudiante_id, ultima_alerta_en) VALUES (10, 3, now())").update();
    }

    @Test
    void formaDeLasEvidencias() {
        JdbcClient jdbc = base();
        jdbc.sql("INSERT INTO evidencia (id_tarea, id_usuario, tipo_evidencia, url) "
                + "VALUES (50, 3, 'ENLACE', 'https://github.com/equipo/repo')").update();
        jdbc.sql("INSERT INTO evidencia (id_tarea, id_usuario, tipo_evidencia, url, nombre_archivo) "
                + "VALUES (50, 3, 'PDF', 'evidencias/" + UUID_PRUEBA + ".pdf', 'informe.pdf')").update();
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO evidencia (id_tarea, id_usuario, tipo_evidencia, url) "
                + "VALUES (50, 3, 'ENLACE', 'ftp://servidor/archivo')").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO evidencia (id_tarea, id_usuario, tipo_evidencia, url, nombre_archivo) "
                + "VALUES (50, 3, 'IMAGEN', 'evidencias/" + UUID_PRUEBA + ".gif', 'foto.gif')").update())
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO evidencia (id_tarea, id_usuario, tipo_evidencia, url) "
                + "VALUES (50, 3, 'PDF', 'evidencias/" + UUID_PRUEBA + ".pdf')").update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
