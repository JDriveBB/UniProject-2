package co.uniproject.nucleo.trazabilidad.servicio;

import co.uniproject.eventos.datos.CaracterNota;
import co.uniproject.nucleo.evaluacion.calificacion.CaracterEvaluacion;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import co.uniproject.nucleo.trazabilidad.reporte.GeneradorReportePdf;
import co.uniproject.nucleo.trazabilidad.reporte.ReporteTrazabilidad;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF15: reporte de trazabilidad del equipo en PDF (decisiones 21-4 y 21-5). Seis consultas fijas. */
@Service
public class ServicioReporte {

    private static final int MAXIMO_CAMBIOS = 5000;

    private static final String CABECERA = "SELECT p.nombre AS proyecto, e.nombre_equipo, d.nombre AS docente "
            + "FROM equipo e JOIN proyecto p ON p.id_proyecto = e.id_proyecto "
            + "JOIN usuario d ON d.id_usuario = p.id_docente WHERE e.id_equipo = :equipo";

    private static final String INTEGRANTES = "SELECT u.nombre, m.rol_equipo FROM miembro_equipo m "
            + "JOIN usuario u ON u.id_usuario = m.id_usuario WHERE m.id_equipo = :equipo "
            + "ORDER BY (m.rol_equipo <> 'LIDER'), u.nombre, u.id_usuario";

    private static final String ENTREGAS = "SELECT h.nombre_hito, en.fecha_envio, u.nombre FROM envio_hito en "
            + "JOIN hito_equipo he ON he.id_hito_equipo = en.id_hito_equipo JOIN hito h ON h.id_hito = he.id_hito "
            + "JOIN usuario u ON u.id_usuario = en.id_usuario_envia WHERE he.id_equipo = :equipo "
            + "ORDER BY en.fecha_envio, en.id_envio";

    private static final String NOTAS = "SELECT ni.id_evaluacion, u.nombre, ni.nota_sugerida, ni.nota_final "
            + "FROM nota_individual ni JOIN usuario u ON u.id_usuario = ni.id_estudiante "
            + "JOIN evaluacion_hito ev ON ev.id_evaluacion = ni.id_evaluacion "
            + "JOIN envio_hito en ON en.id_envio = ev.id_envio "
            + "JOIN hito_equipo he ON he.id_hito_equipo = en.id_hito_equipo WHERE he.id_equipo = :equipo "
            + "ORDER BY ni.id_evaluacion, u.nombre, ni.id_estudiante";

    private static final String CALIFICACIONES = "SELECT ev.id_evaluacion, h.nombre_hito, h.fecha_limite, "
            + "ev.nota_grupal, ev.es_definitiva FROM evaluacion_hito ev JOIN envio_hito en ON en.id_envio = ev.id_envio "
            + "JOIN hito_equipo he ON he.id_hito_equipo = en.id_hito_equipo JOIN hito h ON h.id_hito = he.id_hito "
            + "WHERE he.id_equipo = :equipo ORDER BY ev.fecha_evaluacion, ev.id_evaluacion";

    private record Cabecera(String proyecto, String equipo, String docente) {}

    private final ServicioHistorial historial;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioReporte(ServicioHistorial historial, Permisos permisos, RegistroAuditoria auditoria,
                           JdbcClient jdbc, Clock reloj) {
        this.historial = historial;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional
    public byte[] generar(UsuarioAutenticado usuario, long idEquipo) {
        Optional<Long> idProyecto = jdbc.sql("SELECT id_proyecto FROM equipo WHERE id_equipo = :equipo")
                .param("equipo", idEquipo)
                .query(Long.class)
                .optional();
        permisos.exigir(idProyecto.isPresent() && permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto.get()));
        Instant ahora = Instant.now(reloj);
        Cabecera cabecera = jdbc.sql(CABECERA).param("equipo", idEquipo)
                .query((rs, fila) -> new Cabecera(rs.getString("proyecto"), rs.getString("nombre_equipo"),
                        rs.getString("docente")))
                .single();
        List<ReporteTrazabilidad.Integrante> integrantes = jdbc.sql(INTEGRANTES).param("equipo", idEquipo)
                .query((rs, fila) -> new ReporteTrazabilidad.Integrante(texto(rs.getString("nombre")),
                        "LIDER".equals(rs.getString("rol_equipo")) ? "Líder" : "Integrante"))
                .list();
        List<ReporteTrazabilidad.Cambio> cambios = historial.entradas(idEquipo, true, MAXIMO_CAMBIOS).stream()
                .map(h -> new ReporteTrazabilidad.Cambio(h.fechaHora(), texto(h.autor()), texto(h.accion()),
                        h.detalle()))
                .toList();
        List<ReporteTrazabilidad.Entrega> entregas = jdbc.sql(ENTREGAS).param("equipo", idEquipo)
                .query((rs, fila) -> new ReporteTrazabilidad.Entrega(texto(rs.getString("nombre_hito")),
                        rs.getObject("fecha_envio", OffsetDateTime.class).toInstant(), texto(rs.getString("nombre"))))
                .list();
        Map<Long, List<ReporteTrazabilidad.NotaIndividual>> notas = new LinkedHashMap<>();
        jdbc.sql(NOTAS).param("equipo", idEquipo)
                .query((rs, fila) -> Map.entry(rs.getLong("id_evaluacion"), new ReporteTrazabilidad.NotaIndividual(
                        texto(rs.getString("nombre")), rs.getBigDecimal("nota_sugerida"),
                        rs.getBigDecimal("nota_final"))))
                .list()
                .forEach(par -> notas.computeIfAbsent(par.getKey(), k -> new ArrayList<>()).add(par.getValue()));
        List<ReporteTrazabilidad.Calificacion> calificaciones = jdbc.sql(CALIFICACIONES).param("equipo", idEquipo)
                .query((rs, fila) -> {
                    CaracterNota caracter = CaracterEvaluacion.de(rs.getBoolean("es_definitiva"),
                            rs.getObject("fecha_limite", LocalDate.class), ahora);
                    return new ReporteTrazabilidad.Calificacion(texto(rs.getString("nombre_hito")),
                            rs.getBigDecimal("nota_grupal"),
                            caracter == CaracterNota.DEFINITIVA ? "definitiva" : "provisional",
                            notas.getOrDefault(rs.getLong("id_evaluacion"), List.of()));
                })
                .list();
        ReporteTrazabilidad reporte = new ReporteTrazabilidad(texto(cabecera.proyecto()), texto(cabecera.equipo()),
                texto(cabecera.docente()), ahora, integrantes, cambios, entregas, calificaciones);
        byte[] pdf = new GeneradorReportePdf().generar(reporte);
        auditoria.registrar(EntradaAuditoria.de(usuario.idUsuario(), "equipo", idEquipo,
                AccionAuditoria.EXPORTAR_REPORTE));
        return pdf;
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
