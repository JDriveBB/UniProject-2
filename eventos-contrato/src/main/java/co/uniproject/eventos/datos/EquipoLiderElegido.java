package co.uniproject.eventos.datos;

import java.util.List;

/** Un equipo obtiene líder por votación o desempate del docente; lo reciben los integrantes y el docente. */
public record EquipoLiderElegido(
        Referencia proyecto,
        Referencia equipo,
        Referencia lider,
        OrigenLider origen,
        List<Persona> integrantes,
        Persona docente)
        implements DatosEvento {}
