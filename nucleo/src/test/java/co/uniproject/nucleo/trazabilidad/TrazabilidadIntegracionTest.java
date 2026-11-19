package co.uniproject.nucleo.trazabilidad;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import co.uniproject.nucleo.trazabilidad.servicio.PanelEquipo;
import co.uniproject.nucleo.trazabilidad.servicio.PanelProyecto;
import co.uniproject.nucleo.trazabilidad.servicio.ServicioPanel;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrazabilidadIntegracionTest extends ApiIntegracionBase {

    @Autowired
    private ServicioPanel servicioPanel;

    private record Escenario(
            long idProyecto,
            long idDocente,
            String docente,
            long idEquipoA,
            long idEquipoB,
            long idHitoEquipoA,
            String liderA,
            String liderB,
            long idInactivo) {}

    /**
     * Equipo A: líder + "Integrante Inactivo" (tarea pendiente, sin actividad en 10 días; el hito alerta a los 3) y una
     * tarea bloqueada hace 50 h. Equipo B: líder con una tarea y avance registrado ahora. Miembros desde hace 10 días.
     */
    private Escenario escenario() {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String docente = correoUnico("doc");
        long idDocente = crearUsuario("Docente Panel", docente, "DOCENTE");
        String liderA = correoUnico("lidera");
        long idLiderA = crearUsuario("Lider A", liderA, "ESTUDIANTE");
        long idInactivo = crearUsuario("Integrante Inactivo", correoUnico("inactivo"), "ESTUDIANTE");
        String liderB = correoUnico("liderb");
        long idLiderB = crearUsuario("Lider B", liderB, "ESTUDIANTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Panel', :i, :f) RETURNING id_proyecto")
                .param("d", idDocente)
                .param("i", hoy.minusDays(30))
                .param("f", hoy.plusDays(60))
                .query(Long.class)
                .single();
        long hito = jdbc.sql("INSERT INTO hito (id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite, "
                        + "dias_inactividad_alerta) VALUES (:p, 'Hito Panel', 1, :i, :f, 3) RETURNING id_hito")
                .param("p", proyecto)
                .param("i", hoy.minusDays(10))
                .param("f", hoy.plusDays(20))
                .query(Long.class)
                .single();
        long equipoA = equipo(proyecto, "Equipo A");
        long equipoB = equipo(proyecto, "Equipo B");
        miembro(equipoA, idLiderA, "LIDER");
        miembro(equipoA, idInactivo, "INTEGRANTE");
        miembro(equipoB, idLiderB, "LIDER");
        long heA = hitoEquipo(hito, equipoA);
        long heB = hitoEquipo(hito, equipoB);
        jdbc.sql("INSERT INTO tarea (id_hito_equipo, id_encargado, titulo, horas_estimadas) "
                        + "VALUES (:he, :u, 'Pendiente olvidada', 4)")
                .param("he", heA)
                .param("u", idInactivo)
                .update();
        jdbc.sql(
                        "INSERT INTO tarea (id_hito_equipo, titulo, horas_estimadas, bloqueada_por_dependencia, fecha_bloqueo) "
                                + "VALUES (:he, 'Bloqueada vieja', 2, true, now() - interval '50 hours'), "
                                + "(:he, 'Bloqueada reciente', 2, true, now() - interval '1 hour')")
                .param("he", heA)
                .update();
        long tareaB = jdbc.sql("INSERT INTO tarea (id_hito_equipo, id_encargado, titulo, horas_estimadas) "
                        + "VALUES (:he, :u, 'Al dia', 3) RETURNING id_tarea")
                .param("he", heB)
                .param("u", idLiderB)
                .query(Long.class)
                .single();
        jdbc.sql("INSERT INTO registro_avance (id_tarea, id_usuario, porcentaje_avance) VALUES (:t, :u, 10)")
                .param("t", tareaB)
                .param("u", idLiderB)
                .update();
        return new Escenario(proyecto, idDocente, docente, equipoA, equipoB, heA, liderA, liderB, idInactivo);
    }

    private long equipo(long proyecto, String nombre) {
        return jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo) VALUES (:p, :n) RETURNING id_equipo")
                .param("p", proyecto)
                .param("n", nombre)
                .query(Long.class)
                .single();
    }

    private void miembro(long equipo, long usuario, String rol) {
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo, fecha_ingreso) "
                        + "VALUES (:e, :u, :r, now() - interval '10 days')")
                .param("e", equipo)
                .param("u", usuario)
                .param("r", rol)
                .update();
    }

    private long hitoEquipo(long hito, long equipo) {
        return jdbc.sql("INSERT INTO hito_equipo (id_hito, id_equipo) VALUES (:h, :e) RETURNING id_hito_equipo")
                .param("h", hito)
                .param("e", equipo)
                .query(Long.class)
                .single();
    }

    private void auditar(Long idUsuario, String entidad, long idRegistro, String accion, String campo, String nuevo) {
        jdbc.sql("INSERT INTO log_auditoria (id_usuario_accion, entidad_afectada, id_registro_afectado, accion, "
                        + "campo_modificado, valor_nuevo) VALUES (:u, :e, :r, :a, :c, :v)")
                .param("u", idUsuario, java.sql.Types.BIGINT)
                .param("e", entidad)
                .param("r", idRegistro)
                .param("a", accion)
                .param("c", campo, java.sql.Types.VARCHAR)
                .param("v", nuevo, java.sql.Types.VARCHAR)
                .update();
    }

    @Test
    void elPanelResaltaEquiposInactivosOBloqueados() {
        Escenario e = escenario();
        assertThat(sesion(e.docente())
                        .get("/api/proyectos/" + e.idProyecto() + "/panel")
                        .statusCode())
                .isEqualTo(200);
        assertThat(sesion(e.liderA())
                        .get("/api/proyectos/" + e.idProyecto() + "/panel")
                        .statusCode())
                .isEqualTo(403);
        PanelProyecto panel = servicioPanel.panel(
                new UsuarioAutenticado(e.idDocente(), "Docente Panel", e.docente(), RolPlataforma.DOCENTE),
                e.idProyecto());
        assertThat(panel.equipos()).extracting(PanelEquipo::nombreEquipo).containsExactly("Equipo A", "Equipo B");
        PanelEquipo a = panel.equipos().get(0);
        PanelEquipo b = panel.equipos().get(1);
        assertThat(a.enRojo()).isTrue();
        assertThat(a.integrantesInactivos()).extracting(i -> i.idUsuario()).containsExactly(e.idInactivo());
        assertThat(a.integrantesInactivos().get(0).diasSinActividad()).isGreaterThanOrEqualTo(9);
        assertThat(a.tareasBloqueadas()).extracting(t -> t.titulo()).containsExactly("Bloqueada vieja");
        assertThat(a.tareasBloqueadas().get(0).horasBloqueada()).isGreaterThanOrEqualTo(49);
        assertThat(a.tareasTotales()).isEqualTo(3);
        assertThat(b.enRojo()).isFalse();
        assertThat(b.integrantesInactivos()).isEmpty();
        assertThat(b.tareasBloqueadas()).isEmpty();
    }

    private long hito(long proyecto, String nombre, int orden) {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        return jdbc.sql("INSERT INTO hito (id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) "
                        + "VALUES (:p, :n, :o, :i, :f) RETURNING id_hito")
                .param("p", proyecto)
                .param("n", nombre)
                .param("o", orden)
                .param("i", hoy.minusDays(10))
                .param("f", hoy.plusDays(20))
                .query(Long.class)
                .single();
    }

    private void estadoHito(long hitoEquipo, String estado) {
        jdbc.sql("UPDATE hito_equipo SET estado = :s WHERE id_hito_equipo = :he")
                .param("s", estado)
                .param("he", hitoEquipo)
                .update();
    }

    private void tarea(long hitoEquipo, int horas, int avance) {
        jdbc.sql("INSERT INTO tarea (id_hito_equipo, titulo, horas_estimadas, porcentaje_avance, estado, "
                        + "fecha_completada) VALUES (:he, 'Tarea', :h, :a, :s, :fc)")
                .param("he", hitoEquipo)
                .param("h", horas)
                .param("a", avance)
                .param("s", avance == 100 ? "COMPLETADA" : "EN_PROGRESO")
                .param(
                        "fc",
                        avance == 100 ? OffsetDateTime.now(ZoneOffset.UTC) : null,
                        java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                .update();
    }

    private void envio(long hitoEquipo, long usuario, String fecha) {
        jdbc.sql("INSERT INTO envio_hito (id_hito_equipo, id_usuario_envia, fecha_envio) VALUES (:he, :u, :f)")
                .param("he", hitoEquipo)
                .param("u", usuario)
                .param("f", OffsetDateTime.parse(fecha))
                .update();
    }

    @Test
    void elPanelMideElHitoEnCursoPonderadoPorHorasYElProyectoTraeLaFechaDeEnvio() {
        String docente = correoUnico("doc");
        long idDocente = crearUsuario("Docente Avance", docente, "DOCENTE");
        long lider = crearUsuario("Lider Avance", correoUnico("lider"), "ESTUDIANTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Avance', :i, :f) RETURNING id_proyecto")
                .param("d", idDocente)
                .param("i", LocalDate.now(FechasDominio.ZONA).minusDays(30))
                .param("f", LocalDate.now(FechasDominio.ZONA).plusDays(60))
                .query(Long.class)
                .single();
        long hito2 = hito(proyecto, "Segundo", 2);
        long hito1 = hito(proyecto, "Primero", 1);
        long enCurso = equipo(proyecto, "Equipo 1 en curso");
        long terminado = equipo(proyecto, "Equipo 2 terminado");
        long sinTareas = equipo(proyecto, "Equipo 3 sin tareas");
        long ec1 = hitoEquipo(hito1, enCurso);
        long ec2 = hitoEquipo(hito2, enCurso);
        estadoHito(ec1, "ENVIADO");
        tarea(ec1, 10, 100);
        tarea(ec2, 10, 50);
        tarea(ec2, 2, 100);
        envio(ec1, lider, "2026-11-18T15:00:00Z");
        envio(ec1, lider, "2026-11-20T15:00:00Z");
        long te1 = hitoEquipo(hito1, terminado);
        long te2 = hitoEquipo(hito2, terminado);
        estadoHito(te1, "EVALUADO");
        estadoHito(te2, "ENVIADO");
        tarea(te1, 4, 100);
        hitoEquipo(hito1, sinTareas);
        hitoEquipo(hito2, sinTareas);

        PanelProyecto panel = servicioPanel.panel(
                new UsuarioAutenticado(idDocente, "Docente Avance", docente, RolPlataforma.DOCENTE), proyecto);
        assertThat(panel.equipos()).extracting(PanelEquipo::idEquipo).containsExactly(enCurso, terminado, sinTareas);
        PanelEquipo a = panel.equipos().get(0);
        assertThat(a.avanceHitoActual()).isEqualTo(58);
        assertThat(a.hitoActual()).isEqualTo(new PanelEquipo.HitoActualPanel(ec2, 2, "Segundo"));
        assertThat(a.todoEnviado()).isFalse();
        PanelEquipo b = panel.equipos().get(1);
        assertThat(b.hitoActual()).isNull();
        assertThat(b.avanceHitoActual()).isNull();
        assertThat(b.todoEnviado()).isTrue();
        PanelEquipo c = panel.equipos().get(2);
        assertThat(c.hitoActual()).isNotNull();
        assertThat(c.hitoActual().orden()).isEqualTo(1);
        assertThat(c.avanceHitoActual()).isNull();
        assertThat(c.todoEnviado()).isFalse();

        HttpResponse<String> r = sesion(docente).get("/api/proyectos/" + proyecto);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        assertThat(r.body())
                .contains("{\"idHitoEquipo\":" + ec1 + ",\"idHito\":" + hito1
                        + ",\"estado\":\"ENVIADO\",\"fechaEnvio\":\"2026-11-20T15:00:00Z\"}")
                .contains("{\"idHitoEquipo\":" + ec2 + ",\"idHito\":" + hito2
                        + ",\"estado\":\"EN_CURSO\",\"fechaEnvio\":null}");
    }

    @Test
    void elHistorialSoloTraeLoDelEquipo() {
        Escenario e = escenario();
        long tareaA = jdbc.sql(
                        "SELECT id_tarea FROM tarea WHERE id_hito_equipo = :he AND titulo = 'Pendiente olvidada'")
                .param("he", e.idHitoEquipoA())
                .query(Long.class)
                .single();
        auditar(e.idDocente(), "equipo", e.idEquipoA(), "CREAR", null, null);
        auditar(e.idInactivo(), "tarea", tareaA, "CAMBIAR_ESTADO", "estado", "marca-tarea-A");
        auditar(e.idDocente(), "hito_equipo", e.idHitoEquipoA(), "EVALUAR_HITO", "nota_grupal", "marca-hito-A");
        auditar(e.idDocente(), "equipo", e.idEquipoB(), "ACTUALIZAR", "lider", "marca-equipo-B");
        HttpResponse<String> r = sesion(e.liderA()).get("/api/equipos/" + e.idEquipoA() + "/historial");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body())
                .contains("marca-tarea-A")
                .contains("marca-hito-A")
                .contains("\"detalle\":\"estado: - -> marca-tarea-A\"")
                .doesNotContain("marca-equipo-B");
        assertThat(sesion(e.liderB())
                        .get("/api/equipos/" + e.idEquipoA() + "/historial")
                        .statusCode())
                .isEqualTo(403);
        assertThat(sesion(e.docente())
                        .get("/api/equipos/" + e.idEquipoA() + "/historial?limite=1")
                        .body())
                .contains("marca-hito-A")
                .doesNotContain("marca-tarea-A");
    }

    @Test
    void elDocenteExportaElPdfAunqueHayaCamposNulos() {
        Escenario e = escenario();
        auditar(null, "equipo", e.idEquipoA(), "CREAR", null, null);
        long envio = jdbc.sql("INSERT INTO envio_hito (id_hito_equipo, id_usuario_envia) VALUES (:he, :u) "
                        + "RETURNING id_envio")
                .param("he", e.idHitoEquipoA())
                .param("u", e.idInactivo())
                .query(Long.class)
                .single();
        long evaluacion = jdbc.sql("INSERT INTO evaluacion_hito (id_envio, id_docente, nota_grupal) "
                        + "VALUES (:en, :d, 4.00) RETURNING id_evaluacion")
                .param("en", envio)
                .param("d", e.idDocente())
                .query(Long.class)
                .single();
        jdbc.sql("INSERT INTO nota_individual (id_evaluacion, id_estudiante, factor_ajuste, nota_sugerida) "
                        + "VALUES (:ev, :u, 1.0000, 4.00)")
                .param("ev", evaluacion)
                .param("u", e.idInactivo())
                .update();
        HttpResponse<String> r = sesion(e.docente()).get("/api/equipos/" + e.idEquipoA() + "/reporte.pdf");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        assertThat(r.headers().firstValue("content-type")).hasValue("application/pdf");
        assertThat(r.headers().firstValue("content-disposition").orElse(""))
                .contains("attachment")
                .contains("reporte-equipo-" + e.idEquipoA() + ".pdf");
        assertThat(r.body()).startsWith("%PDF-");
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE accion = 'EXPORTAR_REPORTE' "
                                + "AND entidad_afectada = 'equipo' AND id_registro_afectado = :e")
                        .param("e", e.idEquipoA())
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void soloElDocenteDelProyectoExportaElPdf() {
        Escenario e = escenario();
        assertThat(sesion(e.liderA())
                        .get("/api/equipos/" + e.idEquipoA() + "/reporte.pdf")
                        .statusCode())
                .isEqualTo(403);
        String otroDocente = correoUnico("doc2");
        crearUsuario("Otro Docente", otroDocente, "DOCENTE");
        assertThat(sesion(otroDocente)
                        .get("/api/equipos/" + e.idEquipoA() + "/reporte.pdf")
                        .statusCode())
                .isEqualTo(403);
        assertThat(sesion(e.docente()).get("/api/equipos/999999999/reporte.pdf").statusCode())
                .isEqualTo(403);
    }
}
