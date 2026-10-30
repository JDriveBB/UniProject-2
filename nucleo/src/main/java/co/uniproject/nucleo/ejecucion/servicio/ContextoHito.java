package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.ejecucion.dominio.EstadoHitoEquipo;
import java.time.LocalDate;

/** Hito de un equipo con lo que necesitan las reglas y los eventos (una sola consulta en ConsultasEjecucion). */
public record ContextoHito(long idHitoEquipo, long idHito, String nombreHito, LocalDate fechaInicio,
                           LocalDate fechaLimite, EstadoHitoEquipo estado, long idEquipo, String nombreEquipo,
                           long idProyecto, String nombreProyecto, long idDocente) {}
