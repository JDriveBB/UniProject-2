package co.uniproject.agente.idempotencia;

import java.util.UUID;

/** Pareja (evento, destinatario) ya procesada: enviada u omitida de forma permanente (plan §8.4, variante por destinatario). */
public interface RegistroProcesados {
    boolean yaProcesado(UUID eventoId, long usuarioId);
    void registrar(UUID eventoId, long usuarioId);
}
