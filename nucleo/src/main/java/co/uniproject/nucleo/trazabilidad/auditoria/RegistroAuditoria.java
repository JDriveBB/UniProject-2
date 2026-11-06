package co.uniproject.nucleo.trazabilidad.auditoria;

public interface RegistroAuditoria {

    /**
     * Inserta una entrada en log_auditoria (solo inserción, restricción 4e). Dentro de una transacción participa en
     * ella: si el cambio se deshace, la entrada también. Fuera de una transacción se confirma sola. Solo hechos: ids,
     * números, fechas y estados; nunca texto libre escrito por personas (para eso, registrarTexto).
     */
    void registrar(EntradaAuditoria entrada);

    /**
     * Bloque 29 (Ley 1581): texto libre escrito por personas (motivos, títulos, nombres, descripciones). En
     * log_auditoria queda el hecho (acción, entidad, id y campo) con los valores en NULL; valorAnterior y valorNuevo van
     * a texto_auditoria con el mismo id_log, que la supresión de su autor borra. Misma transacción que registrar.
     */
    void registrarTexto(EntradaAuditoria entrada);
}
