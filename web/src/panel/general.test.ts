import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type {
  EquipoVista,
  HitoVista,
  IntegranteInactivo,
  PanelEquipo,
  PanelProyecto,
  ProyectoDetalle,
  ProyectoVista,
} from '../api/tipos';
import type { ApiCargaPanel } from './carga';
import {
  atencionGeneral,
  cargarPanelesPropios,
  proyectosDelPanel,
  resumenGeneral,
  ultimaActualizacion,
  type PanelDeProyecto,
} from './general';

function proyecto(parcial: Partial<ProyectoVista> = {}): ProyectoVista {
  return {
    idProyecto: 1,
    idDocente: 5,
    nombre: 'Curso A',
    descripcion: null,
    fechaInicio: '2026-08-01',
    fechaFin: '2026-11-30',
    usaMonitores: false,
    ...parcial,
  };
}

function hito(parcial: Partial<HitoVista> = {}): HitoVista {
  return {
    idHito: 10,
    idProyecto: 1,
    nombreHito: 'Entrega',
    descripcion: null,
    orden: 1,
    fechaInicio: '2026-08-01',
    fechaLimite: '2026-11-25',
    diasInactividadAlerta: null,
    notificarDocenteInactividad: false,
    criterios: [],
    ...parcial,
  };
}

function equipoDetalle(parcial: Partial<EquipoVista> = {}): EquipoVista {
  return {
    idEquipo: 1,
    idProyecto: 1,
    nombreEquipo: 'Grupo 1',
    miembros: [],
    hitos: [],
    ...parcial,
  };
}

function equipoPanel(parcial: Partial<PanelEquipo> = {}): PanelEquipo {
  return {
    idEquipo: 1,
    nombreEquipo: 'Grupo 1',
    enRojo: false,
    tareasTotales: 0,
    tareasCompletadas: 0,
    tareasRiesgoAlto: 0,
    avancePromedio: 0,
    integrantesInactivos: [],
    tareasBloqueadas: [],
    hitoActual: null,
    avanceHitoActual: null,
    todoEnviado: false,
    ...parcial,
  };
}

function panelDe(
  p: ProyectoVista,
  hitos: HitoVista[],
  equiposDetalle: EquipoVista[],
  equiposPanel: PanelEquipo[],
  generadoEn = '2026-11-19T12:00:00Z',
): PanelDeProyecto {
  const detalle: ProyectoDetalle = {
    proyecto: p,
    hitos,
    equipos: equiposDetalle,
  };
  const panel: PanelProyecto = {
    idProyecto: p.idProyecto,
    nombre: p.nombre,
    generadoEn,
    equipos: equiposPanel,
  };
  return { proyecto: p, datos: { panel, detalle } };
}

