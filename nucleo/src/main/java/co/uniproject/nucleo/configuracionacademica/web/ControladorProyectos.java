package co.uniproject.nucleo.configuracionacademica.web;

import co.uniproject.nucleo.configuracionacademica.servicio.EquipoVista;
import co.uniproject.nucleo.configuracionacademica.servicio.EstudianteProyectoVista;
import co.uniproject.nucleo.configuracionacademica.servicio.HitoVista;
import co.uniproject.nucleo.configuracionacademica.servicio.MonitorVista;
import co.uniproject.nucleo.configuracionacademica.servicio.ProyectoDetalle;
import co.uniproject.nucleo.configuracionacademica.servicio.ProyectoVista;
import co.uniproject.nucleo.configuracionacademica.servicio.ResultadoHabilitacion;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioCorreosProyecto;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioEquipos;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioHitos;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioProyectos;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudCorreos;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudEquipo;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudHito;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudProyecto;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.cuentas.CorreoHabilitadoVista;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RF1, RF14 y K2. Los permisos los revisan los servicios (403 auditado). */
@RestController
@RequestMapping("/api/proyectos")
public class ControladorProyectos {

    private final ServicioProyectos proyectos;
    private final ServicioHitos hitos;
    private final ServicioEquipos equipos;
    private final ServicioCorreosProyecto correos;

    public ControladorProyectos(
            ServicioProyectos proyectos,
            ServicioHitos hitos,
            ServicioEquipos equipos,
            ServicioCorreosProyecto correos) {
        this.proyectos = proyectos;
        this.hitos = hitos;
        this.equipos = equipos;
        this.correos = correos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProyectoVista crear(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody SolicitudProyecto solicitud) {
        return proyectos.crear(usuario, solicitud);
    }

    @GetMapping
    public List<ProyectoVista> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return proyectos.listar(usuario);
    }

    @GetMapping("/{idProyecto}")
    public ProyectoDetalle detalle(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable("idProyecto") long idProyecto) {
        return proyectos.detalle(usuario, idProyecto);
    }

    @PutMapping("/{idProyecto}")
    public ProyectoVista actualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idProyecto") long idProyecto,
            @Valid @RequestBody SolicitudProyecto solicitud) {
        return proyectos.actualizar(usuario, idProyecto, solicitud);
    }

    @PostMapping("/{idProyecto}/hitos")
    @ResponseStatus(HttpStatus.CREATED)
    public HitoVista crearHito(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idProyecto") long idProyecto,
            @Valid @RequestBody SolicitudHito solicitud) {
        return hitos.crear(usuario, idProyecto, solicitud);
    }

    @PostMapping("/{idProyecto}/equipos")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoVista crearEquipo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idProyecto") long idProyecto,
            @Valid @RequestBody SolicitudEquipo solicitud) {
        return equipos.crear(usuario, idProyecto, solicitud);
    }

    @PostMapping("/{idProyecto}/correos")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultadoHabilitacion habilitarCorreos(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idProyecto") long idProyecto,
            @Valid @RequestBody SolicitudCorreos solicitud) {
        return correos.habilitar(usuario, idProyecto, solicitud);
    }

    @GetMapping("/{idProyecto}/correos")
    public List<CorreoHabilitadoVista> listarCorreos(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable("idProyecto") long idProyecto) {
        return correos.listar(usuario, idProyecto);
    }

    @DeleteMapping("/{idProyecto}/correos/{idCorreoHabilitado}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirarCorreo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idProyecto") long idProyecto,
            @PathVariable("idCorreoHabilitado") long idCorreoHabilitado) {
        correos.retirarCorreo(usuario, idProyecto, idCorreoHabilitado);
    }

    /** D-11 y D-12: estudiantes habilitados del proyecto con su equipo o invitación. Solo el docente dueño. */
    @GetMapping("/{idProyecto}/estudiantes")
    public List<EstudianteProyectoVista> listarEstudiantes(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable("idProyecto") long idProyecto) {
        return correos.estudiantes(usuario, idProyecto);
    }

    @GetMapping("/{idProyecto}/monitores")
    public List<MonitorVista> listarMonitores(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable("idProyecto") long idProyecto) {
        return correos.monitores(usuario, idProyecto);
    }

    @DeleteMapping("/{idProyecto}/monitores/{idMonitor}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarMonitor(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idProyecto") long idProyecto,
            @PathVariable("idMonitor") long idMonitor) {
        correos.quitarMonitor(usuario, idProyecto, idMonitor);
    }
}
