import { api } from './cliente';
import type {
  CompaneroVista,
  CursoDetalle,
  CursoVista,
  EnlaceRegistro,
  ResultadoCorreosCurso,
  SolicitudArchivado,
  SolicitudCorreosCurso,
  SolicitudCurso,
} from './tipos';

/** listarCursos: cursos del docente o cursos en los que participa el estudiante. */
export function listarCursos(): Promise<CursoVista[]> {
  return api.get<CursoVista[]>('/api/cursos');
}

/** crearCurso: solo DOCENTE. 409 CURSO_REPETIDO si ya existe otro con igual nombre y periodo. */
export function crearCurso(solicitud: SolicitudCurso): Promise<CursoVista> {
  return api.post<CursoVista>('/api/cursos', solicitud);
}

/** detalleCurso: solo el docente dueño (403 si no es suyo o no existe). */
export function detalleCurso(idCurso: number): Promise<CursoDetalle> {
  return api.get<CursoDetalle>(`/api/cursos/${idCurso}`);
}

/** editarCurso: 409 CURSO_ARCHIVADO o CURSO_REPETIDO. */
export function editarCurso(idCurso: number, solicitud: SolicitudCurso): Promise<CursoVista> {
  return api.put<CursoVista>(`/api/cursos/${idCurso}`, solicitud);
}

/** borrarCurso: 204. 409 CURSO_EN_USO si está asignado a algún proyecto. */
export function borrarCurso(idCurso: number): Promise<void> {
  return api.borrar<void>(`/api/cursos/${idCurso}`);
}

/** cambiarArchivadoCurso: archiva o desarchiva el curso. */
export function cambiarArchivadoCurso(idCurso: number, archivado: boolean): Promise<CursoVista> {
  const solicitud: SolicitudArchivado = { archivado };
  return api.put<CursoVista>(`/api/cursos/${idCurso}/archivado`, solicitud);
}

/** companerosCurso: nombres de los compañeros del curso (Ley 1581). */
export function companerosCurso(idCurso: number): Promise<CompaneroVista[]> {
  return api.get<CompaneroVista[]>(`/api/cursos/${idCurso}/companeros`);
}

/** agregarCorreosCurso: 400 VALIDACION, CUERPO_INVALIDO o CORREO_RESERVADO; 409 CURSO_ARCHIVADO o CORREO_HABILITADO_EN_PARALELO; agrega estudiantes con cuenta y habilita los que no tienen cuenta. */
export function agregarCorreosCurso(idCurso: number, emails: string[]): Promise<ResultadoCorreosCurso> {
  const solicitud: SolicitudCorreosCurso = { emails };
  return api.post<ResultadoCorreosCurso>(`/api/cursos/${idCurso}/correos`, solicitud);
}

/** retirarCorreoCurso: 204. 409 HABILITACION_USADA si ya creó su cuenta; 404 si no existe en el curso. */
export function retirarCorreoCurso(idCurso: number, idCorreoHabilitado: number): Promise<void> {
  return api.borrar<void>(`/api/cursos/${idCurso}/correos/${idCorreoHabilitado}`);
}

/** generarEnlaceCurso: 200 (Cache-Control: no-store). 409 CURSO_ARCHIVADO o HABILITACION_USADA. */
export function generarEnlaceCurso(idCurso: number, idCorreoHabilitado: number): Promise<EnlaceRegistro> {
  return api.post<EnlaceRegistro>(`/api/cursos/${idCurso}/correos/${idCorreoHabilitado}/enlace`);
}

/** quitarEstudianteCurso: 204. 404 si ya no estaba en el curso. */
export function quitarEstudianteCurso(idCurso: number, idUsuario: number): Promise<void> {
  return api.borrar<void>(`/api/cursos/${idCurso}/estudiantes/${idUsuario}`);
}
