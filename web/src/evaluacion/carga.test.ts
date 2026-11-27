import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type {
  CriterioVista,
  EquipoVista,
  EvaluacionVista,
  HitoVista,
  ProyectoDetalle,
  ProyectoVista,
  TableroHito,
} from '../api/tipos';
import {
  cargarEvaluacion,
  criteriosDelHito,
  etiquetaDeHito,
  type ApiCargaEvaluacion,
} from './carga';

describe('carga', () => {
  it('orquesta la carga en orden, tolera 404 de evaluación y ordena criterios', async () => {
    const llamadas: string[] = [];

    const tablero: TableroHito = {
      idHitoEquipo: 5,
      idHito: 2,
      nombreHito: 'Diseño',
      idEquipo: 7,
      nombreEquipo: 'Equipo 7',
      idProyecto: 3,
      nombreProyecto: 'Proyecto 3',
      fechaLimite: '2026-09-30',
      estado: 'ENVIADO',
      editable: false,
      tareas: [],
    };

    const proyecto: ProyectoVista = {
      idProyecto: 3,
      idDocente: 1,
      nombre: 'Proyecto 3',
      descripcion: 'D3',
      fechaInicio: '2026-08-01',
      fechaFin: '2026-11-30',
      usaMonitores: false,
      equiposMixtos: false,
      cierreEquipos: null,
      horasRonda: 48,
    };

    const criteriosOriginales: CriterioVista[] = [
      {
        idCriterio: 9,
        idHito: 2,
        descripcion: 'C9',
        pesoPorcentaje: 50,
        puntajeMaximo: 5,
      },
      {
        idCriterio: 4,
        idHito: 2,
        descripcion: 'C4',
        pesoPorcentaje: 50,
        puntajeMaximo: 5,
      },
    ];

    const hitos: HitoVista[] = [
      {
        idHito: 1,
        idProyecto: 3,
        nombreHito: 'Plan',
        descripcion: 'D',
        orden: 1,
        fechaInicio: '2026-08-01',
        fechaLimite: '2026-08-15',
        diasInactividadAlerta: 3,
        notificarDocenteInactividad: false,
        criterios: [],
      },
      {
        idHito: 2,
        idProyecto: 3,
        nombreHito: 'Diseño',
        descripcion: 'D',
        orden: 2,
        fechaInicio: '2026-08-16',
        fechaLimite: '2026-09-30',
        diasInactividadAlerta: 3,
        notificarDocenteInactividad: false,
        criterios: criteriosOriginales,
      },
    ];

    const equipos: EquipoVista[] = [
      {
        idEquipo: 7,
        idProyecto: 3,
        nombreEquipo: 'Equipo 7',
        idCurso: null,
        cupo: null,
        miembros: [],
        hitos: [],
      },
    ];

    const detalle: ProyectoDetalle = {
      proyecto,
      hitos,
      equipos,
    };

    const api: ApiCargaEvaluacion = {
      async tableroHito(idHitoEquipo: number): Promise<TableroHito> {
        llamadas.push(`tableroHito ${idHitoEquipo}`);
        return tablero;
      },
      async detalleProyecto(idProyecto: number): Promise<ProyectoDetalle> {
        llamadas.push(`detalleProyecto ${idProyecto}`);
        return detalle;
      },
      async consultarEvaluacion(idHitoEquipo: number): Promise<EvaluacionVista> {
        llamadas.push(`consultarEvaluacion ${idHitoEquipo}`);
        throw new ErrorApi(404, 'NO_ENCONTRADO', 'No encontrado');
      },
    };

    const datos = await cargarEvaluacion(api, 5);

    expect(llamadas[0]).toBe('tableroHito 5');
    expect(llamadas).toContain('detalleProyecto 3');
    expect(llamadas).toContain('consultarEvaluacion 5');
    expect(datos.evaluacion).toBeNull();
    expect(datos.hito?.idHito).toBe(2);
    expect(datos.equipo?.idEquipo).toBe(7);
    expect(etiquetaDeHito(datos)).toBe('Hito 2 · Diseño');

    const criterios = criteriosDelHito(datos);
    expect(criterios.map((c) => c.idCriterio)).toEqual([4, 9]);
    expect(criteriosOriginales.map((c) => c.idCriterio)).toEqual([9, 4]);

    const api403: ApiCargaEvaluacion = {
      ...api,
      async consultarEvaluacion(): Promise<EvaluacionVista> {
        throw new ErrorApi(403, 'ACCESO_DENEGADO', 'Acceso denegado');
      },
    };
    await expect(cargarEvaluacion(api403, 5)).rejects.toThrow(ErrorApi);

    const apiVacio: ApiCargaEvaluacion = {
      ...api,
      async detalleProyecto(): Promise<ProyectoDetalle> {
        return {
          proyecto,
          hitos: [],
          equipos: [],
        };
      },
    };
    const datosVacios = await cargarEvaluacion(apiVacio, 5);
    expect(datosVacios.hito).toBeNull();
    expect(datosVacios.equipo).toBeNull();
    expect(etiquetaDeHito(datosVacios)).toBe('Diseño');
  });
});
