import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type {
  EquipoVista,
  HitoVista,
  PanelEquipo,
  PanelProyecto,
  ProyectoDetalle,
  ProyectoVista,
  TableroHito,
  TareaVista,
} from '../api/tipos';
import {
  avancePonderado,
  cargarResumenes,
  estadoProyecto,
  etapaProyecto,
  resumenDocente,
  resumenEstudiante,
  type ApiResumenes,
  type ResumenDocente,
} from './tarjetas';

function proyectoBase(parcial: Partial<ProyectoVista> = {}): ProyectoVista {
  return {
    idProyecto: 1,
    idDocente: 5,
    nombre: 'Proyecto 1',
    descripcion: null,
    fechaInicio: '2026-08-01',
    fechaFin: '2026-11-30',
    usaMonitores: false,
    equiposMixtos: false,
    cierreEquipos: null,
    horasRonda: 48,
    ...parcial,
  };
}

function hitoBase(parcial: Partial<HitoVista> = {}): HitoVista {
  return {
    idHito: 10,
    idProyecto: 1,
    nombreHito: 'Requisitos',
    descripcion: null,
    orden: 1,
    fechaInicio: '2026-08-01',
    fechaLimite: '2026-09-20',
    diasInactividadAlerta: null,
    notificarDocenteInactividad: false,
    criterios: [],
    ...parcial,
  };
}

function detalleBase(parcial: Partial<ProyectoDetalle> = {}): ProyectoDetalle {
  return {
    proyecto: proyectoBase(parcial.proyecto),
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
    hitoActual: null,
    avanceHitoActual: null,
    todoEnviado: false,
    ...parcial,
  };
}

function tareaBase(parcial: Partial<TareaVista> = {}): TareaVista {
  return {
    idTarea: 1,
    idHitoEquipo: 101,
    titulo: 'Tarea',
    descripcion: null,
    idEncargado: null,
    encargado: null,
    idTareaOrigen: null,
    estado: 'EN_PROGRESO',
    porcentajeAvance: 0,
    horasEstimadas: 1,
    fechaLimite: null,
    fechaLimiteEfectiva: '2026-09-20',
    fechaCompletada: null,
    esPrerrequisitoCritico: false,
    bloqueada: false,
    fechaBloqueo: null,
    enRutaCritica: false,
    holguraHoras: null,
    nivelRiesgo: null,
    prerrequisitos: [],
    ...parcial,
  };
}

function tableroBase(parcial: Partial<TableroHito> = {}): TableroHito {
  return {
    idHitoEquipo: 101,
    idHito: 10,
    nombreHito: 'Requisitos',
    fechaLimite: '2026-09-20',
    estado: 'EN_CURSO',
    editable: true,
    idEquipo: 20,
    nombreEquipo: 'Beta',
    idProyecto: 1,
    nombreProyecto: 'Proyecto 1',
    tareas: [],
    ...parcial,
  };
}

