package co.uniproject.agente.destinatarios;

/** Qué plantilla recibe cada destinatario. El nombre del archivo es clave + sufijo (plan §8.6). */
public enum Variante {
    UNICA(""), NUEVO("-nuevo"), ANTERIOR("-anterior");

    private final String sufijo;
    Variante(String sufijo) { this.sufijo = sufijo; }
    public String sufijo() { return sufijo; }
}
