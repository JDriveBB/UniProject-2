import type {
  EntradaHistorial,
  EquipoVista,
  EvaluacionVista,
  TableroHito,
  TareaVista,
} from '../api/tipos';
import { textoNota } from '../evaluacion/calculo';
import { CARACTER } from '../evaluacion/vista';
import { fechaCorta, fechaDia, fechaHora } from '../formato';
import { avanceDelHito, estadoHito, type Marca } from '../tablero/marcas';
import type { HitoDelEquipo } from '../tablero/ubicacion';
import { LIMITE_HISTORIAL } from './carga';

export const DESCRIPCION_REPORTES =
  'Trazabilidad de cada equipo: resumen, historial de cambios y reporte en PDF.';
export const AYUDA_PDF =
  'El PDF reúne todo el semestre del equipo: integrantes, historial de cambios, entregas y calificaciones. El hito solo filtra lo que ves en esta página.';
export const SIN_PROYECTOS =
  'Aún no tienes proyectos. Cuando crees uno con equipos, aquí verás su trazabilidad.';
export const SIN_EQUIPOS = 'Este proyecto aún no tiene equipos.';
export const EXPLICACION_HISTORIAL =
  'Registro de auditoría del equipo, de solo lectura: nadie puede editarlo ni borrarlo. Del más reciente al más antiguo.';
export const HISTORIAL_DEL_HITO =
  'Solo los cambios de este hito y de sus tareas actuales.';
export const HISTORIAL_RECORTADO =
  'Se muestran los 1000 cambios más recientes; descarga el PDF para ver los anteriores.';
export const SIN_CAMBIOS_EQUIPO =
  'Aún no hay cambios registrados para este equipo.';
export const SIN_CAMBIOS_HITO =
  'Aún no hay cambios registrados en este hito.';
export const TODOS_LOS_HITOS = 'Todos los hitos';

/** Valores de AccionAuditoria.java del núcleo; uno nuevo sin traducir se muestra tal cual */
export const ACCIONES: Readonly<Record<string, string>> = {
  LOGIN_EXITOSO: 'Ingresó al sistema',
  LOGIN_FALLIDO: 'Intento de ingreso fallido',
  LOGOUT: 'Salió del sistema',
  ACCESO_DENEGADO: 'Acceso denegado',
  REGISTRO_CUENTA: 'Creó su cuenta',
  HABILITAR_CORREO: 'Habilitó un correo',
  CREAR: 'Creó',
  ACTUALIZAR: 'Modificó',
  ELIMINAR: 'Eliminó',
  CAMBIAR_ESTADO: 'Cambió el estado',
  ACTUALIZAR_AVANCE: 'Actualizó el avance',
  ADJUNTAR_EVIDENCIA: 'Adjuntó una evidencia',
  ASIGNAR_TAREA: 'Asignó la tarea',
  REASIGNAR_TAREA: 'Reasignó la tarea',
  ESTABLECER_DEPENDENCIA: 'Definió dependencias',
  ENVIAR_HITO: 'Envió el hito a revisión',
  EVALUAR_HITO: 'Evaluó el hito',
  CONFIRMAR_NOTA: 'Confirmó una nota',
  VINCULAR_TELEGRAM: 'Vinculó Telegram',
  DESVINCULAR_TELEGRAM: 'Desvinculó Telegram',
  EXPORTAR_REPORTE: 'Exportó el reporte',
  SUPRIMIR_CUENTA: 'Suprimió su cuenta',
  SOLICITAR_RECUPERACION: 'Pidió recuperar la contraseña',
  RESTABLECER_CONTRASENA: 'Restableció la contraseña',
  CAMBIAR_CONTRASENA: 'Cambió la contraseña',
  SOLICITAR_APOYO: 'Pidió apoyo',
  SOLICITAR_CORRECCION: 'Pidió una corrección',
};

const ENTIDADES: Readonly<Record<string, string>> = {
  equipo: 'Equipo',
  miembro_equipo: 'Integrantes',
  hito_equipo: 'Hito',
  tarea: 'Tarea',
};

export interface DatoResumen {
  etiqueta: string;
  valor: string | null;
  primario: boolean;
  marca: Marca | null;
  detalle: string | null;
}

export function textoAccion(accion: string): string {
  return Object.hasOwn(ACCIONES, accion) ? ACCIONES[accion] : accion;
}

export function textoEntidad(entidad: string): string {
  return Object.hasOwn(ENTIDADES, entidad) ? ENTIDADES[entidad] : entidad;
}

