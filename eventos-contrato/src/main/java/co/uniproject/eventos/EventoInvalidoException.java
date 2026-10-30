package co.uniproject.eventos;

/** El evento no es JSON, no tiene un type conocido o no cumple su JSON Schema. Error permanente. */
public class EventoInvalidoException extends RuntimeException {
    public EventoInvalidoException(String mensaje) { super(mensaje); }
    public EventoInvalidoException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
