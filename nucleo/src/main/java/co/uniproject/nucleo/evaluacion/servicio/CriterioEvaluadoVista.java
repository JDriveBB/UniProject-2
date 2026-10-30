package co.uniproject.nucleo.evaluacion.servicio;

import java.math.BigDecimal;

public record CriterioEvaluadoVista(long idCriterio, String descripcion, BigDecimal pesoPorcentaje,
                                    BigDecimal puntajeMaximo, BigDecimal nota, String retroalimentacion) {}
