package co.uniproject.nucleo.ejecucion.servicio;

import java.time.Instant;

public record ApoyoVista(long idLog, long idTarea, String titulo, Long idAutor, String autor, String mensaje,
                         Instant fechaHora) {}
