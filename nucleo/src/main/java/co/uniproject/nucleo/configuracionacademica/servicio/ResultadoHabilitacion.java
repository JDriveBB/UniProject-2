package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.seguridad.cuentas.CorreoHabilitadoVista;
import java.util.List;

/** habilitados: filas nuevas. yaHabilitados: el correo ya estaba en el proyecto. conOtroRol: ya tiene cuenta con otro rol. */
public record ResultadoHabilitacion(List<CorreoHabilitadoVista> habilitados, List<String> yaHabilitados,
                                    List<String> conOtroRol) {}
