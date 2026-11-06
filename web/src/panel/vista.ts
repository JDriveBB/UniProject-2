import type {
  EstadoHitoEquipo,
  IntegranteInactivo,
  PanelEquipo,
  PanelProyecto,
  ProyectoDetalle,
  ProyectoVista,
  TareaBloqueada,
} from '../api/tipos';
import { fechaCorta, fechaHora } from '../formato';
import { estadoHito, type Marca } from '../tablero/marcas';
import { etiquetaHito, hitosDelEquipo } from '../tablero/ubicacion';

export const SIN_ACCESO_PANEL = 'No tienes acceso al panel de este proyecto o no existe.';
export const SIN_POR_EVALUAR = 'No hay hitos en revisión.';
export const SIN_ALERTAS = 'Sin alertas: nadie está inactivo y no hay tareas bloqueadas por más de 48 horas.';
export const EXPLICACION_ALERTAS =
  'Integrantes sin actividad según la alerta de inactividad de cada hito y tareas bloqueadas por dependencia hace más de 48 horas. Los hitos sin alerta de inactividad no se revisan.';
export const EXPLICACION_ESTADO =
  '«Requiere atención»: hay integrantes inactivos o tareas bloqueadas por más de 48 horas. «Tareas en riesgo»: tareas sin completar con riesgo alto de retraso.';
export const EXPLICACION_VENCIDO =
  '«Vencido sin enviar»: la fecha límite pasó y el equipo no ha enviado el hito.';

export const REQUIERE_ATENCION: Marca = { texto: 'Requiere atención', tono: 'peligro' };
export const TAREAS_EN_RIESGO: Marca = { texto: 'Tareas en riesgo', tono: 'aviso' };
export const AL_DIA: Marca = { texto: 'Al día', tono: 'exito' };
export const VENCIDO_SIN_ENVIAR: Marca = { texto: 'Vencido sin enviar', tono: 'peligro' };
export const INACTIVO: Marca = { texto: 'Inactivo', tono: 'aviso' };
export const BLOQUEADA: Marca = { texto: 'Bloqueada', tono: 'peligro' };

export function proyectosPropios(lista: ProyectoVista[], idUsuario: number): ProyectoVista[] {
  return lista.filter((p) => p.idDocente === idUsuario);
}

export interface PorEvaluar {
  idHitoEquipo: number;
  idEquipo: number;
  nombreEquipo: string;
  orden: number;
  nombreHito: string;
  fechaLimite: string;
}

export function porEvaluar(detalle: ProyectoDetalle): PorEvaluar[] {
  const lista: PorEvaluar[] = [];
  for (const equipo of detalle.equipos) {
    const hitos = hitosDelEquipo({
      proyecto: detalle.proyecto,
      hitos: detalle.hitos,
      equipo,
    });
    for (const h of hitos) {
      if (h.estado === 'ENVIADO') {
        lista.push({
          idHitoEquipo: h.idHitoEquipo,
          idEquipo: equipo.idEquipo,
          nombreEquipo: equipo.nombreEquipo,
          orden: h.orden,
          nombreHito: h.nombreHito,
          fechaLimite: h.fechaLimite,
        });
      }
    }
  }
  return lista.sort(
    (a, b) =>
      a.orden - b.orden ||
      a.nombreEquipo.localeCompare(b.nombreEquipo, 'es') ||
      a.idHitoEquipo - b.idHitoEquipo
  );
}

export function estadoEquipo(e: Pick<PanelEquipo, 'enRojo' | 'tareasRiesgoAlto'>): Marca {
  if (e.enRojo) {
    return REQUIERE_ATENCION;
  }
  if (e.tareasRiesgoAlto > 0) {
    return TAREAS_EN_RIESGO;
  }
  return AL_DIA;
}

export interface ResumenDelPanel {
  enRojo: number;
  equipos: number;
  porEvaluar: number;
  avance: number | null;
}

export function resumenPanel(panel: PanelProyecto, porEvaluar: number): ResumenDelPanel {
  const enRojo = panel.equipos.filter((e) => e.enRojo).length;
  const equipos = panel.equipos.length;
  const conTareas = panel.equipos.filter((e) => e.tareasTotales > 0);
  const avance =
    conTareas.length === 0
      ? null
      : Math.round(conTareas.reduce((suma, e) => suma + e.avancePromedio, 0) / conTareas.length);
  return { enRojo, equipos, porEvaluar, avance };
}

export function marcaCelda(estado: EstadoHitoEquipo, fechaLimite: string, hoy: string): Marca {
  if (estado === 'EN_CURSO' && hoy > fechaLimite) {
    return VENCIDO_SIN_ENVIAR;
  }
  return estadoHito(estado);
}

export interface ColumnaHito {
  idHito: number;
  etiqueta: string;
}

export interface FilaHitos {
  idEquipo: number;
  nombreEquipo: string;
  celdas: (Marca | null)[];
}

export interface MatrizHitos {
  columnas: ColumnaHito[];
  filas: FilaHitos[];
}

export function matrizHitos(detalle: ProyectoDetalle, hoy: string): MatrizHitos {
  const hitosOrdenados = [...detalle.hitos].sort((a, b) => a.orden - b.orden || a.idHito - b.idHito);
  const columnas: ColumnaHito[] = hitosOrdenados.map((h) => ({
    idHito: h.idHito,
    etiqueta: etiquetaHito(h.orden, h.nombreHito),
  }));
  const equiposOrdenados = [...detalle.equipos].sort(
    (a, b) => a.nombreEquipo.localeCompare(b.nombreEquipo, 'es') || a.idEquipo - b.idEquipo
  );
  const filas: FilaHitos[] = equiposOrdenados.map((equipo) => {
    const celdas = hitosOrdenados.map((h) => {
      const he = equipo.hitos.find((item) => item.idHito === h.idHito);
      return he !== undefined ? marcaCelda(he.estado, h.fechaLimite, hoy) : null;
    });
    return {
      idEquipo: equipo.idEquipo,
      nombreEquipo: equipo.nombreEquipo,
      celdas,
    };
  });
  return { columnas, filas };
}

export function textoDias(n: number): string {
  return n === 1 ? '1 día' : `${n} días`;
}

export function textoInactivo(i: IntegranteInactivo): string {
  return `${i.nombre} · sin actividad hace ${textoDias(i.diasSinActividad)} (última: ${fechaCorta(i.ultimaActividad)})`;
}

export function textoBloqueo(horas: number): string {
  return horas >= 48
    ? `bloqueada hace ${textoDias(Math.floor(horas / 24))}`
    : `bloqueada hace ${horas} h`;
}

export interface AlertasEquipo {
  idEquipo: number;
  nombreEquipo: string;
  inactivos: IntegranteInactivo[];
  bloqueadas: TareaBloqueada[];
}

export function alertasPorEquipo(panel: PanelProyecto): AlertasEquipo[] {
  const lista: AlertasEquipo[] = [];
  for (const e of panel.equipos) {
    if (e.integrantesInactivos.length > 0 || e.tareasBloqueadas.length > 0) {
      lista.push({
        idEquipo: e.idEquipo,
        nombreEquipo: e.nombreEquipo,
        inactivos: e.integrantesInactivos,
        bloqueadas: e.tareasBloqueadas,
      });
    }
  }
  return lista;
}

export function textoActualizado(generadoEn: string): string {
  return `Actualizado el ${fechaHora(generadoEn)}`;
}
