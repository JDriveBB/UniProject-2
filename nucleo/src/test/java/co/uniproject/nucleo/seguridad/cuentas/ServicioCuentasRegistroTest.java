package co.uniproject.nucleo.seguridad.cuentas;

import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.configuracionacademica.repositorio.MonitorProyectoRepositorio;
import co.uniproject.nucleo.seguridad.dominio.CorreoHabilitado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import co.uniproject.nucleo.seguridad.dominio.Usuario;
import co.uniproject.nucleo.seguridad.repositorio.CorreoHabilitadoRepositorio;
import co.uniproject.nucleo.seguridad.repositorio.UsuarioRepositorio;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Bloque 41: el registro responde igual y cuesta un bcrypt en todos los rechazos. */
class ServicioCuentasRegistroTest {

    private static final String CLAVE = "ClavePrueba2026";
    private static final String EMAIL = "persona@u.edu.co";

    private final AtomicInteger encodes = new AtomicInteger();
    private UsuarioRepositorio usuarios;
    private CorreoHabilitadoRepositorio correos;
    private ServicioCuentas servicio;
    private int encodesTrasConstruir;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepositorio.class);
        correos = mock(CorreoHabilitadoRepositorio.class);
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
        servicio = new ServicioCuentas(usuarios, correos, mock(MonitorProyectoRepositorio.class), contador,
                mock(RegistroAuditoria.class), Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC));
        encodesTrasConstruir = encodes.get();
    }

    private static SolicitudRegistro solicitud() {
        return new SolicitudRegistro("Persona", EMAIL, CLAVE, true);
    }

    private static CorreoHabilitado habilitacion(RolPlataforma rol) {
        CorreoHabilitado c = new CorreoHabilitado();
        c.setEmail(EMAIL);
        c.setRolPlataforma(rol);
        return c;
    }

    private void exigirRechazoUniforme() {
        assertThatThrownBy(() -> servicio.registrar(solicitud()))
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
        when(correos.findByEmailAndFechaUsoIsNull(EMAIL)).thenReturn(List.of());
        exigirRechazoUniforme();
        verify(usuarios, never()).saveAndFlush(any());

        // (c) habilitaciones con roles distintos
        encodesTrasConstruir = encodes.get();
        when(correos.findByEmailAndFechaUsoIsNull(EMAIL))
                .thenReturn(List.of(habilitacion(RolPlataforma.ESTUDIANTE), habilitacion(RolPlataforma.MONITOR)));
        exigirRechazoUniforme();
        verify(usuarios, never()).saveAndFlush(any());

        // (d) habilitación válida pero la BD rechaza el alta
        encodesTrasConstruir = encodes.get();
        when(correos.findByEmailAndFechaUsoIsNull(EMAIL)).thenReturn(List.of(habilitacion(RolPlataforma.ESTUDIANTE)));
        when(usuarios.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("duplicado"));
        exigirRechazoUniforme();
    }

    @Test
    void contrasenaInvalidaNoHasheaNiMiraElCorreo() {
        SolicitudRegistro corta = new SolicitudRegistro("Persona", EMAIL, "123456789", true);

        assertThatThrownBy(() -> servicio.registrar(corta))
                .isInstanceOfSatisfying(SolicitudInvalidaException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CONTRASENA_INVALIDA"));

        assertThat(encodes.get()).isEqualTo(encodesTrasConstruir);
        verify(usuarios, never()).existsByEmail(any());
    }
}
