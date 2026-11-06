import { Link } from 'react-router';
import { Tarjeta } from '../componentes/Tarjeta';
import { fechaDia } from '../formato';
import { rutas } from '../rutas';
import { etiquetaHito } from '../tablero/ubicacion';
import { SIN_POR_EVALUAR, type PorEvaluar } from './vista';

export function TarjetaPorEvaluar({ pendientes }: { pendientes: PorEvaluar[] }) {
  return (
    <Tarjeta titulo="Por evaluar">
      {pendientes.length === 0 ? (
        <p className="pagina__vacio">{SIN_POR_EVALUAR}</p>
      ) : (
        <ul className="lista-simple">
          {pendientes.map((p) => {
            const etiqueta = etiquetaHito(p.orden, p.nombreHito);
            return (
              <li key={p.idHitoEquipo} className="fila-panel">
                <div className="fila-panel__texto">
                  <span className="fila-panel__titulo">{`${etiqueta} · ${p.nombreEquipo}`}</span>
                  <span className="texto-secundario">{`Fecha límite: ${fechaDia(p.fechaLimite)}`}</span>
                </div>
                <Link
                  className="boton boton--primario"
                  to={rutas.evaluarHito(p.idHitoEquipo)}
                  aria-label={`Evaluar ${etiqueta} de ${p.nombreEquipo}`}
                >
                  Evaluar
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </Tarjeta>
  );
}
