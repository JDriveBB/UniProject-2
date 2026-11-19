import { describe, expect, it } from 'vitest';
import { tituloDocumento } from './tituloDocumento';

describe('tituloDocumento', () => {
  it('agrega el sufijo · UniProject al título de la pantalla', () => {
    expect(tituloDocumento('Tablero del equipo')).toBe('Tablero del equipo · UniProject');
  });

  it('recorta espacios alrededor del título', () => {
    expect(tituloDocumento('  Tablero del equipo  ')).toBe('Tablero del equipo · UniProject');
  });

  it('devuelve UniProject cuando el título está vacío o solo tiene espacios', () => {
    expect(tituloDocumento('')).toBe('UniProject');
    expect(tituloDocumento('   \t\n ')).toBe('UniProject');
  });
});
