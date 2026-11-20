package co.uniproject.nucleo.seguridad.web;

import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.cuentas.CorreoHabilitadoVista;
import co.uniproject.nucleo.seguridad.cuentas.EnlaceRegistroVista;
import co.uniproject.nucleo.seguridad.cuentas.EnlacesRegistro;
import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final EnlacesRegistro enlaces;

    public ControladorAdministracion(ServicioCuentas cuentas, EnlacesRegistro enlaces) {
        this.cuentas = cuentas;
        this.enlaces = enlaces;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CorreoHabilitadoVista habilitar(
            @AuthenticationPrincipal UsuarioAutenticado admin, @Valid @RequestBody SolicitudHabilitarCorreo solicitud) {
        return cuentas.habilitarDocente(admin.idUsuario(), solicitud.email());
    }

    @GetMapping
    public List<CorreoHabilitadoVista> listar() {
        return cuentas.docentesHabilitados();
    }

    @PostMapping("/{idCorreoHabilitado}/enlace")
    public ResponseEntity<EnlaceRegistroVista> generarEnlace(
            @AuthenticationPrincipal UsuarioAutenticado admin,
            @PathVariable("idCorreoHabilitado") long idCorreoHabilitado) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(enlaces.generarParaDocente(admin.idUsuario(), idCorreoHabilitado));
    }
}
