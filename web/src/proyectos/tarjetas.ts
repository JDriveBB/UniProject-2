import type { EquipoVista, ProyectoDetalle, ProyectoVista, TableroHito, TareaVista, UsuarioSesion } from '../api/tipos';
import { cargarPanel, type ApiCargaPanel, type DatosPanel } from '../panel/carga';
import {
  diasHasta,
  entregasDelProyecto,
  marcaCelda,
  porEvaluar,
  resumenPanel,
  textoPlazo,
  type UrgenciaEntrega,
} from '../panel/vista';
import type { Marca } from '../tablero/marcas';
import { etiquetaHito, hitoActual, hitosDelEquipo, rolEnEquipo, type HitoDelEquipo } from '../tablero/ubicacion';
import { modoDeVista } from './vista';

export interface ResumenDocente {
  equipos: number;
  hitos: number;
  estudiantes: number;
  proxima: { etiqueta: string; plazo: string; urgencia: UrgenciaEntrega } | null;
  porCalificar: number;
  enAtencion: number;
}

export interface HitoActualEstudiante {
  idHitoEquipo: number;
  etiqueta: string;
  marca: Marca;
  plazo: string | null;
}

export interface ResumenEstudiante {
  equipo: { idEquipo: number; nombreEquipo: string };
  esLider: boolean;
  hito: HitoActualEstudiante | null;
  avance: number | null;
}

export type ResumenProyecto =
  | { tipo: 'docente'; resumen: ResumenDocente }
  | { tipo: 'estudiante'; resumen: ResumenEstudiante }
  | { tipo: 'sin-equipo' }
  | { tipo: 'error' };

export interface ApiResumenes extends ApiCargaPanel {
  tableroHito(idHitoEquipo: number): Promise<TableroHito>;
}

/** Avance ponderado por horas estimadas, redondeado a entero; null sin tareas o si las horas suman 0. */
export function avancePonderado(tareas: Pick<TareaVista, 'porcentajeAvance' | 'horasEstimadas'>[]): number | null {
  let totalHoras = 0;
  let sumaPonderada = 0;
  for (const tarea of tareas) {
    totalHoras += tarea.horasEstimadas;
    sumaPonderada += tarea.porcentajeAvance * tarea.horasEstimadas;
  }
  if (totalHoras <= 0) {
    return null;
  }
  return Math.round(sumaPonderada / totalHoras);
}

/** Resumen del proyecto para la tarjeta del docente en /proyectos. */
export function resumenDocente(datos: DatosPanel, hoy: string): ResumenDocente {
  const { panel, detalle } = datos;
  const entregas = entregasDelProyecto(detalle, hoy);
  const pendientes = porEvaluar(detalle);
  const base = resumenPanel(panel, pendientes.length, entregas);
  const entregaProxima = entregas.find((e) => e.proxima) ?? null;
  const estudiantes = detalle.equipos.reduce((total, eq) => total + eq.miembros.length, 0);

  return {
    equipos: base.equipos,
    hitos: detalle.hitos.length,
    estudiantes,
    proxima:
      entregaProxima === null
        ? null
        : {
            etiqueta: entregaProxima.etiqueta,
            plazo: entregaProxima.plazo,
            urgencia: entregaProxima.urgencia,
          },
    porCalificar: base.porEvaluar,
    enAtencion: base.enRojo,
  };
}

/** Etapa del proyecto según sus fechas de calendario (aaaa-mm-dd). */
export function etapaProyecto(proyecto: Pick<ProyectoVista, 'fechaInicio' | 'fechaFin'>, hoy: string): Marca {
  if (hoy < proyecto.fechaInicio) {
    return { texto: 'Por empezar', tono: 'neutro' };
  }
  if (hoy > proyecto.fechaFin) {
    return { texto: 'Terminado', tono: 'neutro' };
  }
  return { texto: 'En curso', tono: 'info' };
}

