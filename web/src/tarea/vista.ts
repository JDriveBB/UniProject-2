import { ErrorApi, mensajeDeError } from '../api/cliente';
import type { CorreccionVista, EstadoHitoEquipo, EvidenciaVista, TareaVista, TipoEvidencia } from '../api/tipos';
import type { TonoInsignia } from '../componentes/Insignia';
import { decimalEsCo, fechaCorta } from '../formato';
import { mensajeErrorApi, type Campos } from '../proyectos/errores';
import { estadoTarea, nombreConCodigo, RIESGO, type Marca } from '../tablero/marcas';

export const SIN_ACCESO_TAREA = 'No tienes acceso a esta tarea o no existe.';
export const SIN_PERMISO_AVANCE = 'Solo el encargado de la tarea o el líder del equipo pueden registrar avance.';
export const SIN_PERMISO_EVIDENCIA = 'Solo el encargado de la tarea o el líder del equipo pueden adjuntar evidencias.';
export const SIN_PERMISO_APOYO = 'Solo el encargado de la tarea puede solicitar apoyo.';
export const SIN_PERMISO_LIDER = 'Solo el líder del equipo puede hacerlo.';
export const SIN_PERMISO_COMENTAR = 'No puedes comentar en esta tarea.';
export const CORRECCION_SOLICITADA: Marca = { texto: 'Corrección solicitada', tono: 'aviso' };

export const TIPO_EVIDENCIA: Record<TipoEvidencia, Marca> = {
  PDF: { texto: 'PDF', tono: 'peligro' },
  IMAGEN: { texto: 'Imagen', tono: 'critico' },
  ENLACE: { texto: 'Enlace', tono: 'info' },
};

export interface Prerrequisito {
  idTarea: number;
  texto: string;
  tono: TonoInsignia;
}

export function insigniasDeTarea(t: TareaVista, conCorreccion: boolean): Marca[] {
  const lista: Marca[] = [estadoTarea(t.estado)];
  if (conCorreccion) {
    lista.push(CORRECCION_SOLICITADA);
  }
  return lista;
}

export function textoRiesgo(
  t: Pick<TareaVista, 'estado' | 'nivelRiesgo' | 'holguraHoras'>,
): { marca: Marca; detalle: string } | null {
  if (t.estado === 'COMPLETADA' || t.nivelRiesgo === null) {
    return null;
  }
  const marca = RIESGO[t.nivelRiesgo];
  if (t.holguraHoras === null) {
    return { marca, detalle: '' };
  }
  const horas = decimalEsCo(t.holguraHoras === 0 ? 0 : Math.abs(t.holguraHoras));
  const detalle =
    t.holguraHoras >= 0 ? `Margen de ${horas} h de trabajo` : `Faltan ${horas} h de trabajo para llegar a tiempo`;
  return { marca, detalle };
}

export function correccionPendiente(cs: CorreccionVista[]): CorreccionVista | null {
  const pendientes = cs.filter((c) => c.estado === 'PENDIENTE');
  if (pendientes.length === 0) {
    return null;
  }
  return pendientes.reduce((max, c) => (c.idSolicitud > max.idSolicitud ? c : max));
}

export function prerrequisitosDe(t: TareaVista, tareas: TareaVista[], codigos: Map<number, string>): Prerrequisito[] {
  const ids = [...t.prerrequisitos].sort((a, b) => a - b);
  const mapaTareas = new Map(tareas.map((tarea) => [tarea.idTarea, tarea] as const));

  return ids.map((id) => {
    const p = mapaTareas.get(id);
    if (p !== undefined) {
      const estado = estadoTarea(p.estado);
      return {
        idTarea: id,
        texto: `${nombreConCodigo(codigos, p)} · ${estado.texto}`,
        tono: estado.tono,
      };
    }
    return {
      idTarea: id,
      texto: codigos.get(id) ?? `Tarea ${id}`,
      tono: 'neutro',
    };
  });
}

export function esEnlaceWeb(url: string): boolean {
  try {
    const u = new URL(url);
    return (u.protocol === 'http:' || u.protocol === 'https:') && u.hostname !== '';
  } catch {
    return false;
  }
}

export function textoEnlace(url: string): string {
  if (!esEnlaceWeb(url)) {
    return url;
  }
  try {
    const u = new URL(url);
    const hostYCamino = `${u.host}${u.pathname}${u.search}${u.hash}`;
    return hostYCamino.endsWith('/') ? hostYCamino.slice(0, -1) : hostYCamino;
  } catch {
    return url;
  }
}

export function nombreEvidencia(e: EvidenciaVista): string {
  if (e.tipoEvidencia === 'ENLACE') {
    return textoEnlace(e.enlace ?? '');
  }
  return e.nombreArchivo ?? 'Archivo sin nombre';
}

export function nombreDeEvidencia(evidencias: EvidenciaVista[], idEvidencia: number): string {
  const encontrada = evidencias.find((e) => e.idEvidencia === idEvidencia);
  return encontrada !== undefined ? nombreEvidencia(encontrada) : 'una evidencia anterior';
}

export function primerNombre(nombre: string): string {
  const partes = nombre.trim().split(/\s+/);
  return partes[0] ?? '';
}

export function textoCorreccion(
  c: CorreccionVista,
  evidencia: string,
  esElEncargado: boolean,
): { titulo: string; texto: string } {
  const titulo = `Corrección solicitada por ${c.lider} (líder) · ${fechaCorta(c.fechaSolicitud)}`;
  const remate = esElEncargado
    ? 'La tarea volvió a En progreso hasta que adjuntes la nueva versión.'
    : `La tarea volvió a En progreso hasta que ${primerNombre(c.estudiante)} adjunte la nueva versión.`;
  const texto = `«${c.observaciones}» Evidencia: ${evidencia}. ${remate}`;
  return { titulo, texto };
}

export function bloqueoCorreccion(
  t: TareaVista,
  evidencias: EvidenciaVista[],
  pendiente: CorreccionVista | null,
): string | null {
  if (t.idEncargado === null) {
    return 'La tarea no tiene encargado.';
  }
  if (evidencias.length === 0) {
    return 'La tarea aún no tiene evidencias.';
  }
  if (pendiente !== null) {
    return 'Ya hay una corrección pendiente.';
  }
  return null;
}

export function textoSoloLectura(estado: EstadoHitoEquipo): string {
  if (estado === 'ENVIADO') {
    return 'El hito está en revisión: esta tarea no se puede cambiar.';
  }
  return 'El hito ya cerró: esta tarea no se puede cambiar.';
}

export function mensajeErrorTarea(error: unknown, campos: Campos, sinPermiso: string): string {
  if (error instanceof ErrorApi) {
    if (error.estado === 403 && error.codigo === 'ACCESO_DENEGADO') {
      return sinPermiso;
    }
    if (error.estado === 409 && error.detalles.length > 0) {
      return mensajeDeError(error);
    }
  }
  return mensajeErrorApi(error, campos);
}
