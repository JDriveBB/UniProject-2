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
import co.uniproject.nucleo.seguridad.cuentas.EnlaceRegistroVista;
import co.uniproject.nucleo.seguridad.cuentas.EnlacesRegistro;
import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import co.uniproject.nucleo.seguridad.cuentas.ServicioSupresion;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF14 (monitores habilitados por proyecto) y consulta de estudiantes participantes del proyecto (K2 por curso). Solo
 * el docente dueño.
 */
@Service
public class ServicioCorreosProyecto {

    private final ProyectoRepositorio proyectos;
    private final CorreoHabilitadoRepositorio correos;
    private final UsuarioRepositorio usuarios;
    private final MonitorProyectoRepositorio monitores;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;
    private final JdbcClient jdbc;
    private final EnlacesRegistro enlaces;

    public ServicioCorreosProyecto(
            ProyectoRepositorio proyectos,
            CorreoHabilitadoRepositorio correos,
            UsuarioRepositorio usuarios,
            MonitorProyectoRepositorio monitores,
            Permisos permisos,
            RegistroAuditoria auditoria,
            Clock reloj,
            JdbcClient jdbc,
            EnlacesRegistro enlaces) {
        this.proyectos = proyectos;
        this.correos = correos;
        this.usuarios = usuarios;
        this.monitores = monitores;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.reloj = reloj;
        this.jdbc = jdbc;
        this.enlaces = enlaces;
    }

    /** RF14: habilita correos de MONITOR en el proyecto. La auditoría guarda el rol, nunca el correo (Ley 1581). */
    @Transactional
    public ResultadoHabilitacion habilitar(UsuarioAutenticado usuario, long idProyecto, SolicitudCorreos solicitud) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        if (solicitud.rol() != RolPlataforma.MONITOR) {
            throw new SolicitudInvalidaException(
                    "ROL_INVALIDO",
                    "En un proyecto solo se habilitan monitores. Los estudiantes entran por sus cursos");
        }
        Proyecto proyecto = proyectos
                .findById(idProyecto)
                .orElseThrow(() -> new RecursoNoEncontradoException("El proyecto no existe"));
        if (!proyecto.getUsaMonitores()) {
            throw new ReglaNegocioException(
                    "PROYECTO_SIN_MONITORES", "Activa los monitores en el proyecto antes de habilitar sus correos");
        }
        Set<String> existentes = correos.findByIdProyectoOrderByEmail(idProyecto).stream()
                .map(CorreoHabilitado::getEmail)
                .collect(Collectors.toSet());
        Set<String> pedidos = new LinkedHashSet<>();
        for (String email : solicitud.emails()) {
            String normalizado = ServicioCuentas.normalizarEmail(email);
            if (ServicioSupresion.esCorreoReservado(normalizado)) {
                throw ServicioSupresion.correoReservado();
            }
            pedidos.add(normalizado);
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
            Optional<Usuario> cuenta = usuarios.findByEmail(email).filter(u -> u.getFechaAnonimizacion() == null);
            if (cuenta.isPresent() && cuenta.get().getRolPlataforma() != RolPlataforma.MONITOR) {
                conOtroRol.add(email);
                continue;
            }
            CorreoHabilitado c = new CorreoHabilitado();
            c.setIdProyecto(idProyecto);
            c.setEmail(email);
            c.setRolPlataforma(RolPlataforma.MONITOR);
            c.setIdHabilitadoPor(usuario.idUsuario());
            c.setFechaHabilitacion(ahora);
            if (cuenta.isPresent()) {
                c.setIdUsuarioRegistrado(cuenta.get().getIdUsuario());
                c.setFechaUso(ahora);
                MonitorProyecto m = new MonitorProyecto();
                m.setIdProyecto(idProyecto);
                m.setIdMonitor(cuenta.get().getIdUsuario());
                monitores.save(m);
            }
            try {
                c = correos.saveAndFlush(c);
            } catch (DataIntegrityViolationException e) {
                throw new ReglaNegocioException(
                        "CORREO_HABILITADO_EN_PARALELO",
                        "Otra operación habilitó alguno de estos correos al mismo tiempo. Vuelve a intentarlo");
            }
            auditoria.registrar(EntradaAuditoria.cambio(
                    usuario.idUsuario(),
                    "correo_habilitado",
                    c.getIdCorreoHabilitado(),
                    AccionAuditoria.HABILITAR_CORREO,
                    "rol_plataforma",
                    null,
                    RolPlataforma.MONITOR.name()));
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

    /**
     * Participantes del proyecto (desde participante_proyecto, K2 por curso), con el curso con que cuentan y su equipo
     * en el proyecto (o null). Las cuentas suprimidas no aparecen. Ordenados por nombre y correo.
     */
    @Transactional(readOnly = true)
    public List<EstudianteProyectoVista> estudiantes(UsuarioAutenticado usuario, long idProyecto) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        return jdbc.sql("SELECT pp.id_usuario, u.email, u.nombre, pp.id_curso, c.nombre AS nombre_curso, "
                        + "(SELECT m.id_equipo FROM miembro_equipo m JOIN equipo e ON e.id_equipo = m.id_equipo "
                        + "WHERE e.id_proyecto = pp.id_proyecto AND m.id_usuario = pp.id_usuario LIMIT 1) AS id_equipo "
                        + "FROM participante_proyecto pp "
                        + "JOIN usuario u ON u.id_usuario = pp.id_usuario "
                        + "JOIN curso c ON c.id_curso = pp.id_curso "
                        + "WHERE pp.id_proyecto = :proyecto AND u.fecha_anonimizacion IS NULL "
                        + "ORDER BY u.nombre, u.email")
                .param("proyecto", idProyecto)
                .query((rs, fila) -> new EstudianteProyectoVista(
                        rs.getLong("id_usuario"),
                        rs.getString("email"),
                        rs.getString("nombre"),
                        rs.getLong("id_curso"),
                        rs.getString("nombre_curso"),
                        rs.getObject("id_equipo", Long.class)))
                .list();
    }

