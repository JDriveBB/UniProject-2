import { useEffect, useId, useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router';
import { solicitarRecuperacion } from '../api/cuentas';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { MarcoAcceso } from '../componentes/MarcoAcceso';
import { mensajeErrorHabilitar, validarCorreo } from '../cuentas/validacion';
import { rutas } from '../rutas';

/** Olvidé mi contraseña (bloque 28, 26a-bis). La respuesta es la misma exista o no la cuenta (28-5): nunca se dice si el correo tiene cuenta o Telegram. */
export function OlvideContrasena() {
  const [email, setEmail] = useState('');
  const [intentado, setIntentado] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [enviado, setEnviado] = useState(false);

  const idAlertaExito = useId();

  useEffect(() => {
    if (enviado) {
      document.getElementById(idAlertaExito)?.focus();
    }
  }, [enviado, idAlertaExito]);

  async function alEnviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setIntentado(true);
    setError(null);
    if (validarCorreo(email) !== null) {
      return;
    }
    setEnviando(true);
    try {
      await solicitarRecuperacion(email.trim());
      setEnviado(true);
      setEmail('');
    } catch (e) {
      // mensajeErrorHabilitar sirve tal cual: el único campo es el correo (400 VALIDACION) y 429 trae su texto.
      setError(mensajeErrorHabilitar(e));
    } finally {
      setEnviando(false);
    }
  }

  const subtitulo = enviado
    ? undefined
    : 'Escribe el correo de tu cuenta. Si tiene Telegram vinculado, te enviaremos por el bot un enlace para crear una contraseña nueva.';

  return (
    <MarcoAcceso titulo="Recuperar contraseña" subtitulo={subtitulo} variante="ingreso">
      {!enviado ? (
        <form className="tarjeta-acceso__formulario" onSubmit={(e) => void alEnviar(e)} noValidate>
          {error !== null && <Alerta tipo="error">{error}</Alerta>}
          <Campo
            etiqueta="Correo institucional"
            type="email"
            autoComplete="username"
            placeholder="nombre@uninorte.edu.co"
            maxLength={254}
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={intentado ? (validarCorreo(email) ?? undefined) : undefined}
          />
          <Boton type="submit" cargando={enviando} className="boton--acceso">
            Enviar enlace
          </Boton>
        </form>
      ) : (
        <>
          <Alerta tipo="exito" id={idAlertaExito} tabIndex={-1}>
            Si el correo corresponde a una cuenta con Telegram vinculado, recibirás el enlace en unos segundos. Vence en 15 minutos.
          </Alerta>
          <p className="tarjeta-acceso__nota">
            ¿No tienes Telegram vinculado? No podemos enviarte el enlace: si sigues con la sesión abierta en otro dispositivo, cambia la contraseña desde tu perfil.
          </p>
        </>
      )}
      <p className="tarjeta-acceso__pie">
        <Link className="enlace-fuerte" to={rutas.ingresar()}>
          Volver a ingresar
        </Link>
        <span aria-hidden="true">·</span>
        <Link className="enlace-fuerte" to={rutas.restablecerContrasena()}>
          Ya tengo el código
        </Link>
      </p>
    </MarcoAcceso>
  );
}
