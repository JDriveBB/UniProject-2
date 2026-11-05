package co.uniproject.nucleo.seguridad.cuentas;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.configuracionacademica.dominio.MonitorProyecto;
import co.uniproject.nucleo.configuracionacademica.repositorio.MonitorProyectoRepositorio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.dominio.CorreoHabilitado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import co.uniproject.nucleo.seguridad.dominio.Usuario;
import co.uniproject.nucleo.seguridad.repositorio.CorreoHabilitadoRepositorio;
import co.uniproject.nucleo.seguridad.repositorio.UsuarioRepositorio;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inicio de sesión, alta de cuentas por correo habilitado (K2, R-10) y habilitación de docentes (R-17). */
@Service
public class ServicioCuentas {

    static final int MINIMO_CARACTERES_CONTRASENA = 10;
    static final int MAXIMO_BYTES_CONTRASENA = 72;

    private final UsuarioRepositorio usuarios;
    private final CorreoHabilitadoRepositorio correos;
    private final MonitorProyectoRepositorio monitores;
    private final PasswordEncoder codificador;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;
    private final String hashFicticio;

    public ServicioCuentas(UsuarioRepositorio usuarios, CorreoHabilitadoRepositorio correos,
                           MonitorProyectoRepositorio monitores, PasswordEncoder codificador,
                           RegistroAuditoria auditoria, Clock reloj) {
        this.usuarios = usuarios;
        this.correos = correos;
        this.monitores = monitores;
        this.codificador = codificador;
        this.auditoria = auditoria;
        this.reloj = reloj;
        this.hashFicticio = codificador.encode("igualar-tiempo-de-respuesta");
    }

    /** Forma guardada de un correo: sin espacios alrededor y en minúscula. */
    public static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * Valida correo y contraseña. Correo inexistente y contraseña errada responden igual y cuestan un bcrypt cada uno,
     * para no revelar qué correos tienen cuenta. Una cuenta anonimizada (bloque 27) cuenta como inexistente. Ambos casos quedan en la auditoría, sin el correo ni la contraseña.
     */
    @Transactional
    public Optional<UsuarioAutenticado> autenticar(String email, String contrasena) {
        Optional<Usuario> usuario = usuarios.findByEmail(normalizarEmail(email))
                .filter(u -> u.getFechaAnonimizacion() == null);
        boolean valida = bytes(contrasena) <= MAXIMO_BYTES_CONTRASENA
                && codificador.matches(contrasena, usuario.map(Usuario::getPasswordHash).orElse(hashFicticio))
                && usuario.isPresent();
        Long id = usuario.map(Usuario::getIdUsuario).orElse(null);
        if (!valida) {
            auditoria.registrar(EntradaAuditoria.de(id, "usuario", id, AccionAuditoria.LOGIN_FALLIDO));
            return Optional.empty();
        }
        Usuario u = usuario.orElseThrow();
        auditoria.registrar(EntradaAuditoria.de(id, "usuario", id, AccionAuditoria.LOGIN_EXITOSO));
        return Optional.of(new UsuarioAutenticado(u.getIdUsuario(), u.getNombre(), u.getEmail(), u.getRolPlataforma()));
    }

    /** Crea la cuenta si el correo está habilitado y sin usar. El rol sale de la habilitación, no de la petición. */
    @Transactional
    public UsuarioSesion registrar(SolicitudRegistro solicitud) {
        String email = normalizarEmail(solicitud.email());
        if (usuarios.existsByEmail(email)) {
            throw cuentaExistente();
        }
        List<CorreoHabilitado> habilitados = correos.findByEmailAndFechaUsoIsNull(email);
        if (habilitados.isEmpty()) {
            throw new ReglaNegocioException("CORREO_NO_HABILITADO",
                    "Ese correo no está habilitado. Pide a tu docente que lo agregue al proyecto");
        }
        Set<RolPlataforma> roles = habilitados.stream()
                .map(CorreoHabilitado::getRolPlataforma)
                .collect(Collectors.toSet());
        if (roles.size() > 1) {
            throw new ReglaNegocioException("HABILITACION_AMBIGUA",
                    "Ese correo está habilitado con roles distintos. Pide a tu docente que lo corrija");
        }
        Instant ahora = Instant.now(reloj);
        Usuario usuario = nuevoUsuario(solicitud.nombre(), email, solicitud.contrasena(), roles.iterator().next(), ahora);
        try {
            usuario = usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw cuentaExistente();
        }
        for (CorreoHabilitado c : habilitados) {
            c.setIdUsuarioRegistrado(usuario.getIdUsuario());
            c.setFechaUso(ahora);
            if (c.getRolPlataforma() == RolPlataforma.MONITOR) {
                MonitorProyecto m = new MonitorProyecto();
                m.setIdProyecto(c.getIdProyecto());
                m.setIdMonitor(usuario.getIdUsuario());
                monitores.save(m);
            }
        }
        correos.saveAll(habilitados);
        auditoria.registrar(EntradaAuditoria.de(usuario.getIdUsuario(), "usuario", usuario.getIdUsuario(),
                AccionAuditoria.REGISTRO_CUENTA));
        return vista(usuario);
    }

