import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import { mensajeDeError } from '../api/cliente';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { MarcoAcceso } from '../componentes/MarcoAcceso';
import { rutas } from '../rutas';
import { useSesion } from '../sesion/ContextoSesion';
import { destinoSeguro, vieneDeRegistro } from '../sesion/destino';

/** P01a. Tras ingresar vuelve a la ruta protegida de origen (solo rutas internas) o a la vista del rol. */
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

  const cuentaCreada = vieneDeRegistro(ubicacion.state);

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
    <MarcoAcceso titulo="Iniciar sesión" subtitulo="Ingresa con tu correo institucional." variante="ingreso">
      <form className="tarjeta-acceso__formulario" onSubmit={(e) => void alEnviar(e)} noValidate>
        {cuentaCreada && error === null && (
          <Alerta tipo="exito">Cuenta creada. Ya puedes ingresar con tu correo y tu contraseña.</Alerta>
        )}
        {error !== null && <Alerta tipo="error">{error}</Alerta>}
        <Campo
          etiqueta="Correo"
          type="email"
          autoComplete="username"
          placeholder="nombre@uninorte.edu.co"
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
        <Boton type="submit" cargando={enviando} className="boton--acceso">
          Ingresar
        </Boton>
      </form>
      <p className="tarjeta-acceso__pie">
        <span>¿No tienes cuenta?</span>
        <Link className="enlace-fuerte" to={rutas.registro()}>
          Crear cuenta
        </Link>
      </p>
      <p className="tarjeta-acceso__nota">
        Al ingresar, el sistema te lleva a tu vista según tu rol: docente, estudiante, líder de equipo o monitor.
      </p>
    </MarcoAcceso>
  );
}
