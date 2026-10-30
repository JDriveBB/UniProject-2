package co.uniproject.nucleo.comun.error;

/** 403. La lanzan los servicios cuando Permisos dice que no (restricción 4, RF13). ManejadorErrores la audita. */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException() {
        super("No tienes permiso para esta acción");
    }
}
