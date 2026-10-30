package co.uniproject.nucleo.ejecucion.servicio;

import java.math.BigDecimal;
import java.util.List;

/** RF3 para graficar: nodos en orden topológico con sus tiempos CPM en horas desde el inicio del plan. */
public record RutaCriticaVista(long idHitoEquipo, BigDecimal duracionTotalHoras, List<Long> criticas,
                               List<Nodo> nodos) {

    public record Nodo(long idTarea, String titulo, BigDecimal horasEstimadas, int porcentajeAvance, boolean critica,
                       BigDecimal inicioTempranoHoras, BigDecimal finTempranoHoras, BigDecimal inicioTardioHoras,
                       BigDecimal finTardioHoras, BigDecimal holguraHoras, List<Long> prerrequisitos) {}
}
