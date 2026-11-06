import { describe, expect, it } from 'vitest';
import type { CriterioVista } from '../api/tipos';
import {
  centesimasNota,
  notaCalculada,
  pesosCompletos,
  textoCentesimas,
  textoFactor,
  textoNota,
} from './calculo';

function criterio(parcial: Partial<CriterioVista>): CriterioVista {
  return {
    idCriterio: 1,
    idHito: 1,
    descripcion: 'Criterio de prueba',
    pesoPorcentaje: 100,
    puntajeMaximo: 5,
    ...parcial,
  };
}

describe('calculo', () => {
  it('convierte notas y factores con formato y decimales esperados', () => {
    expect(centesimasNota('4,5')).toBe(450);
    expect(centesimasNota('4.25')).toBe(425);
    expect(centesimasNota(' 3 ')).toBe(300);
    expect(centesimasNota('0')).toBe(0);
    expect(centesimasNota('4,555')).toBeNull();
    expect(centesimasNota('-1')).toBeNull();
    expect(centesimasNota('')).toBeNull();
    expect(centesimasNota('1000')).toBeNull();
    expect(centesimasNota('4,')).toBeNull();

    expect(textoNota(4.2)).toBe('4,20');
    expect(textoNota(5)).toBe('5,00');
    expect(textoNota(0)).toBe('0,00');

    expect(textoFactor(1.25)).toBe('1,2500');
    expect(textoFactor(0.5)).toBe('0,5000');

    expect(textoCentesimas(3.5)).toBe('3,5');
    expect(textoCentesimas(4)).toBe('4');
  });

  it('calcula la nota grupal ponderada en centésimas con redondeo en la mitad hacia arriba', () => {
    const rubrica = [
      criterio({ idCriterio: 1, pesoPorcentaje: 40, puntajeMaximo: 5 }),
      criterio({ idCriterio: 2, pesoPorcentaje: 30, puntajeMaximo: 5 }),
      criterio({ idCriterio: 3, pesoPorcentaje: 30, puntajeMaximo: 5 }),
    ];
    expect(notaCalculada(rubrica, { 1: '4', 2: '3,5', 3: '5' })).toBe(4.15);
    expect(notaCalculada(rubrica, { 1: '4', 2: '3,5' })).toBeNull();
    expect(notaCalculada(rubrica, { 1: '4', 2: '3,5', 3: '5,5' })).toBeNull();
    expect(notaCalculada(rubrica, { 1: 'x', 2: '3,5', 3: '5' })).toBeNull();

    const rubricaMitad = [criterio({ idCriterio: 1, pesoPorcentaje: 100, puntajeMaximo: 2 })];
    expect(notaCalculada(rubricaMitad, { 1: '0,01' })).toBe(0.03);

    const rubricaTercio = [criterio({ idCriterio: 1, pesoPorcentaje: 100, puntajeMaximo: 3 })];
    expect(notaCalculada(rubricaTercio, { 1: '1' })).toBe(1.67);

    const rubricaDos = [
      criterio({ idCriterio: 1, pesoPorcentaje: 50, puntajeMaximo: 10 }),
      criterio({ idCriterio: 2, pesoPorcentaje: 50, puntajeMaximo: 5 }),
    ];
    expect(notaCalculada(rubricaDos, { 1: '7', 2: '4,5' })).toBe(4);

    const rubricaIncompleta = [
      criterio({ idCriterio: 1, pesoPorcentaje: 40 }),
      criterio({ idCriterio: 2, pesoPorcentaje: 30 }),
    ];
    expect(notaCalculada(rubricaIncompleta, { 1: '4', 2: '4' })).toBeNull();
    expect(notaCalculada([], {})).toBeNull();
  });

  it('verifica si los pesos de la rúbrica suman exactamente 100 %', () => {
    expect(
      pesosCompletos([
        { pesoPorcentaje: 33.33 },
        { pesoPorcentaje: 33.33 },
        { pesoPorcentaje: 33.34 },
      ])
    ).toBe(true);
    expect(pesosCompletos([{ pesoPorcentaje: 100 }])).toBe(true);
    expect(
      pesosCompletos([
        { pesoPorcentaje: 50 },
        { pesoPorcentaje: 49.99 },
      ])
    ).toBe(false);
    expect(pesosCompletos([])).toBe(false);
  });
});
