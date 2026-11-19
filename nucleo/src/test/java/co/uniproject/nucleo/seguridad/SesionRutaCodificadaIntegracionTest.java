package co.uniproject.nucleo.seguridad;

import co.uniproject.nucleo.ApiIntegracionBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Bloque 44: la revocación de sesión también se aplica a rutas con la API codificada (/%61pi/...). */
class SesionRutaCodificadaIntegracionTest extends ApiIntegracionBase {

    @Test
    void sesionRevocadaDa401TambienConRutaCodificada() {
        String email = correoUnico("usr");
        long id = crearUsuario("Usuario Ruta", email, "ESTUDIANTE");
        Navegador n = sesion(email);
        assertThat(n.get("/api/sesion/yo").statusCode()).isEqualTo(200);
        jdbc.sql("UPDATE usuario SET version_credenciales = version_credenciales + 1 WHERE id_usuario = :id")
                .param("id", id).update();
        assertThat(n.get("/%61pi/sesion/yo").statusCode()).isEqualTo(401);
    }

    @Test
    void sesionRevocadaDa401EnLaRutaNormal() {
        String email = correoUnico("usr");
        long id = crearUsuario("Usuario Ruta", email, "ESTUDIANTE");
        Navegador n = sesion(email);
        jdbc.sql("UPDATE usuario SET version_credenciales = version_credenciales + 1 WHERE id_usuario = :id")
                .param("id", id).update();
        assertThat(n.get("/api/sesion/yo").statusCode()).isEqualTo(401);
    }

    @Test
    void sesionRevocadaCargaLaSpaEnRutasDePagina() {
        String email = correoUnico("usr");
        long id = crearUsuario("Usuario Ruta", email, "ESTUDIANTE");
        Navegador n = sesion(email);
        jdbc.sql("UPDATE usuario SET version_credenciales = version_credenciales + 1 WHERE id_usuario = :id")
                .param("id", id).update();
        var r = n.get("/mis-tareas");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("uniproject-spa-prueba");
    }
}
