package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.ejecucion.dependencias.NivelRiesgo;
import co.uniproject.nucleo.ejecucion.dominio.EstadoTarea;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Tarea con su encargado (solo el nombre, Ley 1581) y los ids de sus prerrequisitos, ordenados. */
public record TareaVista(long idTarea, long idHitoEquipo, String titulo, String descripcion, Long idEncargado,
                         String encargado, Long idTareaOrigen, EstadoTarea estado, int porcentajeAvance,
                         BigDecimal horasEstimadas, LocalDate fechaLimite, LocalDate fechaLimiteEfectiva,
                         Instant fechaCompletada, boolean esPrerrequisitoCritico, boolean bloqueada,
                         Instant fechaBloqueo, boolean enRutaCritica, BigDecimal holguraHoras,
                         NivelRiesgo nivelRiesgo, List<Long> prerrequisitos) {

    public TareaVista conPrerrequisitos(List<Long> lista) {
        return new TareaVista(idTarea, idHitoEquipo, titulo, descripcion, idEncargado, encargado, idTareaOrigen, estado,
                porcentajeAvance, horasEstimadas, fechaLimite, fechaLimiteEfectiva, fechaCompletada,
                esPrerrequisitoCritico, bloqueada, fechaBloqueo, enRutaCritica, holguraHoras, nivelRiesgo,
                List.copyOf(lista));
    }
}
