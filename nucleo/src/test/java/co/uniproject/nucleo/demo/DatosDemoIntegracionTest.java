package co.uniproject.nucleo.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("demo")
@TestPropertySource(properties = "uniproject.demo.contrasena=" + DatosDemoIntegracionTest.CLAVE_DEMO)
class DatosDemoIntegracionTest extends ApiIntegracionBase {

    static final String CLAVE_DEMO = "DemoClave2026x";

    @Autowired
    private CargaDemo carga;

    /** Los parámetros que la consulta no usa los ignora JdbcClient. */
    private int contar(String sql) {
        return jdbc.sql(sql)
                .param("dominio", "%@" + CargaDemo.DOMINIO)
                .param("proyecto", CargaDemo.PROYECTO)
                .query(Integer.class)
                .single();
    }

    private int usuarios() {
        return contar("SELECT count(*) FROM usuario WHERE email LIKE :dominio");
    }

    private int tareasDemo() {
        return contar("SELECT count(*) FROM tarea t JOIN hito_equipo he ON he.id_hito_equipo = t.id_hito_equipo "
                + "JOIN equipo e ON e.id_equipo = he.id_equipo JOIN proyecto p ON p.id_proyecto = e.id_proyecto "
                + "WHERE p.nombre = :proyecto");
    }

