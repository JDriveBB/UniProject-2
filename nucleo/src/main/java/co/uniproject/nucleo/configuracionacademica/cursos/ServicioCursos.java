package co.uniproject.nucleo.configuracionacademica.cursos;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.configuracionacademica.servicio.AuditoriaCambios;
import co.uniproject.nucleo.configuracionacademica.servicio.Textos;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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

/** Administración de cursos académicos, sus estudiantes y sus habilitaciones de registro. */
@Service
public class ServicioCursos {

    private final JdbcClient jdbc;
    private final CorreoHabilitadoRepositorio correos;
    private final UsuarioRepositorio usuarios;
    private final EnlacesRegistro enlaces;
    private final ParticipacionCursos participacion;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;

    public ServicioCursos(
            JdbcClient jdbc,
            CorreoHabilitadoRepositorio correos,
            UsuarioRepositorio usuarios,
            EnlacesRegistro enlaces,
            ParticipacionCursos participacion,
            Permisos permisos,
            RegistroAuditoria auditoria,
            Clock reloj) {
        this.jdbc = jdbc;
        this.correos = correos;
        this.usuarios = usuarios;
        this.enlaces = enlaces;
        this.participacion = participacion;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Transactional
    public CursoVista crear(UsuarioAutenticado usuario, SolicitudCurso solicitud) {
        permisos.exigir(usuario.rol() == RolPlataforma.DOCENTE);
        String nombre = solicitud.nombre().strip();
        String periodo = solicitud.periodo().strip();
        String descripcion = Textos.opcional(solicitud.descripcion());
        boolean repetido = jdbc.sql("SELECT EXISTS (SELECT 1 FROM curso WHERE id_docente = :d "
                        + "AND lower(nombre) = lower(:n) AND lower(periodo) = lower(:p))")
                .param("d", usuario.idUsuario())
                .param("n", nombre)
                .param("p", periodo)
                .query(Boolean.class)
                .single();
        if (repetido) {
            throw cursoRepetido();
        }
        long idCurso;
        try {
            idCurso = jdbc.sql("INSERT INTO curso (id_docente, nombre, periodo, descripcion, creado_en) "
                            + "VALUES (:d, :n, :p, :desc, :ahora) RETURNING id_curso")
                    .param("d", usuario.idUsuario())
                    .param("n", nombre)
                    .param("p", periodo)
                    .param("desc", descripcion)
                    .param("ahora", OffsetDateTime.ofInstant(Instant.now(reloj), ZoneOffset.UTC))
                    .query(Long.class)
                    .single();
        } catch (DataIntegrityViolationException e) {
            throw cursoRepetido();
        }
        auditoria.registrar(EntradaAuditoria.de(usuario.idUsuario(), "curso", idCurso, AccionAuditoria.CREAR));
        return vista(idCurso);
    }

    @Transactional(readOnly = true)
    public List<CursoVista> listar(UsuarioAutenticado usuario) {
        List<Long> ids;
        if (usuario.rol() == RolPlataforma.DOCENTE) {
            ids = jdbc.sql("SELECT id_curso FROM curso WHERE id_docente = :u "
                            + "ORDER BY (archivado_en IS NOT NULL), periodo DESC, nombre, id_curso")
                    .param("u", usuario.idUsuario())
                    .query(Long.class)
                    .list();
        } else if (usuario.rol() == RolPlataforma.ESTUDIANTE) {
            ids = jdbc.sql("SELECT c.id_curso FROM curso c JOIN miembro_curso mc ON mc.id_curso = c.id_curso "
                            + "WHERE mc.id_usuario = :u "
                            + "ORDER BY (c.archivado_en IS NOT NULL), c.periodo DESC, c.nombre, c.id_curso")
                    .param("u", usuario.idUsuario())
                    .query(Long.class)
                    .list();
        } else {
            permisos.exigir(false);
            return List.of();
        }
        return ids.stream().map(this::vista).toList();
    }

    @Transactional(readOnly = true)
    public CursoDetalle detalle(UsuarioAutenticado usuario, long idCurso) {
        permisos.exigir(permisos.esDocenteDelCurso(usuario.idUsuario(), idCurso));
        CursoVista curso = vista(idCurso);
        List<EstudianteCursoVista> estudiantes = jdbc.sql("SELECT u.id_usuario, u.nombre, u.email, mc.fecha_ingreso "
                        + "FROM miembro_curso mc JOIN usuario u ON u.id_usuario = mc.id_usuario "
                        + "WHERE mc.id_curso = :c AND u.fecha_anonimizacion IS NULL "
                        + "ORDER BY u.nombre, u.email")
                .param("c", idCurso)
                .query((rs, fila) -> new EstudianteCursoVista(
                        rs.getLong("id_usuario"),
                        rs.getString("nombre"),
                        rs.getString("email"),
                        rs.getObject("fecha_ingreso", OffsetDateTime.class).toInstant()))
                .list();
        List<CorreoHabilitadoVista> pendientes = correos.findByIdCursoAndFechaUsoIsNullOrderByEmail(idCurso).stream()
                .map(CorreoHabilitadoVista::de)
                .toList();
        return new CursoDetalle(curso, estudiantes, pendientes);
    }

    @Transactional
    public CursoVista editar(UsuarioAutenticado usuario, long idCurso, SolicitudCurso solicitud) {
        permisos.exigir(permisos.esDocenteDelCurso(usuario.idUsuario(), idCurso));
        DatosCurso actual = datosCurso(idCurso);
        if (actual.archivado()) {
            throw cursoArchivado();
        }
        String nombre = solicitud.nombre().strip();
        String periodo = solicitud.periodo().strip();
        String descripcion = Textos.opcional(solicitud.descripcion());
        boolean repetido = jdbc.sql("SELECT EXISTS (SELECT 1 FROM curso WHERE id_docente = :d AND id_curso <> :c "
                        + "AND lower(nombre) = lower(:n) AND lower(periodo) = lower(:p))")
                .param("d", usuario.idUsuario())
                .param("c", idCurso)
                .param("n", nombre)
                .param("p", periodo)
                .query(Boolean.class)
                .single();
        if (repetido) {
            throw cursoRepetido();
        }
        try {
            jdbc.sql("UPDATE curso SET nombre = :n, periodo = :p, descripcion = :desc WHERE id_curso = :c")
                    .param("n", nombre)
                    .param("p", periodo)
                    .param("desc", descripcion)
                    .param("c", idCurso)
                    .update();
        } catch (DataIntegrityViolationException e) {
            throw cursoRepetido();
        }
        long docente = usuario.idUsuario();
        AuditoriaCambios.siCambia(auditoria, docente, "curso", idCurso, "nombre", actual.nombre(), nombre);
        AuditoriaCambios.siCambia(auditoria, docente, "curso", idCurso, "periodo", actual.periodo(), periodo);
        AuditoriaCambios.siCambia(
                auditoria, docente, "curso", idCurso, "descripcion", actual.descripcion(), descripcion);
        return vista(idCurso);
    }

    @Transactional
    public CursoVista cambiarArchivado(UsuarioAutenticado usuario, long idCurso, SolicitudArchivado solicitud) {
        permisos.exigir(permisos.esDocenteDelCurso(usuario.idUsuario(), idCurso));
        boolean anterior = estaArchivado(idCurso);
        boolean nuevo = solicitud.archivado();
        if (anterior != nuevo) {
            if (nuevo) {
                jdbc.sql("UPDATE curso SET archivado_en = :ahora WHERE id_curso = :c")
                        .param("ahora", OffsetDateTime.ofInstant(Instant.now(reloj), ZoneOffset.UTC))
                        .param("c", idCurso)
                        .update();
            } else {
                jdbc.sql("UPDATE curso SET archivado_en = NULL WHERE id_curso = :c")
                        .param("c", idCurso)
                        .update();
            }
            auditoria.registrar(EntradaAuditoria.cambio(
                    usuario.idUsuario(),
                    "curso",
                    idCurso,
                    AccionAuditoria.ACTUALIZAR,
                    "archivado",
                    String.valueOf(anterior),
                    String.valueOf(nuevo)));
        }
        return vista(idCurso);
    }

    @Transactional
    public void borrar(UsuarioAutenticado usuario, long idCurso) {
        permisos.exigir(permisos.esDocenteDelCurso(usuario.idUsuario(), idCurso));
        // Orden de candados: primero correo_habilitado, luego curso. Si el curso está en uso, el rollback deshace esto.
        jdbc.sql("DELETE FROM correo_habilitado WHERE id_curso = :c")
                .param("c", idCurso)
                .update();
        jdbc.sql("SELECT id_curso FROM curso WHERE id_curso = :c FOR UPDATE")
                .param("c", idCurso)
                .query(Long.class)
                .single();
        boolean enUso = jdbc.sql("SELECT EXISTS (SELECT 1 FROM curso_proyecto WHERE id_curso = :c) "
                        + "OR EXISTS (SELECT 1 FROM participante_proyecto WHERE id_curso = :c) "
                        + "OR EXISTS (SELECT 1 FROM equipo WHERE id_curso = :c)")
                .param("c", idCurso)
                .query(Boolean.class)
                .single();
        if (enUso) {
            throw new ReglaNegocioException(
                    "CURSO_EN_USO", "Ese curso ya se usó en un proyecto. Archívalo en vez de borrarlo");
        }
        jdbc.sql("DELETE FROM curso WHERE id_curso = :c").param("c", idCurso).update();
        auditoria.registrar(EntradaAuditoria.de(usuario.idUsuario(), "curso", idCurso, AccionAuditoria.ELIMINAR));
    }

    @Transactional
    public ResultadoCorreosCurso agregarCorreos(
            UsuarioAutenticado usuario, long idCurso, SolicitudCorreosCurso solicitud) {
        permisos.exigir(permisos.esDocenteDelCurso(usuario.idUsuario(), idCurso));
        participacion.bloquearCurso(idCurso);
        if (estaArchivado(idCurso)) {
            throw cursoArchivado();
        }
        Set<String> pedidos = new LinkedHashSet<>();
        for (String email : solicitud.emails()) {
            String normalizado = ServicioCuentas.normalizarEmail(email);
            if (ServicioSupresion.esCorreoReservado(normalizado)) {
                throw ServicioSupresion.correoReservado();
            }
            pedidos.add(normalizado);
        }
        Set<String> pendientes = correos.findByIdCursoAndFechaUsoIsNullOrderByEmail(idCurso).stream()
                .map(CorreoHabilitado::getEmail)
                .collect(Collectors.toSet());
        Instant ahora = Instant.now(reloj);
        List<String> agregados = new ArrayList<>();
        List<CorreoHabilitadoVista> habilitados = new ArrayList<>();
        List<String> yaEnCurso = new ArrayList<>();
        List<String> conOtroRol = new ArrayList<>();
        for (String email : pedidos) {
            if (pendientes.contains(email)) {
                yaEnCurso.add(email);
                continue;
            }
            Optional<Usuario> cuenta = usuarios.findByEmail(email).filter(u -> u.getFechaAnonimizacion() == null);
            if (cuenta.isPresent()) {
                if (cuenta.get().getRolPlataforma() != RolPlataforma.ESTUDIANTE) {
                    conOtroRol.add(email);
                    continue;
                }
                if (participacion.incorporar(
                        usuario.idUsuario(), idCurso, cuenta.get().getIdUsuario())) {
                    agregados.add(email);
                } else {
                    yaEnCurso.add(email);
                }
                continue;
            }
            CorreoHabilitado c = new CorreoHabilitado();
            c.setIdCurso(idCurso);
            c.setEmail(email);
            c.setRolPlataforma(RolPlataforma.ESTUDIANTE);
            c.setIdHabilitadoPor(usuario.idUsuario());
            c.setFechaHabilitacion(ahora);
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
                    RolPlataforma.ESTUDIANTE.name()));
            habilitados.add(CorreoHabilitadoVista.de(c));
        }
        return new ResultadoCorreosCurso(agregados, habilitados, yaEnCurso, conOtroRol);
    }

    /** Sin candado del curso para evitar ciclos con el registro; permitido aunque el curso esté archivado. */
    @Transactional
    public void retirarCorreo(UsuarioAutenticado usuario, long idCurso, long idCorreoHabilitado) {
        permisos.exigir(permisos.esDocenteDelCurso(usuario.idUsuario(), idCurso));
        int borradas = jdbc.sql(
                        "DELETE FROM correo_habilitado WHERE id_correo_habilitado = :id AND id_curso = :c AND fecha_uso IS NULL")
                .param("id", idCorreoHabilitado)
                .param("c", idCurso)
                .update();
        if (borradas == 1) {
            auditoria.registrar(EntradaAuditoria.de(
                    usuario.idUsuario(), "correo_habilitado", idCorreoHabilitado, AccionAuditoria.ELIMINAR));
            return;
        }
        Optional<CorreoHabilitado> opt = correos.findById(idCorreoHabilitado);
        if (opt.isEmpty() || !Objects.equals(opt.get().getIdCurso(), idCurso)) {
            throw new RecursoNoEncontradoException("Esa habilitación no existe en el curso");
        }
        throw new ReglaNegocioException("HABILITACION_USADA", "Esa persona ya creó su cuenta. Quítala del curso");
    }

    /** Sin candado del curso para evitar ciclos con el registro. */
    @Transactional
    public EnlaceRegistroVista generarEnlace(UsuarioAutenticado usuario, long idCurso, long idCorreoHabilitado) {
        permisos.exigir(permisos.esDocenteDelCurso(usuario.idUsuario(), idCurso));
        if (estaArchivado(idCurso)) {
            throw cursoArchivado();
        }
        return enlaces.generarParaCurso(usuario.idUsuario(), idCurso, idCorreoHabilitado);
    }

    @Transactional
    public void quitarEstudiante(UsuarioAutenticado usuario, long idCurso, long idUsuario) {
        permisos.exigir(permisos.esDocenteDelCurso(usuario.idUsuario(), idCurso));
        participacion.retirar(usuario.idUsuario(), idCurso, idUsuario);
    }

    /** Nombres de los miembros activos del curso, ordenados por nombre. Sin correos ni ids (Ley 1581). */
    @Transactional(readOnly = true)
    public List<CompaneroVista> companeros(UsuarioAutenticado usuario, long idCurso) {
        permisos.exigir(permisos.puedeVerCurso(usuario.idUsuario(), idCurso));
        return jdbc.sql("SELECT u.nombre FROM miembro_curso mc JOIN usuario u ON u.id_usuario = mc.id_usuario "
                        + "WHERE mc.id_curso = :c AND u.fecha_anonimizacion IS NULL ORDER BY u.nombre")
                .param("c", idCurso)
                .query((rs, fila) -> new CompaneroVista(rs.getString("nombre")))
                .list();
    }

    private CursoVista vista(long idCurso) {
        List<ProyectoResumen> proyectos = jdbc.sql("SELECT p.id_proyecto, p.nombre FROM curso_proyecto cp "
                        + "JOIN proyecto p ON p.id_proyecto = cp.id_proyecto "
                        + "WHERE cp.id_curso = :c ORDER BY p.nombre, p.id_proyecto")
                .param("c", idCurso)
                .query((rs, fila) -> new ProyectoResumen(rs.getLong("id_proyecto"), rs.getString("nombre")))
                .list();
        return jdbc.sql(
                        "SELECT c.id_curso, c.nombre, c.periodo, c.descripcion, c.archivado_en IS NOT NULL AS archivado, "
                                + "d.nombre AS nombre_docente, "
                                + "(SELECT count(*) FROM miembro_curso mc JOIN usuario u ON u.id_usuario = mc.id_usuario "
                                + "WHERE mc.id_curso = c.id_curso AND u.fecha_anonimizacion IS NULL) AS estudiantes "
                                + "FROM curso c JOIN usuario d ON d.id_usuario = c.id_docente WHERE c.id_curso = :c")
                .param("c", idCurso)
                .query((rs, fila) -> new CursoVista(
                        rs.getLong("id_curso"),
                        rs.getString("nombre"),
                        rs.getString("periodo"),
                        rs.getString("descripcion"),
                        rs.getBoolean("archivado"),
                        rs.getString("nombre_docente"),
                        rs.getInt("estudiantes"),
                        proyectos))
                .single();
    }

    private DatosCurso datosCurso(long idCurso) {
        return jdbc.sql("SELECT nombre, periodo, descripcion, archivado_en IS NOT NULL AS archivado "
                        + "FROM curso WHERE id_curso = :c")
                .param("c", idCurso)
                .query((rs, fila) -> new DatosCurso(
                        rs.getString("nombre"),
                        rs.getString("periodo"),
                        rs.getString("descripcion"),
                        rs.getBoolean("archivado")))
                .single();
    }

    private boolean estaArchivado(long idCurso) {
        return jdbc.sql("SELECT archivado_en IS NOT NULL FROM curso WHERE id_curso = :c")
                .param("c", idCurso)
                .query(Boolean.class)
                .single();
    }

    private record DatosCurso(String nombre, String periodo, String descripcion, boolean archivado) {}

    private static ReglaNegocioException cursoRepetido() {
        return new ReglaNegocioException("CURSO_REPETIDO", "Ya tienes un curso con ese nombre en ese periodo");
    }

    public static ReglaNegocioException cursoArchivado() {
        return new ReglaNegocioException("CURSO_ARCHIVADO", "El curso está archivado. Desarchívalo para cambiarlo");
    }
}
