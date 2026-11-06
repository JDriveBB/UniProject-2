const FECHA_BOGOTA = new Intl.DateTimeFormat('es-CO', {
  timeZone: 'America/Bogota',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
});

/**
 * Instante UTC de la API (date-time) → "dd/mm/aaaa" en hora de Colombia (decisión 26a-11). Recorta las fracciones a
 * 3 dígitos (Jackson manda microsegundos). "—" si no es una fecha.
 */
export function fechaCorta(instante: string): string {
  const fecha = new Date(instante.replace(/(\.\d{3})\d+/, '$1'));
  if (Number.isNaN(fecha.getTime())) {
    return '—';
  }
  const partes = FECHA_BOGOTA.formatToParts(fecha);
  const valor = (tipo: Intl.DateTimeFormatPartTypes) => partes.find((p) => p.type === tipo)?.value ?? '';
  return `${valor('day')}/${valor('month')}/${valor('year')}`;
}

const FECHA_HORA_BOGOTA = new Intl.DateTimeFormat('es-CO', {
  timeZone: 'America/Bogota',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

/** Historial de avances (Figma P07): 22/09/2026 14:10 */
export function fechaHora(instante: string): string {
  const fecha = new Date(instante.replace(/(\.\d{3})\d+/, '$1'));
  if (Number.isNaN(fecha.getTime())) {
    return '—';
  }
  const partes = FECHA_HORA_BOGOTA.formatToParts(fecha);
  const valor = (tipo: Intl.DateTimeFormatPartTypes) => partes.find((p) => p.type === tipo)?.value ?? '';
  return `${valor('day')}/${valor('month')}/${valor('year')} ${valor('hour')}:${valor('minute')}`;
}

const FECHA_ISO = /^(\d{4})-(\d{2})-(\d{2})$/;

/**
 * Rango de dos fechas de la API (date, "aaaa-mm-dd") como en Figma P03: "01/08 – 29/08/2026"; con años distintos,
 * "15/12/2026 – 20/01/2027". Son fechas de calendario: sin zona horaria. "Sin fechas" si falta alguna.
 */
export function rangoFechas(inicio: string, fin: string): string {
  const a = FECHA_ISO.exec(inicio);
  const b = FECHA_ISO.exec(fin);
  if (a === null || b === null) {
    return 'Sin fechas';
  }
  const desde = a[1] === b[1] ? `${a[3]}/${a[2]}` : `${a[3]}/${a[2]}/${a[1]}`;
  return `${desde} – ${b[3]}/${b[2]}/${b[1]}`;
}

/** Número con coma decimal (es-CO) y sin separador de miles: 33.5 → "33,5". */
export function decimalEsCo(valor: number): string {
  return String(valor).replace('.', ',');
}

/** Fecha de calendario de la API ("aaaa-mm-dd") → "dd/mm/aaaa" (Figma P04/P06). Sin zona horaria. "—" si no es fecha. */
export function fechaDia(fecha: string): string {
  const m = FECHA_ISO.exec(fecha);
  return m === null ? '—' : `${m[3]}/${m[2]}/${m[1]}`;
}

/** Hoy en Bogotá, para comparar con fechas límite (el núcleo vence al empezar el día siguiente: FechasDominio.venceEn). */
export function fechaIsoBogota(instante: Date): string {
  const partes = FECHA_BOGOTA.formatToParts(instante);
  const valor = (tipo: Intl.DateTimeFormatPartTypes) => partes.find((p) => p.type === tipo)?.value ?? '';
  return `${valor('year')}-${valor('month')}-${valor('day')}`;
}

