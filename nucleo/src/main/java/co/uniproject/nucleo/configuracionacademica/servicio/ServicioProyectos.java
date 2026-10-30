package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.configuracionacademica.dominio.Proyecto;
import co.uniproject.nucleo.configuracionacademica.repositorio.CriterioRubricaRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.EquipoRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.HitoRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.ProyectoRepositorio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF1: proyectos. Ver: Permisos.puedeVerProyecto. Crear: rol DOCENTE. Cambiar: solo su docente. */
@Service
public class ServicioProyectos {

    /** Misma regla que PermisosJdbc.puedeVerProyecto, para listar. */
    private static final String VISIBLES = "SELECT p.id_proyecto FROM proyecto p WHERE p.id_docente = :usuario "
            + "OR (p.usa_monitores AND EXISTS (SELECT 1 FROM monitor_proyecto mp WHERE mp.id_proyecto = p.id_proyecto "
            + "AND mp.id_monitor = :usuario)) OR EXISTS (SELECT 1 FROM equipo e JOIN miembro_equipo m "
            + "ON m.id_equipo = e.id_equipo WHERE e.id_proyecto = p.id_proyecto AND m.id_usuario = :usuario) "
            + "ORDER BY p.fecha_inicio DESC, p.id_proyecto";

    private final ProyectoRepositorio proyectos;
    private final HitoRepositorio hitos;
    private final CriterioRubricaRepositorio criterios;
    private final EquipoRepositorio equipos;
    private final ApoyoConfiguracion apoyo;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final JdbcClient jdbc;

    public ServicioProyectos(ProyectoRepositorio proyectos, HitoRepositorio hitos, CriterioRubricaRepositorio criterios,
                             EquipoRepositorio equipos, ApoyoConfiguracion apoyo, Permisos permisos,
                             RegistroAuditoria auditoria, JdbcClient jdbc) {
        this.proyectos = proyectos;
        this.hitos = hitos;
        this.criterios = criterios;
        this.equipos = equipos;
        this.apoyo = apoyo;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
    }

    @Transactional
    public ProyectoVista crear(UsuarioAutenticado usuario, SolicitudProyecto solicitud) {
        permisos.exigir(usuario.rol() == RolPlataforma.DOCENTE);
        validarFechas(solicitud.fechaInicio(), solicitud.fechaFin());
        Proyecto p = new Proyecto();
        p.setIdDocente(usuario.idUsuario());
        aplicar(p, solicitud);
        p = proyectos.saveAndFlush(p);
        auditoria.registrar(EntradaAuditoria.de(usuario.idUsuario(), "proyecto", p.getIdProyecto(), AccionAuditoria.CREAR));
        return ProyectoVista.de(p);
    }

    @Transactional(readOnly = true)
    public List<ProyectoVista> listar(UsuarioAutenticado usuario) {
        List<Long> ids = jdbc.sql(VISIBLES).param("usuario", usuario.idUsuario()).query(Long.class).list();
        Map<Long, Proyecto> porId = proyectos.findAllById(ids).stream()
                .collect(Collectors.toMap(Proyecto::getIdProyecto, Function.identity()));
        return ids.stream().map(porId::get).map(ProyectoVista::de).toList();
    }

    @Transactional(readOnly = true)
    public ProyectoDetalle detalle(UsuarioAutenticado usuario, long idProyecto) {
        permisos.exigir(permisos.puedeVerProyecto(usuario.idUsuario(), idProyecto));
        Proyecto p = buscar(idProyecto);
        List<HitoVista> vistasHitos = hitos.findByIdProyectoOrderByOrden(idProyecto).stream()
                .map(h -> HitoVista.de(h, criterios.findByIdHitoOrderByIdCriterio(h.getIdHito())))
                .toList();
        List<EquipoVista> vistasEquipos = equipos.findByIdProyectoOrderByNombreEquipo(idProyecto).stream()
                .map(apoyo::vistaEquipo)
                .toList();
        return new ProyectoDetalle(ProyectoVista.de(p), vistasHitos, vistasEquipos);
    }

    @Transactional
    public ProyectoVista actualizar(UsuarioAutenticado usuario, long idProyecto, SolicitudProyecto solicitud) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        validarFechas(solicitud.fechaInicio(), solicitud.fechaFin());
        Proyecto p = buscar(idProyecto);
        boolean hitosFuera = jdbc.sql("SELECT EXISTS (SELECT 1 FROM hito WHERE id_proyecto = :proyecto "
                        + "AND (fecha_inicio < :inicio OR fecha_limite > :fin))")
                .param("proyecto", idProyecto)
                .param("inicio", solicitud.fechaInicio())
                .param("fin", solicitud.fechaFin())
                .query(Boolean.class)
                .single();
        if (hitosFuera) {
            throw new ReglaNegocioException("HITOS_FUERA_DE_FECHAS",
                    "Hay hitos fuera de las nuevas fechas del proyecto. Ajusta primero sus fechas");
        }
        long id = usuario.idUsuario();
        AuditoriaCambios.siCambia(auditoria, id, "proyecto", idProyecto, "nombre", p.getNombre(),
                solicitud.nombre().strip());
        AuditoriaCambios.siCambia(auditoria, id, "proyecto", idProyecto, "descripcion", p.getDescripcion(),
                Textos.opcional(solicitud.descripcion()));
        AuditoriaCambios.siCambia(auditoria, id, "proyecto", idProyecto, "fecha_inicio", p.getFechaInicio(),
                solicitud.fechaInicio());
        AuditoriaCambios.siCambia(auditoria, id, "proyecto", idProyecto, "fecha_fin", p.getFechaFin(),
                solicitud.fechaFin());
        AuditoriaCambios.siCambia(auditoria, id, "proyecto", idProyecto, "usa_monitores", p.getUsaMonitores(),
                solicitud.usaMonitores());
        aplicar(p, solicitud);
        return ProyectoVista.de(proyectos.save(p));
    }

    /** Fin no anterior al inicio (proyecto y hito). */
    static void validarFechas(LocalDate inicio, LocalDate fin) {
        if (fin.isBefore(inicio)) {
            throw new SolicitudInvalidaException("FECHAS_INVALIDAS", "La fecha final no puede ser anterior a la inicial");
        }
    }

    private Proyecto buscar(long idProyecto) {
        return proyectos.findById(idProyecto)
                .orElseThrow(() -> new RecursoNoEncontradoException("El proyecto no existe"));
    }

    private static void aplicar(Proyecto p, SolicitudProyecto s) {
        p.setNombre(s.nombre().strip());
        p.setDescripcion(Textos.opcional(s.descripcion()));
        p.setFechaInicio(s.fechaInicio());
        p.setFechaFin(s.fechaFin());
        p.setUsaMonitores(s.usaMonitores());
    }
}
