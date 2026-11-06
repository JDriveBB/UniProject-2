import { describe, expect, it } from 'vitest';
import type { TareaVista } from '../api/tipos';
import {
  MAXIMO_BYTES,
  REASIGNACION_VACIA,
  primerErrorReasignacion,
  solicitudAsignar,
  solicitudAvance,
  solicitudReasignacion,
  validarArchivo,
  validarAvance,
  validarCorreccion,
  validarEnlace,
  validarReasignacion,
} from './formularios';

function tarea(parcial: Partial<TareaVista> = {}): TareaVista {
  return {
    idTarea: 1,
    idHitoEquipo: 10,
    titulo: 'Tarea de prueba',
    descripcion: null,
    idEncargado: null,
    encargado: null,
    idTareaOrigen: null,
    horasEstimadas: 1,
    porcentajeAvance: 0,
    estado: 'PENDIENTE',
    fechaLimite: null,
    fechaLimiteEfectiva: '2026-09-30',
    fechaCompletada: null,
    esPrerrequisitoCritico: false,
    bloqueada: false,
    fechaBloqueo: null,
    enRutaCritica: false,
    holguraHoras: null,
    nivelRiesgo: null,
    prerrequisitos: [],
    ...parcial,
  };
}

describe('formularios de tarea', () => {
  it('avance: validación y solicitud', () => {
    expect(validarAvance({ porcentaje: '75', observacion: '' }).porcentaje).toBeUndefined();
    expect(validarAvance({ porcentaje: '0', observacion: '' }).porcentaje).toBeUndefined();
    expect(validarAvance({ porcentaje: '100', observacion: '' }).porcentaje).toBeUndefined();

    const err = 'Escribe un número entero de 0 a 100.';
    expect(validarAvance({ porcentaje: '101', observacion: '' }).porcentaje).toBe(err);
    expect(validarAvance({ porcentaje: '-1', observacion: '' }).porcentaje).toBe(err);
    expect(validarAvance({ porcentaje: '7.5', observacion: '' }).porcentaje).toBe(err);
    expect(validarAvance({ porcentaje: '', observacion: '' }).porcentaje).toBe(err);
    expect(validarAvance({ porcentaje: 'abc', observacion: '' }).porcentaje).toBe(err);

    const obs501 = 'a'.repeat(501);
    expect(validarAvance({ porcentaje: '50', observacion: obs501 }).observacion).toBe('Máximo 500 caracteres.');

    expect(solicitudAvance({ porcentaje: ' 75 ', observacion: '  ' })).toEqual({
      porcentajeAvance: 75,
      observacion: null,
    });
  });

  it('evidencia: validación de archivos y enlaces', () => {
    expect(validarArchivo(null)).toBe('Elige un archivo.');
    expect(validarArchivo({ size: 10, type: 'text/plain' })).toBe('Solo se aceptan archivos PDF, PNG o JPEG.');
    expect(validarArchivo({ size: 0, type: 'application/pdf' })).toBe('El archivo está vacío.');
    expect(validarArchivo({ size: MAXIMO_BYTES + 1, type: 'image/png' })).toBe(
      'El archivo pesa más de 10 MB. Súbelo a un repositorio y adjunta el enlace.'
    );
    expect(validarArchivo({ size: MAXIMO_BYTES, type: 'image/jpeg' })).toBeUndefined();

    expect(validarEnlace('  ')).toBe('Pega el enlace.');
    const largo2049 = 'https:' + '//' + 'a'.repeat(2041);
    expect(validarEnlace(largo2049)).toBe('Máximo 2048 caracteres.');
    expect(validarEnlace('github.com/x')).toBe('Usa un enlace completo que empiece por http o https.');
    expect(validarEnlace('https:' + '//github.com/x')).toBeUndefined();
  });

  it('corrección: validación de borrador', () => {
    expect(validarCorreccion({ idEvidencia: '', observaciones: ' ' })).toEqual({
      idEvidencia: 'Elige la evidencia que debe rehacer.',
      observaciones: 'Escribe los puntos que debe corregir.',
    });
    expect(validarCorreccion({ idEvidencia: '4', observaciones: 'Faltan llaves.' })).toEqual({});
    expect(validarCorreccion({ idEvidencia: '4', observaciones: 'a'.repeat(1001) })).toEqual({
      observaciones: 'Máximo 1000 caracteres.',
    });
  });

  it('reasignación: validación, orden de error y solicitudes', () => {
    expect(validarReasignacion(REASIGNACION_VACIA, 12)).toEqual({
      idNuevo: 'Elige el nuevo encargado.',
    });

    const error12 = validarReasignacion(
      { idNuevo: '2', tipo: 'PARCIAL', titulo: '', horas: '12', motivo: '' },
      12
    );
    expect(error12.titulo).toBe('Escribe el título de la parte.');
    expect(error12.horas).toBe('Usa un número mayor que 0 y menor que 12 (las horas de la tarea).');

    const error75 = validarReasignacion(
      { idNuevo: '2', tipo: 'PARCIAL', titulo: 'Parte', horas: '8', motivo: '' },
      7.5
    );
    expect(error75.horas).toBe('Usa un número mayor que 0 y menor que 7,5 (las horas de la tarea).');

    expect(
      validarReasignacion({ idNuevo: '2', tipo: 'PARCIAL', titulo: 'Parte', horas: '2,5', motivo: '' }, 12)
    ).toEqual({});

    expect(primerErrorReasignacion({ horas: 'x', titulo: 'y' })).toBe('titulo');

    const solParcial = solicitudReasignacion({
      idNuevo: '5',
      tipo: 'PARCIAL',
      titulo: ' Parte ',
      horas: '2,5',
      motivo: '',
    });
    expect(solParcial).toEqual({
      idNuevoEncargado: 5,
      tipo: 'PARCIAL',
      tituloParcial: 'Parte',
      horasParcial: 2.5,
      motivo: null,
    });

    const solTotal = solicitudReasignacion({
      idNuevo: '5',
      tipo: 'TOTAL',
      titulo: 'Parte',
      horas: '2,5',
      motivo: 'un motivo',
    });
    expect(solTotal).toEqual({
      idNuevoEncargado: 5,
      tipo: 'TOTAL',
      tituloParcial: null,
      horasParcial: null,
      motivo: 'un motivo',
    });

    const t = tarea({
      titulo: 'Diseño DB',
      descripcion: 'Hacer el modelo',
      horasEstimadas: 10,
      fechaLimite: null,
      fechaLimiteEfectiva: '2026-09-23',
      esPrerrequisitoCritico: true,
      idEncargado: 1,
    });
    expect(solicitudAsignar(t, 2)).toEqual({
      titulo: 'Diseño DB',
      descripcion: 'Hacer el modelo',
      horasEstimadas: 10,
      fechaLimite: null,
      esPrerrequisitoCritico: true,
      idEncargado: 2,
    });
  });
});
