import type {
  CalificacionSolicitada,
  CriterioVista,
  EvaluacionVista,
  SolicitudEvaluacion,
} from '../api/tipos';
import { textoOpcional, validarTexto } from '../tarea/formularios';
import { centesimasNota, textoCentesimas } from './calculo';

export interface BorradorCriterio {
  nota: string;
  retroalimentacion: string;
}

export interface BorradorEvaluacion {
  criterios: Record<number, BorradorCriterio>;
  notaGrupal: string;
  retroalimentacion: string;
}

export type ErroresCriterio = Partial<Record<'nota' | 'retroalimentacion', string>>;

export interface ErroresEvaluacion {
  criterios: Record<number, ErroresCriterio>;
  notaGrupal?: string;
  retroalimentacion?: string;
}

export const ERROR_NOTA_VACIA = 'Escribe la nota.';
export const ERROR_NOTA_FORMATO = 'Usa un número con hasta 2 decimales.';
export const ERROR_NOTA_05 = 'Escribe una nota de 0 a 5 con hasta 2 decimales.';

/** Una entrada por criterio de la rúbrica actual. Prellenado si existe evaluación anterior. */
export function borradorInicial(
  criterios: CriterioVista[],
  anterior: EvaluacionVista | null
): BorradorEvaluacion {
  const criteriosBorrador: Record<number, BorradorCriterio> = {};
  const anterioresMap = new Map(anterior?.criterios.map((c) => [c.idCriterio, c]));
  for (const c of criterios) {
    const previo = anterioresMap.get(c.idCriterio);
    if (previo !== undefined) {
      criteriosBorrador[c.idCriterio] = {
        nota: textoCentesimas(previo.nota),
        retroalimentacion: previo.retroalimentacion ?? '',
      };
    } else {
      criteriosBorrador[c.idCriterio] = {
        nota: '',
        retroalimentacion: '',
      };
    }
  }

  return {
    criterios: criteriosBorrador,
    notaGrupal: '',
    retroalimentacion: anterior?.retroalimentacion ?? '',
  };
}

/** Valida una nota de criterio contra su puntaje máximo. */
export function validarNotaCriterio(texto: string, puntajeMaximo: number): string | undefined {
  const recortado = texto.trim();
  if (recortado === '') {
    return ERROR_NOTA_VACIA;
  }
  const valor = centesimasNota(recortado);
  if (valor === null) {
    return ERROR_NOTA_FORMATO;
  }
  const maximo = Math.round(puntajeMaximo * 100);
  if (valor > maximo) {
    return `La nota máxima de este criterio es ${textoCentesimas(puntajeMaximo)}.`;
  }
  return undefined;
}

/** Valida una nota de 0 a 5 con hasta 2 decimales (nota grupal o individual). */
export function validarNota05(texto: string): string | undefined {
  const valor = centesimasNota(texto);
  if (valor === null || valor > 500) {
    return ERROR_NOTA_05;
  }
  return undefined;
}

/** Valida todos los campos del borrador de evaluación. */
export function validarEvaluacion(
  b: BorradorEvaluacion,
  criterios: CriterioVista[]
): ErroresEvaluacion {
  const erroresCriterios: Record<number, ErroresCriterio> = {};
  for (const c of criterios) {
    const bc = b.criterios[c.idCriterio] ?? { nota: '', retroalimentacion: '' };
    const errorNota = validarNotaCriterio(bc.nota, c.puntajeMaximo);
    const errorRetro = validarTexto(bc.retroalimentacion, 2000, null);
    if (errorNota !== undefined || errorRetro !== undefined) {
      const eCriterio: ErroresCriterio = {};
      if (errorNota !== undefined) eCriterio.nota = errorNota;
      if (errorRetro !== undefined) eCriterio.retroalimentacion = errorRetro;
      erroresCriterios[c.idCriterio] = eCriterio;
    }
  }

  const errores: ErroresEvaluacion = { criterios: erroresCriterios };

  const notaGrupalRecortada = b.notaGrupal.trim();
  if (notaGrupalRecortada !== '') {
    const errorGrupal = validarNota05(b.notaGrupal);
    if (errorGrupal !== undefined) {
      errores.notaGrupal = errorGrupal;
    }
  }

  const errorRetroGeneral = validarTexto(b.retroalimentacion, 4000, null);
  if (errorRetroGeneral !== undefined) {
    errores.retroalimentacion = errorRetroGeneral;
  }

  return errores;
}

/** true si no hay criterios con error ni notaGrupal ni retroalimentacion. */
export function sinErrores(e: ErroresEvaluacion): boolean {
  return (
    Object.keys(e.criterios).length === 0 &&
    e.notaGrupal === undefined &&
    e.retroalimentacion === undefined
  );
}

/** Clave del primer campo con error en el orden de la pantalla. */
export function primerCampoConError(
  e: ErroresEvaluacion,
  criterios: CriterioVista[]
): string | null {
  for (const c of criterios) {
    const ec = e.criterios[c.idCriterio];
    if (ec?.nota !== undefined) {
      return `nota-${c.idCriterio}`;
    }
    if (ec?.retroalimentacion !== undefined) {
      return `retro-${c.idCriterio}`;
    }
  }
  if (e.notaGrupal !== undefined) {
    return 'notaGrupal';
  }
  if (e.retroalimentacion !== undefined) {
    return 'retroalimentacion';
  }
  return null;
}

/** Convierte el borrador válido en la solicitud para el API. */
export function solicitudEvaluacion(
  b: BorradorEvaluacion,
  criterios: CriterioVista[]
): SolicitudEvaluacion {
  const calificaciones: CalificacionSolicitada[] = criterios.map((c) => {
    const bc = b.criterios[c.idCriterio] ?? { nota: '', retroalimentacion: '' };
    const cent = centesimasNota(bc.nota) ?? 0;
    return {
      idCriterio: c.idCriterio,
      nota: cent / 100,
      retroalimentacion: textoOpcional(bc.retroalimentacion),
    };
  });

  const notaGrupalTrim = b.notaGrupal.trim();
  const notaGrupal = notaGrupalTrim === '' ? null : (centesimasNota(b.notaGrupal) ?? 0) / 100;

  return {
    criterios: calificaciones,
    notaGrupal,
    retroalimentacion: textoOpcional(b.retroalimentacion),
  };
}

/** true solo si calculada no es nula, notaGrupal es válida de 0 a 5 y difiere de la calculada. */
export function difiereDeCalculada(notaGrupal: string, calculada: number | null): boolean {
  if (calculada === null || notaGrupal.trim() === '') {
    return false;
  }
  if (validarNota05(notaGrupal) !== undefined) {
    return false;
  }
  const cent = centesimasNota(notaGrupal);
  if (cent === null) {
    return false;
  }
  return cent !== Math.round(calculada * 100);
}

/** (centesimasNota(texto) ?? 0) / 100. Solo se usa tras validar. */
export function numeroNota(texto: string): number {
  return (centesimasNota(texto) ?? 0) / 100;
}
