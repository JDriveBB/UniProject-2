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
