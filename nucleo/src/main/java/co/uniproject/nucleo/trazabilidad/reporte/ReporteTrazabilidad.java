package co.uniproject.nucleo.trazabilidad.reporte;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Vista del reporte de trazabilidad (RF15). No es una tabla: la arma el servicio de trazabilidad con log_auditoria,
 * envio_hito, evaluacion_hito y nota_individual. Solo nombres: nunca correos, chat_id ni contraseñas (Ley 1581).
 */
public record ReporteTrazabilidad(String proyecto, String equipo, String docente, Instant generadoEn,
                                  List<Integrante> integrantes, List<Cambio> cambios, List<Entrega> entregas,
                                  List<Calificacion> calificaciones) {

    public ReporteTrazabilidad {
        integrantes = List.copyOf(integrantes);
        cambios = List.copyOf(cambios);
        entregas = List.copyOf(entregas);
        calificaciones = List.copyOf(calificaciones);
    }

    public record Integrante(String nombre, String rol) {}

    /** detalle: texto ya redactado por el servicio, p. ej. "estado: EN_PROGRESO -> COMPLETADA". */
    public record Cambio(Instant fechaHora, String autor, String accion, String detalle) {}

    public record Entrega(String hito, Instant fechaEnvio, String enviadoPor) {}

    /** caracter: "provisional" o "definitiva" (decisión E1). */
    public record Calificacion(String hito, BigDecimal notaGrupal, String caracter, List<NotaIndividual> notas) {

        public Calificacion {
            notas = List.copyOf(notas);
        }
    }

    /** notaFinal nula: el docente aún no la confirma. */
    public record NotaIndividual(String estudiante, BigDecimal notaSugerida, BigDecimal notaFinal) {}
}
