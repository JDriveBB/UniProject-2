import { ErrorApi } from '../api/cliente';
import type { EnlaceRegistro } from '../api/tipos';
import { fechaCorta } from '../formato';
import { rutas } from '../rutas';
import { PATRON_CODIGO } from './recuperacion';

export interface Portapapeles {
  writeText(texto: string): Promise<void>;
}

export interface EnlaceCopiado {
  email: string;
  url: string;
  texto: string;
}

export type ResultadoEnlace = { ok: true; enlace: EnlaceCopiado } | { ok: false; mensaje: string; recargar: boolean };

/** Construye la URL completa de registro codificando el código con URLSearchParams. */
export function enlaceDeRegistro(origen: string, codigo: string): string {
  const base = origen.replace(/\/+$/, '');
  const params = new URLSearchParams({ codigo });
  return `${base}${rutas.registro()}?${params.toString()}`;
}

/** Lee `codigo` de la query de `/registro`; devuelve null si falta o no tiene 43 caracteres Base64 URL. */
export function codigoDeLaUrl(search: string): string | null {
  const limpio = search.startsWith('?') ? search.slice(1) : search;
  try {
    const params = new URLSearchParams(limpio);
    const codigo = params.get('codigo')?.trim();
    if (codigo && PATRON_CODIGO.test(codigo)) {
      return codigo;
    }
  } catch {
    // No lanza
  }
  return null;
}

/** Mensaje de confirmación tras generar el enlace de invitación, indicando si se copió al portapapeles. */
export function textoConfirmacionEnlace(correo: string, venceEn: string, copiado: boolean): string {
  const inicio = copiado ? 'Enlace copiado.' : 'Copia el enlace de abajo.';
  return `${inicio} Vence el ${fechaCorta(venceEn)}. Envíalo solo a ${correo}: sirve una vez y el anterior deja de servir.`;
}

/** Copia texto al portapapeles del navegador; devuelve false si no está disponible o falla el permiso. */
export async function copiarAlPortapapeles(
  texto: string,
  portapapeles: Portapapeles | undefined = typeof navigator !== 'undefined' ? navigator.clipboard : undefined,
): Promise<boolean> {
  if (portapapeles === undefined || typeof portapapeles.writeText !== 'function') {
    return false;
  }
  try {
    await portapapeles.writeText(texto);
    return true;
  } catch {
    return false;
  }
}

/** Genera el enlace de invitación en el servidor, intenta copiarlo y prepara el resultado para la pantalla. */
export async function prepararEnlaceInvitacion(
  generar: () => Promise<EnlaceRegistro>,
  correo: string,
  origen: string,
  mensajeError: (error: unknown) => string,
  copiar: (texto: string) => Promise<boolean> = copiarAlPortapapeles,
): Promise<ResultadoEnlace> {
  try {
    const generado = await generar();
    const url = enlaceDeRegistro(origen, generado.codigo);
    const copiado = await copiar(url);
    return {
      ok: true,
      enlace: {
        email: correo,
        url,
        texto: textoConfirmacionEnlace(correo, generado.venceEn, copiado),
      },
    };
  } catch (e) {
    const recargar = e instanceof ErrorApi && (e.estado === 404 || e.estado === 409);
    return { ok: false, mensaje: mensajeError(e), recargar };
  }
}
