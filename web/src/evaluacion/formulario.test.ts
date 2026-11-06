import { describe, expect, it } from 'vitest';
import type { CriterioVista, EvaluacionVista } from '../api/tipos';
import {
  borradorInicial,
  difiereDeCalculada,
  ERROR_NOTA_05,
  numeroNota,
  primerCampoConError,
  sinErrores,
  solicitudEvaluacion,
  validarEvaluacion,
  validarNota05,
  type BorradorEvaluacion,
} from './formulario';

function criterio(parcial: Partial<CriterioVista>): CriterioVista {
  return {
    idCriterio: 1,
    idHito: 1,
    descripcion: 'Criterio de prueba',
    pesoPorcentaje: 100,
    puntajeMaximo: 5,
    ...parcial,
  };
}

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

describe('formulario', () => {
  it('inicializa el borrador vacío o con datos de la evaluación anterior', () => {
    const rubrica = [criterio({ idCriterio: 1 }), criterio({ idCriterio: 2 })];

    const vacio = borradorInicial(rubrica, null);
    expect(vacio.criterios[1]).toEqual({ nota: '', retroalimentacion: '' });
    expect(vacio.criterios[2]).toEqual({ nota: '', retroalimentacion: '' });
    expect(vacio.notaGrupal).toBe('');
    expect(vacio.retroalimentacion).toBe('');

    const anterior = evaluacion({
      criterios: [
        {
          idCriterio: 1,
          descripcion: 'C1',
          pesoPorcentaje: 50,
          puntajeMaximo: 5,
          nota: 3.5,
          retroalimentacion: 'Bien',
        },
        {
          idCriterio: 9,
          descripcion: 'C9 viejo',
          pesoPorcentaje: 50,
          puntajeMaximo: 5,
          nota: 4,
          retroalimentacion: 'Viejo',
        },
      ],
      retroalimentacion: 'General',
    });

    const prellenado = borradorInicial(rubrica, anterior);
    expect(prellenado.criterios[1]).toEqual({ nota: '3,5', retroalimentacion: 'Bien' });
    expect(prellenado.criterios[2]).toEqual({ nota: '', retroalimentacion: '' });
    expect(Object.keys(prellenado.criterios)).toEqual(['1', '2']);
    expect(prellenado.notaGrupal).toBe('');
    expect(prellenado.retroalimentacion).toBe('General');
  });

  it('valida notas de la rúbrica, nota grupal y retroalimentaciones', () => {
    const rubrica = [
      criterio({ idCriterio: 1, puntajeMaximo: 5 }),
      criterio({ idCriterio: 2, puntajeMaximo: 10 }),
    ];

    const borradorInvalido: BorradorEvaluacion = {
      criterios: {
        1: { nota: '', retroalimentacion: '' },
        2: { nota: '10,5', retroalimentacion: 'a'.repeat(2001) },
      },
      notaGrupal: '6',
      retroalimentacion: 'b'.repeat(4001),
    };

    const errores = validarEvaluacion(borradorInvalido, rubrica);
    expect(errores.criterios[1]?.nota).toBe('Escribe la nota.');
    expect(errores.criterios[2]?.nota).toBe('La nota máxima de este criterio es 10.');
    expect(errores.criterios[2]?.retroalimentacion).toBe('Máximo 2000 caracteres.');
    expect(errores.notaGrupal).toBe('Escribe una nota de 0 a 5 con hasta 2 decimales.');
    expect(errores.retroalimentacion).toBe('Máximo 4000 caracteres.');
    expect(primerCampoConError(errores, rubrica)).toBe('nota-1');

    const conFormato = validarEvaluacion(
      {
        ...borradorInvalido,
        criterios: {
          1: { nota: '4,555', retroalimentacion: '' },
          2: { nota: '5', retroalimentacion: '' },
        },
      },
      rubrica
    );
    expect(conFormato.criterios[1]?.nota).toBe('Usa un número con hasta 2 decimales.');

    const soloRetro2 = validarEvaluacion(
      {
        criterios: {
          1: { nota: '4', retroalimentacion: '' },
          2: { nota: '5', retroalimentacion: 'x'.repeat(2001) },
        },
        notaGrupal: '',
        retroalimentacion: '',
      },
      rubrica
    );
    expect(primerCampoConError(soloRetro2, rubrica)).toBe('retro-2');

    const soloGrupal = validarEvaluacion(
      {
        criterios: {
          1: { nota: '4', retroalimentacion: '' },
          2: { nota: '5', retroalimentacion: '' },
        },
        notaGrupal: '6',
        retroalimentacion: '',
      },
      rubrica
    );
    expect(primerCampoConError(soloGrupal, rubrica)).toBe('notaGrupal');

    const valido: BorradorEvaluacion = {
      criterios: {
        1: { nota: '5', retroalimentacion: '' },
        2: { nota: '7,25', retroalimentacion: '' },
      },
      notaGrupal: '',
      retroalimentacion: '',
    };
    const erroresValido = validarEvaluacion(valido, rubrica);
    expect(sinErrores(erroresValido)).toBe(true);
    expect(primerCampoConError(erroresValido, rubrica)).toBeNull();
  });

  it('construye la solicitud para el API y valida diferencias con la nota calculada', () => {
    const rubrica = [
      criterio({ idCriterio: 1, puntajeMaximo: 5 }),
      criterio({ idCriterio: 2, puntajeMaximo: 10 }),
    ];

    const borradorDesordenado: BorradorEvaluacion = {
      criterios: {
        2: { nota: '7,25', retroalimentacion: '  ' },
        1: { nota: '5', retroalimentacion: ' Muy bien ' },
      },
      notaGrupal: '',
      retroalimentacion: '  ',
    };

    const sol = solicitudEvaluacion(borradorDesordenado, rubrica);
    expect(sol).toEqual({
      criterios: [
        { idCriterio: 1, nota: 5, retroalimentacion: 'Muy bien' },
        { idCriterio: 2, nota: 7.25, retroalimentacion: null },
      ],
      notaGrupal: null,
      retroalimentacion: null,
    });

    const solConGrupal = solicitudEvaluacion({ ...borradorDesordenado, notaGrupal: '4,3' }, rubrica);
    expect(solConGrupal.notaGrupal).toBe(4.3);

    expect(difiereDeCalculada('4,3', 4.15)).toBe(true);
    expect(difiereDeCalculada('4,15', 4.15)).toBe(false);
    expect(difiereDeCalculada('', 4.15)).toBe(false);
    expect(difiereDeCalculada('4,3', null)).toBe(false);
    expect(difiereDeCalculada('9', 4.15)).toBe(false);

    expect(validarNota05('5')).toBeUndefined();
    expect(validarNota05('0')).toBeUndefined();
    expect(validarNota05('5,01')).toBe(ERROR_NOTA_05);
    expect(validarNota05('x')).toBe(ERROR_NOTA_05);
    expect(validarNota05('')).toBe(ERROR_NOTA_05);

    expect(numeroNota('4,2')).toBe(4.2);
  });
});
