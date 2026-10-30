package co.uniproject.nucleo.notificaciones;

/** Resumen de una pasada del publicador. cortado = se detuvo por un fallo (RabbitMQ caído o nack). */
public record ResultadoLote(int leidos, int publicados, boolean cortado) {}
