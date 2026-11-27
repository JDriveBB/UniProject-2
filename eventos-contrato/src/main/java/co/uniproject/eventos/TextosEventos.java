package co.uniproject.eventos;

import co.uniproject.eventos.datos.MovimientoEquipo;
import java.util.List;

/** Textos compartidos entre la bandeja del núcleo y las plantillas del agente de Telegram. */
public final class TextosEventos {

    private TextosEventos() {}

    /** Une nombres en español: «A», «A y B», «A, B y C». */
    public static String listaNombres(List<String> nombres) {
        if (nombres == null || nombres.isEmpty()) {
            throw new IllegalArgumentException("La lista de nombres no puede estar vacía");
        }
        if (nombres.size() == 1) {
            return nombres.getFirst();
        }
        if (nombres.size() == 2) {
            return nombres.get(0) + " y " + nombres.get(1);
        }
        return String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.getLast();
    }

    /** Formatea una cantidad con su sustantivo en singular o plural. */
    public static String cantidad(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    /** Verbo conjugado para el movimiento de una o varias personas en un equipo. */
    public static String verbo(MovimientoEquipo movimiento, int personas) {
        return switch (movimiento) {
            case ENTRO -> personas == 1 ? "entró a" : "entraron a";
            case SALIO -> personas == 1 ? "salió de" : "salieron de";
        };
    }
}
