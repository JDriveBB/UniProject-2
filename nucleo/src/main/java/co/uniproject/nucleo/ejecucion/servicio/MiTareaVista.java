package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.ejecucion.dependencias.NivelRiesgo;
import co.uniproject.nucleo.ejecucion.dominio.EstadoTarea;
import java.time.LocalDate;

/** Responsabilidad 1 del estudiante (entrega1 sección 2.2): sus tareas y fechas límite, en todos sus proyectos. */
public record MiTareaVista(long idTarea, String titulo, EstadoTarea estado, int porcentajeAvance,
                           LocalDate fechaLimite, boolean bloqueada, NivelRiesgo nivelRiesgo, long idHitoEquipo,
                           String nombreHito, String proyecto) {}
