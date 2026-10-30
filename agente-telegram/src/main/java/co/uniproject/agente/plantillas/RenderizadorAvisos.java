package co.uniproject.agente.plantillas;

import co.uniproject.agente.destinatarios.Variante;
import co.uniproject.eventos.EventoCloud;

public interface RenderizadorAvisos {
    /** @throws PlantillaException si la plantilla no existe para esa variante o falla al llenarse */
    Aviso renderizar(EventoCloud evento, Variante variante);
}
