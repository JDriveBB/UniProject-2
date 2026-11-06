import { useEffect, useId, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';
import { restablecerContrasena } from '../api/cuentas';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { MarcoAcceso } from '../componentes/MarcoAcceso';
import {
  CODIGO_NO_VALIDO,
  codigoDelFragmento,
  errorDeRestablecer,
  solicitudDeRestablecer,
  validarRestablecer,
} from '../cuentas/recuperacion';
import type { DatosRestablecer, ErrorRestablecer } from '../cuentas/recuperacion';
import { hayErrores } from '../cuentas/validacion';
import { rutas } from '../rutas';
import { useSesion } from '../sesion/ContextoSesion';

/** Crea una contraseña nueva con el código de Telegram (bloque 28, 26a-bis). El código se lee del fragmento #codigo= y se borra de la URL en seguida; solo vive en este estado (26a-bis-3). */
export function RestablecerContrasena() {
  const { hash } = useLocation();
  const navegar = useNavigate();
  const { sesion, salir } = useSesion();

  const [codigoEnlace, setCodigoEnlace] = useState<string | null>(() => codigoDelFragmento(hash));
  const [codigoEscrito, setCodigoEscrito] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [intentado, setIntentado] = useState(false);
  const [error, setError] = useState<ErrorRestablecer | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [enlaceAgotado, setEnlaceAgotado] = useState(false);

  const idAlertaAgotado = useId();

  useEffect(() => {
    if (hash !== '') {
      setCodigoEnlace(codigoDelFragmento(hash));
      setEnlaceAgotado(false);
      setError(null);
      navegar(rutas.restablecerContrasena(), { replace: true });
    }
  }, [hash, navegar]);

  useEffect(() => {
    if (enlaceAgotado) {
      document.getElementById(idAlertaAgotado)?.focus();
    }
  }, [enlaceAgotado, idAlertaAgotado]);

  const esModoManual = codigoEnlace === null;
  const subtitulo =
    esModoManual && !enlaceAgotado ? 'Pega el código que te llegó por Telegram y elige tu contraseña nueva.' : undefined;

  const datos: DatosRestablecer = {
    codigo: codigoEnlace ?? codigoEscrito,
    contrasena,
    confirmacion,
  };
  const errores = intentado ? validarRestablecer(datos) : {};

  async function alEnviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setIntentado(true);
    setError(null);
    const datosEnvio: DatosRestablecer = {
      codigo: codigoEnlace ?? codigoEscrito,
      contrasena,
      confirmacion,
    };
    if (hayErrores(validarRestablecer(datosEnvio))) {
      return;
    }
    setEnviando(true);
    try {
      await restablecerContrasena(solicitudDeRestablecer(datosEnvio));
      if (sesion.estado === 'autenticado') {
        await salir().catch(() => undefined);
      }
      navegar(rutas.ingresar(), { replace: true, state: { contrasenaRestablecida: true } });
    } catch (e) {
      const r = errorDeRestablecer(e);
      if (r.pedirNuevo && codigoEnlace !== null) {
        setEnlaceAgotado(true);
      } else if (r.pedirNuevo && codigoEnlace === null) {
        setCodigoEscrito('');
        setIntentado(false);
        setError(r);
      } else {
        setError(r);
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <MarcoAcceso titulo="Crea una contraseña nueva" subtitulo={subtitulo} variante="registro">
      {!enlaceAgotado ? (
        <form className="tarjeta-acceso__formulario" onSubmit={(e) => void alEnviar(e)} noValidate>
          {error !== null && (
            <Alerta tipo="error">
              {error.mensaje}
              {error.pedirNuevo && (
                <>
                  {' '}
                  <Link className="enlace-fuerte" to={rutas.olvideContrasena()}>
                    Pedir un enlace nuevo
                  </Link>
                </>
              )}
            </Alerta>
          )}
          {esModoManual && (
            <Campo
              etiqueta="Código"
              autoComplete="one-time-code"
              autoCapitalize="none"
              spellCheck={false}
              maxLength={100}
              required
              value={codigoEscrito}
              onChange={(e) => setCodigoEscrito(e.target.value)}
              error={errores.codigo}
            />
          )}
          <Campo
            etiqueta="Nueva contraseña"
            type="password"
            autoComplete="new-password"
            placeholder="Mínimo 10 caracteres"
            required
            value={contrasena}
            onChange={(e) => setContrasena(e.target.value)}
            error={errores.contrasena}
          />
          <Campo
            etiqueta="Confirmar contraseña"
            type="password"
            autoComplete="new-password"
            placeholder="Repite la contraseña"
            required
            value={confirmacion}
            onChange={(e) => setConfirmacion(e.target.value)}
            error={errores.confirmacion}
          />
          <Boton type="submit" cargando={enviando} className="boton--acceso">
            Guardar contraseña
          </Boton>
        </form>
      ) : (
        <Alerta tipo="error" id={idAlertaAgotado} tabIndex={-1}>
          {CODIGO_NO_VALIDO}{' '}
          <Link className="enlace-fuerte" to={rutas.olvideContrasena()}>
            Pedir un enlace nuevo
          </Link>
        </Alerta>
      )}
      <p className="tarjeta-acceso__pie">
        <Link className="enlace-fuerte" to={rutas.ingresar()}>
          Volver a ingresar
        </Link>
      </p>
    </MarcoAcceso>
  );
}
