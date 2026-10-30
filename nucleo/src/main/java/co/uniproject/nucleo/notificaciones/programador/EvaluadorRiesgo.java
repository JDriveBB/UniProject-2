package co.uniproject.nucleo.notificaciones.programador;

import java.util.List;

public interface EvaluadorRiesgo {
    /** Evalúa TODAS las tareas de la ruta crítica de los proyectos activos según RF9. */
    List<EvaluacionRiesgo> evaluarTareasRutaCritica();
}
