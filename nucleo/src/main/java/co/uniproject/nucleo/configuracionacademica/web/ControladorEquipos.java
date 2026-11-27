package co.uniproject.nucleo.configuracionacademica.web;

import co.uniproject.nucleo.configuracionacademica.servicio.EquipoVista;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioEquipos;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioInscripcion;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudCupo;
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
    private final ServicioInscripcion inscripcion;

    public ControladorEquipos(ServicioEquipos equipos, ServicioInscripcion inscripcion) {
        this.equipos = equipos;
        this.inscripcion = inscripcion;
    }

    @PostMapping("/miembros")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoVista agregarMiembro(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idEquipo") long idEquipo,
            @Valid @RequestBody SolicitudMiembro solicitud) {
        return equipos.agregarMiembro(usuario, idEquipo, solicitud);
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

    @DeleteMapping("/lider")
    public EquipoVista destituirLider(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable("idEquipo") long idEquipo) {
        return equipos.destituir(usuario, idEquipo);
    }

    @PutMapping("/cupo")
    public EquipoVista cambiarCupo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idEquipo") long idEquipo,
            @Valid @RequestBody SolicitudCupo solicitud) {
        return equipos.cambiarCupo(usuario, idEquipo, solicitud);
    }

    @PostMapping("/inscripcion")
    public EquipoVista unirse(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable("idEquipo") long idEquipo) {
        return inscripcion.unirse(usuario, idEquipo);
    }

    @DeleteMapping("/inscripcion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salirse(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable("idEquipo") long idEquipo) {
        inscripcion.salirse(usuario, idEquipo);
    }
}
