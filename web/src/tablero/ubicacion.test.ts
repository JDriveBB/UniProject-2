import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { EquipoVista, HitoEquipoVista, HitoVista, MiembroVista, ProyectoDetalle } from '../api/tipos';
import {
  equiposDelUsuario,
  etiquetaHito,
  hitoActual,
  hitoElegido,
  hitosDelEquipo,
  mensajeErrorAbrir,
  rolEnEquipo,
  SIN_ACCESO_EQUIPO,
  ubicarEquipo,
} from './ubicacion';
import type { ApiUbicacion, EquipoUbicado, HitoDelEquipo } from './ubicacion';

function hito(idHito: number, orden: number, nombreHito: string): HitoVista {
  return {
    idHito,
    idProyecto: 1,
    nombreHito,
    descripcion: null,
    orden,
    fechaInicio: '2026-08-01',
    fechaLimite: '2026-08-29',
    diasInactividadAlerta: 3,
    notificarDocenteInactividad: false,
    criterios: [],
  };
}

function equipo(idEquipo: number, miembros: number[], hitos: HitoEquipoVista[] = []): EquipoVista {
  return {
    idEquipo,
    idProyecto: 1,
    nombreEquipo: 'Equipo ' + idEquipo,
    miembros: miembros.map((idUsuario, i): MiembroVista => ({
      idUsuario,
      nombre: 'Persona ' + idUsuario,
      rolEquipo: i === 0 ? 'LIDER' : 'INTEGRANTE',
    })),
    hitos,
  };
}

function proyecto(idProyecto: number, equipos: EquipoVista[]): ProyectoDetalle {
  return {
    proyecto: {
      idProyecto,
      idDocente: 9,
      nombre: 'Proyecto ' + idProyecto,
      descripcion: null,
      fechaInicio: '2026-08-01',
      fechaFin: '2026-12-01',
      usaMonitores: false,
    },
    hitos: [],
    equipos,
  };
}

function apiFalsa(detalles: ProyectoDetalle[], pedidos: number[]): ApiUbicacion {
  return {
    async listarProyectos() {
      return detalles.map((d) => d.proyecto);
    },
    async detalleProyecto(idProyecto) {
      pedidos.push(idProyecto);
      const d = detalles.find((x) => x.proyecto.idProyecto === idProyecto);
      if (d === undefined) {
        throw new ErrorApi(403, 'ACCESO_DENEGADO', 'No tienes permiso para esta acción');
      }
      return d;
    },
  };
}

describe('ubicación del equipo', () => {
  it('ubicarEquipo busca proyecto por proyecto y para en el primero que tiene el equipo; null si ninguno', async () => {
    const detalles = [proyecto(1, [equipo(10, [5])]), proyecto(2, [equipo(20, [6])]), proyecto(3, [])];
    const pedidos: number[] = [];
    const u = await ubicarEquipo(apiFalsa(detalles, pedidos), 20);
    expect(u === null ? null : [u.proyecto.idProyecto, u.equipo.idEquipo]).toEqual([2, 20]);
    expect(pedidos).toEqual([1, 2]);
    expect(await ubicarEquipo(apiFalsa(detalles, []), 99)).toBeNull();
  });

  it('equiposDelUsuario junta todos los equipos donde el usuario es integrante', async () => {
    const detalles = [proyecto(1, [equipo(10, [5, 7]), equipo(11, [8])]), proyecto(2, [equipo(20, [7])])];
    const api = apiFalsa(detalles, []);
    expect((await equiposDelUsuario(api, 7)).map((u) => u.equipo.idEquipo)).toEqual([10, 20]);
    expect(await equiposDelUsuario(api, 99)).toEqual([]);
  });

  it('hitosDelEquipo ordena por orden; hitoActual es el primero en curso o, si no hay, el último', () => {
    const u: EquipoUbicado = {
      proyecto: proyecto(1, []).proyecto,
      hitos: [hito(3, 3, 'Implementación'), hito(1, 1, 'Análisis'), hito(2, 2, 'Diseño')],
      equipo: equipo(
        10,
        [5],
        [
          { idHitoEquipo: 103, idHito: 3, estado: 'EN_CURSO', fechaEnvio: null },
          { idHitoEquipo: 101, idHito: 1, estado: 'EVALUADO', fechaEnvio: null },
          { idHitoEquipo: 102, idHito: 2, estado: 'EN_CURSO', fechaEnvio: null },
          { idHitoEquipo: 199, idHito: 9, estado: 'EN_CURSO', fechaEnvio: null },
        ],
      ),
    };
    const hitos = hitosDelEquipo(u);
    expect(hitos.map((h) => [h.idHitoEquipo, h.orden, h.nombreHito])).toEqual([
      [101, 1, 'Análisis'],
      [102, 2, 'Diseño'],
      [103, 3, 'Implementación'],
    ]);
    expect(hitoActual(hitos)?.idHitoEquipo).toBe(102);
    expect(hitoActual(hitos.map((h) => ({ ...h, estado: 'EVALUADO' as const })))?.idHitoEquipo).toBe(103);
    expect(hitoActual([])).toBeNull();
  });

  it('hitoElegido usa ?hito= solo si es del equipo; rol en el equipo, etiqueta y mensaje al abrir', () => {
    const hitos: HitoDelEquipo[] = [
      {
        idHitoEquipo: 101,
        idHito: 1,
        orden: 1,
        nombreHito: 'Análisis',
        fechaInicio: '2026-08-01',
        fechaLimite: '2026-08-29',
        estado: 'EVALUADO',
      },
      {
        idHitoEquipo: 102,
        idHito: 2,
        orden: 2,
        nombreHito: 'Diseño',
        fechaInicio: '2026-08-30',
        fechaLimite: '2026-09-26',
        estado: 'EN_CURSO',
      },
    ];
    expect(hitoElegido(hitos, '101')?.idHitoEquipo).toBe(101);
    expect(hitoElegido(hitos, '555')?.idHitoEquipo).toBe(102);
    expect(hitoElegido(hitos, 'abc')?.idHitoEquipo).toBe(102);
    expect(hitoElegido(hitos, null)?.idHitoEquipo).toBe(102);
    const e = equipo(10, [5, 7]);
    expect([rolEnEquipo(e, 5), rolEnEquipo(e, 7), rolEnEquipo(e, 9)]).toEqual(['LIDER', 'INTEGRANTE', null]);
    expect(etiquetaHito(2, 'Diseño')).toBe('Hito 2 · Diseño');
    expect(
      mensajeErrorAbrir(new ErrorApi(403, 'ACCESO_DENEGADO', 'No tienes permiso para esta acción'), SIN_ACCESO_EQUIPO),
    ).toBe('No tienes acceso a este equipo o no existe.');
    expect(
      mensajeErrorAbrir(
        new ErrorApi(0, 'SIN_CONEXION', 'No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo'),
        SIN_ACCESO_EQUIPO,
      ),
    ).toBe('No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo');
  });
});
