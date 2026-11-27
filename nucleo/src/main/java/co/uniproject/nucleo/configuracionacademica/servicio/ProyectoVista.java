package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.configuracionacademica.dominio.Proyecto;
import java.time.Instant;
import java.time.LocalDate;

public record ProyectoVista(
        long idProyecto,
        long idDocente,
        String nombre,
        String descripcion,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean usaMonitores,
        boolean equiposMixtos,
        Instant cierreEquipos,
        int horasRonda) {

    public static ProyectoVista de(Proyecto p) {
        return new ProyectoVista(
                p.getIdProyecto(),
                p.getIdDocente(),
                p.getNombre(),
                p.getDescripcion(),
                p.getFechaInicio(),
                p.getFechaFin(),
                p.getUsaMonitores(),
                Boolean.TRUE.equals(p.getEquiposMixtos()),
                p.getCierreEquipos(),
                p.getHorasRonda());
    }
}
