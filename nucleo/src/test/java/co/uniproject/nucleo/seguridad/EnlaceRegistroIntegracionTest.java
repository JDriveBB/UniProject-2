package co.uniproject.nucleo.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.seguridad.cuentas.EnlaceRegistroVista;
import co.uniproject.nucleo.seguridad.cuentas.EnlacesRegistro;
import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import co.uniproject.nucleo.seguridad.cuentas.SolicitudRegistro;
import co.uniproject.nucleo.trazabilidad.reporte.TextosHistorial;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** T-040 (D-15 5A): crear cuenta solo con un enlace de invitación de un solo uso generado para esa habilitación. */
@ExtendWith(OutputCaptureExtension.class)
class EnlaceRegistroIntegracionTest extends ApiIntegracionBase {

    private static final String CODIGO_INVENTADO = "A".repeat(43);

    @BeforeEach
    void limpiarLimites() {
        jdbc.sql("DELETE FROM limite_recuperacion").update();
    }

    private Navegador docente() {
        String email = correoUnico("docenlace");
        crearUsuario("Docente Enlace", email, "DOCENTE");
        return sesion(email);
    }

    private Navegador administrador() {
        String email = correoUnico("adminenlace");
        crearUsuario("Admin Enlace", email, "ADMINISTRADOR");
        return sesion(email);
    }

    private long habilitarEnCurso(Navegador docente, long idCurso, String email) {
        HttpResponse<String> r =
                docente.post("/api/cursos/" + idCurso + "/correos", "{\"emails\":[\"" + email + "\"]}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idCorreoHabilitado");
    }

    private HttpResponse<String> postRegistro(String email, String codigoJson) {
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        String sufijoCodigo = codigoJson == null ? "" : ",\"codigo\":" + codigoJson;
        return n.post(
                "/api/cuentas/registro",
                "{\"nombre\":\"Estudiante Enlace\",\"email\":\"" + email + "\",\"contrasena\":\"" + CLAVE
                        + "\",\"autorizaTratamientoDatos\":true" + sufijoCodigo + "}");
    }

    private static void exigirRegistroNoDisponible(HttpResponse<String> r) {
        assertThat(r.statusCode()).as(r.body()).isEqualTo(409);
        assertThat(r.body())
                .contains("\"codigo\":\"REGISTRO_NO_DISPONIBLE\"")
                .contains(ServicioCuentas.MENSAJE_REGISTRO_NO_DISPONIBLE);
    }

