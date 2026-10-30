package co.uniproject.agente.destinatarios;

import co.uniproject.eventos.EventoCloud;
import java.util.List;

/** Reglas de destinatarios de plan §8.6. */
public interface ReglasDestinatarios {
    /** Destinatarios con chatId no nulo, sin repetir usuarioId (se conserva el primero), en el orden de la regla. Lista vacía si nadie tiene Telegram. */
    List<Destinatario> destinatarios(EventoCloud evento);
}
