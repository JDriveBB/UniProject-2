import type { EquipoVista, ProyectoVista } from '../api/tipos';
import { idDeRuta } from '../proyectos/vista';
import { rutas } from '../rutas';
import type { HitoDelEquipo } from '../tablero/ubicacion';

export interface Seleccion {
  idProyecto: number | null;
  idEquipo: number | null;
  idHitoEquipo: number | null;
}

export function proyectosDelDocente(
  proyectos: ProyectoVista[],
  idUsuario: number
): ProyectoVista[] {
  return proyectos.filter((p) => p.idDocente === idUsuario);
}

export function elegirProyecto(
  proyectos: ProyectoVista[],
  texto: string | null
): ProyectoVista | null {
  if (proyectos.length === 0) {
    return null;
  }
  const id = idDeRuta(texto ?? undefined);
  if (id !== null) {
    const encontrado = proyectos.find((p) => p.idProyecto === id);
    if (encontrado !== undefined) {
      return encontrado;
    }
  }
  return proyectos[0];
}

export function elegirEquipo(
  equipos: EquipoVista[],
  texto: string | null
): EquipoVista | null {
  if (equipos.length === 0) {
    return null;
  }
  const id = idDeRuta(texto ?? undefined);
  if (id !== null) {
    const encontrado = equipos.find((e) => e.idEquipo === id);
    if (encontrado !== undefined) {
      return encontrado;
    }
  }
  return equipos[0];
}

export function elegirHito(
  hitos: HitoDelEquipo[],
  texto: string | null
): HitoDelEquipo | null {
  const id = idDeRuta(texto ?? undefined);
  if (id !== null) {
    const encontrado = hitos.find((h) => h.idHitoEquipo === id);
    if (encontrado !== undefined) {
      return encontrado;
    }
  }
  return null;
}

export function rutaReportes(sel: Seleccion): string {
  const params = new URLSearchParams();
  if (sel.idProyecto !== null) {
    params.set('proyecto', String(sel.idProyecto));
  }
  if (sel.idEquipo !== null) {
    params.set('equipo', String(sel.idEquipo));
  }
  if (sel.idHitoEquipo !== null) {
    params.set('hito', String(sel.idHitoEquipo));
  }
  const query = params.toString();
  return query ? `${rutas.reportes()}?${query}` : rutas.reportes();
}
