import { describe, expect, it } from 'vitest';
import {
  avanceDelHito,
  codigosDeTareas,
  estadoHito,
  estadoTarea,
  LEYENDA,
  marcaPrincipal,
  nombreConCodigo,
} from './marcas';

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

  it('marcaPrincipal devuelve una sola marca por fila según prioridad y oculta riesgo bajo y completadas', () => {
    expect(marcaPrincipal({ estado: 'COMPLETADA', bloqueada: true, nivelRiesgo: 'ALTO' })).toBeNull();
    expect(marcaPrincipal({ estado: 'PENDIENTE', bloqueada: true, nivelRiesgo: 'ALTO' })).toEqual({
      texto: 'Bloqueada por dependencia',
      tono: 'neutro',
    });
    expect(marcaPrincipal({ estado: 'EN_PROGRESO', bloqueada: false, nivelRiesgo: 'ALTO' })).toEqual({
      texto: 'Riesgo alto',
      tono: 'peligro',
    });
    expect(marcaPrincipal({ estado: 'EN_PROGRESO', bloqueada: false, nivelRiesgo: 'MEDIO' })).toEqual({
      texto: 'Riesgo medio',
      tono: 'aviso',
    });
    expect(marcaPrincipal({ estado: 'EN_PROGRESO', bloqueada: false, nivelRiesgo: 'BAJO' })).toBeNull();
    expect(marcaPrincipal({ estado: 'PENDIENTE', bloqueada: false, nivelRiesgo: null })).toBeNull();
  });

  it('avance del hito: promedio simple redondeado (Figma: 100, 60, 80, 0, 0 → 48 %)', () => {
    const avance = (lista: number[]) => avanceDelHito(lista.map((porcentajeAvance) => ({ porcentajeAvance })));
    expect(avance([100, 60, 80, 0, 0])).toBe(48);
    expect(avance([33, 34])).toBe(34);
    expect(avance([])).toBe(0);
  });

  it('estados de tarea y de hito, y leyenda plegable con franja y marcas', () => {
    expect(estadoTarea('PENDIENTE')).toEqual({ texto: 'Pendiente', tono: 'neutro' });
    expect(estadoTarea('EN_PROGRESO')).toEqual({ texto: 'En progreso', tono: 'info' });
    expect(estadoTarea('COMPLETADA')).toEqual({ texto: 'Completada', tono: 'exito' });
    expect(estadoHito('EN_CURSO')).toEqual({ texto: 'En progreso', tono: 'info' });
    expect(LEYENDA).toEqual([
      {
        marca: null,
        titulo: 'Franja violeta · Ruta crítica',
        explicacion: 'Si esta tarea se atrasa, se atrasa la entrega del hito.',
      },
      {
        marca: { texto: 'Bloqueada por dependencia', tono: 'neutro' },
        titulo: 'Bloqueada por dependencia',
        explicacion: 'Espera a que termine otra tarea antes de poder avanzar.',
      },
      {
        marca: { texto: 'Riesgo alto', tono: 'peligro' },
        titulo: 'Riesgo alto',
        explicacion:
          'Con el trabajo que falta no alcanza a terminar antes de su fecha límite (4 h de trabajo por día).',
      },
      {
        marca: { texto: 'Riesgo medio', tono: 'aviso' },
        titulo: 'Riesgo medio',
        explicacion: 'Alcanza, pero con poco margen.',
      },
    ]);
  });
});
