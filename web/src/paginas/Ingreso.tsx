import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import { mensajeDeError } from '../api/cliente';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Tarjeta } from '../componentes/Tarjeta';
import { rutas } from '../rutas';
import { useSesion } from '../sesion/ContextoSesion';

/** Solo rutas internas: evita redirecciones abiertas con state manipulado. */
function destinoSeguro(estado: unknown): string {
  if (typeof estado === 'object' && estado !== null && 'desde' in estado) {
    const desde = (estado as { desde: unknown }).desde;
    if (typeof desde === 'string' && desde.startsWith('/') && !desde.startsWith('//')) {
      return desde;
    }
  }
  return '/';
}

export function Ingreso() {
  const { sesion, ingresar } = useSesion();
  const ubicacion = useLocation();
  const navegar = useNavigate();
  const [email, setEmail] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  if (sesion.estado === 'autenticado') {
    return <Navigate to={destinoSeguro(ubicacion.state)} replace />;
  }

  async function alEnviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setError(null);
    setEnviando(true);
    try {
      await ingresar(email.trim(), contrasena);
      navegar(destinoSeguro(ubicacion.state), { replace: true });
    } catch (e) {
      setError(mensajeDeError(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="pagina-centrada">
      <Tarjeta titulo="Ingresar a UniProject">
        <form className="formulario" onSubmit={(e) => void alEnviar(e)} noValidate>
          {error !== null && <Alerta tipo="error">{error}</Alerta>}
          <Campo
            etiqueta="Correo institucional"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Campo
            etiqueta="Contraseña"
            type="password"
            autoComplete="current-password"
            value={contrasena}
            onChange={(e) => setContrasena(e.target.value)}
            required
          />
          <Boton type="submit" cargando={enviando}>
            Ingresar
          </Boton>
        </form>
        <p className="texto-secundario">
          ¿Tu docente ya habilitó tu correo? <Link to={rutas.registro()}>Crea tu cuenta</Link>
        </p>
      </Tarjeta>
    </div>
  );
}
