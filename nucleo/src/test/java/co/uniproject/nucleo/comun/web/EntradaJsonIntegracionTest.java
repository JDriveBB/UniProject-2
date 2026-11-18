package co.uniproject.nucleo.comun.web;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Bloque 41: el JSON de entrada rechaza el carácter nulo y los tipos mezclados con 400 CUERPO_INVALIDO. */
class EntradaJsonIntegracionTest extends ApiIntegracionBase {

    private static final String NULO = "\\u0000";

    private static String proyecto(String nombreJson, String usaMonitoresJson) {
        return "{\"nombre\":" + nombreJson + ",\"fechaInicio\":\"2026-08-03\",\"fechaFin\":\"2026-11-27\","
                + "\"usaMonitores\":" + usaMonitoresJson + "}";
    }

    private static void exigirCuerpoInvalido(HttpResponse<String> r) {
        assertThat(r.statusCode()).as(r.body()).isEqualTo(400);
        assertThat(r.body()).contains("\"codigo\":\"CUERPO_INVALIDO\"");
    }

    @Test
    void caracterNuloEnTextoDa400SinGuardar() {
        Navegador anonimo = new Navegador(puerto);
        anonimo.pedirCsrf();
        String correo = correoUnico("nulo");
        exigirCuerpoInvalido(anonimo.post("/api/cuentas/registro", "{\"nombre\":\"Ana" + NULO + "B\",\"email\":\""
                + correo + "\",\"contrasena\":\"" + CLAVE + "\",\"autorizaTratamientoDatos\":true}"));
        assertThat(jdbc.sql("SELECT count(*) FROM usuario WHERE email = :e").param("e", correo)
                .query(Integer.class).single()).isZero();

        String emailDocente = correoUnico("docnulo");
        long idDocente = crearUsuario("Docente Nulo", emailDocente, "DOCENTE");
        Navegador docente = sesion(emailDocente);
        exigirCuerpoInvalido(docente.post("/api/proyectos", proyecto("\"P" + NULO + "Q\"", "false")));
        assertThat(jdbc.sql("SELECT count(*) FROM proyecto WHERE id_docente = :d").param("d", idDocente)
                .query(Integer.class).single()).isZero();

        long idProyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Nulo', DATE '2026-08-03', DATE '2026-11-27') RETURNING id_proyecto")
                .param("d", idDocente).query(Long.class).single();
        exigirCuerpoInvalido(docente.post("/api/proyectos/" + idProyecto + "/correos",
                "{\"emails\":[\"a" + NULO + "b@u.edu.co\"],\"rol\":\"ESTUDIANTE\"}"));
    }

    @Test
    void tiposJsonEstrictos() {
        String emailDocente = correoUnico("doctipos");
        long idDocente = crearUsuario("Docente Tipos", emailDocente, "DOCENTE");
        Navegador docente = sesion(emailDocente);

        exigirCuerpoInvalido(docente.post("/api/proyectos", proyecto("123", "false")));
        exigirCuerpoInvalido(docente.post("/api/proyectos", proyecto("true", "false")));
        exigirCuerpoInvalido(docente.post("/api/proyectos", proyecto("\"Tipos\"", "\"true\"")));
        assertThat(jdbc.sql("SELECT count(*) FROM proyecto WHERE id_docente = :d").param("d", idDocente)
                .query(Integer.class).single()).isZero();

        HttpResponse<String> control = docente.post("/api/proyectos", proyecto("\"Tipos\"", "false"));
        assertThat(control.statusCode()).as(control.body()).isEqualTo(201);
        long idProyecto = numero(control.body(), "idProyecto");

        exigirCuerpoInvalido(docente.post("/api/proyectos/" + idProyecto + "/hitos",
                "{\"nombreHito\":\"Hito\",\"orden\":\"1\",\"fechaInicio\":\"2026-08-10\","
                        + "\"fechaLimite\":\"2026-09-15\"}"));
    }
}
