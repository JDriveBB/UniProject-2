package co.uniproject.nucleo.configuracionacademica.votacion;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Concurrencia al abrir rondas de votación de líder (dos peticiones HTTP o programador y docente a la vez). */
class VotacionConcurrenciaIntegracionTest extends ApiIntegracionBase {

    @Autowired
    private DataSource fuente;

    @Autowired
    private RevisionRondas revisionRondas;

    @Test
    void dosAperturasALaVezDejanUnaSolaRonda() throws Exception {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String emailDoc = correoUnico("doccv");
        long idDocente = crearUsuario("Docente Conc Vot", emailDoc, "DOCENTE");
        String emailEst = correoUnico("estcv");
        long idEst = crearUsuario("Estudiante Conc Vot", emailEst, "ESTUDIANTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Conc Vot', :i, :f) RETURNING id_proyecto")
                .param("d", idDocente)
                .param("i", hoy.minusDays(1))
                .param("f", hoy.plusDays(60))
                .query(Long.class)
                .single();
        long curso = cursoSql(idDocente, proyecto);
        participanteSql(curso, proyecto, idEst);
        long equipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo, id_curso) "
                        + "VALUES (:p, 'Equipo Conc Vot', :c) RETURNING id_equipo")
                .param("p", proyecto)
                .param("c", curso)
                .query(Long.class)
                .single();
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario) VALUES (:e, :u)")
                .param("e", equipo)
                .param("u", idEst)
                .update();

        Navegador n1 = sesion(emailDoc);
        Navegador n2 = sesion(emailDoc);
        try (Connection otra = fuente.getConnection()) {
            otra.setAutoCommit(false);
            try (PreparedStatement ps =
                    otra.prepareStatement("SELECT 1 FROM equipo WHERE id_equipo = ? FOR NO KEY UPDATE")) {
                ps.setLong(1, equipo);
                ps.executeQuery();
            }
            CompletableFuture<HttpResponse<String>> p1 =
                    CompletableFuture.supplyAsync(() -> n1.post("/api/equipos/" + equipo + "/ronda", null));
            esperarEsperas(1);
            CompletableFuture<HttpResponse<String>> p2 =
                    CompletableFuture.supplyAsync(() -> n2.post("/api/equipos/" + equipo + "/ronda", null));
            esperarEsperas(2);
            otra.commit();
            HttpResponse<String> r1 = p1.get(30, TimeUnit.SECONDS);
            HttpResponse<String> r2 = p2.get(30, TimeUnit.SECONDS);
            List<HttpResponse<String>> respuestas = List.of(r1, r2);
            assertThat(respuestas).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(201, 409);
            HttpResponse<String> rechazada = respuestas.stream()
                    .filter(r -> r.statusCode() == 409)
                    .findFirst()
                    .orElseThrow();
            assertThat(rechazada.body()).contains("\"codigo\":\"RONDA_ABIERTA\"");
        }
        assertThat(jdbc.sql("SELECT count(*) FROM ronda_lider WHERE id_equipo = :e AND cerrada_en IS NULL")
                        .param("e", equipo)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void programadorYDocenteALaVez() throws Exception {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String emailDoc = correoUnico("doccp");
        long idDocente = crearUsuario("Docente Conc Prog", emailDoc, "DOCENTE");
        String emailEst = correoUnico("estcp");
        long idEst = crearUsuario("Estudiante Conc Prog", emailEst, "ESTUDIANTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin, cierre_equipos) "
                        + "VALUES (:d, 'Proyecto Conc Prog', :i, :f, now() - interval '1 hour') RETURNING id_proyecto")
                .param("d", idDocente)
                .param("i", hoy.minusDays(1))
                .param("f", hoy.plusDays(60))
                .query(Long.class)
                .single();
        long curso = cursoSql(idDocente, proyecto);
        participanteSql(curso, proyecto, idEst);
        long equipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo, id_curso) "
                        + "VALUES (:p, 'Equipo Conc Prog', :c) RETURNING id_equipo")
                .param("p", proyecto)
                .param("c", curso)
                .query(Long.class)
                .single();
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario) VALUES (:e, :u)")
                .param("e", equipo)
                .param("u", idEst)
                .update();

        Navegador docente = sesion(emailDoc);
        try (Connection otra = fuente.getConnection()) {
            otra.setAutoCommit(false);
            try (PreparedStatement ps =
                    otra.prepareStatement("SELECT 1 FROM equipo WHERE id_equipo = ? FOR NO KEY UPDATE")) {
                ps.setLong(1, equipo);
                ps.executeQuery();
            }
            CompletableFuture<Integer> prog = CompletableFuture.supplyAsync(() -> revisionRondas.abrirProgramadas());
            esperarEsperas(1);
            CompletableFuture<HttpResponse<String>> peticion =
                    CompletableFuture.supplyAsync(() -> docente.post("/api/equipos/" + equipo + "/ronda", null));
            esperarEsperas(2);
            otra.commit();
            prog.get(30, TimeUnit.SECONDS);
            HttpResponse<String> resp = peticion.get(30, TimeUnit.SECONDS);
            assertThat(resp.statusCode()).as(resp.body()).isIn(201, 409);
            if (resp.statusCode() == 409) {
                assertThat(resp.body()).contains("\"codigo\":\"RONDA_ABIERTA\"");
            }
        }
        assertThat(jdbc.sql("SELECT count(*) FROM ronda_lider WHERE id_equipo = :e AND cerrada_en IS NULL")
                        .param("e", equipo)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        OffsetDateTime marca = jdbc.sql("SELECT rondas_programadas_en FROM proyecto WHERE id_proyecto = :p")
                .param("p", proyecto)
                .query(OffsetDateTime.class)
                .single();
        assertThat(marca).isNotNull();
    }

    /** Espera acotada a que haya al menos n sesiones detenidas por un candado de fila (sin sleeps de orden). */
    private void esperarEsperas(int n) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < limite) {
            int esperando = jdbc.sql("SELECT count(*) FROM pg_locks WHERE NOT granted "
                            + "AND locktype IN ('transactionid','tuple')")
                    .query(Integer.class)
                    .single();
            if (esperando >= n) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("La petición " + n + " no esperó el candado del equipo");
    }
}
