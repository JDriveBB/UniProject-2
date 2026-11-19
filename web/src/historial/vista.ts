import type { EntradaHistorial, EquipoVista, TableroHito, TareaVista } from '../api/tipos';
import { fechaDia, fechaHora } from '../formato';
import { textoEntidad } from '../reportes/vista';
import { rutas } from '../rutas';
import { codigosDeTareas, estadoHito, estadoTarea } from '../tablero/marcas';
import { etiquetaHito, rolEnEquipo } from '../tablero/ubicacion';
import type { EquipoUbicado, HitoDelEquipo } from '../tablero/ubicacion';
import { diaBogota } from './filtros';

export const TITULO_HISTORIAL = 'Historial del equipo';
export const SOLO_EQUIPO_Y_DOCENTE =
  'Solo el docente del proyecto y los integrantes del equipo pueden ver este historial.';
export const NOTAS_RESERVADAS = 'Las notas individuales de tus compañeros no se muestran aquí: solo ves la tuya.';
export const TODAS_LAS_ACCIONES = 'Todas las acciones';
export const SIN_COINCIDENCIAS = 'Ningún cambio coincide con los filtros.';
export const HISTORIAL_RECORTADO_EQUIPO =
  'Se cargaron los 1000 cambios más recientes del equipo; los anteriores siguen en el registro de auditoría, pero no se muestran aquí.';
export const TAREA_ELIMINADA = 'Tarea eliminada';
export const EXINTEGRANTE = 'Exintegrante';
export const SIN_ASIGNAR = 'Sin asignar';
export const TAMANO_PAGINA = 50;

export interface Visor {
  idUsuario: number;
  esDocente: boolean;
}

export interface TareaIndexada {
  idTarea: number;
  titulo: string;
  codigo: string;
  idHitoEquipo: number;
}

export interface IndiceEquipo {
  idEquipo: number;
  hitos: ReadonlyMap<number, HitoDelEquipo>;
  tareas: ReadonlyMap<number, TareaIndexada>;
  tareasPorHito: ReadonlyMap<number, TareaVista[]>;
  nombres: ReadonlyMap<number, string>;
}

export interface Referencia {
  texto: string;
  ruta: string | null;
  contexto: string | null;
}

export interface GrupoDia<T> {
  dia: string | null;
  entradas: T[];
}

/** Decisión del usuario 2026-11-06: docente del proyecto e integrantes; monitores no. */
export function puedeVerHistorial(u: Pick<EquipoUbicado, 'proyecto' | 'equipo'>, idUsuario: number): boolean {
  return u.proyecto.idDocente === idUsuario || rolEnEquipo(u.equipo, idUsuario) !== null;
}

// Segunda capa: el núcleo ya filtra estas filas para los integrantes (bloque 34); aquí se repite por defensa en profundidad.
export function entradasVisibles<T extends Pick<EntradaHistorial, 'accion' | 'campo'>>(
  entradas: T[],
  visor: Visor,
): T[] {
  if (visor.esDocente) {
    return [...entradas];
  }
  return entradas.filter((e) => {
    if (e.accion === 'CONFIRMAR_NOTA' && e.campo !== `nota_final:${visor.idUsuario}`) {
      return false;
    }
    return true;
  });
}

export function indiceEquipo(
  equipo: Pick<EquipoVista, 'idEquipo' | 'miembros'>,
  hitos: HitoDelEquipo[],
  tableros: Pick<TableroHito, 'idHitoEquipo' | 'tareas'>[],
): IndiceEquipo {
  const hitosMap = new Map<number, HitoDelEquipo>();
  for (const h of hitos) {
    hitosMap.set(h.idHitoEquipo, h);
  }

  const nombresMap = new Map<number, string>();
  for (const m of equipo.miembros) {
    nombresMap.set(m.idUsuario, m.nombre);
  }

  const tareasPorHito = new Map<number, TareaVista[]>();
  const tareasMap = new Map<number, TareaIndexada>();

  for (const t of tableros) {
    tareasPorHito.set(t.idHitoEquipo, t.tareas);
    const codigos = codigosDeTareas(t.tareas);
    for (const tarea of t.tareas) {
      tareasMap.set(tarea.idTarea, {
        idTarea: tarea.idTarea,
        titulo: tarea.titulo,
        codigo: codigos.get(tarea.idTarea) ?? '?',
        idHitoEquipo: t.idHitoEquipo,
      });
    }
  }

  return {
    idEquipo: equipo.idEquipo,
    hitos: hitosMap,
    tareas: tareasMap,
    tareasPorHito,
    nombres: nombresMap,
  };
}

