package co.uniproject.nucleo.evaluacion.servicio;

import co.uniproject.eventos.datos.CaracterNota;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** notaGrupal: la vigente. notaCalculada: la de la rúbrica (solo para el docente; null si la rúbrica cambió). */
public record EvaluacionVista(long idEvaluacion, long idHitoEquipo, long idEnvio, Instant fechaEnvio,
                              Instant fechaEvaluacion, BigDecimal notaGrupal, BigDecimal notaCalculada,
                              String retroalimentacion, CaracterNota caracter, List<CriterioEvaluadoVista> criterios,
                              List<NotaIndividualVista> notas) {}
