package co.uniproject.nucleo.notificaciones;

import co.uniproject.eventos.datos.DatosEvento;
import java.util.UUID;

/**
 * Puerta de entrada a la outbox (plan §7.2 y §8.1). Los servicios de negocio la llaman DENTRO de su transacción:
 * el evento queda guardado en evento_saliente si y solo si el cambio de negocio se confirma.
 */
public interface RegistroEventos {

    /**
     * Crea el CloudEvent (EventoCloud.nuevo), lo valida contra su esquema y lo inserta en evento_saliente.
     *
     * @return id del evento (también es el id de la fila y el messageId en RabbitMQ)
     * @throws IllegalStateException si no hay una transacción activa
     * @throws co.uniproject.eventos.EventoInvalidoException si los datos no cumplen el esquema
     */
    UUID registrar(DatosEvento datos);
}
