import { Insignia } from '../componentes/Insignia';
import type { DatoResumen } from './vista';

export interface PropsResumenTrazabilidad {
  datos: DatoResumen[];
}

export function ResumenTrazabilidad({ datos }: PropsResumenTrazabilidad) {
  return (
    <div className="resumen-ruta">
      {datos.map((d) => {
        const claseValor = [
          'dato-resumen__valor',
          d.primario ? 'dato-resumen__valor--primario' : '',
        ]
          .filter(Boolean)
          .join(' ');

        return (
          <div key={d.etiqueta} className="dato-resumen">
            <p className="dato-resumen__etiqueta">{d.etiqueta}</p>
            {d.valor !== null && <p className={claseValor}>{d.valor}</p>}
            {d.marca !== null && (
              <div>
                <Insignia tono={d.marca.tono}>{d.marca.texto}</Insignia>
              </div>
            )}
            {d.detalle !== null && <p className="dato-resumen__etiqueta">{d.detalle}</p>}
          </div>
        );
      })}
    </div>
  );
}
