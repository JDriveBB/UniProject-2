package co.uniproject.nucleo.comun.error;

/** 429 DEMASIADAS_SOLICITUDES (decisión 28-6). Mismo texto exista o no la cuenta. */
public class DemasiadasSolicitudesException extends RuntimeException {

    public DemasiadasSolicitudesException() {
        super("Demasiados intentos. Espera un rato y vuelve a intentarlo");
    }
}
