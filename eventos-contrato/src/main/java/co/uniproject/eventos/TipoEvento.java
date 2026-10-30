package co.uniproject.eventos;

import co.uniproject.eventos.datos.*;
import java.util.Arrays;
import java.util.Optional;

/** Los seis eventos de UniProject (plan §5). La clave es también la clave de enrutamiento en uniproject.eventos. */
public enum TipoEvento {
    TAREA_RUTA_CRITICA_RIESGO("tarea.ruta_critica.riesgo", TareaRutaCriticaRiesgo.class),
    EQUIPO_INACTIVIDAD("equipo.inactividad", EquipoInactividad.class),
    HITO_ENVIADO("hito.enviado", HitoEnviado.class),
    HITO_EVALUADO("hito.evaluado", HitoEvaluado.class),
    TAREA_REASIGNADA("tarea.reasignada", TareaReasignada.class),
    TAREA_CORRECCION("tarea.correccion", TareaCorreccion.class);

    private final String clave;
    private final Class<? extends DatosEvento> tipoDatos;

    TipoEvento(String clave, Class<? extends DatosEvento> tipoDatos) {
        this.clave = clave;
        this.tipoDatos = tipoDatos;
    }

    public String clave() { return clave; }

    public Class<? extends DatosEvento> tipoDatos() { return tipoDatos; }

    /** Ruta del JSON Schema dentro del classpath, sin barra inicial. */
    public String rutaEsquema() { return "esquemas/" + clave + ".schema.json"; }

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
        };
    }
}
