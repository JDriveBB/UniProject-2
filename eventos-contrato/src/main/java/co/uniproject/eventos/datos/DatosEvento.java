package co.uniproject.eventos.datos;

/** Contenido del campo data de un evento. Un record por tipo (plan sección 5.1). */
public sealed interface DatosEvento
        permits TareaRutaCriticaRiesgo, EquipoInactividad, HitoEnviado, HitoEvaluado, TareaReasignada, TareaCorreccion,
        RecuperacionSolicitada, ContrasenaCambiada {
}
