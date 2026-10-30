package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.configuracionacademica.dominio.MonitorProyecto;
import co.uniproject.nucleo.configuracionacademica.dominio.MonitorProyectoId;
import co.uniproject.nucleo.configuracionacademica.dominio.Proyecto;
import co.uniproject.nucleo.configuracionacademica.repositorio.MonitorProyectoRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.ProyectoRepositorio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.cuentas.CorreoHabilitadoVista;
import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import co.uniproject.nucleo.seguridad.dominio.CorreoHabilitado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import co.uniproject.nucleo.seguridad.dominio.Usuario;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.seguridad.repositorio.CorreoHabilitadoRepositorio;
import co.uniproject.nucleo.seguridad.repositorio.UsuarioRepositorio;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** K2 y RF14: correos habilitados del proyecto (estudiantes y monitores) y monitores asignados. Solo el docente dueño. */
@Service
public class ServicioCorreosProyecto {

    private final ProyectoRepositorio proyectos;
    private final CorreoHabilitadoRepositorio correos;
    private final UsuarioRepositorio usuarios;
    private final MonitorProyectoRepositorio monitores;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;

    public ServicioCorreosProyecto(ProyectoRepositorio proyectos, CorreoHabilitadoRepositorio correos,
                                   UsuarioRepositorio usuarios, MonitorProyectoRepositorio monitores,
                                   Permisos permisos, RegistroAuditoria auditoria, Clock reloj) {
        this.proyectos = proyectos;
        this.correos = correos;
        this.usuarios = usuarios;
        this.monitores = monitores;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    /** Decisión 15-6. La auditoría guarda el rol habilitado, nunca el correo (Ley 1581). */
    @Transactional
    public ResultadoHabilitacion habilitar(UsuarioAutenticado usuario, long idProyecto, SolicitudCorreos solicitud) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        RolPlataforma rol = solicitud.rol();
        if (rol != RolPlataforma.ESTUDIANTE && rol != RolPlataforma.MONITOR) {
            throw new SolicitudInvalidaException("ROL_INVALIDO", "En un proyecto solo se habilitan estudiantes o monitores");
        }
        Proyecto proyecto = proyectos.findById(idProyecto)
                .orElseThrow(() -> new RecursoNoEncontradoException("El proyecto no existe"));
        if (rol == RolPlataforma.MONITOR && !proyecto.getUsaMonitores()) {
            throw new ReglaNegocioException("PROYECTO_SIN_MONITORES",
                    "Activa los monitores en el proyecto antes de habilitar sus correos");
        }
        Set<String> existentes = correos.findByIdProyectoOrderByEmail(idProyecto).stream()
                .map(CorreoHabilitado::getEmail)
                .collect(Collectors.toSet());
        Set<String> pedidos = new LinkedHashSet<>();
        for (String email : solicitud.emails()) {
            pedidos.add(ServicioCuentas.normalizarEmail(email));
        }
        Instant ahora = Instant.now(reloj);
        List<CorreoHabilitadoVista> habilitados = new ArrayList<>();
        List<String> yaHabilitados = new ArrayList<>();
        List<String> conOtroRol = new ArrayList<>();
        for (String email : pedidos) {
            if (existentes.contains(email)) {
                yaHabilitados.add(email);
                continue;
            }
            Optional<Usuario> cuenta = usuarios.findByEmail(email);
            if (cuenta.isPresent() && cuenta.get().getRolPlataforma() != rol) {
                conOtroRol.add(email);
                continue;
            }
            CorreoHabilitado c = new CorreoHabilitado();
            c.setIdProyecto(idProyecto);
            c.setEmail(email);
            c.setRolPlataforma(rol);
            c.setIdHabilitadoPor(usuario.idUsuario());
            c.setFechaHabilitacion(ahora);
            if (cuenta.isPresent()) {
                c.setIdUsuarioRegistrado(cuenta.get().getIdUsuario());
                c.setFechaUso(ahora);
                if (rol == RolPlataforma.MONITOR) {
                    MonitorProyecto m = new MonitorProyecto();
                    m.setIdProyecto(idProyecto);
                    m.setIdMonitor(cuenta.get().getIdUsuario());
                    monitores.save(m);
                }
            }
            try {
                c = correos.saveAndFlush(c);
            } catch (DataIntegrityViolationException e) {
                throw new ReglaNegocioException("CORREO_HABILITADO_EN_PARALELO",
                        "Otra operación habilitó alguno de estos correos al mismo tiempo. Vuelve a intentarlo");
            }
            auditoria.registrar(EntradaAuditoria.cambio(usuario.idUsuario(), "correo_habilitado",
                    c.getIdCorreoHabilitado(), AccionAuditoria.HABILITAR_CORREO, "rol_plataforma", null, rol.name()));
            habilitados.add(CorreoHabilitadoVista.de(c));
        }
        return new ResultadoHabilitacion(habilitados, yaHabilitados, conOtroRol);
    }

    @Transactional(readOnly = true)
    public List<CorreoHabilitadoVista> listar(UsuarioAutenticado usuario, long idProyecto) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        return correos.findByIdProyectoOrderByEmail(idProyecto).stream()
                .map(CorreoHabilitadoVista::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonitorVista> monitores(UsuarioAutenticado usuario, long idProyecto) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        List<Long> ids = monitores.findByIdProyecto(idProyecto).stream()
                .map(MonitorProyecto::getIdMonitor)
                .toList();
        return usuarios.findAllById(ids).stream()
                .map(u -> new MonitorVista(u.getIdUsuario(), u.getNombre(), u.getEmail()))
                .sorted(Comparator.comparing(MonitorVista::nombre).thenComparingLong(MonitorVista::idUsuario))
                .toList();
    }

    @Transactional
    public void quitarMonitor(UsuarioAutenticado usuario, long idProyecto, long idMonitor) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        MonitorProyectoId id = new MonitorProyectoId(idProyecto, idMonitor);
        if (!monitores.existsById(id)) {
            throw new RecursoNoEncontradoException("Ese monitor no está asignado al proyecto");
        }
        monitores.deleteById(id);
        auditoria.registrar(EntradaAuditoria.cambio(usuario.idUsuario(), "monitor_proyecto", idProyecto,
                AccionAuditoria.ELIMINAR, "id_monitor", String.valueOf(idMonitor), null));
    }
}