/** Insignia principal de la tarjeta del docente según atención, entregas por calificar o al día. */
export function estadoProyecto(r: ResumenDocente): Marca {
  if (r.enAtencion > 0) {
    return {
      texto: r.enAtencion === 1 ? '1 equipo en atención' : `${r.enAtencion} equipos en atención`,
      tono: 'peligro',
    };
  }
  if (r.porCalificar > 0) {
    return {
      texto: `${r.porCalificar} por calificar`,
      tono: 'info',
    };
  }
  return { texto: 'Al día', tono: 'exito' };
}

function hitoActualDelUsuario(
  detalle: ProyectoDetalle,
  idUsuario: number,
): { equipo: EquipoVista; hito: HitoDelEquipo | null } | null {
  const equipo = detalle.equipos.find((e) => e.miembros.some((m) => m.idUsuario === idUsuario));
  if (equipo === undefined) {
    return null;
  }
  const hitos = hitosDelEquipo({ proyecto: detalle.proyecto, hitos: detalle.hitos, equipo });
  return { equipo, hito: hitoActual(hitos) };
}

/** Resumen del proyecto para la tarjeta del estudiante; null si no pertenece a ningún equipo. */
export function resumenEstudiante(
  detalle: ProyectoDetalle,
  idUsuario: number,
  tablero: TableroHito | null,
  hoy: string,
): ResumenEstudiante | null {
  const hallado = hitoActualDelUsuario(detalle, idUsuario);
  if (hallado === null) {
    return null;
  }
  const { equipo, hito } = hallado;
  return {
    equipo: { idEquipo: equipo.idEquipo, nombreEquipo: equipo.nombreEquipo },
    esLider: rolEnEquipo(equipo, idUsuario) === 'LIDER',
    hito:
      hito === null
        ? null
        : {
            idHitoEquipo: hito.idHitoEquipo,
            etiqueta: etiquetaHito(hito.orden, hito.nombreHito),
            marca: marcaCelda(hito.estado, hito.fechaLimite, hoy),
            plazo: hito.estado === 'EN_CURSO' ? textoPlazo(diasHasta(hito.fechaLimite, hoy)) : null,
          },
    avance: tablero === null ? null : avancePonderado(tablero.tareas),
  };
}

async function cargarResumenProyecto(
  api: ApiResumenes,
  proyecto: ProyectoVista,
  usuario: Pick<UsuarioSesion, 'idUsuario' | 'rol'>,
  hoy: string,
): Promise<ResumenProyecto> {
  if (modoDeVista(usuario, proyecto) === 'edicion') {
    const datos = await cargarPanel(api, proyecto.idProyecto);
    return { tipo: 'docente', resumen: resumenDocente(datos, hoy) };
  }

  const detalle = await api.detalleProyecto(proyecto.idProyecto);
  const hallado = hitoActualDelUsuario(detalle, usuario.idUsuario);
  if (hallado === null) {
    return { tipo: 'sin-equipo' };
  }

  const tablero = hallado.hito === null ? null : await api.tableroHito(hallado.hito.idHitoEquipo);
  const resumen = resumenEstudiante(detalle, usuario.idUsuario, tablero, hoy);
  return resumen === null ? { tipo: 'sin-equipo' } : { tipo: 'estudiante', resumen };
}

/** Carga en paralelo el resumen de cada proyecto sin que el fallo de uno detenga a los demás. */
export async function cargarResumenes(
  api: ApiResumenes,
  proyectos: readonly ProyectoVista[],
  usuario: Pick<UsuarioSesion, 'idUsuario' | 'rol'>,
  hoy: string,
): Promise<Map<number, ResumenProyecto>> {
  const resultados = await Promise.allSettled(
    proyectos.map((proyecto) => cargarResumenProyecto(api, proyecto, usuario, hoy)),
  );

  const mapa = new Map<number, ResumenProyecto>();
  proyectos.forEach((proyecto, i) => {
    const resultado = resultados[i];
    mapa.set(proyecto.idProyecto, resultado.status === 'fulfilled' ? resultado.value : { tipo: 'error' });
  });
  return mapa;
}
