package co.uniproject.agente.telegram;

import co.uniproject.agente.plantillas.Aviso;

public interface EnviadorTelegram {
    /** Envía el aviso a un chat privado. Nunca lanza excepción: todo error se traduce a ResultadoEnvio. */
    ResultadoEnvio enviar(long chatId, Aviso aviso);
}
