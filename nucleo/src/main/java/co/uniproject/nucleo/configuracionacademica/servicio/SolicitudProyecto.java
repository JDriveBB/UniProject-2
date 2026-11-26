package co.uniproject.nucleo.configuracionacademica.servicio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Crear o reemplazar un proyecto (RF1). usaMonitores: RF14. equiposMixtos: solo se elige al crear. */
public record SolicitudProyecto(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 2000) String descripcion,
        @NotNull LocalDate fechaInicio,
        @NotNull LocalDate fechaFin,
        @NotNull Boolean usaMonitores,
        Boolean equiposMixtos) {}
