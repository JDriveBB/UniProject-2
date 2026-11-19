package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.ejecucion.dominio.EstadoHitoEquipo;
import co.uniproject.nucleo.ejecucion.dominio.HitoEquipo;
import java.time.Instant;

/**
 * idHitoEquipo es el "hito" de los eventos y de las rutas /hitos/{id}/… (decisión R-7). fechaEnvio: el último envío
 * de ese hito del equipo (null si no se ha enviado; D-12).
 */
public record HitoEquipoVista(long idHitoEquipo, long idHito, EstadoHitoEquipo estado, Instant fechaEnvio) {

    public static HitoEquipoVista de(HitoEquipo he, Instant fechaEnvio) {
        return new HitoEquipoVista(he.getIdHitoEquipo(), he.getIdHito(), he.getEstado(), fechaEnvio);
    }
}
