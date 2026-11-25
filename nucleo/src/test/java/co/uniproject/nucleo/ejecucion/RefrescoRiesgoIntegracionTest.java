package co.uniproject.nucleo.ejecucion;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import co.uniproject.nucleo.ejecucion.repositorio.DependenciaTareaRepositorio;
import co.uniproject.nucleo.ejecucion.repositorio.TareaRepositorio;
import co.uniproject.nucleo.ejecucion.servicio.ConsultasEjecucion;
import co.uniproject.nucleo.ejecucion.servicio.RecalculoTareas;
import co.uniproject.nucleo.ejecucion.servicio.RefrescoRiesgo;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * T-019: el riesgo guardado en las tareas se recalcula con el paso del tiempo (cada hora) para los hitos en curso ya
 * iniciados, tomando primero el candado del hito y sin tocar avance ni estado.
 */
class RefrescoRiesgoIntegracionTest extends ApiIntegracionBase {

    @Autowired
    private DataSource fuente;

    @Autowired
    private TareaRepositorio tareas;

    @Autowired
    private DependenciaTareaRepositorio dependencias;

    @Autowired
    private ConsultasEjecucion consultas;

    @Autowired
    private TransactionTemplate tx;

    private static final LocalDate HOY = LocalDate.now(FechasDominio.ZONA);
    /** Fecha límite de los hitos que deben quedar en riesgo. */
    private static final LocalDate LIMITE = HOY.plusDays(20);
    /** Reloj adelantado al inicio del último día del hito: quedan 24 h de calendario = 4 h de trabajo. */
    private static final Clock RELOJ =
            Clock.fixed(FechasDominio.venceEn(LIMITE).minus(Duration.ofDays(1)), FechasDominio.ZONA);

    private RefrescoRiesgo refresco() {
        return new RefrescoRiesgo(jdbc, new RecalculoTareas(tareas, dependencias, consultas, RELOJ), tx, RELOJ);
    }

