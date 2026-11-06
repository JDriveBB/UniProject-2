import { ErrorApi } from '../api/cliente';
import type {
  CaracterNota,
  EvaluacionVista,
  NotaIndividualVista,
} from '../api/tipos';
import { fechaCorta, fechaDia } from '../formato';
import { mensajeErrorApi, type Campos } from '../proyectos/errores';
import type { Marca } from '../tablero/marcas';
import { textoNota } from './calculo';
import type { DatosEvaluacion } from './carga';

export const SIN_ACCESO_EVALUACION = 'No tienes acceso a esta evaluación o no existe.';
export const SIN_PERMISO_EVALUAR = 'Solo el docente del proyecto puede evaluar este hito.';
export const SIN_EVALUACION = 'El docente aún no evalúa este hito.';
export const AUN_NO_ENVIADO = 'El equipo aún no envía este hito a revisión.';
export const RUBRICA_NO_LISTA =
  'La rúbrica de este hito debe tener criterios y sus pesos deben sumar 100 % antes de evaluar. Corrígela en la configuración del proyecto.';
export const NOTAS_YA_CONFIRMADAS =
  'Ya confirmaste notas individuales: la nota grupal no se puede cambiar.';
export const REENVIADO_ESTUDIANTE =
  'El equipo volvió a enviar el hito: esta evaluación puede cambiar.';
export const EXPLICACION_ICE =
  'Nota sugerida = nota grupal × (0,5 + 0,5 × ICE), con máximo 5. El ICE compara las horas de las tareas que cada estudiante completó (las entregadas tarde cuentan la mitad) con el promedio del equipo: 1 es un aporte igual al promedio. Tú decides la nota final.';

export const TITULO_CONFIRMAR_TODAS = '¿Confirmar todas las notas?';
export const BOTON_CONFIRMAR_TODAS = 'Confirmar todas';
export const BOTON_CANCELAR = 'Cancelar';

export const NOTA_AJUSTADA: Marca = { texto: 'Ajustada por el docente', tono: 'info' };

export const CARACTER: Record<CaracterNota, Marca> = {
  PROVISIONAL: { texto: 'Provisional', tono: 'aviso' },
  DEFINITIVA: { texto: 'Definitiva', tono: 'exito' },
};

export const CAMPOS_EVALUACION: Campos = [
  ['criterios', 'Notas de la rúbrica'],
  ['notaGrupal', 'Nota grupal'],
  ['retroalimentacion', 'Retroalimentación general'],
];

export const CAMPOS_NOTA_GRUPAL: Campos = [['notaGrupal', 'Nota grupal']];
export const CAMPOS_NOTA_FINAL: Campos = [['notaFinal', 'Nota final']];

export function textoCaracter(caracter: CaracterNota, fechaLimite: string): string {
  if (caracter === 'PROVISIONAL') {
    return `Nota provisional: el equipo puede seguir mejorando el hito hasta el ${fechaDia(fechaLimite)}. Al vencer esa fecha, la última nota queda como definitiva.`;
  }
  return 'Nota definitiva: la fecha límite del hito ya pasó.';
}

export function vencido(fechaLimite: string, hoy: string): boolean {
  return hoy > fechaLimite;
}

export function textoConfirmarRegistro(fechaLimite: string, hoy: string): string {
  if (vencido(fechaLimite, hoy)) {
    return 'La fecha límite del hito ya pasó: la nota quedará como definitiva y el equipo ya no podrá editar sus tareas. Los estudiantes recibirán el aviso.';
  }
  return `La nota quedará como provisional: el equipo puede seguir editando hasta el ${fechaDia(fechaLimite)}. Los estudiantes recibirán el aviso.`;
}

export function textoConfirmarTodas(n: number): string {
  if (n === 1) {
    return 'Se confirmará 1 nota con los valores que ves. Cada confirmación queda en el registro de auditoría. Después de confirmar, la nota grupal ya no se podrá cambiar.';
  }
  return `Se confirmarán ${n} notas con los valores que ves. Cada confirmación queda en el registro de auditoría. Después de confirmar, la nota grupal ya no se podrá cambiar.`;
}

export function textoRegistrada(ev: EvaluacionVista): string {
  const palabra = ev.caracter === 'PROVISIONAL' ? 'provisional' : 'definitiva';
  return `Evaluación registrada como ${palabra}: nota grupal ${textoNota(ev.notaGrupal)}. Los estudiantes recibirán el aviso.`;
}

export function textoReevaluacion(anterior: EvaluacionVista): string {
  return `El equipo volvió a enviar el hito después de tu evaluación del ${fechaCorta(anterior.fechaEvaluacion)}. Registra una nueva evaluación; la anterior queda reemplazada.`;
}

export function textoDiferencia(calculada: number): string {
  return `La nota grupal difiere de la calculada (${textoNota(calculada)}). El cambio queda en el registro de auditoría.`;
}

export function estadoNota(n: NotaIndividualVista): Marca {
  if (n.fechaConfirmacion !== null) {
    return { texto: 'Confirmada', tono: 'exito' };
  }
  return { texto: 'Sin confirmar', tono: 'neutro' };
}

export function hayConfirmadas(notas: NotaIndividualVista[]): boolean {
  return notas.some((n) => n.notaFinal !== null);
}

export function notaAjustada(ev: EvaluacionVista): boolean {
  return (
    ev.notaCalculada !== null &&
    Math.round(ev.notaGrupal * 100) !== Math.round(ev.notaCalculada * 100)
  );
}

export type ModoDocente = 'sinAcceso' | 'noEnviado' | 'evaluar' | 'evaluado';

export function modoDocente(
  d: Pick<DatosEvaluacion, 'tablero' | 'proyecto' | 'evaluacion'>,
  idUsuario: number
): ModoDocente {
  if (d.proyecto.idDocente !== idUsuario) {
    return 'sinAcceso';
  }
  if (d.tablero.estado === 'ENVIADO') {
    return 'evaluar';
  }
  if (d.tablero.estado === 'EVALUADO' && d.evaluacion !== null) {
    return 'evaluado';
  }
  return 'noEnviado';
}

export function mensajeErrorEvaluacion(error: unknown, campos: Campos): string {
  if (error instanceof ErrorApi && error.estado === 403 && error.codigo === 'ACCESO_DENEGADO') {
    return SIN_PERMISO_EVALUAR;
  }
  return mensajeErrorApi(error, campos);
}

export function esConflicto(error: unknown): boolean {
  return error instanceof ErrorApi && error.estado === 409;
}
