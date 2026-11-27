import type {
  CursoResumen,
  EquipoVista,
  EstudianteDisponibleVista,
  InscripcionVista,
  ResultadoReparto,
} from '../api/tipos';
import { listaConY } from '../cursos/vista';
import { fechaHora, instanteDeValorLocal } from '../formato';
import type { Mensaje } from './errores';

/** Cupo en pantalla: "2/3", o "Lo arma el docente" si el equipo no tiene cupo. */
export function textoCupo(e: Pick<EquipoVista, 'cupo' | 'miembros'>): string {
  return e.cupo === null ? 'Lo arma el docente' : `${e.miembros.length}/${e.cupo}`;
}

/** El equipo tiene cupo y ya está completo. */
export function lleno(e: Pick<EquipoVista, 'cupo' | 'miembros'>): boolean {
  return e.cupo !== null && e.miembros.length >= e.cupo;
}

/** El equipo tiene más integrantes que su cupo. */
export function pasadoDeCupo(e: Pick<EquipoVista, 'cupo' | 'miembros'>): boolean {
  return e.cupo !== null && e.miembros.length > e.cupo;
}

export function tieneLider(e: Pick<EquipoVista, 'miembros'>): boolean {
  return e.miembros.some((m) => m.rolEquipo === 'LIDER');
}

const ENTERO = /^\d+$/;

/** null si el texto sirve como cupo (entero de 1 a 100; vacío solo si no es obligatorio); si no, el motivo. */
export function validarCupo(texto: string, obligatorio: boolean): string | null {
  const t = texto.trim();
  if (t === '') {
    return obligatorio ? 'Escribe el cupo' : null;
  }
  const n = Number(t);
  if (!ENTERO.test(t) || n < 1 || n > 100) {
    return 'El cupo debe ser un número entero entre 1 y 100';
  }
  return null;
}

/** Cupo escrito → número; vacío → null. Solo se llama tras validar. */
export function cupoDeTexto(texto: string): number | null {
  const t = texto.trim();
  return t === '' ? null : Number(t);
}

export type CursoParaEquipo =
  | { tipo: 'mixto' }
  | { tipo: 'sinCursos' }
  | { tipo: 'unico'; curso: CursoResumen }
  | { tipo: 'elegir'; cursos: CursoResumen[] };

/** Qué pide el formulario de equipo sobre el curso, según los cursos del proyecto. */
export function cursoParaEquipo(cursos: CursoResumen[], equiposMixtos: boolean): CursoParaEquipo {
  if (equiposMixtos) {
    return { tipo: 'mixto' };
  }
  if (cursos.length === 0) {
    return { tipo: 'sinCursos' };
  }
  if (cursos.length === 1) {
    return { tipo: 'unico', curso: cursos[0] };
  }
  return { tipo: 'elegir', cursos };
}

/** Cuántos estudiantes sin equipo hay en ese curso; con null, todos. */
export function sinEquipoDelCurso(disponibles: EstudianteDisponibleVista[], idCurso: number | null): number {
  return idCurso === null ? disponibles.length : disponibles.filter((d) => d.idCurso === idCurso).length;
}

export type VistaPreviaLote = { error: string } | { cantidad: number; nombres: string[] };

/** Valida el lote y calcula cuántos equipos se crearán y con qué nombres. */
export function vistaPreviaLote(
  prefijo: string,
  cantidadTexto: string,
  cupoTexto: string,
  sinEquipo: number,
): VistaPreviaLote {
  const p = prefijo.trim();
  if (p === '') {
    return { error: 'Escribe el prefijo de los nombres' };
  }
  const errorCupo = validarCupo(cupoTexto, true);
  if (errorCupo !== null) {
    return { error: errorCupo };
  }
  const textoCantidad = cantidadTexto.trim();
  let cantidad: number;
  if (textoCantidad !== '') {
    cantidad = Number(textoCantidad);
    if (!ENTERO.test(textoCantidad) || cantidad < 1 || cantidad > 50) {
      return { error: 'La cantidad debe ser un número entero entre 1 y 50' };
    }
  } else {
    if (sinEquipo === 0) {
      return { error: 'No hay estudiantes sin equipo para calcular cuántos equipos crear. Escribe la cantidad.' };
    }
    cantidad = Math.ceil(sinEquipo / Number(cupoTexto.trim()));
    if (cantidad > 50) {
      return { error: 'Serían más de 50 equipos. Sube el cupo o escribe la cantidad.' };
    }
  }
  return { cantidad, nombres: Array.from({ length: cantidad }, (_, i) => `${p} ${i + 1}`) };
}

