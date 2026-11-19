package co.uniproject.nucleo.trazabilidad.reporte;

import java.util.Map;
import java.util.function.Function;

/**
 * Traduce acciones y detalles de auditoría a español claro para el PDF del reporte (equivalente a reportes/vista.ts e
 * historial/vista.ts con visor docente). Usa " -> " porque Helvetica WinAnsi no codifica la flecha Unicode.
 */
public final class TextosHistorial {

    private static final String FLECHA = " -> ";
    private static final String SIN_ASIGNAR = "Sin asignar";
    private static final String EXINTEGRANTE = "Exintegrante";
    private static final String TAREA_ELIMINADA = "Tarea eliminada";
    private static final String PREFIJO_NOTA_FINAL = "nota_final:";

    private static final Map<String, String> ACCIONES = Map.ofEntries(
            Map.entry("LOGIN_EXITOSO", "Ingresó al sistema"),
            Map.entry("LOGIN_FALLIDO", "Intento de ingreso fallido"),
            Map.entry("LOGOUT", "Salió del sistema"),
            Map.entry("ACCESO_DENEGADO", "Acceso denegado"),
            Map.entry("REGISTRO_CUENTA", "Creó su cuenta"),
            Map.entry("HABILITAR_CORREO", "Habilitó un correo"),
            Map.entry("CREAR", "Creó"),
            Map.entry("ACTUALIZAR", "Modificó"),
            Map.entry("ELIMINAR", "Eliminó"),
            Map.entry("CAMBIAR_ESTADO", "Cambió el estado"),
            Map.entry("ACTUALIZAR_AVANCE", "Actualizó el avance"),
            Map.entry("ADJUNTAR_EVIDENCIA", "Adjuntó una evidencia"),
            Map.entry("ASIGNAR_TAREA", "Asignó la tarea"),
            Map.entry("REASIGNAR_TAREA", "Reasignó la tarea"),
            Map.entry("ESTABLECER_DEPENDENCIA", "Definió dependencias"),
            Map.entry("ENVIAR_HITO", "Envió el hito a revisión"),
            Map.entry("EVALUAR_HITO", "Evaluó el hito"),
            Map.entry("CONFIRMAR_NOTA", "Confirmó una nota"),
            Map.entry("VINCULAR_TELEGRAM", "Vinculó Telegram"),
            Map.entry("DESVINCULAR_TELEGRAM", "Desvinculó Telegram"),
            Map.entry("EXPORTAR_REPORTE", "Exportó el reporte"),
            Map.entry("SUPRIMIR_CUENTA", "Suprimió su cuenta"),
            Map.entry("SOLICITAR_RECUPERACION", "Pidió recuperar la contraseña"),
            Map.entry("RESTABLECER_CONTRASENA", "Restableció la contraseña"),
            Map.entry("CAMBIAR_CONTRASENA", "Cambió la contraseña"),
            Map.entry("SOLICITAR_APOYO", "Pidió apoyo"),
            Map.entry("SOLICITAR_CORRECCION", "Pidió una corrección"));

    private static final Map<String, String> ENTIDADES = Map.of(
            "equipo", "Equipo",
            "miembro_equipo", "Integrantes",
            "hito_equipo", "Hito",
            "tarea", "Tarea");

    private static final Map<String, String> ETIQUETAS_PERSONA = Map.of(
            "id_encargado", "Encargado",
            "id_usuario", "Integrante",
            "lider", "Líder");

    private static final Map<String, String> ESTADOS = Map.of(
            "PENDIENTE", "Pendiente",
            "EN_PROGRESO", "En progreso",
            "COMPLETADA", "Completada",
            "EN_CURSO", "En progreso",
            "ENVIADO", "En Revisión",
            "EVALUADO", "Evaluado");

    private static final Map<String, String> EVIDENCIAS = Map.of(
            "PDF", "PDF",
            "IMAGEN", "imagen",
            "ENLACE", "enlace");

    private TextosHistorial() {}

    /** Forma "<acción> · <entidad>" en español; los valores desconocidos quedan tal cual y los nulos como "". */
    public static String accion(String accion, String entidad) {
        String accionLegible = accion == null ? "" : ACCIONES.getOrDefault(accion, accion);
        String entidadLegible = entidad == null ? "" : ENTIDADES.getOrDefault(entidad, entidad);
        return accionLegible + " · " + entidadLegible;
    }

