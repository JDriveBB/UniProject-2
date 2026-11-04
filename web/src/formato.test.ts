import { describe, expect, it } from 'vitest';
import { fechaCorta } from './formato';

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
