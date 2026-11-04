import { Link } from 'react-router';
import { Tarjeta } from '../componentes/Tarjeta';

export function SinPermiso() {
  return (
    <Tarjeta titulo="Sin permiso">
      <p>Tu rol no tiene acceso a esta pantalla.</p>
      <Link to="/">Volver al inicio</Link>
    </Tarjeta>
  );
}
