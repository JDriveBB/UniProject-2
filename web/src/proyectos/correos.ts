import { validarCorreo } from '../cuentas/validacion';
import type { Mensaje } from './errores';
import type { ResumenEstudiantes, ResumenMonitores } from './equipos';

export const MAXIMO_CORREOS_LOTE = 50;

/**
 * Función pura (bloque 31): separa por cualquier espacio en blanco (espacios, tabulaciones,
 * saltos de línea), y también por coma o punto y coma; recorta; ignora vacíos;
 * pasa a minúsculas solo para comparar duplicados pero conserva el texto original del primero;
 * elimina duplicados. Devuelve el orden de aparición.
 */
export function separarCorreos(texto: string): string[] {
  const tokens = texto.split(/[\s,;]+/);
  const resultado: string[] = [];
  const vistos = new Set<string>();

  for (const raw of tokens) {
    const limpio = raw.trim();
    if (limpio === '') {
      continue;
    }
    const clave = limpio.toLowerCase();
    if (!vistos.has(clave)) {
      vistos.add(clave);
      resultado.push(limpio);
    }
  }
  return resultado;
}

/**
 * Validación de lote (bloque 31):
 * - Texto vacío -> el error actual de correo obligatorio ('Escribe un correo electrónico').
 * - Más de 50 correos -> 'Máximo 50 correos por vez.'.
 * - Si hay inválidos, lista los correos inválidos (máx. 5 y «y N más»).
 */
export function validarLoteCorreos(texto: string): string | null {
  const correos = separarCorreos(texto);
  if (correos.length === 0) {
    return validarCorreo('');
  }
  if (correos.length > MAXIMO_CORREOS_LOTE) {
    return 'Máximo 50 correos por vez.';
  }
  const invalidos = correos.filter((c) => validarCorreo(c) !== null);
  if (invalidos.length > 0) {
    const lista =
      invalidos.length <= 5
        ? invalidos.join(', ')
        : `${invalidos.slice(0, 5).join(', ')} y ${invalidos.length - 5} más`;
    return `Correos inválidos: ${lista}`;
  }
  return null;
}

/**
 * Mensaje de resultado para estudiantes (bloque 31): aviso único con el resumen en líneas cortas.
 * Tipo de alerta: éxito si no hubo problemas, aviso si hubo "ya habilitado"/"otro rol",
 * error si algún agregado falló.
 */
export function mensajeResultadoEstudiantes(resumen: ResumenEstudiantes): Mensaje {
  const partes: string[] = [];

  if (resumen.agregados.length > 0) {
    const n = resumen.agregados.length;
    partes.push(n === 1 ? '1 agregado al equipo' : `${n} agregados al equipo`);
  }
  if (resumen.habilitados.length > 0) {
    const n = resumen.habilitados.length;
    partes.push(
      n === 1
        ? '1 habilitado (aparecerá cuando cree su cuenta)'
        : `${n} habilitados (aparecerán cuando creen su cuenta)`,
    );
  }
  if (resumen.yaHabilitados.length > 0) {
    const n = resumen.yaHabilitados.length;
    partes.push(n === 1 ? '1 ya estaba habilitado' : `${n} ya estaban habilitados`);
  }
  if (resumen.conOtroRol.length > 0) {
    const n = resumen.conOtroRol.length;
    partes.push(`${n} con otro rol: ${resumen.conOtroRol.join(', ')}`);
  }
  if (resumen.errores.length > 0) {
    const n = resumen.errores.length;
    const detalles = resumen.errores.map((e) => `${e.email} (${e.motivo})`).join(', ');
    partes.push(
      n === 1
        ? `1 no se pudo agregar al equipo: ${detalles}`
        : `${n} no se pudieron agregar al equipo: ${detalles}`,
    );
  }

  let tipo: Mensaje['tipo'] = 'exito';
  if (resumen.errores.length > 0) {
    tipo = 'error';
  } else if (resumen.yaHabilitados.length > 0 || resumen.conOtroRol.length > 0) {
    tipo = 'aviso';
  }

  return {
    tipo,
    texto: partes.join(' · '),
  };
}

/**
 * Mensaje de resultado para monitores (bloque 31): aviso único con el resumen en líneas cortas.
 * Tipo de alerta: éxito si no hubo problemas, aviso si hubo "ya habilitado"/"otro rol".
 */
export function mensajeResultadoMonitores(resumen: ResumenMonitores): Mensaje {
  const partes: string[] = [];

  if (resumen.asignados.length > 0) {
    const n = resumen.asignados.length;
    partes.push(n === 1 ? '1 agregado como monitor' : `${n} agregados como monitores`);
  }
  if (resumen.habilitados.length > 0) {
    const n = resumen.habilitados.length;
    partes.push(
      n === 1
        ? '1 habilitado (aparecerá cuando cree su cuenta)'
        : `${n} habilitados (aparecerán cuando creen su cuenta)`,
    );
  }
  if (resumen.yaHabilitados.length > 0) {
    const n = resumen.yaHabilitados.length;
    partes.push(n === 1 ? '1 ya estaba habilitado' : `${n} ya estaban habilitados`);
  }
  if (resumen.conOtroRol.length > 0) {
    const n = resumen.conOtroRol.length;
    partes.push(`${n} con otro rol: ${resumen.conOtroRol.join(', ')}`);
  }

  let tipo: Mensaje['tipo'] = 'exito';
  if (resumen.yaHabilitados.length > 0 || resumen.conOtroRol.length > 0) {
    tipo = 'aviso';
  }

  return {
    tipo,
    texto: partes.join(' · '),
  };
}
