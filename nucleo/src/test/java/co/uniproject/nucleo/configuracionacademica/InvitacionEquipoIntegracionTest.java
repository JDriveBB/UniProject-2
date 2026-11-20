package co.uniproject.nucleo.configuracionacademica;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

/** T-027 (D-12 20A): invitar a un equipo a un estudiante habilitado sin cuenta; entra al equipo al registrarse. */
class InvitacionEquipoIntegracionTest extends ApiIntegracionBase {

    private static String correos(String rol, String... emails) {
        return "{\"emails\":[" + Arrays.stream(emails).map(e -> "\"" + e + "\"").collect(Collectors.joining(","))
                + "],\"rol\":\"" + rol + "\"}";
    }

    private static String invitacion(String email) {
        return "{\"email\":\"" + email + "\"}";
    }

    private Navegador docente() {
        String email = correoUnico("docente");
        crearUsuario("Docente Prueba", email, "DOCENTE");
        return sesion(email);
    }

    private long crearProyecto(Navegador docente, boolean usaMonitores) {
        HttpResponse<String> r = docente.post(
                "/api/proyectos",
                "{\"nombre\":\"Proyecto de prueba\","
                        + "\"fechaInicio\":\"2026-08-03\",\"fechaFin\":\"2026-11-27\",\"usaMonitores\":" + usaMonitores
                        + "}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idProyecto");
    }

    private long crearEquipo(Navegador docente, long idProyecto, String nombre) {
        HttpResponse<String> r =
                docente.post("/api/proyectos/" + idProyecto + "/equipos", "{\"nombreEquipo\":\"" + nombre + "\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idEquipo");
    }

    private void habilitar(Navegador docente, long idProyecto, String rol, String... emails) {
        HttpResponse<String> r = docente.post("/api/proyectos/" + idProyecto + "/correos", correos(rol, emails));
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
    }

