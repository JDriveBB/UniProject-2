import { Insignia } from '../componentes/Insignia';
import { LEYENDA } from './marcas';

/** Leyenda «Marcas:» de P04, en el orden de Figma. */
export function LeyendaMarcas() {
  return (
    <div className="leyenda-marcas">
      <span className="leyenda-marcas__titulo">Marcas:</span>
      {LEYENDA.map((m) => (
        <Insignia key={m.texto} tono={m.tono}>
          {m.texto}
        </Insignia>
      ))}
    </div>
  );
}
