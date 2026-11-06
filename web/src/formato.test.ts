import { describe, expect, it } from 'vitest';
import { decimalEsCo, fechaCorta, fechaDia, fechaHora, rangoFechas } from './formato';

describe('fechaCorta', () => {
  it('usa la hora de Bogotá (UTC-5) y acepta microsegundos', () => {
    expect(fechaCorta('2026-11-04T03:00:00Z')).toBe('03/11/2026');
    expect(fechaCorta('2026-11-04T15:30:00Z')).toBe('04/11/2026');
    expect(fechaCorta('2026-11-04T15:30:00.123456Z')).toBe('04/11/2026');
  });

  it('un texto que no es fecha se muestra como raya', () => {
    expect(fechaCorta('no-es-fecha')).toBe('—');
  });
});

describe('rangoFechas y decimalEsCo', () => {
  it('rango como en Figma, con el año solo al final si es el mismo', () => {
    expect(rangoFechas('2026-08-01', '2026-08-29')).toBe('01/08 – 29/08/2026');
    expect(rangoFechas('2026-12-15', '2027-01-20')).toBe('15/12/2026 – 20/01/2027');
    expect(rangoFechas('', '2026-08-29')).toBe('Sin fechas');
  });

  it('decimales con coma y sin ceros de más', () => {
    expect(decimalEsCo(33.5)).toBe('33,5');
    expect(decimalEsCo(100)).toBe('100');
  });
});

describe('fechaDia', () => {
  it('fecha de calendario como en Figma P04 y raya si no es una fecha', () => {
    expect(fechaDia('2026-09-26')).toBe('26/09/2026');
    expect(fechaDia('26/09/2026')).toBe('—');
  });
});

describe('fechaHora', () => {
  it('fecha y hora en hora de Colombia a 24 horas y raya si no es fecha', () => {
    expect(fechaHora('2026-09-22T19:10:00Z')).toBe('22/09/2026 14:10');
    expect(fechaHora('2026-09-22T05:05:00.123456Z')).toBe('22/09/2026 00:05');
    expect(fechaHora('no-es-fecha')).toBe('—');
  });
});
