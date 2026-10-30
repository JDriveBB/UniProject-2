package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.comun.FechasDominio;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.ejecucion.dominio.EstadoHitoEquipo;
import java.time.Instant;

/** Decisión E1 en un solo lugar: cuándo se pueden cambiar las tareas de un hito de equipo. */
public final class EdicionHito {

    private EdicionHito() {
    }

    /** EN_CURSO: sí. ENVIADO: no (congelado para revisión). EVALUADO: solo antes de que venza (nota provisional). */
    public static boolean abierta(ContextoHito hito, Instant ahora) {
        return switch (hito.estado()) {
            case EN_CURSO -> true;
            case ENVIADO -> false;
            case EVALUADO -> ahora.isBefore(FechasDominio.venceEn(hito.fechaLimite()));
        };
    }

    /** 409 HITO_NO_EDITABLE si no está abierta. */
    public static void exigirAbierta(ContextoHito hito, Instant ahora) {
        if (!abierta(hito, ahora)) {
            throw new ReglaNegocioException("HITO_NO_EDITABLE", hito.estado() == EstadoHitoEquipo.ENVIADO
                    ? "El hito está enviado a revisión: sus tareas no se pueden cambiar"
                    : "El hito ya cerró: sus tareas no se pueden cambiar");
        }
    }
}
