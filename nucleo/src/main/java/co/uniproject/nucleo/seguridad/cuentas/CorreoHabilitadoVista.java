package co.uniproject.nucleo.seguridad.cuentas;

import co.uniproject.nucleo.seguridad.dominio.CorreoHabilitado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import java.time.Instant;

public record CorreoHabilitadoVista(long idCorreoHabilitado, String email, RolPlataforma rol, Long idProyecto,
                                    Instant fechaHabilitacion, boolean registrado) {

    public static CorreoHabilitadoVista de(CorreoHabilitado c) {
        return new CorreoHabilitadoVista(c.getIdCorreoHabilitado(), c.getEmail(), c.getRolPlataforma(),
                c.getIdProyecto(), c.getFechaHabilitacion(), c.getFechaUso() != null);
    }
}
