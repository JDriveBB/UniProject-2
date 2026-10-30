package co.uniproject.nucleo.evaluacion.servicio;

import co.uniproject.eventos.datos.CaracterNota;
import co.uniproject.eventos.datos.HitoEvaluado;
import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.Referencia;
import co.uniproject.nucleo.comun.FechasDominio;
import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.configuracionacademica.servicio.Textos;
import co.uniproject.nucleo.ejecucion.dominio.EstadoHitoEquipo;
import co.uniproject.nucleo.ejecucion.servicio.ConsultasEjecucion;
import co.uniproject.nucleo.ejecucion.servicio.ContextoHito;
import co.uniproject.nucleo.ejecucion.servicio.LectorTareas;
import co.uniproject.nucleo.evaluacion.calificacion.CalculadoraIce;
import co.uniproject.nucleo.evaluacion.calificacion.CaracterEvaluacion;
import co.uniproject.nucleo.evaluacion.calificacion.CriterioCalificado;
import co.uniproject.nucleo.evaluacion.calificacion.NotaSugerida;
import co.uniproject.nucleo.evaluacion.calificacion.TareaCompletada;
import co.uniproject.nucleo.notificaciones.RegistroEventos;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF8, C1 y E1: evaluación del hito, nota grupal editable y notas individuales (decisiones 19-3 a 19-10). */
@Service
public class ServicioEvaluacion {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private record Criterio(long id, BigDecimal peso, BigDecimal maximo) {}

