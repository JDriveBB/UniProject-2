package co.uniproject.eventos.datos;

/** Persona que puede recibir el aviso. chatId es null si no vinculó Telegram (el agente la omite). */
public record Persona(long id, String nombre, Long chatId) {}
