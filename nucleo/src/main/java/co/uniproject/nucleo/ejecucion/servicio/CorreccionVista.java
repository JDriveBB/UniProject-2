package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.ejecucion.dominio.EstadoSolicitudCorreccion;
import java.time.Instant;

public record CorreccionVista(long idSolicitud, long idTarea, long idEvidencia, long idLider, String lider,
                              long idEstudiante, String estudiante, String observaciones,
                              EstadoSolicitudCorreccion estado, Instant fechaSolicitud, Instant fechaAtencion) {}
