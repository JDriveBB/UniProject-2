import { describe, expect, it } from 'vitest';
import type { NodoRutaCritica } from '../api/tipos';
import { disponerRed } from './disposicion';

function nodo(idTarea: number, titulo: string, horas: number, holgura: number, prerrequisitos: number[],
              inicio: number): NodoRutaCritica {
  return {
    idTarea,
    titulo,
    horasEstimadas: horas,
    porcentajeAvance: 0,
    critica: holgura === 0,
    inicioTempranoHoras: inicio,
    finTempranoHoras: inicio + horas,
    inicioTardioHoras: inicio + holgura,
    finTardioHoras: inicio + horas + holgura,
    holguraHoras: holgura,
    prerrequisitos,
  };
}

/** Datos de Figma P06: 44 h = 16 + 12 + 10 + 6; T3 con 4 h de holgura. */
const FIGMA: NodoRutaCritica[] = [
  nodo(1, 'Casos de uso', 16, 0, [], 0),
  nodo(2, 'Diagramas de secuencia', 12, 0, [1], 16),
  nodo(3, 'Modelo relacional', 8, 4, [1], 16),
  nodo(4, 'Prototipos en Figma', 10, 0, [2, 3], 28),
  nodo(5, 'Diapositivas', 6, 0, [4], 38),
];

describe('disposición de la red de P06', () => {
  it('con los datos de Figma da sus coordenadas y un lienzo de 1320 × 420', () => {
    const r = disponerRed(FIGMA);
    expect(r.nodos.map((n) => [n.idTarea, n.x, n.y])).toEqual([
      [1, 60, 170],
      [2, 380, 50],
      [3, 380, 290],
      [4, 700, 170],
      [5, 1020, 170],
    ]);
    expect([r.ancho, r.alto, r.niveles]).toEqual([1320, 420, 4]);
    expect(r.nodos[2].detalle).toBe('8 h · holgura 4 h');
  });

  it('flechas de la predecesora a la dependiente; críticas solo entre tareas críticas encadenadas', () => {
    const r = disponerRed(FIGMA);
    expect(r.aristas.map((a) => [a.desde, a.hasta, a.critica])).toEqual([
      [1, 2, true],
      [1, 3, false],
      [2, 4, true],
      [3, 4, false],
      [4, 5, true],
    ]);
    expect(r.aristas[0].d).toBe('M 300 205 C 340 205, 340 85, 380 85');
    expect(r.aristas[4].d).toBe('M 940 205 L 1020 205');
  });

  it('ignora prerrequisitos ausentes o que llegan después, recorta títulos largos y una red vacía mide 0', () => {
    const r = disponerRed([
      nodo(1, 'A', 1, 0, [99], 0),
      nodo(2, 'B', 1, 0, [3], 0),
      nodo(3, 'Una tarea con un título bastante largo', 1, 0, [], 0),
    ]);
    expect(r.nodos.map((n) => n.nivel)).toEqual([0, 0, 0]);
    expect(r.aristas).toEqual([]);
    expect(r.nodos[2].titulo).toBe('Una tarea con un título b…');
    expect(r.nodos[2].tituloCompleto).toBe('Una tarea con un título bastante largo');
    expect(disponerRed([])).toEqual({ nodos: [], aristas: [], ancho: 0, alto: 0, niveles: 0 });
  });

  it('RNF1: 150 tareas en 15 niveles (1400 dependencias) se disponen sin choques y rápido', () => {
    const nodos: NodoRutaCritica[] = [];
    for (let id = 1; id <= 150; id++) {
      const nivel = Math.floor((id - 1) / 10);
      const previas = nivel === 0 ? [] : Array.from({ length: 10 }, (_, i) => (nivel - 1) * 10 + i + 1);
      nodos.push(nodo(id, 'Tarea ' + id, 2, 0, previas, nivel * 2));
    }
    const inicio = performance.now();
    const r = disponerRed(nodos);
    const ms = performance.now() - inicio;
    expect([r.nodos.length, r.aristas.length, r.niveles, r.ancho, r.alto]).toEqual([150, 1400, 15, 4840, 2340]);
    expect(new Set(r.nodos.map((n) => `${n.x},${n.y}`)).size).toBe(150);
    expect(ms).toBeLessThan(100);
  });
});
