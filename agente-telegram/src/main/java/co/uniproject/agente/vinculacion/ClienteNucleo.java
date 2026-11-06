package co.uniproject.agente.vinculacion;

import java.util.UUID;

/** Cliente del endpoint interno del núcleo (ContratoInterno). Nunca lanza excepción. */
public interface ClienteNucleo {
    /** 204 → VINCULADO, 404 → CODIGO_INEXISTENTE, 410 → CODIGO_VENCIDO_O_USADO, cualquier otra cosa → ERROR. */
    ResultadoVinculacion vincular(String codigo, long chatId);

    /** Avisa que el chat bloqueó el bot (403). Si falla, solo registra una advertencia. */
    void informarBloqueo(long chatId);

    /** 200 con código válido → EMITIDO; 404 o 410 → DESCARTADA; cualquier otra cosa → ERROR. Nunca registra el código. */
    CodigoRecuperacion pedirCodigoRecuperacion(UUID solicitud, long chatId);
}