export function referenciaDe(e: Pick<EntradaHistorial, 'entidad' | 'idRegistro'>, indice: IndiceEquipo): Referencia {
  if (e.entidad === 'tarea') {
    const t = e.idRegistro !== null ? indice.tareas.get(e.idRegistro) : undefined;
    if (t !== undefined) {
      const hito = indice.hitos.get(t.idHitoEquipo);
      return {
        texto: `${t.codigo} · ${t.titulo}`,
        ruta: rutas.tarea(t.idTarea),
        contexto: hito !== undefined ? etiquetaHito(hito.orden, hito.nombreHito) : null,
      };
    }
    return {
      texto: TAREA_ELIMINADA,
      ruta: null,
      contexto: null,
    };
  }

  if (e.entidad === 'hito_equipo' && e.idRegistro !== null) {
    const hito = indice.hitos.get(e.idRegistro);
    if (hito !== undefined) {
      return {
        texto: etiquetaHito(hito.orden, hito.nombreHito),
        ruta: rutas.tableroEnHito(indice.idEquipo, hito.idHitoEquipo),
        contexto: null,
      };
    }
  }

  return {
    texto: textoEntidad(e.entidad),
    ruta: null,
    contexto: null,
  };
}

const ETIQUETAS_EVIDENCIA: ReadonlyMap<string, string> = new Map([
  ['PDF', 'PDF'],
  ['IMAGEN', 'imagen'],
  ['ENLACE', 'enlace'],
]);

function textoEstado(valor: string): string {
  if (valor === 'PENDIENTE' || valor === 'EN_PROGRESO' || valor === 'COMPLETADA') {
    return estadoTarea(valor).texto;
  }
  if (valor === 'EN_CURSO' || valor === 'ENVIADO' || valor === 'EVALUADO') {
    return estadoHito(valor).texto;
  }
  return valor;
}

