package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.configuracionacademica.cursos.CursoResumen;
import java.util.List;

/** Resultado de asignar cursos a un proyecto o de su vista previa antes de confirmar. */
public record ResultadoCursosProyecto(
        boolean aplicado, List<CursoResumen> cursos, List<EstudianteRepetidoVista> repetidos) {}
