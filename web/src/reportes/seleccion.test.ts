import { describe, expect, it } from 'vitest';
import type { EquipoVista, ProyectoVista } from '../api/tipos';
import type { HitoDelEquipo } from '../tablero/ubicacion';
import {
  elegirEquipo,
  elegirHito,
  elegirProyecto,
  proyectosDelDocente,
  rutaReportes,
} from './seleccion';

function proyecto(parcial: Partial<ProyectoVista>): ProyectoVista {
  return {
    idProyecto: 1,
    idDocente: 1,
    nombre: 'P',
    descripcion: null,
    fechaInicio: '2026-01-01',
    fechaFin: '2026-06-30',
    usaMonitores: false,
    equiposMixtos: false,
    cierreEquipos: null,
    horasRonda: 48,
    ...parcial,
  };
}

function equipo(parcial: Partial<EquipoVista>): EquipoVista {
  return {
    idEquipo: 1,
    idProyecto: 1,
    nombreEquipo: 'E',
    idCurso: null,
    cupo: null,
    miembros: [],
    hitos: [],
    ...parcial,
  };
}

function hito(parcial: Partial<HitoDelEquipo>): HitoDelEquipo {
  return {
    idHitoEquipo: 1,
    idHito: 1,
    orden: 1,
    nombreHito: 'H',
    fechaInicio: '2026-01-01',
    fechaLimite: '2026-06-30',
    estado: 'EN_CURSO',
    ...parcial,
  };
}

describe('seleccion de reportes', () => {
  it('filtra proyectos del docente y resuelve la seleccion de proyecto, equipo e hito', () => {
    const p1 = proyecto({ idProyecto: 1, idDocente: 7 });
    const p2 = proyecto({ idProyecto: 2, idDocente: 8 });
    const p3 = proyecto({ idProyecto: 3, idDocente: 7 });

    const delDocente = proyectosDelDocente([p1, p2, p3], 7);
    expect(delDocente.map((p) => p.idProyecto)).toEqual([1, 3]);

    expect(elegirProyecto(delDocente, '3')?.idProyecto).toBe(3);
    expect(elegirProyecto(delDocente, '2')?.idProyecto).toBe(1);
    expect(elegirProyecto(delDocente, null)?.idProyecto).toBe(1);
    expect(elegirProyecto(delDocente, 'abc')?.idProyecto).toBe(1);
    expect(elegirProyecto([], '1')).toBeNull();

    const eq5 = equipo({ idEquipo: 5 });
    const eq7 = equipo({ idEquipo: 7 });
    const equipos = [eq5, eq7];
    expect(elegirEquipo(equipos, '7')?.idEquipo).toBe(7);
    expect(elegirEquipo(equipos, '9')?.idEquipo).toBe(5);
    expect(elegirEquipo([], null)).toBeNull();

    const h12 = hito({ idHitoEquipo: 12 });
    const h13 = hito({ idHitoEquipo: 13 });
    const hitos = [h12, h13];
    expect(elegirHito(hitos, '13')?.idHitoEquipo).toBe(13);
    expect(elegirHito(hitos, '99')).toBeNull();
    expect(elegirHito(hitos, null)).toBeNull();
  });

  it('construye la ruta de reportes con sus parametros en orden', () => {
    expect(rutaReportes({ idProyecto: null, idEquipo: null, idHitoEquipo: null })).toBe(
      '/reportes'
    );
    expect(rutaReportes({ idProyecto: 3, idEquipo: null, idHitoEquipo: null })).toBe(
      '/reportes?proyecto=3'
    );
    expect(rutaReportes({ idProyecto: 3, idEquipo: 7, idHitoEquipo: null })).toBe(
      '/reportes?proyecto=3&equipo=7'
    );
    expect(rutaReportes({ idProyecto: 3, idEquipo: 7, idHitoEquipo: 12 })).toBe(
      '/reportes?proyecto=3&equipo=7&hito=12'
    );
  });
});
