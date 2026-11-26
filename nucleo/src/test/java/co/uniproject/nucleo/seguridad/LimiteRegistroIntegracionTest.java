package co.uniproject.nucleo.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/** Bloque 41: límite de registros fallidos por IP (20 por hora); un alta exitosa no gasta cupo. */
class LimiteRegistroIntegracionTest extends ApiIntegracionBase {

    @Value("${uniproject.recuperacion.maximo-registro-por-ip}")
    private int maximo;

    @BeforeEach
    void sinLimites() {
        jdbc.sql("DELETE FROM limite_recuperacion").update();
    }

    @AfterEach
    void limpiarLimites() {
        jdbc.sql("DELETE FROM limite_recuperacion").update();
    }

    private static final String CODIGO_FICTICIO = "A".repeat(43);

    private record Habilitacion(String email, String codigo) {}

    private HttpResponse<String> registrar(String email, String codigo) {
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        return n.post(
                "/api/cuentas/registro",
                "{\"nombre\":\"Persona Limite\",\"email\":\"" + email
                        + "\",\"contrasena\":\"" + CLAVE + "\",\"autorizaTratamientoDatos\":true,\"codigo\":\"" + codigo
                        + "\"}");
    }

    private Habilitacion habilitar(String prefijo) {
        String docente = correoUnico("doclimreg");
        long idDocente = crearUsuario("Docente Limite", docente, "DOCENTE");
        long curso = cursoSql(idDocente);
        String email = correoUnico(prefijo);
        jdbc.sql("INSERT INTO correo_habilitado (id_curso, email, rol_plataforma, id_habilitado_por) "
                        + "VALUES (:c, :e, 'ESTUDIANTE', :d)")
                .param("c", curso)
                .param("e", email)
                .param("d", idDocente)
                .update();
        return new Habilitacion(email, enlaceCurso(sesion(docente), curso, email));
    }

    private void fallidos(int cuantos) {
        for (int i = 0; i < cuantos; i++) {
            HttpResponse<String> r = registrar(correoUnico("nohab" + i), CODIGO_FICTICIO);
            assertThat(r.statusCode()).isEqualTo(409);
            assertThat(r.body()).contains("\"codigo\":\"REGISTRO_NO_DISPONIBLE\"");
        }
    }

    @Test
    void pasadoElMaximoDa429SinCrearCuenta() {
        fallidos(maximo);

        Habilitacion habilitado = habilitar("habilitado");
        HttpResponse<String> r = registrar(habilitado.email(), habilitado.codigo());

        assertThat(r.statusCode()).isEqualTo(429);
        assertThat(r.body()).contains("\"codigo\":\"DEMASIADAS_SOLICITUDES\"").doesNotContain(habilitado.email());
        assertThat(jdbc.sql("SELECT count(*) FROM usuario WHERE email = :e")
                        .param("e", habilitado.email())
                        .query(Integer.class)
                        .single())
                .isZero();
    }

    @Test
    void elAltaExitosaNoGastaCupo() {
        fallidos(maximo - 1);

        Habilitacion valido = habilitar("valido");
        HttpResponse<String> alta = registrar(valido.email(), valido.codigo());
        assertThat(alta.statusCode()).as(alta.body()).isEqualTo(201);

        HttpResponse<String> otro = registrar(correoUnico("nohabfinal"), CODIGO_FICTICIO);
        assertThat(otro.statusCode()).isEqualTo(409);
    }
}
