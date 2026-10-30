package co.uniproject.agente.plantillas;

/** Plantilla ausente o rota. Error permanente: el evento va a fallidos. */
public class PlantillaException extends RuntimeException {
    public PlantillaException(String mensaje) { super(mensaje); }
    public PlantillaException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
