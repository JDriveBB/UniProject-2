import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import {
  descargarReporte,
  EQUIPO_NO_EXISTE,
  esPdf,
  mensajeErrorDescarga,
  NO_ES_PDF,
  nombreArchivoReporte,
  SIN_PERMISO_REPORTE,
  textoDescargado,
  type ApiDescarga,
} from './descarga';

describe('descarga de reporte', () => {
  it('genera el nombre del archivo, detecta pdf y formatea el mensaje de exito', () => {
    const hoy = '2026-11-06';
    expect(nombreArchivoReporte('Equipo Beta', 7, hoy)).toBe(
      'reporte-trazabilidad-equipo-beta-2026-11-06.pdf'
    );
    expect(nombreArchivoReporte('Ñandú & Café', 3, hoy)).toBe(
      'reporte-trazabilidad-nandu-cafe-2026-11-06.pdf'
    );
    expect(nombreArchivoReporte('***', 9, hoy)).toBe(
      'reporte-trazabilidad-equipo-9-2026-11-06.pdf'
    );
    expect(nombreArchivoReporte('../x', 4, hoy)).toBe(
      'reporte-trazabilidad-x-2026-11-06.pdf'
    );
    expect(nombreArchivoReporte('a'.repeat(60), 1, hoy)).toBe(
      `reporte-trazabilidad-${'a'.repeat(40)}-2026-11-06.pdf`
    );
    expect(nombreArchivoReporte('a'.repeat(39) + ' b', 1, hoy)).toBe(
      `reporte-trazabilidad-${'a'.repeat(39)}-2026-11-06.pdf`
    );

    expect(esPdf(new Blob(['x'], { type: 'application/pdf' }))).toBe(true);
    expect(esPdf(new Blob(['x']))).toBe(false);

    expect(textoDescargado('r.pdf')).toBe(
      'Reporte listo: r.pdf. Búscalo en las descargas de tu navegador.'
    );
  });

  it('descarga y valida el archivo con el api y mapea errores', async () => {
    const llamadasGuardar: { archivo: Blob; nombre: string }[] = [];
    const guardarFalso = (archivo: Blob, nombre: string) => {
      llamadasGuardar.push({ archivo, nombre });
    };

    const pdfBlob = new Blob(['pdf-data'], { type: 'application/pdf' });
    const apiPdf: ApiDescarga = {
      async reporteEquipo() {
        return pdfBlob;
      },
    };

    await descargarReporte(apiPdf, 7, 'r.pdf', guardarFalso);
    expect(llamadasGuardar).toHaveLength(1);
    expect(llamadasGuardar[0].archivo).toBe(pdfBlob);
    expect(llamadasGuardar[0].nombre).toBe('r.pdf');

    const htmlBlob = new Blob(['<html></html>'], { type: 'text/html' });
    const apiHtml: ApiDescarga = {
      async reporteEquipo() {
        return htmlBlob;
      },
    };

    llamadasGuardar.length = 0;
    await expect(
      descargarReporte(apiHtml, 7, 'r.pdf', guardarFalso)
    ).rejects.toThrow(NO_ES_PDF);
    expect(llamadasGuardar).toHaveLength(0);

    const error403 = new ErrorApi(403, 'ACCESO_DENEGADO', 'Acceso denegado');
    const apiError: ApiDescarga = {
      async reporteEquipo() {
        throw error403;
      },
    };

    llamadasGuardar.length = 0;
    await expect(
      descargarReporte(apiError, 7, 'r.pdf', guardarFalso)
    ).rejects.toBe(error403);
    expect(llamadasGuardar).toHaveLength(0);

    expect(mensajeErrorDescarga(error403)).toBe(SIN_PERMISO_REPORTE);
    expect(
      mensajeErrorDescarga(new ErrorApi(404, 'NO_ENCONTRADO', 'No encontrado'))
    ).toBe(EQUIPO_NO_EXISTE);
    expect(mensajeErrorDescarga(new Error(NO_ES_PDF))).toBe(NO_ES_PDF);
    expect(
      mensajeErrorDescarga(
        new ErrorApi(
          0,
          'SIN_CONEXION',
          'No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo'
        )
      )
    ).toBe('No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo');
    expect(mensajeErrorDescarga(new Error('x'))).toBe(
      'Ocurrió un error inesperado. Recarga la página'
    );
  });
});
