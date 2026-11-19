import { Insignia } from '../componentes/Insignia';
import { LEYENDA } from './marcas';

/** Leyenda plegable de marcas y franja de ruta crítica en el tablero. */
export function LeyendaMarcas() {
  return (
    <details className="leyenda-marcas">
      <summary className="leyenda-marcas__resumen">¿Qué significan las marcas?</summary>
      <ul className="leyenda-marcas__lista">
        {LEYENDA.map((item) => (
          <li key={item.titulo} className="leyenda-marcas__item">
            {item.marca === null ? (
              <span className="leyenda-marcas__ruta">
                <span className="leyenda-marcas__franja" aria-hidden="true" />
                <span className="leyenda-marcas__titulo">{item.titulo}</span>
              </span>
            ) : (
              <Insignia tono={item.marca.tono}>{item.marca.texto}</Insignia>
            )}
            <span className="leyenda-marcas__explicacion">{item.explicacion}</span>
          </li>
        ))}
      </ul>
    </details>
  );
}
