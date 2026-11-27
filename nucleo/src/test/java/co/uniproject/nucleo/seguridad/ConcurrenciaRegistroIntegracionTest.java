package co.uniproject.nucleo.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Bloque 46: dos registros simultáneos del mismo correo con enlaces de proyectos distintos no se interbloquean. */
class ConcurrenciaRegistroIntegracionTest extends ApiIntegracionBase {

    @Autowired
    private DataSource fuente;

    @BeforeEach
    void limpiarLimites() {
        jdbc.sql("DELETE FROM limite_recuperacion").update();
    }

    /** Habilita el correo en el curso, genera el enlace y devuelve el código. */
    private String codigo(Navegador docente, long idCurso, String email) {
        HttpResponse<String> h =
                docente.post("/api/cursos/" + idCurso + "/correos", "{\"emails\":[\"" + email + "\"]}");
        assertThat(h.statusCode()).as(h.body()).isEqualTo(201);
        long idCorreo = numero(h.body(), "idCorreoHabilitado");
        HttpResponse<String> e = docente.post("/api/cursos/" + idCurso + "/correos/" + idCorreo + "/enlace", null);
        assertThat(e.statusCode()).as(e.body()).isEqualTo(200);
        return campoTexto(e.body(), "codigo");
    }

    private HttpResponse<String> registrar(String email, String codigo) {
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        return n.post(
                "/api/cuentas/registro",
                "{\"nombre\":\"Estudiante Doble\",\"email\":\"" + email + "\",\"contrasena\":\"" + CLAVE
                        + "\",\"autorizaTratamientoDatos\":true,\"codigo\":\"" + codigo + "\"}");
    }

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
        throw new AssertionError("Los registros no esperaron el candado de las habilitaciones");
    }

    @Test
    void dosRegistrosDelMismoCorreoConEnlacesDeProyectosDistintosNoDanError500() throws Exception {
        String emailDocente = correoUnico("docdoble");
        crearUsuario("Docente Doble", emailDocente, "DOCENTE");
        Navegador docente = sesion(emailDocente);
        String email = correoUnico("estdoble");
        String codigo1 = codigo(docente, crearCurso(docente, "Curso Registro 1"), email);
        String codigo2 = codigo(docente, crearCurso(docente, "Curso Registro 2"), email);

        HttpResponse<String> r1;
        HttpResponse<String> r2;
        try (Connection otra = fuente.getConnection()) {
            otra.setAutoCommit(false);
            // Una tercera conexión retiene las habilitaciones pendientes del correo: ambos registros llegan y esperan.
            try (PreparedStatement ps = otra.prepareStatement(
                    "SELECT 1 FROM correo_habilitado WHERE email = ? AND fecha_uso IS NULL FOR UPDATE")) {
                ps.setString(1, email);
                ps.executeQuery();
            }
            CompletableFuture<HttpResponse<String>> f1 = CompletableFuture.supplyAsync(() -> registrar(email, codigo1));
            esperarEsperas(1);
            CompletableFuture<HttpResponse<String>> f2 = CompletableFuture.supplyAsync(() -> registrar(email, codigo2));
            esperarEsperas(2);
            otra.commit();
            r1 = f1.get(30, TimeUnit.SECONDS);
            r2 = f2.get(30, TimeUnit.SECONDS);
        }

        int creadas = (r1.statusCode() == 201 ? 1 : 0) + (r2.statusCode() == 201 ? 1 : 0);
        assertThat(creadas).as(r1.body() + " | " + r2.body()).isEqualTo(1);
        HttpResponse<String> rechazado = r1.statusCode() == 201 ? r2 : r1;
        assertThat(rechazado.statusCode()).as(rechazado.body()).isEqualTo(409);
        assertThat(rechazado.body())
                .contains("\"codigo\":\"REGISTRO_NO_DISPONIBLE\"")
                .contains(ServicioCuentas.MENSAJE_REGISTRO_NO_DISPONIBLE);
        assertThat(jdbc.sql("SELECT count(*) FROM usuario WHERE email = :e")
                        .param("e", email)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }
}
