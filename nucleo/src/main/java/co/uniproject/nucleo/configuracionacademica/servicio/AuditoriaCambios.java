package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.math.BigDecimal;
import java.util.Objects;

/** Un renglón ACTUALIZAR por campo que cambió (valores como texto). Nunca se le pasan correos ni contraseñas. */
public final class AuditoriaCambios {

    private AuditoriaCambios() {
    }

    public static void siCambia(RegistroAuditoria auditoria, long idUsuario, String entidad, long idRegistro, String campo,
                         Object anterior, Object nuevo) {
        String textoAnterior = texto(anterior);
        String textoNuevo = texto(nuevo);
        if (!Objects.equals(textoAnterior, textoNuevo)) {
            auditoria.registrar(EntradaAuditoria.cambio(idUsuario, entidad, idRegistro, AccionAuditoria.ACTUALIZAR,
                    campo, textoAnterior, textoNuevo));
        }
    }

    /** BigDecimal sin ceros a la derecha (40.00 y 40 son el mismo valor); lo demás con toString. */
    public static String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return valor.toString();
    }
}
