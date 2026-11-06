import { Link } from 'react-router';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { rutas } from '../rutas';
import {
  BLOQUEADA,
  EXPLICACION_ALERTAS,
  INACTIVO,
  SIN_ALERTAS,
  textoBloqueo,
  textoInactivo,
  type AlertasEquipo,
} from './vista';

export function TarjetaAlertas({ alertas }: { alertas: AlertasEquipo[] }) {
  return (
    <Tarjeta titulo="Alertas">
      <p className="texto-secundario">{EXPLICACION_ALERTAS}</p>
      {alertas.length === 0 ? (
        <p className="pagina__vacio">{SIN_ALERTAS}</p>
      ) : (
        <div className="pila">
          {alertas.map((a) => {
            const idTitulo = `titulo-alerta-equipo-${a.idEquipo}`;
            return (
              <section key={a.idEquipo} className="pagina__seccion" aria-labelledby={idTitulo}>
                <h3 id={idTitulo} className="tarjeta__subtitulo">
                  {a.nombreEquipo}
                </h3>
                <ul className="lista-simple">
                  {a.inactivos.map((i, indice) => (
                    <li key={`i-${i.idUsuario}-${indice}`} className="fila-panel">
                      <Insignia tono={INACTIVO.tono}>{INACTIVO.texto}</Insignia>
                      <span>{textoInactivo(i)}</span>
                    </li>
                  ))}
                  {a.bloqueadas.map((b) => (
                    <li key={`t-${b.idTarea}`} className="fila-panel">
                      <Insignia tono={BLOQUEADA.tono}>{BLOQUEADA.texto}</Insignia>
                      <Link className="enlace-tarea" to={rutas.tarea(b.idTarea)}>
                        {b.titulo}
                      </Link>
                      <span className="texto-secundario">{textoBloqueo(b.horasBloqueada)}</span>
                    </li>
                  ))}
                </ul>
              </section>
            );
          })}
        </div>
      )}
    </Tarjeta>
  );
}
