package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.ejecucion.dominio.TipoEvidencia;
import java.time.Instant;

/** enlace: solo si es ENLACE. descarga: ruta de la API si es archivo. La clave interna no se expone. */
public record EvidenciaVista(long idEvidencia, long idTarea, long idUsuario, String autor, TipoEvidencia tipoEvidencia,
                             String enlace, String nombreArchivo, String descarga, Instant fechaSubida) {

    public static EvidenciaVista de(long idEvidencia, long idTarea, long idUsuario, String autor, TipoEvidencia tipo,
                                    String url, String nombreArchivo, Instant fechaSubida) {
        boolean esEnlace = tipo == TipoEvidencia.ENLACE;
        return new EvidenciaVista(idEvidencia, idTarea, idUsuario, autor, tipo, esEnlace ? url : null, nombreArchivo,
                esEnlace ? null : "/api/evidencias/" + idEvidencia + "/archivo", fechaSubida);
    }
}
