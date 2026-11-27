import { describe, expect, it } from 'vitest';
import type { ProyectoDetalle } from '../api/tipos';
import {
  BORRADOR_VACIO,
  borradorDe,
  claveSeleccionada,
  conCriterio,
  conCriterioNuevo,
  conHito,
  nuevoHito,
  sinAlerta,
  sinCriterio,
} from './borrador';

const detalle: ProyectoDetalle = {
  proyecto: {
    idProyecto: 1,
    idDocente: 10,
    nombre: 'Diseño de Software I',
    descripcion: null,
    fechaInicio: '2026-08-01',
    fechaFin: '2026-11-30',
    usaMonitores: true,
    equiposMixtos: true,
    cierreEquipos: null,
    horasRonda: 48,
  },
  hitos: [
    {
      idHito: 11,
      idProyecto: 1,
      nombreHito: 'Análisis',
      descripcion: null,
      orden: 1,
      fechaInicio: '2026-08-01',
      fechaLimite: '2026-08-29',
      diasInactividadAlerta: 5,
      notificarDocenteInactividad: true,
      criterios: [{ idCriterio: 21, idHito: 11, descripcion: 'Claridad', pesoPorcentaje: 33.5, puntajeMaximo: 5 }],
    },
  ],
  equipos: [],
};

describe('borrador del proyecto', () => {
  it('se arma desde el detalle con textos para los campos', () => {
    expect(borradorDe(detalle)).toEqual({
      nombre: 'Diseño de Software I',
      descripcion: '',
      fechaInicio: '2026-08-01',
      fechaFin: '2026-11-30',
      usaMonitores: true,
      equiposMixtos: true,
      hitos: [
        {
          clave: 'h11',
          idHito: 11,
          orden: 1,
          nombre: 'Análisis',
          descripcion: null,
          fechaInicio: '2026-08-01',
          fechaLimite: '2026-08-29',
          diasInactividad: '5',
          notificarDocente: true,
          criterios: [{ clave: 'c21', idCriterio: 21, descripcion: 'Claridad', peso: '33.5', puntaje: '5' }],
        },
      ],
    });
    expect(claveSeleccionada(null, borradorDe(detalle))).toBe('h11');
    expect(claveSeleccionada('n9', borradorDe(detalle))).toBe('h11');
    expect(claveSeleccionada(null, BORRADOR_VACIO)).toBeNull();
  });

  it('nuevo hito con el siguiente orden y 3 días; los cambios no mutan el borrador', () => {
    const b = borradorDe(detalle);
    const h = nuevoHito(b, 'n1');
    expect(h).toEqual({
      clave: 'n1',
      idHito: null,
      orden: 2,
      nombre: '',
      descripcion: null,
      fechaInicio: '',
      fechaLimite: '',
      diasInactividad: '3',
      notificarDocente: false,
      criterios: [],
    });
    const conNuevo = { ...b, hitos: [...b.hitos, h] };
    const editado = conCriterio(
      conCriterioNuevo(conHito(conNuevo, 'n1', { nombre: 'Diseño' }), 'n1', 'n2'),
      'n1',
      'n2',
      { peso: '40' },
    );
    expect(editado.hitos[1].nombre).toBe('Diseño');
    expect(editado.hitos[1].criterios).toEqual([{ clave: 'n2', idCriterio: null, descripcion: '', peso: '40', puntaje: '' }]);
    expect(sinCriterio(editado, 'n1', 'n2').hitos[1].criterios).toEqual([]);
    expect(conNuevo.hitos[1].nombre).toBe('');
    expect(b.hitos).toHaveLength(1);
  });

  it('hito sin alerta de inactividad se carga con campo vacío', () => {
    const detalleSinAlerta: ProyectoDetalle = {
      ...detalle,
      hitos: [
        {
          ...detalle.hitos[0],
          diasInactividadAlerta: null,
          notificarDocenteInactividad: true,
        },
      ],
    };
    const b = borradorDe(detalleSinAlerta);
    expect(b.hitos[0].diasInactividad).toBe('');
    expect(b.hitos[0].notificarDocente).toBe(true);
    expect(sinAlerta('')).toBe(true);
    expect(sinAlerta('   ')).toBe(true);
    expect(sinAlerta('3')).toBe(false);
  });
});
