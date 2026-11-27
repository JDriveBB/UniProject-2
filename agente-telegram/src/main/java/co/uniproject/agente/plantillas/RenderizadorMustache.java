package co.uniproject.agente.plantillas;

import co.uniproject.agente.destinatarios.Variante;
import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.EventoCloud;
import co.uniproject.eventos.RutasWeb;
import co.uniproject.eventos.TextosEventos;
import co.uniproject.eventos.Topologia;
import co.uniproject.eventos.datos.*;
import co.uniproject.eventos.interno.ContratoInterno;
import com.samskivert.mustache.Escapers;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

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
        "cuenta.contrasena.cambiada",
        "curso.estudiante_agregado",
        "equipo.votacion_abierta",
        "equipo.lider_elegido",
        "equipo.sin_lider",
        "equipo.miembro_cambio",
        "equipo.miembro_cambio-nuevo",
        "equipo.inscripcion_por_cerrar",
        "equipo.inscripcion_resumen"
    };

    private final CodecEventos codec;
    private final Map<String, Template> plantillas = new HashMap<>();

    public RenderizadorMustache(CodecEventos codec) {
        this.codec = codec;
        Mustache.Compiler compilador = Mustache.compiler()
                .withEscaper(Escapers.simple(
                        new String[][] {{"&", "&amp;"}, {"<", "&lt;"}, {">", "&gt;"}, {"\"", "&quot;"}}))
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
            throw new PlantillaException(
                    "La plantilla " + RECUPERACION + " exige el código: usa renderizarRecuperacion");
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
                    modelo.put(
                            "ultimaActividadTexto",
                            FORMATO_FECHA.format(i.ultimaActividad().atZone(ZONA)));
                    modelo.put("periodoTexto", i.periodoDias() == 1 ? "1 día" : i.periodoDias() + " días");
                }
                case HitoEvaluado h -> {
                    modelo.put(
                            "caracterNotaTexto",
                            h.caracterNota() == CaracterNota.PROVISIONAL ? "provisional" : "definitiva");
                    modelo.put("esProvisional", h.caracterNota() == CaracterNota.PROVISIONAL);
                }
                case ContrasenaCambiada c -> {
                    modelo.put("fechaTexto", FORMATO_FECHA_HORA.format(c.fecha().atZone(ZONA)));
                    modelo.put(
                            "origenTexto",
                            c.origen() == OrigenCambioContrasena.PERFIL
                                    ? "desde tu perfil"
                                    : "con un enlace de recuperación");
                }
                case EquipoVotacionAbierta v ->
                    modelo.put(
                            "cierraEnTexto",
                            FORMATO_FECHA_HORA.format(v.cierraEn().atZone(ZONA)));
                case EquipoLiderElegido l ->
                    modelo.put(
                            "origenTexto",
                            l.origen() == OrigenLider.VOTACION
                                    ? "elegido por votación"
                                    : "elegido por el docente tras un empate");
                case EquipoSinLider s -> {
                    modelo.put("motivoTexto", s.motivo() == MotivoSinLider.EMPATE ? "en empate" : "sin votos");
                    modelo.put(
                            "accionTexto",
                            s.motivo() == MotivoSinLider.EMPATE
                                    ? "Elige al líder entre los empatados."
                                    : "Nombra un líder o abre otra votación.");
                }
                case EquipoMiembroCambio m -> {
                    modelo.put(
                            "personasTexto",
                            TextosEventos.listaNombres(
                                    m.personas().stream().map(Persona::nombre).toList()));
                    modelo.put(
                            "verboTexto",
                            TextosEventos.verbo(m.movimiento(), m.personas().size()));
                }
                case InscripcionPorCerrar i ->
                    modelo.put(
                            "cierreTexto", FORMATO_FECHA_HORA.format(i.cierre().atZone(ZONA)));
                case InscripcionResumen r -> {
                    modelo.put(
                            "cierreTexto", FORMATO_FECHA_HORA.format(r.cierre().atZone(ZONA)));
                    modelo.put("sinEquipoTexto", TextosEventos.cantidad(r.sinEquipo(), "estudiante", "estudiantes"));
                    modelo.put(
                            "cuposLibresTexto", TextosEventos.cantidad(r.cuposLibres(), "cupo libre", "cupos libres"));
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
                ruta = RutasWeb.tarea(
                        ((TareaRutaCriticaRiesgo) evento.data()).tarea().id());
            }
            case "equipo.inactividad" -> {
                textoBoton = "Ver tablero del equipo";
                ruta = RutasWeb.tableroEquipo(
                        ((EquipoInactividad) evento.data()).equipo().id());
            }
            case "hito.enviado" -> {
                textoBoton = "Evaluar hito";
                ruta = RutasWeb.evaluarHito(((HitoEnviado) evento.data()).hito().id());
            }
            case "hito.evaluado" -> {
                textoBoton = "Ver evaluación";
                ruta = RutasWeb.evaluacionHito(
                        ((HitoEvaluado) evento.data()).hito().id());
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
            case "curso.estudiante_agregado" -> {
                textoBoton = "Ver mis cursos";
                ruta = RutasWeb.misCursos();
            }
            case "equipo.votacion_abierta" -> {
                textoBoton = "Votar";
                ruta = RutasWeb.votacionEquipo(
                        ((EquipoVotacionAbierta) evento.data()).equipo().id());
            }
            case "equipo.lider_elegido" -> {
                textoBoton = "Ver votación";
                ruta = RutasWeb.votacionEquipo(
                        ((EquipoLiderElegido) evento.data()).equipo().id());
            }
            case "equipo.sin_lider" -> {
                textoBoton = "Ver votación";
                ruta = RutasWeb.votacionEquipo(
                        ((EquipoSinLider) evento.data()).equipo().id());
            }
            case "equipo.miembro_cambio", "equipo.miembro_cambio-nuevo" -> {
                textoBoton = "Ver tablero del equipo";
                ruta = RutasWeb.tableroEquipo(
                        ((EquipoMiembroCambio) evento.data()).equipo().id());
            }
            case "equipo.inscripcion_por_cerrar" -> {
                textoBoton = "Elegir equipo";
                ruta = RutasWeb.inscripcionProyecto(
                        ((InscripcionPorCerrar) evento.data()).proyecto().id());
            }
            case "equipo.inscripcion_resumen" -> {
                textoBoton = "Ver equipos del proyecto";
                ruta = RutasWeb.proyecto(
                        ((InscripcionResumen) evento.data()).proyecto().id());
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
            html = plantillas
                    .get(RECUPERACION)
                    .execute(modelo)
                    .replace("\r\n", "\n")
                    .strip();
        } catch (RuntimeException e) {
            throw new PlantillaException("La plantilla " + RECUPERACION + " falló", e);
        }
        return new Aviso(html, "Restablecer contraseña", RutasWeb.restablecerContrasena(codigo));
    }
}
