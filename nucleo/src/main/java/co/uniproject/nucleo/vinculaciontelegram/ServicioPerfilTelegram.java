package co.uniproject.nucleo.vinculaciontelegram;

import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que el usuario hace desde su perfil (D5, D7). La vinculación termina en Telegram (/interno, plan §8.8); aquí se
 * audita la petición del código (nunca el código) y la desvinculación (restricción 4e, Ley 1581).
 */
@Service
public class ServicioPerfilTelegram {

    private final ServicioVinculacion vinculacion;
    private final RegistroAuditoria auditoria;

    public ServicioPerfilTelegram(ServicioVinculacion vinculacion, RegistroAuditoria auditoria) {
        this.vinculacion = vinculacion;
        this.auditoria = auditoria;
    }

    @Transactional
    public CodigoGenerado generarCodigo(long idUsuario) {
        CodigoGenerado codigo;
        try {
            codigo = vinculacion.generarCodigo(idUsuario);
        } catch (IllegalStateException e) {
            throw new ReglaNegocioException("TELEGRAM_NO_CONFIGURADO",
                    "El bot de Telegram no está configurado. Avísale al administrador");
        }
        auditoria.registrar(EntradaAuditoria.cambio(idUsuario, "usuario", idUsuario, AccionAuditoria.VINCULAR_TELEGRAM,
                "codigo_vinculacion", null, "generado"));
        return codigo;
    }

    @Transactional
    public void desvincular(long idUsuario) {
        vinculacion.desvincular(idUsuario);
        auditoria.registrar(EntradaAuditoria.de(idUsuario, "usuario", idUsuario, AccionAuditoria.DESVINCULAR_TELEGRAM));
    }
}