    /** El administrador habilita el correo de un docente; el docente crea su cuenta con /api/cuentas/registro. */
    @Transactional
    public CorreoHabilitadoVista habilitarDocente(long idAdministrador, String emailSolicitado) {
        String email = normalizarEmail(emailSolicitado);
        if (ServicioSupresion.esCorreoReservado(email)) {
            throw ServicioSupresion.correoReservado();
        }
        boolean pendiente = correos.findByEmailAndFechaUsoIsNull(email).stream()
                .anyMatch(c -> c.getRolPlataforma() == RolPlataforma.DOCENTE);
        if (pendiente || usuarios.existsByEmail(email)) {
            throw yaHabilitado();
        }
        CorreoHabilitado c = new CorreoHabilitado();
        c.setEmail(email);
        c.setRolPlataforma(RolPlataforma.DOCENTE);
        c.setIdHabilitadoPor(idAdministrador);
        c.setFechaHabilitacion(Instant.now(reloj));
        try {
            c = correos.saveAndFlush(c);
        } catch (DataIntegrityViolationException e) {
            throw yaHabilitado();
        }
        auditoria.registrar(EntradaAuditoria.cambio(idAdministrador, "correo_habilitado", c.getIdCorreoHabilitado(),
                AccionAuditoria.HABILITAR_CORREO, "rol_plataforma", null, RolPlataforma.DOCENTE.name()));
        return CorreoHabilitadoVista.de(c);
    }

    @Transactional(readOnly = true)
    public List<CorreoHabilitadoVista> docentesHabilitados() {
        return correos.findByRolPlataformaOrderByEmail(RolPlataforma.DOCENTE).stream()
                .map(CorreoHabilitadoVista::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioSesion sesion(long idUsuario) {
        return usuarios.findById(idUsuario)
                .map(ServicioCuentas::vista)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta ya no existe"));
    }

    /** Primer administrador (R-17). false si ya hay uno o si el correo ya tiene cuenta. */
    @Transactional
    public boolean crearAdministradorInicial(String emailConfigurado, String nombre, String contrasena) {
        String email = normalizarEmail(emailConfigurado);
        if (usuarios.existsByRolPlataforma(RolPlataforma.ADMINISTRADOR) || usuarios.existsByEmail(email)) {
            return false;
        }
        Usuario admin = usuarios.saveAndFlush(
                nuevoUsuario(nombre, email, contrasena, RolPlataforma.ADMINISTRADOR, Instant.now(reloj)));
        auditoria.registrar(EntradaAuditoria.de(null, "usuario", admin.getIdUsuario(), AccionAuditoria.REGISTRO_CUENTA));
        return true;
    }

    private Usuario nuevoUsuario(String nombre, String email, String contrasena, RolPlataforma rol, Instant ahora) {
        if (contrasena.length() < MINIMO_CARACTERES_CONTRASENA || bytes(contrasena) > MAXIMO_BYTES_CONTRASENA) {
            throw new SolicitudInvalidaException("CONTRASENA_INVALIDA",
                    "La contraseña debe tener al menos 10 caracteres y no más de 72 bytes");
        }
        Usuario u = new Usuario();
        u.setNombre(nombre.strip());
        u.setEmail(email);
        u.setPasswordHash(codificador.encode(contrasena));
        u.setRolPlataforma(rol);
        u.setAutorizaTratamientoDatos(true);
        u.setFechaAutorizacion(ahora);
        u.setFechaRegistro(ahora);
        return u;
    }

    private static UsuarioSesion vista(Usuario u) {
        return new UsuarioSesion(u.getIdUsuario(), u.getNombre(), u.getEmail(), u.getRolPlataforma(),
                u.getTelegramChatId() != null);
    }

    private static int bytes(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8).length;
    }

    private static ReglaNegocioException cuentaExistente() {
        return new ReglaNegocioException("CUENTA_EXISTENTE", "Ya existe una cuenta con ese correo");
    }

    private static ReglaNegocioException yaHabilitado() {
        return new ReglaNegocioException("YA_HABILITADO", "Ese correo ya está habilitado o ya tiene cuenta");
    }
}
