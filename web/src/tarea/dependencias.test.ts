import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { TareaVista } from '../api/tipos';
import {
  cambiosDeDependencias,
  guardarDependencias,
  textoCiclo,
  type ApiDependencias,
} from './dependencias';

function tareaVacia(id: number): TareaVista {
  return {
    idTarea: id,
    idHitoEquipo: 10,
    titulo: `Tarea ${id}`,
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
  };
}

describe('dependencias de tarea', () => {
  it('cálculo de cambios y sinCambios', async () => {
    expect(cambiosDeDependencias([3, 1], [1, 4, 2, 4])).toEqual({
      quitar: [3],
      agregar: [2, 4],
    });

    const llamadas: string[] = [];
    const apiFalsa: ApiDependencias = {
      async agregarDependencias(id, prerrequisitos) {
        llamadas.push(`agregar ${prerrequisitos.join(',')}`);
        return { aceptados: prerrequisitos, ciclicos: [], tarea: tareaVacia(id) };
      },
      async quitarDependencia(id, idPrerrequisito) {
        llamadas.push(`quitar ${idPrerrequisito}`);
        return tareaVacia(id);
      },
    };

    const res = await guardarDependencias(apiFalsa, 5, [1, 2, 3], [3, 1, 2]);
    expect(res).toEqual({ tipo: 'sinCambios' });
    expect(llamadas).toEqual([]);
  });

  it('guardado con quitar y agregar, cíclicos y solo quitar', async () => {
    const llamadas: string[] = [];
    const apiFalsa: ApiDependencias = {
      async agregarDependencias(id, prerrequisitos) {
        llamadas.push(`agregar ${prerrequisitos.join(',')}`);
        return { aceptados: [2], ciclicos: [4], tarea: tareaVacia(id) };
      },
      async quitarDependencia(id, idPrerrequisito) {
        llamadas.push(`quitar ${idPrerrequisito}`);
        return tareaVacia(id);
      },
    };

    const res = await guardarDependencias(apiFalsa, 5, [3, 1], [1, 2, 4]);
    expect(llamadas).toEqual(['quitar 3', 'agregar 2,4']);
    expect(res).toEqual({ tipo: 'guardado', ciclicos: [4] });

    llamadas.length = 0;
    const resSoloQuitar = await guardarDependencias(apiFalsa, 5, [3, 1], [1]);
    expect(llamadas).toEqual(['quitar 3']);
    expect(resSoloQuitar).toEqual({ tipo: 'guardado', ciclicos: [] });
  });

  it('errores al guardar dependencias y límite de 50', async () => {
    const apiFallaAgregar: ApiDependencias = {
      async agregarDependencias() {
        throw new ErrorApi(403, 'ACCESO_DENEGADO', 'No autorizado', []);
      },
      async quitarDependencia(id) {
        return tareaVacia(id);
      },
    };

    const resFallaAgregar = await guardarDependencias(apiFallaAgregar, 5, [3], [4]);
    expect(resFallaAgregar).toEqual({
      tipo: 'error',
      texto: 'No se guardaron todas las dependencias: Solo el líder del equipo puede hacerlo.',
      huboCambios: true,
    });

    const llamadas: string[] = [];
    const apiFallaQuitar: ApiDependencias = {
      async agregarDependencias(id, p) {
        llamadas.push(`agregar ${p.join(',')}`);
        return { aceptados: p, ciclicos: [], tarea: tareaVacia(id) };
      },
      async quitarDependencia() {
        throw new ErrorApi(403, 'ACCESO_DENEGADO', 'No autorizado', []);
      },
    };

    const resFallaQuitar = await guardarDependencias(apiFallaQuitar, 5, [3], [4]);
    expect(resFallaQuitar.tipo).toBe('error');
    if (resFallaQuitar.tipo === 'error') {
      expect(resFallaQuitar.huboCambios).toBe(false);
    }
    expect(llamadas).toEqual([]);

    const elegidas51 = Array.from({ length: 51 }, (_, i) => i + 10);
    const res51 = await guardarDependencias(apiFallaQuitar, 5, [], elegidas51);
    expect(res51).toEqual({
      tipo: 'error',
      texto: 'Máximo 50 tareas nuevas a la vez.',
      huboCambios: false,
    });
  });

  it('textoCiclo con una o varias dependencias circulares', () => {
    expect(textoCiclo(['T4 · Prototipos en Figma'])).toBe(
      'Dependencia circular: se excluyó T4 · Prototipos en Figma porque ya depende de esta tarea. Las demás dependencias se guardaron y la ruta crítica se recalculó.'
    );

    expect(textoCiclo(['T2 · A', 'T3 · B', 'T4 · C'])).toBe(
      'Dependencia circular: se excluyeron T2 · A, T3 · B y T4 · C porque ya dependen de esta tarea. Las demás dependencias se guardaron y la ruta crítica se recalculó.'
    );
  });
});
