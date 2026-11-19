import { validarCorreo } from '../cuentas/validacion';
import { MAXIMO_CORREOS_LOTE, separarCorreos } from './correos';
import type { ErrorAlAgregar, ResumenEstudiantes, ResumenMonitores } from './equipos';

export type Ficha = {
  correo: string;
  error: string | null;
  resultado?: string;
};

/** Estudiante habilitado que se puede elegir con un clic en el campo de fichas (D-11). */
export interface Sugerencia {
  email: string;
  /** null si aún no tiene cuenta. */
  nombre: string | null;
  registrado: boolean;
}

export const AVISO_LIMITE_CORREOS = 'Máximo 50 correos por vez.';
export const MAXIMO_SUGERENCIAS = 6;

/** Textos de correos habilitados sin cuenta: los estudiantes quedan invitados al equipo (D-12). */
export const TEXTO_HABILITADO_SIN_CUENTA = {
  estudianteSingular: 'invitado: entrará al equipo cuando cree su cuenta',
  estudiantePlural: 'invitados: entrarán al equipo cuando creen su cuenta',
  monitorSingular: 'habilitado sin cuenta (aparecerá cuando cree su cuenta)',
  monitorPlural: 'habilitados sin cuenta (aparecerán cuando creen su cuenta)',
} as const;

const MOTIVO_YA_HABILITADO_ESTUDIANTE = 'Ya estaba habilitado; aún no tiene cuenta';
const MOTIVO_YA_HABILITADO_MONITOR = 'Ya estaba habilitado';
const MOTIVO_OTRO_ROL_ESTUDIANTE = 'Tiene otro rol: no es estudiante';
const MOTIVO_OTRO_ROL_MONITOR = 'Tiene otro rol: no es monitor';

/**
 * Separa un texto con correos, descarta repetidos (sin distinguir mayúsculas) contra las fichas
 * existentes, valida cada uno con validarCorreo y respeta el máximo de 50 correos por lote.
 */
export function agregarTexto(fichas: Ficha[], texto: string): { fichas: Ficha[]; aviso: string | null } {
  const candidatos = separarCorreos(texto);
  if (candidatos.length === 0) {
    return { fichas, aviso: null };
  }

  const existentes = new Set(fichas.map((f) => f.correo.toLowerCase()));
  const sinRepetidos = candidatos.filter((correo) => !existentes.has(correo.toLowerCase()));
  if (sinRepetidos.length === 0) {
    return { fichas, aviso: null };
  }

  const disponibles = Math.max(0, MAXIMO_CORREOS_LOTE - fichas.length);
  const sobran = sinRepetidos.length > disponibles;
  const aceptados = sinRepetidos.slice(0, disponibles).map((correo) => ({
    correo,
    error: validarCorreo(correo),
  }));

  return {
    fichas: aceptados.length > 0 ? [...fichas, ...aceptados] : fichas,
    aviso: sobran ? AVISO_LIMITE_CORREOS : null,
  };
}

/** Quita la ficha correspondiente al correo (sin distinguir mayúsculas). */
export function quitar(fichas: Ficha[], correo: string): Ficha[] {
  const clave = correo.toLowerCase();
  return fichas.filter((f) => f.correo.toLowerCase() !== clave);
}

/** Minúsculas y sin tildes, para buscar «ines» en «Inés». */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
}

/**
 * Hasta MAXIMO_SUGERENCIAS sugerencias que aún no están en las fichas (sin distinguir mayúsculas) y cuyo nombre o
 * correo contiene el texto escrito (sin distinguir mayúsculas ni tildes). Texto vacío: las primeras.
 */
export function filtrarSugerencias(sugerencias: Sugerencia[], fichas: Ficha[], texto: string): Sugerencia[] {
  const enFichas = new Set(fichas.map((f) => f.correo.toLowerCase()));
  const buscado = normalizar(texto.trim());
  return sugerencias
    .filter((s) => !enFichas.has(s.email.toLowerCase()))
    .filter((s) => buscado === '' || normalizar(`${s.nombre ?? ''} ${s.email}`).includes(buscado))
    .slice(0, MAXIMO_SUGERENCIAS);
}

