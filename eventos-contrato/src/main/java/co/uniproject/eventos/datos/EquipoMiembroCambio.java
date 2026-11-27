package co.uniproject.eventos.datos;

import java.util.List;

/** Personas que entraron o salieron del equipo (personas); lo reciben los demás integrantes (integrantes, sin ellos) y, si las metió el docente, también quienes entraron. */
public record EquipoMiembroCambio(
        Referencia proyecto,
        Referencia equipo,
        MovimientoEquipo movimiento,
        boolean porDocente,
        List<Persona> personas,
        List<Persona> integrantes)
        implements DatosEvento {}
