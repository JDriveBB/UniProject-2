import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import { BORRADOR_VACIO, nuevoHito } from './borrador';
import {
  CAMPOS_CORREO,
  CAMPOS_CRITERIO,
  CAMPOS_PROYECTO,
  mensajeErrorApi,
  mensajeErrorCarga,
  textoErrorGuardado,
} from './errores';
import type { Operacion } from './plan';

describe('mensajes de error de proyectos', () => {
  it('VALIDACION nombra los campos del formulario, sin repetir', () => {
    const e = new ErrorApi(400, 'VALIDACION', 'Datos inválidos', [
      'fechaFin: must not be null',
      'nombre: must not be blank',
      'nombre: size must be between 0 and 150',
    ]);
    expect(mensajeErrorApi(e, CAMPOS_PROYECTO)).toBe('Revisa estos campos: Nombre del proyecto, Fecha de fin');
    expect(
      mensajeErrorApi(
        new ErrorApi(400, 'VALIDACION', 'Datos inválidos', ['emails[0]: must be a well-formed email address']),
        CAMPOS_CORREO,
      ),
    ).toBe('Revisa estos campos: Correo');
    expect(mensajeErrorApi(new ErrorApi(400, 'VALIDACION', 'Datos inválidos', ['otro: x']), CAMPOS_PROYECTO)).toBe(
      'Datos inválidos',
    );
  });

  it('403, 404 y 409 con mensajes claros; sin conexión, el texto general', () => {
    expect(mensajeErrorApi(new ErrorApi(403, 'ACCESO_DENEGADO', 'Acceso denegado'), CAMPOS_PROYECTO)).toBe(
      'Solo el docente del proyecto puede cambiarlo',
    );
    expect(
      mensajeErrorApi(
        new ErrorApi(409, 'HITOS_FUERA_DE_FECHAS', 'Hay hitos fuera de las nuevas fechas del proyecto. Ajusta primero sus fechas'),
        CAMPOS_PROYECTO,
      ),
    ).toBe('Hay hitos fuera de las nuevas fechas del proyecto. Ajusta primero sus fechas');
    expect(mensajeErrorApi(new ErrorApi(404, 'NO_ENCONTRADO', 'El criterio no existe en este hito'), CAMPOS_CRITERIO)).toBe(
      'El criterio no existe en este hito',
    );
    expect(mensajeErrorCarga(new ErrorApi(403, 'ACCESO_DENEGADO', 'Acceso denegado'))).toBe(
      'No tienes acceso a este proyecto o no existe.',
    );
    expect(mensajeErrorCarga(new ErrorApi(404, 'NO_ENCONTRADO', 'El proyecto no existe'))).toBe('Este proyecto no existe.');
    expect(
      mensajeErrorCarga(new ErrorApi(0, 'SIN_CONEXION', 'No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo')),
    ).toBe('No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo');
  });

  it('el error de guardado dice qué parte falló y si algo ya quedó guardado', () => {
    const b = { ...BORRADOR_VACIO, hitos: [{ ...nuevoHito(BORRADOR_VACIO, 'n1'), nombre: 'Diseño' }] };
    const op: Operacion = {
      tipo: 'crearCriterio',
      claveHito: 'n1',
      idHito: 30,
      claveCriterio: 'n2',
      solicitud: { descripcion: 'Claridad', pesoPorcentaje: 60, puntajeMaximo: 5 },
    };
    const causa = new ErrorApi(409, 'PESOS_EXCEDEN_100', 'Los pesos de la rúbrica sumarían 110 %; el máximo es 100 %');
    expect(textoErrorGuardado(op, causa, b, 2)).toBe(
      'Error al guardar la rúbrica del hito «Diseño»: Los pesos de la rúbrica sumarían 110 %; el máximo es 100 %. '
        + 'Los cambios anteriores a ese paso sí quedaron guardados.',
    );
    const proyecto: Operacion = {
      tipo: 'actualizarProyecto',
      solicitud: { nombre: 'P', descripcion: null, fechaInicio: '2026-08-01', fechaFin: '2026-11-30', usaMonitores: false },
    };
    expect(textoErrorGuardado(proyecto, new ErrorApi(403, 'ACCESO_DENEGADO', 'Acceso denegado'), b, 0)).toBe(
      'Error al guardar los datos del proyecto: Solo el docente del proyecto puede cambiarlo',
    );
  });
});
