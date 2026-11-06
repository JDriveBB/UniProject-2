package co.uniproject.nucleo.trazabilidad.servicio;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class HistorialPermisosIntegracionTest extends ApiIntegracionBase {

    @Autowired
    private ServicioHistorial servicioHistorial;

    @Autowired
    private ServicioReporte servicioReporte;

    private record Escenario(long idProyecto, long idDocente, String docente,
                             long idMonitor, String monitor,
                             long idEquipo, long idOtroEquipo,
                             long idEstudiante1, String estudiante1,
                             long idEstudiante2, String estudiante2,
                             long idEstudianteOtro, String estudianteOtro,
                             long idDocenteOtro, String docenteOtro,
                             long idHitoEquipo) {}

    private Escenario crearEscenario() {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String docente = correoUnico("docente.p");
        long idDocente = crearUsuario("Docente Principal", docente, "DOCENTE");

        String monitor = correoUnico("monitor.p");
        long idMonitor = crearUsuario("Monitor Principal", monitor, "MONITOR");

        String est1 = correoUnico("est1");
        long idEst1 = crearUsuario("Estudiante Uno", est1, "ESTUDIANTE");

        String est2 = correoUnico("est2");
        long idEst2 = crearUsuario("Estudiante Dos", est2, "ESTUDIANTE");

        String estOtro = correoUnico("est.otro");
        long idEstOtro = crearUsuario("Estudiante Otro", estOtro, "ESTUDIANTE");

        String docOtro = correoUnico("docente.otro");
        long idDocOtro = crearUsuario("Docente Otro", docOtro, "DOCENTE");

        long idProyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin, usa_monitores) "
                        + "VALUES (:d, 'Proyecto Historial', :i, :f, true) RETURNING id_proyecto")
                .param("d", idDocente).param("i", hoy.minusDays(15)).param("f", hoy.plusDays(45))
                .query(Long.class).single();

        jdbc.sql("INSERT INTO monitor_proyecto (id_proyecto, id_monitor) VALUES (:p, :m)")
                .param("p", idProyecto).param("m", idMonitor).update();

        long idOtroProyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin, usa_monitores) "
                        + "VALUES (:d, 'Proyecto Ajeno', :i, :f, false) RETURNING id_proyecto")
                .param("d", idDocOtro).param("i", hoy.minusDays(15)).param("f", hoy.plusDays(45))
                .query(Long.class).single();

        long idEquipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo) VALUES (:p, 'Equipo Alpha') RETURNING id_equipo")
                .param("p", idProyecto).query(Long.class).single();

        long idOtroEquipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo) VALUES (:p, 'Equipo Beta') RETURNING id_equipo")
                .param("p", idProyecto).query(Long.class).single();

        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES (:e, :u, 'LIDER')")
                .param("e", idEquipo).param("u", idEst1).update();
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES (:e, :u, 'INTEGRANTE')")
                .param("e", idEquipo).param("u", idEst2).update();
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES (:e, :u, 'LIDER')")
                .param("e", idOtroEquipo).param("u", idEstOtro).update();

        long idHito = jdbc.sql("INSERT INTO hito (id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) "
                        + "VALUES (:p, 'Hito 1', 1, :i, :f) RETURNING id_hito")
                .param("p", idProyecto).param("i", hoy.minusDays(10)).param("f", hoy.plusDays(10))
                .query(Long.class).single();

        long idHitoEquipo = jdbc.sql("INSERT INTO hito_equipo (id_hito, id_equipo) VALUES (:h, :e) RETURNING id_hito_equipo")
                .param("h", idHito).param("e", idEquipo).query(Long.class).single();

        return new Escenario(idProyecto, idDocente, docente, idMonitor, monitor, idEquipo, idOtroEquipo,
                idEst1, est1, idEst2, est2, idEstOtro, estOtro, idDocOtro, docOtro, idHitoEquipo);
    }

    private void auditar(Long idUsuario, String entidad, long idRegistro, String accion, String campo,
                         String anterior, String nuevo) {
        jdbc.sql("INSERT INTO log_auditoria (id_usuario_accion, entidad_afectada, id_registro_afectado, accion, "
                        + "campo_modificado, valor_anterior, valor_nuevo) VALUES (:u, :e, :r, :a, :c, :ant, :v)")
                .param("u", idUsuario, java.sql.Types.BIGINT)
                .param("e", entidad)
                .param("r", idRegistro)
                .param("a", accion)
                .param("c", campo, java.sql.Types.VARCHAR)
                .param("ant", anterior, java.sql.Types.VARCHAR)
                .param("v", nuevo, java.sql.Types.VARCHAR)
                .update();
    }

    @Test
    void monitorActivoDelProyectoRecibe403() {
        Escenario e = crearEscenario();
        auditar(e.idDocente(), "equipo", e.idEquipo(), "CREAR", null, null, "Equipo Alpha");

        HttpResponse<String> r = sesion(e.monitor()).get("/api/equipos/" + e.idEquipo() + "/historial");
        assertThat(r.statusCode()).isEqualTo(403);
        assertThat(r.body()).contains("ACCESO_DENEGADO");
    }

    @Test
    void matrizDePermisosParaVerHistorialEquipo() {
        Escenario e = crearEscenario();
        auditar(e.idDocente(), "equipo", e.idEquipo(), "CREAR", null, null, "Equipo Alpha");

        // Integrante del equipo -> 200
        assertThat(sesion(e.estudiante1()).get("/api/equipos/" + e.idEquipo() + "/historial").statusCode()).isEqualTo(200);

        // Docente dueño -> 200
        assertThat(sesion(e.docente()).get("/api/equipos/" + e.idEquipo() + "/historial").statusCode()).isEqualTo(200);

        // Docente de otro proyecto -> 403 ACCESO_DENEGADO
        HttpResponse<String> rDocOtro = sesion(e.docenteOtro()).get("/api/equipos/" + e.idEquipo() + "/historial");
        assertThat(rDocOtro.statusCode()).isEqualTo(403);
        assertThat(rDocOtro.body()).contains("ACCESO_DENEGADO");

        // Integrante de otro equipo -> 403 ACCESO_DENEGADO
        HttpResponse<String> rEstOtro = sesion(e.estudianteOtro()).get("/api/equipos/" + e.idEquipo() + "/historial");
        assertThat(rEstOtro.statusCode()).isEqualTo(403);
        assertThat(rEstOtro.body()).contains("ACCESO_DENEGADO");
    }

    @Test
    void integranteSoloRecibeSuPropiaNotaConfirmada() {
        Escenario e = crearEscenario();
        auditar(e.idDocente(), "hito_equipo", e.idHitoEquipo(), "CONFIRMAR_NOTA", "nota_final:" + e.idEstudiante1(),
                "3.50", "4.00");
        auditar(e.idDocente(), "hito_equipo", e.idHitoEquipo(), "CONFIRMAR_NOTA", "nota_final:" + e.idEstudiante2(),
                "4.20", "4.80");

        // Integrante 1 recibe solo su nota confirmada
        HttpResponse<String> rEst1 = sesion(e.estudiante1()).get("/api/equipos/" + e.idEquipo() + "/historial");
        assertThat(rEst1.statusCode()).isEqualTo(200);
        assertThat(rEst1.body()).contains("nota_final:" + e.idEstudiante1())
                .doesNotContain("nota_final:" + e.idEstudiante2());

        // Integrante 2 recibe solo su nota confirmada
        HttpResponse<String> rEst2 = sesion(e.estudiante2()).get("/api/equipos/" + e.idEquipo() + "/historial");
        assertThat(rEst2.statusCode()).isEqualTo(200);
        assertThat(rEst2.body()).contains("nota_final:" + e.idEstudiante2())
                .doesNotContain("nota_final:" + e.idEstudiante1());

        // Docente dueño recibe ambas notas confirmadas
        HttpResponse<String> rDoc = sesion(e.docente()).get("/api/equipos/" + e.idEquipo() + "/historial");
        assertThat(rDoc.statusCode()).isEqualTo(200);
        assertThat(rDoc.body()).contains("nota_final:" + e.idEstudiante1())
                .contains("nota_final:" + e.idEstudiante2());
    }

    @Test
    void notaGrupalOcultaValorAnteriorParaEstudianteYEsCompletaParaDocente() {
        Escenario e = crearEscenario();
        auditar(e.idDocente(), "hito_equipo", e.idHitoEquipo(), "EVALUAR_HITO", "nota_grupal", "3.20", "4.50");

        // Integrante: valorAnterior nulo y detalle recalculado ("nota_grupal: - -> 4.50")
        HttpResponse<String> rEst = sesion(e.estudiante1()).get("/api/equipos/" + e.idEquipo() + "/historial");
        assertThat(rEst.statusCode()).isEqualTo(200);
        assertThat(rEst.body()).contains("\"campo\":\"nota_grupal\"")
                .contains("\"valorAnterior\":null")
                .contains("\"valorNuevo\":\"4.50\"")
                .contains("\"detalle\":\"nota_grupal: - -> 4.50\"");

        UsuarioAutenticado usuarioEst = new UsuarioAutenticado(e.idEstudiante1(), "Estudiante Uno", e.estudiante1(),
                RolPlataforma.ESTUDIANTE);
        List<EntradaHistorial> entradasEst = servicioHistorial.consultar(usuarioEst, e.idEquipo(), 50);
        EntradaHistorial grupalEst = entradasEst.stream()
                .filter(ent -> "nota_grupal".equals(ent.campo()))
                .findFirst()
                .orElseThrow();
        assertThat(grupalEst.valorAnterior()).isNull();
        assertThat(grupalEst.valorNuevo()).isEqualTo("4.50");
        assertThat(grupalEst.detalle()).isEqualTo("nota_grupal: - -> 4.50");

        // Docente dueño: ve valorAnterior original y detalle completo
        HttpResponse<String> rDoc = sesion(e.docente()).get("/api/equipos/" + e.idEquipo() + "/historial");
        assertThat(rDoc.statusCode()).isEqualTo(200);
        assertThat(rDoc.body()).contains("\"campo\":\"nota_grupal\"")
                .contains("\"valorAnterior\":\"3.20\"")
                .contains("\"valorNuevo\":\"4.50\"")
                .contains("\"detalle\":\"nota_grupal: 3.20 -> 4.50\"");

        UsuarioAutenticado usuarioDoc = new UsuarioAutenticado(e.idDocente(), "Docente Principal", e.docente(),
                RolPlataforma.DOCENTE);
        List<EntradaHistorial> entradasDoc = servicioHistorial.consultar(usuarioDoc, e.idEquipo(), 50);
        EntradaHistorial grupalDoc = entradasDoc.stream()
                .filter(ent -> "nota_grupal".equals(ent.campo()))
                .findFirst()
                .orElseThrow();
        assertThat(grupalDoc.valorAnterior()).isEqualTo("3.20");
        assertThat(grupalDoc.valorNuevo()).isEqualTo("4.50");
        assertThat(grupalDoc.detalle()).isEqualTo("nota_grupal: 3.20 -> 4.50");
    }

    @Test
    void servicioReportePdfSigueIncluyendoTodasLasEntradasYNotas() {
        Escenario e = crearEscenario();
        auditar(e.idDocente(), "hito_equipo", e.idHitoEquipo(), "CONFIRMAR_NOTA", "nota_final:" + e.idEstudiante1(),
                "3.50", "4.00");
        auditar(e.idDocente(), "hito_equipo", e.idHitoEquipo(), "CONFIRMAR_NOTA", "nota_final:" + e.idEstudiante2(),
                "4.20", "4.80");
        auditar(e.idDocente(), "hito_equipo", e.idHitoEquipo(), "EVALUAR_HITO", "nota_grupal", "3.00", "4.50");

        // El endpoint del reporte responde 200 y genera el PDF
        HttpResponse<String> r = sesion(e.docente()).get("/api/equipos/" + e.idEquipo() + "/reporte.pdf");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.headers().firstValue("content-type")).hasValue("application/pdf");
        assertThat(r.body()).startsWith("%PDF-");

        // La consulta del PDF (ServicioHistorial.entradas) incluye todas las entradas de todos los estudiantes
        List<EntradaHistorial> entradas = servicioHistorial.entradas(e.idEquipo(), true, 100);
        assertThat(entradas).extracting(EntradaHistorial::campo)
                .contains("nota_final:" + e.idEstudiante1(), "nota_final:" + e.idEstudiante2(), "nota_grupal");
        EntradaHistorial grupal = entradas.stream()
                .filter(ent -> "nota_grupal".equals(ent.campo()))
                .findFirst()
                .orElseThrow();
        assertThat(grupal.valorAnterior()).isEqualTo("3.00");
        assertThat(grupal.valorNuevo()).isEqualTo("4.50");
    }

    @Test
    void idUsuarioHostilNoAlteraConsulta() {
        Escenario e = crearEscenario();
        long he = e.idHitoEquipo();

        // Varias filas en log_auditoria con posibles prefijos o caracteres conflictivos
        auditar(e.idDocente(), "hito_equipo", he, "CONFIRMAR_NOTA", "nota_final:" + e.idEstudiante1(), "3.00", "4.00");
        auditar(e.idDocente(), "hito_equipo", he, "CONFIRMAR_NOTA", "nota_final:" + e.idEstudiante1() + "0", "3.00", "4.00");
        auditar(e.idDocente(), "hito_equipo", he, "CONFIRMAR_NOTA", "nota_final:" + e.idEstudiante1() + "%", "3.00", "4.00");
        auditar(e.idDocente(), "hito_equipo", he, "CONFIRMAR_NOTA", "nota_final:" + e.idEstudiante1() + "_", "3.00", "4.00");
        auditar(e.idDocente(), "hito_equipo", he, "CONFIRMAR_NOTA", "nota_final:" + e.idEstudiante1() + "' OR '1'='1", "3.00", "4.00");

        // El estudiante 1 solo recibe exactamente su nota_final:idEstudiante1
        HttpResponse<String> r = sesion(e.estudiante1()).get("/api/equipos/" + e.idEquipo() + "/historial");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("nota_final:" + e.idEstudiante1())
                .doesNotContain("nota_final:" + e.idEstudiante1() + "0")
                .doesNotContain("nota_final:" + e.idEstudiante1() + "%")
                .doesNotContain("nota_final:" + e.idEstudiante1() + "_")
                .doesNotContain("OR '1'='1");

        UsuarioAutenticado usuarioEst = new UsuarioAutenticado(e.idEstudiante1(), "Estudiante Uno", e.estudiante1(),
                RolPlataforma.ESTUDIANTE);
        List<EntradaHistorial> entradas = servicioHistorial.consultar(usuarioEst, e.idEquipo(), 50);
        List<String> camposConfirmar = entradas.stream()
                .filter(ent -> "CONFIRMAR_NOTA".equals(ent.accion()))
                .map(EntradaHistorial::campo)
                .toList();
        assertThat(camposConfirmar).containsExactly("nota_final:" + e.idEstudiante1());
    }
}
