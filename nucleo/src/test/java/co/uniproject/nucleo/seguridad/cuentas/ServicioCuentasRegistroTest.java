package co.uniproject.nucleo.seguridad.cuentas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.configuracionacademica.cursos.ParticipacionCursos;
import co.uniproject.nucleo.configuracionacademica.repositorio.MonitorProyectoRepositorio;
import co.uniproject.nucleo.seguridad.dominio.CorreoHabilitado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import co.uniproject.nucleo.seguridad.dominio.Usuario;
import co.uniproject.nucleo.seguridad.repositorio.CorreoHabilitadoRepositorio;
import co.uniproject.nucleo.seguridad.repositorio.UsuarioRepositorio;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Bloque 41: el registro responde igual y cuesta un bcrypt en todos los rechazos. */
class ServicioCuentasRegistroTest {

    private static final String CLAVE = "ClavePrueba2026";
    private static final String EMAIL = "persona@u.edu.co";
    private static final Instant AHORA = Instant.parse("2026-09-01T12:00:00Z");

    private final AtomicInteger encodes = new AtomicInteger();
    private UsuarioRepositorio usuarios;
    private CorreoHabilitadoRepositorio correos;
    private ParticipacionCursos participacion;
    private ServicioCuentas servicio;
    private CodigosRecuperacion.Nuevo codigoValido;
    private int encodesTrasConstruir;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepositorio.class);
        correos = mock(CorreoHabilitadoRepositorio.class);
        participacion = mock(ParticipacionCursos.class);
        codigoValido = CodigosRecuperacion.nuevo();
        when(correos.bloquearPendientes(EMAIL))
                .thenReturn(List.of(habilitacionConCodigo(AHORA.plus(Duration.ofDays(1)))));
        PasswordEncoder bcrypt = new BCryptPasswordEncoder(4);
        PasswordEncoder contador = new PasswordEncoder() {
            @Override
            public String encode(CharSequence cruda) {
                encodes.incrementAndGet();
                return bcrypt.encode(cruda);
            }

            @Override
            public boolean matches(CharSequence cruda, String codificada) {
                return bcrypt.matches(cruda, codificada);
            }
        };
        servicio = new ServicioCuentas(
                usuarios,
                correos,
                mock(MonitorProyectoRepositorio.class),
                participacion,
                contador,
                mock(RegistroAuditoria.class),
                Clock.fixed(AHORA, ZoneOffset.UTC));
        encodesTrasConstruir = encodes.get();
    }

    private SolicitudRegistro solicitud() {
        return solicitudCon(EMAIL, codigoValido.texto());
    }

    private static SolicitudRegistro solicitudCon(String email, String codigo) {
        return new SolicitudRegistro("Persona", email, CLAVE, true, codigo);
    }

    private static CorreoHabilitado habilitacion(RolPlataforma rol) {
        CorreoHabilitado c = new CorreoHabilitado();
        c.setEmail(EMAIL);
        c.setRolPlataforma(rol);
        return c;
    }

    private CorreoHabilitado habilitacionConCodigo(Instant venceEn) {
        CorreoHabilitado c = habilitacion(RolPlataforma.ESTUDIANTE);
        c.setEnlaceSelector(codigoValido.selector());
        c.setEnlaceVerificadorHash(codigoValido.hashVerificador());
        c.setEnlaceVenceEn(venceEn);
        return c;
    }

    private static String otroVerificador(String codigo) {
        byte[] bytes = Base64.getUrlDecoder().decode(codigo);
        bytes[bytes.length - 1] ^= 0x01;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void exigirRechazoUniforme() {
        exigirRechazoUniforme(solicitud());
    }

    private void exigirRechazoUniforme(SolicitudRegistro peticion) {
        assertThatThrownBy(() -> servicio.registrar(peticion))
                .isInstanceOfSatisfying(ReglaNegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ServicioCuentas.CODIGO_REGISTRO_NO_DISPONIBLE);
                    assertThat(e.getMessage()).isEqualTo(ServicioCuentas.MENSAJE_REGISTRO_NO_DISPONIBLE);
                });
        assertThat(encodes.get()).isEqualTo(encodesTrasConstruir + 1);
    }

    @Test
    void rechazosIgualesYConUnBcrypt() {
        // (a) el correo ya tiene cuenta
        when(usuarios.existsByEmail(EMAIL)).thenReturn(true);
        exigirRechazoUniforme();
        verify(usuarios, never()).saveAndFlush(any());

        // (b) sin habilitaciones
        encodesTrasConstruir = encodes.get();
        when(usuarios.existsByEmail(EMAIL)).thenReturn(false);
        when(correos.bloquearPendientes(EMAIL)).thenReturn(List.of());
        exigirRechazoUniforme();
        verify(usuarios, never()).saveAndFlush(any());

        // (c) habilitaciones con roles distintos
        encodesTrasConstruir = encodes.get();
        when(correos.bloquearPendientes(EMAIL))
                .thenReturn(List.of(
                        habilitacionConCodigo(AHORA.plus(Duration.ofDays(1))), habilitacion(RolPlataforma.MONITOR)));
        exigirRechazoUniforme();
        verify(usuarios, never()).saveAndFlush(any());

        // (d) habilitación válida pero la BD rechaza el alta
        encodesTrasConstruir = encodes.get();
        when(correos.bloquearPendientes(EMAIL))
                .thenReturn(List.of(habilitacionConCodigo(AHORA.plus(Duration.ofDays(1)))));
        when(usuarios.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("duplicado"));
        exigirRechazoUniforme();
        verify(participacion, never()).incorporar(anyLong(), anyLong(), anyLong());

        clearInvocations(usuarios);

        // (e) código con formato inválido
        encodesTrasConstruir = encodes.get();
        exigirRechazoUniforme(solicitudCon(EMAIL, "no-es-un-codigo-valido"));
        verify(usuarios, never()).saveAndFlush(any());

        // (f) selector desconocido: ninguna pendiente del correo lo tiene
        encodesTrasConstruir = encodes.get();
        when(correos.bloquearPendientes(EMAIL)).thenReturn(List.of(habilitacion(RolPlataforma.ESTUDIANTE)));
        exigirRechazoUniforme();
        verify(usuarios, never()).saveAndFlush(any());

        // (g) verificador que no coincide
        encodesTrasConstruir = encodes.get();
        when(correos.bloquearPendientes(EMAIL))
                .thenReturn(List.of(habilitacionConCodigo(AHORA.plus(Duration.ofDays(1)))));
        exigirRechazoUniforme(solicitudCon(EMAIL, otroVerificador(codigoValido.texto())));
        verify(usuarios, never()).saveAndFlush(any());

        // (h) correo distinto al de la habilitación del código
        encodesTrasConstruir = encodes.get();
        exigirRechazoUniforme(solicitudCon("otro@u.edu.co", codigoValido.texto()));
        verify(usuarios, never()).saveAndFlush(any());

        // (i) código vencido
        encodesTrasConstruir = encodes.get();
        when(correos.bloquearPendientes(EMAIL)).thenReturn(List.of(habilitacionConCodigo(AHORA.minusSeconds(1))));
        exigirRechazoUniforme();
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void conflictoDeCandadosDaElMismoRechazoUniforme() {
        doThrow(new CannotAcquireLockException("candado")).when(correos).bloquearPendientes(EMAIL);
        exigirRechazoUniforme();
        encodesTrasConstruir = encodes.get();
        doThrow(new DeadlockLoserDataAccessException("interbloqueo", null))
                .when(correos)
                .bloquearPendientes(EMAIL);
        exigirRechazoUniforme();
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void bloqueaLasPendientesDelCorreoYNoBuscaElSelectorPorSeparado() {
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setIdUsuario(42L);
            return u;
        });

        assertThat(servicio.registrar(solicitud()).idUsuario()).isEqualTo(42L);

        verify(correos).bloquearPendientes(EMAIL);
    }

    @Test
    void incorporaAlEstudianteEnSusCursosEnOrdenAscendenteYNadaSinCurso() {
        CorreoHabilitado cursoAlto = habilitacionConCodigo(AHORA.plus(Duration.ofDays(1)));
        cursoAlto.setIdCorreoHabilitado(7L);
        cursoAlto.setIdCurso(8L);
        CorreoHabilitado cursoBajo = habilitacion(RolPlataforma.ESTUDIANTE);
        cursoBajo.setIdCorreoHabilitado(8L);
        cursoBajo.setIdCurso(3L);
        when(correos.bloquearPendientes(EMAIL)).thenReturn(List.of(cursoAlto, cursoBajo));
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setIdUsuario(42L);
            return u;
        });

        UsuarioSesion sesion = servicio.registrar(solicitud());

        assertThat(sesion.idUsuario()).isEqualTo(42L);
        var orden = inOrder(correos, participacion);
        orden.verify(correos).saveAll(List.of(cursoAlto, cursoBajo));
        orden.verify(participacion).bloquearCurso(3L);
        orden.verify(participacion).bloquearCurso(8L);
        orden.verify(participacion).incorporar(42L, 3L, 42L);
        orden.verify(participacion).incorporar(42L, 8L, 42L);

        clearInvocations(participacion);
        CorreoHabilitado monitor = habilitacion(RolPlataforma.MONITOR);
        monitor.setIdProyecto(5L);
        monitor.setEnlaceSelector(codigoValido.selector());
        monitor.setEnlaceVerificadorHash(codigoValido.hashVerificador());
        monitor.setEnlaceVenceEn(AHORA.plus(Duration.ofDays(1)));
        when(correos.bloquearPendientes(EMAIL)).thenReturn(List.of(monitor));

        servicio.registrar(solicitud());

        verify(participacion, never()).incorporar(anyLong(), anyLong(), anyLong());
    }

    @Test
    void contrasenaInvalidaNoHasheaNiMiraElCorreo() {
        SolicitudRegistro corta = new SolicitudRegistro("Persona", EMAIL, "123456789", true, codigoValido.texto());

        assertThatThrownBy(() -> servicio.registrar(corta))
                .isInstanceOfSatisfying(
                        SolicitudInvalidaException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CONTRASENA_INVALIDA"));

        assertThat(encodes.get()).isEqualTo(encodesTrasConstruir);
        verify(usuarios, never()).existsByEmail(any());
    }
}
