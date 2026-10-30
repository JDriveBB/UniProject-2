package co.uniproject.agente.telegram;

/** Resultado de un envío (plan §8.5; decisiones-tecnicas-java §4). */
public enum ResultadoEnvio {
    /** Telegram aceptó el mensaje. */
    ENVIADO,
    /** 429, 5xx, red, token ausente o inválido: reintentar el evento completo más tarde. */
    TEMPORAL,
    /** 403: el usuario bloqueó el bot. Permanente para ese destinatario; se avisa al núcleo. */
    BLOQUEADO,
    /** 400 por el chat ("chat not found", "PEER_ID_INVALID"). Permanente para ese destinatario. */
    CHAT_INVALIDO,
    /** Cualquier otro 400: HTML ("can't parse entities"), botón ("inline keyboard button URL ... is invalid") o no reconocido. Permanente para el evento: va a fallidos. */
    MENSAJE_INVALIDO
}