    @Test
    void cargaElEscenarioYSeIngresaConLaContrasenaDemo() {
        assertThat(usuarios()).isEqualTo(9);
        assertThat(tareasDemo()).isEqualTo(14);
        assertThat(contar("SELECT count(*) FROM tarea t JOIN hito_equipo he ON he.id_hito_equipo = t.id_hito_equipo "
                        + "JOIN hito h ON h.id_hito = he.id_hito JOIN proyecto p ON p.id_proyecto = h.id_proyecto "
                        + "WHERE p.nombre = :proyecto AND h.orden = 1"))
                .isEqualTo(7);
        assertThat(contar("SELECT count(*) FROM hito_equipo he JOIN equipo e ON e.id_equipo = he.id_equipo "
                        + "JOIN proyecto p ON p.id_proyecto = e.id_proyecto WHERE p.nombre = :proyecto "
                        + "AND e.nombre_equipo = 'Equipo Alfa' AND he.estado = 'EVALUADO'"))
                .isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM monitor_proyecto mp JOIN proyecto p ON p.id_proyecto = mp.id_proyecto "
                        + "WHERE p.nombre = :proyecto"))
                .isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM correo_habilitado ch JOIN proyecto p ON p.id_proyecto = ch.id_proyecto "
                        + "WHERE p.nombre = :proyecto AND ch.fecha_uso IS NULL"))
                .isEqualTo(2);
        assertThat(contar("SELECT count(*) FROM miembro_equipo me JOIN usuario u ON u.id_usuario = me.id_usuario "
                        + "WHERE u.email = 'hugo@" + CargaDemo.DOMINIO + "'"))
                .isEqualTo(0);
        long idProyecto = jdbc.sql("SELECT id_proyecto FROM proyecto WHERE nombre = :p")
                .param("p", CargaDemo.PROYECTO)
                .query(Long.class)
                .single();

        Navegador docente = new Navegador(puerto);
        docente.pedirCsrf();
        HttpResponse<String> login = docente.post(
                "/api/sesion/login", "{\"email\":\"" + CargaDemo.DOCENTE + "\",\"contrasena\":\"" + CLAVE_DEMO + "\"}");
        assertThat(login.statusCode()).as(login.body()).isEqualTo(200);
        HttpResponse<String> panel = docente.get("/api/proyectos/" + idProyecto + "/panel");
        assertThat(panel.statusCode()).as(panel.body()).isEqualTo(200);
        assertThat(panel.body())
                .contains("\"nombreEquipo\":\"Equipo Beta\"")
                .contains("\"enRojo\":true")
                .contains("Elena Gómez");
    }

    @Test
    void cargaAlfaHito2ReasignacionCorreccionYComentario() {
        long idProyecto = jdbc.sql("SELECT id_proyecto FROM proyecto WHERE nombre = :p")
                .param("p", CargaDemo.PROYECTO)
                .query(Long.class)
                .single();
        long idAlfa = jdbc.sql("SELECT id_equipo FROM equipo WHERE id_proyecto = :p AND nombre_equipo = 'Equipo Alfa'")
                .param("p", idProyecto)
                .query(Long.class)
                .single();
        long idAlfaHito2 = jdbc.sql("SELECT he.id_hito_equipo FROM hito_equipo he "
                        + "JOIN hito h ON h.id_hito = he.id_hito WHERE he.id_equipo = :e AND h.orden = 2")
                .param("e", idAlfa)
                .query(Long.class)
                .single();

        assertThat(jdbc.sql("SELECT estado FROM hito_equipo WHERE id_hito_equipo = :he")
                        .param("he", idAlfaHito2)
                        .query(String.class)
                        .single())
                .isEqualTo("EN_CURSO");
        assertThat(jdbc.sql("SELECT count(*) FROM tarea WHERE id_hito_equipo = :he")
                        .param("he", idAlfaHito2)
                        .query(Integer.class)
                        .single())
                .isEqualTo(7);
        assertThat(jdbc.sql("SELECT count(*) FROM tarea WHERE id_hito_equipo = :he AND porcentaje_avance = 100")
                        .param("he", idAlfaHito2)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM tarea WHERE id_hito_equipo = :he "
                                + "AND porcentaje_avance BETWEEN 30 AND 60")
                        .param("he", idAlfaHito2)
                        .query(Integer.class)
                        .single())
                .isEqualTo(2);
        assertThat(jdbc.sql(
                                "SELECT count(*) FROM dependencia_tarea dt JOIN tarea t ON t.id_tarea = dt.id_tarea_dependiente "
                                        + "WHERE t.id_hito_equipo = :he")
                        .param("he", idAlfaHito2)
                        .query(Integer.class)
                        .single())
                .isEqualTo(8);
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria l JOIN tarea t ON t.id_tarea = l.id_registro_afectado "
                                + "WHERE t.id_hito_equipo = :he AND l.entidad_afectada = 'tarea' "
                                + "AND l.accion = 'REASIGNAR_TAREA' AND l.campo_modificado = 'id_encargado'")
                        .param("he", idAlfaHito2)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria l JOIN texto_auditoria x ON x.id_log = l.id_log "
                                + "JOIN tarea t ON t.id_tarea = l.id_registro_afectado "
                                + "WHERE t.id_hito_equipo = :he AND l.entidad_afectada = 'tarea' "
                                + "AND l.accion = 'REASIGNAR_TAREA' AND l.campo_modificado = 'motivo'")
                        .param("he", idAlfaHito2)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("SELECT u.email FROM tarea t JOIN usuario u ON u.id_usuario = t.id_encargado "
                                + "WHERE t.id_hito_equipo = :he AND t.titulo = 'Pantalla de préstamos'")
                        .param("he", idAlfaHito2)
                        .query(String.class)
                        .single())
                .isEqualTo("camila@" + CargaDemo.DOMINIO);
        assertThat(jdbc.sql("SELECT count(*) FROM comentario_tarea c JOIN tarea t ON t.id_tarea = c.id_tarea "
                                + "JOIN usuario u ON u.id_usuario = c.id_autor "
                                + "WHERE t.id_hito_equipo = :he AND u.email = :docente")
                        .param("he", idAlfaHito2)
                        .param("docente", CargaDemo.DOCENTE)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);

        long idBeta = jdbc.sql("SELECT id_equipo FROM equipo WHERE id_proyecto = :p AND nombre_equipo = 'Equipo Beta'")
                .param("p", idProyecto)
                .query(Long.class)
                .single();
        assertThat(jdbc.sql("SELECT count(*) FROM evidencia ev JOIN tarea t ON t.id_tarea = ev.id_tarea "
                                + "JOIN hito_equipo he ON he.id_hito_equipo = t.id_hito_equipo "
                                + "WHERE he.id_equipo = :e AND ev.tipo_evidencia = 'ENLACE' "
                                + "AND ev.url LIKE 'https://example.org/demo/%'")
                        .param("e", idBeta)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM solicitud_correccion sc JOIN tarea t ON t.id_tarea = sc.id_tarea "
                                + "JOIN hito_equipo he ON he.id_hito_equipo = t.id_hito_equipo "
                                + "WHERE he.id_equipo = :e AND sc.estado = 'PENDIENTE'")
                        .param("e", idBeta)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);

        Navegador ana = new Navegador(puerto);
        ana.pedirCsrf();
        HttpResponse<String> loginAna = ana.post(
                "/api/sesion/login",
                "{\"email\":\"ana@" + CargaDemo.DOMINIO + "\",\"contrasena\":\"" + CLAVE_DEMO + "\"}");
        assertThat(loginAna.statusCode()).as(loginAna.body()).isEqualTo(200);

        HttpResponse<String> ruta = ana.get("/api/hitos-equipo/" + idAlfaHito2 + "/ruta-critica");
        assertThat(ruta.statusCode()).as(ruta.body()).isEqualTo(200);
        JsonNode arbolRuta = JsonMapper.builder().build().readTree(ruta.body());
        assertThat(arbolRuta.get("nodos").size()).isEqualTo(7);
        assertThat(arbolRuta.get("criticas").size()).isEqualTo(5);
        assertThat(arbolRuta.get("duracionTotalHoras").decimalValue()).isEqualByComparingTo(new BigDecimal("36"));

        HttpResponse<String> historial = ana.get("/api/equipos/" + idAlfa + "/historial");
        assertThat(historial.statusCode()).as(historial.body()).isEqualTo(200);
        assertThat(historial.body()).contains("REASIGNAR_TAREA").contains("Equilibrar la carga");

        long idTareaCorreccion = jdbc.sql(
                        "SELECT sc.id_tarea FROM solicitud_correccion sc JOIN tarea t ON t.id_tarea = sc.id_tarea "
                                + "JOIN hito_equipo he ON he.id_hito_equipo = t.id_hito_equipo "
                                + "WHERE he.id_equipo = :e AND sc.estado = 'PENDIENTE'")
                .param("e", idBeta)
                .query(Long.class)
                .single();
        Navegador diego = new Navegador(puerto);
        diego.pedirCsrf();
        HttpResponse<String> loginDiego = diego.post(
                "/api/sesion/login",
                "{\"email\":\"diego@" + CargaDemo.DOMINIO + "\",\"contrasena\":\"" + CLAVE_DEMO + "\"}");
        assertThat(loginDiego.statusCode()).as(loginDiego.body()).isEqualTo(200);
        HttpResponse<String> correcciones = diego.get("/api/tareas/" + idTareaCorreccion + "/correcciones");
        assertThat(correcciones.statusCode()).as(correcciones.body()).isEqualTo(200);
        assertThat(correcciones.body()).contains("\"estado\":\"PENDIENTE\"");
    }

    @Test
    void laDocenteVeDosProyectosYElPanelDelSegundo() {
        long idProyecto2 = jdbc.sql("SELECT id_proyecto FROM proyecto WHERE nombre = :p")
                .param("p", CargaDemo.PROYECTO_2)
                .query(Long.class)
                .single();
        assertThat(jdbc.sql("SELECT count(*) FROM tarea t JOIN hito_equipo he ON he.id_hito_equipo = t.id_hito_equipo "
                                + "JOIN equipo e ON e.id_equipo = he.id_equipo WHERE e.id_proyecto = :p")
                        .param("p", idProyecto2)
                        .query(Integer.class)
                        .single())
                .isEqualTo(4);
        assertThat(jdbc.sql("SELECT count(*) FROM hito_equipo he JOIN equipo e ON e.id_equipo = he.id_equipo "
                                + "JOIN hito h ON h.id_hito = he.id_hito WHERE e.id_proyecto = :p AND h.orden = 1 "
                                + "AND e.nombre_equipo = 'Equipo Gamma' AND he.estado = 'ENVIADO'")
                        .param("p", idProyecto2)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM hito_equipo he JOIN equipo e ON e.id_equipo = he.id_equipo "
                                + "JOIN hito h ON h.id_hito = he.id_hito WHERE e.id_proyecto = :p AND h.orden = 1 "
                                + "AND e.nombre_equipo = 'Equipo Delta' AND he.estado = 'EN_CURSO'")
                        .param("p", idProyecto2)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql(
                                "SELECT count(*) FROM dependencia_tarea dt JOIN tarea t ON t.id_tarea = dt.id_tarea_dependiente "
                                        + "JOIN hito_equipo he ON he.id_hito_equipo = t.id_hito_equipo "
                                        + "JOIN equipo e ON e.id_equipo = he.id_equipo WHERE e.id_proyecto = :p "
                                        + "AND e.nombre_equipo = 'Equipo Delta'")
                        .param("p", idProyecto2)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);

        Navegador docente = new Navegador(puerto);
        docente.pedirCsrf();
        HttpResponse<String> login = docente.post(
                "/api/sesion/login", "{\"email\":\"" + CargaDemo.DOCENTE + "\",\"contrasena\":\"" + CLAVE_DEMO + "\"}");
        assertThat(login.statusCode()).as(login.body()).isEqualTo(200);

        HttpResponse<String> proyectos = docente.get("/api/proyectos");
        assertThat(proyectos.statusCode()).as(proyectos.body()).isEqualTo(200);
        JsonNode lista = JsonMapper.builder().build().readTree(proyectos.body());
        assertThat(lista.size()).isEqualTo(2);
        assertThat(proyectos.body()).contains(CargaDemo.PROYECTO).contains(CargaDemo.PROYECTO_2);

        HttpResponse<String> panel = docente.get("/api/proyectos/" + idProyecto2 + "/panel");
        assertThat(panel.statusCode()).as(panel.body()).isEqualTo(200);
        assertThat(panel.body()).contains("\"nombreEquipo\":\"Equipo Gamma\"");
    }

    @Test
    void esIdempotente() {
        int antes = usuarios();
        assertThat(antes).isEqualTo(9);
        assertThat(carga.cargar(CLAVE_DEMO)).isFalse();
        assertThat(usuarios()).isEqualTo(9);
        assertThat(tareasDemo()).isEqualTo(14);
    }

    @Test
    void sinContrasenaNoCargaNada() {
        CargaDemo simulada = mock(CargaDemo.class);
        new DatosDemo(simulada, " ").run(null);
        verifyNoInteractions(simulada);
    }
}