export function detalleLegible(
  e: Pick<EntradaHistorial, 'campo' | 'valorAnterior' | 'valorNuevo' | 'detalle'>,
  nombres: ReadonlyMap<number, string>,
  visor: Visor,
  tareas?: ReadonlyMap<number, Pick<TareaIndexada, 'codigo' | 'titulo'>>,
): string | null {
  const { campo, valorAnterior: a, valorNuevo: b, detalle } = e;

  const ultimaRegla = () => (detalle === '' ? null : detalle);

  const persona = (v: string): string => {
    if (!/^\d+$/.test(v)) {
      return SIN_ASIGNAR;
    }
    return nombres.get(Number(v)) ?? EXINTEGRANTE;
  };

  const nota = (v: string): string => v.replace('.', ',');

  const cambioDesde = (etiqueta: string, formatear: (v: string) => string): string | null => {
    if (b === null) {
      return ultimaRegla();
    }
    if (a !== null) {
      return `${etiqueta}: ${formatear(a)} → ${formatear(b)}`;
    }
    return `${etiqueta}: ${formatear(b)}`;
  };

  const nombreTarea = (v: string): string => {
    const t = /^\d+$/.test(v) ? tareas?.get(Number(v)) : undefined;
    return t !== undefined ? `${t.codigo} · ${t.titulo}` : TAREA_ELIMINADA;
  };

  if (campo === 'id_encargado' || campo === 'id_usuario' || campo === 'lider') {
    const etiqueta = campo === 'id_encargado' ? 'Encargado' : campo === 'id_usuario' ? 'Integrante' : 'Líder';
    if (a !== null && b !== null) {
      return `${etiqueta}: ${persona(a)} → ${persona(b)}`;
    }
    if (b !== null) {
      return `${etiqueta}: ${persona(b)}`;
    }
    if (a !== null) {
      return `${etiqueta}: ${persona(a)}`;
    }
    return ultimaRegla();
  }

  if (campo !== null && campo.startsWith('nota_final:')) {
    const id = campo.slice('nota_final:'.length);
    const quien = !visor.esDocente && id === String(visor.idUsuario) ? 'Tu nota final' : `Nota final de ${persona(id)}`;
    if (b === null) {
      return ultimaRegla();
    }
    if (a !== null) {
      return `${quien}: ${nota(a)} → ${nota(b)}`;
    }
    return `${quien}: ${nota(b)}`;
  }

  if (campo === 'nota_grupal') {
    if (b === null) {
      return ultimaRegla();
    }
    if (visor.esDocente && a !== null) {
      return `Nota grupal: ${nota(a)} → ${nota(b)}`;
    }
    return `Nota grupal: ${nota(b)}`;
  }

  if (campo === 'solicitud_apoyo' || campo === 'solicitud_correccion' || campo === 'tarea_parcial') {
    return null;
  }

  if (campo === 'estado') {
    return cambioDesde('Estado', textoEstado);
  }

  if (campo === 'porcentaje_avance') {
    return cambioDesde('Avance', (v) => `${v} %`);
  }

  if (campo === 'prerrequisito') {
    const accion = b !== null ? 'Prerrequisito agregado' : a !== null ? 'Prerrequisito quitado' : null;
    const id = b ?? a;
    if (accion === null || id === null) {
      return ultimaRegla();
    }
    return tareas !== undefined ? `${accion}: ${nombreTarea(id)}` : accion;
  }

  if (campo === 'evidencia') {
    const tipo = b !== null ? ETIQUETAS_EVIDENCIA.get(b.trim().split(/\s+/)[0]) : undefined;
    return tipo !== undefined ? `Evidencia: ${tipo}` : ultimaRegla();
  }

  return ultimaRegla();
}

export function horaBogota(instante: string): string {
  const fh = fechaHora(instante);
  if (fh === '—') {
    return '—';
  }
  const espacio = fh.indexOf(' ');
  return espacio === -1 ? fh : fh.slice(espacio + 1);
}

export function agruparPorDia<T extends Pick<EntradaHistorial, 'fechaHora'>>(entradas: T[]): GrupoDia<T>[] {
  const grupos: GrupoDia<T>[] = [];
  for (const e of entradas) {
    const dia = diaBogota(e.fechaHora);
    const ultimo = grupos[grupos.length - 1];
    if (ultimo !== undefined && ultimo.dia === dia) {
      ultimo.entradas.push(e);
    } else {
      grupos.push({ dia, entradas: [e] });
    }
  }
  return grupos;
}

function diaAnteriorIso(fechaIso: string): string {
  const partes = fechaIso.split('-').map(Number);
  const d = new Date(Date.UTC(partes[0], partes[1] - 1, partes[2] - 1));
  return d.toISOString().slice(0, 10);
}

export function etiquetaDia(dia: string | null, hoy: string): string {
  if (dia === null) {
    return 'Sin fecha';
  }
  if (dia === hoy) {
    return `Hoy · ${fechaDia(dia)}`;
  }
  if (dia === diaAnteriorIso(hoy)) {
    return `Ayer · ${fechaDia(dia)}`;
  }
  return fechaDia(dia);
}

export function textoConteo(mostradas: number, coinciden: number, total: number): string {
  const p = (n: number) => (n === 1 ? 'cambio' : 'cambios');
  if (coinciden === total) {
    return `Mostrando ${mostradas} de ${total} ${p(total)}.`;
  }
  return `Mostrando ${mostradas} de ${coinciden} ${p(coinciden)} (filtrados de ${total}).`;
}

export function textoMostrarMas(restantes: number): string {
  return `Mostrar ${Math.min(TAMANO_PAGINA, restantes)} más`;
}
