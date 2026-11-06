import { describe, expect, it } from 'vitest';
import { avanceDelHito, codigosDeTareas, estadoHito, estadoTarea, LEYENDA, marcasDe, nombreConCodigo } from './marcas';

describe('marcas del tablero', () => {
  it('códigos T1, T2… por idTarea y nombre con código como en los chips de P05', () => {
    const codigos = codigosDeTareas([{ idTarea: 30 }, { idTarea: 12 }, { idTarea: 25 }]);
    expect([...codigos.entries()]).toEqual([
      [12, 'T1'],
      [25, 'T2'],
      [30, 'T3'],
    ]);
    expect(nombreConCodigo(codigos, { idTarea: 25, titulo: 'Modelo relacional' })).toBe('T2 · Modelo relacional');
  });

  it('marcas de una fila en el orden de Figma', () => {
    const todas = marcasDe({ bloqueada: true, enRutaCritica: true, esPrerrequisitoCritico: true, nivelRiesgo: 'MEDIO' });
    expect(todas.map((m) => [m.texto, m.tono])).toEqual([
      ['Bloqueada por dependencia', 'neutro'],
      ['Ruta crítica', 'info'],
      ['Prerrequisito crítico', 'critico'],
      ['Riesgo medio', 'aviso'],
    ]);
    expect(marcasDe({ bloqueada: false, enRutaCritica: false, esPrerrequisitoCritico: false, nivelRiesgo: null }))
      .toEqual([]);
  });

  it('avance del hito: promedio simple redondeado (Figma: 100, 60, 80, 0, 0 → 48 %)', () => {
    const avance = (lista: number[]) => avanceDelHito(lista.map((porcentajeAvance) => ({ porcentajeAvance })));
    expect(avance([100, 60, 80, 0, 0])).toBe(48);
    expect(avance([33, 34])).toBe(34);
    expect(avance([])).toBe(0);
  });

  it('estados de tarea y de hito, y leyenda en el orden de Figma', () => {
    expect(estadoTarea('PENDIENTE')).toEqual({ texto: 'Pendiente', tono: 'neutro' });
    expect(estadoTarea('EN_PROGRESO')).toEqual({ texto: 'En progreso', tono: 'info' });
    expect(estadoTarea('COMPLETADA')).toEqual({ texto: 'Completada', tono: 'exito' });
    expect(estadoHito('EN_CURSO')).toEqual({ texto: 'En progreso', tono: 'info' });
    expect(LEYENDA.map((m) => m.texto)).toEqual([
      'Ruta crítica',
      'Prerrequisito crítico',
      'Bloqueada por dependencia',
      'Riesgo alto',
      'Riesgo medio',
      'Riesgo bajo',
    ]);
  });
});
