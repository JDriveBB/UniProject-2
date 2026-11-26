package co.uniproject.nucleo.configuracionacademica.votacion;

import co.uniproject.nucleo.configuracionacademica.servicio.ApoyoConfiguracion;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Tareas periódicas de rondas de votación de líder y recordatorios de cierre de inscripción. */
@Component
public class RevisionRondas {

    private static final Logger log = LoggerFactory.getLogger(RevisionRondas.class);

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final ApoyoConfiguracion apoyo;
    private final RondasLider rondas;
    private final Clock reloj;

    public RevisionRondas(
            JdbcClient jdbc, TransactionTemplate tx, ApoyoConfiguracion apoyo, RondasLider rondas, Clock reloj) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.apoyo = apoyo;
        this.rondas = rondas;
        this.reloj = reloj;
    }

    /** Cierra las rondas vencidas y devuelve cuántas cerró. */
    public int cerrarVencidas() {
        OffsetDateTime ahora = OffsetDateTime.ofInstant(Instant.now(reloj), ZoneOffset.UTC);
        List<Long> equipos = jdbc.sql("SELECT id_equipo FROM ronda_lider "
                        + "WHERE cerrada_en IS NULL AND cierra_en <= :ahora ORDER BY id_ronda")
                .param("ahora", ahora)
                .query(Long.class)
                .list();
        AtomicInteger cerradas = new AtomicInteger();
        for (long idEquipo : equipos) {
            try {
                tx.executeWithoutResult(s -> {
                    apoyo.bloquearEquipo(idEquipo);
                    if (rondas.cerrarSiVencida(idEquipo, Instant.now(reloj)).isPresent()) {
                        cerradas.incrementAndGet();
                    }
                });
            } catch (RuntimeException e) {
                log.error("Falló el cierre de la ronda vencida del equipo {}", idEquipo, e);
            }
        }
        return cerradas.get();
    }

    /** Abre rondas programadas al llegar cierre_equipos y devuelve cuántas rondas abrió. */
    public int abrirProgramadas() {
        OffsetDateTime ahora = OffsetDateTime.ofInstant(Instant.now(reloj), ZoneOffset.UTC);
        List<Long> proyectos = jdbc.sql("SELECT id_proyecto FROM proyecto "
                        + "WHERE cierre_equipos IS NOT NULL AND cierre_equipos <= :ahora "
                        + "AND rondas_programadas_en IS NULL ORDER BY id_proyecto")
                .param("ahora", ahora)
                .query(Long.class)
                .list();
        AtomicInteger abiertas = new AtomicInteger();
        for (long idProyecto : proyectos) {
            try {
                tx.executeWithoutResult(s -> {
                    apoyo.bloquearProyecto(idProyecto);
                    Instant momento = Instant.now(reloj);
                    OffsetDateTime momentoUtc = OffsetDateTime.ofInstant(momento, ZoneOffset.UTC);
                    boolean pendiente = jdbc.sql("SELECT EXISTS (SELECT 1 FROM proyecto WHERE id_proyecto = :p "
                                    + "AND cierre_equipos IS NOT NULL AND cierre_equipos <= :ahora "
                                    + "AND rondas_programadas_en IS NULL)")
                            .param("p", idProyecto)
                            .param("ahora", momentoUtc)
                            .query(Boolean.class)
                            .single();
                    if (!pendiente) {
                        return;
                    }
                    List<Long> equipos = jdbc.sql(
                                    "SELECT id_equipo FROM equipo WHERE id_proyecto = :p ORDER BY id_equipo")
                            .param("p", idProyecto)
                            .query(Long.class)
                            .list();
                    for (long idEquipo : equipos) {
                        apoyo.bloquearEquipo(idEquipo);
                        rondas.cerrarSiVencida(idEquipo, momento);
                        if (!rondas.candidatos(idEquipo).isEmpty()
                                && rondas.sinCerrar(idEquipo).isEmpty()) {
                            rondas.abrir(null, idEquipo, OrigenRonda.PROGRAMADA, momento);
                            abiertas.incrementAndGet();
                        }
                    }
                    jdbc.sql("UPDATE proyecto SET rondas_programadas_en = :ahora WHERE id_proyecto = :p")
                            .param("ahora", momentoUtc)
                            .param("p", idProyecto)
                            .update();
                });
            } catch (RuntimeException e) {
                log.error("Falló la apertura programada de rondas del proyecto {}", idProyecto, e);
            }
        }
        return abiertas.get();
    }

    /** Marca los proyectos a 24 h o menos del cierre de inscripción y devuelve cuántos marcó. */
    public int recordarCierres() {
        OffsetDateTime ahora = OffsetDateTime.ofInstant(Instant.now(reloj), ZoneOffset.UTC);
        List<Long> proyectos = jdbc.sql("SELECT id_proyecto FROM proyecto "
                        + "WHERE cierre_equipos IS NOT NULL AND recordatorio_cierre_en IS NULL "
                        + "AND cierre_equipos > :ahora AND cierre_equipos <= :ahora + interval '24 hours' "
                        + "ORDER BY id_proyecto")
                .param("ahora", ahora)
                .query(Long.class)
                .list();
        AtomicInteger marcados = new AtomicInteger();
        for (long idProyecto : proyectos) {
            try {
                tx.executeWithoutResult(s -> {
                    apoyo.bloquearProyecto(idProyecto);
                    Instant momento = Instant.now(reloj);
                    OffsetDateTime momentoUtc = OffsetDateTime.ofInstant(momento, ZoneOffset.UTC);
                    boolean pendiente = jdbc.sql("SELECT EXISTS (SELECT 1 FROM proyecto WHERE id_proyecto = :p "
                                    + "AND cierre_equipos IS NOT NULL AND recordatorio_cierre_en IS NULL "
                                    + "AND cierre_equipos > :ahora "
                                    + "AND cierre_equipos <= :ahora + interval '24 hours')")
                            .param("p", idProyecto)
                            .param("ahora", momentoUtc)
                            .query(Boolean.class)
                            .single();
                    if (!pendiente) {
                        return;
                    }
                    jdbc.sql("UPDATE proyecto SET recordatorio_cierre_en = :ahora WHERE id_proyecto = :p")
                            .param("ahora", momentoUtc)
                            .param("p", idProyecto)
                            .update();
                    // Bloque 4: registrar equipo.inscripcion_por_cerrar (estudiantes sin equipo) y
                    // equipo.inscripcion_resumen (docente).
                    marcados.incrementAndGet();
                });
            } catch (RuntimeException e) {
                log.error("Falló el recordatorio de cierre del proyecto {}", idProyecto, e);
            }
        }
        return marcados.get();
    }
}
