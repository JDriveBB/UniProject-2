package co.uniproject.nucleo.configuracionacademica.servicio;

import java.util.List;

/** Equipo en la vista de autoinscripción: solo nombres de integrantes, sin correos ni ids de usuario (Ley 1581). */
public record EquipoInscripcionVista(
        long idEquipo,
        String nombreEquipo,
        Long idCurso,
        String nombreCurso,
        Integer cupo,
        int integrantes,
        List<String> nombres,
        boolean puedeUnirse) {}
