package co.uniproject.nucleo.trazabilidad.servicio;

import java.time.Instant;

/** Una fila de log_auditoria del equipo. autor nunca es nulo ("Sistema"); detalle tampoco (decisión 21-5). */
public record EntradaHistorial(long idLog, Instant fechaHora, Long idUsuario, String autor, String accion,
                               String entidad, Long idRegistro, String campo, String valorAnterior, String valorNuevo,
                               String detalle) {}