    @Test
    void rechazaRegistroSinCodigoOConCodigoInventado() {
        Navegador docente = docente();
        long idCurso = crearCurso(docente, "Curso Enlace");
        String email = correoUnico("sinenlace");
        long idCorreo = habilitarEnCurso(docente, idCurso, email);

        // Correo habilitado pero aún sin enlace generado.
        exigirRegistroNoDisponible(postRegistro(email, "\"" + CODIGO_INVENTADO + "\""));

        // Petición sin campo codigo o en blanco -> 400 VALIDACION.
        HttpResponse<String> sinCampo = postRegistro(email, null);
        assertThat(sinCampo.statusCode()).as(sinCampo.body()).isEqualTo(400);
        assertThat(sinCampo.body()).contains("\"codigo\":\"VALIDACION\"").contains("codigo");

        HttpResponse<String> enBlanco = postRegistro(email, "\"   \"");
        assertThat(enBlanco.statusCode()).as(enBlanco.body()).isEqualTo(400);
        assertThat(enBlanco.body()).contains("\"codigo\":\"VALIDACION\"").contains("codigo");

        // Generado el enlace real, un código mal formado, con otro selector o con otro verificador sigue fallando.
        HttpResponse<String> enlace = docente.post("/api/cursos/" + idCurso + "/correos/" + idCorreo + "/enlace", null);
        assertThat(enlace.statusCode()).as(enlace.body()).isEqualTo(200);
        String codigoReal = campoTexto(enlace.body(), "codigo");

        exigirRegistroNoDisponible(postRegistro(email, "\"no-es-base64-de-43-caracteres\""));
        exigirRegistroNoDisponible(postRegistro(email, "\"" + CODIGO_INVENTADO + "\""));

        byte[] bytes = Base64.getUrlDecoder().decode(codigoReal);
        bytes[bytes.length - 1] ^= 0x01;
        String mismoSelectorOtroVerificador =
                Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        exigirRegistroNoDisponible(postRegistro(email, "\"" + mismoSelectorOtroVerificador + "\""));

        assertThat(jdbc.sql("SELECT count(*) FROM usuario WHERE email = :e")
                        .param("e", email)
                        .query(Integer.class)
                        .single())
                .isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM correo_habilitado WHERE id_correo_habilitado = :id "
                                + "AND fecha_uso IS NULL AND enlace_selector IS NOT NULL")
                        .param("id", idCorreo)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void docenteGeneraEnlaceEstudianteSeRegistraYRepetirElCodigoFalla() {
        Navegador docente = docente();
        long idCurso1 = crearCurso(docente, "Curso Enlace 1");
        long idCurso2 = crearCurso(docente, "Curso Enlace 2");
        String email = correoUnico("estok");
        long idCorreo1 = habilitarEnCurso(docente, idCurso1, email);
        long idCorreo2 = habilitarEnCurso(docente, idCurso2, email);
        // También generamos enlace en el segundo curso para comprobar que al registrarse se limpian ambos.
        assertThat(docente.post("/api/cursos/" + idCurso2 + "/correos/" + idCorreo2 + "/enlace", null)
                        .statusCode())
                .isEqualTo(200);

        Instant antes = Instant.now();
        HttpResponse<String> rEnlace =
                docente.post("/api/cursos/" + idCurso1 + "/correos/" + idCorreo1 + "/enlace", null);
        Instant despues = Instant.now();

        assertThat(rEnlace.statusCode()).as(rEnlace.body()).isEqualTo(200);
        assertThat(rEnlace.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        String codigo = campoTexto(rEnlace.body(), "codigo");
        assertThat(codigo).hasSize(43).matches("^[A-Za-z0-9_-]{43}$");
        Instant venceEn = Instant.parse(campoTexto(rEnlace.body(), "venceEn"));
        assertThat(venceEn)
                .isBetween(
                        antes.plus(EnlacesRegistro.VIGENCIA).minusSeconds(5),
                        despues.plus(EnlacesRegistro.VIGENCIA).plusSeconds(5));

        HttpResponse<String> alta = postRegistro(email.toUpperCase(Locale.ROOT), "\"" + codigo + "\"");
        assertThat(alta.statusCode()).as(alta.body()).isEqualTo(201);
        assertThat(alta.body()).contains("\"email\":\"" + email + "\"").contains("\"rol\":\"ESTUDIANTE\"");

        // Las dos habilitaciones quedan usadas y con enlace_* en nulo.
        assertThat(jdbc.sql("SELECT count(*) FROM correo_habilitado WHERE email = :e AND fecha_uso IS NOT NULL "
                                + "AND enlace_selector IS NULL AND enlace_verificador_hash IS NULL "
                                + "AND enlace_vence_en IS NULL")
                        .param("e", email)
                        .query(Integer.class)
                        .single())
                .isEqualTo(2);

        // Repetir el mismo código falla con REGISTRO_NO_DISPONIBLE.
        exigirRegistroNoDisponible(postRegistro(email, "\"" + codigo + "\""));
        String otroHabilitado = correoUnico("otro");
        habilitarEnCurso(docente, idCurso1, otroHabilitado);
        exigirRegistroNoDisponible(postRegistro(otroHabilitado, "\"" + codigo + "\""));
    }

    @Test
    void codigoValidoConOtroCorreoHabilitadoEsRechazadoYNoConsumeElEnlace() {
        Navegador docente = docente();
        long idCurso = crearCurso(docente, "Curso Enlace Otro");
        String emailA = correoUnico("esta");
        String emailB = correoUnico("estb");
        long idCorreoA = habilitarEnCurso(docente, idCurso, emailA);
        habilitarEnCurso(docente, idCurso, emailB);

        String codigoA = campoTexto(
                docente.post("/api/cursos/" + idCurso + "/correos/" + idCorreoA + "/enlace", null)
                        .body(),
                "codigo");

        exigirRegistroNoDisponible(postRegistro(emailB, "\"" + codigoA + "\""));
        assertThat(jdbc.sql("SELECT count(*) FROM usuario WHERE email = :e")
                        .param("e", emailB)
                        .query(Integer.class)
                        .single())
                .isZero();

        // El enlace de A sigue vigente para A.
        HttpResponse<String> altaA = postRegistro(emailA, "\"" + codigoA + "\"");
        assertThat(altaA.statusCode()).as(altaA.body()).isEqualTo(201);
    }

    @Test
    void generarDosVecesAnulaElPrimerCodigo() {
        Navegador docente = docente();
        long idCurso = crearCurso(docente, "Curso Reemplazo");
        String email = correoUnico("reemplazo");
        long idCorreo = habilitarEnCurso(docente, idCurso, email);

        String primero = campoTexto(
                docente.post("/api/cursos/" + idCurso + "/correos/" + idCorreo + "/enlace", null)
                        .body(),
                "codigo");
        String segundo = campoTexto(
                docente.post("/api/cursos/" + idCurso + "/correos/" + idCorreo + "/enlace", null)
                        .body(),
                "codigo");
        assertThat(segundo).isNotEqualTo(primero);

        exigirRegistroNoDisponible(postRegistro(email, "\"" + primero + "\""));
        assertThat(jdbc.sql("SELECT count(*) FROM usuario WHERE email = :e")
                        .param("e", email)
                        .query(Integer.class)
                        .single())
                .isZero();

        HttpResponse<String> alta = postRegistro(email, "\"" + segundo + "\"");
        assertThat(alta.statusCode()).as(alta.body()).isEqualTo(201);
    }

    @Test
    void codigoVencidoEsRechazado() {
        Navegador docente = docente();
        long idCurso = crearCurso(docente, "Curso Vencido");
        String email = correoUnico("vencido");
        long idCorreo = habilitarEnCurso(docente, idCurso, email);

        String codigoVencido = campoTexto(
                docente.post("/api/cursos/" + idCurso + "/correos/" + idCorreo + "/enlace", null)
                        .body(),
                "codigo");
        jdbc.sql("UPDATE correo_habilitado SET enlace_vence_en = now() - interval '1 second' "
                        + "WHERE id_correo_habilitado = :id")
                .param("id", idCorreo)
                .update();

        exigirRegistroNoDisponible(postRegistro(email, "\"" + codigoVencido + "\""));
        assertThat(jdbc.sql("SELECT count(*) FROM usuario WHERE email = :e")
                        .param("e", email)
                        .query(Integer.class)
                        .single())
                .isZero();

        // Un enlace nuevo sobre la misma habilitación vuelve a funcionar.
        String codigoNuevo = campoTexto(
                docente.post("/api/cursos/" + idCurso + "/correos/" + idCorreo + "/enlace", null)
                        .body(),
                "codigo");
        assertThat(postRegistro(email, "\"" + codigoNuevo + "\"").statusCode()).isEqualTo(201);
    }

    @Test
    void validaPermisosProyectoCorrectoYHabilitacionSinUsar() {
        Navegador docenteA = docente();
        long idA = crearCurso(docenteA, "Curso A");
        String emailA = correoUnico("proya");
        long idCorreoA = habilitarEnCurso(docenteA, idA, emailA);

        Navegador docenteB = docente();
        long idB = crearCurso(docenteB, "Curso B");
        long idCorreoB = habilitarEnCurso(docenteB, idB, correoUnico("proyb"));

        // Otro docente (no del curso) -> 403.
        HttpResponse<String> otroDocente =
                docenteB.post("/api/cursos/" + idA + "/correos/" + idCorreoA + "/enlace", null);
        assertThat(otroDocente.statusCode()).as(otroDocente.body()).isEqualTo(403);
        assertThat(otroDocente.body()).contains("\"codigo\":\"ACCESO_DENEGADO\"");

        // Curso inexistente -> 403 (Permisos).
        assertThat(docenteA.post("/api/cursos/999999999/correos/" + idCorreoA + "/enlace", null)
                        .statusCode())
                .isEqualTo(403);

        // Id de otro curso o inexistente -> 404.
        HttpResponse<String> otroCurso =
                docenteA.post("/api/cursos/" + idA + "/correos/" + idCorreoB + "/enlace", null);
        assertThat(otroCurso.statusCode()).as(otroCurso.body()).isEqualTo(404);
        assertThat(otroCurso.body()).contains("\"codigo\":\"NO_ENCONTRADO\"");

        HttpResponse<String> inexistente = docenteA.post("/api/cursos/" + idA + "/correos/999999999/enlace", null);
        assertThat(inexistente.statusCode()).as(inexistente.body()).isEqualTo(404);

        // Habilitación ya usada -> 409 HABILITACION_USADA.
        String codigoA = enlaceCurso(docenteA, idA, emailA);
        assertThat(postRegistro(emailA, "\"" + codigoA + "\"").statusCode()).isEqualTo(201);

        HttpResponse<String> usada = docenteA.post("/api/cursos/" + idA + "/correos/" + idCorreoA + "/enlace", null);
        assertThat(usada.statusCode()).as(usada.body()).isEqualTo(409);
        assertThat(usada.body())
                .contains("\"codigo\":\"HABILITACION_USADA\"")
                .contains("Esa persona ya creó su cuenta. Quítala del curso");
    }

    @Test
    void administradorGeneraEnlaceSoloParaDocentes() {
        Navegador admin = administrador();
        String emailDoc = correoUnico("docadmin");
        HttpResponse<String> rHab = admin.post("/api/admin/docentes", "{\"email\":\"" + emailDoc + "\"}");
        assertThat(rHab.statusCode()).as(rHab.body()).isEqualTo(201);
        long idHabDoc = numero(rHab.body(), "idCorreoHabilitado");

        HttpResponse<String> rEnlace = admin.post("/api/admin/docentes/" + idHabDoc + "/enlace", null);
        assertThat(rEnlace.statusCode()).as(rEnlace.body()).isEqualTo(200);
        assertThat(rEnlace.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        String codigoDoc = campoTexto(rEnlace.body(), "codigo");
        assertThat(codigoDoc).hasSize(43);

        HttpResponse<String> altaDoc = postRegistro(emailDoc, "\"" + codigoDoc + "\"");
        assertThat(altaDoc.statusCode()).as(altaDoc.body()).isEqualTo(201);
        assertThat(altaDoc.body()).contains("\"rol\":\"DOCENTE\"");

        // Ya usada -> 409 HABILITACION_USADA.
        HttpResponse<String> usada = admin.post("/api/admin/docentes/" + idHabDoc + "/enlace", null);
        assertThat(usada.statusCode()).as(usada.body()).isEqualTo(409);
        assertThat(usada.body()).contains("\"codigo\":\"HABILITACION_USADA\"");

        // Habilitación de estudiante -> 404 NO_ENCONTRADO; inexistente -> 404 NO_ENCONTRADO.
        Navegador docente = sesion(emailDoc);
        long idCurso = crearCurso(docente, "Curso Admin");
        long idHabEst = habilitarEnCurso(docente, idCurso, correoUnico("estadmin"));
        HttpResponse<String> sobreEstudiante = admin.post("/api/admin/docentes/" + idHabEst + "/enlace", null);
        assertThat(sobreEstudiante.statusCode()).as(sobreEstudiante.body()).isEqualTo(404);
        assertThat(sobreEstudiante.body()).contains("\"codigo\":\"NO_ENCONTRADO\"");
        assertThat(admin.post("/api/admin/docentes/999999999/enlace", null).statusCode())
                .isEqualTo(404);

        // Un docente no puede usar el endpoint de administración -> 403 ACCESO_DENEGADO.
        assertThat(docente.post("/api/admin/docentes/" + idHabDoc + "/enlace", null)
                        .statusCode())
                .isEqualTo(403);
    }

    @Test
    void noGuardaElCodigoEnLaBaseNiEnAuditoriaLogsOToString(CapturedOutput salida) throws Exception {
        Navegador docente = docente();
        long idCurso = crearCurso(docente, "Curso Secreto");
        long idDocente = jdbc.sql("SELECT id_docente FROM curso WHERE id_curso = :c")
                .param("c", idCurso)
                .query(Long.class)
                .single();
        String email = correoUnico("secreto");
        long idCorreo = habilitarEnCurso(docente, idCurso, email);

        HttpResponse<String> rEnlace =
                docente.post("/api/cursos/" + idCurso + "/correos/" + idCorreo + "/enlace", null);
        assertThat(rEnlace.statusCode()).as(rEnlace.body()).isEqualTo(200);
        String codigo = campoTexto(rEnlace.body(), "codigo");
        Instant venceEn = Instant.parse(campoTexto(rEnlace.body(), "venceEn"));

        byte[] decodificado = Base64.getUrlDecoder().decode(codigo);
        byte[] selectorEsperado = Arrays.copyOfRange(decodificado, 0, 8);
        byte[] hashEsperado =
                MessageDigest.getInstance("SHA-256").digest(Arrays.copyOfRange(decodificado, 8, decodificado.length));

        byte[] selectorGuardado = jdbc.sql(
                        "SELECT enlace_selector FROM correo_habilitado WHERE id_correo_habilitado = :id")
                .param("id", idCorreo)
                .query(byte[].class)
                .single();
        byte[] hashGuardado = jdbc.sql(
                        "SELECT enlace_verificador_hash FROM correo_habilitado WHERE id_correo_habilitado = :id")
                .param("id", idCorreo)
                .query(byte[].class)
                .single();
        assertThat(selectorGuardado).hasSize(8).isEqualTo(selectorEsperado);
        assertThat(hashGuardado).hasSize(32).isEqualTo(hashEsperado);
        assertThat(jdbc.sql("SELECT count(*) FROM correo_habilitado WHERE id_correo_habilitado = :id "
                                + "AND (position(:c in encode(enlace_selector, 'escape')) > 0 "
                                + "OR position(:c in encode(enlace_verificador_hash, 'escape')) > 0)")
                        .param("id", idCorreo)
                        .param("c", codigo)
                        .query(Integer.class)
                        .single())
                .isZero();

        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'correo_habilitado' "
                                + "AND id_registro_afectado = :id AND accion = 'GENERAR_ENLACE_REGISTRO' "
                                + "AND id_usuario_accion = :doc AND campo_modificado IS NULL "
                                + "AND valor_anterior IS NULL AND valor_nuevo IS NULL")
                        .param("id", idCorreo)
                        .param("doc", idDocente)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE strpos(coalesce(campo_modificado, '') "
                                + "|| coalesce(valor_anterior, '') || coalesce(valor_nuevo, ''), :c) > 0 "
                                + "OR strpos(coalesce(campo_modificado, '') || coalesce(valor_anterior, '') "
                                + "|| coalesce(valor_nuevo, ''), :e) > 0")
                        .param("c", codigo)
                        .param("e", email)
                        .query(Integer.class)
                        .single())
                .isZero();

        assertThat(TextosHistorial.accion("GENERAR_ENLACE_REGISTRO", "correo_habilitado"))
                .isEqualTo("Generó un enlace de registro · correo_habilitado");
        assertThat(new EnlaceRegistroVista(codigo, venceEn).toString()).doesNotContain(codigo);
        assertThat(new SolicitudRegistro("Nombre", email, CLAVE, true, codigo).toString())
                .doesNotContain(codigo)
                .doesNotContain(CLAVE);
        assertThat(salida.getAll()).doesNotContain(codigo).doesNotContain(email);
    }
}
