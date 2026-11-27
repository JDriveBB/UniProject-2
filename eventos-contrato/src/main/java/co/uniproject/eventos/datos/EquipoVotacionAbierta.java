package co.uniproject.eventos.datos;

import java.time.Instant;
import java.util.List;

/** Se abre una ronda de votación de líder en el equipo; lo reciben los integrantes. */
public record EquipoVotacionAbierta(Referencia proyecto, Referencia equipo, Instant cierraEn, List<Persona> integrantes)
        implements DatosEvento {}
