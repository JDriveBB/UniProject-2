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
  atencionDelDia,
  diasHasta,
  entregasDelProyecto,
  estadoBotonPanel,
  estadoEquipo,
  marcaCelda,
  porEvaluar,
  proyectosPropios,
  resumenPanel,
  textoActualizado,
  textoBloqueo,
  textoDias,
  textoInactivo,
  textoEnvio,
  textoPlazo,
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
    hitoActual: null,
    avanceHitoActual: null,
    todoEnviado: false,
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

    const eqA = equipoPanel({
      idEquipo: 10,
      nombreEquipo: 'A',
      enRojo: true,
      tareasTotales: 10,
      avancePromedio: 90,
      avanceHitoActual: 40,
    });
    const eqB = equipoPanel({
      idEquipo: 20,
      nombreEquipo: 'B',
      enRojo: false,
      tareasTotales: 4,
      avancePromedio: 10,
      avanceHitoActual: 70,
    });
    const eqC = equipoPanel({ idEquipo: 30, nombreEquipo: 'C', enRojo: false, tareasTotales: 3, avancePromedio: 50 });
    const eqTodoEnviado = equipoPanel({ idEquipo: 40, nombreEquipo: 'D', tareasTotales: 5, todoEnviado: true });

    const panelABC: PanelProyecto = {
      idProyecto: 3,
      nombre: 'P',
      generadoEn: '2026-09-23T19:05:00Z',
      equipos: [eqA, eqB, eqC, eqTodoEnviado],
    };
    const entregasResumen = entregasDelProyecto(datosReferencia, '2026-09-18');
    expect(resumenPanel(panelABC, 2, entregasResumen)).toEqual({
      enRojo: 1,
      equipos: 4,
      porEvaluar: 2,
      avance: 55,
      proxima: { etiqueta: 'Hito 1 · Requisitos', plazo: 'vence en 2 días' },
    });

    const panelC: PanelProyecto = {
      idProyecto: 3,
      nombre: 'P',
      generadoEn: '2026-09-23T19:05:00Z',
      equipos: [eqC],
    };
    const resumenSinEntregas = resumenPanel(panelC, 2, entregasDelProyecto(datosReferencia, '2026-10-20'));
    expect(resumenSinEntregas.avance).toBeNull();
    expect(resumenSinEntregas.proxima).toBeNull();

    const antesDeD12 = equipoPanel({ tareasTotales: 2, avancePromedio: 30 });
    delete antesDeD12.avanceHitoActual;
    expect(resumenPanel({ ...panelC, equipos: [antesDeD12] }, 0, []).avance).toBe(30);

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

  it('determina marcas de celdas según el estado y la fecha límite', () => {
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
  });

  it('calcula días hasta una fecha cruzando fin de mes y genera los 5 textos de plazo', () => {
    expect(diasHasta('2026-12-02', '2026-11-29')).toBe(3);
    expect(diasHasta('2026-02-27', '2026-03-02')).toBe(-3);
    expect(diasHasta('2026-11-19', '2026-11-19')).toBe(0);
    expect(diasHasta('2027-01-02', '2026-12-30')).toBe(3);

    expect(textoPlazo(0)).toBe('vence hoy');
    expect(textoPlazo(1)).toBe('vence mañana');
    expect(textoPlazo(4)).toBe('vence en 4 días');
    expect(textoPlazo(-1)).toBe('venció ayer');
    expect(textoPlazo(-5)).toBe('venció hace 5 días');
  });

  it('ordena atencionDelDia por grupo, respeta plurales, evita duplicar equipo en rojo y devuelve [] si no hay pendientes', () => {
    const inactivo: IntegranteInactivo = {
      idUsuario: 1,
      nombre: 'Ana',
      ultimaActividad: '2026-11-10T15:00:00Z',
      diasSinActividad: 5,
    };
    const bloqueada = (idTarea: number): TareaBloqueada => ({
      idTarea,
      titulo: `T${idTarea}`,
      fechaBloqueo: '2026-11-15T10:00:00Z',
      horasBloqueada: 60,
    });

    const hitos: HitoVista[] = [
      { ...hitosReferencia[0], idHito: 10, orden: 1, nombreHito: 'Requisitos', fechaLimite: '2026-11-17' },
      { ...hitosReferencia[0], idHito: 20, orden: 2, nombreHito: 'Diseño', fechaLimite: '2026-11-21' },
      { ...hitosReferencia[0], idHito: 30, orden: 3, nombreHito: 'Entrega final', fechaLimite: '2026-11-30' },
    ];
    const equipos: EquipoVista[] = [
      {
        idEquipo: 2,
        idProyecto: 3,
        nombreEquipo: 'Equipo Beta',
        miembros: [],
        hitos: [
          { idHitoEquipo: 201, idHito: 10, estado: 'EN_CURSO' },
          { idHitoEquipo: 202, idHito: 20, estado: 'EN_CURSO' },
        ],
      },
      {
        idEquipo: 1,
        idProyecto: 3,
        nombreEquipo: 'Equipo Alfa',
        miembros: [],
        hitos: [
          { idHitoEquipo: 101, idHito: 10, estado: 'ENVIADO', fechaEnvio: '2026-11-20T15:00:00.123456Z' },
          { idHitoEquipo: 102, idHito: 20, estado: 'EVALUADO', fechaEnvio: '2026-11-18T15:00:00Z' },
        ],
      },
    ];
    const panel: PanelProyecto = {
      idProyecto: 3,
      nombre: 'P',
      generadoEn: '2026-11-19T12:00:00Z',
      equipos: [
        equipoPanel({
          idEquipo: 2,
          nombreEquipo: 'Equipo Beta',
          enRojo: true,
          tareasRiesgoAlto: 2,
          integrantesInactivos: [inactivo],
          tareasBloqueadas: [bloqueada(1), bloqueada(2)],
        }),
        equipoPanel({
          idEquipo: 1,
          nombreEquipo: 'Equipo Alfa',
          enRojo: false,
          tareasRiesgoAlto: 1,
        }),
      ],
    };

    const items = atencionDelDia(panel, detalle({ hitos, equipos }), '2026-11-19');
    expect(items.map((i) => i.tipo)).toEqual(['vencido', 'equipo', 'calificar', 'proximo', 'riesgo']);
    expect(items[0]).toEqual({
      clave: 'vencido-2-201',
      tipo: 'vencido',
      tono: 'peligro',
      etiqueta: 'Vencido',
      texto: 'Equipo Beta no envió Hito 1 · Requisitos: venció hace 2 días',
      accion: 'Ver tablero',
      ruta: '/equipos/2/tablero?hito=201',
      nombreAccesible: 'Ver tablero: Equipo Beta no envió Hito 1 · Requisitos: venció hace 2 días',
    });
    expect(items[1]).toEqual({
      clave: 'equipo-2',
      tipo: 'equipo',
      tono: 'peligro',
      etiqueta: 'Requiere atención',
      texto: 'Equipo Beta: 1 integrante inactivo · 2 tareas bloqueadas',
      accion: 'Ver tablero',
      ruta: '/equipos/2/tablero',
      nombreAccesible: 'Ver tablero: Equipo Beta: 1 integrante inactivo · 2 tareas bloqueadas',
    });
    expect(items[2]).toEqual({
      clave: 'calificar-101',
      tipo: 'calificar',
      tono: 'info',
      etiqueta: 'Por calificar',
      texto: 'Equipo Alfa envió Hito 1 · Requisitos el 20/11',
      accion: 'Calificar',
      ruta: '/hitos/101/evaluar',
      nombreAccesible: 'Calificar: Equipo Alfa envió Hito 1 · Requisitos el 20/11',
    });
    expect(items[3]).toEqual({
      clave: 'proximo-2-202',
      tipo: 'proximo',
      tono: 'aviso',
      etiqueta: 'Entrega próxima',
      texto: 'Equipo Beta aún no envía Hito 2 · Diseño: vence en 2 días',
      accion: 'Ver tablero',
      ruta: '/equipos/2/tablero?hito=202',
      nombreAccesible: 'Ver tablero: Equipo Beta aún no envía Hito 2 · Diseño: vence en 2 días',
    });
    expect(items[4]).toEqual({
      clave: 'riesgo-1',
      tipo: 'riesgo',
      tono: 'aviso',
      etiqueta: 'Riesgo alto',
      texto: 'Equipo Alfa: 1 tarea con riesgo alto de retraso',
      accion: 'Ver tablero',
      ruta: '/equipos/1/tablero',
      nombreAccesible: 'Ver tablero: Equipo Alfa: 1 tarea con riesgo alto de retraso',
    });
    expect(items.some((i) => i.clave === 'riesgo-2')).toBe(false);

    const panelSinPendientes: PanelProyecto = {
      idProyecto: 3,
      nombre: 'P',
      generadoEn: '2026-11-19T12:00:00Z',
      equipos: [equipoPanel({ idEquipo: 1, nombreEquipo: 'Equipo Alfa', enRojo: false, tareasRiesgoAlto: 0 })],
    };
    const detalleSinPendientes = detalle({
      hitos: [hitos[2]],
      equipos: [
        {
          idEquipo: 1,
          idProyecto: 3,
          nombreEquipo: 'Equipo Alfa',
          miembros: [],
          hitos: [{ idHitoEquipo: 103, idHito: 30, estado: 'EN_CURSO' }],
        },
      ],
    });
    expect(atencionDelDia(panelSinPendientes, detalleSinPendientes, '2026-11-19')).toEqual([]);
  });

  it('calcula entregasDelProyecto con urgencia pasada, vencida, pronto, normal y marca solo el primer hito próximo', () => {
    const hitos: HitoVista[] = [
      { ...hitosReferencia[0], idHito: 4, orden: 4, nombreHito: 'Cierre', fechaLimite: '2026-12-05' },
      { ...hitosReferencia[0], idHito: 1, orden: 1, nombreHito: 'Inicio', fechaLimite: '2026-11-10' },
      { ...hitosReferencia[0], idHito: 3, orden: 3, nombreHito: 'Prototipo', fechaLimite: '2026-11-21' },
      { ...hitosReferencia[0], idHito: 2, orden: 2, nombreHito: 'Diseño', fechaLimite: '2026-11-18' },
    ];
    const equipos: EquipoVista[] = [
      {
        idEquipo: 2,
        idProyecto: 3,
        nombreEquipo: 'Equipo Beta',
        miembros: [],
        hitos: [
          { idHitoEquipo: 21, idHito: 1, estado: 'EVALUADO' },
          { idHitoEquipo: 22, idHito: 2, estado: 'EN_CURSO' },
          { idHitoEquipo: 23, idHito: 3, estado: 'EN_CURSO' },
        ],
      },
      {
        idEquipo: 1,
        idProyecto: 3,
        nombreEquipo: 'Equipo Alfa',
        miembros: [],
        hitos: [
          { idHitoEquipo: 11, idHito: 1, estado: 'EVALUADO', fechaEnvio: '2026-11-08T15:00:00Z' },
          { idHitoEquipo: 12, idHito: 2, estado: 'EVALUADO', fechaEnvio: '2026-11-19T03:00:00Z' },
          { idHitoEquipo: 13, idHito: 3, estado: 'ENVIADO', fechaEnvio: '2026-11-21T20:00:00Z' },
          { idHitoEquipo: 14, idHito: 4, estado: 'EN_CURSO' },
        ],
      },
    ];

    const entregas = entregasDelProyecto(detalle({ hitos, equipos }), '2026-11-19');
    expect(entregas.map((e) => [e.idHito, e.urgencia, e.proxima, e.plazo])).toEqual([
      [1, 'pasada', false, 'venció hace 9 días'],
      [2, 'vencida', false, 'venció ayer'],
      [3, 'pronto', true, 'vence en 2 días'],
      [4, 'normal', false, 'vence en 16 días'],
    ]);
    expect(entregas[1].equipos.map((eq) => [eq.nombreEquipo, eq.marca.texto])).toEqual([
      ['Equipo Alfa', 'Evaluado'],
      ['Equipo Beta', 'Vencido sin enviar'],
    ]);
    expect(entregas[3].equipos.map((eq) => eq.nombreEquipo)).toEqual(['Equipo Alfa']);
    expect(entregas.map((e) => e.equipos.map((eq) => eq.envio))).toEqual([
      ['Enviado el 08/11 · 2 días antes', null],
      ['Enviado el 18/11 · el mismo día', null],
      ['Enviado el 21/11 · el mismo día', null],
      [null],
    ]);
  });

  it('textoEnvio compara el día de envío en Bogotá con la fecha límite', () => {
    expect(textoEnvio('2026-11-18T15:00:00Z', '2026-11-20')).toBe('Enviado el 18/11 · 2 días antes');
    expect(textoEnvio('2026-11-19T15:00:00Z', '2026-11-20')).toBe('Enviado el 19/11 · 1 día antes');
    expect(textoEnvio('2026-11-21T04:59:00Z', '2026-11-20')).toBe('Enviado el 20/11 · el mismo día');
    expect(textoEnvio('2026-11-21T05:00:00Z', '2026-11-20')).toBe('Enviado el 21/11 · 1 día tarde');
    expect(textoEnvio('2026-11-23T15:00:00Z', '2026-11-20')).toBe('Enviado el 23/11 · 3 días tarde');
    expect(textoEnvio('no es fecha', '2026-11-20')).toBeNull();
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
