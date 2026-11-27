package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.configuracionacademica.cursos.CursoResumen;
import java.util.List;

/** Estudiante presente en dos o más cursos asignados al mismo proyecto y el curso con que cuenta. */
public record EstudianteRepetidoVista(
        long idUsuario, String nombre, String email, List<CursoResumen> cursos, long idCursoQueCuenta) {}
