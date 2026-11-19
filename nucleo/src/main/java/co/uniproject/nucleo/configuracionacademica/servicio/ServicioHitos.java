package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.configuracionacademica.dominio.CriterioRubrica;
import co.uniproject.nucleo.configuracionacademica.dominio.Hito;
import co.uniproject.nucleo.configuracionacademica.dominio.Proyecto;
import co.uniproject.nucleo.configuracionacademica.repositorio.CriterioRubricaRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.HitoRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.ProyectoRepositorio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF1 y RF6 (configuración): hitos del proyecto, su rúbrica y su alerta de inactividad. Solo el docente dueño. */
@Service
public class ServicioHitos {

    static final BigDecimal CIEN = new BigDecimal("100");

    private final HitoRepositorio hitos;
    private final CriterioRubricaRepositorio criterios;
    private final ProyectoRepositorio proyectos;
    private final ApoyoConfiguracion apoyo;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final JdbcClient jdbc;

    public ServicioHitos(HitoRepositorio hitos, CriterioRubricaRepositorio criterios, ProyectoRepositorio proyectos,
                         ApoyoConfiguracion apoyo, Permisos permisos, RegistroAuditoria auditoria, JdbcClient jdbc) {
        this.hitos = hitos;
        this.criterios = criterios;
        this.proyectos = proyectos;
        this.apoyo = apoyo;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
    }

