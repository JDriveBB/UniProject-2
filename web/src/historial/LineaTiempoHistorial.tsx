import { Link } from 'react-router';
import type { EntradaHistorial } from '../api/tipos';
import { textoAccion } from '../reportes/vista';
import {
  detalleLegible,
  etiquetaDia,
  horaBogota,
  referenciaDe,
  type GrupoDia,
  type IndiceEquipo,
  type Visor,
} from './vista';

export interface PropsLineaTiempoHistorial {
  grupos: GrupoDia<EntradaHistorial>[];
  hoy: string;
  indice: IndiceEquipo;
  visor: Visor;
  prefijoId: string;
}

export function LineaTiempoHistorial({ grupos, hoy, indice, visor, prefijoId }: PropsLineaTiempoHistorial) {
  return (
    <ol className="linea-tiempo">
      {grupos.map((grupo) => {
        const claveDia = grupo.dia ?? 'sin-fecha';
        return (
          <li key={claveDia} className="linea-tiempo__dia">
            <h3 className="linea-tiempo__fecha">{etiquetaDia(grupo.dia, hoy)}</h3>
            <ol className="linea-tiempo__eventos">
              {grupo.entradas.map((entrada) => {
                const { idLog, fechaHora, autor, accion } = entrada;
                const ref = referenciaDe(entrada, indice);
                const detalle = detalleLegible(entrada, indice.nombres, visor, indice.tareas);

                return (
                  <li key={idLog} className="evento-historial" id={`${prefijoId}-${idLog}`} tabIndex={-1}>
                    <time className="evento-historial__hora" dateTime={fechaHora}>
                      {horaBogota(fechaHora)}
                    </time>
                    <span className="evento-historial__punto" aria-hidden="true" />
                    <div className="evento-historial__cuerpo">
                      <p className="evento-historial__titulo">
                        <span className="evento-historial__autor">{autor}</span>
                        {` · ${textoAccion(accion)}`}
                      </p>
                      <p className="evento-historial__referencia">
                        {ref.ruta !== null ? (
                          <Link className="enlace-tarea" to={ref.ruta}>
                            {ref.texto}
                          </Link>
                        ) : (
                          <span>{ref.texto}</span>
                        )}
                        {ref.contexto !== null && <span className="texto-secundario">{ref.contexto}</span>}
                      </p>
                      {detalle !== null && <p className="texto-largo texto-secundario">{detalle}</p>}
                    </div>
                  </li>
                );
              })}
            </ol>
          </li>
        );
      })}
    </ol>
  );
}
