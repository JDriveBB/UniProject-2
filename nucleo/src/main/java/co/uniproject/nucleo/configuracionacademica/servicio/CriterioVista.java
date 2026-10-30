package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.configuracionacademica.dominio.CriterioRubrica;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pesos y puntajes siempre con 2 decimales (numeric(5,2)), vengan de la petición o de la BD. */
public record CriterioVista(long idCriterio, long idHito, String descripcion, BigDecimal pesoPorcentaje,
                            BigDecimal puntajeMaximo) {

    public static CriterioVista de(CriterioRubrica c) {
        return new CriterioVista(c.getIdCriterio(), c.getIdHito(), c.getDescripcion(),
                c.getPesoPorcentaje().setScale(2, RoundingMode.HALF_UP),
                c.getPuntajeMaximo().setScale(2, RoundingMode.HALF_UP));
    }
}
