package co.uniproject.nucleo.configuracionacademica;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Concurrencia al pedir el último cupo de un equipo por autoinscripción. */
class UltimoCupoConcurrenciaIntegracionTest extends ApiIntegracionBase {

    @Autowired
    private DataSource fuente;

    @Test
    void dosEstudiantesPorElUltimoCupoSoloUnoEntra() throws Exception {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String docente = correoUnico("docuc");
        long idDocente = crearUsuario("Docente Ultimo Cupo", docente, "DOCENTE");
        String email1 = correoUnico("estuc1");
        String email2 = correoUnico("estuc2");
        long id1 = crearUsuario("Estudiante UC1", email1, "ESTUDIANTE");
        long id2 = crearUsuario("Estudiante UC2", email2, "ESTUDIANTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin, cierre_equipos) "
                        + "VALUES (:d, 'Proyecto Ultimo Cupo', :i, :f, now() + interval '7 days') "
                        + "RETURNING id_proyecto")
                .param("d", idDocente)
                .param("i", hoy.minusDays(1))
                .param("f", hoy.plusDays(60))
                .query(Long.class)
                .single();
        long curso = cursoSql(idDocente, proyecto);
        participanteSql(curso, proyecto, id1);
        participanteSql(curso, proyecto, id2);
        long equipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo, id_curso, cupo) "
                        + "VALUES (:p, 'Equipo Ultimo', :c, 1) RETURNING id_equipo")
                .param("p", proyecto)
                .param("c", curso)
                .query(Long.class)
                .single();

        Navegador n1 = sesion(email1);
        Navegador n2 = sesion(email2);
        try (Connection otra = fuente.getConnection()) {
            otra.setAutoCommit(false);
            try (PreparedStatement ps =
                    otra.prepareStatement("SELECT 1 FROM proyecto WHERE id_proyecto = ? FOR NO KEY UPDATE")) {
                ps.setLong(1, proyecto);
                ps.executeQuery();
            }
            CompletableFuture<HttpResponse<String>> p1 =
                    CompletableFuture.supplyAsync(() -> n1.post("/api/equipos/" + equipo + "/inscripcion", null));
            esperarEsperas(1);
            CompletableFuture<HttpResponse<String>> p2 =
                    CompletableFuture.supplyAsync(() -> n2.post("/api/equipos/" + equipo + "/inscripcion", null));
            esperarEsperas(2);
            otra.commit();
            HttpResponse<String> r1 = p1.get(30, TimeUnit.SECONDS);
            HttpResponse<String> r2 = p2.get(30, TimeUnit.SECONDS);
            List<HttpResponse<String>> respuestas = List.of(r1, r2);
            assertThat(respuestas).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(200, 409);
            HttpResponse<String> rechazada = respuestas.stream()
                    .filter(r -> r.statusCode() == 409)
                    .findFirst()
                    .orElseThrow();
            assertThat(rechazada.body()).contains("\"codigo\":\"EQUIPO_LLENO\"");
        }
        assertThat(jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_equipo = :e")
                        .param("e", equipo)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
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
        throw new AssertionError("La petición " + n + " no esperó el candado del proyecto");
    }
}
