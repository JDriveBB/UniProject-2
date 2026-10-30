package co.uniproject.nucleo.notificaciones.programador;

import java.time.Instant;
import java.util.List;

public interface DetectorInactividad {
    /** Estudiantes que, a la fecha 'ahora', superan el periodo sin actividad que configuró el docente (RF6). */
    List<CasoInactividad> buscarInactivos(Instant ahora);
}
