package co.uniproject.nucleo.notificaciones.programador;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class EstadoAlertasJdbc implements EstadoAlertas {

    private final JdbcClient jdbc;

    public EstadoAlertasJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Boolean> riesgoAnterior(long tareaId) {
        return jdbc.sql("SELECT en_riesgo FROM estado_riesgo_tarea WHERE tarea_id = :tareaId")
                .param("tareaId", tareaId)
                .query(Boolean.class)
                .optional();
    }

    @Override
    public void guardarRiesgo(long tareaId, boolean enRiesgo, Instant ahora) {
        jdbc.sql("INSERT INTO estado_riesgo_tarea (tarea_id, en_riesgo, cambiado_en) VALUES (:tareaId, :enRiesgo, :ahora) ON CONFLICT (tarea_id) DO UPDATE SET en_riesgo = EXCLUDED.en_riesgo, cambiado_en = EXCLUDED.cambiado_en")
                .param("tareaId", tareaId)
                .param("enRiesgo", enRiesgo)
                .param("ahora", OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                .update();
    }

    @Override
    public Optional<Instant> ultimaAlertaInactividad(long proyectoId, long estudianteId) {
        return jdbc.sql("SELECT ultima_alerta_en FROM alerta_inactividad WHERE proyecto_id = :proyectoId AND estudiante_id = :estudianteId")
                .param("proyectoId", proyectoId)
                .param("estudianteId", estudianteId)
                .query((rs, rowNum) -> rs.getObject(1, OffsetDateTime.class).toInstant())
                .optional();
    }

    @Override
    public void guardarAlertaInactividad(long proyectoId, long estudianteId, Instant ahora) {
        jdbc.sql("INSERT INTO alerta_inactividad (proyecto_id, estudiante_id, ultima_alerta_en) VALUES (:proyectoId, :estudianteId, :ahora) ON CONFLICT (proyecto_id, estudiante_id) DO UPDATE SET ultima_alerta_en = EXCLUDED.ultima_alerta_en")
                .param("proyectoId", proyectoId)
                .param("estudianteId", estudianteId)
                .param("ahora", OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                .update();
    }
}