    private final ConsultasEjecucion consultas;
    private final LectorEvaluaciones lector;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final RegistroEventos eventos;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioEvaluacion(ConsultasEjecucion consultas, LectorEvaluaciones lector, Permisos permisos,
                              RegistroAuditoria auditoria, RegistroEventos eventos, JdbcClient jdbc, Clock reloj) {
        this.consultas = consultas;
        this.lector = lector;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.eventos = eventos;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional
    public EvaluacionVista evaluar(UsuarioAutenticado usuario, long idHitoEquipo, SolicitudEvaluacion s) {
        long id = usuario.idUsuario();
        permisos.exigir(permisos.puedeEvaluarHito(id, idHitoEquipo));
        ServicioEnvio.bloquear(jdbc, idHitoEquipo);
        ContextoHito hito = consultas.hito(idHitoEquipo).orElseThrow();
        if (hito.estado() != EstadoHitoEquipo.ENVIADO) {
            throw new ReglaNegocioException("HITO_NO_ENVIADO", "Solo se evalúa un hito enviado que aún no tiene nota");
        }
        Instant ahora = Instant.now(reloj);
        Map<Long, Criterio> rubrica = new LinkedHashMap<>();
        jdbc.sql("SELECT id_criterio, peso_porcentaje, puntaje_maximo FROM criterio_rubrica WHERE id_hito = :hito "
                        + "ORDER BY id_criterio")
                .param("hito", hito.idHito())
                .query((rs, fila) -> new Criterio(rs.getLong("id_criterio"), rs.getBigDecimal("peso_porcentaje"),
                        rs.getBigDecimal("puntaje_maximo")))
                .list()
                .forEach(c -> rubrica.put(c.id(), c));
        BigDecimal pesos = rubrica.values().stream().map(Criterio::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (pesos.compareTo(CIEN) != 0) {
            throw new ReglaNegocioException("PESOS_NO_SUMAN_100",
                    "Los pesos de la rúbrica del hito deben sumar 100 antes de evaluar");
        }
        Set<Long> calificados = new HashSet<>();
        List<CriterioCalificado> notasCriterio = new ArrayList<>();
        for (CalificacionSolicitada c : s.criterios()) {
            Criterio r = rubrica.get(c.idCriterio());
            if (r == null || !calificados.add(c.idCriterio())) {
                throw rubricaIncompleta();
            }
            if (c.nota().compareTo(r.maximo()) > 0) {
                throw new SolicitudInvalidaException("NOTA_FUERA_DE_RANGO",
                        "La nota de un criterio no puede superar su puntaje máximo");
            }
            notasCriterio.add(new CriterioCalificado(c.nota(), r.maximo(), r.peso()));
        }
        if (calificados.size() != rubrica.size()) {
            throw rubricaIncompleta();
        }
        List<Long> miembros = consultas.miembros(hito.idEquipo());
        if (miembros.isEmpty()) {
            throw new ReglaNegocioException("EQUIPO_SIN_INTEGRANTES", "El equipo no tiene integrantes que calificar");
        }
        BigDecimal calculada = CalculadoraIce.notaGrupal(notasCriterio);
        BigDecimal grupal = s.notaGrupal() == null ? calculada : s.notaGrupal().setScale(2, RoundingMode.HALF_UP);
        boolean definitiva = !ahora.isBefore(FechasDominio.venceEn(hito.fechaLimite()));
        long idEnvio = jdbc.sql("SELECT id_envio FROM envio_hito WHERE id_hito_equipo = :he "
                        + "ORDER BY fecha_envio DESC, id_envio DESC LIMIT 1")
                .param("he", idHitoEquipo)
                .query(Long.class)
                .single();
        long idEvaluacion = jdbc.sql("INSERT INTO evaluacion_hito (id_envio, id_docente, nota_grupal, "
                        + "retroalimentacion_general, es_definitiva, fecha_evaluacion) "
                        + "VALUES (:envio, :docente, :nota, :retro, :definitiva, :fecha) RETURNING id_evaluacion")
                .param("envio", idEnvio)
                .param("docente", id)
                .param("nota", grupal)
                .param("retro", Textos.opcional(s.retroalimentacion()), Types.VARCHAR)
                .param("definitiva", definitiva)
                .param("fecha", OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                .query(Long.class)
                .single();
        for (CalificacionSolicitada c : s.criterios()) {
            jdbc.sql("INSERT INTO calificacion_criterio (id_evaluacion, id_criterio, nota, retroalimentacion) "
                            + "VALUES (:ev, :criterio, :nota, :retro)")
                    .param("ev", idEvaluacion)
                    .param("criterio", c.idCriterio())
                    .param("nota", c.nota())
                    .param("retro", Textos.opcional(c.retroalimentacion()), Types.VARCHAR)
                    .update();
        }
        List<TareaCompletada> tareas = jdbc.sql("SELECT t.id_encargado, t.horas_estimadas, t.fecha_completada, "
                        + "coalesce(t.fecha_limite, :limite) AS limite FROM tarea t WHERE t.id_hito_equipo = :he "
                        + "AND t.estado = 'COMPLETADA' AND t.id_encargado IN "
                        + "(SELECT id_usuario FROM miembro_equipo WHERE id_equipo = :equipo)")
                .param("limite", hito.fechaLimite())
                .param("he", idHitoEquipo)
                .param("equipo", hito.idEquipo())
                .query((rs, fila) -> new TareaCompletada(rs.getLong("id_encargado"), rs.getBigDecimal("horas_estimadas"),
                        LectorTareas.instante(rs, "fecha_completada")
                                .isBefore(FechasDominio.venceEn(rs.getObject("limite", LocalDate.class)))))
                .list();
        for (NotaSugerida n : CalculadoraIce.calcular(grupal, miembros, tareas)) {
            jdbc.sql("INSERT INTO nota_individual (id_evaluacion, id_estudiante, factor_ajuste, nota_sugerida) "
                            + "VALUES (:ev, :estudiante, :ice, :nota)")
                    .param("ev", idEvaluacion)
                    .param("estudiante", n.idEstudiante())
                    .param("ice", n.ice())
                    .param("nota", n.notaSugerida())
                    .update();
        }
        jdbc.sql("UPDATE hito_equipo SET estado = 'EVALUADO' WHERE id_hito_equipo = :he")
                .param("he", idHitoEquipo)
                .update();
        jdbc.sql("UPDATE tarea SET edicion_habilitada = :abierta WHERE id_hito_equipo = :he")
                .param("abierta", !definitiva)
                .param("he", idHitoEquipo)
                .update();
        auditoria.registrar(EntradaAuditoria.cambio(id, "hito_equipo", idHitoEquipo, AccionAuditoria.EVALUAR_HITO,
                "nota_grupal", null, grupal.toPlainString()));
        if (grupal.compareTo(calculada) != 0) {
            auditoria.registrar(EntradaAuditoria.cambio(id, "hito_equipo", idHitoEquipo, AccionAuditoria.ACTUALIZAR,
                    "nota_grupal", calculada.toPlainString(), grupal.toPlainString()));
        }
        Map<Long, Persona> personas = consultas.personas(miembros);
        CaracterNota caracter = CaracterEvaluacion.de(definitiva, hito.fechaLimite(), ahora);
        eventos.registrar(new HitoEvaluado(new Referencia(hito.idProyecto(), hito.nombreProyecto()),
                new Referencia(hito.idEquipo(), hito.nombreEquipo()), new Referencia(idHitoEquipo, hito.nombreHito()),
                caracter, miembros.stream().map(personas::get).toList()));
        return lector.leer(idEvaluacion, id, true, ahora);
    }

    @Transactional(readOnly = true)
    public EvaluacionVista consultar(UsuarioAutenticado usuario, long idHitoEquipo) {
        long id = usuario.idUsuario();
        permisos.exigir(permisos.puedeVerHitoEquipo(id, idHitoEquipo));
        long idEvaluacion = lector.ultima(idHitoEquipo)
                .orElseThrow(() -> new RecursoNoEncontradoException("El hito aún no tiene evaluación"));
        return lector.leer(idEvaluacion, id, permisos.puedeEvaluarHito(id, idHitoEquipo), Instant.now(reloj));
    }

    /** Decisión 19-4: solo mientras no haya notas individuales confirmadas. */
    @Transactional
    public EvaluacionVista cambiarNotaGrupal(UsuarioAutenticado usuario, long idEvaluacion, CambioNotaGrupal s) {
        long id = usuario.idUsuario();
        long idHitoEquipo = exigirVigente(id, idEvaluacion);
        BigDecimal anterior = jdbc.sql("SELECT nota_grupal FROM evaluacion_hito WHERE id_evaluacion = :ev FOR UPDATE")
                .param("ev", idEvaluacion)
                .query(BigDecimal.class)
                .single();
        boolean confirmadas = jdbc.sql("SELECT EXISTS (SELECT 1 FROM nota_individual WHERE id_evaluacion = :ev "
                        + "AND nota_final IS NOT NULL)")
                .param("ev", idEvaluacion)
                .query(Boolean.class)
                .single();
        if (confirmadas) {
            throw new ReglaNegocioException("NOTAS_CONFIRMADAS",
                    "Ya confirmaste notas individuales: la nota grupal no se puede cambiar");
        }
        BigDecimal nueva = s.notaGrupal().setScale(2, RoundingMode.HALF_UP);
        if (anterior.compareTo(nueva) != 0) {
            jdbc.sql("UPDATE evaluacion_hito SET nota_grupal = :nota WHERE id_evaluacion = :ev")
                    .param("nota", nueva)
                    .param("ev", idEvaluacion)
                    .update();
            List<Map.Entry<Long, BigDecimal>> factores = jdbc.sql("SELECT id_nota_individual, factor_ajuste "
                            + "FROM nota_individual WHERE id_evaluacion = :ev")
                    .param("ev", idEvaluacion)
                    .query((rs, fila) -> Map.entry(rs.getLong("id_nota_individual"), rs.getBigDecimal("factor_ajuste")))
                    .list();
            for (Map.Entry<Long, BigDecimal> f : factores) {
                jdbc.sql("UPDATE nota_individual SET nota_sugerida = :nota WHERE id_nota_individual = :id")
                        .param("nota", CalculadoraIce.notaIndividual(nueva, f.getValue()))
                        .param("id", f.getKey())
                        .update();
            }
            auditoria.registrar(EntradaAuditoria.cambio(id, "hito_equipo", idHitoEquipo, AccionAuditoria.ACTUALIZAR,
                    "nota_grupal", anterior.toPlainString(), nueva.toPlainString()));
        }
        return lector.leer(idEvaluacion, id, true, Instant.now(reloj));
    }

    /** Decisión 19-7. */
    @Transactional
    public EvaluacionVista confirmarNota(UsuarioAutenticado usuario, long idEvaluacion, long idEstudiante,
                                        ConfirmacionNota s) {
        long id = usuario.idUsuario();
        long idHitoEquipo = exigirVigente(id, idEvaluacion);
        jdbc.sql("SELECT id_evaluacion FROM evaluacion_hito WHERE id_evaluacion = :ev FOR UPDATE")
                .param("ev", idEvaluacion)
                .query(Long.class)
                .single();
        List<BigDecimal> fila = jdbc.sql("SELECT nota_final FROM nota_individual WHERE id_evaluacion = :ev "
                        + "AND id_estudiante = :estudiante")
                .param("ev", idEvaluacion)
                .param("estudiante", idEstudiante)
                .query((rs, n) -> rs.getBigDecimal("nota_final"))
                .list();
        if (fila.isEmpty()) {
            throw new RecursoNoEncontradoException("Ese estudiante no tiene nota en esta evaluación");
        }
        BigDecimal anterior = fila.get(0);
        BigDecimal nueva = s.notaFinal().setScale(2, RoundingMode.HALF_UP);
        Instant ahora = Instant.now(reloj);
        jdbc.sql("UPDATE nota_individual SET nota_final = :nota, fecha_confirmacion = :fecha "
                        + "WHERE id_evaluacion = :ev AND id_estudiante = :estudiante")
                .param("nota", nueva)
                .param("fecha", OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                .param("ev", idEvaluacion)
                .param("estudiante", idEstudiante)
                .update();
        auditoria.registrar(EntradaAuditoria.cambio(id, "hito_equipo", idHitoEquipo, AccionAuditoria.CONFIRMAR_NOTA,
                "nota_final:" + idEstudiante, anterior == null ? null : anterior.toPlainString(),
                nueva.toPlainString()));
        return lector.leer(idEvaluacion, id, true, ahora);
    }

    /** Docente del proyecto (403 si no existe) y evaluación más reciente del hito (decisión 19-8). */
    private long exigirVigente(long idUsuario, long idEvaluacion) {
        Optional<Long> idHitoEquipo = lector.hitoEquipoDe(idEvaluacion);
        permisos.exigir(idHitoEquipo.isPresent() && permisos.puedeEvaluarHito(idUsuario, idHitoEquipo.get()));
        if (lector.ultima(idHitoEquipo.get()).orElseThrow() != idEvaluacion) {
            throw new ReglaNegocioException("EVALUACION_REEMPLAZADA", "Hay una evaluación más reciente de este hito");
        }
        return idHitoEquipo.get();
    }

    private static SolicitudInvalidaException rubricaIncompleta() {
        return new SolicitudInvalidaException("RUBRICA_INCOMPLETA",
                "Califica cada criterio de la rúbrica del hito exactamente una vez");
    }
}
