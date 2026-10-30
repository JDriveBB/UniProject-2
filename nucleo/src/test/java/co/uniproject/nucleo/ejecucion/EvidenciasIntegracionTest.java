package co.uniproject.nucleo.ejecucion;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenciasIntegracionTest extends ApiIntegracionBase {

    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF\n"
            .getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};

    private record Escenario(long idHitoEquipo, String docente, String lider, String integrante, String otro,
                             String monitor, long idLider, long idIntegrante, long idOtro, long tareaIntegrante,
                             long tareaOtro) {}

    /** Proyecto con monitores, hito vigente, equipo (líder + 2) y una tarea pendiente para cada integrante. */
    private Escenario escenario() {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String docente = correoUnico("doc");
        long idDocente = crearUsuario("Docente Evidencias", docente, "DOCENTE");
        String lider = correoUnico("lider");
        long idLider = crearUsuario("Lider Evidencias", lider, "ESTUDIANTE");
        String integrante = correoUnico("int");
        long idIntegrante = crearUsuario("Integrante Uno", integrante, "ESTUDIANTE");
        String otro = correoUnico("otro");
        long idOtro = crearUsuario("Integrante Dos", otro, "ESTUDIANTE");
        String monitor = correoUnico("monitor");
        long idMonitor = crearUsuario("Monitor Uno", monitor, "MONITOR");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin, usa_monitores) "
                        + "VALUES (:d, 'Proyecto Evidencias', :i, :f, true) RETURNING id_proyecto")
                .param("d", idDocente).param("i", hoy.minusDays(30)).param("f", hoy.plusDays(60))
                .query(Long.class).single();
        jdbc.sql("INSERT INTO monitor_proyecto (id_proyecto, id_monitor) VALUES (:p, :m)")
                .param("p", proyecto).param("m", idMonitor).update();
        long hito = jdbc.sql("INSERT INTO hito (id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) "
                        + "VALUES (:p, 'Hito 1', 1, :i, :f) RETURNING id_hito")
                .param("p", proyecto).param("i", hoy.minusDays(10)).param("f", hoy.plusDays(20))
                .query(Long.class).single();
        long equipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo) VALUES (:p, 'Equipo E') "
                        + "RETURNING id_equipo")
                .param("p", proyecto).query(Long.class).single();
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES (:e, :l, 'LIDER'), "
                        + "(:e, :a, 'INTEGRANTE'), (:e, :b, 'INTEGRANTE')")
                .param("e", equipo).param("l", idLider).param("a", idIntegrante).param("b", idOtro).update();
        long he = jdbc.sql("INSERT INTO hito_equipo (id_hito, id_equipo) VALUES (:h, :e) RETURNING id_hito_equipo")
                .param("h", hito).param("e", equipo).query(Long.class).single();
        long tareaIntegrante = tarea(he, idIntegrante, "Informe");
        long tareaOtro = tarea(he, idOtro, "Diagramas");
        return new Escenario(he, docente, lider, integrante, otro, monitor, idLider, idIntegrante, idOtro,
                tareaIntegrante, tareaOtro);
    }

    private long tarea(long idHitoEquipo, long encargado, String titulo) {
        return jdbc.sql("INSERT INTO tarea (id_hito_equipo, id_encargado, titulo, horas_estimadas) "
                        + "VALUES (:he, :u, :t, 4) RETURNING id_tarea")
                .param("he", idHitoEquipo).param("u", encargado).param("t", titulo).query(Long.class).single();
    }

    private int contar(String sql, long id) {
        return jdbc.sql(sql).param("id", id).query(Integer.class).single();
    }

    private static String rutaEvidencias(long idTarea) {
        return "/api/tareas/" + idTarea + "/evidencias";
    }

    @Test
    void adjuntaUnPdfYSoloQuienVeLaTareaLoDescarga() {
        Escenario e = escenario();
        HttpResponse<String> r = sesion(e.integrante()).postArchivo(rutaEvidencias(e.tareaIntegrante()), "archivo",
                "Informe final.pdf", "application/pdf", PDF);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        assertThat(r.body()).contains("\"tipoEvidencia\":\"PDF\"").contains("\"nombreArchivo\":\"Informe final.pdf\"");
        long id = numero(r.body(), "idEvidencia");
        assertThat(jdbc.sql("SELECT url FROM evidencia WHERE id_evidencia = :id").param("id", id)
                .query(String.class).single()).matches("evidencias/[0-9a-f-]{36}\\.pdf");
        HttpResponse<byte[]> descarga = sesion(e.docente()).getBytes("/api/evidencias/" + id + "/archivo");
        assertThat(descarga.statusCode()).isEqualTo(200);
        assertThat(descarga.body()).isEqualTo(PDF);
        assertThat(descarga.headers().firstValue("content-type")).hasValue("application/pdf");
        assertThat(descarga.headers().firstValue("content-disposition").orElse("")).contains("attachment");
        String ajeno = correoUnico("ajeno");
        crearUsuario("Ajeno", ajeno, "ESTUDIANTE");
        assertThat(sesion(ajeno).getBytes("/api/evidencias/" + id + "/archivo").statusCode()).isEqualTo(403);
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'tarea' "
                + "AND id_registro_afectado = :id AND accion = 'ADJUNTAR_EVIDENCIA'", e.tareaIntegrante())).isEqualTo(1);
    }

    @Test
    void rechazaArchivosVaciosGrandesODeOtroTipo() {
        Escenario e = escenario();
        Navegador integrante = sesion(e.integrante());
        String ruta = rutaEvidencias(e.tareaIntegrante());
        HttpResponse<String> disfrazado = integrante.postArchivo(ruta, "archivo", "notas.pdf", "application/pdf",
                "solo texto".getBytes(StandardCharsets.UTF_8));
        assertThat(disfrazado.statusCode()).isEqualTo(400);
        assertThat(disfrazado.body()).contains("\"codigo\":\"TIPO_NO_PERMITIDO\"");
        HttpResponse<String> grande = integrante.postArchivo(ruta, "archivo", "foto.png", "image/png",
                Arrays.copyOf(PNG, 10 * 1024 * 1024 + 1));
        assertThat(grande.statusCode()).isEqualTo(400);
        assertThat(grande.body()).contains("\"codigo\":\"ARCHIVO_DEMASIADO_GRANDE\"");
        HttpResponse<String> vacio = integrante.postArchivo(ruta, "archivo", "vacio.png", "image/png", new byte[0]);
        assertThat(vacio.statusCode()).isEqualTo(400);
        assertThat(vacio.body()).contains("\"codigo\":\"ARCHIVO_VACIO\"");
        assertThat(integrante.postArchivo(ruta, "archivo", "foto.png", "image/png", PNG).statusCode()).isEqualTo(201);
        assertThat(contar("SELECT count(*) FROM evidencia WHERE id_tarea = :id", e.tareaIntegrante())).isEqualTo(1);
    }

    @Test
    void aceptaEnlacesHttpYRechazaLosDemas() {
        Escenario e = escenario();
        Navegador integrante = sesion(e.integrante());
        String ruta = rutaEvidencias(e.tareaIntegrante());
        HttpResponse<String> ok = integrante.post(ruta, "{\"url\":\" https://github.com/equipo/repo \"}");
        assertThat(ok.statusCode()).as(ok.body()).isEqualTo(201);
        assertThat(ok.body()).contains("\"tipoEvidencia\":\"ENLACE\"").contains("\"enlace\":\"https://github.com/equipo/repo\"");
        long id = numero(ok.body(), "idEvidencia");
        for (String malo : new String[] {"ftp://servidor/archivo", "javascript:alert(1)", "HTTPS://github.com/x"}) {
            HttpResponse<String> r = integrante.post(ruta, "{\"url\":\"" + malo + "\"}");
            assertThat(r.statusCode()).as(malo).isEqualTo(400);
            assertThat(r.body()).contains("\"codigo\":\"ENLACE_INVALIDO\"");
        }
        assertThat(integrante.getBytes("/api/evidencias/" + id + "/archivo").statusCode()).isEqualTo(404);
        assertThat(sesion(e.otro()).get(ruta).body()).contains("https://github.com/equipo/repo");
    }

    @Test
    void otroIntegranteNoAdjuntaYElLiderSi() {
        Escenario e = escenario();
        String ruta = rutaEvidencias(e.tareaIntegrante());
        assertThat(sesion(e.otro()).postArchivo(ruta, "archivo", "a.pdf", "application/pdf", PDF).statusCode())
                .isEqualTo(403);
        assertThat(sesion(e.lider()).postArchivo(ruta, "archivo", "a.pdf", "application/pdf", PDF).statusCode())
                .isEqualTo(201);
    }

    @Test
    void elMonitorComentaSinCambiarLaTarea() {
        Escenario e = escenario();
        Navegador monitor = sesion(e.monitor());
        String ruta = "/api/tareas/" + e.tareaIntegrante() + "/comentarios";
        HttpResponse<String> r = monitor.post(ruta, "{\"contenido\":\"Revisen la normalización\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        assertThat(r.body()).contains("\"rolAutor\":\"MONITOR\"").contains("\"autor\":\"Monitor Uno\"");
        assertThat(sesion(e.integrante()).get(ruta).body()).contains("Revisen la normalización");
        assertThat(monitor.post(ruta, "{\"contenido\":\"   \"}").statusCode()).isEqualTo(400);
        assertThat(monitor.postArchivo(rutaEvidencias(e.tareaIntegrante()), "archivo", "a.pdf", "application/pdf", PDF)
                .statusCode()).isEqualTo(403);
        assertThat(monitor.put("/api/tareas/" + e.tareaIntegrante() + "/avance", "{\"porcentajeAvance\":50}")
                .statusCode()).isEqualTo(403);
        jdbc.sql("UPDATE hito_equipo SET estado = 'ENVIADO' WHERE id_hito_equipo = :id")
                .param("id", e.idHitoEquipo()).update();
        HttpResponse<String> cerrado = monitor.post(ruta, "{\"contenido\":\"Tarde\"}");
        assertThat(cerrado.statusCode()).isEqualTo(409);
        assertThat(cerrado.body()).contains("\"codigo\":\"TAREA_NO_EN_CURSO\"");
        assertThat(jdbc.sql("SELECT estado || ':' || porcentaje_avance FROM tarea WHERE id_tarea = :id")
                .param("id", e.tareaIntegrante()).query(String.class).single()).isEqualTo("PENDIENTE:0");
    }

    @Test
    void laCorreccionDevuelveLaTareaYAvisaAlResponsable() {
        Escenario e = escenario();
        long t = e.tareaIntegrante();
        Navegador integrante = sesion(e.integrante());
        Navegador lider = sesion(e.lider());
        long ev = numero(integrante.post(rutaEvidencias(t), "{\"url\":\"https://github.com/equipo/v1\"}").body(),
                "idEvidencia");
        assertThat(integrante.put("/api/tareas/" + t + "/avance", "{\"porcentajeAvance\":100}").statusCode())
                .isEqualTo(200);
        String peticion = "{\"idEvidencia\":" + ev + ",\"observaciones\":\"Falta el diagrama de clases\"}";
        HttpResponse<String> r = lider.post("/api/tareas/" + t + "/correcciones", peticion);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        assertThat(r.body()).contains("\"estado\":\"PENDIENTE\"").contains("\"estudiante\":\"Integrante Uno\"");
        assertThat(jdbc.sql("SELECT estado || ':' || porcentaje_avance || ':' || (fecha_completada IS NULL) "
                + "FROM tarea WHERE id_tarea = :id").param("id", t).query(String.class).single())
                .isEqualTo("EN_PROGRESO:99:true");
        assertThat(contar("SELECT count(*) FROM evento_saliente WHERE tipo = 'tarea.correccion' "
                + "AND (carga->'data'->'tarea'->>'id')::bigint = :id", t)).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM notificacion WHERE tipo = 'tarea.correccion' "
                        + "AND id_destinatario = :u AND id_referencia = :t")
                .param("u", e.idIntegrante()).param("t", t).query(Integer.class).single()).isEqualTo(1);
        HttpResponse<String> otra = lider.post("/api/tareas/" + t + "/correcciones", peticion);
        assertThat(otra.statusCode()).isEqualTo(409);
        assertThat(otra.body()).contains("\"codigo\":\"CORRECCION_PENDIENTE\"");
        assertThat(integrante.put("/api/tareas/" + t + "/avance", "{\"porcentajeAvance\":100}").statusCode())
                .isEqualTo(409);
        assertThat(integrante.post(rutaEvidencias(t), "{\"url\":\"https://github.com/equipo/v2\"}").statusCode())
                .isEqualTo(201);
        assertThat(jdbc.sql("SELECT estado FROM solicitud_correccion WHERE id_tarea = :id").param("id", t)
                .query(String.class).single()).isEqualTo("ATENDIDA");
        assertThat(integrante.put("/api/tareas/" + t + "/avance", "{\"porcentajeAvance\":100}").statusCode())
                .isEqualTo(200);
    }

    @Test
    void soloElLiderPideCorreccionesSobreEvidenciasDeLaTarea() {
        Escenario e = escenario();
        long evIntegrante = numero(sesion(e.integrante()).post(rutaEvidencias(e.tareaIntegrante()),
                "{\"url\":\"https://github.com/equipo/a\"}").body(), "idEvidencia");
        long evOtro = numero(sesion(e.otro()).post(rutaEvidencias(e.tareaOtro()),
                "{\"url\":\"https://github.com/equipo/b\"}").body(), "idEvidencia");
        String ruta = "/api/tareas/" + e.tareaIntegrante() + "/correcciones";
        assertThat(sesion(e.integrante()).post(ruta, "{\"idEvidencia\":" + evIntegrante
                + ",\"observaciones\":\"Autocorrección\"}").statusCode()).isEqualTo(403);
        assertThat(sesion(e.lider()).post(ruta, "{\"idEvidencia\":" + evOtro
                + ",\"observaciones\":\"De otra tarea\"}").statusCode()).isEqualTo(404);
        HttpResponse<String> lista = sesion(e.docente()).get(ruta);
        assertThat(lista.statusCode()).isEqualTo(200);
        assertThat(lista.body()).isEqualTo("[]");
    }
}
