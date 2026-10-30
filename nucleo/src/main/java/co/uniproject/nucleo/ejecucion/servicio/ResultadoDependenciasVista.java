package co.uniproject.nucleo.ejecucion.servicio;

import java.util.List;

/** aceptados: guardados. ciclicos: excluidos por dependencia circular (flujo alterno del caso de uso). */
public record ResultadoDependenciasVista(List<Long> aceptados, List<Long> ciclicos, TareaVista tarea) {}