/** "Se crearán 3 equipos: A, B y C."; con más de 4, solo los dos primeros y el último. */
export function textoVistaPrevia(v: { cantidad: number; nombres: string[] }): string {
  const encabezado = v.cantidad === 1 ? 'Se creará 1 equipo' : `Se crearán ${v.cantidad} equipos`;
  if (v.nombres.length <= 4) {
    return `${encabezado}: ${listaConY(v.nombres)}.`;
  }
  return `${encabezado}: ${v.nombres[0]}, ${v.nombres[1]} … ${v.nombres[v.nombres.length - 1]}.`;
}

/** Equipos a los que se puede asignar a un estudiante de ese curso (sin curso o del mismo), por nombre. */
export function equiposParaAsignar(equipos: EquipoVista[], idCursoEstudiante: number): EquipoVista[] {
  return equipos
    .filter((e) => e.idCurso === null || e.idCurso === idCursoEstudiante)
    .sort((a, b) => a.nombreEquipo.localeCompare(b.nombreEquipo, 'es'));
}

export function etiquetaEquipoAsignar(e: EquipoVista): string {
  return `${e.nombreEquipo} (${textoCupo(e)})`;
}

/** Resultado del reparto automático para una Alerta. */
export function textoReparto(r: ResultadoReparto): Mensaje {
  if (r.asignados.length === 0 && r.sinCupo.length === 0) {
    return { tipo: 'aviso', texto: 'No había estudiantes sin equipo.' };
  }
  const frases: string[] = [];
  if (r.asignados.length > 0) {
    frases.push(
      r.asignados.length === 1 ? 'Se asignó 1 estudiante' : `Se asignaron ${r.asignados.length} estudiantes`,
    );
  }
  if (r.sinCupo.length > 0) {
    const nombres = r.sinCupo.map((s) => s.nombre);
    frases.push(
      nombres.length === 1
        ? `1 quedó sin cupo: ${nombres[0]}`
        : `${nombres.length} quedaron sin cupo: ${listaConY(nombres)}`,
    );
  }
  return { tipo: r.sinCupo.length > 0 ? 'aviso' : 'exito', texto: frases.join('. ') + '.' };
}

/** Frase de estado de la inscripción a equipos. */
export function estadoInscripcion(v: Pick<InscripcionVista, 'cierreEquipos' | 'abierta'>): string {
  if (v.cierreEquipos === null) {
    return 'Sin fecha de cierre: los estudiantes aún no pueden inscribirse solos en los equipos con cupo.';
  }
  return v.abierta
    ? `Inscripción abierta hasta el ${fechaHora(v.cierreEquipos)}. Al cerrar, cada equipo abre su votación de líder.`
    : `La inscripción cerró el ${fechaHora(v.cierreEquipos)}.`;
}

/** Duración de la votación: entero de 1 a 336 horas. */
export function validarHorasRonda(texto: string): string | null {
  const t = texto.trim();
  const n = Number(t);
  return ENTERO.test(t) && n >= 1 && n <= 336 ? null : 'Escribe un número entero de horas entre 1 y 336';
}

/** Fecha de cierre de la inscripción: vacía es válida (sin fecha); si no, válida y futura. */
export function validarCierre(valor: string, ahora: Date): string | null {
  if (valor.trim() === '') {
    return null;
  }
  const instante = instanteDeValorLocal(valor);
  if (instante === null) {
    return 'Escribe una fecha y hora válidas';
  }
  return new Date(instante).getTime() > ahora.getTime() ? null : 'La fecha de cierre debe ser futura';
}

export function preguntaPasarCupo(nombreEquipo: string, personas: string[]): string {
  return `${nombreEquipo} ya tiene su cupo completo. ¿Agregar igual a ${listaConY(personas)}? El equipo quedará pasado de cupo.`;
}

export function preguntaRepartir(n: number): string {
  const quien = n === 1 ? 'el estudiante sin equipo' : `los ${n} estudiantes sin equipo`;
  return `Se repartirán ${quien} entre los equipos con cupo libre, respetando el curso y sin pasar ningún cupo. ¿Continuar?`;
}
