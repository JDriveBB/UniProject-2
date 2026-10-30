package co.uniproject.nucleo.notificaciones.bandeja;

import co.uniproject.eventos.DestinatariosEvento;
import co.uniproject.eventos.EventoCloud;
import co.uniproject.eventos.datos.Persona;
import co.uniproject.nucleo.notificaciones.dominio.EstadoEnvioNotificacion;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** EN_COLA si el destinatario tenía Telegram (el aviso sale por la outbox); SIN_TELEGRAM si solo queda en la bandeja. */
@Component
public class RegistroNotificacionesJdbc implements RegistroNotificaciones {

    private final JdbcClient jdbc;

    public RegistroNotificacionesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int registrar(EventoCloud evento) {
        MensajesBandeja.ReferenciaBandeja referencia = MensajesBandeja.referencia(evento.data());
        OffsetDateTime fecha = OffsetDateTime.ofInstant(evento.time(), ZoneOffset.UTC);
        int filas = 0;
        for (DestinatariosEvento.Destino d : DestinatariosEvento.de(evento.data())) {
            Persona p = d.persona();
            EstadoEnvioNotificacion estado = p.chatId() != null
                    ? EstadoEnvioNotificacion.EN_COLA
                    : EstadoEnvioNotificacion.SIN_TELEGRAM;
            filas += jdbc.sql("INSERT INTO notificacion (id_destinatario, tipo, mensaje, entidad_referencia, "
                            + "id_referencia, fecha_creacion, estado_envio) VALUES (:destinatario, :tipo, :mensaje, "
                            + ":entidad, :referencia, :fecha, :estado)")
                    .param("destinatario", p.id())
                    .param("tipo", evento.type())
                    .param("mensaje", MensajesBandeja.mensaje(evento.data(), d.papel()))
                    .param("entidad", referencia.entidad())
                    .param("referencia", referencia.id())
                    .param("fecha", fecha)
                    .param("estado", estado.name())
                    .update();
        }
        return filas;
    }
}
