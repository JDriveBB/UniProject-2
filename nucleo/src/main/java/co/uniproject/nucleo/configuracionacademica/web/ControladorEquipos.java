package co.uniproject.nucleo.configuracionacademica.web;

import co.uniproject.nucleo.configuracionacademica.servicio.EquipoVista;
import co.uniproject.nucleo.configuracionacademica.servicio.EstudianteProyectoVista;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioEquipos;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudInvitacion;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudLider;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudMiembro;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipos/{idEquipo}")
public class ControladorEquipos {

    private final ServicioEquipos equipos;

    public ControladorEquipos(ServicioEquipos equipos) {
        this.equipos = equipos;
    }

    @PostMapping("/miembros")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoVista agregarMiembro(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idEquipo") long idEquipo,
            @Valid @RequestBody SolicitudMiembro solicitud) {
        return equipos.agregarMiembro(usuario, idEquipo, solicitud);
    }

    /** D-12 20A: estudiante habilitado sin cuenta que entrará al equipo al registrarse. */
    @PostMapping("/invitaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public EstudianteProyectoVista invitar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idEquipo") long idEquipo,
            @Valid @RequestBody SolicitudInvitacion solicitud) {
        return equipos.invitar(usuario, idEquipo, solicitud);
    }

    @DeleteMapping("/miembros/{idUsuario}")
    public EquipoVista quitarMiembro(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idEquipo") long idEquipo,
            @PathVariable("idUsuario") long idUsuario) {
        return equipos.quitarMiembro(usuario, idEquipo, idUsuario);
    }

    @PutMapping("/lider")
    public EquipoVista cambiarLider(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idEquipo") long idEquipo,
            @Valid @RequestBody SolicitudLider solicitud) {
        return equipos.cambiarLider(usuario, idEquipo, solicitud);
    }
}
