package co.uniproject.nucleo.configuracionacademica.cursos;

import co.uniproject.nucleo.seguridad.cuentas.CorreoHabilitadoVista;
import java.util.List;

/** Resultado de agregar o habilitar correos de estudiantes en un curso. */
public record ResultadoCorreosCurso(
        List<String> agregados,
        List<CorreoHabilitadoVista> habilitados,
        List<String> yaEnCurso,
        List<String> conOtroRol) {}
