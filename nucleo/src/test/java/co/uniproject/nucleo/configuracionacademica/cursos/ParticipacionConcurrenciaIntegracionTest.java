package co.uniproject.nucleo.configuracionacademica.cursos;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Concurrencia entre quitar un estudiante del curso y agregarlo a un equipo del proyecto. */
class ParticipacionConcurrenciaIntegracionTest extends ApiIntegracionBase {

    @Autowired
    private DataSource fuente;

    @Test
    void quitarDelCursoYAgregarAlEquipoNoDejanIntegranteSinCurso() throws Exception {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String docente = correoUnico("docconc");
        long idDocente = crearUsuario("Docente Concurrencia", docente, "DOCENTE");
        String email = correoUnico("estconc");
        long idEstudiante = crearUsuario("Estudiante Concurrencia", email, "ESTUDIANTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Conc', :i, :f) RETURNING id_proyecto")
                .param("d", idDocente)
                .param("i", hoy.minusDays(1))
                .param("f", hoy.plusDays(60))
                .query(Long.class)
                .single();
        long curso = cursoSql(idDocente, proyecto);
        participanteSql(curso, proyecto, idEstudiante);
        long equipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo) VALUES (:p, 'Equipo Conc') "
                        + "RETURNING id_equipo")
                .param("p", proyecto)
                .query(Long.class)
                .single();

        Navegador n1 = sesion(docente);
        Navegador n2 = sesion(docente);
        try (Connection otra = fuente.getConnection()) {
            otra.setAutoCommit(false);
            try (PreparedStatement ps =
                    otra.prepareStatement("SELECT 1 FROM proyecto WHERE id_proyecto = ? FOR NO KEY UPDATE")) {
                ps.setLong(1, proyecto);
                ps.executeQuery();
            }
            CompletableFuture<HttpResponse<String>> p1 = CompletableFuture.supplyAsync(
                    () -> n1.delete("/api/cursos/" + curso + "/estudiantes/" + idEstudiante));
            esperarEsperas(1);
            CompletableFuture<HttpResponse<String>> p2 = CompletableFuture.supplyAsync(
                    () -> n2.post("/api/equipos/" + equipo + "/miembros", "{\"email\":\"" + email + "\"}"));
            esperarEsperas(2);
            otra.commit();
            HttpResponse<String> r1 = p1.get(30, TimeUnit.SECONDS);
            HttpResponse<String> r2 = p2.get(30, TimeUnit.SECONDS);
            assertThat(r1.statusCode()).as(r1.body()).isEqualTo(204);
            assertThat(r2.statusCode()).as(r2.body()).isEqualTo(409);
            assertThat(r2.body()).contains("\"codigo\":\"ESTUDIANTE_SIN_CURSO\"");
        }
        assertThat(jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_equipo = :e AND id_usuario = :u")
                        .param("e", equipo)
                        .param("u", idEstudiante)
                        .query(Integer.class)
                        .single())
                .isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u")
                        .param("p", proyecto)
                        .param("u", idEstudiante)
                        .query(Integer.class)
                        .single())
                .isZero();
    }

    /** Espera acotada a que haya al menos n sesiones detenidas por un candado de fila. */
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