describe('tarjetas de proyectos', () => {
  it('avancePonderado calcula promedio ponderado redondeado y devuelve null sin horas', () => {
    expect(
      avancePonderado([
        { porcentajeAvance: 50, horasEstimadas: 10 },
        { porcentajeAvance: 100, horasEstimadas: 2 },
      ]),
    ).toBe(58);
    expect(avancePonderado([])).toBeNull();
    expect(avancePonderado([{ porcentajeAvance: 80, horasEstimadas: 0 }])).toBeNull();
  });

  it('resumenDocente resume equipos, hitos, estudiantes, próxima entrega, por calificar y en atención', () => {
    const hitos: HitoVista[] = [
      hitoBase({ idHito: 1, orden: 1, nombreHito: 'Análisis', fechaLimite: '2026-09-10' }),
      hitoBase({ idHito: 2, orden: 2, nombreHito: 'Diseño', fechaLimite: '2026-09-23' }),
      hitoBase({ idHito: 3, orden: 3, nombreHito: 'Cierre', fechaLimite: '2026-10-15' }),
    ];
    const equipos: EquipoVista[] = [
      {
        idEquipo: 10,
        idProyecto: 1,
        nombreEquipo: 'Alfa',
        idCurso: null,
        cupo: null,
        miembros: [
          { idUsuario: 101, nombre: 'E1', rolEquipo: 'LIDER' },
          { idUsuario: 102, nombre: 'E2', rolEquipo: 'INTEGRANTE' },
        ],
        hitos: [
          { idHitoEquipo: 101, idHito: 1, estado: 'ENVIADO', fechaEnvio: '2026-09-12T12:00:00Z' },
          { idHitoEquipo: 102, idHito: 2, estado: 'EN_CURSO', fechaEnvio: null },
        ],
      },
      {
        idEquipo: 20,
        idProyecto: 1,
        nombreEquipo: 'Beta',
        idCurso: null,
        cupo: null,
        miembros: [
          { idUsuario: 201, nombre: 'E3', rolEquipo: 'LIDER' },
          { idUsuario: 202, nombre: 'E4', rolEquipo: 'INTEGRANTE' },
          { idUsuario: 203, nombre: 'E5', rolEquipo: 'INTEGRANTE' },
        ],
        hitos: [
          { idHitoEquipo: 201, idHito: 1, estado: 'ENVIADO', fechaEnvio: '2026-09-12T12:00:00Z' },
          { idHitoEquipo: 202, idHito: 2, estado: 'EN_CURSO', fechaEnvio: null },
        ],
      },
    ];
    const panel: PanelProyecto = {
      idProyecto: 1,
      nombre: 'Proyecto 1',
      generadoEn: '2026-09-20T12:00:00Z',
      equipos: [
        equipoPanel({
          idEquipo: 10,
          nombreEquipo: 'Alfa',
          enRojo: true,
          tareasTotales: 4,
          avancePromedio: 40,
          avanceHitoActual: 40,
        }),
        equipoPanel({
          idEquipo: 20,
          nombreEquipo: 'Beta',
          enRojo: false,
          tareasTotales: 2,
          avancePromedio: 70,
          avanceHitoActual: 70,
        }),
      ],
    };

    expect(resumenDocente({ panel, detalle: detalleBase({ hitos, equipos }) }, '2026-09-20')).toEqual({
      equipos: 2,
      hitos: 3,
      estudiantes: 5,
      proxima: { etiqueta: 'Hito 2 · Diseño', plazo: 'vence en 3 días', urgencia: 'pronto' },
      porCalificar: 2,
      enAtencion: 1,
    });

    const sinDatos = resumenDocente(
      {
        panel: { ...panel, equipos: [equipoPanel({ idEquipo: 10, tareasTotales: 0 })] },
        detalle: detalleBase({ hitos: [hitoBase({ fechaLimite: '2026-09-10' })], equipos: [] }),
      },
      '2026-09-20',
    );
    expect(sinDatos).toEqual({
      equipos: 1,
      hitos: 1,
      estudiantes: 0,
      proxima: null,
      porCalificar: 0,
      enAtencion: 0,
    });
  });

  it('estadoProyecto cubre los 3 estados y diferencia singular y plural', () => {
    const base: ResumenDocente = {
      equipos: 3,
      hitos: 2,
      estudiantes: 9,
      proxima: null,
      porCalificar: 2,
      enAtencion: 0,
    };

    expect(estadoProyecto({ ...base, enAtencion: 1 })).toEqual({
      texto: '1 equipo en atención',
      tono: 'peligro',
    });
    expect(estadoProyecto({ ...base, enAtencion: 2 })).toEqual({
      texto: '2 equipos en atención',
      tono: 'peligro',
    });
    expect(estadoProyecto({ ...base, enAtencion: 0, porCalificar: 2 })).toEqual({
      texto: '2 por calificar',
      tono: 'info',
    });
    expect(estadoProyecto({ ...base, enAtencion: 0, porCalificar: 0 })).toEqual({
      texto: 'Al día',
      tono: 'exito',
    });
  });

  it('etapaProyecto clasifica antes, primer día, último día y después', () => {
    const fechas = { fechaInicio: '2026-08-10', fechaFin: '2026-11-20' };
    expect(etapaProyecto(fechas, '2026-08-09')).toEqual({ texto: 'Por empezar', tono: 'neutro' });
    expect(etapaProyecto(fechas, '2026-08-10')).toEqual({ texto: 'En curso', tono: 'info' });
    expect(etapaProyecto(fechas, '2026-11-20')).toEqual({ texto: 'En curso', tono: 'info' });
    expect(etapaProyecto(fechas, '2026-11-21')).toEqual({ texto: 'Terminado', tono: 'neutro' });
  });

  it('resumenEstudiante cubre líder, integrante, sin equipo, sin hitos y vencido sin enviar', () => {
    const hitos: HitoVista[] = [
      hitoBase({ idHito: 10, orden: 1, nombreHito: 'Requisitos', fechaLimite: '2026-09-20' }),
      hitoBase({ idHito: 20, orden: 2, nombreHito: 'Diseño', fechaLimite: '2026-09-25' }),
    ];
    const equipoBeta: EquipoVista = {
      idEquipo: 20,
      idProyecto: 1,
      nombreEquipo: 'Equipo Beta',
      idCurso: null,
      cupo: null,
      miembros: [
        { idUsuario: 11, nombre: 'Líder', rolEquipo: 'LIDER' },
        { idUsuario: 12, nombre: 'Integrante', rolEquipo: 'INTEGRANTE' },
      ],
      hitos: [
        { idHitoEquipo: 101, idHito: 10, estado: 'EN_CURSO', fechaEnvio: null },
        { idHitoEquipo: 102, idHito: 20, estado: 'EN_CURSO', fechaEnvio: null },
      ],
    };
    const detalle = detalleBase({ hitos, equipos: [equipoBeta] });
    const tablero = tableroBase({
      idHitoEquipo: 101,
      tareas: [
        tareaBase({ idTarea: 1, porcentajeAvance: 50, horasEstimadas: 10 }),
        tareaBase({ idTarea: 2, porcentajeAvance: 100, horasEstimadas: 2 }),
      ],
    });

    expect(resumenEstudiante(detalle, 11, tablero, '2026-09-17')).toEqual({
      equipo: { idEquipo: 20, nombreEquipo: 'Equipo Beta' },
      esLider: true,
      hito: {
        idHitoEquipo: 101,
        etiqueta: 'Hito 1 · Requisitos',
        marca: { texto: 'En progreso', tono: 'info' },
        plazo: 'vence en 3 días',
      },
      avance: 58,
    });

    const detalleEnviado = detalleBase({
      hitos: [hitos[0]],
      equipos: [
        {
          ...equipoBeta,
          hitos: [{ idHitoEquipo: 101, idHito: 10, estado: 'ENVIADO', fechaEnvio: '2026-09-19T12:00:00Z' }],
        },
      ],
    });
    expect(resumenEstudiante(detalleEnviado, 12, tablero, '2026-09-22')).toEqual({
      equipo: { idEquipo: 20, nombreEquipo: 'Equipo Beta' },
      esLider: false,
      hito: {
        idHitoEquipo: 101,
        etiqueta: 'Hito 1 · Requisitos',
        marca: { texto: 'En Revisión', tono: 'critico' },
        plazo: null,
      },
      avance: 58,
    });

    expect(resumenEstudiante(detalle, 99, tablero, '2026-09-17')).toBeNull();

    const detalleSinHitos = detalleBase({
      hitos,
      equipos: [{ ...equipoBeta, hitos: [] }],
    });
    expect(resumenEstudiante(detalleSinHitos, 11, null, '2026-09-17')).toEqual({
      equipo: { idEquipo: 20, nombreEquipo: 'Equipo Beta' },
      esLider: true,
      hito: null,
      avance: null,
    });

    expect(resumenEstudiante(detalle, 11, tablero, '2026-09-22')).toEqual({
      equipo: { idEquipo: 20, nombreEquipo: 'Equipo Beta' },
      esLider: true,
      hito: {
        idHitoEquipo: 101,
        etiqueta: 'Hito 1 · Requisitos',
        marca: { texto: 'Vencido sin enviar', tono: 'peligro' },
        plazo: 'venció hace 2 días',
      },
      avance: 58,
    });
  });

  it('cargarResumenes aísla fallos por proyecto para docente y resuelve estudiante o sin equipo', async () => {
    const p1 = proyectoBase({ idProyecto: 1, idDocente: 5, nombre: 'P1' });
    const p2 = proyectoBase({ idProyecto: 2, idDocente: 5, nombre: 'P2' });
    const p3 = proyectoBase({ idProyecto: 3, idDocente: 8, nombre: 'P3' });
    const p4 = proyectoBase({ idProyecto: 4, idDocente: 8, nombre: 'P4' });

    const hito = hitoBase({ idHito: 10, idProyecto: 3, orden: 1, nombreHito: 'Inicio', fechaLimite: '2026-09-25' });
    const detalle1 = detalleBase({ proyecto: p1, hitos: [hitoBase({ idProyecto: 1 })], equipos: [] });
    const detalle3 = detalleBase({
      proyecto: p3,
      hitos: [hito],
      equipos: [
        {
          idEquipo: 30,
          idProyecto: 3,
          nombreEquipo: 'Gamma',
          idCurso: null,
          cupo: null,
          miembros: [{ idUsuario: 22, nombre: 'Est', rolEquipo: 'INTEGRANTE' }],
          hitos: [{ idHitoEquipo: 301, idHito: 10, estado: 'EN_CURSO', fechaEnvio: null }],
        },
      ],
    });
    const detalle4 = detalleBase({ proyecto: p4, hitos: [], equipos: [] });

    const apiDocente: ApiResumenes = {
      async panelProyecto(idProyecto) {
        if (idProyecto === 2) {
          throw new ErrorApi(500, 'ERROR', 'Fallo');
        }
        return { idProyecto, nombre: 'P1', generadoEn: '2026-09-20T12:00:00Z', equipos: [] };
      },
      async detalleProyecto(idProyecto) {
        if (idProyecto === 1) {
          return detalle1;
        }
        if (idProyecto === 3) {
          return detalle3;
        }
        return detalle4;
      },
      async tableroHito() {
        return tableroBase({ idHitoEquipo: 301, tareas: [tareaBase({ porcentajeAvance: 40, horasEstimadas: 5 })] });
      },
    };

    const mapaDocente = await cargarResumenes(apiDocente, [p1, p2], { idUsuario: 5, rol: 'DOCENTE' }, '2026-09-20');
    expect(mapaDocente.get(1)).toEqual({
      tipo: 'docente',
      resumen: {
        equipos: 0,
        hitos: 1,
        estudiantes: 0,
        proxima: { etiqueta: 'Hito 1 · Requisitos', plazo: 'vence hoy', urgencia: 'pronto' },
        porCalificar: 0,
        enAtencion: 0,
      },
    });
    expect(mapaDocente.get(2)).toEqual({ tipo: 'error' });

    const mapaEstudiante = await cargarResumenes(
      apiDocente,
      [p3, p4],
      { idUsuario: 22, rol: 'ESTUDIANTE' },
      '2026-09-20',
    );
    expect(mapaEstudiante.get(3)).toEqual({
      tipo: 'estudiante',
      resumen: {
        equipo: { idEquipo: 30, nombreEquipo: 'Gamma' },
        esLider: false,
        hito: {
          idHitoEquipo: 301,
          etiqueta: 'Hito 1 · Inicio',
          marca: { texto: 'En progreso', tono: 'info' },
          plazo: 'vence en 5 días',
        },
        avance: 40,
      },
    });
    expect(mapaEstudiante.get(4)).toEqual({ tipo: 'sin-equipo' });
  });
});
