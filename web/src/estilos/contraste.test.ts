import { describe, expect, it } from 'vitest';
import tema from './tema.css?raw';

const cssSinComentarios = tema.replace(/\/\*[\s\S]*?\*\//g, '');
const bloqueRoot = cssSinComentarios.match(/:root\s*\{([^}]+)\}/)?.[1] ?? '';

const variables = new Map<string, string>();
for (const coincidencia of bloqueRoot.matchAll(/--([a-z0-9-]+):\s*([^;]+);/g)) {
  variables.set(`--${coincidencia[1]}`, coincidencia[2].trim());
}

function canalLineal(canal: number): number {
  return canal <= 0.04045 ? canal / 12.92 : ((canal + 0.055) / 1.055) ** 2.4;
}

function luminancia(hex: string): number {
  const limpio = hex.trim().replace(/^#/, '');
  if (!/^[0-9a-fA-F]{6}$/.test(limpio)) {
    throw new Error(`Color hexadecimal inválido: ${hex}`);
  }
  const r = canalLineal(Number.parseInt(limpio.slice(0, 2), 16) / 255);
  const g = canalLineal(Number.parseInt(limpio.slice(2, 4), 16) / 255);
  const b = canalLineal(Number.parseInt(limpio.slice(4, 6), 16) / 255);
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contraste(hexA: string, hexB: string): number {
  const lumA = luminancia(hexA);
  const lumB = luminancia(hexB);
  const lMax = Math.max(lumA, lumB);
  const lMin = Math.min(lumA, lumB);
  return (lMax + 0.05) / (lMin + 0.05);
}

function valorVariable(nombre: string): string {
  const valor = variables.get(nombre);
  if (!valor) {
    throw new Error(`Variable no encontrada en :root: ${nombre}`);
  }
  return valor;
}

const paresTexto: ReadonlyArray<readonly [string, string]> = [
  ['--color-texto-secundario', '--color-superficie'],
  ['--color-texto-secundario', '--color-fondo'],
  ['--color-texto-secundario', '--color-tabla-encabezado-fondo'],
  ['--color-texto-secundario', '--color-info-fondo'],
  ['--color-texto-secundario', '--color-neutro-fondo'],
  ['--color-texto-secundario', '--color-aviso-fondo'],
  ['--color-texto-secundario', '--color-exito-fondo'],
  ['--color-texto-secundario', '--color-peligro-fondo'],
  ['--color-tabla-encabezado-texto', '--color-tabla-encabezado-fondo'],
  ['--color-texto', '--color-fondo'],
  ['--color-texto-sobre-primario', '--color-peligro-hover'],
  ['--color-texto', '--color-secundario-hover'],
];

const paresFoco: ReadonlyArray<readonly [string, string]> = [
  ['--color-primario', '--color-superficie'],
  ['--color-primario', '--color-fondo'],
];

describe('accesibilidad base en tema.css (T-004)', () => {
  it('tema.css se carga en la prueba', () => {
    expect(tema).toContain(':root');
    expect(tema.length).toBeGreaterThan(1000);
  });

  it('calcula contraste 21:1 para negro sobre blanco', () => {
    expect(contraste('#000000', '#ffffff')).toBeCloseTo(21, 5);
  });

  it.each(paresTexto)('cumple contraste >= 4.5 para %s sobre %s', (variableTexto, variableFondo) => {
    const relacion = contraste(valorVariable(variableTexto), valorVariable(variableFondo));
    expect(relacion).toBeGreaterThanOrEqual(4.5);
  });

  it.each(paresFoco)('cumple contraste >= 3 para %s sobre %s', (variableFoco, variableFondo) => {
    const relacion = contraste(valorVariable(variableFoco), valorVariable(variableFondo));
    expect(relacion).toBeGreaterThanOrEqual(3);
  });

  it('no contiene outline: none ni outline:none', () => {
    expect(cssSinComentarios).not.toMatch(/outline:\s*none\b/);
  });

  it('no contiene 100vh', () => {
    expect(cssSinComentarios).not.toContain('100vh');
  });

  it('define un bloque @media (forced-colors: active)', () => {
    expect(cssSinComentarios).toMatch(/@media\s*\(\s*forced-colors:\s*active\s*\)/);
  });

  it('define en rem todas las variables --tamano-* usadas como font-size', () => {
    const variablesTamanoFuente = new Set<string>();
    for (const coincidencia of cssSinComentarios.matchAll(/font-size:\s*var\((--[a-z0-9-]+)\)/g)) {
      variablesTamanoFuente.add(coincidencia[1]);
    }
    expect(variablesTamanoFuente.size).toBeGreaterThan(0);
    for (const nombre of variablesTamanoFuente) {
      expect(valorVariable(nombre), `${nombre} debe terminar en rem`).toMatch(/rem$/);
    }
  });
});
