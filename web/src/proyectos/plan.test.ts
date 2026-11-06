import { describe, expect, it } from 'vitest';
import type { HitoVista, ProyectoDetalle } from '../api/tipos';
import { borradorDe } from './borrador';
import type { BorradorProyecto } from './borrador';
import { planDeGuardado, solicitudHito } from './plan';
import type { Operacion } from './plan';

const hitoVista = (idHito: number, orden: number, fechaInicio: string, fechaLimite: string,
                   criterios: [number, number][] = []): HitoVista => ({
  idHito,
  idProyecto: 1,
  nombreHito: 'Hito ' + orden,
  descripcion: 'Entrega ' + orden,
  orden,
  fechaInicio,
  fechaLimite,
  diasInactividadAlerta: 3,
  notificarDocenteInactividad: false,
  criterios: criterios.map(([idCriterio, peso]) => ({
    idCriterio,
    idHito,
    descripcion: 'Criterio ' + idCriterio,
    pesoPorcentaje: peso,
    puntajeMaximo: 5,
  })),
});

const servidor: ProyectoDetalle = {
  proyecto: {
    idProyecto: 1,
    idDocente: 10,
    nombre: 'Diseño de Software I',
    descripcion: null,
    fechaInicio: '2026-08-01',
    fechaFin: '2026-11-30',
    usaMonitores: false,
  },
  hitos: [
    hitoVista(11, 1, '2026-08-01', '2026-08-29', [[21, 50], [22, 50]]),
    hitoVista(12, 2, '2026-09-01', '2026-11-30'),
  ],
  equipos: [],
};

const resumen = (plan: Operacion[]) =>
  plan.map((o) => o.tipo + ('idCriterio' in o ? ':' + o.idCriterio : 'claveHito' in o ? ':' + o.claveHito : ''));

describe('plan de guardado', () => {
  it('sin cambios no hay operaciones', () => {
    expect(planDeGuardado(servidor, borradorDe(servidor))).toEqual([]);
  });

  it('proyecto nuevo: primero el proyecto, luego cada hito y su rúbrica', () => {
    const b: BorradorProyecto = {
      nombre: ' Proyecto nuevo ',
      descripcion: '',
      fechaInicio: '2026-08-01',
      fechaFin: '2026-11-30',
      usaMonitores: true,
      hitos: [
        {
          clave: 'n1',
          idHito: null,
          orden: 1,
          nombre: 'Análisis',
          descripcion: null,
          fechaInicio: '2026-08-01',
          fechaLimite: '2026-08-29',
          diasInactividad: '3',
          notificarDocente: true,
          criterios: [{ clave: 'n2', idCriterio: null, descripcion: 'Claridad', peso: '100', puntaje: '5' }],
        },
      ],
    };
    expect(planDeGuardado(null, b)).toEqual([
      {
        tipo: 'crearProyecto',
        solicitud: { nombre: 'Proyecto nuevo', descripcion: null, fechaInicio: '2026-08-01', fechaFin: '2026-11-30', usaMonitores: true },
      },
      {
        tipo: 'crearHito',
        claveHito: 'n1',
        solicitud: {
          nombreHito: 'Análisis',
          descripcion: null,
          orden: 1,
          fechaInicio: '2026-08-01',
          fechaLimite: '2026-08-29',
          diasInactividadAlerta: 3,
          notificarDocenteInactividad: true,
        },
      },
      {
        tipo: 'crearCriterio',
        claveHito: 'n1',
        idHito: null,
        claveCriterio: 'n2',
        solicitud: { descripcion: 'Claridad', pesoPorcentaje: 100, puntajeMaximo: 5 },
      },
    ]);
  });

  it('rúbrica: borra, baja pesos, sube pesos y crea, en ese orden', () => {
    const conTres: ProyectoDetalle = {
      ...servidor,
      hitos: [hitoVista(11, 1, '2026-08-01', '2026-08-29', [[21, 40], [22, 30], [23, 30]])],
    };
    const b = borradorDe(conTres);
    const [, c22, c23] = b.hitos[0].criterios;
    b.hitos[0].criterios = [
      { ...c22, peso: '20' },
      { ...c23, peso: '50' },
      { clave: 'n9', idCriterio: null, descripcion: 'Nuevo', peso: '30', puntaje: '5' },
    ];
    expect(resumen(planDeGuardado(conTres, b))).toEqual([
      'eliminarCriterio:21',
      'actualizarCriterio:22',
      'actualizarCriterio:23',
      'crearCriterio:h11',
    ]);
  });

  it('fechas: el hito que cabe en el proyecto guardado va antes del proyecto; el que no, después', () => {
    const b = borradorDe(servidor);
    b.fechaInicio = '2026-08-15';
    b.fechaFin = '2026-12-15';
    b.hitos[0] = { ...b.hitos[0], fechaInicio: '2026-08-15', fechaLimite: '2026-09-15' };
    b.hitos[1] = { ...b.hitos[1], fechaLimite: '2026-12-15' };
    expect(resumen(planDeGuardado(servidor, b))).toEqual(['actualizarHito:h11', 'actualizarProyecto', 'actualizarHito:h12']);
  });

  it('el hito se reenvía completo (PUT reemplaza): conserva la descripción y convierte los días', () => {
    const h = borradorDe(servidor).hitos[0];
    expect(solicitudHito({ ...h, nombre: ' Análisis ', diasInactividad: ' 7 ' })).toEqual({
      nombreHito: 'Análisis',
      descripcion: 'Entrega 1',
      orden: 1,
      fechaInicio: '2026-08-01',
      fechaLimite: '2026-08-29',
      diasInactividadAlerta: 7,
      notificarDocenteInactividad: false,
    });
  });

  it('días vacíos en el plan: solicitud con null y notificación false, sin cambios no genera plan, y actualiza a null', () => {
    const h = borradorDe(servidor).hitos[0];
    expect(solicitudHito({ ...h, diasInactividad: '', notificarDocente: true })).toEqual({
      nombreHito: 'Hito 1',
      descripcion: 'Entrega 1',
      orden: 1,
      fechaInicio: '2026-08-01',
      fechaLimite: '2026-08-29',
      diasInactividadAlerta: null,
      notificarDocenteInactividad: false,
    });
    const servSinAlerta: ProyectoDetalle = {
      ...servidor,
      hitos: [
        { ...servidor.hitos[0], diasInactividadAlerta: null, notificarDocenteInactividad: true },
        servidor.hitos[1],
      ],
    };
    expect(planDeGuardado(servSinAlerta, borradorDe(servSinAlerta))).toEqual([]);
    const b = borradorDe(servidor);
    b.hitos[0] = { ...b.hitos[0], diasInactividad: '' };
    const plan = planDeGuardado(servidor, b);
    expect(plan).toHaveLength(1);
    expect(plan[0].tipo).toBe('actualizarHito');
    if (plan[0].tipo === 'actualizarHito') {
      expect(plan[0].solicitud.diasInactividadAlerta).toBeNull();
    }
  });
});
