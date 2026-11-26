package co.uniproject.nucleo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * Base de las pruebas HTTP del dominio: PostgreSQL y RabbitMQ reales, un solo contexto de Spring para todas las
 * subclases y un navegador con cookies y CSRF como el SPA. Cada prueba crea sus propios usuarios con correoUnico(...):
 * no dependen del orden ni de las demás.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApiIntegracionBase {

    /** Contraseña de los usuarios creados con crearUsuario (hash bcrypt de costo 4 para que las pruebas sean rápidas). */
    protected static final String CLAVE = "ClavePrueba2026";

    private static final String HASH_CLAVE = new BCryptPasswordEncoder(4).encode(CLAVE);
    private static final AtomicLong SECUENCIA = new AtomicLong();
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6");
    private static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.3.7-management");

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
    }

    @Value("${local.server.port}")
    protected int puerto;

    @Autowired
    protected JdbcClient jdbc;

    /** Correo distinto en cada llamada (p. ej. "est.17@u.edu.co"). */
    protected static String correoUnico(String prefijo) {
        return prefijo + "." + SECUENCIA.incrementAndGet() + "@u.edu.co";
    }

    /** Crea un usuario con la contraseña CLAVE directamente en la BD. rol: ADMINISTRADOR, DOCENTE, ESTUDIANTE o MONITOR. */
    protected long crearUsuario(String nombre, String email, String rol) {
        return jdbc.sql(
                        "INSERT INTO usuario (nombre, email, password_hash, rol_plataforma, autoriza_tratamiento_datos, "
                                + "fecha_autorizacion) VALUES (:nombre, :email, :hash, :rol, true, now()) RETURNING id_usuario")
                .param("nombre", nombre)
                .param("email", email)
                .param("hash", HASH_CLAVE)
                .param("rol", rol)
                .query(Long.class)
                .single();
    }

    protected long idUsuario(String email) {
        return jdbc.sql("SELECT id_usuario FROM usuario WHERE email = :email")
                .param("email", email)
                .query(Long.class)
                .single();
    }

    /** Navegador con sesión iniciada (contraseña CLAVE) y cookie CSRF vigente. */
    protected Navegador sesion(String email) {
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        HttpResponse<String> r =
                n.post("/api/sesion/login", "{\"email\":\"" + email + "\",\"contrasena\":\"" + CLAVE + "\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        n.pedirCsrf();
        return n;
    }

    /** Primer valor numérico del campo en un JSON, p. ej. numero(cuerpo, "idProyecto"). */
    public static long numero(String json, String campo) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*(\\d+)")
                .matcher(json);
        assertThat(m.find()).as("campo %s en %s", campo, json).isTrue();
        return Long.parseLong(m.group(1));
    }

    /** Primer valor de texto del campo en un JSON plano, p. ej. campoTexto(cuerpo, "codigo"). */
    public static String campoTexto(String json, String campo) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);
        assertThat(m.find()).as("campo %s en %s", campo, json).isTrue();
        return m.group(1);
    }

    /** Busca la habilitación pendiente del correo en el proyecto y pide al docente un enlace de registro nuevo. */
    protected String enlace(Navegador docente, long idProyecto, String email) {
        long idCorreo = jdbc.sql("SELECT id_correo_habilitado FROM correo_habilitado "
                        + "WHERE id_proyecto = :p AND email = :e AND fecha_uso IS NULL")
                .param("p", idProyecto)
                .param("e", email.strip().toLowerCase(Locale.ROOT))
                .query(Long.class)
                .single();
        HttpResponse<String> r =
                docente.post("/api/proyectos/" + idProyecto + "/correos/" + idCorreo + "/enlace", null);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        return campoTexto(r.body(), "codigo");
    }

    /** Crea un curso del docente en el periodo 2026-2 y devuelve su id. */
    protected long crearCurso(Navegador docente, String nombre) {
        HttpResponse<String> r = docente.post("/api/cursos", "{\"nombre\":\"" + nombre + "\",\"periodo\":\"2026-2\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idCurso");
    }

    /** Asigna cursos a un proyecto con confirmar=true y exige 200 con aplicado=true. */
    protected void asignarCursos(Navegador docente, long idProyecto, long... idsCursos) {
        String lista = Arrays.stream(idsCursos).mapToObj(String::valueOf).collect(Collectors.joining(","));
        HttpResponse<String> r = docente.put(
                "/api/proyectos/" + idProyecto + "/cursos", "{\"idsCursos\":[" + lista + "],\"confirmar\":true}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        assertThat(r.body()).contains("\"aplicado\":true");
    }

    /** Agrega o habilita correos de estudiantes en el curso y exige 201. */
    protected void agregarAlCurso(Navegador docente, long idCurso, String... emails) {
        String lista = Arrays.stream(emails).map(e -> "\"" + e + "\"").collect(Collectors.joining(","));
        HttpResponse<String> r = docente.post("/api/cursos/" + idCurso + "/correos", "{\"emails\":[" + lista + "]}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
    }

    /** Busca la habilitación pendiente del correo en el curso y pide al docente un enlace de registro nuevo. */
    protected String enlaceCurso(Navegador docente, long idCurso, String email) {
        long idCorreo = jdbc.sql("SELECT id_correo_habilitado FROM correo_habilitado "
                        + "WHERE id_curso = :c AND email = :e AND fecha_uso IS NULL")
                .param("c", idCurso)
                .param("e", email.strip().toLowerCase(Locale.ROOT))
                .query(Long.class)
                .single();
        HttpResponse<String> r = docente.post("/api/cursos/" + idCurso + "/correos/" + idCorreo + "/enlace", null);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        return campoTexto(r.body(), "codigo");
    }

    /** Crea un curso por SQL con nombre único y lo asigna a los proyectos indicados. */
    protected long cursoSql(long idDocente, long... idsProyectos) {
        long idCurso = jdbc.sql("INSERT INTO curso (id_docente, nombre, periodo) VALUES (:d, :n, '2026-2') "
                        + "RETURNING id_curso")
                .param("d", idDocente)
                .param("n", "Curso SQL " + SECUENCIA.incrementAndGet())
                .query(Long.class)
                .single();
        for (long idProyecto : idsProyectos) {
            jdbc.sql("INSERT INTO curso_proyecto (id_curso, id_proyecto) VALUES (:c, :p)")
                    .param("c", idCurso)
                    .param("p", idProyecto)
                    .update();
        }
        return idCurso;
    }

    /** Inserta directamente miembro_curso y participante_proyecto para el estudiante. */
    protected void participanteSql(long idCurso, long idProyecto, long idUsuario) {
        jdbc.sql("INSERT INTO miembro_curso (id_curso, id_usuario) VALUES (:c, :u) ON CONFLICT DO NOTHING")
                .param("c", idCurso)
                .param("u", idUsuario)
                .update();
        jdbc.sql("INSERT INTO participante_proyecto (id_proyecto, id_usuario, id_curso) VALUES (:p, :u, :c) "
                        + "ON CONFLICT DO NOTHING")
                .param("p", idProyecto)
                .param("u", idUsuario)
                .param("c", idCurso)
                .update();
    }

    /** Crea un equipo vía API omitiendo del JSON los campos nulos; exige 201 y devuelve idEquipo. */
    protected long crearEquipoApi(Navegador docente, long idProyecto, String nombre, Long idCurso, Integer cupo) {
        StringBuilder json =
                new StringBuilder("{\"nombreEquipo\":\"").append(nombre).append("\"");
        if (idCurso != null) {
            json.append(",\"idCurso\":").append(idCurso);
        }
        if (cupo != null) {
            json.append(",\"cupo\":").append(cupo);
        }
        json.append("}");
        HttpResponse<String> r = docente.post("/api/proyectos/" + idProyecto + "/equipos", json.toString());
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idEquipo");
    }

    /** Abre la autoinscripción del proyecto fijando el cierre en 7 días y horasRonda = 48. */
    protected void abrirInscripcion(Navegador docente, long idProyecto) {
        Instant cierre = Instant.now().plus(7, ChronoUnit.DAYS);
        HttpResponse<String> r = docente.put(
                "/api/proyectos/" + idProyecto + "/inscripcion",
                "{\"cierreEquipos\":\"" + cierre + "\",\"horasRonda\":48}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
    }

    /** Cierra la autoinscripción del proyecto en la BD poniendo cierre_equipos una hora en el pasado. */
    protected void cerrarInscripcionSql(long idProyecto) {
        jdbc.sql("UPDATE proyecto SET cierre_equipos = now() - interval '1 hour' WHERE id_proyecto = :p")
                .param("p", idProyecto)
                .update();
    }

    /** true si el JSON tiene "campo": valor exacto (12 no casa con 123). */
    protected static boolean tieneValor(String json, String campo, long valor) {
        return Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*" + valor + "(?!\\d)")
                .matcher(json)
                .find();
    }

    public static final Duration ESPERA_HTTP = Duration.ofSeconds(30);

    // HTTP/1.1 sin upgrade h2c y con tiempo límite de conexión para evitar bloqueos en pruebas (P-14).
    public static HttpClient clienteHttp() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Cliente con cookies propias: guarda Set-Cookie y manda X-XSRF-TOKEN como lo hará el SPA. */
    public static final class Navegador {

        private final HttpClient cliente = clienteHttp();
        private final Map<String, String> cookies = new LinkedHashMap<>();
        private final String base;

        public Navegador(int puerto) {
            this.base = "http://localhost:" + puerto;
        }

        public HttpResponse<String> get(String ruta) {
            return enviar("GET", ruta, null);
        }

        public HttpResponse<String> post(String ruta, String json) {
            return enviar("POST", ruta, json);
        }

        public HttpResponse<String> put(String ruta, String json) {
            return enviar("PUT", ruta, json);
        }

        public HttpResponse<String> delete(String ruta) {
            return enviar("DELETE", ruta, null);
        }

        public HttpResponse<byte[]> getBytes(String ruta) {
            return enviar("GET", ruta, null, null, HttpResponse.BodyHandlers.ofByteArray());
        }

        /** multipart/form-data con un solo archivo, como lo manda el navegador. */
        public HttpResponse<String> postArchivo(
                String ruta, String campo, String nombreArchivo, String tipo, byte[] contenido) {
            String limite = "limite" + UUID.randomUUID().toString().replace("-", "");
            ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();
            cuerpo.writeBytes(("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + campo + "\"; filename=\""
                            + nombreArchivo + "\"\r\nContent-Type: " + tipo + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            cuerpo.writeBytes(contenido);
            cuerpo.writeBytes(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return enviar(
                    "POST",
                    ruta,
                    "multipart/form-data; boundary=" + limite,
                    cuerpo.toByteArray(),
                    HttpResponse.BodyHandlers.ofString());
        }

        public void pedirCsrf() {
            assertThat(get("/api/sesion/csrf").statusCode()).isEqualTo(204);
            assertThat(cookies).containsKey("XSRF-TOKEN");
        }

        public String cookie(String nombre) {
            return cookies.get(nombre);
        }

        private HttpResponse<String> enviar(String metodo, String ruta, String json) {
            return enviar(
                    metodo,
                    ruta,
                    json == null ? null : "application/json",
                    json == null ? null : json.getBytes(StandardCharsets.UTF_8),
                    HttpResponse.BodyHandlers.ofString());
        }

        private <T> HttpResponse<T> enviar(
                String metodo, String ruta, String tipo, byte[] cuerpo, HttpResponse.BodyHandler<T> manejador) {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + ruta))
                    .timeout(ESPERA_HTTP)
                    .method(
                            metodo,
                            cuerpo == null
                                    ? HttpRequest.BodyPublishers.noBody()
                                    : HttpRequest.BodyPublishers.ofByteArray(cuerpo));
            if (tipo != null) {
                b.header("Content-Type", tipo);
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
                HttpResponse<T> r = cliente.send(b.build(), manejador);
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
}
