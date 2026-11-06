package co.uniproject.nucleo;

import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Bloque 29 (decisión 29-5): V6 saca del log el texto libre que ya había y deja activo el trigger de solo inserción. */
class MigracionTextoLibreTest {

    /** Datos "viejos" en V5: apoyos y textos dentro del log, incluida una cuenta ya anonimizada (9104). */
    private static final List<String> DATOS_V5 = List.of(
            "INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma) VALUES "
                    + "(9101, 'Ana Lider', 'ana.v6@u.edu.co', '!', 'ESTUDIANTE'), "
                    + "(9102, 'Beto Encargado', 'beto.v6@u.edu.co', '!', 'ESTUDIANTE'), "
                    + "(9103, 'Docente Seis', 'doc.v6@u.edu.co', '!', 'DOCENTE')",
            "INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma, fecha_anonimizacion) "
                    + "VALUES (9104, 'Usuario anonimizado', 'anonimo-9104@anonimo.invalid', '!', 'ESTUDIANTE', now())",
            "INSERT INTO proyecto (id_proyecto, id_docente, nombre, fecha_inicio, fecha_fin) "
                    + "VALUES (9101, 9103, 'Proyecto Seis', DATE '2026-08-03', DATE '2026-12-04')",
            "INSERT INTO hito (id_hito, id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) "
                    + "VALUES (9101, 9101, 'Hito Seis', 1, DATE '2026-08-03', DATE '2026-09-30')",
            "INSERT INTO equipo (id_equipo, id_proyecto, nombre_equipo) VALUES (9101, 9101, 'Equipo Seis')",
            "INSERT INTO hito_equipo (id_hito_equipo, id_hito, id_equipo) VALUES (9101, 9101, 9101)",
            "INSERT INTO tarea (id_tarea, id_hito_equipo, id_encargado, titulo, horas_estimadas) "
                    + "VALUES (9101, 9101, 9102, 'Tarea Seis', 4)",
            "INSERT INTO log_auditoria (id_log, id_usuario_accion, entidad_afectada, id_registro_afectado, accion, "
                    + "campo_modificado, valor_anterior, valor_nuevo) VALUES "
                    + "(9001, 9102, 'tarea', 9101, 'SOLICITAR_APOYO', 'mensaje', NULL, 'Estoy en tratamiento medico'), "
                    + "(9002, 9104, 'tarea', 9101, 'SOLICITAR_APOYO', 'mensaje', NULL, 'Texto de una cuenta suprimida'), "
                    + "(9003, 9101, 'tarea', 9101, 'REASIGNAR_TAREA', 'motivo', NULL, 'Beto esta incapacitado'), "
                    + "(9004, 9101, 'tarea', 9101, 'ACTUALIZAR', 'titulo', 'Tarea vieja', 'Tarea Seis'), "
                    + "(9005, 9103, 'proyecto', 9101, 'ACTUALIZAR', 'descripcion', NULL, 'Curso de prueba'), "
                    + "(9006, 9101, 'tarea', 9101, 'ACTUALIZAR', 'horas_estimadas', '3', '4'), "
                    + "(9007, 9101, 'tarea', 9101, 'REASIGNAR_TAREA', 'id_encargado', '9101', '9102'), "
                    + "(9008, 9104, 'tarea', 9101, 'ACTUALIZAR', 'descripcion', NULL, 'Texto de otra cuenta suprimida')");

    @Test
    void migraLosTextosLibresYDejaElLogSoloConHechos() {
        DataSource ds = PostgresPrueba.baseLimpia("5");
        JdbcClient jdbc = JdbcClient.create(ds);
        DATOS_V5.forEach(sql -> jdbc.sql(sql).update());

        Flyway.configure().dataSource(ds).locations("classpath:db/migration").target("6").load().migrate();

        assertThat(jdbc.sql("SELECT id_solicitante || '|' || coalesce(mensaje, '-') FROM solicitud_apoyo "
                        + "ORDER BY id_solicitante").query(String.class).list())
                .containsExactly("9102|Estoy en tratamiento medico", "9104|-");
        assertThat(jdbc.sql("SELECT l.id_log || '|' || l.campo_modificado || '|' || s.id_solicitante "
                        + "FROM log_auditoria l JOIN solicitud_apoyo s "
                        + "ON CAST(s.id_solicitud_apoyo AS varchar) = l.valor_nuevo "
                        + "WHERE l.accion = 'SOLICITAR_APOYO' ORDER BY l.id_log").query(String.class).list())
                .containsExactly("9001|solicitud_apoyo|9102", "9002|solicitud_apoyo|9104");
        assertThat(jdbc.sql("SELECT id_log || '|' || coalesce(valor_anterior, '-') || '|' || coalesce(valor_nuevo, '-') "
                        + "FROM texto_auditoria ORDER BY id_log").query(String.class).list())
                .containsExactly("9003|-|Beto esta incapacitado", "9004|Tarea vieja|Tarea Seis",
                        "9005|-|Curso de prueba");
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE id_log IN (9003, 9004, 9005, 9008) "
                        + "AND campo_modificado IS NOT NULL AND valor_anterior IS NULL AND valor_nuevo IS NULL")
                .query(Integer.class).single()).isEqualTo(4);
        assertThat(jdbc.sql("SELECT valor_anterior || '>' || valor_nuevo FROM log_auditoria "
                        + "WHERE id_log IN (9006, 9007) ORDER BY id_log").query(String.class).list())
                .containsExactly("3>4", "9101>9102");
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria "
                        + "WHERE coalesce(valor_anterior, '') || coalesce(valor_nuevo, '') ~ '[A-Za-z]'")
                .query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM information_schema.columns WHERE table_name = 'solicitud_apoyo' "
                        + "AND column_name = 'id_log_origen'").query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT tgenabled::text FROM pg_trigger WHERE tgname = 'log_auditoria_sin_cambios'")
                .query(String.class).single()).isEqualTo("O");
        assertThatThrownBy(() -> jdbc.sql("UPDATE log_auditoria SET valor_nuevo = 'x' WHERE id_log = 9006").update())
                .isInstanceOf(DataAccessException.class).hasMessageContaining("solo inserción");
    }
}