    /**
     * Participantes no anonimizados del proyecto sin equipo en el proyecto (K2 por curso). Solo para el docente dueño.
     * Ordenados por nombre y correo.
     */
    @Transactional(readOnly = true)
    public List<EstudianteDisponibleVista> disponibles(UsuarioAutenticado usuario, long idProyecto) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        return jdbc.sql("SELECT u.email, u.nombre FROM participante_proyecto pp "
                        + "JOIN usuario u ON u.id_usuario = pp.id_usuario "
                        + "WHERE pp.id_proyecto = :proyecto AND u.fecha_anonimizacion IS NULL "
                        + "AND NOT EXISTS (SELECT 1 FROM miembro_equipo m JOIN equipo e ON e.id_equipo = m.id_equipo "
                        + "WHERE e.id_proyecto = pp.id_proyecto AND m.id_usuario = pp.id_usuario) "
                        + "ORDER BY u.nombre, u.email")
                .param("proyecto", idProyecto)
                .query((rs, fila) -> new EstudianteDisponibleVista(rs.getString("email"), rs.getString("nombre")))
                .list();
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

    /**
     * Decisión 30-4: la baja deshace el alta. Borra la asignación y la habilitación de MONITOR de esa persona en este
     * proyecto; volver a habilitar el mismo correo la asigna otra vez. Su cuenta, su historial y sus habilitaciones en
     * otros proyectos no cambian. La auditoría guarda solo ids.
     */
    @Transactional
    public void quitarMonitor(UsuarioAutenticado usuario, long idProyecto, long idMonitor) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        MonitorProyectoId id = new MonitorProyectoId(idProyecto, idMonitor);
        if (!monitores.existsById(id)) {
            throw new RecursoNoEncontradoException("Ese monitor no está asignado al proyecto");
        }
        monitores.deleteById(id);
        List<CorreoHabilitado> aBorrar = correos.findByIdProyectoOrderByEmail(idProyecto).stream()
                .filter(c -> c.getRolPlataforma() == RolPlataforma.MONITOR
                        && Objects.equals(c.getIdUsuarioRegistrado(), idMonitor))
                .toList();
        correos.deleteAll(aBorrar);
        correos.flush();

        auditoria.registrar(EntradaAuditoria.cambio(
                usuario.idUsuario(),
                "monitor_proyecto",
                idProyecto,
                AccionAuditoria.ELIMINAR,
                "id_monitor",
                String.valueOf(idMonitor),
                null));
        for (CorreoHabilitado c : aBorrar) {
            auditoria.registrar(EntradaAuditoria.de(
                    usuario.idUsuario(), "correo_habilitado", c.getIdCorreoHabilitado(), AccionAuditoria.ELIMINAR));
        }
    }

    /**
     * Decisiones 30-5 a 30-8: retira una habilitación pendiente de monitor (la persona aún no creó su cuenta). Borrado
     * condicional en una sentencia para no borrar una habilitación que se usa en ese mismo instante. Usada -> 409
     * HABILITACION_USADA. La auditoría guarda solo el id.
     */
    @Transactional
    public void retirarCorreo(UsuarioAutenticado usuario, long idProyecto, long idCorreoHabilitado) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        int borradas = jdbc.sql(
                        "DELETE FROM correo_habilitado WHERE id_correo_habilitado = :id AND id_proyecto = :p AND fecha_uso IS NULL")
                .param("id", idCorreoHabilitado)
                .param("p", idProyecto)
                .update();
        if (borradas == 1) {
            auditoria.registrar(EntradaAuditoria.de(
                    usuario.idUsuario(), "correo_habilitado", idCorreoHabilitado, AccionAuditoria.ELIMINAR));
            return;
        }
        Optional<CorreoHabilitado> opt = correos.findById(idCorreoHabilitado);
        if (opt.isEmpty() || !Objects.equals(opt.get().getIdProyecto(), idProyecto)) {
            throw new RecursoNoEncontradoException("Esa habilitación no existe en el proyecto");
        }
        throw new ReglaNegocioException(
                "HABILITACION_USADA",
                "Esa persona ya creó su cuenta. Quítala de su equipo o de los monitores del proyecto");
    }

    /** T-040 (D-15 5A): genera o reemplaza el enlace de invitación de un solo uso de una habilitación pendiente. */
    @Transactional
    public EnlaceRegistroVista generarEnlace(UsuarioAutenticado usuario, long idProyecto, long idCorreoHabilitado) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        return enlaces.generarParaProyecto(usuario.idUsuario(), idProyecto, idCorreoHabilitado);
    }
}
