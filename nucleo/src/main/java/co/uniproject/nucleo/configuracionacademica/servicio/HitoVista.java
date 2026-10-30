package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.configuracionacademica.dominio.CriterioRubrica;
import co.uniproject.nucleo.configuracionacademica.dominio.Hito;
import java.time.LocalDate;
import java.util.List;

public record HitoVista(long idHito, long idProyecto, String nombreHito, String descripcion, int orden,
                        LocalDate fechaInicio, LocalDate fechaLimite, int diasInactividadAlerta,
                        boolean notificarDocenteInactividad, List<CriterioVista> criterios) {

    public static HitoVista de(Hito h, List<CriterioRubrica> criterios) {
        return new HitoVista(h.getIdHito(), h.getIdProyecto(), h.getNombreHito(), h.getDescripcion(), h.getOrden(),
                h.getFechaInicio(), h.getFechaLimite(), h.getDiasInactividadAlerta(),
                h.getNotificarDocenteInactividad(), criterios.stream().map(CriterioVista::de).toList());
    }
}
