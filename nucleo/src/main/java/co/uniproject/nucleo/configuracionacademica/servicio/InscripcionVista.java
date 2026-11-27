package co.uniproject.nucleo.configuracionacademica.servicio;

import java.time.Instant;
import java.util.List;

/** Estado de autoinscripción a equipos del proyecto y equipos visibles para el usuario. */
public record InscripcionVista(
        Instant cierreEquipos,
        int horasRonda,
        boolean abierta,
        Long idCursoQueCuenta,
        Long idMiEquipo,
        List<EquipoInscripcionVista> equipos) {}
