import { describe, expect, it } from 'vitest';
import type { EntradaHistorial, TareaVista } from '../api/tipos';
import type { HitoDelEquipo } from '../tablero/ubicacion';
import {
  aplicarFiltros,
  diaBogota,
  FILTROS_VACIOS,
  filtrosDeBusqueda,
  hayFiltros,
  opcionesAccion,
  rutaHistorial,
  validarRango,
} from './filtros';

function entrada(parcial: Partial<EntradaHistorial> = {}): EntradaHistorial {
  return {
    idLog: 1,
    fechaHora: '2026-09-23T15:00:00Z',
    idUsuario: 10,
    autor: 'Ana',
    accion: 'CREAR',
    entidad: 'tarea',
    idRegistro: 40,
    campo: null,
    valorAnterior: null,
    valorNuevo: null,
    detalle: '',
    ...parcial,
  };
}

describe('filtros del historial', () => {
  it('URL, ruta, rango y día', () => {
    const acciones = ['CREAR', 'ENVIAR_HITO'];
    const hitos: Pick<HitoDelEquipo, 'idHitoEquipo'>[] = [{ idHitoEquipo: 12 }, { idHitoEquipo: 13 }];

    expect(
      filtrosDeBusqueda('?accion=CREAR&hito=13&desde=2026-09-01&hasta=2026-09-30', acciones, hitos)
    ).toEqual({
      accion: 'CREAR',
      idHitoEquipo: 13,
      desde: '2026-09-01',
      hasta: '2026-09-30',
    });
    expect(filtrosDeBusqueda('?accion=LOGIN_EXITOSO&hito=99&desde=2026-9-1&hasta=x', acciones, hitos)).toEqual(
      FILTROS_VACIOS
    );
    expect(filtrosDeBusqueda('', acciones, hitos)).toEqual(FILTROS_VACIOS);
    // fechas con formato correcto pero imposibles en el calendario se descartan sin lanzar
    expect(filtrosDeBusqueda('?desde=2026-02-30&hasta=2026-13-45', acciones, hitos)).toEqual(FILTROS_VACIOS);
    expect(filtrosDeBusqueda('?hito=abc&accion=', acciones, hitos)).toEqual(FILTROS_VACIOS);

    expect(rutaHistorial(7, FILTROS_VACIOS)).toBe('/equipos/7/historial');
    expect(
      rutaHistorial(7, { accion: 'CREAR', idHitoEquipo: 12, desde: '2026-09-01', hasta: null })
    ).toBe('/equipos/7/historial?accion=CREAR&hito=12&desde=2026-09-01');
    expect(
      rutaHistorial(7, { accion: null, idHitoEquipo: null, desde: null, hasta: '2026-09-30' })
    ).toBe('/equipos/7/historial?hasta=2026-09-30');

    expect(hayFiltros(FILTROS_VACIOS)).toBe(false);
    expect(hayFiltros({ accion: null, idHitoEquipo: null, desde: null, hasta: '2026-09-30' })).toBe(true);

    expect(validarRango('2026-09-23', '2026-09-21')).toBe('La fecha «Hasta» no puede ser anterior a «Desde».');
    expect(validarRango('2026-09-21', '2026-09-21')).toBeUndefined();
    expect(validarRango(null, '2026-09-21')).toBeUndefined();
    expect(validarRango('2026-09-21', null)).toBeUndefined();

    expect(diaBogota('2026-09-23T04:59:00Z')).toBe('2026-09-22');
    expect(diaBogota('2026-09-23T05:00:00.123456Z')).toBe('2026-09-23');
    expect(diaBogota('x')).toBeNull();
  });

  it('aplicar filtros y opciones', () => {
    const entradas: EntradaHistorial[] = [
      entrada({ idLog: 1, entidad: 'hito_equipo', idRegistro: 12, accion: 'ENVIAR_HITO', fechaHora: '2026-09-23T15:00:00Z' }),
      entrada({ idLog: 2, entidad: 'tarea', idRegistro: 40, accion: 'ACTUALIZAR_AVANCE', fechaHora: '2026-09-22T15:00:00Z' }),
      entrada({ idLog: 3, entidad: 'tarea', idRegistro: 41, accion: 'ACTUALIZAR_AVANCE', fechaHora: '2026-09-21T15:00:00Z' }),
      entrada({ idLog: 4, entidad: 'miembro_equipo', idRegistro: 7, accion: 'CREAR', fechaHora: '2026-09-20T15:00:00Z' }),
      entrada({ idLog: 5, entidad: 'tarea', idRegistro: 99, accion: 'ELIMINAR', fechaHora: '2026-09-19T15:00:00Z' }),
      entrada({ idLog: 6, entidad: 'equipo', idRegistro: 7, accion: 'CREAR', fechaHora: 'x' }),
    ];

    const tareasPorHito = new Map<number, Pick<TareaVista, 'idTarea'>[]>([
      [12, [{ idTarea: 40 }]],
      [13, [{ idTarea: 41 }]],
    ]);

    const ids = (resultado: EntradaHistorial[]) => resultado.map((e) => e.idLog);

    expect(ids(aplicarFiltros(entradas, FILTROS_VACIOS, tareasPorHito))).toEqual([1, 2, 3, 4, 5, 6]);
    expect(ids(aplicarFiltros(entradas, { ...FILTROS_VACIOS, accion: 'CREAR' }, tareasPorHito))).toEqual([4, 6]);
    expect(ids(aplicarFiltros(entradas, { ...FILTROS_VACIOS, idHitoEquipo: 12 }, tareasPorHito))).toEqual([1, 2]);
    expect(ids(aplicarFiltros(entradas, { ...FILTROS_VACIOS, idHitoEquipo: 13 }, tareasPorHito))).toEqual([3]);
    expect(ids(aplicarFiltros(entradas, { ...FILTROS_VACIOS, desde: '2026-09-21' }, tareasPorHito))).toEqual([1, 2, 3]);
    expect(ids(aplicarFiltros(entradas, { ...FILTROS_VACIOS, hasta: '2026-09-21' }, tareasPorHito))).toEqual([3, 4, 5]);
    expect(
      ids(aplicarFiltros(entradas, { ...FILTROS_VACIOS, desde: '2026-09-21', hasta: '2026-09-22' }, tareasPorHito))
    ).toEqual([2, 3]);
    expect(
      ids(aplicarFiltros(entradas, { ...FILTROS_VACIOS, desde: '2026-09-23', hasta: '2026-09-21' }, tareasPorHito))
    ).toEqual([]);
    expect(
      ids(aplicarFiltros(entradas, { ...FILTROS_VACIOS, accion: 'ACTUALIZAR_AVANCE', idHitoEquipo: 12 }, tareasPorHito))
    ).toEqual([2]);

    expect(opcionesAccion(entradas)).toEqual([
      { valor: 'ACTUALIZAR_AVANCE', texto: 'Actualizó el avance' },
      { valor: 'CREAR', texto: 'Creó' },
      { valor: 'ELIMINAR', texto: 'Eliminó' },
      { valor: 'ENVIAR_HITO', texto: 'Envió el hito a revisión' },
    ]);
  });
});
