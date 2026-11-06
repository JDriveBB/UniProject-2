import type { EquipoVista, RolPlataforma } from '../api/tipos';
import { rutas } from '../rutas';

export interface EnlaceBarra {
  texto: string;
  a: string;
  activo: boolean;
}

const PROYECTO = /^\/proyectos\/(\d+)(?:\/|$)/;
const PANEL_PROYECTO = /^\/proyectos\/\d+\/panel\/?$/;
const HISTORIAL = /^\/equipos\/\d+\/historial\/?$/;
const TABLERO = /^\/equipos\/\d+\/tablero\/?$/;
const RUTA_CRITICA = /^\/hitos\/\d+\/ruta-critica\/?$/;
const TAREA = /^\/tareas\/\d+\/?$/;

const ROLES: Record<RolPlataforma, string> = {
  ADMINISTRADOR: 'Administrador',
  DOCENTE: 'Docente',
  ESTUDIANTE: 'Estudiante',
  MONITOR: 'Monitor',
};

function enProyectos(ruta: string): boolean {
  return ruta === rutas.proyectos() || ruta.startsWith(rutas.proyectos() + '/');
}

/** id del proyecto abierto en la ruta (/proyectos/5, /proyectos/5/panel); null si no hay. */
export function idProyectoEnRuta(ruta: string): number | null {
  const m = PROYECTO.exec(ruta);
  return m === null ? null : Number(m[1]);
}

/**
 * Navegación de la barra por rol (decisión 26b-2, Figma P02/P03/P04). "Panel" abre el panel del proyecto que se está
 * viendo; fuera de un proyecto va a /panel. Las pantallas de 26c y 26f son PaginaPendiente por ahora.
 */
export function enlacesDe(rol: RolPlataforma, ruta: string): EnlaceBarra[] {
  if (rol === 'DOCENTE') {
    const id = idProyectoEnRuta(ruta);
    const enPanel = ruta === rutas.panel() || PANEL_PROYECTO.test(ruta);
    return [
      { texto: 'Panel', a: id === null ? rutas.panel() : rutas.panelProyecto(id), activo: enPanel },
      { texto: 'Proyectos', a: rutas.proyectos(), activo: !enPanel && enProyectos(ruta) },
      { texto: 'Reportes', a: rutas.reportes(), activo: ruta === rutas.reportes() || HISTORIAL.test(ruta) },
    ];
  }
  if (rol === 'ESTUDIANTE') {
    return [
      { texto: 'Tablero', a: rutas.tablero(), activo: ruta === rutas.tablero() || TABLERO.test(ruta) },
      { texto: 'Mis tareas', a: rutas.misTareas(), activo: ruta === rutas.misTareas() || TAREA.test(ruta) },
      {
        texto: 'Ruta crítica',
        a: rutas.rutaCriticaActual(),
        activo: ruta === rutas.rutaCriticaActual() || RUTA_CRITICA.test(ruta),
      },
    ];
  }
  if (rol === 'MONITOR') {
    return [{ texto: 'Proyectos', a: rutas.proyectos(), activo: enProyectos(ruta) }];
  }
  return [{ texto: 'Docentes', a: rutas.adminDocentes(), activo: ruta.startsWith('/admin/') }];
}

/** Rol bajo el nombre (26b-3). Un estudiante que lidera algún equipo ve "Líder de equipo" (decisión 26d-12). */
export function etiquetaRol(rol: RolPlataforma, liderDeEquipo = false): string {
  if (rol === 'ESTUDIANTE' && liderDeEquipo) {
    return 'Líder de equipo';
  }
  return ROLES[rol];
}

export function esLiderDeAlgunEquipo(equipos: Pick<EquipoVista, 'miembros'>[], idUsuario: number): boolean {
  return equipos.some((e) => e.miembros.some((m) => m.idUsuario === idUsuario && m.rolEquipo === 'LIDER'));
}

/** Texto de la píldora: null sin no leídas; "99+" desde 100. */
export function textoContador(noLeidas: number): string | null {
  if (!Number.isFinite(noLeidas) || noLeidas <= 0) {
    return null;
  }
  return noLeidas > 99 ? '99+' : String(Math.trunc(noLeidas));
}
