package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.ejecucion.dominio.EstadoHitoEquipo;
import co.uniproject.nucleo.ejecucion.dominio.HitoEquipo;

/** idHitoEquipo es el "hito" de los eventos y de las rutas /hitos/{id}/… (decisión R-7). */
public record HitoEquipoVista(long idHitoEquipo, long idHito, EstadoHitoEquipo estado) {

    public static HitoEquipoVista de(HitoEquipo he) {
        return new HitoEquipoVista(he.getIdHitoEquipo(), he.getIdHito(), he.getEstado());
    }
}
