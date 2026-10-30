package co.uniproject.agente.destinatarios;

import co.uniproject.eventos.DestinatariosEvento;
import co.uniproject.eventos.EventoCloud;
import java.util.List;
import org.springframework.stereotype.Component;

/** Aplica la regla común de eventos-contrato (DestinatariosEvento) y deja solo a quien vinculó Telegram. */
@Component
public class ReglasDestinatariosPorTipo implements ReglasDestinatarios {

    @Override
    public List<Destinatario> destinatarios(EventoCloud evento) {
        return DestinatariosEvento.de(evento.data()).stream()
                .filter(d -> d.persona().chatId() != null)
                .map(d -> new Destinatario(d.persona().id(), d.persona().chatId(), variante(d.papel())))
                .toList();
    }

    static Variante variante(DestinatariosEvento.Papel papel) {
        return switch (papel) {
            case UNICO -> Variante.UNICA;
            case NUEVO -> Variante.NUEVO;
            case ANTERIOR -> Variante.ANTERIOR;
        };
    }
}
