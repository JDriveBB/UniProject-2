package co.uniproject.nucleo.configuracionacademica.servicio;

import java.util.List;

public record EquipoVista(
        long idEquipo,
        long idProyecto,
        String nombreEquipo,
        List<MiembroVista> miembros,
        List<HitoEquipoVista> hitos,
        Long idCurso,
        Integer cupo) {}
