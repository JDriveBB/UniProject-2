package co.uniproject.agente.vinculacion;

/** Cliente del endpoint interno del núcleo (ContratoInterno). Nunca lanza excepción. */
public interface ClienteNucleo {
    /** 204 → VINCULADO, 404 → CODIGO_INEXISTENTE, 410 → CODIGO_VENCIDO_O_USADO, cualquier otra cosa → ERROR. */
    ResultadoVinculacion vincular(String codigo, long chatId);

    /** Avisa que el chat bloqueó el bot (403). Si falla, solo registra una advertencia. */
    void informarBloqueo(long chatId);
}
