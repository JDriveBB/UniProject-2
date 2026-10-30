package co.uniproject.eventos;

/** Rutas de la aplicación web a las que apuntan los avisos. Contrato con el frontend: si cambia una ruta, se cambia aquí. */
public final class RutasWeb {
    private RutasWeb() {}
    public static String tarea(long tareaId) { return "/tareas/" + tareaId; }
    public static String misTareas() { return "/mis-tareas"; }
    public static String tableroEquipo(long equipoId) { return "/equipos/" + equipoId + "/tablero"; }
    public static String evaluarHito(long hitoId) { return "/hitos/" + hitoId + "/evaluar"; }
    public static String evaluacionHito(long hitoId) { return "/hitos/" + hitoId + "/evaluacion"; }
}
