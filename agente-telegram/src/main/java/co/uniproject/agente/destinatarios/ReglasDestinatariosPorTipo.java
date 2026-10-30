package co.uniproject.agente.destinatarios;

import co.uniproject.eventos.EventoCloud;
import co.uniproject.eventos.datos.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ReglasDestinatariosPorTipo implements ReglasDestinatarios {

    private record Candidato(Persona persona, Variante variante) {}

    @Override
    public List<Destinatario> destinatarios(EventoCloud evento) {
        List<Candidato> candidatos = switch (evento.data()) {
            case TareaRutaCriticaRiesgo r -> {
                List<Candidato> list = new ArrayList<>();
                for (Persona p : r.integrantes()) {
                    list.add(new Candidato(p, Variante.UNICA));
                }
                list.add(new Candidato(r.docente(), Variante.UNICA));
                yield list;
            }
            case EquipoInactividad i -> {
                List<Candidato> list = new ArrayList<>();
                list.add(new Candidato(i.lider(), Variante.UNICA));
                if (i.notificarDocente()) {
                    list.add(new Candidato(i.docente(), Variante.UNICA));
                }
                yield list;
            }
            case HitoEnviado h -> List.of(new Candidato(h.docente(), Variante.UNICA));
            case HitoEvaluado h -> {
                List<Candidato> list = new ArrayList<>();
                for (Persona p : h.integrantes()) {
                    list.add(new Candidato(p, Variante.UNICA));
                }
                yield list;
            }
            case TareaReasignada t -> List.of(
                    new Candidato(t.integranteNuevo(), Variante.NUEVO),
                    new Candidato(t.integranteAnterior(), Variante.ANTERIOR)
            );
            case TareaCorreccion t -> List.of(new Candidato(t.responsable(), Variante.UNICA));
        };

        Set<Long> vistos = new HashSet<>();
        List<Destinatario> resultado = new ArrayList<>();
        for (Candidato c : candidatos) {
            Persona p = c.persona();
            if (p.chatId() != null && vistos.add(p.id())) {
                resultado.add(new Destinatario(p.id(), p.chatId(), c.variante()));
            }
        }
        return resultado;
    }
}
