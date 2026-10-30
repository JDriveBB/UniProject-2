package co.uniproject.agente.destinatarios;

/** Destinatario con Telegram vinculado (chatId siempre presente y positivo). */
public record Destinatario(long usuarioId, long chatId, Variante variante) {}
