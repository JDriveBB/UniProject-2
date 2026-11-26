package co.uniproject.eventos;

import co.uniproject.eventos.datos.*;
import java.util.Arrays;
import java.util.Optional;

/** Los seis eventos de UniProject (plan sección 5), los dos de cuenta (bloque 28) y los siete de cursos y equipos (bloque 50). La clave es también la clave de enrutamiento en uniproject.eventos. */
public enum TipoEvento {
    TAREA_RUTA_CRITICA_RIESGO("tarea.ruta_critica.riesgo", TareaRutaCriticaRiesgo.class),
    EQUIPO_INACTIVIDAD("equipo.inactividad", EquipoInactividad.class),
    HITO_ENVIADO("hito.enviado", HitoEnviado.class),
    HITO_EVALUADO("hito.evaluado", HitoEvaluado.class),
    TAREA_REASIGNADA("tarea.reasignada", TareaReasignada.class),
    TAREA_CORRECCION("tarea.correccion", TareaCorreccion.class),
    CUENTA_RECUPERACION_SOLICITADA("cuenta.recuperacion.solicitada", RecuperacionSolicitada.class),
    CUENTA_CONTRASENA_CAMBIADA("cuenta.contrasena.cambiada", ContrasenaCambiada.class),
    CURSO_ESTUDIANTE_AGREGADO("curso.estudiante_agregado", CursoEstudianteAgregado.class),
    EQUIPO_VOTACION_ABIERTA("equipo.votacion_abierta", EquipoVotacionAbierta.class),
    EQUIPO_LIDER_ELEGIDO("equipo.lider_elegido", EquipoLiderElegido.class),
    EQUIPO_SIN_LIDER("equipo.sin_lider", EquipoSinLider.class),
    EQUIPO_MIEMBRO_CAMBIO("equipo.miembro_cambio", EquipoMiembroCambio.class),
    EQUIPO_INSCRIPCION_POR_CERRAR("equipo.inscripcion_por_cerrar", InscripcionPorCerrar.class),
    EQUIPO_INSCRIPCION_RESUMEN("equipo.inscripcion_resumen", InscripcionResumen.class);

    private final String clave;
    private final Class<? extends DatosEvento> tipoDatos;

    TipoEvento(String clave, Class<? extends DatosEvento> tipoDatos) {
        this.clave = clave;
        this.tipoDatos = tipoDatos;
    }

    public String clave() {
        return clave;
    }

    public Class<? extends DatosEvento> tipoDatos() {
        return tipoDatos;
    }

    /** Ruta del JSON Schema dentro del classpath, sin barra inicial. */
    public String rutaEsquema() {
        return "esquemas/" + clave + ".schema.json";
    }

    public static Optional<TipoEvento> desdeClave(String clave) {
        return Arrays.stream(values()).filter(t -> t.clave.equals(clave)).findFirst();
    }

    public static TipoEvento de(DatosEvento datos) {
        return switch (datos) {
            case TareaRutaCriticaRiesgo _ -> TAREA_RUTA_CRITICA_RIESGO;
            case EquipoInactividad _ -> EQUIPO_INACTIVIDAD;
            case HitoEnviado _ -> HITO_ENVIADO;
            case HitoEvaluado _ -> HITO_EVALUADO;
            case TareaReasignada _ -> TAREA_REASIGNADA;
            case TareaCorreccion _ -> TAREA_CORRECCION;
            case RecuperacionSolicitada _ -> CUENTA_RECUPERACION_SOLICITADA;
            case ContrasenaCambiada _ -> CUENTA_CONTRASENA_CAMBIADA;
            case CursoEstudianteAgregado _ -> CURSO_ESTUDIANTE_AGREGADO;
            case EquipoVotacionAbierta _ -> EQUIPO_VOTACION_ABIERTA;
            case EquipoLiderElegido _ -> EQUIPO_LIDER_ELEGIDO;
            case EquipoSinLider _ -> EQUIPO_SIN_LIDER;
            case EquipoMiembroCambio _ -> EQUIPO_MIEMBRO_CAMBIO;
            case InscripcionPorCerrar _ -> EQUIPO_INSCRIPCION_POR_CERRAR;
            case InscripcionResumen _ -> EQUIPO_INSCRIPCION_RESUMEN;
        };
    }
}
