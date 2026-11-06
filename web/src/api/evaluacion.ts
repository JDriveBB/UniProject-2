import { api } from './cliente';
import type {
  CambioNotaGrupal,
  ConfirmacionNota,
  EvaluacionVista,
  SolicitudEvaluacion,
} from './tipos';

/** GET /api/hitos-equipo/{idHitoEquipo}/evaluacion. 404 si aún no hay; 403 sin acceso. */
export function consultarEvaluacion(idHitoEquipo: number): Promise<EvaluacionVista> {
  return api.get<EvaluacionVista>(`/api/hitos-equipo/${idHitoEquipo}/evaluacion`);
}

/** POST /api/hitos-equipo/{idHitoEquipo}/evaluacion. Solo el docente del proyecto; 400 VALIDACION/RUBRICA_INCOMPLETA/NOTA_FUERA_DE_RANGO; 409 HITO_NO_ENVIADO/PESOS_NO_SUMAN_100/EQUIPO_SIN_INTEGRANTES. */
export function evaluarHito(
  idHitoEquipo: number,
  solicitud: SolicitudEvaluacion
): Promise<EvaluacionVista> {
  return api.post<EvaluacionVista>(`/api/hitos-equipo/${idHitoEquipo}/evaluacion`, solicitud);
}

/** PUT /api/evaluaciones/{idEvaluacion}/nota-grupal. 409 NOTAS_CONFIRMADAS/EVALUACION_REEMPLAZADA. */
export function cambiarNotaGrupal(
  idEvaluacion: number,
  notaGrupal: number
): Promise<EvaluacionVista> {
  const cuerpo: CambioNotaGrupal = { notaGrupal };
  return api.put<EvaluacionVista>(`/api/evaluaciones/${idEvaluacion}/nota-grupal`, cuerpo);
}

/** PUT /api/evaluaciones/{idEvaluacion}/notas/{idEstudiante}. 404 si el estudiante no tiene nota; 409 EVALUACION_REEMPLAZADA. */
export function confirmarNota(
  idEvaluacion: number,
  idEstudiante: number,
  notaFinal: number
): Promise<EvaluacionVista> {
  const cuerpo: ConfirmacionNota = { notaFinal };
  return api.put<EvaluacionVista>(`/api/evaluaciones/${idEvaluacion}/notas/${idEstudiante}`, cuerpo);
}