describe('panel general', () => {
  it('atencionGeneral junta elementos de varios proyectos, ordena por tipo, prefija claves e ignora fallidos', () => {
    const p1 = proyecto({ idProyecto: 11, nombre: 'Curso Norte' });
    const p2 = proyecto({ idProyecto: 22, nombre: 'Curso Sur' });
    const p3 = proyecto({ idProyecto: 33, nombre: 'Curso Oeste' });

    const inactivo: IntegranteInactivo = {
      idUsuario: 9,
      nombre: 'Luis',
      ultimaActividad: '2026-11-10T12:00:00Z',
      diasSinActividad: 4,
    };

    const panel1 = panelDe(
      p1,
      [
        hito({ idHito: 10, idProyecto: 11, orden: 1, nombreHito: 'Inicio', fechaLimite: '2026-11-18' }),
        hito({ idHito: 11, idProyecto: 11, orden: 2, nombreHito: 'Avance', fechaLimite: '2026-11-28' }),
      ],
      [
        equipoDetalle({
          idEquipo: 1,
          idProyecto: 11,
          nombreEquipo: 'Grupo Uno',
          hitos: [{ idHitoEquipo: 100, idHito: 10, estado: 'ENVIADO', fechaEnvio: '2026-11-17T15:00:00Z' }],
        }),
      ],
      [equipoPanel({ idEquipo: 1, nombreEquipo: 'Grupo Uno', enRojo: false, tareasRiesgoAlto: 1 })],
    );

    const panel2 = panelDe(
      p2,
      [hito({ idHito: 20, idProyecto: 22, orden: 1, nombreHito: 'Análisis', fechaLimite: '2026-11-21' })],
      [
        equipoDetalle({
          idEquipo: 1,
          idProyecto: 22,
          nombreEquipo: 'Grupo Dos',
          hitos: [{ idHitoEquipo: 200, idHito: 20, estado: 'EN_CURSO', fechaEnvio: null }],
        }),
      ],
      [equipoPanel({ idEquipo: 1, nombreEquipo: 'Grupo Dos', enRojo: true, integrantesInactivos: [inactivo] })],
    );

    const items = atencionGeneral([panel1, { proyecto: p3, datos: null }, panel2], '2026-11-19');

    expect(items.map((i) => [i.tipo, i.clave, i.proyecto])).toEqual([
      ['equipo', 'p22-equipo-1', 'Curso Sur'],
      ['calificar', 'p11-calificar-100', 'Curso Norte'],
      ['proximo', 'p22-proximo-1-200', 'Curso Sur'],
      ['riesgo', 'p11-riesgo-1', 'Curso Norte'],
    ]);
    expect(items[0].nombreAccesible).toBe('Ver tablero: Grupo Dos: 1 integrante inactivo · Curso Sur');
    expect(items[1].nombreAccesible).toBe('Calificar: Grupo Uno envió Hito 1 · Inicio el 17/11 · Curso Norte');
    expect(new Set(items.map((i) => i.clave)).size).toBe(items.length);
  });

  it('resumenGeneral suma cifras, elige la entrega más próxima con su proyecto y omite avance', () => {
    const p1 = proyecto({ idProyecto: 1, nombre: 'Curso Norte' });
    const p2 = proyecto({ idProyecto: 2, nombre: 'Curso Sur' });
    const p3 = proyecto({ idProyecto: 3, nombre: 'Curso Este' });

    const panel1 = panelDe(
      p1,
      [hito({ idHito: 1, idProyecto: 1, orden: 1, nombreHito: 'Análisis', fechaLimite: '2026-11-24' })],
      [
        equipoDetalle({
          idEquipo: 10,
          idProyecto: 1,
          hitos: [{ idHitoEquipo: 101, idHito: 1, estado: 'ENVIADO', fechaEnvio: null }],
        }),
        equipoDetalle({ idEquipo: 11, idProyecto: 1 }),
      ],
      [equipoPanel({ idEquipo: 10, enRojo: true }), equipoPanel({ idEquipo: 11, enRojo: false })],
    );

    const panel2 = panelDe(
      p2,
      [hito({ idHito: 2, idProyecto: 2, orden: 2, nombreHito: 'Prototipo', fechaLimite: '2026-11-21' })],
      [
        equipoDetalle({
          idEquipo: 20,
          idProyecto: 2,
          hitos: [{ idHitoEquipo: 201, idHito: 2, estado: 'ENVIADO', fechaEnvio: null }],
        }),
        equipoDetalle({
          idEquipo: 21,
          idProyecto: 2,
          hitos: [{ idHitoEquipo: 202, idHito: 2, estado: 'ENVIADO', fechaEnvio: null }],
        }),
      ],
      [equipoPanel({ idEquipo: 20, enRojo: true }), equipoPanel({ idEquipo: 21, enRojo: true })],
    );

    const resumen = resumenGeneral([panel1, { proyecto: p3, datos: null }, panel2], '2026-11-19');
    expect(resumen).toEqual({
      enRojo: 3,
      equipos: 4,
      porEvaluar: 3,
      proxima: { etiqueta: 'Hito 2 · Prototipo', plazo: 'vence en 2 días · Curso Sur' },
    });
    expect('avance' in resumen).toBe(false);

    const sinProximas = resumenGeneral([panel1, panel2], '2026-11-30');
    expect(sinProximas.proxima).toBeNull();
  });

  it('proyectosDelPanel devuelve marca por proyecto y null cuando falló', () => {
    const p1 = proyecto({ idProyecto: 1, nombre: 'Curso Norte' });
    const p2 = proyecto({ idProyecto: 2, nombre: 'Curso Sur' });
    const p3 = proyecto({ idProyecto: 3, nombre: 'Curso Este' });

    const panel1 = panelDe(p1, [], [], [equipoPanel({ idEquipo: 1, enRojo: true })]);
    const panel2 = panelDe(p2, [], [], [equipoPanel({ idEquipo: 2, enRojo: false })]);

    expect(proyectosDelPanel([panel1, { proyecto: p3, datos: null }, panel2], '2026-11-19')).toEqual([
      { idProyecto: 1, nombre: 'Curso Norte', marca: { texto: '1 equipo en atención', tono: 'peligro' } },
      { idProyecto: 3, nombre: 'Curso Este', marca: null },
      { idProyecto: 2, nombre: 'Curso Sur', marca: { texto: 'Al día', tono: 'exito' } },
    ]);
  });

  it('cargarPanelesPropios mantiene el orden y deja datos null si un proyecto falla', async () => {
    const p1 = proyecto({ idProyecto: 1, nombre: 'Curso Norte' });
    const p2 = proyecto({ idProyecto: 2, nombre: 'Curso Sur' });
    const p3 = proyecto({ idProyecto: 3, nombre: 'Curso Este' });

    const api: ApiCargaPanel = {
      async panelProyecto(idProyecto) {
        if (idProyecto === 2) {
          throw new ErrorApi(500, 'ERROR', 'Fallo');
        }
        return { idProyecto, nombre: `Curso ${idProyecto}`, generadoEn: '2026-11-19T12:00:00Z', equipos: [] };
      },
      async detalleProyecto(idProyecto) {
        return { proyecto: proyecto({ idProyecto }), hitos: [], equipos: [] };
      },
    };

    const paneles = await cargarPanelesPropios(api, [p1, p2, p3]);
    expect(paneles.map((p) => p.proyecto.idProyecto)).toEqual([1, 2, 3]);
    expect(paneles[0].datos?.panel.idProyecto).toBe(1);
    expect(paneles[1].datos).toBeNull();
    expect(paneles[2].datos?.panel.idProyecto).toBe(3);
  });

  it('ultimaActualizacion devuelve el generadoEn más reciente o null si ninguno cargó', () => {
    const p1 = proyecto({ idProyecto: 1, nombre: 'Curso Norte' });
    const p2 = proyecto({ idProyecto: 2, nombre: 'Curso Sur' });
    const p3 = proyecto({ idProyecto: 3, nombre: 'Curso Este' });

    const panel1 = panelDe(p1, [], [], [], '2026-11-19T10:00:00Z');
    const panel2 = panelDe(p2, [], [], [], '2026-11-19T14:30:00.123456Z');

    expect(ultimaActualizacion([panel1, { proyecto: p3, datos: null }, panel2])).toBe('2026-11-19T14:30:00.123456Z');
    expect(
      ultimaActualizacion([
        { proyecto: p1, datos: null },
        { proyecto: p2, datos: null },
      ]),
    ).toBeNull();
  });
});
