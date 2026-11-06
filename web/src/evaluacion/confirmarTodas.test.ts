import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { EvaluacionVista, NotaIndividualVista } from '../api/tipos';
import { confirmarTodas } from './confirmarTodas';
import { textoConfirmarTodas } from './vista';

function crearEvaluacion(notas: Partial<NotaIndividualVista>[] = []): EvaluacionVista {
  return {
    idEvaluacion: 10,
    idHitoEquipo: 5,
    idEnvio: 1,
    fechaEnvio: '2026-11-01T10:00:00Z',
    fechaEvaluacion: '2026-11-01T10:00:00Z',
    notaGrupal: 4.0,
    notaCalculada: 4.0,
    retroalimentacion: null,
    caracter: 'PROVISIONAL',
    criterios: [],
    notas: notas.map((n, i) => ({
      idEstudiante: n.idEstudiante ?? i + 1,
      nombre: n.nombre ?? `Estudiante ${i + 1}`,
      factorAjuste: n.factorAjuste ?? 1.0,
      notaSugerida: n.notaSugerida ?? 4.0,
      notaFinal: n.notaFinal ?? null,
      fechaConfirmacion: n.fechaConfirmacion ?? null,
    })),
  };
}

describe('confirmarTodas', () => {
  it('(a) todas OK: confirma todas las filas y devuelve la última evaluación', async () => {
    const llamadas: { idEvaluacion: number; idEstudiante: number; notaFinal: number }[] = [];
    const api = {
      confirmarNota: async (idEvaluacion: number, idEstudiante: number, notaFinal: number) => {
        llamadas.push({ idEvaluacion, idEstudiante, notaFinal });
        return crearEvaluacion([
          { idEstudiante, notaFinal, fechaConfirmacion: '2026-11-06T12:00:00Z' },
        ]);
      },
    };

    const filas = [
      { idEstudiante: 1, nombre: 'Ana', notaFinal: 4.5 },
      { idEstudiante: 2, nombre: 'Beto', notaFinal: 3.8 },
    ];

    const res = await confirmarTodas(api, 10, filas);

    expect(llamadas).toEqual([
      { idEvaluacion: 10, idEstudiante: 1, notaFinal: 4.5 },
      { idEvaluacion: 10, idEstudiante: 2, notaFinal: 3.8 },
    ]);
    expect(res.confirmadas).toBe(2);
    expect(res.fallidas).toEqual([]);
    expect(res.reemplazada).toBe(false);
    expect(res.evaluacion).not.toBeNull();
    expect(res.evaluacion?.notas[0].idEstudiante).toBe(2);
  });

  it('(b) una falla y las demás continúan', async () => {
    const llamadas: number[] = [];
    const api = {
      confirmarNota: async (_idEvaluacion: number, idEstudiante: number, notaFinal: number) => {
        llamadas.push(idEstudiante);
        if (idEstudiante === 2) {
          throw new Error('Fallo de red al confirmar');
        }
        return crearEvaluacion([{ idEstudiante, notaFinal }]);
      },
    };

    const filas = [
      { idEstudiante: 1, nombre: 'Ana', notaFinal: 4.5 },
      { idEstudiante: 2, nombre: 'Beto', notaFinal: 3.8 },
      { idEstudiante: 3, nombre: 'Carlos', notaFinal: 5.0 },
    ];

    const res = await confirmarTodas(api, 10, filas);

    expect(llamadas).toEqual([1, 2, 3]);
    expect(res.confirmadas).toBe(2);
    expect(res.fallidas).toEqual([{ nombre: 'Beto', motivo: 'Fallo de red al confirmar' }]);
    expect(res.reemplazada).toBe(false);
    expect(res.evaluacion?.notas[0].idEstudiante).toBe(3);
  });

  it('(c) 409 EVALUACION_REEMPLAZADA detiene el resto y marca reemplazada', async () => {
    const llamadas: number[] = [];
    const api = {
      confirmarNota: async (_idEvaluacion: number, idEstudiante: number, notaFinal: number) => {
        llamadas.push(idEstudiante);
        if (idEstudiante === 2) {
          throw new ErrorApi(
            409,
            'EVALUACION_REEMPLAZADA',
            'El equipo volvió a enviar el hito: esta evaluación ya no es válida'
          );
        }
        return crearEvaluacion([{ idEstudiante, notaFinal }]);
      },
    };

    const filas = [
      { idEstudiante: 1, nombre: 'Ana', notaFinal: 4.5 },
      { idEstudiante: 2, nombre: 'Beto', notaFinal: 3.8 },
      { idEstudiante: 3, nombre: 'Carlos', notaFinal: 5.0 },
    ];

    const res = await confirmarTodas(api, 10, filas);

    expect(llamadas).toEqual([1, 2]);
    expect(res.confirmadas).toBe(1);
    expect(res.reemplazada).toBe(true);
    expect(res.fallidas).toEqual([
      {
        nombre: 'Beto',
        motivo: 'El equipo volvió a enviar el hito: esta evaluación ya no es válida',
      },
    ]);
    expect(res.evaluacion?.notas[0].idEstudiante).toBe(1);
  });

  it('(d) solo envía filas pendientes y en orden', async () => {
    const llamadas: number[] = [];
    const api = {
      confirmarNota: async (_idEvaluacion: number, idEstudiante: number, notaFinal: number) => {
        llamadas.push(idEstudiante);
        return crearEvaluacion([{ idEstudiante, notaFinal }]);
      },
    };

    const filas = [
      { idEstudiante: 1, nombre: 'Ana', notaFinal: 4.5, fechaConfirmacion: '2026-11-01T10:00:00Z' },
      { idEstudiante: 2, nombre: 'Beto', notaFinal: 3.8, fechaConfirmacion: null },
      { idEstudiante: 3, nombre: 'Carlos', notaFinal: 5.0, fechaConfirmacion: '2026-11-02T10:00:00Z' },
      { idEstudiante: 4, nombre: 'Daniela', notaFinal: 4.2 },
    ];

    const res = await confirmarTodas(api, 10, filas);

    expect(llamadas).toEqual([2, 4]);
    expect(res.confirmadas).toBe(2);
    expect(res.fallidas).toEqual([]);
    expect(res.reemplazada).toBe(false);
  });

  it('(e) textoConfirmarTodas(1) y (3) genera singular y plural', () => {
    expect(textoConfirmarTodas(1)).toBe(
      'Se confirmará 1 nota con los valores que ves. Cada confirmación queda en el registro de auditoría. Después de confirmar, la nota grupal ya no se podrá cambiar.'
    );
    expect(textoConfirmarTodas(3)).toBe(
      'Se confirmarán 3 notas con los valores que ves. Cada confirmación queda en el registro de auditoría. Después de confirmar, la nota grupal ya no se podrá cambiar.'
    );
  });

  it('devuelve evaluacion null cuando todas fallan', async () => {
    const api = {
      confirmarNota: async () => {
        throw new Error('Error al conectar');
      },
    };
    const filas = [
      { idEstudiante: 1, nombre: 'Ana', notaFinal: 4.5 },
      { idEstudiante: 2, nombre: 'Beto', notaFinal: 3.8 },
    ];
    const res = await confirmarTodas(api, 10, filas);
    expect(res.evaluacion).toBeNull();
    expect(res.confirmadas).toBe(0);
    expect(res.fallidas).toHaveLength(2);
    expect(res.reemplazada).toBe(false);
  });
});
