package co.uniproject.nucleo.notificaciones.bandeja;

import co.uniproject.eventos.DestinatariosEvento;
import co.uniproject.eventos.EventoCloud;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Rehace el texto de los avisos de la bandeja que salieron de un evento (bloque 27, Aclaraciones 1 y 2). Cada aviso se
 * reconoce por destinatario, tipo, referencia, fecha (RegistroNotificacionesJdbc copia el time del evento, N1) y el
 * texto que ese evento generó: time va en segundos, así que dos eventos del mismo segundo comparten la fecha.
 * El texto nuevo sale de MensajesBandeja con los datos ya corregidos: nada se reemplaza por coincidencia de texto.
 */
@Component
public class NombresEnNotificaciones {

    /** fecha_creacion guarda microsegundos y el time del evento nanosegundos. */
    private static final long TOLERANCIA_NANOS = 1_000_000L;

    private final JdbcClient jdbc;

    public NombresEnNotificaciones(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * anterior: el evento tal como estaba; nuevo: ya reescrito (mismo id, mismos destinatarios). Devuelve cuántas
     * notificaciones reescribió. Debe correr dentro de la transacción del servicio que lo llama.
     */
    public int regenerar(EventoCloud anterior, EventoCloud nuevo) {
        MensajesBandeja.ReferenciaBandeja referencia = MensajesBandeja.referencia(nuevo.data());
        OffsetDateTime fecha = OffsetDateTime.ofInstant(nuevo.time(), ZoneOffset.UTC);
        int filas = 0;
        for (DestinatariosEvento.Destino d : DestinatariosEvento.de(nuevo.data())) {
            String textoAnterior = MensajesBandeja.mensaje(anterior.data(), d.papel());
            String textoNuevo = MensajesBandeja.mensaje(nuevo.data(), d.papel());
            if (textoAnterior.equals(textoNuevo)) {
                continue;
            }
            filas += jdbc.sql("UPDATE notificacion SET mensaje = :mensaje WHERE id_destinatario = :destinatario "
                            + "AND tipo = :tipo AND entidad_referencia = :entidad AND id_referencia = :referencia "
                            + "AND fecha_creacion BETWEEN :desde AND :hasta AND mensaje = :anterior")
                    .param("mensaje", textoNuevo)
                    .param("destinatario", d.persona().id())
                    .param("tipo", nuevo.type())
                    .param("entidad", referencia.entidad())
                    .param("referencia", referencia.id())
                    .param("desde", fecha.minusNanos(TOLERANCIA_NANOS))
                    .param("hasta", fecha.plusNanos(TOLERANCIA_NANOS))
                    .param("anterior", textoAnterior)
                    .update();
        }
        return filas;
    }
}
