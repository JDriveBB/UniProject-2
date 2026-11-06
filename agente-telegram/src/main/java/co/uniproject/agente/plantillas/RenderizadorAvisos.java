package co.uniproject.agente.plantillas;

import co.uniproject.agente.destinatarios.Variante;
import co.uniproject.eventos.EventoCloud;

public interface RenderizadorAvisos {
    /** @throws PlantillaException si la plantilla no existe para esa variante o falla al llenarse */
    Aviso renderizar(EventoCloud evento, Variante variante);

    /**
     * cuenta.recuperacion.solicitada con el código recién pedido al núcleo (bloque 28). El código no entra en ningún
     * mensaje de excepción.
     * @throws PlantillaException si el evento no es de recuperación o el código no tiene el formato del contrato
     */
    Aviso renderizarRecuperacion(EventoCloud evento, String codigo);
}
