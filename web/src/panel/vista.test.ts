import { describe, expect, it } from 'vitest';
import type {
  EquipoVista,
  HitoVista,
  IntegranteInactivo,
  PanelEquipo,
  PanelProyecto,
  ProyectoDetalle,
  ProyectoVista,
  TareaBloqueada,
} from '../api/tipos';
import {
  alertasPorEquipo,
  estadoBotonPanel,
  estadoEquipo,
  marcaCelda,
  matrizHitos,
  porEvaluar,
  proyectosPropios,
  resumenPanel,
  textoActualizado,
  textoBloqueo,
  textoDias,
  textoInactivo,
  REQUIERE_ATENCION,
} from './vista';

function detalle(parcial: Partial<ProyectoDetalle> = {}): ProyectoDetalle {
  return {
    proyecto: {
      idProyecto: 3,
      idDocente: 5,
      nombre: 'Proyecto 3',
      descripcion: null,
      fechaInicio: '2026-08-01',
      fechaFin: '2026-11-30',
      usaMonitores: false,
      ...parcial.proyecto,
    },
    hitos: parcial.hitos ?? [],
    equipos: parcial.equipos ?? [],
  };
}

function equipoPanel(parcial: Partial<PanelEquipo> = {}): PanelEquipo {
  return {
    idEquipo: 1,
    nombreEquipo: 'Equipo 1',
    enRojo: false,
    tareasTotales: 0,
    tareasCompletadas: 0,
    tareasRiesgoAlto: 0,
    avancePromedio: 0,
    integrantesInactivos: [],
    tareasBloqueadas: [],
    ...parcial,
  };
}

