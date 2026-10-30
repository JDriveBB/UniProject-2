package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import java.time.Instant;

public record ComentarioVista(long idComentario, long idTarea, long idAutor, String autor, RolPlataforma rolAutor,
                              String contenido, Instant fechaHora) {}
