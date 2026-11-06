import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { EnvioVista, TareaVista } from '../api/tipos';
import { codigosDeTareas } from '../tablero/marcas';
import {
  SOLO_INTEGRANTES,
  enviarConResultado,
  pendientesDelEnvio,
  type ApiEnvio,
} from './envio';

function tarea(parcial: Partial<TareaVista> = {}): TareaVista {
  return {
    idTarea: 1,
    idHitoEquipo: 10,
    titulo: 'Tarea de prueba',
    descripcion: null,
    idEncargado: null,
    encargado: null,
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

describe('envío del hito', () => {
  it('envío exitoso devuelve el EnvioVista y realiza una sola llamada', async () => {
    const llamadas: number[] = [];
    const envioEsperado: EnvioVista = {
      idEnvio: 1,
      idHitoEquipo: 10,
      idUsuarioEnvia: 2,
      enviadoPor: 'Samuel',
      fechaEnvio: '2026-09-22T10:00:00Z',
    };
    const apiFalsa: ApiEnvio = {
      async enviarHito(id) {
        llamadas.push(id);
        return envioEsperado;
      },
    };

    const res = await enviarConResultado(apiFalsa, 10, [], new Map());
    expect(llamadas).toEqual([10]);
    expect(res).toEqual({ tipo: 'enviado', envio: envioEsperado });
  });

  it('rechazado por prerrequisitos incompletos cruza con tareas críticas y codigos', async () => {
    const t1 = tarea({ idTarea: 1, titulo: 'Diagramas de secuencia', estado: 'COMPLETADA', esPrerrequisitoCritico: true });
    const t2 = tarea({ idTarea: 2, titulo: 'Diagramas de secuencia', estado: 'EN_PROGRESO', porcentajeAvance: 60, esPrerrequisitoCritico: true });
    const t3 = tarea({ idTarea: 3, titulo: 'Prototipos en Figma', estado: 'PENDIENTE', porcentajeAvance: 0, esPrerrequisitoCritico: false });
    const t4 = tarea({ idTarea: 4, titulo: 'Prototipos en Figma', estado: 'PENDIENTE', porcentajeAvance: 0, esPrerrequisitoCritico: true });

    const tareas = [t1, t2, t3, t4];
    const codigos = codigosDeTareas(tareas);

    const pendientes = pendientesDelEnvio(
      ['Diagramas de secuencia', 'Prototipos en Figma', 'Otra'],
      tareas,
      codigos
    );

    expect(pendientes).toEqual([
      { clave: 'tarea-2', nombre: 'T2 · Diagramas de secuencia', porcentaje: 60 },
      { clave: 'tarea-4', nombre: 'T4 · Prototipos en Figma', porcentaje: 0 },
      { clave: 'titulo-2', nombre: 'Otra', porcentaje: null },
    ]);

    const t5 = tarea({ idTarea: 5, titulo: 'Crítica Repetida', estado: 'PENDIENTE', esPrerrequisitoCritico: true });
    const t6 = tarea({ idTarea: 6, titulo: 'Crítica Repetida', estado: 'PENDIENTE', esPrerrequisitoCritico: true });
    const pendientesRepetidos = pendientesDelEnvio(
      ['Crítica Repetida', 'Crítica Repetida'],
      [t5, t6],
      codigosDeTareas([t5, t6])
    );
    expect(pendientesRepetidos[0].clave).not.toBe(pendientesRepetidos[1].clave);
  });

  it('errores 403, 409 HITO_NO_ENVIABLE y errores inesperados nunca lanzan', async () => {
    const api403: ApiEnvio = {
      async enviarHito() {
        throw new ErrorApi(403, 'ACCESO_DENEGADO', 'Prohibido', []);
      },
    };
    const res403 = await enviarConResultado(api403, 10, [], new Map());
    expect(res403).toEqual({ tipo: 'error', texto: SOLO_INTEGRANTES });

    const api409: ApiEnvio = {
      async enviarHito() {
        throw new ErrorApi(409, 'HITO_NO_ENVIABLE', 'El hito ya fue enviado', []);
      },
    };
    const res409 = await enviarConResultado(api409, 10, [], new Map());
    expect(res409).toEqual({ tipo: 'error', texto: 'El hito ya fue enviado' });

    const apiError: ApiEnvio = {
      async enviarHito() {
        throw new Error('Fallo de red');
      },
    };
    const resError = await enviarConResultado(apiError, 10, [], new Map());
    expect(resError).toEqual({ tipo: 'error', texto: 'Ocurrió un error inesperado. Recarga la página' });
  });
});
