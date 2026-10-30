package co.uniproject.nucleo.notificaciones.bandeja;

import co.uniproject.nucleo.notificaciones.dominio.EstadoEnvioNotificacion;
import java.time.Instant;

/** enlace: ruta del SPA (RutasWeb), la misma del aviso de Telegram. mensaje es texto plano: escapar al pintar. */
public record NotificacionVista(long idNotificacion, String tipo, String mensaje, String entidadReferencia,
                                long idReferencia, String enlace, Instant fechaCreacion,
                                EstadoEnvioNotificacion estadoEnvio, boolean leida) {}
