package co.uniproject.nucleo.configuracionacademica.servicio;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Nuevo cupo del equipo (null lo convierte en equipo armado por el docente). */
public record SolicitudCupo(@Min(1) @Max(100) Integer cupo) {}
