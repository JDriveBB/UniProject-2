import { describe, expect, it } from 'vitest';
import type { EntradaHistorial, EquipoVista, MiembroVista, ProyectoVista, TareaVista } from '../api/tipos';
import type { HitoDelEquipo } from '../tablero/ubicacion';
import {
  agruparPorDia,
  detalleLegible,
  entradasVisibles,
  etiquetaDia,
  horaBogota,
  indiceEquipo,
  puedeVerHistorial,
  referenciaDe,
  TAMANO_PAGINA,
  textoConteo,
  textoMostrarMas,
  type Visor,
} from './vista';

const Ana: MiembroVista = {
  idUsuario: 10,
  nombre: 'Ana',
  rolEquipo: 'LIDER',
};

const Luis: MiembroVista = {
  idUsuario: 11,
  nombre: 'Luis',
  rolEquipo: 'INTEGRANTE',
};

function proyecto(parcial: Partial<ProyectoVista> = {}): ProyectoVista {
  return {
    idProyecto: 1,
    idDocente: 7,
    nombre: 'Proyecto 1',
    descripcion: null,
    fechaInicio: '2026-08-01',
    fechaFin: '2026-11-30',
    usaMonitores: false,
    ...parcial,
  };
}

function equipo(parcial: Partial<EquipoVista> = {}): EquipoVista {
  return {
    idEquipo: 7,
    idProyecto: 1,
    nombreEquipo: 'Equipo 7',
    miembros: [Ana, Luis],
    hitos: [],
    ...parcial,
  };
}

function hito(parcial: Partial<HitoDelEquipo> = {}): HitoDelEquipo {
  return {
    idHitoEquipo: 12,
    idHito: 1,
    orden: 1,
    nombreHito: 'Análisis',
    estado: 'EN_CURSO',
    fechaInicio: '2026-08-01',
    fechaLimite: '2026-08-29',
    ...parcial,
  };
}

