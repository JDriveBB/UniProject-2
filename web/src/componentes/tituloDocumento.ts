/** Construye el título de la pestaña del navegador a partir del título de la pantalla actual. */
export function tituloDocumento(titulo: string): string {
  const limpio = titulo.trim();
  return limpio === '' ? 'UniProject' : `${limpio} · UniProject`;
}
