import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router';
import { registrarCuenta } from '../api/cuentas';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Casilla } from '../componentes/Casilla';
import { MarcoAcceso } from '../componentes/MarcoAcceso';
import { hayErrores, mensajeErrorRegistro, solicitudDeRegistro, validarRegistro } from '../cuentas/validacion';
import type { DatosRegistro, ErroresRegistro } from '../cuentas/validacion';
import { rutas } from '../rutas';
import { useSesion } from '../sesion/ContextoSesion';

const VACIO: DatosRegistro = { nombre: '', email: '', contrasena: '', confirmacion: '', autoriza: false };

/** P01b. Solo crea la cuenta de un correo habilitado (K2, R-10); no inicia sesión (decisión 26a-5). */
export function Registro() {
  const { sesion } = useSesion();
  const navegar = useNavigate();
  const [datos, setDatos] = useState<DatosRegistro>(VACIO);
  const [intentado, setIntentado] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  if (sesion.estado === 'autenticado') {
    return <Navigate to="/" replace />;
  }

  const errores: ErroresRegistro = intentado ? validarRegistro(datos) : {};

  function cambiar<K extends keyof DatosRegistro>(campo: K, valor: DatosRegistro[K]) {
    setDatos((previos) => ({ ...previos, [campo]: valor }));
  }

  async function alEnviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setIntentado(true);
    setError(null);
    if (hayErrores(validarRegistro(datos))) {
      return;
    }
    setEnviando(true);
    try {
      await registrarCuenta(solicitudDeRegistro(datos));
      navegar(rutas.ingresar(), { replace: true, state: { cuentaCreada: true } });
    } catch (e) {
      setError(mensajeErrorRegistro(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <MarcoAcceso titulo="Crear cuenta" variante="registro">
      <form className="tarjeta-acceso__formulario" onSubmit={(e) => void alEnviar(e)} noValidate>
        {error !== null && <Alerta tipo="error">{error}</Alerta>}
        <Campo
          etiqueta="Nombre completo"
          placeholder="Nombre y apellidos"
          autoComplete="name"
          maxLength={120}
          value={datos.nombre}
          onChange={(e) => cambiar('nombre', e.target.value)}
          error={errores.nombre}
          required
        />
        <Campo
          etiqueta="Correo electrónico"
          type="email"
          placeholder="nombre@uninorte.edu.co"
          autoComplete="email"
          maxLength={254}
          value={datos.email}
          onChange={(e) => cambiar('email', e.target.value)}
          error={errores.email}
          required
        />
        <Campo
          etiqueta="Contraseña"
          type="password"
          placeholder="Mínimo 10 caracteres"
          autoComplete="new-password"
          value={datos.contrasena}
          onChange={(e) => cambiar('contrasena', e.target.value)}
          error={errores.contrasena}
          required
        />
        <Campo
          etiqueta="Confirmar contraseña"
          type="password"
          placeholder="Repite la contraseña"
          autoComplete="new-password"
          value={datos.confirmacion}
          onChange={(e) => cambiar('confirmacion', e.target.value)}
          error={errores.confirmacion}
          required
        />
        <Casilla
          checked={datos.autoriza}
          onChange={(e) => cambiar('autoriza', e.target.checked)}
          error={errores.autoriza}
          required
        >
          Autorizo el tratamiento de mis datos personales conforme a la Ley 1581 de 2012. (Obligatorio)
        </Casilla>
        <Boton type="submit" cargando={enviando} className="boton--acceso">
          Crear cuenta
        </Boton>
      </form>
      <p className="tarjeta-acceso__pie">
        <span>¿Ya tienes cuenta?</span>
        <Link className="enlace-fuerte" to={rutas.ingresar()}>
          Iniciar sesión
        </Link>
      </p>
    </MarcoAcceso>
  );
}
