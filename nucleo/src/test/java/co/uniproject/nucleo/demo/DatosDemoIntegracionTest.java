package co.uniproject.nucleo.demo;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ActiveProfiles("demo")
@TestPropertySource(properties = "uniproject.demo.contrasena=" + DatosDemoIntegracionTest.CLAVE_DEMO)
class DatosDemoIntegracionTest extends ApiIntegracionBase {

    static final String CLAVE_DEMO = "DemoClave2026x";

    @Autowired
    private CargaDemo carga;

    /** Los parámetros que la consulta no usa los ignora JdbcClient. */
    private int contar(String sql) {
        return jdbc.sql(sql).param("dominio", "%@" + CargaDemo.DOMINIO).param("proyecto", CargaDemo.PROYECTO)
                .query(Integer.class).single();
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
        assertThat(usuarios()).isEqualTo(8);
        assertThat(tareasDemo()).isEqualTo(7);
        assertThat(contar("SELECT count(*) FROM hito_equipo he JOIN equipo e ON e.id_equipo = he.id_equipo "
                + "JOIN proyecto p ON p.id_proyecto = e.id_proyecto WHERE p.nombre = :proyecto "
                + "AND e.nombre_equipo = 'Equipo Alfa' AND he.estado = 'EVALUADO'"))
                .isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM monitor_proyecto mp JOIN proyecto p ON p.id_proyecto = mp.id_proyecto "
                + "WHERE p.nombre = :proyecto")).isEqualTo(1);
        long idProyecto = jdbc.sql("SELECT id_proyecto FROM proyecto WHERE nombre = :p")
                .param("p", CargaDemo.PROYECTO).query(Long.class).single();

        Navegador docente = new Navegador(puerto);
        docente.pedirCsrf();
        HttpResponse<String> login = docente.post("/api/sesion/login",
                "{\"email\":\"" + CargaDemo.DOCENTE + "\",\"contrasena\":\"" + CLAVE_DEMO + "\"}");
        assertThat(login.statusCode()).as(login.body()).isEqualTo(200);
        HttpResponse<String> panel = docente.get("/api/proyectos/" + idProyecto + "/panel");
        assertThat(panel.statusCode()).as(panel.body()).isEqualTo(200);
        assertThat(panel.body()).contains("\"nombreEquipo\":\"Equipo Beta\"").contains("\"enRojo\":true");
    }

    @Test
    void esIdempotente() {
        int antes = usuarios();
        assertThat(carga.cargar(CLAVE_DEMO)).isFalse();
        assertThat(usuarios()).isEqualTo(antes);
        assertThat(tareasDemo()).isEqualTo(7);
    }

    @Test
    void sinContrasenaNoCargaNada() {
        CargaDemo simulada = mock(CargaDemo.class);
        new DatosDemo(simulada, " ").run(null);
        verifyNoInteractions(simulada);
    }
}