export function accionConEntidad(e: Pick<EntradaHistorial, 'accion' | 'entidad'>): string {
  return `${textoAccion(e.accion)} · ${textoEntidad(e.entidad)}`;
}

export function filtrarPorHito<T extends Pick<EntradaHistorial, 'entidad' | 'idRegistro'>>(
  entradas: T[],
  idHitoEquipo: number,
  tareas: Pick<TareaVista, 'idTarea'>[]
): T[] {
  // shortcut: las tareas ya borradas no están en el tablero y sus cambios no salen en este filtro; si se necesita, filtrar en el núcleo (§15).
  const idsTareas = new Set(tareas.map((t) => t.idTarea));
  return entradas.filter((e) => {
    if (e.entidad === 'hito_equipo' && e.idRegistro === idHitoEquipo) {
      return true;
    }
    if (e.entidad === 'tarea' && e.idRegistro !== null && idsTareas.has(e.idRegistro)) {
      return true;
    }
    return false;
  });
}

export function resumenEquipo(
  equipo: Pick<EquipoVista, 'miembros'>,
  hitos: Pick<HitoDelEquipo, 'estado'>[],
  historial: Pick<EntradaHistorial, 'fechaHora'>[]
): DatoResumen[] {
  const lider = equipo.miembros.find((m) => m.rolEquipo === 'LIDER');
  const detalleLider = lider !== undefined ? `Líder: ${lider.nombre}` : 'Sin líder';

  const evaluados = hitos.filter((h) => h.estado === 'EVALUADO').length;
  const enRevision = hitos.filter((h) => h.estado === 'ENVIADO').length;
  const detalleRevision = enRevision > 0 ? `${enRevision} en revisión` : 'Ninguno en revisión';

  const total = historial.length;
  const valorHistorial = total === LIMITE_HISTORIAL ? `${LIMITE_HISTORIAL} o más` : String(total);
  const detalleHistorial =
    total > 0 ? `Último: ${fechaHora(historial[0].fechaHora)}` : 'Sin cambios';

  return [
    {
      etiqueta: 'Integrantes',
      valor: String(equipo.miembros.length),
      primario: false,
      marca: null,
      detalle: detalleLider,
    },
    {
      etiqueta: 'Hitos evaluados',
      valor: `${evaluados} de ${hitos.length}`,
      primario: false,
      marca: null,
      detalle: detalleRevision,
    },
    {
      etiqueta: 'Cambios registrados',
      valor: valorHistorial,
      primario: true,
      marca: null,
      detalle: detalleHistorial,
    },
  ];
}

export function resumenHito(
  tablero: Pick<TableroHito, 'estado' | 'fechaLimite'>,
  tareas: Pick<TareaVista, 'estado' | 'porcentajeAvance' | 'bloqueada'>[],
  evaluacion: Pick<EvaluacionVista, 'notaGrupal' | 'caracter' | 'fechaEvaluacion'> | null
): DatoResumen[] {
  let detalleAvance: string;
  if (tareas.length === 0) {
    detalleAvance = 'Sin tareas';
  } else {
    const completadas = tareas.filter((t) => t.estado === 'COMPLETADA').length;
    const bloqueadas = tareas.filter((t) => t.bloqueada).length;
    const parteBloqueadas =
      bloqueadas === 0
        ? ''
        : bloqueadas === 1
          ? ' · 1 bloqueada'
          : ` · ${bloqueadas} bloqueadas`;
    detalleAvance = `${completadas} de ${tareas.length} tareas completadas${parteBloqueadas}`;
  }

  let datoEvaluacion: DatoResumen;
  if (evaluacion === null) {
    datoEvaluacion = {
      etiqueta: 'Evaluación',
      valor: 'Sin evaluar',
      primario: false,
      marca: null,
      detalle: null,
    };
  } else {
    datoEvaluacion = {
      etiqueta: 'Evaluación',
      valor: textoNota(evaluacion.notaGrupal),
      primario: false,
      marca: CARACTER[evaluacion.caracter],
      detalle: `Evaluada el ${fechaCorta(evaluacion.fechaEvaluacion)}`,
    };
  }

  return [
    {
      etiqueta: 'Estado del hito',
      valor: null,
      primario: false,
      marca: estadoHito(tablero.estado),
      detalle: `Fecha límite: ${fechaDia(tablero.fechaLimite)}`,
    },
    {
      etiqueta: 'Avance',
      valor: `${avanceDelHito(tareas)} %`,
      primario: true,
      marca: null,
      detalle: detalleAvance,
    },
    datoEvaluacion,
  ];
}
