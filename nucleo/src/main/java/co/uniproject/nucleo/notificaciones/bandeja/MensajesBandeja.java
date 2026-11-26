package co.uniproject.nucleo.notificaciones.bandeja;

import co.uniproject.eventos.DestinatariosEvento;
import co.uniproject.eventos.TextosEventos;
import co.uniproject.eventos.datos.CaracterNota;
import co.uniproject.eventos.datos.ContrasenaCambiada;
import co.uniproject.eventos.datos.CursoEstudianteAgregado;
import co.uniproject.eventos.datos.DatosEvento;
import co.uniproject.eventos.datos.EquipoInactividad;
import co.uniproject.eventos.datos.EquipoLiderElegido;
import co.uniproject.eventos.datos.EquipoMiembroCambio;
import co.uniproject.eventos.datos.EquipoSinLider;
import co.uniproject.eventos.datos.EquipoVotacionAbierta;
import co.uniproject.eventos.datos.HitoEnviado;
import co.uniproject.eventos.datos.HitoEvaluado;
import co.uniproject.eventos.datos.InscripcionPorCerrar;
import co.uniproject.eventos.datos.InscripcionResumen;
import co.uniproject.eventos.datos.MotivoSinLider;
import co.uniproject.eventos.datos.OrigenLider;
import co.uniproject.eventos.datos.Persona;
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

    /** entidad: "tarea", "hito_equipo", "equipo", "curso" o "proyecto" (CHECK de notificacion.entidad_referencia). */
    public record ReferenciaBandeja(String entidad, long id) {}

    private MensajesBandeja() {}

    /** Decisión 28-13: los eventos de cuenta solo van por Telegram. */
    public static boolean vaALaBandeja(DatosEvento datos) {
        return !(datos instanceof RecuperacionSolicitada || datos instanceof ContrasenaCambiada);
    }

    public static ReferenciaBandeja referencia(DatosEvento datos) {
        return switch (datos) {
            case TareaRutaCriticaRiesgo r ->
                new ReferenciaBandeja("tarea", r.tarea().id());
            case EquipoInactividad i ->
                new ReferenciaBandeja("equipo", i.equipo().id());
            case HitoEnviado h -> new ReferenciaBandeja("hito_equipo", h.hito().id());
            case HitoEvaluado h -> new ReferenciaBandeja("hito_equipo", h.hito().id());
            case TareaReasignada t -> new ReferenciaBandeja("tarea", t.tarea().id());
            case TareaCorreccion t -> new ReferenciaBandeja("tarea", t.tarea().id());
            case CursoEstudianteAgregado c ->
                new ReferenciaBandeja("curso", c.curso().id());
            case EquipoVotacionAbierta v ->
                new ReferenciaBandeja("equipo", v.equipo().id());
            case EquipoLiderElegido l ->
                new ReferenciaBandeja("equipo", l.equipo().id());
            case EquipoSinLider s -> new ReferenciaBandeja("equipo", s.equipo().id());
            case EquipoMiembroCambio m ->
                new ReferenciaBandeja("equipo", m.equipo().id());
            case InscripcionPorCerrar i ->
                new ReferenciaBandeja("proyecto", i.proyecto().id());
            case InscripcionResumen r ->
                new ReferenciaBandeja("proyecto", r.proyecto().id());
            case RecuperacionSolicitada _, ContrasenaCambiada _ ->
                throw new IllegalArgumentException("Los eventos de cuenta no van a la bandeja");
        };
    }

    public static String mensaje(DatosEvento datos, DestinatariosEvento.Papel papel) {
        String texto = switch (datos) {
            case TareaRutaCriticaRiesgo r ->
                "La tarea \"" + r.tarea().titulo()
                        + "\" de la ruta crítica pone en riesgo el hito \""
                        + r.hito().nombre() + "\" (avance "
                        + r.avance() + " %).";
            case EquipoInactividad i ->
                i.estudiante().nombre() + " lleva " + i.periodoDias() + " días sin registrar actividad en "
                        + i.equipo().nombre() + ".";
            case HitoEnviado h ->
                h.equipo().nombre() + " envió el hito \"" + h.hito().nombre() + "\" para evaluación.";
            case HitoEvaluado h ->
                "El hito \"" + h.hito().nombre() + "\" de " + h.equipo().nombre() + " tiene nota "
                        + (h.caracterNota() == CaracterNota.DEFINITIVA ? "definitiva." : "provisional.");
            case TareaReasignada t ->
                papel == DestinatariosEvento.Papel.ANTERIOR
                        ? "La tarea \"" + t.tarea().titulo() + "\" se reasignó a "
                                + t.integranteNuevo().nombre() + "."
                        : "Se te asignó la tarea \"" + t.tarea().titulo() + "\" en "
                                + t.equipo().nombre() + ".";
            case TareaCorreccion t ->
                t.lider().nombre() + " pidió corregir la tarea \"" + t.tarea().titulo() + "\": " + t.resumen();
            case CursoEstudianteAgregado c ->
                c.docente().nombre() + " te agregó al curso \"" + c.curso().nombre() + "\" (" + c.periodo() + ").";
            case EquipoVotacionAbierta v ->
                "Se abrió la votación de líder de " + v.equipo().nombre() + ". Ya puedes votar.";
            case EquipoLiderElegido l ->
                l.lider().nombre() + " es el nuevo líder de " + l.equipo().nombre() + ", "
                        + (l.origen() == OrigenLider.VOTACION
                                ? "elegido por votación."
                                : "elegido por el docente tras un empate.");
            case EquipoSinLider s ->
                "La votación de líder de " + s.equipo().nombre() + " terminó "
                        + (s.motivo() == MotivoSinLider.EMPATE
                                ? "en empate. Elige al líder entre los empatados."
                                : "sin votos. Nombra un líder o abre otra votación.");
            case EquipoMiembroCambio m ->
                papel == DestinatariosEvento.Papel.NUEVO
                        ? "El docente te agregó a " + m.equipo().nombre() + " en \""
                                + m.proyecto().nombre() + "\"."
                        : TextosEventos.listaNombres(m.personas().stream()
                                        .map(Persona::nombre)
                                        .toList()) + " "
                                + TextosEventos.verbo(
                                        m.movimiento(), m.personas().size()) + " "
                                + m.equipo().nombre() + ".";
            case InscripcionPorCerrar i ->
                "La inscripción a equipos de \"" + i.proyecto().nombre()
                        + "\" cierra en menos de 24 horas y aún no tienes equipo.";
            case InscripcionResumen r ->
                "La inscripción a equipos de \"" + r.proyecto().nombre()
                        + "\" cierra en menos de 24 horas: "
                        + TextosEventos.cantidad(r.sinEquipo(), "estudiante", "estudiantes") + " sin equipo y "
                        + TextosEventos.cantidad(r.cuposLibres(), "cupo libre", "cupos libres") + ".";
            case RecuperacionSolicitada _, ContrasenaCambiada _ ->
                throw new IllegalArgumentException("Los eventos de cuenta no van a la bandeja");
        };
        return recortar(texto);
    }

    static String recortar(String texto) {
        return texto.length() <= MAXIMO ? texto : texto.substring(0, MAXIMO - 3) + "...";
    }
}
