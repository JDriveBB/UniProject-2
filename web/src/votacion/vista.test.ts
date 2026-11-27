import { describe, expect, it } from 'vitest';
import type { RondaVista, VotacionVista } from '../api/tipos';
import {
  empatados,
  estadoRonda,
  puedeEnviarVoto,
  resumenRonda,
  textoBotonVoto,
  textoLiderActual,
  textoVotos,
} from './vista';

function ronda(cambios: Partial<RondaVista> = {}): RondaVista {
  return {
    idRonda: 1,
    origen: 'DEMANDA',
    abreEn: '2026-11-28T15:00:00Z',
    cierraEn: '2026-11-30T22:00:00Z',
    cerradaEn: null,
    abierta: true,
    resultado: null,
    idLiderElegido: null,
    nombreLiderElegido: null,
    votosEmitidos: 1,
    votantes: 2,
    miVoto: null,
    puedoVotar: true,
    candidatos: [],
    ...cambios,
  };
}

function vista(r: RondaVista | null, cambios: Partial<VotacionVista> = {}): VotacionVista {
  return {
    idEquipo: 5,
    idLiderActual: null,
    nombreLiderActual: null,
    ronda: r,
    puedeAbrir: false,
    puedeDesempatar: false,
    ...cambios,
  };
}

const cerrada = { abierta: false, cerradaEn: '2026-11-30T22:01:00Z' };

describe('estadoRonda', () => {
  it('sin, abierta, vencida y cerrada', () => {
    expect(estadoRonda(null)).toBe('sin');
    expect(estadoRonda(ronda())).toBe('abierta');
    expect(estadoRonda(ronda({ abierta: false, cerradaEn: null }))).toBe('vencida');
    expect(estadoRonda(ronda(cerrada))).toBe('cerrada');
  });
});

describe('resumenRonda', () => {
  it('sin ronda es null', () => {
    expect(resumenRonda(vista(null))).toBeNull();
  });

  it('abierta', () => {
    expect(resumenRonda(vista(ronda()))).toEqual({
      texto: 'Votación abierta: votaron 1 de 2. Cierra el 30/11/2026 17:00.',
      tono: 'info',
      desempatar: false,
    });
  });

  it('vencida', () => {
    expect(resumenRonda(vista(ronda({ abierta: false, cerradaEn: null })))).toEqual({
      texto: 'La votación terminó; el resultado aparecerá en unos minutos.',
      tono: 'neutro',
      desempatar: false,
    });
  });

  it('líder elegido', () => {
    const r = ronda({ ...cerrada, resultado: 'LIDER_ELEGIDO', nombreLiderElegido: 'Ana', idLiderElegido: 3 });
    expect(resumenRonda(vista(r))).toEqual({
      texto: 'Última votación: ganó Ana.',
      tono: 'neutro',
      desempatar: false,
    });
  });

  it('empate con desempate disponible', () => {
    expect(resumenRonda(vista(ronda({ ...cerrada, resultado: 'EMPATE' }), { puedeDesempatar: true }))).toEqual({
      texto: 'La votación terminó en empate. Elige al líder entre los empatados.',
      tono: 'aviso',
      desempatar: true,
    });
  });

  it('empate ya resuelto por el docente', () => {
    const r = ronda({ ...cerrada, resultado: 'EMPATE', idLiderElegido: 4, nombreLiderElegido: 'Luis' });
    expect(resumenRonda(vista(r))).toEqual({
      texto: 'Hubo empate y el docente eligió a Luis.',
      tono: 'neutro',
      desempatar: false,
    });
  });

  it('empate pendiente para quien no puede desempatar', () => {
    expect(resumenRonda(vista(ronda({ ...cerrada, resultado: 'EMPATE' })))).toEqual({
      texto: 'La votación terminó en empate. El docente elegirá al líder.',
      tono: 'aviso',
      desempatar: false,
    });
  });

  it('sin votos', () => {
    expect(resumenRonda(vista(ronda({ ...cerrada, resultado: 'SIN_VOTOS' })))).toEqual({
      texto: 'La última votación terminó sin votos.',
      tono: 'aviso',
      desempatar: false,
    });
  });

  it('cancelada', () => {
    expect(resumenRonda(vista(ronda({ ...cerrada, resultado: 'CANCELADA' })))).toEqual({
      texto: 'La última votación se canceló porque el docente nombró al líder.',
      tono: 'neutro',
      desempatar: false,
    });
  });
});

describe('textos del voto', () => {
  it('empatados', () => {
    const r = ronda({
      candidatos: [
        { idUsuario: 1, nombre: 'A', votos: 2, empatado: true },
        { idUsuario: 2, nombre: 'B', votos: 2, empatado: true },
        { idUsuario: 3, nombre: 'C', votos: 1, empatado: false },
      ],
    });
    expect(empatados(r).map((c) => c.idUsuario)).toEqual([1, 2]);
  });

  it('textoVotos', () => {
    expect(textoVotos(null)).toBe('—');
    expect(textoVotos(0)).toBe('0 votos');
    expect(textoVotos(1)).toBe('1 voto');
    expect(textoVotos(3)).toBe('3 votos');
  });

  it('textoBotonVoto y puedeEnviarVoto', () => {
    expect(textoBotonVoto(null)).toBe('Votar');
    expect(textoBotonVoto(7)).toBe('Cambiar mi voto');
    expect(puedeEnviarVoto(null, null)).toBe(false);
    expect(puedeEnviarVoto(7, 7)).toBe(false);
    expect(puedeEnviarVoto(8, 7)).toBe(true);
    expect(puedeEnviarVoto(8, null)).toBe(true);
  });

  it('textoLiderActual', () => {
    expect(textoLiderActual({ nombreLiderActual: 'Ana' })).toBe('Líder actual: Ana');
    expect(textoLiderActual({ nombreLiderActual: null })).toBe('Este equipo no tiene líder.');
  });
});
