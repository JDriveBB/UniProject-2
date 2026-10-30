package co.uniproject.nucleo.evaluacion.calificacion;

import java.math.BigDecimal;

/** Resultado por estudiante: aporte exacto (horas ponderadas), ice con 4 decimales, notaSugerida con 2 (0..5). */
public record NotaSugerida(long idEstudiante, BigDecimal aporte, BigDecimal ice, BigDecimal notaSugerida) {}
