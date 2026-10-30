package co.uniproject.nucleo.notificaciones.programador;

import co.uniproject.eventos.datos.TareaRutaCriticaRiesgo;
import co.uniproject.eventos.ejemplos.EjemplosEventos;
import co.uniproject.nucleo.notificaciones.RegistroEventos;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevisionRiesgoTest {

    private static final Instant AHORA = Instant.parse("2026-10-24T19:00:00Z");
    private final Clock reloj = Clock.fixed(AHORA, ZoneId.of("America/Bogota"));
    private final TareaRutaCriticaRiesgo datos = EjemplosEventos.tareaRutaCriticaRiesgo();

    private EvaluadorRiesgo evaluador;
    private EstadoAlertas estado;
    private RegistroEventos registro;
    private RevisionRiesgo revision;

    @BeforeEach
    void setUp() {
        evaluador = Mockito.mock(EvaluadorRiesgo.class);
        estado = Mockito.mock(EstadoAlertas.class);
        registro = Mockito.mock(RegistroEventos.class);
        TransactionTemplate tx = new TransactionTemplate(Mockito.mock(PlatformTransactionManager.class));
        revision = new RevisionRiesgo(evaluador, estado, registro, tx, reloj);
    }

    @Test
    void primeraVezEnRiesgoPublica() {
        when(evaluador.evaluarTareasRutaCritica()).thenReturn(List.of(new EvaluacionRiesgo(45, true, datos)));
        when(estado.riesgoAnterior(45)).thenReturn(Optional.empty());

        int publicados = revision.revisar();

        assertThat(publicados).isEqualTo(1);
        verify(registro).registrar(datos);
        verify(estado).guardarRiesgo(45, true, AHORA);
    }

    @Test
    void pasaDeSinRiesgoARiesgoPublica() {
        when(evaluador.evaluarTareasRutaCritica()).thenReturn(List.of(new EvaluacionRiesgo(45, true, datos)));
        when(estado.riesgoAnterior(45)).thenReturn(Optional.of(false));

        int publicados = revision.revisar();

        assertThat(publicados).isEqualTo(1);
        verify(registro).registrar(datos);
        verify(estado).guardarRiesgo(45, true, AHORA);
    }

    @Test
    void sigueEnRiesgoNoRepite() {
        when(evaluador.evaluarTareasRutaCritica()).thenReturn(List.of(new EvaluacionRiesgo(45, true, datos)));
        when(estado.riesgoAnterior(45)).thenReturn(Optional.of(true));

        int publicados = revision.revisar();

        assertThat(publicados).isEqualTo(0);
        verify(registro, never()).registrar(any());
        verify(estado, never()).guardarRiesgo(anyLong(), anyBoolean(), any());
    }

    @Test
    void saleDeRiesgoSoloGuarda() {
        when(evaluador.evaluarTareasRutaCritica()).thenReturn(List.of(new EvaluacionRiesgo(45, false, null)));
        when(estado.riesgoAnterior(45)).thenReturn(Optional.of(true));

        int publicados = revision.revisar();

        assertThat(publicados).isEqualTo(0);
        verify(registro, never()).registrar(any());
        verify(estado).guardarRiesgo(45, false, AHORA);
    }

    @Test
    void sinRiesgoYSinFilaGuardaFalse() {
        when(evaluador.evaluarTareasRutaCritica()).thenReturn(List.of(new EvaluacionRiesgo(45, false, null)));
        when(estado.riesgoAnterior(45)).thenReturn(Optional.empty());

        int publicados = revision.revisar();

        assertThat(publicados).isEqualTo(0);
        verify(registro, never()).registrar(any());
        verify(estado).guardarRiesgo(45, false, AHORA);
    }

    @Test
    void unFalloNoDetieneLasDemas() {
        when(evaluador.evaluarTareasRutaCritica()).thenReturn(List.of(
                new EvaluacionRiesgo(45, true, datos),
                new EvaluacionRiesgo(46, true, datos)
        ));
        when(estado.riesgoAnterior(45)).thenReturn(Optional.empty());
        when(estado.riesgoAnterior(46)).thenReturn(Optional.empty());
        when(registro.registrar(datos))
                .thenThrow(new RuntimeException("fallo simulado"))
                .thenReturn(UUID.randomUUID());

        int publicados = revision.revisar();

        assertThat(publicados).isEqualTo(1);
        verify(estado).guardarRiesgo(46, true, AHORA);
        verify(estado, never()).guardarRiesgo(eq(45L), anyBoolean(), any());
    }
}
