import { ErrorApi, mensajeDeError } from '../api/cliente';
import type { GuardarArchivo } from '../reportes/descarga';

export const EVIDENCIA_NO_DESCARGABLE = 'Esta evidencia no se puede descargar.';

export interface ApiDescargaEvidencia {
  descargar(ruta: string): Promise<Blob>;
}

/** Descarga con el cliente de la API (cookie de sesión y manejo del 401); solo rutas /api/evidencias/. */
export async function descargarEvidencia(
  api: ApiDescargaEvidencia,
  ruta: string,
  nombre: string,
  guardar: GuardarArchivo,
): Promise<void> {
  if (!ruta.startsWith('/api/evidencias/')) {
    throw new Error(EVIDENCIA_NO_DESCARGABLE);
  }
  const archivo = await api.descargar(ruta);
  guardar(archivo, nombre);
}

export function mensajeErrorEvidencia(error: unknown): string {
  if (error instanceof ErrorApi) {
    if (error.estado === 401) {
      return 'Tu sesión terminó. Vuelve a ingresar para descargar la evidencia.';
    }
    if (error.estado === 403) {
      return 'No tienes permiso para descargar esta evidencia.';
    }
    if (error.estado === 404) {
      return 'El archivo de esta evidencia ya no está disponible.';
    }
  } else if (error instanceof Error && error.message === EVIDENCIA_NO_DESCARGABLE) {
    return EVIDENCIA_NO_DESCARGABLE;
  }
  return mensajeDeError(error);
}