describe('vista', () => {
  const hitosReferencia: HitoVista[] = [
    {
      idHito: 1,
      idProyecto: 3,
      orden: 2,
      nombreHito: 'Diseño',
      descripcion: null,
      fechaInicio: '2026-09-21',
      fechaLimite: '2026-10-10',
      diasInactividadAlerta: null,
      notificarDocenteInactividad: false,
      criterios: [],
    },
    {
      idHito: 2,
      idProyecto: 3,
      orden: 1,
      nombreHito: 'Requisitos',
      descripcion: null,
      fechaInicio: '2026-09-01',
      fechaLimite: '2026-09-20',
      diasInactividadAlerta: null,
      notificarDocenteInactividad: false,
      criterios: [],
    },
  ];

  const equiposReferencia: EquipoVista[] = [
    {
      idEquipo: 8,
      idProyecto: 3,
      nombreEquipo: 'Beta',
      miembros: [],
      hitos: [
        { idHitoEquipo: 81, idHito: 2, estado: 'ENVIADO' },
        { idHitoEquipo: 82, idHito: 1, estado: 'ENVIADO' },
      ],
    },
    {
      idEquipo: 7,
      idProyecto: 3,
      nombreEquipo: 'Alfa',
      miembros: [],
      hitos: [
        { idHitoEquipo: 71, idHito: 2, estado: 'ENVIADO' },
        { idHitoEquipo: 72, idHito: 1, estado: 'EN_CURSO' },
      ],
    },
    {
      idEquipo: 9,
      idProyecto: 3,
      nombreEquipo: 'Gamma',
      miembros: [],
      hitos: [{ idHitoEquipo: 91, idHito: 99, estado: 'ENVIADO' }],
    },
  ];

  const datosReferencia = detalle({
    hitos: hitosReferencia,
    equipos: equiposReferencia,
  });

  it('obtiene hitos por evaluar ordenados y filtra proyectos propios del docente', () => {
    const pendientes = porEvaluar(datosReferencia);
    expect(pendientes.map((p) => p.idHitoEquipo)).toEqual([71, 81, 82]);
    expect(pendientes[0]).toEqual({
      idHitoEquipo: 71,
      idEquipo: 7,
      nombreEquipo: 'Alfa',
      orden: 1,
      nombreHito: 'Requisitos',
      fechaLimite: '2026-09-20',
    });
    expect(pendientes.some((p) => p.nombreEquipo === 'Gamma')).toBe(false);

    const proyectos: ProyectoVista[] = [
      {
        idProyecto: 1,
        idDocente: 5,
        nombre: 'P1',
        descripcion: null,
        fechaInicio: '2026-01-01',
        fechaFin: '2026-06-01',
        usaMonitores: false,
      },
      {
        idProyecto: 2,
        idDocente: 6,
        nombre: 'P2',
        descripcion: null,
        fechaInicio: '2026-01-01',
        fechaFin: '2026-06-01',
        usaMonitores: false,
      },
      {
        idProyecto: 3,
        idDocente: 5,
        nombre: 'P3',
        descripcion: null,
        fechaInicio: '2026-01-01',
        fechaFin: '2026-06-01',
        usaMonitores: false,
      },
    ];
    expect(proyectosPropios(proyectos, 5).map((p) => p.idProyecto)).toEqual([1, 3]);
  });

  it('calcula estado del equipo, resumen del panel, formato de textos y alertas', () => {
    expect(estadoEquipo({ enRojo: true, tareasRiesgoAlto: 2 })).toEqual(REQUIERE_ATENCION);
    expect(estadoEquipo({ enRojo: false, tareasRiesgoAlto: 2 })).toEqual({
      texto: 'Tareas en riesgo',
      tono: 'aviso',
    });
    expect(estadoEquipo({ enRojo: false, tareasRiesgoAlto: 0 })).toEqual({
      texto: 'Al día',
      tono: 'exito',
    });

    const eqA = equipoPanel({ idEquipo: 10, nombreEquipo: 'A', enRojo: true, tareasTotales: 10, avancePromedio: 40 });
    const eqB = equipoPanel({ idEquipo: 20, nombreEquipo: 'B', enRojo: false, tareasTotales: 4, avancePromedio: 70 });
    const eqC = equipoPanel({ idEquipo: 30, nombreEquipo: 'C', enRojo: false, tareasTotales: 0, avancePromedio: 0 });

    const panelABC: PanelProyecto = {
      idProyecto: 3,
      nombre: 'P',
      generadoEn: '2026-09-23T19:05:00Z',
      equipos: [eqA, eqB, eqC],
    };
    expect(resumenPanel(panelABC, 2)).toEqual({ enRojo: 1, equipos: 3, porEvaluar: 2, avance: 55 });

    const panelC: PanelProyecto = {
      idProyecto: 3,
      nombre: 'P',
      generadoEn: '2026-09-23T19:05:00Z',
      equipos: [eqC],
    };
    expect(resumenPanel(panelC, 2).avance).toBeNull();

    expect(textoDias(1)).toBe('1 día');
    expect(textoDias(3)).toBe('3 días');

    const inactivo: IntegranteInactivo = {
      idUsuario: 4,
      nombre: 'Elena Ruiz',
      ultimaActividad: '2026-09-20T15:00:00Z',
      diasSinActividad: 4,
    };
    expect(textoInactivo(inactivo)).toBe('Elena Ruiz · sin actividad hace 4 días (última: 20/09/2026)');

    expect(textoBloqueo(50)).toBe('bloqueada hace 2 días');
    expect(textoBloqueo(72)).toBe('bloqueada hace 3 días');
    expect(textoBloqueo(47)).toBe('bloqueada hace 47 h');

    const bloqueada: TareaBloqueada = {
      idTarea: 100,
      titulo: 'T1',
      fechaBloqueo: '2026-09-20T15:00:00Z',
      horasBloqueada: 50,
    };
    const eqAlertA = equipoPanel({ idEquipo: 1, nombreEquipo: 'A', integrantesInactivos: [inactivo] });
    const eqAlertB = equipoPanel({ idEquipo: 2, nombreEquipo: 'B' });
    const eqAlertD = equipoPanel({ idEquipo: 4, nombreEquipo: 'D', tareasBloqueadas: [bloqueada] });

    const panelAlertas: PanelProyecto = {
      idProyecto: 3,
      nombre: 'P',
      generadoEn: '2026-09-23T19:05:00Z',
      equipos: [eqAlertA, eqAlertB, eqAlertD],
    };
    expect(alertasPorEquipo(panelAlertas).map((a) => a.idEquipo)).toEqual([1, 4]);

    expect(textoActualizado('2026-09-23T19:05:00Z')).toBe('Actualizado el 23/09/2026 14:05');
  });

  it('determina marcas de celdas y genera la matriz de hitos por equipo', () => {
    expect(marcaCelda('EN_CURSO', '2026-09-22', '2026-09-22')).toEqual({
      texto: 'En progreso',
      tono: 'info',
    });
    expect(marcaCelda('EN_CURSO', '2026-09-22', '2026-09-23')).toEqual({
      texto: 'Vencido sin enviar',
      tono: 'peligro',
    });
    expect(marcaCelda('ENVIADO', '2026-09-22', '2026-09-30')).toEqual({
      texto: 'En Revisión',
      tono: 'critico',
    });
    expect(marcaCelda('EVALUADO', '2026-09-22', '2026-09-30')).toEqual({
      texto: 'Evaluado',
      tono: 'exito',
    });

    const matriz = matrizHitos(datosReferencia, '2026-09-25');
    expect(matriz.columnas).toEqual([
      { idHito: 2, etiqueta: 'Hito 1 · Requisitos' },
      { idHito: 1, etiqueta: 'Hito 2 · Diseño' },
    ]);
    expect(matriz.filas.map((f) => f.nombreEquipo)).toEqual(['Alfa', 'Beta', 'Gamma']);
    expect(matriz.filas[0].celdas.map((c) => c?.texto ?? null)).toEqual(['En Revisión', 'En progreso']);
    expect(matriz.filas[0].celdas.map((c) => c?.idHitoEquipo ?? null)).toEqual([71, 72]);
    expect(matriz.filas[1].celdas.map((c) => c?.texto ?? null)).toEqual(['En Revisión', 'En Revisión']);
    expect(matriz.filas[1].celdas.map((c) => c?.idHitoEquipo ?? null)).toEqual([81, 82]);
    expect(matriz.filas[2].celdas.map((c) => c?.texto ?? null)).toEqual([null, null]);
    expect(datosReferencia.hitos.map((h) => h.idHito)).toEqual([1, 2]);
  });

  it('estadoBotonPanel decide el texto y estado deshabilitado para actualizar y reintentar', () => {
    expect(estadoBotonPanel('actualizar', false)).toEqual({
      texto: 'Actualizar',
      deshabilitado: false,
    });
    expect(estadoBotonPanel('actualizar', true)).toEqual({
      texto: 'Actualizando…',
      deshabilitado: true,
    });
    expect(estadoBotonPanel('reintentar', false)).toEqual({
      texto: 'Reintentar',
      deshabilitado: false,
    });
    expect(estadoBotonPanel('reintentar', true)).toEqual({
      texto: 'Reintentando…',
      deshabilitado: true,
    });
  });
});
