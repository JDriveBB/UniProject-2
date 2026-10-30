package co.uniproject.nucleo.notificaciones.programador;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevisionInactividadTest {

    private static final Instant AHORA = Instant.parse("2026-10-24T19:00:00Z");
    private final Clock reloj = Clock.fixed(AHORA, ZoneId.of("America/Bogota"));
    private final CasoInactividad caso = new CasoInactividad(
            12, 102, Instant.parse("2026-10-20T14:30:00Z"), EjemplosEventos.equipoInactividad(false));

    private DetectorInactividad detector;
    private EstadoAlertas estado;
    private RegistroEventos registro;
    private RevisionInactividad revision;

    @BeforeEach
    void setUp() {
        detector = Mockito.mock(DetectorInactividad.class);
        estado = Mockito.mock(EstadoAlertas.class);
        registro = Mockito.mock(RegistroEventos.class);
        TransactionTemplate tx = new TransactionTemplate(Mockito.mock(PlatformTransactionManager.class));
        revision = new RevisionInactividad(detector, estado, registro, tx, reloj);
    }

    @Test
    void primeraAlertaPublica() {
        when(detector.buscarInactivos(AHORA)).thenReturn(List.of(caso));
        when(estado.ultimaAlertaInactividad(12, 102)).thenReturn(Optional.empty());

        int publicados = revision.revisar();

        assertThat(publicados).isEqualTo(1);
        verify(registro).registrar(caso.datos());
        verify(estado).guardarAlertaInactividad(12, 102, AHORA);
    }

    @Test
    void yaAlertadoEnEstePeriodoNoRepite() {
        when(detector.buscarInactivos(AHORA)).thenReturn(List.of(caso));
        when(estado.ultimaAlertaInactividad(12, 102))
                .thenReturn(Optional.of(Instant.parse("2026-10-23T19:00:00Z")));

        int publicados = revision.revisar();

        assertThat(publicados).isEqualTo(0);
        verify(registro, never()).registrar(any());
        verify(estado, never()).guardarAlertaInactividad(anyLong(), anyLong(), any());
    }

    @Test
    void volvioATrabajarYRecayoPublica() {
        when(detector.buscarInactivos(AHORA)).thenReturn(List.of(caso));
        when(estado.ultimaAlertaInactividad(12, 102))
                .thenReturn(Optional.of(Instant.parse("2026-10-15T19:00:00Z")));

        int publicados = revision.revisar();

        assertThat(publicados).isEqualTo(1);
        verify(registro).registrar(caso.datos());
        verify(estado).guardarAlertaInactividad(12, 102, AHORA);
    }

    @Test
    void unFalloNoDetieneLosDemas() {
        CasoInactividad caso2 = new CasoInactividad(
                12, 103, Instant.parse("2026-10-20T14:30:00Z"), EjemplosEventos.equipoInactividad(false));
        when(detector.buscarInactivos(AHORA)).thenReturn(List.of(caso, caso2));
        when(estado.ultimaAlertaInactividad(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(registro.registrar(any()))
                .thenThrow(new RuntimeException("fallo simulado"))
                .thenReturn(UUID.randomUUID());

        int publicados = revision.revisar();

        assertThat(publicados).isEqualTo(1);
        verify(estado).guardarAlertaInactividad(12, 103, AHORA);
        verify(estado, never()).guardarAlertaInactividad(eq(12L), eq(102L), any());
    }
}
