package co.uniproject.eventos;

import co.uniproject.eventos.datos.ContrasenaCambiada;
import co.uniproject.eventos.datos.DatosEvento;
import co.uniproject.eventos.datos.EquipoInactividad;
import co.uniproject.eventos.datos.HitoEnviado;
import co.uniproject.eventos.datos.HitoEvaluado;
import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.RecuperacionSolicitada;
import co.uniproject.eventos.datos.TareaCorreccion;
import co.uniproject.eventos.datos.TareaReasignada;
import co.uniproject.eventos.datos.TareaRutaCriticaRiesgo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Quién recibe cada evento (plan sección 5 y sección 8.6, RF6). Una sola regla para la bandeja del núcleo (decisión N1) y para el
 * agente de Telegram: incluye a quien no vinculó Telegram (chatId null) y no repite personas (gana la primera aparición).
 */
public final class DestinatariosEvento {

    /** UNICO: aviso normal. NUEVO y ANTERIOR: las dos caras de una reasignación. */
    public enum Papel { UNICO, NUEVO, ANTERIOR }

    public record Destino(Persona persona, Papel papel) {}

    private DestinatariosEvento() {
    }

    public static List<Destino> de(DatosEvento datos) {
        List<Destino> candidatos = new ArrayList<>();
        switch (datos) {
            case TareaRutaCriticaRiesgo r -> {
                r.integrantes().forEach(p -> candidatos.add(new Destino(p, Papel.UNICO)));
                candidatos.add(new Destino(r.docente(), Papel.UNICO));
            }
            case EquipoInactividad i -> {
                candidatos.add(new Destino(i.lider(), Papel.UNICO));
                if (i.notificarDocente()) {
                    candidatos.add(new Destino(i.docente(), Papel.UNICO));
                }
            }
            case HitoEnviado h -> candidatos.add(new Destino(h.docente(), Papel.UNICO));
            case HitoEvaluado h -> h.integrantes().forEach(p -> candidatos.add(new Destino(p, Papel.UNICO)));
            case TareaReasignada t -> {
                candidatos.add(new Destino(t.integranteNuevo(), Papel.NUEVO));
                candidatos.add(new Destino(t.integranteAnterior(), Papel.ANTERIOR));
            }
            case TareaCorreccion t -> candidatos.add(new Destino(t.responsable(), Papel.UNICO));
            case RecuperacionSolicitada r -> candidatos.add(new Destino(r.titular(), Papel.UNICO));
            case ContrasenaCambiada c -> candidatos.add(new Destino(c.titular(), Papel.UNICO));
        }
        Set<Long> vistos = new HashSet<>();
        List<Destino> resultado = new ArrayList<>();
        for (Destino d : candidatos) {
            if (vistos.add(d.persona().id())) {
                resultado.add(d);
            }
        }
        return List.copyOf(resultado);
    }
}
