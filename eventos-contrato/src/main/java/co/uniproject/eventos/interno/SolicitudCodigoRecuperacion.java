package co.uniproject.eventos.interno;

import java.util.UUID;

/** Cuerpo de POST ContratoInterno.RUTA_RECUPERACIONES: la solicitud del evento y el chat al que se va a enviar. */
public record SolicitudCodigoRecuperacion(UUID solicitud, long chatId) {}
