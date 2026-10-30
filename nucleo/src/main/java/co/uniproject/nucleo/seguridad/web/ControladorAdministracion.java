package co.uniproject.nucleo.seguridad.web;

import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.cuentas.CorreoHabilitadoVista;
import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Solo ADMINISTRADOR (lo exige la cadena web en /api/admin/**). */
@RestController
@RequestMapping("/api/admin/docentes")
public class ControladorAdministracion {

    private final ServicioCuentas cuentas;

    public ControladorAdministracion(ServicioCuentas cuentas) {
        this.cuentas = cuentas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CorreoHabilitadoVista habilitar(@AuthenticationPrincipal UsuarioAutenticado admin,
                                           @Valid @RequestBody SolicitudHabilitarCorreo solicitud) {
        return cuentas.habilitarDocente(admin.idUsuario(), solicitud.email());
    }

    @GetMapping
    public List<CorreoHabilitadoVista> listar() {
        return cuentas.docentesHabilitados();
    }
}
