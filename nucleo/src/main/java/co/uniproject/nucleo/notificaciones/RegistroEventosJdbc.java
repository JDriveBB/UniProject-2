package co.uniproject.nucleo.notificaciones;

import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.EventoCloud;
import co.uniproject.eventos.datos.DatosEvento;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RegistroEventosJdbc implements RegistroEventos {

    private final JdbcClient jdbc;
    private final CodecEventos codec;
    private final Clock reloj;

    public RegistroEventosJdbc(JdbcClient jdbc, CodecEventos codec, Clock reloj) {
        this.jdbc = jdbc;
        this.codec = codec;
        this.reloj = reloj;
    }

    @Override
    public UUID registrar(DatosEvento datos) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("RegistroEventos.registrar exige una transacción activa: el evento se guarda en la misma transacción que el cambio de negocio");
        }
        EventoCloud evento = EventoCloud.nuevo(datos, reloj);
        String json = codec.serializar(evento);
        jdbc.sql("INSERT INTO evento_saliente (id, tipo, carga) VALUES (:id, :tipo, CAST(:carga AS jsonb))")
                .param("id", evento.id())
                .param("tipo", evento.type())
                .param("carga", json)
                .update();
        return evento.id();
    }
}