/** Devuelve las fichas sin error de formato. */
export function validas(fichas: Ficha[]): Ficha[] {
  return fichas.filter((f) => f.error === null);
}

interface TextosResultado {
  yaHabilitado: string;
  otroRol: string;
  habilitadoSingular: string;
  habilitadoPlural: string;
}

function aplicarResultado(
  fichas: Ficha[],
  exitos: string[],
  habilitados: string[],
  yaHabilitados: string[],
  conOtroRol: string[],
  errores: ErrorAlAgregar[],
  textos: TextosResultado,
): { fichas: Ficha[]; resumen: string } {
  const resueltos = new Set([...exitos, ...habilitados].map((c) => c.toLowerCase()));
  const motivos = new Map<string, string>();
  for (const correo of yaHabilitados) {
    motivos.set(correo.toLowerCase(), textos.yaHabilitado);
  }
  for (const correo of conOtroRol) {
    motivos.set(correo.toLowerCase(), textos.otroRol);
  }
  for (const err of errores) {
    motivos.set(err.email.toLowerCase(), err.motivo);
  }

  const restantes: Ficha[] = [];
  for (const f of fichas) {
    const clave = f.correo.toLowerCase();
    if (resueltos.has(clave)) {
      continue;
    }
    const motivo = motivos.get(clave);
    restantes.push(motivo !== undefined ? { correo: f.correo, error: f.error, resultado: motivo } : { ...f });
  }

  const partes: string[] = [];
  if (exitos.length > 0) {
    const n = exitos.length;
    partes.push(n === 1 ? '1 agregado' : `${n} agregados`);
  }
  if (habilitados.length > 0) {
    const n = habilitados.length;
    partes.push(n === 1 ? `1 ${textos.habilitadoSingular}` : `${n} ${textos.habilitadoPlural}`);
  }
  const conProblema = restantes.filter((f) => f.resultado !== undefined).length;
  if (conProblema > 0) {
    partes.push(conProblema === 1 ? '1 con problema' : `${conProblema} con problemas`);
  }

  return {
    fichas: restantes,
    resumen: partes.join(' · '),
  };
}

/** Quita fichas agregadas o invitadas y deja las demás con su motivo en resultado. */
export function aplicarResultadoEstudiantes(
  fichas: Ficha[],
  resumen: ResumenEstudiantes,
): { fichas: Ficha[]; resumen: string } {
  return aplicarResultado(
    fichas,
    resumen.agregados,
    [...(resumen.invitados ?? []), ...resumen.habilitados],
    resumen.yaHabilitados,
    resumen.conOtroRol,
    resumen.errores,
    {
      yaHabilitado: MOTIVO_YA_HABILITADO_ESTUDIANTE,
      otroRol: MOTIVO_OTRO_ROL_ESTUDIANTE,
      habilitadoSingular: TEXTO_HABILITADO_SIN_CUENTA.estudianteSingular,
      habilitadoPlural: TEXTO_HABILITADO_SIN_CUENTA.estudiantePlural,
    },
  );
}

/** Igual que aplicarResultadoEstudiantes, para monitores del proyecto. */
export function aplicarResultadoMonitores(
  fichas: Ficha[],
  resumen: ResumenMonitores,
): { fichas: Ficha[]; resumen: string } {
  return aplicarResultado(
    fichas,
    resumen.asignados,
    resumen.habilitados,
    resumen.yaHabilitados,
    resumen.conOtroRol,
    [],
    {
      yaHabilitado: MOTIVO_YA_HABILITADO_MONITOR,
      otroRol: MOTIVO_OTRO_ROL_MONITOR,
      habilitadoSingular: TEXTO_HABILITADO_SIN_CUENTA.monitorSingular,
      habilitadoPlural: TEXTO_HABILITADO_SIN_CUENTA.monitorPlural,
    },
  );
}
