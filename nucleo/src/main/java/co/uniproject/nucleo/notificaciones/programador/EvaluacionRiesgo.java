package co.uniproject.nucleo.notificaciones.programador;

import co.uniproject.eventos.datos.TareaRutaCriticaRiesgo;

/** datos != null si y solo si enRiesgo. */
public record EvaluacionRiesgo(long tareaId, boolean enRiesgo, TareaRutaCriticaRiesgo datos) {}
