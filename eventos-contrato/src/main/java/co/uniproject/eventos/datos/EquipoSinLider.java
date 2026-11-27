package co.uniproject.eventos.datos;

/** Una votación de líder cierra en empate o sin votos; lo recibe solo el docente. */
public record EquipoSinLider(Referencia proyecto, Referencia equipo, MotivoSinLider motivo, Persona docente)
        implements DatosEvento {}
