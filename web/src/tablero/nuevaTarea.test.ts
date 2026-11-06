import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { TareaVista } from '../api/tipos';
import {
  crearConDependencias,
  primerCampoConError,
  solicitudDe,
  SOLO_LIDER,
  TAREA_VACIA,
  validarTarea,
} from './nuevaTarea';
import type { ApiNuevaTarea, BorradorTarea } from './nuevaTarea';

const HITO = { fechaInicio: '2026-09-01', fechaLimite: '2026-09-26' };
const HORAS_INVALIDAS = 'Usa un número mayor que 0, con hasta 2 decimales (máximo 9999,99).';
const BASE: BorradorTarea = { ...TAREA_VACIA, titulo: 'Prototipos en Figma', horas: '10' };
const CON_DOS: BorradorTarea = { ...BASE, prerrequisitos: [1, 2] };

function apiFalsa(llamadas: string[],
                  fallas: { crear?: ErrorApi; dependencias?: ErrorApi; ciclicos?: number[] } = {}): ApiNuevaTarea {
  return {
    async crearTarea(idHitoEquipo, solicitud) {
      llamadas.push('crearTarea:' + idHitoEquipo);
      if (fallas.crear !== undefined) {
        throw fallas.crear;
      }
      return { idTarea: 40, titulo: solicitud.titulo } as TareaVista;
    },
    async agregarDependencias(idTarea, prerrequisitos) {
      llamadas.push(`agregarDependencias:${idTarea}:${prerrequisitos.join(',')}`);
      if (fallas.dependencias !== undefined) {
        throw fallas.dependencias;
      }
      return { aceptados: prerrequisitos, ciclicos: fallas.ciclicos ?? [], tarea: { idTarea } as TareaVista };
    },
  };
}

describe('nueva tarea (P05)', () => {
  it('pide título y horas; el foco va al primer campo con error', () => {
    const errores = validarTarea(TAREA_VACIA, HITO);
    expect(errores).toEqual({ titulo: 'Escribe el título de la tarea.', horas: 'Escribe las horas estimadas.' });
    expect(primerCampoConError(errores)).toBe('titulo');
    expect(validarTarea(BASE, HITO)).toEqual({});
    expect(primerCampoConError({})).toBeNull();
  });

  it('límites del contrato: horas, fecha dentro del hito, 150 caracteres y 50 dependencias', () => {
    for (const horas of ['0', '0,00', '12,345', '10000', 'abc', '-1']) {
      expect(validarTarea({ ...BASE, horas }, HITO).horas).toBe(HORAS_INVALIDAS);
    }
    for (const horas of ['2,5', '2.5', '9999.99']) {
      expect(validarTarea({ ...BASE, horas }, HITO).horas).toBeUndefined();
    }
    expect(validarTarea({ ...BASE, fechaLimite: '2026-08-31' }, HITO).fechaLimite)
      .toBe('Debe estar entre 01/09/2026 y 26/09/2026 (fechas del hito).');
    expect(validarTarea({ ...BASE, fechaLimite: '2026-09-26' }, HITO).fechaLimite).toBeUndefined();
    expect(validarTarea({ ...BASE, titulo: 'x'.repeat(151) }, HITO).titulo).toBe('Máximo 150 caracteres.');
    const muchas = Array.from({ length: 51 }, (_, i) => i + 1);
    expect(validarTarea({ ...BASE, prerrequisitos: muchas }, HITO).prerrequisitos).toBe('Máximo 50 tareas.');
  });

  it('solicitudDe recorta, convierte la coma y manda null en lo vacío', () => {
    expect(solicitudDe({ ...TAREA_VACIA, titulo: '  Prototipos  ', descripcion: '   ', horas: '2,5', critico: true }))
      .toEqual({
        titulo: 'Prototipos',
        descripcion: null,
        idEncargado: null,
        horasEstimadas: 2.5,
        fechaLimite: null,
        esPrerrequisitoCritico: true,
      });
    expect(solicitudDe({ ...BASE, descripcion: ' En Figma ', idEncargado: '7', fechaLimite: '2026-09-20' })).toEqual({
      titulo: 'Prototipos en Figma',
      descripcion: 'En Figma',
      idEncargado: 7,
      horasEstimadas: 10,
      fechaLimite: '2026-09-20',
      esPrerrequisitoCritico: false,
    });
  });

  it('crea la tarea y luego sus dependencias; si la segunda llamada falla, la tarea queda creada y se avisa', async () => {
    const sinDependencias: string[] = [];
    expect(await crearConDependencias(apiFalsa(sinDependencias), 5, BASE))
      .toEqual({ tipo: 'exito', texto: 'La tarea «Prototipos en Figma» se creó.' });
    expect(sinDependencias).toEqual(['crearTarea:5']);

    const conDependencias: string[] = [];
    expect((await crearConDependencias(apiFalsa(conDependencias), 5, CON_DOS)).tipo).toBe('exito');
    expect(conDependencias).toEqual(['crearTarea:5', 'agregarDependencias:40:1,2']);

    const otroHito = new ErrorApi(400, 'PRERREQUISITO_DE_OTRO_HITO', 'Los prerrequisitos deben ser tareas del mismo hito');
    expect(await crearConDependencias(apiFalsa([], { dependencias: otroHito }), 5, CON_DOS)).toEqual({
      tipo: 'aviso',
      texto: 'La tarea «Prototipos en Figma» se creó, pero no se guardaron sus dependencias: '
        + 'Los prerrequisitos deben ser tareas del mismo hito',
    });
    expect(await crearConDependencias(apiFalsa([], { ciclicos: [2] }), 5, CON_DOS)).toEqual({
      tipo: 'aviso',
      texto: 'La tarea «Prototipos en Figma» se creó, pero algunas dependencias formarían un ciclo y no se guardaron.',
    });
  });

  it('si crear falla no se piden dependencias; 403 dice que solo el líder crea y 400 VALIDACION nombra los campos', async () => {
    const llamadas: string[] = [];
    const prohibido = new ErrorApi(403, 'ACCESO_DENEGADO', 'No tienes permiso para esta acción');
    expect(await crearConDependencias(apiFalsa(llamadas, { crear: prohibido }), 5, CON_DOS))
      .toEqual({ tipo: 'error', texto: SOLO_LIDER });
    expect(llamadas).toEqual(['crearTarea:5']);
    const invalida = new ErrorApi(400, 'VALIDACION', 'Datos inválidos', ['horasEstimadas: debe ser mayor que 0']);
    expect((await crearConDependencias(apiFalsa([], { crear: invalida }), 5, BASE)).texto)
      .toBe('Revisa estos campos: Horas estimadas');
    const cerrado = new ErrorApi(409, 'HITO_NO_EDITABLE', 'El hito está enviado a revisión: sus tareas no se pueden cambiar');
    expect((await crearConDependencias(apiFalsa([], { crear: cerrado }), 5, BASE)).texto)
      .toBe('El hito está enviado a revisión: sus tareas no se pueden cambiar');
  });
});
