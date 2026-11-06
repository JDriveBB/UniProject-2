import { describe, expect, it } from 'vitest';
import type { BorradorCriterio, BorradorHito, BorradorProyecto } from './borrador';
import {
  centesimas,
  estadoSuma,
  primerHitoConErrores,
  sumaPesos,
  textoSumaPesos,
  validarBorrador,
  validarCriterio,
  validarDatosProyecto,
  validarHito,
  validarNombreEquipo,
} from './validacion';

const criterio = (clave: string, peso: string): BorradorCriterio => ({
  clave,
  idCriterio: null,
  descripcion: 'Criterio ' + clave,
  peso,
  puntaje: '5',
});

const hito: BorradorHito = {
  clave: 'h1',
  idHito: 1,
  orden: 1,
  nombre: 'Análisis',
  descripcion: null,
  fechaInicio: '2026-08-01',
  fechaLimite: '2026-08-29',
  diasInactividad: '3',
  notificarDocente: false,
  criterios: [],
};

const proyecto: BorradorProyecto = {
  nombre: 'Diseño de Software I',
  descripcion: '',
  fechaInicio: '2026-08-01',
  fechaFin: '2026-11-30',
  usaMonitores: false,
  hitos: [hito],
};

describe('validación del proyecto y sus hitos', () => {
  it('un proyecto completo no tiene errores', () => {
    expect(validarDatosProyecto(proyecto)).toEqual({});
    expect(validarHito(hito, proyecto)).toEqual({});
  });

  it('datos del proyecto: obligatorios, longitudes y fin no anterior al inicio', () => {
    expect(validarDatosProyecto({ ...proyecto, nombre: '  ', fechaInicio: '', fechaFin: '' })).toEqual({
      nombre: 'Escribe el nombre del proyecto',
      fechaInicio: 'Elige la fecha de inicio',
      fechaFin: 'Elige la fecha de fin',
    });
    expect(
      validarDatosProyecto({ ...proyecto, nombre: 'x'.repeat(151), descripcion: 'd'.repeat(2001), fechaFin: '2026-07-31' }),
    ).toEqual({
      nombre: 'El nombre no puede pasar de 150 caracteres',
      descripcion: 'La descripción no puede pasar de 2000 caracteres',
      fechaFin: 'La fecha de fin no puede ser anterior a la de inicio',
    });
  });

  it('hito: fechas dentro del proyecto y alerta de 1 a 60 días', () => {
    expect(
      validarHito({ ...hito, fechaInicio: '2026-07-31', fechaLimite: '2026-12-01', diasInactividad: '' }, proyecto),
    ).toEqual({
      fechaInicio: 'Debe estar dentro de las fechas del proyecto',
      fechaLimite: 'Debe estar dentro de las fechas del proyecto',
      diasInactividad: 'Escribe un número entero de 1 a 60',
    });
    expect(validarHito({ ...hito, nombre: '', fechaLimite: '2026-07-15', diasInactividad: '61' }, proyecto)).toEqual({
      nombre: 'Escribe el nombre del hito',
      fechaLimite: 'No puede ser anterior a la fecha de inicio',
      diasInactividad: 'Escribe un número entero de 1 a 60',
    });
    expect(validarHito({ ...hito, diasInactividad: '2.5' }, proyecto).diasInactividad).toBe(
      'Escribe un número entero de 1 a 60',
    );
    expect(validarHito({ ...hito, diasInactividad: '60' }, proyecto)).toEqual({});
  });

  it('criterio: texto, peso y puntaje como numeric(5,2) del núcleo; nombre del equipo', () => {
    expect(validarCriterio(criterio('a', '25'))).toEqual({});
    expect(validarCriterio({ ...criterio('a', '0'), descripcion: ' ', puntaje: '1000' })).toEqual({
      descripcion: 'Escribe el criterio',
      peso: 'Mayor que 0 y hasta 100, con 2 decimales como máximo',
      puntaje: 'Mayor que 0 y hasta 999,99, con 2 decimales como máximo',
    });
    expect(validarCriterio(criterio('a', '100.01')).peso).toBe('Mayor que 0 y hasta 100, con 2 decimales como máximo');
    expect(validarCriterio(criterio('a', '12.345')).peso).toBe('Mayor que 0 y hasta 100, con 2 decimales como máximo');
    expect(validarNombreEquipo('  ')).toBe('Escribe el nombre del equipo');
    expect(validarNombreEquipo('Equipo Alfa')).toBeNull();
  });
});

describe('suma de pesos de la rúbrica', () => {
  it('suma en centésimas exactas y la muestra como Figma', () => {
    const tercios = [criterio('a', '33.33'), criterio('b', '33.33'), criterio('c', '33.34')];
    expect(sumaPesos(tercios)).toBe(10000);
    expect(estadoSuma(sumaPesos(tercios))).toBe('completa');
    expect(textoSumaPesos(10000)).toBe('Suma de pesos: 100 %');
    expect(sumaPesos([criterio('a', '50'), criterio('b', '49.5'), criterio('c', '')])).toBe(9950);
    expect(textoSumaPesos(9950)).toBe('Suma de pesos: 99,5 %');
    expect(estadoSuma(9950)).toBe('incompleta');
    expect(estadoSuma(10050)).toBe('excedida');
    expect(centesimas('abc')).toBeNull();
  });

  it('el borrador marca la rúbrica que pasa de 100 % y el primer hito con errores', () => {
    const otro: BorradorHito = {
      ...hito,
      clave: 'n2',
      idHito: null,
      orden: 2,
      nombre: 'Diseño',
      fechaInicio: '2026-09-01',
      fechaLimite: '2026-09-30',
      criterios: [criterio('n3', '60'), criterio('n4', '50')],
    };
    const b: BorradorProyecto = { ...proyecto, hitos: [hito, otro] };
    const errores = validarBorrador(b);
    expect(errores.rubricas).toEqual({ n2: 'Los pesos suman 110 %; el máximo es 100 %' });
    expect(errores.hitos).toEqual({});
    expect(primerHitoConErrores(b, errores)).toBe('n2');
    expect(primerHitoConErrores(proyecto, validarBorrador(proyecto))).toBeNull();
  });
});
