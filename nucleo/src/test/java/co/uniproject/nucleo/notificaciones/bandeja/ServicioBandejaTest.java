package co.uniproject.nucleo.notificaciones.bandeja;

import co.uniproject.nucleo.PostgresPrueba;
import co.uniproject.nucleo.comun.error.AccesoDenegadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicioBandejaTest {

    private ServicioBandeja servicio;
    private JdbcClient jdbc;

    @BeforeEach
    void setUp() {
        jdbc = JdbcClient.create(PostgresPrueba.baseLimpia("3"));
        servicio = new ServicioBandeja(jdbc);
        jdbc.sql("INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma) VALUES "
                + "(1, 'Uno', 'uno@prueba.invalid', '!', 'ESTUDIANTE'), "
                + "(2, 'Dos', 'dos@prueba.invalid', '!', 'DOCENTE')").update();
        jdbc.sql("INSERT INTO notificacion (id_notificacion, id_destinatario, tipo, mensaje, entidad_referencia, "
                + "id_referencia, fecha_creacion, estado_envio) VALUES "
                + "(11, 1, 'tarea.correccion', 'm11', 'tarea', 45, TIMESTAMPTZ '2026-10-24 10:00:00+00', 'EN_COLA'), "
                + "(12, 1, 'hito.evaluado', 'm12', 'hito_equipo', 7, TIMESTAMPTZ '2026-10-24 11:00:00+00', 'SIN_TELEGRAM'), "
                + "(13, 1, 'equipo.inactividad', 'm13', 'equipo', 4, TIMESTAMPTZ '2026-10-24 12:00:00+00', 'EN_COLA'), "
                + "(21, 2, 'hito.enviado', 'm21', 'hito_equipo', 7, TIMESTAMPTZ '2026-10-24 12:00:00+00', 'EN_COLA')")
                .update();
    }

    @Test
    void cadaUnoVeLasSuyasDeLaMasReciente() {
        Bandeja b = servicio.bandeja(1);
        assertThat(b.noLeidas()).isEqualTo(3);
        assertThat(b.notificaciones()).extracting(NotificacionVista::idNotificacion).containsExactly(13L, 12L, 11L);
        assertThat(b.notificaciones()).extracting(NotificacionVista::enlace)
                .containsExactly("/equipos/4/tablero", "/hitos/7/evaluacion", "/tareas/45");
        assertThat(servicio.bandeja(2).notificaciones()).extracting(NotificacionVista::enlace)
                .containsExactly("/hitos/7/evaluar");
    }

    @Test
    void marcarLeidaSoloLaPropia() {
        servicio.marcarLeida(1, 12);
        assertThat(servicio.bandeja(1).noLeidas()).isEqualTo(2);
        assertThatThrownBy(() -> servicio.marcarLeida(1, 21)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> servicio.marcarLeida(1, 999)).isInstanceOf(AccesoDenegadoException.class);
        assertThat(servicio.bandeja(2).noLeidas()).isEqualTo(1);
    }

    @Test
    void marcarTodas() {
        assertThat(servicio.marcarTodasLeidas(1)).isEqualTo(3);
        assertThat(servicio.marcarTodasLeidas(1)).isZero();
        assertThat(servicio.bandeja(1).noLeidas()).isZero();
        assertThat(servicio.bandeja(2).noLeidas()).isEqualTo(1);
    }

    @Test
    void reasignadaLlevaAlNuevoALaTareaYAlAnteriorAMisTareas() {
        jdbc.sql("INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma) VALUES "
                + "(3, 'Tres', 'tres@prueba.invalid', '!', 'ESTUDIANTE')").update();
        jdbc.sql("INSERT INTO proyecto (id_proyecto, id_docente, nombre, fecha_inicio, fecha_fin) VALUES "
                + "(1, 2, 'P', DATE '2026-08-01', DATE '2026-12-01')").update();
        jdbc.sql("INSERT INTO hito (id_hito, id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) VALUES "
                + "(1, 1, 'H', 1, DATE '2026-08-01', DATE '2026-11-01')").update();
        jdbc.sql("INSERT INTO equipo (id_equipo, id_proyecto, nombre_equipo) VALUES (1, 1, 'E')").update();
        jdbc.sql("INSERT INTO hito_equipo (id_hito_equipo, id_hito, id_equipo) VALUES (1, 1, 1)").update();
        jdbc.sql("INSERT INTO tarea (id_tarea, id_hito_equipo, id_encargado, titulo, horas_estimadas) VALUES "
                + "(77, 1, 3, 'T', 2)").update();
        jdbc.sql("INSERT INTO notificacion (id_notificacion, id_destinatario, tipo, mensaje, entidad_referencia, "
                + "id_referencia, fecha_creacion, estado_envio) VALUES "
                + "(31, 3, 'tarea.reasignada', 'nuevo', 'tarea', 77, TIMESTAMPTZ '2026-10-25 10:00:00+00', 'EN_COLA'), "
                + "(32, 1, 'tarea.reasignada', 'anterior', 'tarea', 77, TIMESTAMPTZ '2026-10-25 10:00:00+00', 'EN_COLA')")
                .update();

        assertThat(servicio.bandeja(3).notificaciones()).extracting(NotificacionVista::enlace)
                .containsExactly("/tareas/77");
        assertThat(servicio.bandeja(1).notificaciones()).extracting(NotificacionVista::enlace)
                .contains("/mis-tareas");
        assertThat(servicio.bandeja(1).notificaciones().stream()
                .filter(n -> n.idNotificacion() == 32).findFirst().orElseThrow().enlace()).isEqualTo("/mis-tareas");
    }
}
