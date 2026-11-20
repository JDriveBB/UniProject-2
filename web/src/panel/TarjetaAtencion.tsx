import { Link } from 'react-router';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { SIN_ATENCION, type ItemAtencion } from './vista';

export function TarjetaAtencion({ elementos }: { elementos: ItemAtencion[] }) {
  return (
    <Tarjeta titulo="Necesitan tu atención">
      {elementos.length === 0 ? (
        <p className="pagina__vacio">{SIN_ATENCION}</p>
      ) : (
        <ul className="atencion">
          {elementos.map((item) => (
            <li key={item.clave} className="atencion__fila">
              <Insignia tono={item.tono}>{item.etiqueta}</Insignia>
              <span className="atencion__texto">
                {item.proyecto !== undefined && <span className="atencion__proyecto">{item.proyecto}</span>}
                {item.texto}
              </span>
              <Link
                className={
                  item.tipo === 'calificar'
                    ? 'boton boton--primario atencion__accion'
                    : 'boton boton--secundario atencion__accion'
                }
                to={item.ruta}
                aria-label={item.nombreAccesible}
              >
                {item.accion}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </Tarjeta>
  );
}
