import { Link } from 'react-router';
import { Ayuda } from '../componentes/Ayuda';
import { EXPLICACION_VENCIDO } from '../componentes/ayudas';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { fechaDia } from '../formato';
import { rutas } from '../rutas';
import type { Entrega, UrgenciaEntrega } from './vista';

function clasePlazo(urgencia: UrgenciaEntrega): string {
  if (urgencia === 'normal') {
    return 'entrega-hito__plazo';
  }
  return `entrega-hito__plazo entrega-hito__plazo--${urgencia}`;
}

export function TarjetaEntregas({ entregas }: { entregas: Entrega[] }) {
  const ayuda = <Ayuda termino="Entregas">{EXPLICACION_VENCIDO}</Ayuda>;

  if (entregas.length === 0) {
    return (
      <Tarjeta titulo="Entregas" acciones={ayuda}>
        <p className="pagina__vacio">El proyecto aún no tiene hitos.</p>
      </Tarjeta>
    );
  }

  return (
    <Tarjeta titulo="Entregas" acciones={ayuda}>
      <ol className="entregas">
        {entregas.map((entrega) => (
          <li
            key={entrega.idHito}
            className={entrega.urgencia === 'pasada' ? 'entrega-hito entrega-hito--pasada' : 'entrega-hito'}
          >
            <div className="entrega-hito__cabecera">
              <div className="entrega-hito__titulo">
                <h3 className="entrega-hito__nombre">{entrega.etiqueta}</h3>
                {entrega.proxima && <Insignia tono="info">Próxima</Insignia>}
              </div>
              <span
                className={clasePlazo(entrega.urgencia)}
              >{`${fechaDia(entrega.fechaLimite)} · ${entrega.plazo}`}</span>
            </div>
            {entrega.equipos.length === 0 ? (
              <p className="pagina__vacio">El proyecto aún no tiene equipos.</p>
            ) : (
              <ul className="entrega-hito__equipos">
                {entrega.equipos.map((eq) => (
                  <li key={eq.idEquipo} className="entrega-hito__equipo">
                    <span className="entrega-hito__nombre-equipo">{eq.nombreEquipo}</span>
                    <Link
                      className="enlace-insignia"
                      to={rutas.tableroEnHito(eq.idEquipo, eq.idHitoEquipo)}
                      aria-label={`${entrega.etiqueta} de ${eq.nombreEquipo}: ${eq.marca.texto}`}
                    >
                      <Insignia tono={eq.marca.tono}>{eq.marca.texto}</Insignia>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ol>
    </Tarjeta>
  );
}