function tarea(parcial: Partial<TareaVista> = {}): TareaVista {
  return {
    idTarea: 40,
    idHitoEquipo: 12,
    titulo: 'Requisitos',
    descripcion: null,
    idEncargado: 10,
    encargado: 'Ana',
    idTareaOrigen: null,
    horasEstimadas: 1,
    porcentajeAvance: 0,
    estado: 'PENDIENTE',
    fechaLimite: null,
    fechaLimiteEfectiva: '2026-09-30',
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

function entrada(parcial: Partial<EntradaHistorial> = {}): EntradaHistorial {
  return {
    idLog: 1,
    fechaHora: '2026-09-23T15:00:00Z',
    idUsuario: 10,
    autor: 'Ana',
    accion: 'CREAR',
    entidad: 'tarea',
    idRegistro: 40,
    campo: null,
    valorAnterior: null,
    valorNuevo: null,
    detalle: '',
    ...parcial,
  };
}

describe('vista del historial', () => {
  it('quién ve y qué notas', () => {
    const ubicacion = {
      proyecto: proyecto({ idDocente: 7 }),
      equipo: equipo({ miembros: [Ana, Luis] }),
    };

    expect(puedeVerHistorial(ubicacion, 7)).toBe(true);
    expect(puedeVerHistorial(ubicacion, 10)).toBe(true);
    expect(puedeVerHistorial(ubicacion, 11)).toBe(true);
    expect(puedeVerHistorial(ubicacion, 12)).toBe(false);

    const entradas = [
      entrada({ idLog: 1, accion: 'CONFIRMAR_NOTA', campo: 'nota_final:10' }),
      entrada({ idLog: 2, accion: 'CONFIRMAR_NOTA', campo: 'nota_final:11' }),
      entrada({ idLog: 3, accion: 'CREAR', campo: null }),
      entrada({ idLog: 4, accion: 'CONFIRMAR_NOTA', campo: null }),
    ];

    const ids = (resultado: EntradaHistorial[]) => resultado.map((e) => e.idLog);
    expect(ids(entradasVisibles(entradas, { idUsuario: 10, esDocente: false }))).toEqual([1, 3]);
    expect(ids(entradasVisibles(entradas, { idUsuario: 11, esDocente: false }))).toEqual([2, 3]);
    expect(ids(entradasVisibles(entradas, { idUsuario: 7, esDocente: true }))).toEqual([1, 2, 3, 4]);
  });

  it('índice y referencias', () => {
    const eq = equipo({ idEquipo: 7, miembros: [Ana, Luis] });
    const hitos = [
      hito({ idHitoEquipo: 12, orden: 1, nombreHito: 'Análisis' }),
      hito({ idHitoEquipo: 13, idHito: 2, orden: 2, nombreHito: 'Diseño' }),
    ];
    const tableros = [
      {
        idHitoEquipo: 12,
        tareas: [tarea({ idTarea: 41, titulo: 'Casos de uso' }), tarea({ idTarea: 40, titulo: 'Requisitos' })],
      },
      {
        idHitoEquipo: 13,
        tareas: [tarea({ idTarea: 50, idHitoEquipo: 13, titulo: 'Diagramas' })],
      },
    ];

    const indice = indiceEquipo(eq, hitos, tableros);

    expect(referenciaDe({ entidad: 'tarea', idRegistro: 40 }, indice)).toEqual({
      texto: 'T1 · Requisitos',
      ruta: '/tareas/40',
      contexto: 'Hito 1 · Análisis',
    });
    expect(referenciaDe({ entidad: 'tarea', idRegistro: 41 }, indice).texto).toBe('T2 · Casos de uso');
    expect(referenciaDe({ entidad: 'tarea', idRegistro: 50 }, indice)).toEqual({
      texto: 'T1 · Diagramas',
      ruta: '/tareas/50',
      contexto: 'Hito 2 · Diseño',
    });
    expect(referenciaDe({ entidad: 'tarea', idRegistro: 99 }, indice)).toEqual({
      texto: 'Tarea eliminada',
      ruta: null,
      contexto: null,
    });
    expect(referenciaDe({ entidad: 'tarea', idRegistro: null }, indice)).toEqual({
      texto: 'Tarea eliminada',
      ruta: null,
      contexto: null,
    });
    expect(referenciaDe({ entidad: 'hito_equipo', idRegistro: 13 }, indice)).toEqual({
      texto: 'Hito 2 · Diseño',
      ruta: '/equipos/7/tablero?hito=13',
      contexto: null,
    });
    expect(referenciaDe({ entidad: 'hito_equipo', idRegistro: 99 }, indice)).toEqual({
      texto: 'Hito',
      ruta: null,
      contexto: null,
    });
    expect(referenciaDe({ entidad: 'equipo', idRegistro: 7 }, indice).texto).toBe('Equipo');
    expect(referenciaDe({ entidad: 'miembro_equipo', idRegistro: 7 }, indice).texto).toBe('Integrantes');
    expect(referenciaDe({ entidad: 'evaluacion_hito', idRegistro: 1 }, indice).texto).toBe('evaluacion_hito');

    expect(indice.tareasPorHito.get(12)?.map((t) => t.idTarea)).toEqual([41, 40]);
    expect(indice.nombres.get(11)).toBe('Luis');
  });

  it('detalle legible', () => {
    const nombres = new Map<number, string>([
      [10, 'Ana'],
      [11, 'Luis'],
    ]);
    const docente: Visor = { idUsuario: 7, esDocente: true };
    const visorAna: Visor = { idUsuario: 10, esDocente: false };
    const visorLuis: Visor = { idUsuario: 11, esDocente: false };

    expect(
      detalleLegible(entrada({ campo: 'id_encargado', valorAnterior: '10', valorNuevo: '11' }), nombres, docente),
    ).toBe('Encargado: Ana → Luis');
    expect(
      detalleLegible(entrada({ campo: 'id_encargado', valorAnterior: 'null', valorNuevo: '11' }), nombres, docente),
    ).toBe('Encargado: Sin asignar → Luis');
    expect(
      detalleLegible(entrada({ campo: 'id_encargado', valorAnterior: '10', valorNuevo: '99' }), nombres, docente),
    ).toBe('Encargado: Ana → Exintegrante');

    expect(
      detalleLegible(entrada({ campo: 'id_usuario', valorAnterior: null, valorNuevo: '11' }), nombres, docente),
    ).toBe('Integrante: Luis');
    expect(
      detalleLegible(entrada({ campo: 'id_usuario', valorAnterior: '11', valorNuevo: null }), nombres, docente),
    ).toBe('Integrante: Luis');
    expect(detalleLegible(entrada({ campo: 'lider', valorAnterior: null, valorNuevo: '10' }), nombres, docente)).toBe(
      'Líder: Ana',
    );

    expect(
      detalleLegible(entrada({ campo: 'nota_final:11', valorAnterior: null, valorNuevo: '4.50' }), nombres, docente),
    ).toBe('Nota final de Luis: 4,50');
    expect(
      detalleLegible(entrada({ campo: 'nota_final:11', valorAnterior: null, valorNuevo: '4.50' }), nombres, visorLuis),
    ).toBe('Tu nota final: 4,50');
    expect(
      detalleLegible(entrada({ campo: 'nota_final:11', valorAnterior: '4.50', valorNuevo: '3.80' }), nombres, docente),
    ).toBe('Nota final de Luis: 4,50 → 3,80');
    expect(
      detalleLegible(entrada({ campo: 'nota_final:99', valorAnterior: null, valorNuevo: '4.00' }), nombres, docente),
    ).toBe('Nota final de Exintegrante: 4,00');

    expect(
      detalleLegible(entrada({ campo: 'nota_grupal', valorAnterior: '4.15', valorNuevo: '4.30' }), nombres, docente),
    ).toBe('Nota grupal: 4,15 → 4,30');
    expect(
      detalleLegible(entrada({ campo: 'nota_grupal', valorAnterior: '4.15', valorNuevo: '4.30' }), nombres, visorAna),
    ).toBe('Nota grupal: 4,30');
    expect(
      detalleLegible(entrada({ campo: 'nota_grupal', valorAnterior: null, valorNuevo: '4.15' }), nombres, visorAna),
    ).toBe('Nota grupal: 4,15');

    expect(
      detalleLegible(entrada({ campo: 'solicitud_apoyo', valorAnterior: null, valorNuevo: '33' }), nombres, docente),
    ).toBeNull();
    expect(
      detalleLegible(
        entrada({ campo: 'solicitud_correccion', valorAnterior: null, valorNuevo: '33' }),
        nombres,
        docente,
      ),
    ).toBeNull();
    expect(
      detalleLegible(entrada({ campo: 'tarea_parcial', valorAnterior: null, valorNuevo: '33' }), nombres, docente),
    ).toBeNull();

    expect(detalleLegible(entrada({ campo: null, detalle: 'Diseño de BD' }), nombres, docente)).toBe('Diseño de BD');
    expect(detalleLegible(entrada({ campo: null, detalle: '' }), nombres, docente)).toBeNull();
    expect(detalleLegible(entrada({ campo: 'estado', detalle: 'estado: EN_CURSO -> ENVIADO' }), nombres, docente)).toBe(
      'estado: EN_CURSO -> ENVIADO',
    );
    expect(
      detalleLegible(
        entrada({ campo: 'id_encargado', valorAnterior: null, valorNuevo: null, detalle: '' }),
        nombres,
        docente,
      ),
    ).toBeNull();
  });

  it('detalle legible para estado, avance, prerrequisito y evidencia', () => {
    const nombres = new Map<number, string>([
      [10, 'Ana'],
      [11, 'Luis'],
    ]);
    const docente: Visor = { idUsuario: 7, esDocente: true };
    const tareas = new Map<number, { codigo: string; titulo: string }>([
      [40, { codigo: 'T1', titulo: 'Levantar requisitos' }],
    ]);

    expect(
      detalleLegible(
        entrada({ campo: 'estado', valorAnterior: 'PENDIENTE', valorNuevo: 'COMPLETADA' }),
        nombres,
        docente,
      ),
    ).toBe('Estado: Pendiente → Completada');
    expect(
      detalleLegible(entrada({ campo: 'estado', valorAnterior: 'EN_CURSO', valorNuevo: 'ENVIADO' }), nombres, docente),
    ).toBe('Estado: En progreso → En Revisión');
    expect(
      detalleLegible(entrada({ campo: 'estado', valorAnterior: null, valorNuevo: 'EN_PROGRESO' }), nombres, docente),
    ).toBe('Estado: En progreso');
    expect(
      detalleLegible(entrada({ campo: 'estado', valorAnterior: null, valorNuevo: 'OTRO' }), nombres, docente),
    ).toBe('Estado: OTRO');
    expect(
      detalleLegible(
        entrada({
          campo: 'estado',
          valorAnterior: 'PENDIENTE',
          valorNuevo: null,
          detalle: 'estado: PENDIENTE -> -',
        }),
        nombres,
        docente,
      ),
    ).toBe('estado: PENDIENTE -> -');

    expect(
      detalleLegible(entrada({ campo: 'porcentaje_avance', valorAnterior: '0', valorNuevo: '100' }), nombres, docente),
    ).toBe('Avance: 0 % → 100 %');
    expect(
      detalleLegible(entrada({ campo: 'porcentaje_avance', valorAnterior: null, valorNuevo: '60' }), nombres, docente),
    ).toBe('Avance: 60 %');
    expect(
      detalleLegible(
        entrada({
          campo: 'porcentaje_avance',
          valorAnterior: '60',
          valorNuevo: null,
          detalle: 'porcentaje_avance: 60 -> -',
        }),
        nombres,
        docente,
      ),
    ).toBe('porcentaje_avance: 60 -> -');

    expect(
      detalleLegible(
        entrada({ campo: 'prerrequisito', valorAnterior: null, valorNuevo: '40' }),
        nombres,
        docente,
        tareas,
      ),
    ).toBe('Prerrequisito agregado: T1 · Levantar requisitos');
    expect(
      detalleLegible(
        entrada({ campo: 'prerrequisito', valorAnterior: '99', valorNuevo: null }),
        nombres,
        docente,
        tareas,
      ),
    ).toBe('Prerrequisito quitado: Tarea eliminada');
    expect(
      detalleLegible(entrada({ campo: 'prerrequisito', valorAnterior: null, valorNuevo: '40' }), nombres, docente),
    ).toBe('Prerrequisito agregado');
    expect(
      detalleLegible(entrada({ campo: 'prerrequisito', valorAnterior: '40', valorNuevo: null }), nombres, docente),
    ).toBe('Prerrequisito quitado');
    expect(
      detalleLegible(
        entrada({
          campo: 'prerrequisito',
          valorAnterior: null,
          valorNuevo: null,
          detalle: 'prerrequisito: - -> -',
        }),
        nombres,
        docente,
        tareas,
      ),
    ).toBe('prerrequisito: - -> -');

    expect(
      detalleLegible(entrada({ campo: 'evidencia', valorAnterior: null, valorNuevo: 'ENLACE 1' }), nombres, docente),
    ).toBe('Evidencia: enlace');
    expect(
      detalleLegible(entrada({ campo: 'evidencia', valorAnterior: null, valorNuevo: 'PDF 3' }), nombres, docente),
    ).toBe('Evidencia: PDF');
    expect(
      detalleLegible(entrada({ campo: 'evidencia', valorAnterior: null, valorNuevo: 'IMAGEN 5' }), nombres, docente),
    ).toBe('Evidencia: imagen');
    expect(
      detalleLegible(
        entrada({
          campo: 'evidencia',
          valorAnterior: null,
          valorNuevo: 'OTRO 1',
          detalle: 'evidencia: - -> OTRO 1',
        }),
        nombres,
        docente,
      ),
    ).toBe('evidencia: - -> OTRO 1');
    expect(
      detalleLegible(
        entrada({
          campo: 'evidencia',
          valorAnterior: null,
          valorNuevo: null,
          detalle: 'evidencia: - -> -',
        }),
        nombres,
        docente,
      ),
    ).toBe('evidencia: - -> -');
  });

  it('días, horas, conteo y «más»', () => {
    expect(horaBogota('2026-09-23T15:04:00Z')).toBe('10:04');
    expect(horaBogota('x')).toBe('—');

    const entradas = [
      entrada({ idLog: 1, fechaHora: '2026-09-23T15:00:00Z' }),
      entrada({ idLog: 2, fechaHora: '2026-09-23T13:00:00Z' }),
      entrada({ idLog: 3, fechaHora: '2026-09-22T20:00:00Z' }),
      entrada({ idLog: 4, fechaHora: 'x' }),
    ];

    const grupos = agruparPorDia(entradas);
    expect(grupos.map((g) => [g.dia, g.entradas.map((e) => e.idLog)])).toEqual([
      ['2026-09-23', [1, 2]],
      ['2026-09-22', [3]],
      [null, [4]],
    ]);
    expect(agruparPorDia([])).toEqual([]);

    expect(etiquetaDia('2026-09-23', '2026-09-23')).toBe('Hoy · 23/09/2026');
    expect(etiquetaDia('2026-09-22', '2026-09-23')).toBe('Ayer · 22/09/2026');
    expect(etiquetaDia('2026-02-28', '2026-03-01')).toBe('Ayer · 28/02/2026');
    expect(etiquetaDia('2025-12-31', '2026-01-01')).toBe('Ayer · 31/12/2025');
    expect(etiquetaDia('2026-09-20', '2026-09-23')).toBe('20/09/2026');
    expect(etiquetaDia(null, '2026-09-23')).toBe('Sin fecha');

    expect(textoConteo(50, 120, 120)).toBe('Mostrando 50 de 120 cambios.');
    expect(textoConteo(1, 1, 1)).toBe('Mostrando 1 de 1 cambio.');
    expect(textoConteo(3, 3, 120)).toBe('Mostrando 3 de 3 cambios (filtrados de 120).');
    expect(textoConteo(1, 1, 120)).toBe('Mostrando 1 de 1 cambio (filtrados de 120).');

    expect(textoMostrarMas(70)).toBe('Mostrar 50 más');
    expect(textoMostrarMas(7)).toBe('Mostrar 7 más');
    expect(TAMANO_PAGINA).toBe(50);
  });
});
