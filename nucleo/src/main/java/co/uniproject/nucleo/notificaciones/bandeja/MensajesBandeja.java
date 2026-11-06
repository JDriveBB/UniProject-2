package co.uniproject.nucleo.notificaciones.bandeja;

import co.uniproject.eventos.DestinatariosEvento;
import co.uniproject.eventos.datos.CaracterNota;
import co.uniproject.eventos.datos.ContrasenaCambiada;
import co.uniproject.eventos.datos.DatosEvento;
import co.uniproject.eventos.datos.EquipoInactividad;
import co.uniproject.eventos.datos.HitoEnviado;
import co.uniproject.eventos.datos.HitoEvaluado;
import co.uniproject.eventos.datos.RecuperacionSolicitada;
import co.uniproject.eventos.datos.TareaCorreccion;
import co.uniproject.eventos.datos.TareaReasignada;
import co.uniproject.eventos.datos.TareaRutaCriticaRiesgo;

/**
 * Texto plano de la bandeja (P10) y la entidad a la que apunta cada aviso. El mensaje de Telegram lo arma el agente
 * con sus plantillas; este no lleva HTML y el SPA lo pinta escapado.
 */
public final class MensajesBandeja {

    static final int MAXIMO = 500;

    /** entidad: "tarea", "hito_equipo" o "equipo" (CHECK de notificacion.entidad_referencia). */
    public record ReferenciaBandeja(String entidad, long id) {}

    private MensajesBandeja() {
    }

    /** Decisión 28-13: los eventos de cuenta solo van por Telegram. */
    public static boolean vaALaBandeja(DatosEvento datos) {
        return !(datos instanceof RecuperacionSolicitada || datos instanceof ContrasenaCambiada);
    }

    public static ReferenciaBandeja referencia(DatosEvento datos) {
        return switch (datos) {
            case TareaRutaCriticaRiesgo r -> new ReferenciaBandeja("tarea", r.tarea().id());
            case EquipoInactividad i -> new ReferenciaBandeja("equipo", i.equipo().id());
            case HitoEnviado h -> new ReferenciaBandeja("hito_equipo", h.hito().id());
            case HitoEvaluado h -> new ReferenciaBandeja("hito_equipo", h.hito().id());
            case TareaReasignada t -> new ReferenciaBandeja("tarea", t.tarea().id());
            case TareaCorreccion t -> new ReferenciaBandeja("tarea", t.tarea().id());
            case RecuperacionSolicitada _, ContrasenaCambiada _ ->
                    throw new IllegalArgumentException("Los eventos de cuenta no van a la bandeja");
        };
    }

    public static String mensaje(DatosEvento datos, DestinatariosEvento.Papel papel) {
        String texto = switch (datos) {
            case TareaRutaCriticaRiesgo r -> "La tarea \"" + r.tarea().titulo()
                    + "\" de la ruta crítica pone en riesgo el hito \"" + r.hito().nombre() + "\" (avance "
                    + r.avance() + " %).";
            case EquipoInactividad i -> i.estudiante().nombre() + " lleva " + i.periodoDias()
                    + " días sin registrar actividad en " + i.equipo().nombre() + ".";
            case HitoEnviado h -> h.equipo().nombre() + " envió el hito \"" + h.hito().nombre()
                    + "\" para evaluación.";
            case HitoEvaluado h -> "El hito \"" + h.hito().nombre() + "\" de " + h.equipo().nombre() + " tiene nota "
                    + (h.caracterNota() == CaracterNota.DEFINITIVA ? "definitiva." : "provisional.");
            case TareaReasignada t -> papel == DestinatariosEvento.Papel.ANTERIOR
                    ? "La tarea \"" + t.tarea().titulo() + "\" se reasignó a " + t.integranteNuevo().nombre() + "."
                    : "Se te asignó la tarea \"" + t.tarea().titulo() + "\" en " + t.equipo().nombre() + ".";
            case TareaCorreccion t -> t.lider().nombre() + " pidió corregir la tarea \"" + t.tarea().titulo()
                    + "\": " + t.resumen();
            case RecuperacionSolicitada _, ContrasenaCambiada _ ->
                    throw new IllegalArgumentException("Los eventos de cuenta no van a la bandeja");
        };
        return recortar(texto);
    }

    static String recortar(String texto) {
        return texto.length() <= MAXIMO ? texto : texto.substring(0, MAXIMO - 3) + "...";
    }
}
