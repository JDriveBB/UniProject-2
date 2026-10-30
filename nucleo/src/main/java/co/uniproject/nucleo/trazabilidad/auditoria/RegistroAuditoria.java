package co.uniproject.nucleo.trazabilidad.auditoria;

public interface RegistroAuditoria {

    /**
     * Inserta una entrada en log_auditoria (solo inserción, restricción 4e). Dentro de una transacción participa en
     * ella: si el cambio se deshace, la entrada también. Fuera de una transacción se confirma sola.
     */
    void registrar(EntradaAuditoria entrada);
}
