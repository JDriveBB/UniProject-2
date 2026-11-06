import { useEffect, useId, useState, type FormEvent } from 'react';
import { cambiarContrasena } from '../api/perfil';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Tarjeta } from '../componentes/Tarjeta';
import type { Mensaje } from '../proyectos/errores';
import {
  CONTRASENA_VACIA,
  campoDelErrorContrasena,
  primerErrorContrasena,
  validarCambioContrasena,
  type BorradorContrasena,
  type CampoContrasena,
  type ErroresContrasena,
} from './formularios';
import {
  CAMPOS_CONTRASENA,
  CONTRASENA_ACTUALIZADA,
  ID_TELEGRAM,
  mensajeErrorPerfil,
  SIN_TELEGRAM_RECUPERAR,
} from './vista';

export interface PropsTarjetaContrasena {
  telegramVinculado: boolean;
  avisar: (m: Mensaje) => void;
}

export function TarjetaContrasena({ telegramVinculado, avisar }: PropsTarjetaContrasena) {
  const [abierto, setAbierto] = useState(false);
  const [borrador, setBorrador] = useState<BorradorContrasena>(CONTRASENA_VACIA);
  const [intentado, setIntentado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [erroresServidor, setErroresServidor] = useState<ErroresContrasena>({});
  const [foco, setFoco] = useState<string | null>(null);

  const baseId = useId();
  const idBotonCambiar = baseId + '-cambiar';
  const idCampoActual = baseId + '-actual';
  const idCampoNueva = baseId + '-nueva';
  const idCampoConfirmacion = baseId + '-confirmacion';

  useEffect(() => {
    if (abierto) {
      document.getElementById(idCampoActual)?.focus();
    }
  }, [abierto, idCampoActual]);

  useEffect(() => {
    if (!enviando && foco !== null) {
      document.getElementById(foco)?.focus();
      setFoco(null);
    }
  }, [enviando, foco]);

  function cancelar() {
    setBorrador(CONTRASENA_VACIA);
    setIntentado(false);
    setErroresServidor({});
    setFoco(null);
    setAbierto(false);
    document.getElementById(idBotonCambiar)?.focus();
  }

  function actualizarCampo(campo: CampoContrasena, valor: string) {
    setBorrador((prev) => ({ ...prev, [campo]: valor }));
    if (erroresServidor[campo] !== undefined) {
      setErroresServidor((prev) => ({ ...prev, [campo]: undefined }));
    }
  }

  async function guardar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setIntentado(true);
    const erroresLocales = validarCambioContrasena(borrador);
    const primer = primerErrorContrasena(erroresLocales);
    if (primer !== null) {
      if (primer === 'actual') {
        document.getElementById(idCampoActual)?.focus();
      } else if (primer === 'nueva') {
        document.getElementById(idCampoNueva)?.focus();
      } else if (primer === 'confirmacion') {
        document.getElementById(idCampoConfirmacion)?.focus();
      }
      return;
    }
    setEnviando(true);
    try {
      await cambiarContrasena(borrador.actual, borrador.nueva);
      setBorrador(CONTRASENA_VACIA);
      setIntentado(false);
      setErroresServidor({});
      setAbierto(false);
      avisar({ tipo: 'exito', texto: CONTRASENA_ACTUALIZADA });
    } catch (e) {
      const campo = campoDelErrorContrasena(e);
      if (campo !== null && e instanceof Error) {
        setErroresServidor((prev) => ({ ...prev, [campo]: e.message }));
        if (campo === 'actual') {
          setFoco(idCampoActual);
        } else if (campo === 'nueva') {
          setFoco(idCampoNueva);
        }
      } else {
        avisar({ tipo: 'error', texto: mensajeErrorPerfil(e, CAMPOS_CONTRASENA) });
      }
    } finally {
      setEnviando(false);
    }
  }

  const erroresLocales = intentado ? validarCambioContrasena(borrador) : {};
  const errorActual = erroresServidor.actual ?? erroresLocales.actual;
  const errorNueva = erroresServidor.nueva ?? erroresLocales.nueva;
  const errorConfirmacion = erroresServidor.confirmacion ?? erroresLocales.confirmacion;

  return (
    <Tarjeta titulo="Contraseña">
      {!telegramVinculado && (
        <Alerta tipo="aviso">
          {SIN_TELEGRAM_RECUPERAR}{' '}
          <a className="enlace-fuerte" href={`#${ID_TELEGRAM}`}>
            Vincular Telegram
          </a>
        </Alerta>
      )}
      {!abierto ? (
        <>
          <p className="texto-secundario">Al cambiarla se cierran tus otras sesiones abiertas.</p>
          <div>
            <Boton id={idBotonCambiar} variante="secundario" onClick={() => setAbierto(true)}>
              Cambiar contraseña
            </Boton>
          </div>
        </>
      ) : (
        <form className="pila" noValidate onSubmit={(e) => void guardar(e)}>
          <Campo
            id={idCampoActual}
            etiqueta="Contraseña actual"
            type="password"
            autoComplete="current-password"
            value={borrador.actual}
            onChange={(e) => actualizarCampo('actual', e.target.value)}
            error={errorActual}
            disabled={enviando}
          />
          <Campo
            id={idCampoNueva}
            etiqueta="Nueva contraseña"
            type="password"
            autoComplete="new-password"
            value={borrador.nueva}
            onChange={(e) => actualizarCampo('nueva', e.target.value)}
            error={errorNueva}
            disabled={enviando}
          />
          <p className="texto-ayuda">Mínimo 10 caracteres.</p>
          <Campo
            id={idCampoConfirmacion}
            etiqueta="Confirmar nueva contraseña"
            type="password"
            autoComplete="new-password"
            value={borrador.confirmacion}
            onChange={(e) => actualizarCampo('confirmacion', e.target.value)}
            error={errorConfirmacion}
            disabled={enviando}
          />
          <div className="pagina__acciones">
            <Boton variante="secundario" onClick={cancelar} disabled={enviando}>
              Cancelar
            </Boton>
            <Boton type="submit" cargando={enviando} disabled={enviando}>
              Guardar
            </Boton>
          </div>
        </form>
      )}
    </Tarjeta>
  );
}
