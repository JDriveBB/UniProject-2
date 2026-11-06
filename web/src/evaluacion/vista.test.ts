import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { EvaluacionVista, ProyectoVista, TableroHito } from '../api/tipos';
import {
  CAMPOS_EVALUACION,
  esConflicto,
  estadoNota,
  hayConfirmadas,
  mensajeErrorEvaluacion,
  modoDocente,
  notaAjustada,
  textoCaracter,
  textoConfirmarRegistro,
  textoDiferencia,
  textoRegistrada,
  vencido,
} from './vista';

function evaluacion(parcial: Partial<EvaluacionVista>): EvaluacionVista {
  return {
    idEvaluacion: 1,
    idHitoEquipo: 1,
    idEnvio: 1,
    fechaEnvio: '2026-09-23T15:00:00Z',
    fechaEvaluacion: '2026-09-23T15:00:00Z',
    notaGrupal: 4,
    notaCalculada: 4,
    retroalimentacion: null,
    caracter: 'PROVISIONAL',
    criterios: [],
    notas: [],
    ...parcial,
  };
}

describe('vista', () => {
  it('genera textos, marcas y validaciones de estado y fechas', () => {
    expect(textoCaracter('PROVISIONAL', '2026-09-22')).toBe(
      'Nota provisional: el equipo puede seguir mejorando el hito hasta el 22/09/2026. Al vencer esa fecha, la última nota queda como definitiva.'
    );
    expect(textoCaracter('DEFINITIVA', '2026-09-22')).toBe(
      'Nota definitiva: la fecha límite del hito ya pasó.'
    );

    expect(vencido('2026-09-22', '2026-09-22')).toBe(false);
    expect(vencido('2026-09-22', '2026-09-23')).toBe(true);

    expect(textoConfirmarRegistro('2026-09-22', '2026-09-22')).toBe(
      'La nota quedará como provisional: el equipo puede seguir editando hasta el 22/09/2026. Los estudiantes recibirán el aviso.'
    );
    expect(textoConfirmarRegistro('2026-09-22', '2026-09-23')).toBe(
      'La fecha límite del hito ya pasó: la nota quedará como definitiva y el equipo ya no podrá editar sus tareas. Los estudiantes recibirán el aviso.'
    );

    expect(textoRegistrada(evaluacion({ notaGrupal: 4.15 }))).toBe(
      'Evaluación registrada como provisional: nota grupal 4,15. Los estudiantes recibirán el aviso.'
    );
    expect(textoRegistrada(evaluacion({ caracter: 'DEFINITIVA', notaGrupal: 4.15 }))).toBe(
      'Evaluación registrada como definitiva: nota grupal 4,15. Los estudiantes recibirán el aviso.'
    );

    expect(textoDiferencia(4.15)).toBe(
      'La nota grupal difiere de la calculada (4,15). El cambio queda en el registro de auditoría.'
    );

    expect(
      estadoNota({
        idEstudiante: 1,
        nombre: 'Ana',
        factorAjuste: 1,
        notaSugerida: 4,
        notaFinal: 4,
        fechaConfirmacion: '2026-09-23T15:00:00Z',
      })
    ).toEqual({ texto: 'Confirmada', tono: 'exito' });
    expect(
      estadoNota({
        idEstudiante: 1,
        nombre: 'Ana',
        factorAjuste: 1,
        notaSugerida: 4,
        notaFinal: null,
        fechaConfirmacion: null,
      })
    ).toEqual({ texto: 'Sin confirmar', tono: 'neutro' });

    expect(
      hayConfirmadas([
        {
          idEstudiante: 1,
          nombre: 'Ana',
          factorAjuste: 1,
          notaSugerida: 4,
          notaFinal: 4.5,
          fechaConfirmacion: '2026-09-23T15:00:00Z',
        },
        {
          idEstudiante: 2,
          nombre: 'Beto',
          factorAjuste: 1,
          notaSugerida: 4,
          notaFinal: null,
          fechaConfirmacion: null,
        },
      ])
    ).toBe(true);
    expect(
      hayConfirmadas([
        {
          idEstudiante: 1,
          nombre: 'Ana',
          factorAjuste: 1,
          notaSugerida: 4,
          notaFinal: null,
          fechaConfirmacion: null,
        },
      ])
    ).toBe(false);
    expect(hayConfirmadas([])).toBe(false);

    expect(notaAjustada(evaluacion({ notaGrupal: 4, notaCalculada: null }))).toBe(false);
    expect(notaAjustada(evaluacion({ notaGrupal: 4.3, notaCalculada: 4.15 }))).toBe(true);
    expect(notaAjustada(evaluacion({ notaGrupal: 4.15, notaCalculada: 4.15 }))).toBe(false);
  });

  it('determina el modo docente, mensajes de error y detección de conflicto', () => {
    const proyecto: ProyectoVista = {
      idProyecto: 1,
      idDocente: 7,
      nombre: 'P1',
      descripcion: 'D1',
      fechaInicio: '2026-08-01',
      fechaFin: '2026-11-30',
      usaMonitores: false,
    };
    const tableroEnviado: TableroHito = {
      idHitoEquipo: 5,
      idHito: 2,
      nombreHito: 'Diseño',
      idEquipo: 7,
      nombreEquipo: 'Equipo 7',
      idProyecto: 1,
      nombreProyecto: 'P1',
      fechaLimite: '2026-09-30',
      estado: 'ENVIADO',
      editable: false,
      tareas: [],
    };

    expect(
      modoDocente({ proyecto, tablero: tableroEnviado, evaluacion: null }, 8)
    ).toBe('sinAcceso');

    const tableroEnCurso: TableroHito = { ...tableroEnviado, estado: 'EN_CURSO' };
    expect(
      modoDocente({ proyecto, tablero: tableroEnCurso, evaluacion: null }, 7)
    ).toBe('noEnviado');

    expect(
      modoDocente({ proyecto, tablero: tableroEnviado, evaluacion: null }, 7)
    ).toBe('evaluar');
    expect(
      modoDocente({ proyecto, tablero: tableroEnviado, evaluacion: evaluacion({}) }, 7)
    ).toBe('evaluar');

    const tableroEvaluado: TableroHito = { ...tableroEnviado, estado: 'EVALUADO' };
    expect(
      modoDocente({ proyecto, tablero: tableroEvaluado, evaluacion: evaluacion({}) }, 7)
    ).toBe('evaluado');
    expect(
      modoDocente({ proyecto, tablero: tableroEvaluado, evaluacion: null }, 7)
    ).toBe('noEnviado');

    expect(
      mensajeErrorEvaluacion(new ErrorApi(403, 'ACCESO_DENEGADO', 'Acceso denegado'), CAMPOS_EVALUACION)
    ).toBe('Solo el docente del proyecto puede evaluar este hito.');

    expect(
      mensajeErrorEvaluacion(
        new ErrorApi(
          409,
          'NOTAS_CONFIRMADAS',
          'Ya confirmaste notas individuales: la nota grupal no se puede cambiar'
        ),
        CAMPOS_EVALUACION
      )
    ).toBe('Ya confirmaste notas individuales: la nota grupal no se puede cambiar');

    expect(
      mensajeErrorEvaluacion(
        new ErrorApi(400, 'VALIDACION', 'Error de validación', [
          'criterios[0].nota: debe ser mayor o igual a 0',
        ]),
        CAMPOS_EVALUACION
      )
    ).toBe('Revisa estos campos: Notas de la rúbrica');

    expect(esConflicto(new ErrorApi(409, 'CONFLICTO', 'Conflicto'))).toBe(true);
    expect(esConflicto(new ErrorApi(400, 'VALIDACION', 'Error'))).toBe(false);
    expect(esConflicto(new Error('x'))).toBe(false);
  });
});
