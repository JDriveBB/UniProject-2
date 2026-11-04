import { Link } from 'react-router';
import { Tarjeta } from '../componentes/Tarjeta';

export function NoEncontrada() {
  return (
    <div className="pagina-centrada">
      <Tarjeta titulo="Página no encontrada">
        <p>La dirección no existe.</p>
        <Link to="/">Volver al inicio</Link>
      </Tarjeta>
    </div>
  );
}
