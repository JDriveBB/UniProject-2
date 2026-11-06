import { useEffect, useId, useState } from 'react';
import { useNavigate } from 'react-router';
import { ErrorApi } from '../api/cliente';
import { suprimirCuenta } from '../api/perfil';
import type { RolPlataforma } from '../api/tipos';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Casilla } from '../componentes/Casilla';
import { Modal } from '../componentes/Modal';
import { Tarjeta } from '../componentes/Tarjeta';
import type { Mensaje } from '../proyectos/errores';
import { rutas } from '../rutas';
import { useSesion } from '../sesion/ContextoSesion';
import { SUPRESION_VACIA, validarSupresion, type BorradorSupresion } from './formularios';
import {
  ADMIN_NO_ELIMINA,
  CAMPOS_SUPRESION,
  mensajeErrorPerfil,
  puedeEliminarse,
  REGLAS_ELIMINAR,
  SE_BORRA,
  SE_CONSERVA,
  TEXTO_CONFIRMAR_ELIMINAR,
  TEXTO_ELIMINAR,
} from './vista';

export interface PropsTarjetaEliminarCuenta {
  rol: RolPlataforma;
  avisar: (m: Mensaje) => void;
}

export function TarjetaEliminarCuenta({ rol, avisar }: PropsTarjetaEliminarCuenta) {
  const { salir } = useSesion();
  const navegar = useNavigate();
  const [confirmando, setConfirmando] = useState(false);
  const [borrador, setBorrador] = useState<BorradorSupresion>(SUPRESION_VACIA);
  const [intentado, setIntentado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [errorServidor, setErrorServidor] = useState<string | undefined>(undefined);
  const [foco, setFoco] = useState<string | null>(null);

  const baseId = useId();
  const idBotonEliminar = baseId + '-eliminar';
  const idCampoContrasena = baseId + '-contrasena';
  const idCasillaEntiendo = baseId + '-entiendo';

  useEffect(() => {
    if (confirmando) {
      document.getElementById(idCampoContrasena)?.focus();
    }
  }, [confirmando, idCampoContrasena]);

  useEffect(() => {
    if (!enviando && foco !== null) {
      document.getElementById(foco)?.focus();
      setFoco(null);
    }
  }, [enviando, foco]);

  function cerrarModal() {
    setConfirmando(false);
    setBorrador(SUPRESION_VACIA);
    setIntentado(false);
    setErrorServidor(undefined);
    setFoco(null);
    document.getElementById(idBotonEliminar)?.focus();
  }

  async function ejecutarSupresion() {
    setIntentado(true);
    const errores = validarSupresion(borrador);
    if (errores.contrasena !== undefined) {
      document.getElementById(idCampoContrasena)?.focus();
      return;
    }
    if (errores.entiendo !== undefined) {
      document.getElementById(idCasillaEntiendo)?.focus();
      return;
    }
    setEnviando(true);
    try {
      await suprimirCuenta(borrador.contrasena);
      try {
        await salir();
      } catch {
        // ignorado deliberadamente
      }
      navegar(rutas.ingresar(), { replace: true, state: { cuentaEliminada: true } });
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 400 && e.codigo === 'CONTRASENA_INCORRECTA') {
        setErrorServidor(e.message);
        setFoco(idCampoContrasena);
        setEnviando(false);
        return;
      }
      setConfirmando(false);
      setIntentado(false);
      setBorrador(SUPRESION_VACIA);
      setErrorServidor(undefined);
      setEnviando(false);
      avisar({ tipo: 'error', texto: mensajeErrorPerfil(e, CAMPOS_SUPRESION) });
    }
  }

  const erroresLocales = intentado ? validarSupresion(borrador) : {};
  const errorContrasena = errorServidor ?? erroresLocales.contrasena;
  const errorEntiendo = erroresLocales.entiendo;

  return (
    <>
      <Tarjeta titulo="Eliminar cuenta">
        <p>{TEXTO_ELIMINAR}</p>
        <h3 className="tarjeta__subtitulo">Se borra o se anonimiza</h3>
        <ul className="lista-puntos">
          {SE_BORRA.map((punto) => (
            <li key={punto}>{punto}</li>
          ))}
        </ul>
        <h3 className="tarjeta__subtitulo">Se conserva, sin tu nombre</h3>
        <ul className="lista-puntos">
          {SE_CONSERVA.map((punto) => (
            <li key={punto}>{punto}</li>
          ))}
        </ul>
        {!puedeEliminarse(rol) ? (
          <p className="texto-ayuda">{ADMIN_NO_ELIMINA}</p>
        ) : (
          <>
            <p className="texto-ayuda">{REGLAS_ELIMINAR}</p>
            <div>
              <Boton id={idBotonEliminar} variante="peligro" onClick={() => setConfirmando(true)}>
                Eliminar mi cuenta
              </Boton>
            </div>
          </>
        )}
      </Tarjeta>
      {confirmando && (
        <Modal
          ancho="dialogo"
          titulo="¿Eliminar tu cuenta?"
          tonoTitulo="peligro"
          ocupado={enviando}
          alCerrar={cerrarModal}
          alEnviar={() => void ejecutarSupresion()}
          acciones={(cerrar) => (
            <>
              <Boton variante="secundario" onClick={cerrar} disabled={enviando}>
                Cancelar
              </Boton>
              <Boton variante="peligro" type="submit" cargando={enviando}>
                Eliminar mi cuenta
              </Boton>
            </>
          )}
        >
          <p className="modal__texto">{TEXTO_CONFIRMAR_ELIMINAR}</p>
          <Campo
            id={idCampoContrasena}
            etiqueta="Contraseña actual"
            type="password"
            autoComplete="current-password"
            value={borrador.contrasena}
            onChange={(e) => {
              setBorrador((prev) => ({ ...prev, contrasena: e.target.value }));
              setErrorServidor(undefined);
            }}
            error={errorContrasena}
            disabled={enviando}
          />
          <Casilla
            id={idCasillaEntiendo}
            checked={borrador.entiendo}
            onChange={(e) => setBorrador((prev) => ({ ...prev, entiendo: e.target.checked }))}
            error={errorEntiendo}
            disabled={enviando}
          >
            Entiendo que esto no se puede deshacer.
          </Casilla>
        </Modal>
      )}
    </>
  );
}
