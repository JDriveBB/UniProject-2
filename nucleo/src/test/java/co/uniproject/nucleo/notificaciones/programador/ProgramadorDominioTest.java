package co.uniproject.nucleo.notificaciones.programador;

import co.uniproject.eventos.datos.EquipoInactividad;
import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.Referencia;
import co.uniproject.eventos.datos.ReferenciaTarea;
import co.uniproject.eventos.datos.TareaRutaCriticaRiesgo;
import co.uniproject.nucleo.PostgresPrueba;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

class ProgramadorDominioTest {

    private static final Instant AHORA = Instant.parse("2026-09-10T15:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneId.of("America/Bogota"));
    private static final Persona DOCENTE = new Persona(1, "Docente D", 900L);
    private static final Persona LIDER = new Persona(2, "Lider L", 902L);
    private static final Persona ESTUDIANTE = new Persona(3, "Est E", null);
    private static final String[] SEMILLA = {
            "INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma, telegram_chat_id) VALUES "
                    + "(1, 'Docente D', 'd@prueba.invalid', '!', 'DOCENTE', 900), "
                    + "(2, 'Lider L', 'l@prueba.invalid', '!', 'ESTUDIANTE', 902), "
                    + "(3, 'Est E', 'e@prueba.invalid', '!', 'ESTUDIANTE', NULL)",
            "INSERT INTO proyecto (id_proyecto, id_docente, nombre, fecha_inicio, fecha_fin) "
                    + "VALUES (10, 1, 'Proyecto P', DATE '2026-08-03', DATE '2026-11-27')",
            "INSERT INTO hito (id_hito, id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite, "
                    + "dias_inactividad_alerta, notificar_docente_inactividad) "
                    + "VALUES (20, 10, 'Hito 1', 1, DATE '2026-08-03', DATE '2026-09-15', 3, true)",
            "INSERT INTO equipo (id_equipo, id_proyecto, nombre_equipo) VALUES (30, 10, 'Equipo 1')",
            "INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo, fecha_ingreso) VALUES "
                    + "(30, 2, 'LIDER', TIMESTAMPTZ '2026-08-03 05:00:00+00'), "
                    + "(30, 3, 'INTEGRANTE', TIMESTAMPTZ '2026-08-03 05:00:00+00')",
            "INSERT INTO hito_equipo (id_hito_equipo, id_hito, id_equipo) VALUES (40, 20, 30)",
            "INSERT INTO tarea (id_tarea, id_hito_equipo, id_encargado, titulo, horas_estimadas) VALUES "
                    + "(50, 40, 2, 'A', 100), (51, 40, 3, 'B', 50), (52, 40, 3, 'C', 10)",
            "INSERT INTO dependencia_tarea (id_tarea_predecesora, id_tarea_dependiente) VALUES (50, 51)"
    };

    private JdbcClient jdbc;
    private EvaluadorRiesgoDominio evaluador;
    private DetectorInactividadDominio detector;

    @BeforeEach
    void setUp() {
        jdbc = JdbcClient.create(PostgresPrueba.baseLimpia("3"));
        Arrays.stream(SEMILLA).forEach(sql -> jdbc.sql(sql).update());
        evaluador = new EvaluadorRiesgoDominio(jdbc, RELOJ);
        detector = new DetectorInactividadDominio(jdbc);
    }

    private void sql(String sentencia) {
        jdbc.sql(sentencia).update();
    }

    private static TareaRutaCriticaRiesgo riesgo(long idTarea, String titulo, Referencia responsable) {
        return new TareaRutaCriticaRiesgo(new Referencia(10, "Proyecto P"), new Referencia(30, "Equipo 1"),
                new ReferenciaTarea(idTarea, titulo), responsable, 0, new Referencia(40, "Hito 1"),
                LocalDate.of(2026, 9, 15), List.of(LIDER, ESTUDIANTE), DOCENTE);
    }

    @Test
    void laCadenaQueNoAlcanzaEstaEnRiesgo() {
        assertThat(evaluador.evaluarTareasRutaCritica()).containsExactly(
                new EvaluacionRiesgo(50, true, riesgo(50, "A", new Referencia(2, "Lider L"))),
                new EvaluacionRiesgo(51, true, riesgo(51, "B", new Referencia(3, "Est E"))),
                new EvaluacionRiesgo(52, false, null));
    }

    @Test
    void sinEncargadoNoSeAvisaTodavia() {
        sql("UPDATE tarea SET id_encargado = NULL WHERE id_tarea = 50");
        assertThat(evaluador.evaluarTareasRutaCritica()).extracting(EvaluacionRiesgo::enRiesgo)
                .containsExactly(false, true, false);
    }

    @Test
    void conAvanceSuficienteNoHayRiesgo() {
        sql("UPDATE tarea SET porcentaje_avance = 50, estado = 'EN_PROGRESO' WHERE id_tarea = 50");
        assertThat(evaluador.evaluarTareasRutaCritica()).extracting(EvaluacionRiesgo::enRiesgo)
                .containsExactly(false, false, false);
    }

    @Test
    void completadasHitosEnviadosYNoIniciadosNoSeEvaluan() {
        sql("UPDATE tarea SET porcentaje_avance = 100, estado = 'COMPLETADA', fecha_completada = now() "
                + "WHERE id_tarea = 52");
        assertThat(evaluador.evaluarTareasRutaCritica()).extracting(EvaluacionRiesgo::tareaId)
                .containsExactly(50L, 51L);
        EvaluadorRiesgoDominio antesDelHito = new EvaluadorRiesgoDominio(jdbc,
                Clock.fixed(Instant.parse("2026-08-01T15:00:00Z"), ZoneId.of("America/Bogota")));
        assertThat(antesDelHito.evaluarTareasRutaCritica()).isEmpty();
        sql("UPDATE hito_equipo SET estado = 'ENVIADO' WHERE id_hito_equipo = 40");
        assertThat(evaluador.evaluarTareasRutaCritica()).isEmpty();
    }

    @Test
    void estudianteSinActividadEsInactivo() {
        sql("INSERT INTO registro_avance (id_tarea, id_usuario, fecha_hora, porcentaje_avance) "
                + "VALUES (50, 2, TIMESTAMPTZ '2026-09-09 12:00:00+00', 10)");
        Instant ultima = Instant.parse("2026-08-03T05:00:00Z");
        assertThat(detector.buscarInactivos(AHORA)).containsExactly(new CasoInactividad(10, 3, ultima,
                new EquipoInactividad(new Referencia(10, "Proyecto P"), new Referencia(30, "Equipo 1"),
                        new Referencia(3, "Est E"), ultima, 3, LIDER, DOCENTE, true)));
    }

    @Test
    void unaEvidenciaCuentaComoActividad() {
        sql("INSERT INTO registro_avance (id_tarea, id_usuario, fecha_hora, porcentaje_avance) "
                + "VALUES (50, 2, TIMESTAMPTZ '2026-09-09 12:00:00+00', 10)");
        sql("INSERT INTO evidencia (id_tarea, id_usuario, tipo_evidencia, url, fecha_subida) "
                + "VALUES (51, 3, 'ENLACE', 'https://ejemplo.invalid/avance', TIMESTAMPTZ '2026-09-09 00:00:00+00')");
        assertThat(detector.buscarInactivos(AHORA)).isEmpty();
    }

    @Test
    void sinTareasPendientesNoSeAvisa() {
        sql("UPDATE tarea SET porcentaje_avance = 100, estado = 'COMPLETADA', fecha_completada = now()");
        assertThat(detector.buscarInactivos(AHORA)).isEmpty();
    }

    @Test
    void sinLiderNoHayAQuienAvisar() {
        sql("UPDATE miembro_equipo SET rol_equipo = 'INTEGRANTE' WHERE id_usuario = 2");
        assertThat(detector.buscarInactivos(AHORA)).isEmpty();
    }

    @Test
    void fueraDelHitoNoSeRevisa() {
        assertThat(detector.buscarInactivos(Instant.parse("2026-09-20T15:00:00Z"))).isEmpty();
        assertThat(detector.buscarInactivos(Instant.parse("2026-08-01T15:00:00Z"))).isEmpty();
    }
}
