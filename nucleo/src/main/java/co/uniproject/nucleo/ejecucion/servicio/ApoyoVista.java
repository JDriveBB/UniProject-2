package co.uniproject.nucleo.ejecucion.servicio;

import java.time.Instant;

/** idSolicitud: id de solicitud_apoyo. mensaje null: su autor suprimió la cuenta (bloque 29, Ley 1581). */
public record ApoyoVista(long idSolicitud, long idTarea, String titulo, long idAutor, String autor, String mensaje,
                         Instant fechaHora) {}
