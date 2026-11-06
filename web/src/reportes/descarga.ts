import { ErrorApi, mensajeDeError } from '../api/cliente';

export const SIN_PERMISO_REPORTE =
  'No puedes descargar este reporte: solo el docente del proyecto puede hacerlo, o el equipo ya no existe.';
export const EQUIPO_NO_EXISTE = 'Este equipo ya no existe. Recarga la página.';
export const NO_ES_PDF =
  'El servidor no devolvió un PDF. Inténtalo de nuevo más tarde.';

export type GuardarArchivo = (archivo: Blob, nombre: string) => void;

export interface ApiDescarga {
  reporteEquipo(idEquipo: number): Promise<Blob>;
}

export function nombreArchivoReporte(
  nombreEquipo: string,
  idEquipo: number,
  hoy: string
): string {
  let parte = nombreEquipo
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 40)
    .replace(/-+$/g, '');

  if (parte === '') {
    parte = `equipo-${idEquipo}`;
  }

  return `reporte-trazabilidad-${parte}-${hoy}.pdf`;
}

export function esPdf(archivo: Blob): boolean {
  return archivo.type.startsWith('application/pdf');
}

export async function descargarReporte(
  api: ApiDescarga,
  idEquipo: number,
  nombre: string,
  guardar: GuardarArchivo
): Promise<void> {
  const archivo = await api.reporteEquipo(idEquipo);
  if (!esPdf(archivo)) {
    throw new Error(NO_ES_PDF);
  }
  guardar(archivo, nombre);
}

export function textoDescargado(nombre: string): string {
  return `Reporte listo: ${nombre}. Búscalo en las descargas de tu navegador.`;
}

export function mensajeErrorDescarga(error: unknown): string {
  if (error instanceof ErrorApi) {
    if (error.estado === 403) {
      return SIN_PERMISO_REPORTE;
    }
    if (error.estado === 404) {
      return EQUIPO_NO_EXISTE;
    }
    return mensajeDeError(error);
  }
  if (error instanceof Error && error.message === NO_ES_PDF) {
    return NO_ES_PDF;
  }
  return mensajeDeError(error);
}

export function guardarEnDescargas(archivo: Blob, nombre: string): void {
  // El PDF solo vive en memoria: la URL se revoca y nada queda guardado en el navegador (Ley 1581).
  const url = URL.createObjectURL(archivo);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  document.body.appendChild(enlace);
  enlace.click();
  document.body.removeChild(enlace);
  // Se revoca un segundo después porque algunos navegadores cancelan la descarga si se revoca en el mismo turno.
  setTimeout(() => {
    URL.revokeObjectURL(url);
  }, 1000);
}
