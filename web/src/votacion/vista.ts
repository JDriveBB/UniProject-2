import type { CandidatoVista, RondaVista, VotacionVista } from '../api/tipos';
import { fechaHora } from '../formato';

export type EstadoRonda = 'sin' | 'abierta' | 'vencida' | 'cerrada';

/** sin: no hay ronda; abierta; vencida: ya pasó la hora pero el núcleo aún no la cierra; cerrada. */
export function estadoRonda(r: RondaVista | null): EstadoRonda {
  if (r === null) {
    return 'sin';
  }
  if (r.abierta) {
    return 'abierta';
  }
  return r.cerradaEn === null ? 'vencida' : 'cerrada';
}

export interface ResumenRonda {
  texto: string;
  tono: 'info' | 'aviso' | 'neutro';
  desempatar: boolean;
}

/** Frase de estado de la votación para la tarjeta del equipo y la página; null si no hay ronda. */
export function resumenRonda(v: VotacionVista): ResumenRonda | null {
  const r = v.ronda;
  if (r === null) {
    return null;
  }
  const estado = estadoRonda(r);
  if (estado === 'abierta') {
    return {
      texto: `Votación abierta: votaron ${r.votosEmitidos} de ${r.votantes}. Cierra el ${fechaHora(r.cierraEn)}.`,
      tono: 'info',
      desempatar: false,
    };
  }
  if (estado === 'vencida') {
    return {
      texto: 'La votación terminó; el resultado aparecerá en unos minutos.',
      tono: 'neutro',
      desempatar: false,
    };
  }
  switch (r.resultado) {
    case 'LIDER_ELEGIDO':
      return { texto: `Última votación: ganó ${r.nombreLiderElegido}.`, tono: 'neutro', desempatar: false };
    case 'EMPATE':
      if (v.puedeDesempatar) {
        return {
          texto: 'La votación terminó en empate. Elige al líder entre los empatados.',
          tono: 'aviso',
          desempatar: true,
        };
      }
      if (r.idLiderElegido !== null) {
        return {
          texto: `Hubo empate y el docente eligió a ${r.nombreLiderElegido}.`,
          tono: 'neutro',
          desempatar: false,
        };
      }
      return { texto: 'La votación terminó en empate. El docente elegirá al líder.', tono: 'aviso', desempatar: false };
    case 'SIN_VOTOS':
      return { texto: 'La última votación terminó sin votos.', tono: 'aviso', desempatar: false };
    case 'CANCELADA':
      return {
        texto: 'La última votación se canceló porque el docente nombró al líder.',
        tono: 'neutro',
        desempatar: false,
      };
    default:
      return null;
  }
}

/** Candidatos empatados de una ronda cerrada. */
export function empatados(r: RondaVista): CandidatoVista[] {
  return r.candidatos.filter((c) => c.empatado);
}

export function textoVotos(n: number | null): string {
  if (n === null) {
    return '—';
  }
  return n === 1 ? '1 voto' : `${n} votos`;
}

export function textoBotonVoto(miVoto: number | null): string {
  return miVoto === null ? 'Votar' : 'Cambiar mi voto';
}

export function puedeEnviarVoto(seleccion: number | null, miVoto: number | null): boolean {
  return seleccion !== null && seleccion !== miVoto;
}

export function textoLiderActual(v: Pick<VotacionVista, 'nombreLiderActual'>): string {
  return v.nombreLiderActual === null ? 'Este equipo no tiene líder.' : `Líder actual: ${v.nombreLiderActual}`;
}