    @Transactional
    public HitoVista crear(UsuarioAutenticado usuario, long idProyecto, SolicitudHito solicitud) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        Proyecto proyecto = proyectos.findById(idProyecto)
                .orElseThrow(() -> new RecursoNoEncontradoException("El proyecto no existe"));
        validar(proyecto, solicitud);
        exigirOrdenLibre(idProyecto, solicitud.orden(), null);
        Hito h = new Hito();
        h.setIdProyecto(idProyecto);
        aplicar(h, solicitud);
        h = guardar(h);
        apoyo.completarHitosEquipo(idProyecto);
        auditoria.registrar(EntradaAuditoria.de(usuario.idUsuario(), "hito", h.getIdHito(), AccionAuditoria.CREAR));
        return HitoVista.de(h, List.of());
    }

    /** PUT: reemplaza todo; diasInactividadAlerta nulo = sin alerta (bloque 30); notificarDocenteInactividad nulo = false. */
    @Transactional
    public HitoVista actualizar(UsuarioAutenticado usuario, long idHito, SolicitudHito solicitud) {
        Hito h = hitoDelDocente(usuario, idHito);
        Proyecto proyecto = proyectos.findById(h.getIdProyecto()).orElseThrow();
        validar(proyecto, solicitud);
        exigirOrdenLibre(h.getIdProyecto(), solicitud.orden(), idHito);
        long id = usuario.idUsuario();
        AuditoriaCambios.siCambia(auditoria, id, "hito", idHito, "nombre_hito", h.getNombreHito(),
                solicitud.nombreHito().strip());
        AuditoriaCambios.siCambia(auditoria, id, "hito", idHito, "descripcion", h.getDescripcion(),
                Textos.opcional(solicitud.descripcion()));
        AuditoriaCambios.siCambia(auditoria, id, "hito", idHito, "orden", h.getOrden(), solicitud.orden());
        AuditoriaCambios.siCambia(auditoria, id, "hito", idHito, "fecha_inicio", h.getFechaInicio(),
                solicitud.fechaInicio());
        AuditoriaCambios.siCambia(auditoria, id, "hito", idHito, "fecha_limite", h.getFechaLimite(),
                solicitud.fechaLimite());
        AuditoriaCambios.siCambia(auditoria, id, "hito", idHito, "dias_inactividad_alerta",
                h.getDiasInactividadAlerta(), solicitud.diasInactividadAlerta());
        AuditoriaCambios.siCambia(auditoria, id, "hito", idHito, "notificar_docente_inactividad",
                h.getNotificarDocenteInactividad(), Boolean.TRUE.equals(solicitud.notificarDocenteInactividad()));
        aplicar(h, solicitud);
        h = guardar(h);
        return HitoVista.de(h, criterios.findByIdHitoOrderByIdCriterio(idHito));
    }

    @Transactional
    public CriterioVista crearCriterio(UsuarioAutenticado usuario, long idHito, SolicitudCriterio solicitud) {
        hitoDelDocente(usuario, idHito);
        bloquearHito(idHito);
        exigirRubricaLibre(idHito);
        exigirPesos(idHito, null, solicitud.pesoPorcentaje());
        CriterioRubrica c = new CriterioRubrica();
        c.setIdHito(idHito);
        aplicar(c, solicitud);
        c = criterios.saveAndFlush(c);
        auditoria.registrar(EntradaAuditoria.de(usuario.idUsuario(), "criterio_rubrica", c.getIdCriterio(),
                AccionAuditoria.CREAR));
        return CriterioVista.de(c);
    }

    @Transactional
    public CriterioVista actualizarCriterio(UsuarioAutenticado usuario, long idHito, long idCriterio,
                                            SolicitudCriterio solicitud) {
        hitoDelDocente(usuario, idHito);
        bloquearHito(idHito);
        CriterioRubrica c = criterioDelHito(idHito, idCriterio);
        exigirRubricaLibre(idHito);
        exigirPesos(idHito, idCriterio, solicitud.pesoPorcentaje());
        long id = usuario.idUsuario();
        AuditoriaCambios.siCambia(auditoria, id, "criterio_rubrica", idCriterio, "descripcion", c.getDescripcion(),
                solicitud.descripcion().strip());
        AuditoriaCambios.siCambia(auditoria, id, "criterio_rubrica", idCriterio, "peso_porcentaje",
                c.getPesoPorcentaje(), solicitud.pesoPorcentaje());
        AuditoriaCambios.siCambia(auditoria, id, "criterio_rubrica", idCriterio, "puntaje_maximo",
                c.getPuntajeMaximo(), solicitud.puntajeMaximo());
        aplicar(c, solicitud);
        return CriterioVista.de(criterios.save(c));
    }

    @Transactional
    public void eliminarCriterio(UsuarioAutenticado usuario, long idHito, long idCriterio) {
        hitoDelDocente(usuario, idHito);
        bloquearHito(idHito);
        CriterioRubrica c = criterioDelHito(idHito, idCriterio);
        exigirRubricaLibre(idHito);
        criterios.delete(c);
        auditoria.registrar(EntradaAuditoria.de(usuario.idUsuario(), "criterio_rubrica", idCriterio,
                AccionAuditoria.ELIMINAR));
    }

    /** 403 si el hito no existe o no es de un proyecto del docente (decisión 15-2). */
    private Hito hitoDelDocente(UsuarioAutenticado usuario, long idHito) {
        Optional<Hito> hito = hitos.findById(idHito);
        permisos.exigir(hito.isPresent()
                && permisos.esDocenteDelProyecto(usuario.idUsuario(), hito.get().getIdProyecto()));
        return hito.get();
    }

    private CriterioRubrica criterioDelHito(long idHito, long idCriterio) {
        return criterios.findById(idCriterio)
                .filter(c -> c.getIdHito() == idHito)
                .orElseThrow(() -> new RecursoNoEncontradoException("El criterio no existe en este hito"));
    }

    private void exigirRubricaLibre(long idHito) {
        boolean enUso = jdbc.sql("SELECT EXISTS (SELECT 1 FROM calificacion_criterio cc JOIN criterio_rubrica c "
                        + "ON c.id_criterio = cc.id_criterio WHERE c.id_hito = :hito)")
                .param("hito", idHito)
                .query(Boolean.class)
                .single();
        if (enUso) {
            throw new ReglaNegocioException("RUBRICA_EN_USO",
                    "La rúbrica ya se usó para calificar este hito: no se puede cambiar");
        }
    }

    /** Serializa altas y ediciones de criterios del mismo hito para que la suma de pesos no pase de 100 %. */
    private void bloquearHito(long idHito) {
        jdbc.sql("SELECT id_hito FROM hito WHERE id_hito = :hito FOR NO KEY UPDATE")
                .param("hito", idHito)
                .query(Long.class)
                .single();
    }

    private void exigirPesos(long idHito, Long idCriterioExcluido, BigDecimal pesoNuevo) {
        BigDecimal total = criterios.findByIdHitoOrderByIdCriterio(idHito).stream()
                .filter(c -> !Objects.equals(c.getIdCriterio(), idCriterioExcluido))
                .map(CriterioRubrica::getPesoPorcentaje)
                .reduce(pesoNuevo, BigDecimal::add);
        if (total.compareTo(CIEN) > 0) {
            throw new ReglaNegocioException("PESOS_EXCEDEN_100", "Los pesos de la rúbrica sumarían "
                    + total.stripTrailingZeros().toPlainString() + " %; el máximo es 100 %");
        }
    }

    private void exigirOrdenLibre(long idProyecto, int orden, Long idHitoExcluido) {
        boolean ocupado = hitos.findByIdProyectoOrderByOrden(idProyecto).stream()
                .anyMatch(h -> h.getOrden() == orden && !Objects.equals(h.getIdHito(), idHitoExcluido));
        if (ocupado) {
            throw ordenRepetido();
        }
    }

    private Hito guardar(Hito h) {
        try {
            return hitos.saveAndFlush(h);
        } catch (DataIntegrityViolationException e) {
            throw ordenRepetido();
        }
    }

    private static void validar(Proyecto proyecto, SolicitudHito s) {
        ServicioProyectos.validarFechas(s.fechaInicio(), s.fechaLimite());
        if (s.fechaInicio().isBefore(proyecto.getFechaInicio()) || s.fechaLimite().isAfter(proyecto.getFechaFin())) {
            throw new SolicitudInvalidaException("FECHAS_FUERA_DEL_PROYECTO",
                    "Las fechas del hito deben estar dentro de las del proyecto");
        }
    }

    private static void aplicar(Hito h, SolicitudHito s) {
        h.setNombreHito(s.nombreHito().strip());
        h.setDescripcion(Textos.opcional(s.descripcion()));
        h.setOrden(s.orden());
        h.setFechaInicio(s.fechaInicio());
        h.setFechaLimite(s.fechaLimite());
        h.setDiasInactividadAlerta(s.diasInactividadAlerta());
        h.setNotificarDocenteInactividad(Boolean.TRUE.equals(s.notificarDocenteInactividad()));
    }

    private static void aplicar(CriterioRubrica c, SolicitudCriterio s) {
        c.setDescripcion(s.descripcion().strip());
        c.setPesoPorcentaje(s.pesoPorcentaje());
        c.setPuntajeMaximo(s.puntajeMaximo());
    }

    private static ReglaNegocioException ordenRepetido() {
        return new ReglaNegocioException("ORDEN_REPETIDO", "Ya hay un hito con ese orden en el proyecto");
    }
}
