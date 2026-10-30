package co.uniproject.nucleo.comun.error;

/** 404. mensaje: frase completa para el usuario, p. ej. "La tarea no existe". */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
