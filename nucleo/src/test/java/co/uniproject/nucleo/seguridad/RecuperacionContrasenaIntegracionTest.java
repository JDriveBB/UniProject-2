package co.uniproject.nucleo.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.datos.ContrasenaCambiada;
import co.uniproject.eventos.datos.OrigenCambioContrasena;
import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.RecuperacionSolicitada;
import co.uniproject.eventos.interno.ContratoInterno;
import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.seguridad.cuentas.LimpiezaRecuperacion;
import co.uniproject.nucleo.seguridad.cuentas.ServicioSupresion;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Bloque 28: cambio con sesión y recuperación por Telegram. Nunca se usa .as(cuerpo): el cuerpo puede traer el código. */
@ExtendWith(OutputCaptureExtension.class)
class RecuperacionContrasenaIntegracionTest extends ApiIntegracionBase {

    private static final String TOKEN = "token-de-prueba";
    private static final String NUEVA = "NuevaClave2026!";
    private static final Pattern CODIGO = Pattern.compile("\"codigo\":\"([A-Za-z0-9_-]{43})\"");

    @Autowired
    private CodecEventos codec;

    @Autowired
    private ServicioSupresion supresion;

    @Autowired
    private LimpiezaRecuperacion limpieza;

    private final HttpClient cliente = clienteHttp();

    private record Cuenta(long id, String email, long chat) {}

    @BeforeEach
    void sinLimites() {
        jdbc.sql("DELETE FROM limite_recuperacion").update();
    }

    private Cuenta cuentaConTelegram(String prefijo) {
        String email = correoUnico(prefijo);
        long id = crearUsuario("Titular Recuperacion", email, "ESTUDIANTE");
        long chat = 720000000L + id;
        jdbc.sql("UPDATE usuario SET telegram_chat_id = :c WHERE id_usuario = :id")
                .param("c", chat)
                .param("id", id)
                .update();
        return new Cuenta(id, email, chat);
    }

    private Navegador anonimo() {
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        return n;
    }

    private HttpResponse<String> pedir(String email) {
        return anonimo().post("/api/cuentas/recuperacion", "{\"email\":\"" + email + "\"}");
    }

    private HttpResponse<String> restablecer(String codigo, String nueva) {
        return anonimo()
                .post(
                        "/api/cuentas/recuperacion/restablecer",
                        "{\"codigo\":\"" + codigo + "\",\"contrasenaNueva\":\"" + nueva + "\"}");
    }

    private static HttpResponse<String> cambiar(Navegador n, String actual, String nueva) {
        return n.post(
                "/api/perfil/contrasena",
                "{\"contrasenaActual\":\"" + actual + "\",\"contrasenaNueva\":\"" + nueva + "\"}");
    }

    private int ingresar(String email, String contrasena) {
        return anonimo()
                .post("/api/sesion/login", "{\"email\":\"" + email + "\",\"contrasena\":\"" + contrasena + "\"}")
                .statusCode();
    }

    private static String json(UUID solicitud, long chat) {
        return "{\"solicitud\":\"" + solicitud + "\",\"chatId\":" + chat + "}";
    }