    private long hitoEquipo(String estado, LocalDate inicio, LocalDate limite) {
        long docente = crearUsuario("Docente Refresco", correoUnico("doc"), "DOCENTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Refresco', :i, :f) RETURNING id_proyecto")
                .param("d", docente)
                .param("i", HOY.minusDays(30))
                .param("f", HOY.plusDays(90))
                .query(Long.class)
                .single();
        long hito = jdbc.sql("INSERT INTO hito (id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) "
                        + "VALUES (:p, 'Hito 1', 1, :i, :f) RETURNING id_hito")
                .param("p", proyecto)
                .param("i", inicio)
                .param("f", limite)
                .query(Long.class)
                .single();
        long equipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo) VALUES (:p, 'Equipo R') "
                        + "RETURNING id_equipo")
                .param("p", proyecto)
                .query(Long.class)
                .single();
        return jdbc.sql("INSERT INTO hito_equipo (id_hito, id_equipo, estado) VALUES (:h, :e, :s) "
                        + "RETURNING id_hito_equipo")
                .param("h", hito)
                .param("e", equipo)
                .param("s", estado)
                .query(Long.class)
                .single();
    }

    /** Tarea con un cálculo viejo guardado: riesgo BAJO y holgura de 100 h. */
    private long tarea(long idHitoEquipo, String horas, int avance) {
        return jdbc.sql("INSERT INTO tarea (id_hito_equipo, titulo, horas_estimadas, porcentaje_avance, estado, "
                        + "nivel_riesgo, holgura_horas) VALUES (:he, 'Tarea', :h, :a, :s, 'BAJO', 100) "
                        + "RETURNING id_tarea")
                .param("he", idHitoEquipo)
                .param("h", new BigDecimal(horas))
                .param("a", avance)
                .param("s", avance == 0 ? "PENDIENTE" : "EN_PROGRESO")
                .query(Long.class)
                .single();
    }

    private void depende(long predecesora, long dependiente) {
        jdbc.sql("INSERT INTO dependencia_tarea (id_tarea_predecesora, id_tarea_dependiente) VALUES (:a, :b)")
                .param("a", predecesora)
                .param("b", dependiente)
                .update();
    }

    private String nivel(long tarea) {
        return jdbc.sql("SELECT nivel_riesgo FROM tarea WHERE id_tarea = :t")
                .param("t", tarea)
                .query(String.class)
                .single();
    }

    private String avanceYEstado(long tarea) {
        return jdbc.sql("SELECT porcentaje_avance || ' ' || estado FROM tarea WHERE id_tarea = :t")
                .param("t", tarea)
                .query(String.class)
                .single();
    }

    @Test
    void cadenaCercaDelLimiteQuedaEnRiesgoAltoSinCambiarAvanceNiEstado() {
        long he = hitoEquipo("EN_CURSO", HOY.minusDays(10), LIMITE);
        long a = tarea(he, "3", 20);
        long b = tarea(he, "3", 0);
        depende(a, b);

        int n = refresco().refrescarEnCurso();

        assertThat(n).isGreaterThanOrEqualTo(1);
        assertThat(nivel(a)).isEqualTo("ALTO");
        assertThat(nivel(b)).isEqualTo("ALTO");
        assertThat(jdbc.sql("SELECT holgura_horas FROM tarea WHERE id_tarea = :t")
                        .param("t", b)
                        .query(BigDecimal.class)
                        .single())
                .isNegative();
        assertThat(jdbc.sql("SELECT en_ruta_critica FROM tarea WHERE id_tarea = :t")
                        .param("t", b)
                        .query(Boolean.class)
                        .single())
                .isTrue();
        assertThat(avanceYEstado(a)).isEqualTo("20 EN_PROGRESO");
        assertThat(avanceYEstado(b)).isEqualTo("0 PENDIENTE");
    }

    @Test
    void hitosEnviadosEvaluadosOFuturosNoSeTocan() {
        long enviado = tarea(hitoEquipo("ENVIADO", HOY.minusDays(10), LIMITE), "30", 0);
        long evaluado = tarea(hitoEquipo("EVALUADO", HOY.minusDays(10), LIMITE), "30", 0);
        LocalDate diaDelReloj = FechasDominio.hoy(RELOJ.instant());
        long futuro = tarea(hitoEquipo("EN_CURSO", diaDelReloj.plusDays(1), diaDelReloj.plusDays(1)), "30", 0);

        refresco().refrescarEnCurso();

        assertThat(nivel(enviado)).isEqualTo("BAJO");
        assertThat(nivel(evaluado)).isEqualTo("BAJO");
        assertThat(nivel(futuro)).isEqualTo("BAJO");
        assertThat(jdbc.sql("SELECT holgura_horas FROM tarea WHERE id_tarea IN (:a, :b, :c) GROUP BY holgura_horas")
                        .param("a", enviado)
                        .param("b", evaluado)
                        .param("c", futuro)
                        .query(BigDecimal.class)
                        .list())
                .singleElement()
                .satisfies(h -> assertThat(h).isEqualByComparingTo("100"));
    }

    @Test
    void esperaElCandadoDelHitoAntesDeRecalcular() throws Exception {
        long he = hitoEquipo("EN_CURSO", HOY.minusDays(10), LIMITE);
        long t = tarea(he, "30", 0);
        try (Connection otra = fuente.getConnection()) {
            otra.setAutoCommit(false);
            try (PreparedStatement ps =
                    otra.prepareStatement("SELECT 1 FROM hito_equipo WHERE id_hito_equipo = ? FOR UPDATE")) {
                ps.setLong(1, he);
                ps.executeQuery();
            }
            CompletableFuture<Integer> refrescando =
                    CompletableFuture.supplyAsync(() -> refresco().refrescarEnCurso());
            esperarUnaEspera();
            assertThat(nivel(t)).isEqualTo("BAJO");
            otra.commit();
            refrescando.get(30, TimeUnit.SECONDS);
        }
        assertThat(nivel(t)).isEqualTo("ALTO");
    }

    @Test
    void hitoEntregadoMientrasEsperabaElCandadoNoSeRecalcula() throws Exception {
        long he = hitoEquipo("EN_CURSO", HOY.minusDays(10), LIMITE);
        long t = tarea(he, "30", 0);
        try (Connection otra = fuente.getConnection()) {
            otra.setAutoCommit(false);
            try (PreparedStatement ps =
                    otra.prepareStatement("UPDATE hito_equipo SET estado = 'ENVIADO' WHERE id_hito_equipo = ?")) {
                ps.setLong(1, he);
                ps.executeUpdate();
            }
            CompletableFuture<Integer> refrescando =
                    CompletableFuture.supplyAsync(() -> refresco().refrescarEnCurso());
            esperarUnaEspera();
            otra.commit();
            refrescando.get(30, TimeUnit.SECONDS);
        }
        assertThat(nivel(t)).isEqualTo("BAJO");
    }

    /** Espera acotada a que alguna sesión quede detenida por un candado de fila. */
    private void esperarUnaEspera() throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < limite) {
            int esperando = jdbc.sql("SELECT count(*) FROM pg_locks WHERE NOT granted "
                            + "AND locktype IN ('transactionid','tuple')")
                    .query(Integer.class)
                    .single();
            if (esperando >= 1) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("El recálculo no esperó el candado del hito");
    }
}
