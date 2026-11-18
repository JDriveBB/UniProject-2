import { describe, expect, it, vi } from 'vitest';
import { ErrorApi } from '../api/cliente';
import { descargarEvidencia, EVIDENCIA_NO_DESCARGABLE, mensajeErrorEvidencia } from './descargaEvidencia';

describe('descarga de evidencias', () => {
  it('descarga por la API y guarda; rechaza rutas que no son de evidencias', async () => {
    const blob = new Blob(['x']);
    const descargar = vi.fn(async () => blob);
    const guardar = vi.fn();
    await descargarEvidencia({ descargar }, '/api/evidencias/7/archivo', 'informe.pdf', guardar);
    expect(descargar).toHaveBeenCalledWith('/api/evidencias/7/archivo');
    expect(guardar).toHaveBeenCalledWith(blob, 'informe.pdf');

    const otra = vi.fn(async () => blob);
    await expect(descargarEvidencia({ descargar: otra }, 'https://x/y', 'a.pdf', guardar)).rejects.toThrow(
      EVIDENCIA_NO_DESCARGABLE,
    );
    expect(otra).not.toHaveBeenCalled();
  });

  it('traduce los errores a textos para el usuario', () => {
    expect(mensajeErrorEvidencia(new ErrorApi(401, 'NO_AUTENTICADO', 'x'))).toBe(
      'Tu sesión terminó. Vuelve a ingresar para descargar la evidencia.',
    );
    expect(mensajeErrorEvidencia(new ErrorApi(403, 'ACCESO_DENEGADO', 'x'))).toBe(
      'No tienes permiso para descargar esta evidencia.',
    );
    expect(mensajeErrorEvidencia(new ErrorApi(404, 'NO_ENCONTRADO', 'x'))).toBe(
      'El archivo de esta evidencia ya no está disponible.',
    );
    expect(mensajeErrorEvidencia(new Error(EVIDENCIA_NO_DESCARGABLE))).toBe(EVIDENCIA_NO_DESCARGABLE);
    expect(mensajeErrorEvidencia(new ErrorApi(0, 'SIN_CONEXION', 'No hay conexión…'))).toBe('No hay conexión…');
  });
});
