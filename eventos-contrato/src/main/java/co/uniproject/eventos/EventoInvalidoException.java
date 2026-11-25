package co.uniproject.eventos;

import java.util.regex.Pattern;

/**
 * El evento no es JSON, no tiene un type conocido o no cumple su JSON Schema. Error permanente.
 * El mensaje se sanea al construirse (decisiones-tecnicas-java sección 12): el type y los errores del esquema vienen
 * del cuerpo recibido y el consumidor registra getMessage(); sin controles ni saltos de línea no se pueden
 * falsificar líneas de log.
 */
public class EventoInvalidoException extends RuntimeException {

    /** Largo máximo del mensaje saneado, sin contar el "..." final. */
    static final int LARGO_MAXIMO = 500;

    private static final Pattern NO_IMPRIMIBLES = Pattern.compile("[\\p{Cc}\\p{Zl}\\p{Zp}]");

    public EventoInvalidoException(String mensaje) { super(sanear(mensaje)); }
    public EventoInvalidoException(String mensaje, Throwable causa) { super(sanear(mensaje), causa); }

    /** Cambia controles (CR, LF, TAB, NEL…) y separadores de línea Unicode por '?' y recorta a LARGO_MAXIMO. */
    static String sanear(String mensaje) {
        String limpio = NO_IMPRIMIBLES.matcher(mensaje).replaceAll("?");
        return limpio.length() <= LARGO_MAXIMO ? limpio : limpio.substring(0, LARGO_MAXIMO) + "...";
    }
}
