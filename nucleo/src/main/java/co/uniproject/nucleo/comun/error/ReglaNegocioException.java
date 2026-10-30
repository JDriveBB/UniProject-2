package co.uniproject.nucleo.comun.error;

import java.util.List;

/** 409. Regla de negocio incumplida (p. ej. RF7: codigo PRERREQUISITOS_INCOMPLETOS, detalles = títulos de las tareas). */
public class ReglaNegocioException extends RuntimeException {

    private final String codigo;
    private final List<String> detalles;

    public ReglaNegocioException(String codigo, String mensaje) {
        this(codigo, mensaje, List.of());
    }

    public ReglaNegocioException(String codigo, String mensaje, List<String> detalles) {
        super(mensaje);
        this.codigo = codigo;
        this.detalles = List.copyOf(detalles);
    }

    public String getCodigo() {
        return codigo;
    }

    public List<String> getDetalles() {
        return detalles;
    }
}
