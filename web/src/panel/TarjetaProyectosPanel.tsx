import { Link } from 'react-router';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { rutas } from '../rutas';
import type { ProyectoDelPanel } from './general';

export function TarjetaProyectosPanel({ proyectos }: { proyectos: ProyectoDelPanel[] }) {
  return (
    <Tarjeta titulo="Tus proyectos">
      <ul className="proyectos-panel">
        {proyectos.map((p) => (
          <li key={p.idProyecto} className="proyectos-panel__fila">
            <div className="proyectos-panel__datos">
              <Link
                className="enlace-fuerte proyectos-panel__nombre"
                to={rutas.panelProyecto(p.idProyecto)}
                aria-label={`Abrir panel de ${p.nombre}`}
              >
                {p.nombre}
              </Link>
              {p.marca !== null ? (
                <Insignia tono={p.marca.tono}>{p.marca.texto}</Insignia>
              ) : (
                <span className="texto-secundario proyectos-panel__estado">No se pudieron cargar sus datos</span>
              )}
            </div>
            <Link
              className="boton boton--secundario proyectos-panel__accion"
              to={rutas.proyecto(p.idProyecto)}
              aria-label={`Configurar ${p.nombre}`}
            >
              Configurar
            </Link>
          </li>
        ))}
      </ul>
    </Tarjeta>
  );
}