    /** Redacta el detalle en español para el PDF del docente; nunca devuelve nulo. */
    public static String detalle(
            String campo,
            String anterior,
            String nuevo,
            String detalleNucleo,
            Map<Long, String> nombres,
            Map<Long, String> tareas) {
        if (campo == null) {
            return ultimoRecurso(detalleNucleo);
        }
        String etiquetaPersona = ETIQUETAS_PERSONA.get(campo);
        if (etiquetaPersona != null) {
            return cambioPersona(etiquetaPersona, anterior, nuevo, detalleNucleo, nombres);
        }
        if (campo.startsWith(PREFIJO_NOTA_FINAL)) {
            String id = campo.substring(PREFIJO_NOTA_FINAL.length());
            return cambioDesde(
                    "Nota final de " + persona(id, nombres), anterior, nuevo, detalleNucleo, TextosHistorial::nota);
        }
        return switch (campo) {
            case "nota_grupal" -> cambioDesde("Nota grupal", anterior, nuevo, detalleNucleo, TextosHistorial::nota);
            case "solicitud_apoyo", "solicitud_correccion", "tarea_parcial" -> "";
            case "estado" -> cambioDesde("Estado", anterior, nuevo, detalleNucleo, TextosHistorial::textoEstado);
            case "porcentaje_avance" -> cambioDesde("Avance", anterior, nuevo, detalleNucleo, v -> v + " %");
            case "prerrequisito" -> detallePrerrequisito(anterior, nuevo, detalleNucleo, tareas);
            case "evidencia" -> detalleEvidencia(nuevo, detalleNucleo);
            default -> ultimoRecurso(detalleNucleo);
        };
    }

    private static String cambioPersona(
            String etiqueta, String anterior, String nuevo, String detalleNucleo, Map<Long, String> nombres) {
        if (anterior != null && nuevo != null) {
            return etiqueta + ": " + persona(anterior, nombres) + FLECHA + persona(nuevo, nombres);
        }
        if (nuevo != null) {
            return etiqueta + ": " + persona(nuevo, nombres);
        }
        if (anterior != null) {
            return etiqueta + ": " + persona(anterior, nombres);
        }
        return ultimoRecurso(detalleNucleo);
    }

    private static String cambioDesde(
            String etiqueta,
            String anterior,
            String nuevo,
            String detalleNucleo,
            Function<String, String> formateador) {
        if (nuevo == null) {
            return ultimoRecurso(detalleNucleo);
        }
        if (anterior != null) {
            return etiqueta + ": " + formateador.apply(anterior) + FLECHA + formateador.apply(nuevo);
        }
        return etiqueta + ": " + formateador.apply(nuevo);
    }

    private static String detallePrerrequisito(
            String anterior, String nuevo, String detalleNucleo, Map<Long, String> tareas) {
        if (nuevo != null) {
            return "Prerrequisito agregado: " + nombreTarea(nuevo, tareas);
        }
        if (anterior != null) {
            return "Prerrequisito quitado: " + nombreTarea(anterior, tareas);
        }
        return ultimoRecurso(detalleNucleo);
    }

    private static String detalleEvidencia(String nuevo, String detalleNucleo) {
        if (nuevo == null) {
            return ultimoRecurso(detalleNucleo);
        }
        String limpio = nuevo.strip();
        if (limpio.isEmpty()) {
            return ultimoRecurso(detalleNucleo);
        }
        String primera = limpio.split("\\s+", 2)[0];
        String tipo = EVIDENCIAS.get(primera);
        return tipo != null ? "Evidencia: " + tipo : ultimoRecurso(detalleNucleo);
    }

    private static String persona(String valor, Map<Long, String> nombres) {
        Long id = parsearId(valor);
        if (id == null) {
            return SIN_ASIGNAR;
        }
        String nombre = nombres == null ? null : nombres.get(id);
        return nombre != null ? nombre : EXINTEGRANTE;
    }

    private static String nombreTarea(String valor, Map<Long, String> tareas) {
        Long id = parsearId(valor);
        String titulo = id == null || tareas == null ? null : tareas.get(id);
        return titulo != null ? titulo : TAREA_ELIMINADA;
    }

    private static Long parsearId(String valor) {
        if (valor == null || valor.isEmpty()) {
            return null;
        }
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c < '0' || c > '9') {
                return null;
            }
        }
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String nota(String valor) {
        return valor.replace('.', ',');
    }

    private static String textoEstado(String valor) {
        return ESTADOS.getOrDefault(valor, valor);
    }

    private static String ultimoRecurso(String detalleNucleo) {
        return detalleNucleo == null ? "" : detalleNucleo;
    }
}
