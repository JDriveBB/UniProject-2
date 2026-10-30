package co.uniproject.nucleo.ejecucion.dependencias;

import java.util.List;

/** Caso de uso "Establecer dependencias": aceptados se guardan; ciclicos se excluyen y se avisan al líder. Ambos ordenados. */
public record ResultadoDependencias(List<Long> aceptados, List<Long> ciclicos) {}