    private HttpResponse<String> interno(String cuerpo, String token) {
        HttpRequest.Builder b = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + puerto + ContratoInterno.RUTA_RECUPERACIONES))
                .timeout(ESPERA_HTTP)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo));
        if (token != null) {
            b.header(ContratoInterno.CABECERA_TOKEN, token);
        }
        try {
            return cliente.send(b.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private String codigo(UUID solicitud, long chat) {
        HttpResponse<String> r = interno(json(solicitud, chat), TOKEN);
        assertThat(r.statusCode()).isEqualTo(200);
        Matcher m = CODIGO.matcher(r.body());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private UUID solicitudActiva(long idUsuario) {
        return jdbc.sql("SELECT id FROM recuperacion_contrasena WHERE usuario_id = :id AND usada_en IS NULL "
                        + "AND anulada_en IS NULL")
                .param("id", idUsuario)
                .query((rs, fila) -> rs.getObject("id", UUID.class))
                .single();
    }

    private int contar(String sql, long id) {
        return jdbc.sql(sql).param("id", id).query(Integer.class).single();
    }

    private String carga(String tipo, long idTitular) {
        return jdbc.sql("SELECT carga::text FROM evento_saliente WHERE tipo = :tipo "
                        + "AND (carga->'data'->'titular'->>'id')::bigint = :id")
                .param("tipo", tipo)
                .param("id", idTitular)
                .query(String.class)
                .single();
    }

    /** Mismo selector, verificador con un bit cambiado. */
    private static String alterar(String codigo) {
        byte[] b = Base64.getUrlDecoder().decode(codigo);
        b[b.length - 1] ^= 1;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    @Test
    void respondeIgualExistaONoLaCuenta() {
        Cuenta conTelegram = cuentaConTelegram("rectg");
        String sinTelegram = correoUnico("recsin");
        long idSin = crearUsuario("Sin Telegram", sinTelegram, "ESTUDIANTE");
        Cuenta suprimida = cuentaConTelegram("recsup");
        supresion.suprimir(suprimida.id(), CLAVE);

        List<HttpResponse<String>> respuestas = List.of(
                pedir(correoUnico("recnadie")),
                pedir(sinTelegram),
                pedir(suprimida.email()),
                pedir("  " + conTelegram.email().toUpperCase(Locale.ROOT) + " "));

        assertThat(respuestas.stream().map(HttpResponse::statusCode).toList()).containsExactly(202, 202, 202, 202);
        assertThat(respuestas.stream().map(HttpResponse::body).toList()).containsExactly("", "", "", "");
        assertThat(contar("SELECT count(*) FROM recuperacion_contrasena WHERE usuario_id = :id", idSin))
                .isZero();
        assertThat(contar("SELECT count(*) FROM recuperacion_contrasena WHERE usuario_id = :id", suprimida.id()))
                .isZero();
        assertThat(contar(
                        "SELECT count(*) FROM recuperacion_contrasena WHERE usuario_id = :id AND selector IS NULL "
                                + "AND usada_en IS NULL AND anulada_en IS NULL",
                        conTelegram.id()))
                .isEqualTo(1);
        String carga = carga("cuenta.recuperacion.solicitada", conTelegram.id());
        RecuperacionSolicitada datos = (RecuperacionSolicitada)
                codec.leer(carga.getBytes(StandardCharsets.UTF_8)).data();
        assertThat(datos.titular())
                .isEqualTo(new Persona(conTelegram.id(), "Titular Recuperacion", conTelegram.chat()));
        assertThat(datos.solicitud()).isEqualTo(solicitudActiva(conTelegram.id()));
        assertThat(carga)
                .doesNotContain("\"codigo\"")
                .doesNotContain("selector")
                .doesNotContain("verificador")
                .doesNotContain(conTelegram.email());
        assertThat(contar("SELECT count(*) FROM notificacion WHERE id_destinatario = :id", conTelegram.id()))
                .isZero();
        assertThat(contar(
                        "SELECT count(*) FROM log_auditoria WHERE accion = 'SOLICITAR_RECUPERACION' "
                                + "AND id_registro_afectado = :id AND id_usuario_accion IS NULL AND campo_modificado IS NULL "
                                + "AND valor_anterior IS NULL AND valor_nuevo IS NULL",
                        conTelegram.id()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*) FROM log_auditoria WHERE accion = 'SOLICITAR_RECUPERACION' "
                                + "AND id_registro_afectado = :id",
                        idSin))
                .isZero();
    }

    @Test
    void restableceUnaSolaVezCierraLasSesionesYAvisa(CapturedOutput salida) {
        Cuenta c = cuentaConTelegram("recok");
        Navegador abierta = sesion(c.email());
        assertThat(pedir(c.email()).statusCode()).isEqualTo(202);
        UUID solicitud = solicitudActiva(c.id());
        String codigo = codigo(solicitud, c.chat());

        assertThat(restablecer(codigo, NUEVA).statusCode()).isEqualTo(204);

        assertThat(abierta.get("/api/sesion/yo").statusCode()).isEqualTo(401);
        assertThat(ingresar(c.email(), CLAVE)).isEqualTo(401);
        assertThat(ingresar(c.email(), NUEVA)).isEqualTo(200);
        HttpResponse<String> reuso = restablecer(codigo, "OtraClave2026!");
        assertThat(reuso.statusCode()).isEqualTo(400);
        assertThat(reuso.body()).contains("\"codigo\":\"CODIGO_INVALIDO\"");
        assertThat(interno(json(solicitud, c.chat()), TOKEN).statusCode()).isEqualTo(410);
        assertThat(jdbc.sql("SELECT password_hash FROM usuario WHERE id_usuario = :id")
                        .param("id", c.id())
                        .query(String.class)
                        .single())
                .startsWith("$2a$12$");
        String carga = carga("cuenta.contrasena.cambiada", c.id());
        ContrasenaCambiada cambio = (ContrasenaCambiada)
                codec.leer(carga.getBytes(StandardCharsets.UTF_8)).data();
        assertThat(cambio.origen()).isEqualTo(OrigenCambioContrasena.RECUPERACION);
        assertThat(cambio.titular().chatId()).isEqualTo(c.chat());
        assertThat(jdbc.sql("SELECT count(*) FROM evento_saliente WHERE strpos(carga::text, :c) > 0 "
                                + "OR strpos(carga::text, :n) > 0")
                        .param("c", codigo)
                        .param("n", NUEVA)
                        .query(Integer.class)
                        .single())
                .isZero();
        assertThat(contar(
                        "SELECT count(*) FROM log_auditoria WHERE accion = 'RESTABLECER_CONTRASENA' "
                                + "AND id_registro_afectado = :id AND id_usuario_accion = :id AND valor_anterior IS NULL "
                                + "AND valor_nuevo IS NULL",
                        c.id()))
                .isEqualTo(1);
        assertThat(salida.getAll()).doesNotContain(codigo).doesNotContain(NUEVA).doesNotContain(c.email());
    }

    @Test
    void caducidadYNuevaSolicitudAnulanElCodigo() {
        Cuenta c = cuentaConTelegram("reccad");
        pedir(c.email());
        UUID primera = solicitudActiva(c.id());
        String codigoPrimera = codigo(primera, c.chat());
        pedir(c.email());
        UUID segunda = solicitudActiva(c.id());
        assertThat(segunda).isNotEqualTo(primera);
        assertThat(restablecer(codigoPrimera, NUEVA).body()).contains("\"codigo\":\"CODIGO_INVALIDO\"");
        assertThat(interno(json(primera, c.chat()), TOKEN).statusCode()).isEqualTo(410);

        String codigoSegunda = codigo(segunda, c.chat());
        jdbc.sql("UPDATE recuperacion_contrasena SET creada_en = now() - interval '20 minutes', "
                        + "vence_en = now() - interval '5 minutes' WHERE id = :id")
                .param("id", segunda)
                .update();
        HttpResponse<String> vencido = restablecer(codigoSegunda, NUEVA);
        assertThat(vencido.statusCode()).isEqualTo(400);
        assertThat(vencido.body()).contains("\"codigo\":\"CODIGO_INVALIDO\"");
        assertThat(interno(json(segunda, c.chat()), TOKEN).statusCode()).isEqualTo(410);
        assertThat(ingresar(c.email(), CLAVE)).isEqualTo(200);

        jdbc.sql("UPDATE recuperacion_contrasena SET creada_en = now() - interval '3 days', "
                        + "vence_en = now() - interval '2 days' WHERE usuario_id = :id")
                .param("id", c.id())
                .update();
        limpieza.limpiar();
        assertThat(contar("SELECT count(*) FROM recuperacion_contrasena WHERE usuario_id = :id", c.id()))
                .isZero();
    }

    @Test
    void limitaLasEntregasYLosIntentosFallidos() {
        Cuenta c = cuentaConTelegram("recint");
        pedir(c.email());
        UUID s = solicitudActiva(c.id());
        String primero = codigo(s, c.chat());
        codigo(s, c.chat());
        String ultimo = codigo(s, c.chat());
        assertThat(interno(json(s, c.chat()), TOKEN).statusCode()).isEqualTo(410);
        assertThat(restablecer(primero, NUEVA).body()).contains("\"codigo\":\"CODIGO_INVALIDO\"");

        String alterado = alterar(ultimo);
        for (int i = 0; i < 5; i++) {
            assertThat(restablecer(alterado, NUEVA).statusCode()).isEqualTo(400);
        }
        assertThat(restablecer(ultimo, NUEVA).body()).contains("\"codigo\":\"CODIGO_INVALIDO\"");
        assertThat(jdbc.sql("SELECT intentos_fallidos FROM recuperacion_contrasena WHERE id = :id")
                        .param("id", s)
                        .query(Integer.class)
                        .single())
                .isEqualTo(5);
        assertThat(ingresar(c.email(), CLAVE)).isEqualTo(200);
    }

    @Test
    void limitaPorCorreoPorIpYLosCanjesSinRevelarLaCuenta() {
        String nadie = correoUnico("reclim");
        List<Integer> porCorreo =
                IntStream.range(0, 4).mapToObj(i -> pedir(nadie).statusCode()).toList();
        assertThat(porCorreo).containsExactly(202, 202, 202, 429);
        HttpResponse<String> limitado = pedir(nadie);
        assertThat(limitado.body()).contains("\"codigo\":\"DEMASIADAS_SOLICITUDES\"");

        Cuenta c = cuentaConTelegram("reclimtg");
        List<Integer> existente = IntStream.range(0, 4)
                .mapToObj(i -> pedir(c.email()).statusCode())
                .toList();
        assertThat(existente).containsExactly(202, 202, 202, 429);
        assertThat(contar("SELECT count(*) FROM recuperacion_contrasena WHERE usuario_id = :id", c.id()))
                .isEqualTo(3);

        jdbc.sql("DELETE FROM limite_recuperacion").update();
        List<Integer> porIp = IntStream.range(0, 11)
                .mapToObj(i -> pedir(correoUnico("recip")).statusCode())
                .toList();
        assertThat(porIp.subList(0, 10)).containsOnly(202);
        assertThat(porIp.get(10)).isEqualTo(429);

        List<Integer> canjes = IntStream.range(0, 21)
                .mapToObj(i -> restablecer("A".repeat(43), NUEVA).statusCode())
                .toList();
        assertThat(canjes.subList(0, 20)).containsOnly(400);
        assertThat(canjes.get(20)).isEqualTo(429);
        assertThat(jdbc.sql("SELECT count(*) FROM limite_recuperacion "
                                + "WHERE clave !~ '^(correo|ip|canje|cambio):[0-9a-f]{64}$'")
                        .query(Integer.class)
                        .single())
                .isZero();
    }

    @Test
    void cambiaConSesionMantieneEstaYCierraLasDemas() {
        Cuenta c = cuentaConTelegram("reccam");
        Navegador actual = sesion(c.email());
        Navegador otra = sesion(c.email());
        pedir(c.email());
        String pendiente = codigo(solicitudActiva(c.id()), c.chat());

        HttpResponse<String> mala = cambiar(actual, "NoEsLaClave1", NUEVA);
        assertThat(mala.statusCode()).isEqualTo(400);
        assertThat(mala.body()).contains("\"codigo\":\"CONTRASENA_INCORRECTA\"");
        HttpResponse<String> corta = cambiar(actual, CLAVE, "corta");
        assertThat(corta.statusCode()).isEqualTo(400);
        assertThat(corta.body()).contains("\"codigo\":\"CONTRASENA_INVALIDA\"");
        HttpResponse<String> igual = cambiar(actual, CLAVE, CLAVE);
        assertThat(igual.statusCode()).isEqualTo(400);
        assertThat(igual.body()).contains("\"codigo\":\"CONTRASENA_REPETIDA\"");
        HttpResponse<String> vacia = actual.post("/api/perfil/contrasena", "{}");
        assertThat(vacia.statusCode()).isEqualTo(400);
        assertThat(vacia.body()).contains("\"codigo\":\"VALIDACION\"");

        assertThat(cambiar(actual, CLAVE, NUEVA).statusCode()).isEqualTo(204);

        assertThat(actual.get("/api/sesion/yo").statusCode()).isEqualTo(200);
        assertThat(otra.get("/api/sesion/yo").statusCode()).isEqualTo(401);
        assertThat(ingresar(c.email(), NUEVA)).isEqualTo(200);
        assertThat(ingresar(c.email(), CLAVE)).isEqualTo(401);
        assertThat(restablecer(pendiente, "OtraClave2026!").body()).contains("\"codigo\":\"CODIGO_INVALIDO\"");
        ContrasenaCambiada cambio = (ContrasenaCambiada)
                codec.leer(carga("cuenta.contrasena.cambiada", c.id()).getBytes(StandardCharsets.UTF_8))
                        .data();
        assertThat(cambio.origen()).isEqualTo(OrigenCambioContrasena.PERFIL);
        assertThat(contar(
                        "SELECT count(*) FROM log_auditoria WHERE accion = 'CAMBIAR_CONTRASENA' "
                                + "AND id_registro_afectado = :id AND valor_nuevo IS NULL",
                        c.id()))
                .isEqualTo(1);
        assertThat(cambiar(anonimo(), CLAVE, NUEVA).statusCode()).isEqualTo(401);
    }

    @Test
    void elEndpointInternoExigeTokenYElChatVinculado() {
        Cuenta c = cuentaConTelegram("recintx");
        pedir(c.email());
        UUID s = solicitudActiva(c.id());
        assertThat(interno(json(s, c.chat()), null).statusCode()).isEqualTo(401);
        assertThat(interno(json(s, c.chat()), "token-incorrecto").statusCode()).isEqualTo(401);
        assertThat(interno(json(s, c.chat() + 1), TOKEN).statusCode()).isEqualTo(410);
        assertThat(interno(json(UUID.randomUUID(), c.chat()), TOKEN).statusCode())
                .isEqualTo(404);
        assertThat(interno("{\"solicitud\":\"no-es-uuid\",\"chatId\":1}", TOKEN).statusCode())
                .isEqualTo(400);
        assertThat(interno("{\"solicitud\":\"" + s + "\",\"chatId\":0}", TOKEN).statusCode())
                .isEqualTo(400);
        HttpResponse<String> ok = interno(json(s, c.chat()), TOKEN);
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.headers().allValues("Cache-Control").toString()).contains("no-store");

        jdbc.sql("UPDATE usuario SET telegram_chat_id = NULL WHERE id_usuario = :id")
                .param("id", c.id())
                .update();
        assertThat(interno(json(s, c.chat()), TOKEN).statusCode()).isEqualTo(410);

        Cuenta d = cuentaConTelegram("recintsup");
        pedir(d.email());
        UUID sd = solicitudActiva(d.id());
        supresion.suprimir(d.id(), CLAVE);
        assertThat(interno(json(sd, d.chat()), TOKEN).statusCode()).isEqualTo(404);
    }
}
