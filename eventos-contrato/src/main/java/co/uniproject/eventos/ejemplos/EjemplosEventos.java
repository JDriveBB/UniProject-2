package co.uniproject.eventos.ejemplos;

import co.uniproject.eventos.TipoEvento;
import co.uniproject.eventos.datos.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Ejemplos fijos de cada evento, para pruebas y demostraciones. No usar en código de producción. */
public final class EjemplosEventos {
    private EjemplosEventos() {}

    public static final Instant FECHA = Instant.parse("2026-10-24T19:05:00Z");
    public static final Referencia PROYECTO = new Referencia(12, "Proyecto final");
    public static final Referencia EQUIPO = new Referencia(4, "Equipo 4");
    public static final Referencia HITO = new Referencia(7, "Entrega de diagramas");
    public static final ReferenciaTarea TAREA = new ReferenciaTarea(45, "Diagramas de secuencia");
    public static final Persona LIDER_C = new Persona(100, "Estudiante C", 900000100L);
    public static final Persona INTEGRANTE_A = new Persona(101, "Estudiante A", 900000101L);
    public static final Persona INTEGRANTE_B_SIN_TELEGRAM = new Persona(102, "Estudiante B", null);
    public static final Persona INTEGRANTE_E = new Persona(103, "Estudiante E", 900000103L);
    public static final Persona DOCENTE = new Persona(200, "Docente D", 900000200L);

    public static TareaRutaCriticaRiesgo tareaRutaCriticaRiesgo() {
        return new TareaRutaCriticaRiesgo(PROYECTO, EQUIPO, TAREA, new Referencia(101, "Estudiante A"), 40, HITO,
                LocalDate.of(2026, 11, 5), List.of(LIDER_C, INTEGRANTE_A, INTEGRANTE_B_SIN_TELEGRAM), DOCENTE);
    }

    public static EquipoInactividad equipoInactividad(boolean notificarDocente) {
        return new EquipoInactividad(PROYECTO, EQUIPO, new Referencia(102, "Estudiante B"),
                Instant.parse("2026-10-20T14:30:00Z"), 3, LIDER_C, DOCENTE, notificarDocente);
    }

    public static HitoEnviado hitoEnviado() {
        return new HitoEnviado(PROYECTO, EQUIPO, HITO, FECHA, new Referencia(100, "Estudiante C"), DOCENTE);
    }

    public static HitoEvaluado hitoEvaluado(CaracterNota caracterNota) {
        return new HitoEvaluado(PROYECTO, EQUIPO, HITO, caracterNota,
                List.of(LIDER_C, INTEGRANTE_A, INTEGRANTE_B_SIN_TELEGRAM));
    }

    public static TareaReasignada tareaReasignada() {
        return new TareaReasignada(PROYECTO, EQUIPO, TAREA, INTEGRANTE_A, INTEGRANTE_E,
                new Referencia(100, "Estudiante C"), FECHA);
    }

    public static TareaCorreccion tareaCorreccion() {
        return new TareaCorreccion(PROYECTO, EQUIPO, TAREA, INTEGRANTE_A, new Referencia(100, "Estudiante C"),
                "Falta el caso \"sin conexión\" & revisar <notas>");
    }

    /** Ejemplo por defecto de cada tipo (inactividad sin docente; hito evaluado provisional). */
    public static DatosEvento de(TipoEvento tipo) {
        return switch (tipo) {
            case TAREA_RUTA_CRITICA_RIESGO -> tareaRutaCriticaRiesgo();
            case EQUIPO_INACTIVIDAD -> equipoInactividad(false);
            case HITO_ENVIADO -> hitoEnviado();
            case HITO_EVALUADO -> hitoEvaluado(CaracterNota.PROVISIONAL);
            case TAREA_REASIGNADA -> tareaReasignada();
            case TAREA_CORRECCION -> tareaCorreccion();
        };
    }
}
