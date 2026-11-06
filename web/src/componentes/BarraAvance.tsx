/**
 * Barra de avance de Figma P04: pista gris, relleno azul y verde al 100 %, con el porcentaje al lado. Es un
 * <progress> estilado (sin style en línea); el lector de pantalla oye el texto "48 %". ancho: 100 px en la tabla,
 * 160 px en el resumen del hito.
 */
export function BarraAvance({ porcentaje, ancho = 'tabla' }: { porcentaje: number; ancho?: 'tabla' | 'hito' }) {
  const valor = Math.min(100, Math.max(0, Math.round(porcentaje)));
  const clases = ['avance', `avance--${ancho}`, valor === 100 ? 'avance--completo' : ''].filter(Boolean).join(' ');
  return (
    <span className={clases}>
      <progress className="avance__barra" max={100} value={valor} aria-hidden="true" />
      <span className="avance__texto">{`${valor} %`}</span>
    </span>
  );
}
