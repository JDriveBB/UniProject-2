import { ErrorApi, mensajeDeError } from '../api/cliente';
import type {
  EquipoVista,
  EstadoHitoEquipo,
  HitoVista,
  ProyectoDetalle,
  ProyectoVista,
  RolEquipo,
} from '../api/tipos';
import { idDeRuta } from '../proyectos/vista';

/** No hay GET de un equipo por id: se busca en los proyectos que el usuario ve (decisión 26c-1). */
export interface EquipoUbicado {
  proyecto: ProyectoVista;
  hitos: HitoVista[];
  equipo: EquipoVista;
}

/** Hito del proyecto visto desde un equipo: orden, nombre y fechas del hito + id y estado de su HitoEquipo. */
export interface HitoDelEquipo {
  idHitoEquipo: number;
  idHito: number;
  orden: number;
  nombreHito: string;
  fechaInicio: string;
  fechaLimite: string;
  estado: EstadoHitoEquipo;
}

/** Lo que se usa del API (en la pantalla, las funciones de api/proyectos; en las pruebas, una falsa). */
export interface ApiUbicacion {
  listarProyectos(): Promise<ProyectoVista[]>;
  detalleProyecto(idProyecto: number): Promise<ProyectoDetalle>;
}

export const SIN_ACCESO_EQUIPO = 'No tienes acceso a este equipo o no existe.';
export const SIN_ACCESO_HITO = 'No tienes acceso a este hito o no existe.';

function ubicado(detalle: ProyectoDetalle, equipo: EquipoVista): EquipoUbicado {
  return { proyecto: detalle.proyecto, hitos: detalle.hitos, equipo };
}

/** Recorre los proyectos uno por uno y para en el primero que tiene el equipo. null si ninguno (no existe o no lo ve). */
export async function ubicarEquipo(api: ApiUbicacion, idEquipo: number): Promise<EquipoUbicado | null> {
  for (const p of await api.listarProyectos()) {
    const detalle = await api.detalleProyecto(p.idProyecto);
    const equipo = detalle.equipos.find((e) => e.idEquipo === idEquipo);
    if (equipo !== undefined) {
      return ubicado(detalle, equipo);
    }
  }
  return null;
}

/** Equipos donde el usuario es integrante, en el orden de los proyectos (para /tablero y /ruta-critica). */
export async function equiposDelUsuario(api: ApiUbicacion, idUsuario: number): Promise<EquipoUbicado[]> {
  const encontrados: EquipoUbicado[] = [];
  for (const p of await api.listarProyectos()) {
    const detalle = await api.detalleProyecto(p.idProyecto);
    for (const equipo of detalle.equipos) {
      if (equipo.miembros.some((m) => m.idUsuario === idUsuario)) {
        encontrados.push(ubicado(detalle, equipo));
      }
    }
  }
  return encontrados;
}

/** Hitos del equipo ordenados por el "orden" del hito; un HitoEquipo cuyo hito no está en el proyecto se omite. */
export function hitosDelEquipo(u: EquipoUbicado): HitoDelEquipo[] {
  const porId = new Map(u.hitos.map((h) => [h.idHito, h] as const));
  const lista: HitoDelEquipo[] = [];
  for (const he of u.equipo.hitos) {
    const h = porId.get(he.idHito);
    if (h !== undefined) {
      lista.push({
        idHitoEquipo: he.idHitoEquipo,
        idHito: h.idHito,
        orden: h.orden,
        nombreHito: h.nombreHito,
        fechaInicio: h.fechaInicio,
        fechaLimite: h.fechaLimite,
        estado: he.estado,
      });
    }
  }
  return lista.sort((a, b) => a.orden - b.orden || a.idHitoEquipo - b.idHitoEquipo);
}

/** Decisión 26c-2: el primer hito EN_CURSO por orden; si no hay, el último. null si el equipo no tiene hitos. */
export function hitoActual(hitos: HitoDelEquipo[]): HitoDelEquipo | null {
  return hitos.find((h) => h.estado === 'EN_CURSO') ?? hitos.at(-1) ?? null;
}

/** ?hito=<idHitoEquipo> si es de este equipo; si falta, no es un id o no es suyo, el hito actual. */
export function hitoElegido(hitos: HitoDelEquipo[], texto: string | null): HitoDelEquipo | null {
  const id = idDeRuta(texto ?? undefined);
  const elegido = id === null ? undefined : hitos.find((h) => h.idHitoEquipo === id);
  return elegido ?? hitoActual(hitos);
}

/** Rol del usuario en el equipo; null si no es integrante (docente, monitor). */
export function rolEnEquipo(equipo: EquipoVista, idUsuario: number): RolEquipo | null {
  return equipo.miembros.find((m) => m.idUsuario === idUsuario)?.rolEquipo ?? null;
}

/** "Hito 2 · Diseño" (Figma P04/P05/P06). */
export function etiquetaHito(orden: number, nombreHito: string): string {
  return `Hito ${orden} · ${nombreHito}`;
}

/** Al abrir P04 o P06: el núcleo responde 403 también si no existe (decisión 15-2). */
export function mensajeErrorAbrir(error: unknown, sinAcceso: string): string {
  if (error instanceof ErrorApi && (error.estado === 403 || error.estado === 404)) {
    return sinAcceso;
  }
  return mensajeDeError(error);
}
