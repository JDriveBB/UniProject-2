package co.uniproject.nucleo.configuracionacademica;

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

import static org.assertj.core.api.Assertions.assertThat;

/** Corrección 15-A: el mismo correo habilitado a la vez por dos operaciones da 409, no 500. */
class CorreosConcurrenciaIntegracionTest extends ApiIntegracionBase {

    @Autowired
    private DataSource fuente;

    @Test
    void elCorreoHabilitadoEnParaleloDa409() throws Exception {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String docente = correoUnico("doc");
        long idDocente = crearUsuario("Docente Paralelo", docente, "DOCENTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Paralelo', :i, :f) RETURNING id_proyecto")
                .param("d", idDocente).param("i", hoy.minusDays(1)).param("f", hoy.plusDays(60))
                .query(Long.class).single();
        String email = correoUnico("est");
        String cuerpo = "{\"emails\":[\"" + email + "\"],\"rol\":\"ESTUDIANTE\"}";
        Navegador navegador = sesion(docente);
        try (Connection otra = fuente.getConnection()) {
            otra.setAutoCommit(false);
            try (PreparedStatement ps = otra.prepareStatement("INSERT INTO correo_habilitado "
                    + "(id_proyecto, email, rol_plataforma, id_habilitado_por) VALUES (?, ?, 'ESTUDIANTE', ?)")) {
                ps.setLong(1, proyecto);
                ps.setString(2, email);
                ps.setLong(3, idDocente);
                ps.executeUpdate();
            }
            CompletableFuture<HttpResponse<String>> pedido = CompletableFuture.supplyAsync(
                    () -> navegador.post("/api/proyectos/" + proyecto + "/correos", cuerpo));
            esperarBloqueo();
            otra.commit();
            HttpResponse<String> r = pedido.get(30, TimeUnit.SECONDS);
            assertThat(r.statusCode()).as(r.body()).isEqualTo(409);
            assertThat(r.body()).contains("\"codigo\":\"CORREO_HABILITADO_EN_PARALELO\"");
        }
        assertThat(jdbc.sql("SELECT count(*) FROM correo_habilitado WHERE id_proyecto = :p").param("p", proyecto)
                .query(Integer.class).single()).isEqualTo(1);
        assertThat(navegador.post("/api/proyectos/" + proyecto + "/correos", cuerpo).body())
                .contains("\"yaHabilitados\":[\"" + email + "\"]");
    }

    /** Espera a que el INSERT de la API quede detenido por la fila sin confirmar (determinista, sin sleeps fijos). */
    private void esperarBloqueo() throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < limite) {
            int esperando = jdbc.sql("SELECT count(*) FROM pg_locks WHERE NOT granted AND locktype = 'transactionid'")
                    .query(Integer.class).single();
            if (esperando > 0) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("La habilitación nunca quedó esperando la fila sin confirmar");
    }
}
