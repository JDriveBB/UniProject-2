package co.uniproject.nucleo.notificaciones.bandeja;

import java.util.List;

/** noLeidas cuenta todas las no leídas, aunque la lista traiga solo las 50 más recientes. */
public record Bandeja(long noLeidas, List<NotificacionVista> notificaciones) {}
