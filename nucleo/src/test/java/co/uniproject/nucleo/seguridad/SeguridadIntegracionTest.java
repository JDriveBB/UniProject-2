package co.uniproject.nucleo.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.seguridad.cuentas.EnlacesRegistro;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeguridadIntegracionTest {

    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6");
    private static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.3.7-management");
    private static final String ADMIN = "{\"email\":\"Admin@UniProject.test\",\"contrasena\":\"ClaveAdmin2026\"}";
    private static final String CODIGO_FICTICIO = "A".repeat(43);

    static {
        postgres.start();
        rabbit.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("uniproject.token-interno", () -> "token-de-prueba");
        registry.add("uniproject.outbox.intervalo-ms", () -> 600000L);
        registry.add("uniproject.admin.email", () -> "admin@uniproject.test");
        registry.add("uniproject.admin.nombre", () -> "Admin Prueba");
        registry.add("uniproject.admin.contrasena", () -> "ClaveAdmin2026");
    }

    @Value("${local.server.port}")
    private int puerto;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EnlacesRegistro enlaces;

    /** Cliente con cookies propias: guarda Set-Cookie y manda X-XSRF-TOKEN como lo hará el SPA. */
    static final class Navegador {

        private final HttpClient cliente = ApiIntegracionBase.clienteHttp();
        private final Map<String, String> cookies = new LinkedHashMap<>();
        private final String base;

        Navegador(int puerto) {
            this.base = "http://localhost:" + puerto;
        }

        HttpResponse<String> get(String ruta) {
            return enviar("GET", ruta, null);
        }

        HttpResponse<String> post(String ruta, String json) {
            return enviar("POST", ruta, json);
        }

        void pedirCsrf() {
            assertThat(get("/api/sesion/csrf").statusCode()).isEqualTo(204);
            assertThat(cookies).containsKey("XSRF-TOKEN");
        }

        String cookie(String nombre) {
            return cookies.get(nombre);
        }

        void ponerCookie(String nombre, String valor) {
            cookies.put(nombre, valor);
        }

        private HttpResponse<String> enviar(String metodo, String ruta, String json) {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + ruta))
                    .timeout(ApiIntegracionBase.ESPERA_HTTP)
                    .method(
                            metodo,
                            json == null
                                    ? HttpRequest.BodyPublishers.noBody()
                                    : HttpRequest.BodyPublishers.ofString(json));
            if (json != null) {
                b.header("Content-Type", "application/json");
            }
            if (!cookies.isEmpty()) {
                b.header(
                        "Cookie",
                        cookies.entrySet().stream()
                                .map(e -> e.getKey() + "=" + e.getValue())
                                .collect(Collectors.joining("; ")));
            }
            if (cookies.containsKey("XSRF-TOKEN")) {
                b.header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"));
            }
            try {
                HttpResponse<String> r = cliente.send(b.build(), HttpResponse.BodyHandlers.ofString());
                r.headers().allValues("set-cookie").forEach(this::guardar);
                return r;
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        private void guardar(String setCookie) {
            String par = setCookie.split(";", 2)[0];
            int igual = par.indexOf('=');
            String nombre = par.substring(0, igual);
            String valor = par.substring(igual + 1);
            if (valor.isEmpty() || setCookie.toLowerCase(Locale.ROOT).contains("max-age=0")) {
                cookies.remove(nombre);
            } else {
                cookies.put(nombre, valor);
            }
        }
    }

    private Navegador comoAdmin() {
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        assertThat(n.post("/api/sesion/login", ADMIN).statusCode()).isEqualTo(200);
        n.pedirCsrf();
        return n;
    }

    private static String registro(String nombre, String email, String contrasena, boolean autoriza, String codigo) {
        return "{\"nombre\":\"" + nombre + "\",\"email\":\"" + email + "\",\"contrasena\":\"" + contrasena
                + "\",\"autorizaTratamientoDatos\":" + autoriza + ",\"codigo\":\"" + codigo + "\"}";
    }

    private String enlaceProyecto(long idProyecto, String email) {
        long idCorreo = jdbc.sql("SELECT id_correo_habilitado FROM correo_habilitado "
                        + "WHERE id_proyecto = :p AND email = :e AND fecha_uso IS NULL")
                .param("p", idProyecto)
                .param("e", email)
                .query(Long.class)
                .single();
        return enlaces.generarParaProyecto(9101L, idProyecto, idCorreo).codigo();
    }

    @Test
    void sinSesionLaApiResponde401() {
        Navegador n = new Navegador(puerto);
        assertThat(n.get("/api/sesion/yo").statusCode()).isEqualTo(401);
        assertThat(n.get("/api/admin/docentes").statusCode()).isEqualTo(401);
        assertThat(n.get("/actuator/health").statusCode()).isEqualTo(200);
        assertThat(n.cookie("JSESSIONID")).isNull();
    }

    @Test
    void postSinCsrfResponde403() {
        HttpResponse<String> r = new Navegador(puerto).post("/api/sesion/login", ADMIN);
        assertThat(r.statusCode()).isEqualTo(403);
        assertThat(r.body()).contains("\"codigo\":\"CSRF_INVALIDO\"");
    }

    @Test
    void elAdministradorInicialHabilitaDocentes() {
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        HttpResponse<String> login = n.post("/api/sesion/login", ADMIN);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("\"rol\":\"ADMINISTRADOR\"").contains("\"email\":\"admin@uniproject.test\"");
        assertThat(n.cookie("XSRF-TOKEN")).isNull();
        n.pedirCsrf();
        HttpResponse<String> alta = n.post("/api/admin/docentes", "{\"email\":\" Docente.Nuevo@U.edu.co \"}");
        assertThat(alta.statusCode()).isEqualTo(201);
        assertThat(alta.body()).contains("\"email\":\"docente.nuevo@u.edu.co\"").contains("\"registrado\":false");
        HttpResponse<String> repetida = n.post("/api/admin/docentes", "{\"email\":\"docente.nuevo@u.edu.co\"}");
        assertThat(repetida.statusCode()).isEqualTo(409);
        assertThat(repetida.body()).contains("\"codigo\":\"YA_HABILITADO\"");
        assertThat(n.get("/api/admin/docentes").body()).contains("docente.nuevo@u.edu.co");
    }

    @Test
    void docenteSeRegistraEntraYSale() {
        Navegador admin = comoAdmin();
        HttpResponse<String> hab = admin.post("/api/admin/docentes", "{\"email\":\"docente.registro@u.edu.co\"}");
        assertThat(hab.statusCode()).isEqualTo(201);
        long idHab = ApiIntegracionBase.numero(hab.body(), "idCorreoHabilitado");
        HttpResponse<String> enlace = admin.post("/api/admin/docentes/" + idHab + "/enlace", null);
        assertThat(enlace.statusCode()).isEqualTo(200);
        String codigo = ApiIntegracionBase.campoTexto(enlace.body(), "codigo");
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        String cuerpo = registro("Docente Registro", "Docente.Registro@U.edu.co", "ClaveDocente2026", true, codigo);
        HttpResponse<String> alta = n.post("/api/cuentas/registro", cuerpo);
        assertThat(alta.statusCode()).isEqualTo(201);
        assertThat(alta.body()).contains("\"rol\":\"DOCENTE\"").contains("\"telegramVinculado\":false");
        HttpResponse<String> repetido = n.post("/api/cuentas/registro", cuerpo);
        assertThat(repetido.statusCode()).isEqualTo(409);
        assertThat(repetido.body()).contains("\"codigo\":\"REGISTRO_NO_DISPONIBLE\"");

        HttpResponse<String> errada = n.post(
                "/api/sesion/login", "{\"email\":\"docente.registro@u.edu.co\",\"contrasena\":\"OtraClave2026\"}");
        assertThat(errada.statusCode()).isEqualTo(401);
        assertThat(errada.body()).contains("\"codigo\":\"CREDENCIALES_INVALIDAS\"");
        assertThat(n.post(
                                "/api/sesion/login",
                                "{\"email\":\"docente.registro@u.edu.co\",\"contrasena\":\"ClaveDocente2026\"}")
                        .statusCode())
                .isEqualTo(200);
        n.pedirCsrf();
        assertThat(n.get("/api/sesion/yo").body()).contains("\"nombre\":\"Docente Registro\"");

        HttpResponse<String> prohibido = n.get("/api/admin/docentes");
        assertThat(prohibido.statusCode()).isEqualTo(403);
        assertThat(prohibido.body()).contains("\"codigo\":\"ACCESO_DENEGADO\"");
        long id = jdbc.sql("SELECT id_usuario FROM usuario WHERE email = 'docente.registro@u.edu.co'")
                .query(Long.class)
                .single();
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE accion = 'ACCESO_DENEGADO' "
                                + "AND id_usuario_accion = :id AND valor_nuevo = 'GET /api/admin/docentes'")
                        .param("id", id)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);

        assertThat(n.post("/api/sesion/logout", null).statusCode()).isEqualTo(204);
        assertThat(n.get("/api/sesion/yo").statusCode()).isEqualTo(401);
    }

    @Test
    void registroRechazado() {
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        HttpResponse<String> noHabilitado = n.post(
                "/api/cuentas/registro", registro("X", "nadie@u.edu.co", "ClaveNadie2026", true, CODIGO_FICTICIO));
        assertThat(noHabilitado.statusCode()).isEqualTo(409);
        assertThat(noHabilitado.body()).contains("\"codigo\":\"REGISTRO_NO_DISPONIBLE\"");
        // Mismo cuerpo que el de una cuenta existente: no revela si el correo tiene cuenta.
        HttpResponse<String> conCuenta = n.post(
                "/api/cuentas/registro",
                registro("X", "admin@uniproject.test", "ClaveNadie2026", true, CODIGO_FICTICIO));
        assertThat(conCuenta.statusCode()).isEqualTo(409);
        assertThat(conCuenta.body()).isEqualTo(noHabilitado.body());
        HttpResponse<String> sinAutorizacion = n.post(
                "/api/cuentas/registro", registro("X", "nadie@u.edu.co", "ClaveNadie2026", false, CODIGO_FICTICIO));
        assertThat(sinAutorizacion.statusCode()).isEqualTo(400);
        assertThat(sinAutorizacion.body()).contains("\"codigo\":\"VALIDACION\"");
        assertThat(n.post("/api/cuentas/registro", registro("X", "nadie@u.edu.co", "corta", true, CODIGO_FICTICIO))
                        .statusCode())
                .isEqualTo(400);
        assertThat(n.post("/api/cuentas/registro", "no es json").body()).contains("\"codigo\":\"CUERPO_INVALIDO\"");
    }

    @Test
    void estudianteYMonitorSeRegistranPorProyecto() {
        jdbc.sql("INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma) "
                        + "VALUES (9101, 'Docente SQL', 'docente.sql@u.edu.co', '!', 'DOCENTE')")
                .update();
        jdbc.sql(
                        "INSERT INTO proyecto (id_proyecto, id_docente, nombre, fecha_inicio, fecha_fin, usa_monitores) VALUES "
                                + "(9201, 9101, 'P1', DATE '2026-08-03', DATE '2026-11-27', true), "
                                + "(9202, 9101, 'P2', DATE '2026-08-03', DATE '2026-11-27', true)")
                .update();
        jdbc.sql("INSERT INTO correo_habilitado (id_proyecto, email, rol_plataforma, id_habilitado_por) VALUES "
                        + "(9201, 'est.uno@u.edu.co', 'ESTUDIANTE', 9101), (9202, 'est.uno@u.edu.co', 'ESTUDIANTE', 9101), "
                        + "(9201, 'mon.uno@u.edu.co', 'MONITOR', 9101), "
                        + "(9201, 'ambiguo@u.edu.co', 'ESTUDIANTE', 9101), (9202, 'ambiguo@u.edu.co', 'MONITOR', 9101)")
                .update();
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();

        assertThat(n.post(
                                "/api/cuentas/registro",
                                registro(
                                        "Est Uno",
                                        "est.uno@u.edu.co",
                                        "ClaveEst2026",
                                        true,
                                        enlaceProyecto(9201, "est.uno@u.edu.co")))
                        .body())
                .contains("\"rol\":\"ESTUDIANTE\"");
        assertThat(jdbc.sql(
                                "SELECT count(*) FROM correo_habilitado c JOIN usuario u ON u.id_usuario = c.id_usuario_registrado "
                                        + "WHERE c.email = 'est.uno@u.edu.co' AND u.email = c.email AND c.fecha_uso IS NOT NULL")
                        .query(Integer.class)
                        .single())
                .isEqualTo(2);

        assertThat(n.post(
                                "/api/cuentas/registro",
                                registro(
                                        "Mon Uno",
                                        "mon.uno@u.edu.co",
                                        "ClaveMon20260",
                                        true,
                                        enlaceProyecto(9201, "mon.uno@u.edu.co")))
                        .body())
                .contains("\"rol\":\"MONITOR\"");
        assertThat(jdbc.sql("SELECT count(*) FROM monitor_proyecto mp JOIN usuario u ON u.id_usuario = mp.id_monitor "
                                + "WHERE mp.id_proyecto = 9201 AND u.email = 'mon.uno@u.edu.co'")
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);

        HttpResponse<String> ambiguo = n.post(
                "/api/cuentas/registro",
                registro("Amb", "ambiguo@u.edu.co", "ClaveAmb20260", true, enlaceProyecto(9201, "ambiguo@u.edu.co")));
        assertThat(ambiguo.statusCode()).isEqualTo(409);
        assertThat(ambiguo.body()).contains("\"codigo\":\"REGISTRO_NO_DISPONIBLE\"");
        HttpResponse<String> noHabilitado = n.post(
                "/api/cuentas/registro", registro("Nadie", "nadie@u.edu.co", "ClaveNadie2026", true, CODIGO_FICTICIO));
        assertThat(noHabilitado.statusCode()).isEqualTo(409);
        assertThat(ambiguo.body()).isEqualTo(noHabilitado.body());
    }

    @Test
    void contrasenasConBcrypt12YAuditoriaSinSecretos() {
        comoAdmin();
        String hash = jdbc.sql("SELECT password_hash FROM usuario WHERE email = 'admin@uniproject.test'")
                .query(String.class)
                .single();
        assertThat(hash).matches("^\\$2[aby]\\$12\\$[./A-Za-z0-9]{53}$");
        assertThat(jdbc.sql("SELECT count(*) FROM usuario WHERE rol_plataforma = 'ADMINISTRADOR'")
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE accion = 'LOGIN_EXITOSO'")
                        .query(Integer.class)
                        .single())
                .isPositive();
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE coalesce(valor_anterior, '') "
                                + "|| coalesce(valor_nuevo, '') || coalesce(campo_modificado, '') ~* '(clave|@)'")
                        .query(Integer.class)
                        .single())
                .isZero();
    }

    @Test
    void elLoginCambiaElIdentificadorDeSesion() {
        Navegador n = comoAdmin();
        String primera = n.cookie("JSESSIONID");
        assertThat(primera).isNotNull();
        assertThat(n.post("/api/sesion/login", ADMIN).statusCode()).isEqualTo(200);
        String segunda = n.cookie("JSESSIONID");
        assertThat(segunda).isNotNull().isNotEqualTo(primera);
        n.ponerCookie("JSESSIONID", primera);
        assertThat(n.get("/api/sesion/yo").statusCode()).isEqualTo(401);
    }
}
