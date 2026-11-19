package co.uniproject.nucleo.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import co.uniproject.nucleo.ApiIntegracionBase;
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
        assertThat(tareasDemo()).isEqualTo(7);
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
        assertThat(panel.body()).contains("\"nombreEquipo\":\"Equipo Beta\"").contains("\"enRojo\":true");
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
        assertThat(tareasDemo()).isEqualTo(7);
    }

    @Test
    void sinContrasenaNoCargaNada() {
        CargaDemo simulada = mock(CargaDemo.class);
        new DatosDemo(simulada, " ").run(null);
        verifyNoInteractions(simulada);
    }
}
