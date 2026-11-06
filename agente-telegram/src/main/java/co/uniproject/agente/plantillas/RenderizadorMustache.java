package co.uniproject.agente.plantillas;

import co.uniproject.agente.destinatarios.Variante;
import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.EventoCloud;
import co.uniproject.eventos.RutasWeb;
import co.uniproject.eventos.Topologia;
import co.uniproject.eventos.datos.*;
import co.uniproject.eventos.interno.ContratoInterno;
import com.samskivert.mustache.Escapers;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class RenderizadorMustache implements RenderizadorAvisos {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
    private static final ZoneId ZONA = ZoneId.of(Topologia.ZONA);
    private static final String RECUPERACION = "cuenta.recuperacion.solicitada";
    private static final Pattern PATRON_RECUPERACION = Pattern.compile(ContratoInterno.PATRON_CODIGO_RECUPERACION);

    private static final String[] NOMBRES_PLANTILLAS = {
            "tarea.ruta_critica.riesgo",
            "equipo.inactividad",
            "hito.enviado",
            "hito.evaluado",
            "tarea.reasignada-nuevo",
            "tarea.reasignada-anterior",
            "tarea.correccion",
            "cuenta.recuperacion.solicitada",
            "cuenta.contrasena.cambiada"
    };

    private final CodecEventos codec;
    private final Map<String, Template> plantillas = new HashMap<>();

    public RenderizadorMustache(CodecEventos codec) {
        this.codec = codec;
        Mustache.Compiler compilador = Mustache.compiler()
                .withEscaper(Escapers.simple(new String[][] {
                        {"&", "&amp;"}, {"<", "&lt;"}, {">", "&gt;"}, {"\"", "&quot;"}}))
                .nullValue("");

        for (String nombre : NOMBRES_PLANTILLAS) {
            String ruta = "plantillas/" + nombre + ".mustache";
            InputStream in = getClass().getClassLoader().getResourceAsStream(ruta);
            if (in == null) {
                throw new IllegalStateException("Falta la plantilla " + nombre);
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Template template = compilador.compile(reader);
                plantillas.put(nombre, template);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer la plantilla " + nombre, e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Aviso renderizar(EventoCloud evento, Variante variante) {
        if (evento.data() instanceof RecuperacionSolicitada) {
            throw new PlantillaException("La plantilla " + RECUPERACION + " exige el código: usa renderizarRecuperacion");
        }
        String nombre = evento.type() + variante.sufijo();
        Template plantilla = plantillas.get(nombre);
        if (plantilla == null) {
            throw new PlantillaException("No hay plantilla " + nombre);
        }

        String html;
        try {
            Map<String, Object> modelo = new HashMap<>(codec.mapeador().convertValue(evento.data(), Map.class));
            switch (evento.data()) {
                case TareaRutaCriticaRiesgo r ->
                        modelo.put("fechaLimiteHitoTexto", r.fechaLimiteHito().format(FORMATO_FECHA));
                case EquipoInactividad i -> {
                    modelo.put("ultimaActividadTexto", FORMATO_FECHA.format(i.ultimaActividad().atZone(ZONA)));
                    modelo.put("periodoTexto", i.periodoDias() == 1 ? "1 día" : i.periodoDias() + " días");
                }
                case HitoEvaluado h -> {
                    modelo.put("caracterNotaTexto", h.caracterNota() == CaracterNota.PROVISIONAL ? "provisional" : "definitiva");
                    modelo.put("esProvisional", h.caracterNota() == CaracterNota.PROVISIONAL);
                }
                case ContrasenaCambiada c -> {
                    modelo.put("fechaTexto", FORMATO_FECHA_HORA.format(c.fecha().atZone(ZONA)));
                    modelo.put("origenTexto", c.origen() == OrigenCambioContrasena.PERFIL
                            ? "desde tu perfil" : "con un enlace de recuperación");
                }
                default -> {}
            }
            html = plantilla.execute(modelo).replace("\r\n", "\n").strip();
        } catch (RuntimeException e) {
            throw new PlantillaException("La plantilla " + nombre + " falló", e);
        }

        String textoBoton;
        String ruta;
        switch (nombre) {
            case "tarea.ruta_critica.riesgo" -> {
                textoBoton = "Ver tarea";
                ruta = RutasWeb.tarea(((TareaRutaCriticaRiesgo) evento.data()).tarea().id());
            }
            case "equipo.inactividad" -> {
                textoBoton = "Ver tablero del equipo";
                ruta = RutasWeb.tableroEquipo(((EquipoInactividad) evento.data()).equipo().id());
            }
            case "hito.enviado" -> {
                textoBoton = "Evaluar hito";
                ruta = RutasWeb.evaluarHito(((HitoEnviado) evento.data()).hito().id());
            }
            case "hito.evaluado" -> {
                textoBoton = "Ver evaluación";
                ruta = RutasWeb.evaluacionHito(((HitoEvaluado) evento.data()).hito().id());
            }
            case "tarea.reasignada-nuevo" -> {
                textoBoton = "Ver tarea";
                ruta = RutasWeb.tarea(((TareaReasignada) evento.data()).tarea().id());
            }
            case "tarea.reasignada-anterior" -> {
                textoBoton = "Ver mis tareas";
                ruta = RutasWeb.misTareas();
            }
            case "tarea.correccion" -> {
                textoBoton = "Ver tarea";
                ruta = RutasWeb.tarea(((TareaCorreccion) evento.data()).tarea().id());
            }
            case "cuenta.contrasena.cambiada" -> {
                textoBoton = "Ir a UniProject";
                ruta = RutasWeb.ingresar();
            }
            default -> throw new PlantillaException("No hay plantilla " + nombre);
        }

        return new Aviso(html, textoBoton, ruta);
    }

    /** Modelo mínimo: solo el código y la hora de vencimiento (sin nombre ni chat). */
    @Override
    public Aviso renderizarRecuperacion(EventoCloud evento, String codigo) {
        if (!(evento.data() instanceof RecuperacionSolicitada r)) {
            throw new PlantillaException("El evento no es " + RECUPERACION);
        }
        if (codigo == null || !PATRON_RECUPERACION.matcher(codigo).matches()) {
            throw new PlantillaException("Código de recuperación con formato inválido");
        }
        String html;
        try {
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("codigo", codigo);
            modelo.put("venceEnTexto", FORMATO_HORA.format(r.venceEn().atZone(ZONA)));
            html = plantillas.get(RECUPERACION).execute(modelo).replace("\r\n", "\n").strip();
        } catch (RuntimeException e) {
            throw new PlantillaException("La plantilla " + RECUPERACION + " falló", e);
        }
        return new Aviso(html, "Restablecer contraseña", RutasWeb.restablecerContrasena(codigo));
    }
}
