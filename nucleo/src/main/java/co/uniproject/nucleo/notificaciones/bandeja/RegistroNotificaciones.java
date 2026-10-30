package co.uniproject.nucleo.notificaciones.bandeja;

import co.uniproject.eventos.EventoCloud;

/** N1: la bandeja (tabla notificacion) se llena en la misma transacción que la outbox. La llama RegistroEventosJdbc. */
@FunctionalInterface
public interface RegistroNotificaciones {

    /** Inserta una fila por destinatario del evento (DestinatariosEvento). Devuelve cuántas insertó. */
    int registrar(EventoCloud evento);
}
