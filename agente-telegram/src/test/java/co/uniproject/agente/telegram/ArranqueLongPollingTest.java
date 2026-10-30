package co.uniproject.agente.telegram;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class ArranqueLongPollingTest {

    @Test
    void sinTokenNoArranca() {
        ArranqueLongPolling arranque = new ArranqueLongPolling(mock(ReceptorTelegram.class), "");

        assertThatCode(arranque::iniciar).doesNotThrowAnyException();
        assertThat(arranque.activo()).isFalse();
        assertThatCode(arranque::detener).doesNotThrowAnyException();
    }
}
