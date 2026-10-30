package co.uniproject.agente.consumo;

import co.uniproject.agente.destinatarios.Destinatario;
import co.uniproject.agente.destinatarios.ReglasDestinatarios;
import co.uniproject.agente.idempotencia.RegistroProcesados;
import co.uniproject.agente.plantillas.Aviso;
import co.uniproject.agente.plantillas.PlantillaException;
import co.uniproject.agente.plantillas.RenderizadorAvisos;
import co.uniproject.agente.telegram.EnviadorTelegram;
import co.uniproject.agente.vinculacion.ClienteNucleo;
import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.EventoCloud;
import co.uniproject.eventos.EventoInvalidoException;
import co.uniproject.eventos.TipoEvento;
import co.uniproject.eventos.Topologia;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class ConsumidorNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorNotificaciones.class);

    private final CodecEventos codec;
    private final ReglasDestinatarios reglas;
    private final RenderizadorAvisos renderizador;
    private final EnviadorTelegram enviador;
    private final RegistroProcesados registro;
    private final ClienteNucleo clienteNucleo;

    public ConsumidorNotificaciones(CodecEventos codec, ReglasDestinatarios reglas, RenderizadorAvisos renderizador,
                                    EnviadorTelegram enviador, RegistroProcesados registro, ClienteNucleo clienteNucleo) {
        this.codec = codec;
        this.reglas = reglas;
        this.renderizador = renderizador;
        this.enviador = enviador;
        this.registro = registro;
        this.clienteNucleo = clienteNucleo;
    }

    @RabbitListener(queues = Topologia.COLA_TELEGRAM)
    public void recibir(Message mensaje, Channel canal, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        EventoCloud evento;
        try {
            evento = codec.leer(mensaje.getBody());
        } catch (EventoInvalidoException e) {
            log.warn("Evento inválido, va a fallidos: {}", e.getMessage());
            rechazar(canal, tag, false);
            return;
        }

        try {
            TipoEvento tipo = TipoEvento.desdeClave(evento.type()).orElseThrow();
            List<Destinatario> destinatarios = reglas.destinatarios(evento);
            if (destinatarios.isEmpty()) {
                log.info("Evento {} ({}) sin destinatarios con Telegram", evento.id(), evento.type());
                ack(canal, tag);
                return;
            }
            int enviados = 0;
            for (Destinatario d : destinatarios) {
                if (registro.yaProcesado(evento.id(), d.usuarioId())) {
                    continue;
                }
                Aviso aviso = renderizador.renderizar(evento, d.variante());
                switch (enviador.enviar(d.chatId(), aviso)) {
                    case ENVIADO -> {
                        registro.registrar(evento.id(), d.usuarioId());
                        enviados++;
                    }
                    case BLOQUEADO -> {
                        clienteNucleo.informarBloqueo(d.chatId());
                        registro.registrar(evento.id(), d.usuarioId());
                    }
                    case CHAT_INVALIDO -> {
                        registro.registrar(evento.id(), d.usuarioId());
                    }
                    case TEMPORAL -> {
                        log.warn("Error temporal con el evento {} ({}); se reintenta más tarde", evento.id(), evento.type());
                        rechazar(canal, tag, true);
                        return;
                    }
                    case MENSAJE_INVALIDO -> {
                        log.error("Mensaje inválido para Telegram en el evento {} ({}); va a fallidos", evento.id(), evento.type());
                        rechazar(canal, tag, false);
                        return;
                    }
                }
            }
            log.info("Evento {} ({}) procesado: {} avisos enviados", evento.id(), evento.type(), enviados);
            ack(canal, tag);
        } catch (PlantillaException e) {
            log.error("Plantilla inválida en el evento {} ({}); va a fallidos", evento.id(), evento.type(), e);
            rechazar(canal, tag, false);
        } catch (Exception e) {
            log.error("Fallo inesperado con el evento {}; se reintenta más tarde", evento.id(), e);
            rechazar(canal, tag, true);
        }
    }

    private void ack(Channel canal, long tag) {
        try {
            canal.basicAck(tag, false);
        } catch (IOException | RuntimeException e) {
            log.error("Error al confirmar mensaje con tag {}", tag, e);
        }
    }

    private void rechazar(Channel canal, long tag, boolean requeue) {
        try {
            canal.basicReject(tag, requeue);
        } catch (IOException | RuntimeException e) {
            log.error("Error al rechazar mensaje con tag {} (requeue={})", tag, requeue, e);
        }
    }
}
