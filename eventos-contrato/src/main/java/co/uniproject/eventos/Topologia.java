package co.uniproject.eventos;

/** Nombres de la topología de RabbitMQ (plan §8.3). Se declaran en infra/rabbitmq/definitions.json, nunca en código. */
public final class Topologia {
    private Topologia() {}
    public static final String EXCHANGE_EVENTOS = "uniproject.eventos";
    public static final String EXCHANGE_NO_ENRUTADOS = "uniproject.no_enrutados";
    public static final String EXCHANGE_FALLIDOS = "uniproject.fallidos";
    public static final String COLA_TELEGRAM = "notificaciones.telegram";
    public static final String COLA_TELEGRAM_FALLIDOS = "notificaciones.telegram.fallidos";
    public static final String COLA_NO_ENRUTADOS = "no_enrutados";
    /** content-type AMQP de cada mensaje (CloudEvents, modo estructurado). */
    public static final String CONTENT_TYPE = "application/cloudevents+json";
    /** Atributo source de los eventos del núcleo. */
    public static final String SOURCE_NUCLEO = "/uniproject/nucleo";
    /** Zona horaria del proyecto (programador y textos de los avisos). */
    public static final String ZONA = "America/Bogota";
}
