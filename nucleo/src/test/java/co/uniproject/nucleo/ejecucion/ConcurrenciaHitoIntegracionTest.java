package co.uniproject.nucleo.ejecucion;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Bloque 42: candado por hito de equipo (hito_equipo FOR UPDATE) en las escrituras de tareas y en el envío. */
class ConcurrenciaHitoIntegracionTest extends ApiIntegracionBase {

    @Autowired
    private DataSource fuente;

    private record Escenario(long idHitoEquipo, String lider, String integrante, long idLider, long idIntegrante,
                             long idOtro) {}

    private Escenario escenario() {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String docente = correoUnico("doc");
        long idDocente = crearUsuario("Docente Concurrencia", docente, "DOCENTE");
        String lider = correoUnico("lider");
        long idLider = crearUsuario("Lider Concurrencia", lider, "ESTUDIANTE");
        String integrante = correoUnico("int");
        long idIntegrante = crearUsuario("Integrante Uno", integrante, "ESTUDIANTE");
        long idOtro = crearUsuario("Integrante Dos", correoUnico("otro"), "ESTUDIANTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Concurrencia', :i, :f) RETURNING id_proyecto")
                .param("d", idDocente).param("i", hoy.minusDays(30)).param("f", hoy.plusDays(60))
                .query(Long.class).single();
        long hito = jdbc.sql("INSERT INTO hito (id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) "
                        + "VALUES (:p, 'Hito 1', 1, :i, :f) RETURNING id_hito")
                .param("p", proyecto).param("i", hoy.minusDays(10)).param("f", hoy.plusDays(20))
                .query(Long.class).single();
        long equipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo) VALUES (:p, 'Equipo C') "
                        + "RETURNING id_equipo")
                .param("p", proyecto).query(Long.class).single();
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES (:e, :l, 'LIDER'), "
                        + "(:e, :a, 'INTEGRANTE'), (:e, :b, 'INTEGRANTE')")
                .param("e", equipo).param("l", idLider).param("a", idIntegrante).param("b", idOtro).update();
        long he = jdbc.sql("INSERT INTO hito_equipo (id_hito, id_equipo) VALUES (:h, :e) RETURNING id_hito_equipo")
                .param("h", hito).param("e", equipo).query(Long.class).single();
        return new Escenario(he, lider, integrante, idLider, idIntegrante, idOtro);
    }

    private long tarea(long idHitoEquipo, Long encargado, String titulo, String horas) {
        return jdbc.sql("INSERT INTO tarea (id_hito_equipo, id_encargado, titulo, horas_estimadas) "
                        + "VALUES (:he, :u, :t, :h) RETURNING id_tarea")
                .param("he", idHitoEquipo).param("u", encargado, java.sql.Types.BIGINT).param("t", titulo)
                .param("h", new BigDecimal(horas)).query(Long.class).single();
    }

    /** Toma el candado del hito en otra conexión, lanza dos peticiones en orden, las deja esperando y libera. */
    private HttpResponse<String>[] enRafaga(long idHitoEquipo, Supplier<HttpResponse<String>> primera,
                                            Supplier<HttpResponse<String>> segunda) throws Exception {
        try (Connection otra = fuente.getConnection()) {
            otra.setAutoCommit(false);
            try (PreparedStatement ps = otra.prepareStatement(
                    "SELECT 1 FROM hito_equipo WHERE id_hito_equipo = ? FOR UPDATE")) {
                ps.setLong(1, idHitoEquipo);
                ps.executeQuery();
            }
            CompletableFuture<HttpResponse<String>> p1 = CompletableFuture.supplyAsync(primera);
            esperarEsperas(1);
            CompletableFuture<HttpResponse<String>> p2 = CompletableFuture.supplyAsync(segunda);
            esperarEsperas(2);
            otra.commit();
            @SuppressWarnings("unchecked")
            HttpResponse<String>[] r = new HttpResponse[] {p1.get(30, TimeUnit.SECONDS), p2.get(30, TimeUnit.SECONDS)};
            return r;
        }
    }

    /** Espera acotada a que haya al menos n sesiones detenidas por un candado de fila (sin sleeps de orden). */
    private void esperarEsperas(int n) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < limite) {
            int esperando = jdbc.sql("SELECT count(*) FROM pg_locks WHERE NOT granted "
                            + "AND locktype IN ('transactionid','tuple')")
                    .query(Integer.class).single();
            if (esperando >= n) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("La petición " + n + " no esperó el candado del hito");
    }

    @Test
    void dependenciasCruzadasNoFormanCiclo() throws Exception {
        Escenario e = escenario();
        long a = tarea(e.idHitoEquipo(), e.idIntegrante(), "Tarea A", "2");
        long b = tarea(e.idHitoEquipo(), e.idOtro(), "Tarea B", "2");
        Navegador n1 = sesion(e.lider());
        Navegador n2 = sesion(e.lider());
        HttpResponse<String>[] r = enRafaga(e.idHitoEquipo(),
                () -> n1.post("/api/tareas/" + b + "/dependencias", "{\"prerrequisitos\":[" + a + "]}"),
                () -> n2.post("/api/tareas/" + a + "/dependencias", "{\"prerrequisitos\":[" + b + "]}"));
        assertThat(r[0].statusCode()).as(r[0].body()).isEqualTo(200);
        assertThat(r[0].body()).contains("\"aceptados\":[" + a + "]").contains("\"ciclicos\":[]");
        assertThat(r[1].statusCode()).as(r[1].body()).isEqualTo(200);
        assertThat(r[1].body()).contains("\"aceptados\":[]").contains("\"ciclicos\":[" + b + "]");
        assertThat(jdbc.sql("SELECT count(*) FROM dependencia_tarea WHERE id_tarea_predecesora IN (:a, :b) "
                        + "AND id_tarea_dependiente IN (:a, :b)")
                .param("a", a).param("b", b).query(Integer.class).single()).isEqualTo(1);
        assertThat(n1.get("/api/hitos-equipo/" + e.idHitoEquipo() + "/ruta-critica").statusCode()).isEqualTo(200);
    }

    @Test
    void reasignacionParcialDobleNoDuplicaNiPierdeHoras() throws Exception {
        Escenario e = escenario();
        long t = tarea(e.idHitoEquipo(), e.idIntegrante(), "Tarea grande", "10");
        String cuerpo = "{\"idNuevoEncargado\":" + e.idOtro()
                + ",\"tipo\":\"PARCIAL\",\"tituloParcial\":\"Parte doble\",\"horasParcial\":4}";
        Navegador n1 = sesion(e.lider());
        Navegador n2 = sesion(e.lider());
        HttpResponse<String>[] r = enRafaga(e.idHitoEquipo(),
                () -> n1.post("/api/tareas/" + t + "/reasignacion", cuerpo),
                () -> n2.post("/api/tareas/" + t + "/reasignacion", cuerpo));
        assertThat(r[0].statusCode()).as(r[0].body()).isEqualTo(200);
        assertThat(r[1].statusCode()).as(r[1].body()).isEqualTo(409);
        assertThat(r[1].body()).contains("\"codigo\":\"PARTE_REPETIDA\"");
        assertThat(jdbc.sql("SELECT horas_estimadas FROM tarea WHERE id_tarea = :t").param("t", t)
                .query(BigDecimal.class).single()).isEqualByComparingTo("6.00");
        assertThat(jdbc.sql("SELECT count(*) FROM tarea WHERE id_tarea_origen = :t").param("t", t)
                .query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void avanceConcurrenteAlEnvioNoSeGuarda() throws Exception {
        Escenario e = escenario();
        long t = tarea(e.idHitoEquipo(), e.idIntegrante(), "Tarea en curso", "3");
        Navegador envia = sesion(e.lider());
        Navegador avanza = sesion(e.integrante());
        HttpResponse<String>[] r = enRafaga(e.idHitoEquipo(),
                () -> envia.post("/api/hitos-equipo/" + e.idHitoEquipo() + "/envio", "{}"),
                () -> avanza.put("/api/tareas/" + t + "/avance", "{\"porcentajeAvance\":50}"));
        assertThat(r[0].statusCode()).as(r[0].body()).isEqualTo(201);
        assertThat(r[1].statusCode()).as(r[1].body()).isEqualTo(409);
        assertThat(r[1].body()).contains("\"codigo\":\"HITO_NO_EDITABLE\"");
        assertThat(jdbc.sql("SELECT porcentaje_avance FROM tarea WHERE id_tarea = :t").param("t", t)
                .query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM registro_avance WHERE id_tarea = :t").param("t", t)
                .query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT estado FROM hito_equipo WHERE id_hito_equipo = :he")
                .param("he", e.idHitoEquipo()).query(String.class).single()).isEqualTo("ENVIADO");
    }
}
