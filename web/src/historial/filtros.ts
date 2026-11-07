import type { EntradaHistorial, TareaVista } from '../api/tipos';
import { fechaIsoBogota } from '../formato';
import { idDeRuta } from '../proyectos/vista';
import { filtrarPorHito, textoAccion } from '../reportes/vista';
import { rutas } from '../rutas';
import type { HitoDelEquipo } from '../tablero/ubicacion';

export interface Filtros {
  accion: string | null;
  idHitoEquipo: number | null;
  desde: string | null;
  hasta: string | null;
}

export const FILTROS_VACIOS: Filtros = {
  accion: null,
  idHitoEquipo: null,
  desde: null,
  hasta: null,
};

export const RANGO_INVALIDO = 'La fecha «Hasta» no puede ser anterior a «Desde».';

export interface OpcionAccion {
  valor: string;
  texto: string;
}

const FORMATO_FECHA = /^\d{4}-\d{2}-\d{2}$/;

/** Formato aaaa-mm-dd Y fecha real del calendario (rechaza 2026-02-30). */
function esFechaValida(texto: string): boolean {
  if (!FORMATO_FECHA.test(texto)) {
    return false;
  }
  const fecha = new Date(`${texto}T00:00:00Z`);
  return !Number.isNaN(fecha.getTime()) && fecha.toISOString().startsWith(texto);
}

/** Día de calendario en Bogotá ("aaaa-mm-dd") de un instante del API. Recorta fracciones a 3 dígitos. */
export function diaBogota(instante: string): string | null {
  const fecha = new Date(instante.replace(/(\.\d{3})\d+/, '$1'));
  if (Number.isNaN(fecha.getTime())) {
    return null;
  }
  return fechaIsoBogota(fecha);
}

/** Lee filtros de búsqueda de la URL (?accion=&hito=&desde=&hasta=). */
export function filtrosDeBusqueda(
  search: string,
  acciones: readonly string[],
  hitos: readonly Pick<HitoDelEquipo, 'idHitoEquipo'>[]
): Filtros {
  const params = new URLSearchParams(search);
  const accionParam = params.get('accion');
  const accion = accionParam !== null && acciones.includes(accionParam) ? accionParam : null;

  const hitoParam = params.get('hito');
  const idHito = idDeRuta(hitoParam ?? undefined);
  const idHitoEquipo = idHito !== null && hitos.some((h) => h.idHitoEquipo === idHito) ? idHito : null;

  const desdeParam = params.get('desde');
  const desde = desdeParam !== null && esFechaValida(desdeParam) ? desdeParam : null;

  const hastaParam = params.get('hasta');
  const hasta = hastaParam !== null && esFechaValida(hastaParam) ? hastaParam : null;

  return { accion, idHitoEquipo, desde, hasta };
}

/** Ruta canónica del historial con los filtros activos en la URL. */
export function rutaHistorial(idEquipo: number, f: Filtros): string {
  const base = rutas.historialEquipo(idEquipo);
  if (!hayFiltros(f)) {
    return base;
  }
  const params = new URLSearchParams();
  if (f.accion !== null) {
    params.set('accion', f.accion);
  }
  if (f.idHitoEquipo !== null) {
    params.set('hito', String(f.idHitoEquipo));
  }
  if (f.desde !== null) {
    params.set('desde', f.desde);
  }
  if (f.hasta !== null) {
    params.set('hasta', f.hasta);
  }
  return `${base}?${params.toString()}`;
}

export function hayFiltros(f: Filtros): boolean {
  return f.accion !== null || f.idHitoEquipo !== null || f.desde !== null || f.hasta !== null;
}

export function validarRango(desde: string | null, hasta: string | null): string | undefined {
  if (desde !== null && hasta !== null && desde > hasta) {
    return RANGO_INVALIDO;
  }
  return undefined;
}

export function opcionesAccion(entradas: readonly Pick<EntradaHistorial, 'accion'>[]): OpcionAccion[] {
  const valores = Array.from(new Set(entradas.map((e) => e.accion)));
  const opciones: OpcionAccion[] = valores.map((valor) => ({
    valor,
    texto: textoAccion(valor),
  }));
  opciones.sort((a, b) => a.texto.localeCompare(b.texto, 'es'));
  return opciones;
}

export function aplicarFiltros<T extends Pick<EntradaHistorial, 'accion' | 'entidad' | 'idRegistro' | 'fechaHora'>>(
  entradas: T[],
  f: Filtros,
  tareasPorHito: ReadonlyMap<number, Pick<TareaVista, 'idTarea'>[]>
): T[] {
  if (!hayFiltros(f)) {
    return [...entradas];
  }
  let resultado = entradas;
  if (f.accion !== null) {
    resultado = resultado.filter((e) => e.accion === f.accion);
  }
  if (f.idHitoEquipo !== null) {
    resultado = filtrarPorHito(resultado, f.idHitoEquipo, tareasPorHito.get(f.idHitoEquipo) ?? []);
  }
  if (f.desde !== null || f.hasta !== null) {
    resultado = resultado.filter((e) => {
      const dia = diaBogota(e.fechaHora);
      if (dia === null) {
        return false;
      }
      if (f.desde !== null && dia < f.desde) {
        return false;
      }
      if (f.hasta !== null && dia > f.hasta) {
        return false;
      }
      return true;
    });
  }
  return resultado;
}
