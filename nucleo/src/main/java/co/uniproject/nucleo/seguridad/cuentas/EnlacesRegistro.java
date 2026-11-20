package co.uniproject.nucleo.seguridad.cuentas;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.seguridad.dominio.CorreoHabilitado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import co.uniproject.nucleo.seguridad.repositorio.CorreoHabilitadoRepositorio;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enlaces de invitación de un solo uso para crear cuenta (T-040, D-15 5A). Generar un enlace nuevo sobrescribe el
 * anterior de esa habilitación; el servidor solo guarda selector + SHA-256 del verificador.
 */
@Service
public class EnlacesRegistro {

    public static final Duration VIGENCIA = Duration.ofDays(30);
    public static final String CODIGO_HABILITACION_USADA = "HABILITACION_USADA";
    public static final String MENSAJE_HABILITACION_USADA =
            "Esa persona ya creó su cuenta. Quítala de su equipo o de los monitores del proyecto";

    private final CorreoHabilitadoRepositorio correos;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;

    public EnlacesRegistro(CorreoHabilitadoRepositorio correos, RegistroAuditoria auditoria, Clock reloj) {
        this.correos = correos;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    /** Para una habilitación de estudiante o monitor de un proyecto (el llamador ya verificó el permiso del docente). */
    @Transactional
    public EnlaceRegistroVista generarParaProyecto(long idDocente, long idProyecto, long idCorreoHabilitado) {
        return generar(
                idDocente,
                idCorreoHabilitado,
                c -> Objects.equals(c.getIdProyecto(), idProyecto),
                "Esa habilitación no existe en el proyecto");
    }

    /** Para una habilitación de docente sin proyecto (el llamador ya verificó que es ADMINISTRADOR). */
    @Transactional
    public EnlaceRegistroVista generarParaDocente(long idAdministrador, long idCorreoHabilitado) {
        return generar(
                idAdministrador,
                idCorreoHabilitado,
                c -> c.getRolPlataforma() == RolPlataforma.DOCENTE && c.getIdProyecto() == null,
                "Esa habilitación de docente no existe");
    }

    private EnlaceRegistroVista generar(
            long idUsuarioAccion,
            long idCorreoHabilitado,
            Predicate<CorreoHabilitado> perteneceAlAmbito,
            String mensajeNoEncontrado) {
        CorreoHabilitado c = correos.findById(idCorreoHabilitado)
                .filter(perteneceAlAmbito)
                .orElseThrow(() -> new RecursoNoEncontradoException(mensajeNoEncontrado));
        if (c.getFechaUso() != null) {
            throw new ReglaNegocioException(CODIGO_HABILITACION_USADA, MENSAJE_HABILITACION_USADA);
        }
        CodigosRecuperacion.Nuevo nuevo = CodigosRecuperacion.nuevo();
        Instant venceEn = Instant.now(reloj).plus(VIGENCIA);
        c.setEnlaceSelector(nuevo.selector());
        c.setEnlaceVerificadorHash(nuevo.hashVerificador());
        c.setEnlaceVenceEn(venceEn);
        correos.saveAndFlush(c);
        auditoria.registrar(EntradaAuditoria.de(
                idUsuarioAccion,
                "correo_habilitado",
                c.getIdCorreoHabilitado(),
                AccionAuditoria.GENERAR_ENLACE_REGISTRO));
        return new EnlaceRegistroVista(nuevo.texto(), venceEn);
    }
}
