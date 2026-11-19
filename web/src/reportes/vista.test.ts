import { describe, expect, it } from 'vitest';
import type { EntradaHistorial } from '../api/tipos';
import {
  ACCIONES,
  accionConEntidad,
  filtrarPorHito,
  resumenEquipo,
  resumenHito,
  textoAccion,
  textoEntidad,
} from './vista';

function entrada(parcial: Partial<EntradaHistorial> = {}): EntradaHistorial {
  return {
    idLog: 1,
    fechaHora: '2026-09-23T15:00:00Z',
    idUsuario: null,
    autor: 'Ana',
    accion: 'CREAR',
    entidad: 'tarea',
    idRegistro: null,
    campo: null,
    valorAnterior: null,
    valorNuevo: null,
    detalle: '',
    ...parcial,
  };
}

describe('vista de reportes', () => {
  it('traduce acciones y entidades de auditoria', () => {
    expect(textoAccion('ACTUALIZAR_AVANCE')).toBe('Actualizó el avance');
    expect(textoAccion('EXPORTAR_REPORTE')).toBe('Exportó el reporte');
    expect(textoAccion('ACCION_NUEVA')).toBe('ACCION_NUEVA');
    expect(Object.keys(ACCIONES).length).toBe(27);

    expect(textoEntidad('miembro_equipo')).toBe('Integrantes');
    expect(textoEntidad('evaluacion_hito')).toBe('evaluacion_hito');

    expect(accionConEntidad({ accion: 'ENVIAR_HITO', entidad: 'hito_equipo' })).toBe('Envió el hito a revisión · Hito');
  });

  it('filtra entradas del historial por hito y tareas actuales', () => {
    const entradas = [
      entrada({ idLog: 1, entidad: 'hito_equipo', idRegistro: 12 }),
      entrada({ idLog: 2, entidad: 'hito_equipo', idRegistro: 13 }),
      entrada({ idLog: 3, entidad: 'tarea', idRegistro: 40 }),
      entrada({ idLog: 4, entidad: 'tarea', idRegistro: 41 }),
      entrada({ idLog: 5, entidad: 'equipo', idRegistro: 12 }),
      entrada({ idLog: 6, entidad: 'tarea', idRegistro: null }),
      entrada({ idLog: 7, entidad: 'tarea', idRegistro: 40 }),
    ];

    const filtradas = filtrarPorHito(entradas, 12, [{ idTarea: 40 }]);
    expect(filtradas.map((e) => e.idLog)).toEqual([1, 3, 7]);

    const sinTareas = filtrarPorHito(entradas, 12, []);
    expect(sinTareas.map((e) => e.idLog)).toEqual([1]);
  });

  it('calcula los datos de resumen para el equipo y para un hito', () => {
    const miembros = [
      { idUsuario: 1, nombre: 'Luis', rolEquipo: 'INTEGRANTE' as const },
      { idUsuario: 2, nombre: 'Ana', rolEquipo: 'LIDER' as const },
      { idUsuario: 3, nombre: 'Eva', rolEquipo: 'INTEGRANTE' as const },
    ];
    const hitos = [{ estado: 'EVALUADO' as const }, { estado: 'ENVIADO' as const }, { estado: 'EN_CURSO' as const }];
    const historial = [{ fechaHora: '2026-09-23T15:00:00Z' }, { fechaHora: '2026-09-20T15:00:00Z' }];

    expect(resumenEquipo({ miembros }, hitos, historial)).toEqual([
      {
        etiqueta: 'Integrantes',
        valor: '3',
        primario: false,
        marca: null,
        detalle: 'Líder: Ana',
      },
      {
        etiqueta: 'Hitos evaluados',
        valor: '1 de 3',
        primario: false,
        marca: null,
        detalle: '1 en revisión',
      },
      {
        etiqueta: 'Cambios registrados',
        valor: '2',
        primario: true,
        marca: null,
        detalle: 'Último: 23/09/2026 10:00',
      },
    ]);

    expect(resumenEquipo({ miembros: [] }, [], [])).toEqual([
      {
        etiqueta: 'Integrantes',
        valor: '0',
        primario: false,
        marca: null,
        detalle: 'Sin líder',
      },
      {
        etiqueta: 'Hitos evaluados',
        valor: '0 de 0',
        primario: false,
        marca: null,
        detalle: 'Ninguno en revisión',
      },
      {
        etiqueta: 'Cambios registrados',
        valor: '0',
        primario: true,
        marca: null,
        detalle: 'Sin cambios',
      },
    ]);

    const milEntradas = Array.from({ length: 1000 }, () => ({
      fechaHora: '2026-09-23T15:00:00Z',
    }));
    expect(resumenEquipo({ miembros: [] }, [], milEntradas)[2].valor).toBe('1000 o más');

    const tablero = { estado: 'ENVIADO' as const, fechaLimite: '2026-09-22' };
    const tareas = [
      { estado: 'COMPLETADA' as const, porcentajeAvance: 100, horasEstimadas: 1, bloqueada: false },
      { estado: 'EN_PROGRESO' as const, porcentajeAvance: 50, horasEstimadas: 1, bloqueada: true },
      { estado: 'PENDIENTE' as const, porcentajeAvance: 0, horasEstimadas: 1, bloqueada: false },
    ];

    expect(resumenHito(tablero, tareas, null)).toEqual([
      {
        etiqueta: 'Estado del hito',
        valor: null,
        primario: false,
        marca: { texto: 'En Revisión', tono: 'critico' },
        detalle: 'Fecha límite: 22/09/2026',
      },
      {
        etiqueta: 'Avance',
        valor: '50 %',
        primario: true,
        marca: null,
        detalle: '1 de 3 tareas completadas · 1 bloqueada',
      },
      {
        etiqueta: 'Evaluación',
        valor: 'Sin evaluar',
        primario: false,
        marca: null,
        detalle: null,
      },
    ]);

    const tareasDosBloqueadas = [
      { estado: 'COMPLETADA' as const, porcentajeAvance: 100, horasEstimadas: 1, bloqueada: false },
      { estado: 'EN_PROGRESO' as const, porcentajeAvance: 50, horasEstimadas: 1, bloqueada: true },
      { estado: 'PENDIENTE' as const, porcentajeAvance: 0, horasEstimadas: 1, bloqueada: true },
    ];
    expect(resumenHito(tablero, tareasDosBloqueadas, null)[1].detalle).toBe('1 de 3 tareas completadas · 2 bloqueadas');

    expect(resumenHito(tablero, [], null)[1]).toEqual({
      etiqueta: 'Avance',
      valor: '0 %',
      primario: true,
      marca: null,
      detalle: 'Sin tareas',
    });

    const evaluacion = {
      notaGrupal: 4.15,
      caracter: 'PROVISIONAL' as const,
      fechaEvaluacion: '2026-09-23T15:00:00Z',
    };
    expect(resumenHito(tablero, tareas, evaluacion)[2]).toEqual({
      etiqueta: 'Evaluación',
      valor: '4,15',
      primario: false,
      marca: { texto: 'Provisional', tono: 'aviso' },
      detalle: 'Evaluada el 23/09/2026',
    });
  });
});
