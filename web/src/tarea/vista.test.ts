import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { CorreccionVista, EvidenciaVista, TareaVista } from '../api/tipos';
import type { Campos } from '../proyectos/errores';
import { codigosDeTareas } from '../tablero/marcas';
import {
  bloqueoCorreccion,
  correccionPendiente,
  esEnlaceWeb,
  insigniasDeTarea,
  mensajeErrorTarea,
  nombreDeEvidencia,
  prerrequisitosDe,
  textoCorreccion,
  textoEnlace,
  textoRiesgo,
  textoSoloLectura,
} from './vista';

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

const CAMPOS_AVANCE: Campos = [
  ['porcentajeAvance', 'Porcentaje de avance'],
  ['observacion', 'Observación'],
];

describe('vista de tarea', () => {
  it('insignias de tarea y prerrequisitos con códigos de hito', () => {
    const t = tarea({
      estado: 'EN_PROGRESO',
      enRutaCritica: true,
      esPrerrequisitoCritico: true,
      nivelRiesgo: 'ALTO',
      prerrequisitos: [99, 1],
    });

    const insignias = insigniasDeTarea(t, true);
    expect(insignias.map((m) => m.texto)).toEqual(['En progreso', 'Corrección solicitada']);

    const tarea1 = tarea({
      idTarea: 1,
      titulo: 'Diagrama de casos de uso',
      estado: 'COMPLETADA',
    });
    const tarea2 = tarea({
      idTarea: 2,
      titulo: 'Segunda tarea',
    });
    const codigos = codigosDeTareas([tarea1, tarea2]);

    const prereqs = prerrequisitosDe(t, [tarea1, tarea2], codigos);
    expect(prereqs).toEqual([
      { idTarea: 1, texto: 'T1 · Diagrama de casos de uso · Completada', tono: 'exito' },
      { idTarea: 99, texto: 'Tarea 99', tono: 'neutro' },
    ]);
  });

  it('evidencias, enlaces, correcciones y bloqueo de corrección', () => {
    expect(esEnlaceWeb('https:' + '//github.com/grupo-alfa/uniproject')).toBe(true);
    expect(esEnlaceWeb('http:' + '//localhost:8080/a')).toBe(true);
    expect(esEnlaceWeb('javascript:alert(1)')).toBe(false);
    expect(esEnlaceWeb('ftp:' + '//x.org')).toBe(false);
    expect(esEnlaceWeb('github.com')).toBe(false);
    expect(esEnlaceWeb('')).toBe(false);

    expect(textoEnlace('https:' + '//github.com/grupo-alfa/uniproject/')).toBe('github.com/grupo-alfa/uniproject');

    expect(nombreDeEvidencia([], 7)).toBe('una evidencia anterior');

    const c1: CorreccionVista = {
      idSolicitud: 1,
      idTarea: 5,
      idEvidencia: 2,
      idLider: 1,
      lider: 'Juan',
      idEstudiante: 2,
      estudiante: 'Santy',
      observaciones: 'Obs 1',
      estado: 'PENDIENTE',
      fechaSolicitud: '2026-09-20T10:00:00Z',
      fechaAtencion: null,
    };
    const c2: CorreccionVista = {
      ...c1,
      idSolicitud: 2,
      observaciones: 'Obs 2',
    };
    const c3: CorreccionVista = {
      ...c1,
      idSolicitud: 3,
      estado: 'ATENDIDA',
    };

    expect(correccionPendiente([c3, c1, c2])).toEqual(c2);
    expect(correccionPendiente([])).toBeNull();

    const corr: CorreccionVista = {
      idSolicitud: 4,
      idTarea: 5,
      idEvidencia: 10,
      idLider: 1,
      lider: 'Juan Benavides',
      idEstudiante: 2,
      estudiante: 'Santy Baza',
      observaciones: 'Faltan las llaves foráneas.',
      estado: 'PENDIENTE',
      fechaSolicitud: '2026-09-23T15:00:00Z',
      fechaAtencion: null,
    };

    const textoEncargado = textoCorreccion(corr, 'modelo_relacional.png', true);
    expect(textoEncargado.titulo).toBe('Corrección solicitada por Juan Benavides (líder) · 23/09/2026');
    expect(textoEncargado.texto).toBe(
      '«Faltan las llaves foráneas.» Evidencia: modelo_relacional.png. La tarea volvió a En progreso hasta que adjuntes la nueva versión.',
    );

    const textoOtro = textoCorreccion(corr, 'modelo_relacional.png', false);
    expect(textoOtro.texto).toBe(
      '«Faltan las llaves foráneas.» Evidencia: modelo_relacional.png. La tarea volvió a En progreso hasta que Santy adjunte la nueva versión.',
    );

    const ev: EvidenciaVista = {
      idEvidencia: 10,
      idTarea: 5,
      idUsuario: 2,
      autor: 'Santy',
      tipoEvidencia: 'IMAGEN',
      enlace: null,
      nombreArchivo: 'modelo_relacional.png',
      descarga: '/descarga/10',
      fechaSubida: '2026-09-22T10:00:00Z',
    };

    expect(bloqueoCorreccion(tarea({ idEncargado: null }), [], null)).toBe('La tarea no tiene encargado.');
    expect(bloqueoCorreccion(tarea({ idEncargado: 2 }), [], null)).toBe('La tarea aún no tiene evidencias.');
    expect(bloqueoCorreccion(tarea({ idEncargado: 2 }), [ev], corr)).toBe('Ya hay una corrección pendiente.');
    expect(bloqueoCorreccion(tarea({ idEncargado: 2 }), [ev], null)).toBeNull();
  });

  it('mensajes de error de tarea y textos de solo lectura', () => {
    const err403 = new ErrorApi(403, 'ACCESO_DENEGADO', 'Prohibido', []);
    expect(mensajeErrorTarea(err403, CAMPOS_AVANCE, 'Sin permiso avance')).toBe('Sin permiso avance');

    const err409 = new ErrorApi(409, 'TAREA_BLOQUEADA', 'La tarea está bloqueada', ['Diagrama de casos de uso']);
    expect(mensajeErrorTarea(err409, CAMPOS_AVANCE, 'Sin permiso avance')).toBe(
      'La tarea está bloqueada: Diagrama de casos de uso',
    );

    const err400 = new ErrorApi(400, 'VALIDACION', 'Error de validación', [
      'porcentajeAvance: debe ser menor o igual a 100',
    ]);
    expect(mensajeErrorTarea(err400, CAMPOS_AVANCE, 'Sin permiso avance')).toBe(
      'Revisa estos campos: Porcentaje de avance',
    );

    expect(textoSoloLectura('ENVIADO')).toBe('El hito está en revisión: esta tarea no se puede cambiar.');
    expect(textoSoloLectura('EVALUADO')).toBe('El hito ya cerró: esta tarea no se puede cambiar.');
  });

  it('textoRiesgo muestra nivel (incluido bajo) y margen o faltante en horas de trabajo', () => {
    expect(textoRiesgo({ estado: 'COMPLETADA', nivelRiesgo: 'ALTO', holguraHoras: -4 })).toBeNull();
    expect(textoRiesgo({ estado: 'PENDIENTE', nivelRiesgo: null, holguraHoras: 8 })).toBeNull();

    expect(textoRiesgo({ estado: 'EN_PROGRESO', nivelRiesgo: 'BAJO', holguraHoras: 12 })).toEqual({
      marca: { texto: 'Riesgo bajo', tono: 'exito' },
      detalle: 'Margen de 12 h de trabajo',
    });
    expect(textoRiesgo({ estado: 'PENDIENTE', nivelRiesgo: 'MEDIO', holguraHoras: 0 })).toEqual({
      marca: { texto: 'Riesgo medio', tono: 'aviso' },
      detalle: 'Margen de 0 h de trabajo',
    });
    expect(textoRiesgo({ estado: 'EN_PROGRESO', nivelRiesgo: 'MEDIO', holguraHoras: 2.5 })).toEqual({
      marca: { texto: 'Riesgo medio', tono: 'aviso' },
      detalle: 'Margen de 2,5 h de trabajo',
    });
    expect(textoRiesgo({ estado: 'EN_PROGRESO', nivelRiesgo: 'ALTO', holguraHoras: -3.5 })).toEqual({
      marca: { texto: 'Riesgo alto', tono: 'peligro' },
      detalle: 'Faltan 3,5 h de trabajo para llegar a tiempo',
    });
    expect(textoRiesgo({ estado: 'PENDIENTE', nivelRiesgo: 'ALTO', holguraHoras: -4 })).toEqual({
      marca: { texto: 'Riesgo alto', tono: 'peligro' },
      detalle: 'Faltan 4 h de trabajo para llegar a tiempo',
    });
  });
});
