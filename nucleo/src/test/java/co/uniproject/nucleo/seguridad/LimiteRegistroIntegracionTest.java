package co.uniproject.nucleo.seguridad;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

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

    private HttpResponse<String> registrar(String email) {
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        return n.post("/api/cuentas/registro", "{\"nombre\":\"Persona Limite\",\"email\":\"" + email
                + "\",\"contrasena\":\"" + CLAVE + "\",\"autorizaTratamientoDatos\":true}");
    }

    private String habilitar(String prefijo) {
        String docente = correoUnico("doclimreg");
        long idDocente = crearUsuario("Docente Limite", docente, "DOCENTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Limite', DATE '2026-08-03', DATE '2026-11-27') RETURNING id_proyecto")
                .param("d", idDocente).query(Long.class).single();
        String email = correoUnico(prefijo);
        jdbc.sql("INSERT INTO correo_habilitado (id_proyecto, email, rol_plataforma, id_habilitado_por) "
                        + "VALUES (:p, :e, 'ESTUDIANTE', :d)")
                .param("p", proyecto).param("e", email).param("d", idDocente).update();
        return email;
    }

    private void fallidos(int cuantos) {
        for (int i = 0; i < cuantos; i++) {
            HttpResponse<String> r = registrar(correoUnico("nohab" + i));
            assertThat(r.statusCode()).isEqualTo(409);
            assertThat(r.body()).contains("\"codigo\":\"REGISTRO_NO_DISPONIBLE\"");
        }
    }

    @Test
    void pasadoElMaximoDa429SinCrearCuenta() {
        fallidos(maximo);

        String habilitado = habilitar("habilitado");
        HttpResponse<String> r = registrar(habilitado);

        assertThat(r.statusCode()).isEqualTo(429);
        assertThat(r.body()).contains("\"codigo\":\"DEMASIADAS_SOLICITUDES\"").doesNotContain(habilitado);
        assertThat(jdbc.sql("SELECT count(*) FROM usuario WHERE email = :e").param("e", habilitado)
                .query(Integer.class).single()).isZero();
    }

    @Test
    void elAltaExitosaNoGastaCupo() {
        fallidos(maximo - 1);

        HttpResponse<String> alta = registrar(habilitar("valido"));
        assertThat(alta.statusCode()).as(alta.body()).isEqualTo(201);

        HttpResponse<String> otro = registrar(correoUnico("nohabfinal"));
        assertThat(otro.statusCode()).isEqualTo(409);
    }
}
