import { api } from './cliente';
import type { SolicitudDesempate, SolicitudVoto, VotacionVista } from './tipos';

/** vistaVotacion: estado actual de liderazgo y ronda del equipo. */
export function vistaVotacion(idEquipo: number): Promise<VotacionVista> {
  return api.get<VotacionVista>(`/api/equipos/${idEquipo}/ronda`);
}

/** abrirRonda: abre una ronda a demanda (409 EQUIPO_SIN_INTEGRANTES o RONDA_ABIERTA). */
export function abrirRonda(idEquipo: number): Promise<VotacionVista> {
  return api.post<VotacionVista>(`/api/equipos/${idEquipo}/ronda`);
}

/** votarLider: emite o cambia el voto del integrante (400 VALIDACION; 409 SIN_RONDA_ABIERTA o CANDIDATO_NO_VALIDO). */
export function votarLider(idEquipo: number, idCandidato: number): Promise<VotacionVista> {
  const solicitud: SolicitudVoto = { idCandidato };
  return api.put<VotacionVista>(`/api/equipos/${idEquipo}/ronda/voto`, solicitud);
}

/** desempatarLider: el docente elige al líder entre los candidatos empatados (400 VALIDACION; 409 SIN_EMPATE, EMPATE_RESUELTO o NO_ESTA_EMPATADO). */
export function desempatarLider(idEquipo: number, idUsuario: number): Promise<VotacionVista> {
  const solicitud: SolicitudDesempate = { idUsuario };
  return api.put<VotacionVista>(`/api/equipos/${idEquipo}/ronda/desempate`, solicitud);
}