    private void registrar(Navegador docente, long idProyecto, String email, String nombre) {
        String codigo = enlace(docente, idProyecto, email);
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        HttpResponse<String> r = n.post(
                "/api/cuentas/registro",
                "{\"nombre\":\"" + nombre + "\",\"email\":\"" + email + "\",\"contrasena\":\"" + CLAVE
                        + "\",\"autorizaTratamientoDatos\":true,\"codigo\":\"" + codigo + "\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
    }

    private Long equipoInvitado(long idProyecto, String email) {
        return jdbc.sql("SELECT id_equipo_invitado FROM correo_habilitado WHERE id_proyecto = :p AND email = :e")
                .param("p", idProyecto)
                .param("e", email)
                .query(Long.class)
                .optional()
                .orElse(null);
    }

    /** Filas de log_auditoria que cumplen la condición; los valores van como :v0, :v1… */
    private int auditoria(String condicion, Object... valores) {
        JdbcClient.StatementSpec consulta = jdbc.sql("SELECT count(*) FROM log_auditoria WHERE " + condicion);
        for (int i = 0; i < valores.length; i++) {
            consulta = consulta.param("v" + i, valores[i]);
        }
        return consulta.query(Integer.class).single();
    }

    @Test
    void laInvitadaEntraAlEquipoCuandoCreaSuCuenta() {
        Navegador docente = docente();
        long id = crearProyecto(docente, false);
        long idA = crearEquipo(docente, id, "Equipo A");
        crearEquipo(docente, id, "Equipo B");
        String ines = correoUnico("ines");
        String libre = correoUnico("libre");
        habilitar(docente, id, "ESTUDIANTE", ines, libre);

        HttpResponse<String> r = docente.post(
                "/api/equipos/" + idA + "/invitaciones", invitacion(" " + ines.toUpperCase(Locale.ROOT) + " "));
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        assertThat(r.body())
                .contains("\"email\":\"" + ines + "\"")
                .contains("\"registrado\":false")
                .contains("\"idEquipoInvitado\":" + idA)
                .contains("\"idEquipo\":null")
                .contains("\"nombre\":null");
        assertThat(equipoInvitado(id, ines)).isEqualTo(idA);

        HttpResponse<String> otraVez = docente.post("/api/equipos/" + idA + "/invitaciones", invitacion(ines));
        assertThat(otraVez.statusCode()).as(otraVez.body()).isEqualTo(201);
        long idCorreo = jdbc.sql(
                        "SELECT id_correo_habilitado FROM correo_habilitado WHERE id_proyecto = :p AND email = :e")
                .param("p", id)
                .param("e", ines)
                .query(Long.class)
                .single();
        assertThat(auditoria(
                        "entidad_afectada = 'correo_habilitado' AND id_registro_afectado = :v0 "
                                + "AND accion = 'ACTUALIZAR' AND campo_modificado = 'id_equipo_invitado' AND valor_anterior IS NULL "
                                + "AND valor_nuevo = :v1",
                        idCorreo,
                        String.valueOf(idA)))
                .isEqualTo(1);

        HttpResponse<String> lista = docente.get("/api/proyectos/" + id + "/estudiantes");
        assertThat(lista.statusCode()).as(lista.body()).isEqualTo(200);
        assertThat(lista.body())
                .contains("{\"email\":\"" + ines + "\",\"nombre\":null,\"registrado\":false,\"idEquipo\":null,"
                        + "\"idEquipoInvitado\":" + idA + "}")
                .contains("{\"email\":\"" + libre + "\",\"nombre\":null,\"registrado\":false,\"idEquipo\":null,"
                        + "\"idEquipoInvitado\":null}");

        registrar(docente, id, ines, "Ines Prueba");
        long idInes = idUsuario(ines);
        assertThat(jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_equipo = :e AND id_usuario = :u "
                                + "AND rol_equipo = 'INTEGRANTE'")
                        .param("e", idA)
                        .param("u", idInes)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(equipoInvitado(id, ines)).isNull();
        assertThat(auditoria(
                        "entidad_afectada = 'miembro_equipo' AND id_registro_afectado = :v0 AND accion = 'CREAR' "
                                + "AND campo_modificado = 'id_usuario' AND valor_anterior IS NULL AND valor_nuevo = :v1 "
                                + "AND id_usuario_accion = :v2",
                        idA,
                        String.valueOf(idInes),
                        idInes))
                .isEqualTo(1);
        assertThat(docente.get("/api/proyectos/" + id).body()).contains("\"nombre\":\"Ines Prueba\"");
        assertThat(docente.get("/api/proyectos/" + id + "/estudiantes").body())
                .contains("{\"email\":\"" + ines + "\",\"nombre\":\"Ines Prueba\",\"registrado\":true,\"idEquipo\":"
                        + idA + ",\"idEquipoInvitado\":null}");
        assertThat(sesion(ines).get("/api/proyectos/" + id).statusCode()).isEqualTo(200);
        assertThat(auditoria("coalesce(valor_anterior, '') || coalesce(valor_nuevo, '') LIKE '%@%'"))
                .isZero();
    }

    @Test
    void noSeInvitaFueraDeLasReglas() {
        Navegador docente = docente();
        long id = crearProyecto(docente, true);
        long idA = crearEquipo(docente, id, "Equipo A");
        long idB = crearEquipo(docente, id, "Equipo B");
        String invitada = correoUnico("est");
        String conCuenta = correoUnico("est");
        String monitor = correoUnico("mon");
        habilitar(docente, id, "ESTUDIANTE", invitada, conCuenta);
        habilitar(docente, id, "MONITOR", monitor);
        registrar(docente, id, conCuenta, "Con Cuenta");

        HttpResponse<String> noHabilitado =
                docente.post("/api/equipos/" + idA + "/invitaciones", invitacion(correoUnico("nadie")));
        assertThat(noHabilitado.statusCode()).isEqualTo(409);
        assertThat(noHabilitado.body()).contains("\"codigo\":\"CORREO_NO_HABILITADO\"");

        HttpResponse<String> otroRol = docente.post("/api/equipos/" + idA + "/invitaciones", invitacion(monitor));
        assertThat(otroRol.statusCode()).isEqualTo(409);
        assertThat(otroRol.body()).contains("\"codigo\":\"CORREO_NO_HABILITADO\"");

        HttpResponse<String> registrado = docente.post("/api/equipos/" + idA + "/invitaciones", invitacion(conCuenta));
        assertThat(registrado.statusCode()).isEqualTo(409);
        assertThat(registrado.body()).contains("\"codigo\":\"YA_TIENE_CUENTA\"");

        assertThat(docente.post("/api/equipos/" + idA + "/invitaciones", invitacion(invitada))
                        .statusCode())
                .isEqualTo(201);
        HttpResponse<String> otroEquipo = docente.post("/api/equipos/" + idB + "/invitaciones", invitacion(invitada));
        assertThat(otroEquipo.statusCode()).isEqualTo(409);
        assertThat(otroEquipo.body()).contains("\"codigo\":\"YA_INVITADO\"");
        assertThat(equipoInvitado(id, invitada)).isEqualTo(idA);

        HttpResponse<String> invalido =
                docente.post("/api/equipos/" + idA + "/invitaciones", invitacion("no-es-correo"));
        assertThat(invalido.statusCode()).isEqualTo(400);
        assertThat(invalido.body()).contains("\"codigo\":\"VALIDACION\"");
    }

    @Test
    void siLaInvitacionYaNoSePuedeCumplirLaCuentaSeCreaIgual() {
        Navegador docente = docente();
        long id = crearProyecto(docente, false);
        long idA = crearEquipo(docente, id, "Equipo A");
        long otroProyecto = crearProyecto(docente, false);
        long ajeno = crearEquipo(docente, otroProyecto, "Equipo Ajeno");
        String ines = correoUnico("ines");
        habilitar(docente, id, "ESTUDIANTE", ines);
        assertThat(docente.post("/api/equipos/" + idA + "/invitaciones", invitacion(ines))
                        .statusCode())
                .isEqualTo(201);
        // La regla del equipo deja de cumplirse: la invitación apunta a un equipo que no es del proyecto.
        jdbc.sql("UPDATE correo_habilitado SET id_equipo_invitado = :e WHERE id_proyecto = :p AND email = :c")
                .param("e", ajeno)
                .param("p", id)
                .param("c", ines)
                .update();

        registrar(docente, id, ines, "Ines Prueba");
        long idInes = idUsuario(ines);
        assertThat(jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_usuario = :u")
                        .param("u", idInes)
                        .query(Integer.class)
                        .single())
                .isZero();
        assertThat(equipoInvitado(id, ines)).isNull();
        long idCorreo = jdbc.sql(
                        "SELECT id_correo_habilitado FROM correo_habilitado WHERE id_proyecto = :p AND email = :e")
                .param("p", id)
                .param("e", ines)
                .query(Long.class)
                .single();
        assertThat(auditoria(
                        "entidad_afectada = 'correo_habilitado' AND id_registro_afectado = :v0 "
                                + "AND accion = 'ACTUALIZAR' AND campo_modificado = 'id_equipo_invitado' AND valor_anterior = :v1 "
                                + "AND valor_nuevo IS NULL AND id_usuario_accion = :v2",
                        idCorreo,
                        String.valueOf(ajeno),
                        idInes))
                .isEqualTo(1);
        assertThat(auditoria("entidad_afectada = 'miembro_equipo' AND valor_nuevo = :v0", String.valueOf(idInes)))
                .isZero();
        assertThat(docente.get("/api/proyectos/" + id + "/estudiantes").body())
                .contains("\"registrado\":true,\"idEquipo\":null,\"idEquipoInvitado\":null");
    }

    @Test
    void soloLaDocenteDelProyectoInvitaYLista() {
        Navegador docente = docente();
        long id = crearProyecto(docente, false);
        long idA = crearEquipo(docente, id, "Equipo A");
        String ines = correoUnico("ines");
        String ana = correoUnico("ana");
        habilitar(docente, id, "ESTUDIANTE", ines, ana);
        registrar(docente, id, ana, "Ana Prueba");
        assertThat(docente.post("/api/equipos/" + idA + "/miembros", invitacion(ana))
                        .statusCode())
                .isEqualTo(201);

        Navegador otraDocente = docente();
        Navegador estudiante = sesion(ana);
        for (Navegador n : new Navegador[] {otraDocente, estudiante}) {
            HttpResponse<String> invitar = n.post("/api/equipos/" + idA + "/invitaciones", invitacion(ines));
            assertThat(invitar.statusCode()).isEqualTo(403);
            assertThat(invitar.body()).contains("\"codigo\":\"ACCESO_DENEGADO\"");
            assertThat(n.get("/api/proyectos/" + id + "/estudiantes").statusCode())
                    .isEqualTo(403);
        }
        assertThat(docente.post("/api/equipos/999999999/invitaciones", invitacion(ines))
                        .statusCode())
                .isEqualTo(403);
        assertThat(docente.get("/api/proyectos/999999999/estudiantes").statusCode())
                .isEqualTo(403);
        assertThat(equipoInvitado(id, ines)).isNull();
    }
}
