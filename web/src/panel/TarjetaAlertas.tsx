import { Link } from 'react-router';
import { Ayuda } from '../componentes/Ayuda';
import { EXPLICACION_ALERTAS } from '../componentes/ayudas';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { rutas } from '../rutas';
import { BLOQUEADA, INACTIVO, SIN_ALERTAS, textoBloqueo, textoInactivo, type AlertasEquipo } from './vista';

export function TarjetaAlertas({ alertas }: { alertas: AlertasEquipo[] }) {
  return (
    <Tarjeta titulo="Alertas" acciones={<Ayuda termino="Alertas">{EXPLICACION_ALERTAS}</Ayuda>}>
      {alertas.length === 0 ? (
        <p className="pagina__vacio">{SIN_ALERTAS}</p>
      ) : (
        <div className="pila">
          {alertas.map((a) => {
            const idTitulo = `titulo-alerta-equipo-${a.idEquipo}`;
            return (
              <section key={a.idEquipo} className="pagina__seccion" aria-labelledby={idTitulo}>
                <h3 id={idTitulo} className="tarjeta__subtitulo">
                  <Link className="enlace-fuerte" to={rutas.tableroEquipo(a.idEquipo)}>
                    {a.nombreEquipo}
                  </Link>
                </h3>
                <ul className="lista-simple">
                  {a.inactivos.map((i, indice) => (
                    <li key={`i-${i.idUsuario}-${indice}`} className="fila-panel fila-panel--alerta">
                      <Insignia tono={INACTIVO.tono}>{INACTIVO.texto}</Insignia>
                      <div className="fila-panel__texto">
                        <span>{textoInactivo(i)}</span>
                      </div>
                    </li>
                  ))}
                  {a.bloqueadas.map((b) => (
                    <li key={`t-${b.idTarea}`} className="fila-panel fila-panel--alerta">
                      <Insignia tono={BLOQUEADA.tono}>{BLOQUEADA.texto}</Insignia>
                      <div className="fila-panel__texto">
                        <Link className="enlace-tarea" to={rutas.tarea(b.idTarea)}>
                          {b.titulo}
                        </Link>
                        <span className="texto-secundario">{textoBloqueo(b.horasBloqueada)}</span>
                      </div>
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
