import { useId, useState } from 'react';
import { mensajeDeError } from '../api/cliente';
import { desvincularTelegram, generarCodigoTelegram } from '../api/perfil';
import type { CodigoGenerado } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Insignia } from '../componentes/Insignia';
import { Modal } from '../componentes/Modal';
import { Tarjeta } from '../componentes/Tarjeta';
import type { Mensaje } from '../proyectos/errores';
import { useSesion } from '../sesion/ContextoSesion';
import {
  esEnlaceTelegram,
  mensajeErrorPerfil,
  textoVence,
  TELEGRAM_ACTIVO,
  TELEGRAM_DESVINCULADO,
  TELEGRAM_OBLIGATORIO,
  TELEGRAM_PENDIENTE,
  TELEGRAM_VINCULADO,
  TEXTO_DESVINCULAR,
} from './vista';

export interface PropsTarjetaTelegram {
  vinculado: boolean;
  avisar: (m: Mensaje) => void;
}

export function TarjetaTelegram({ vinculado, avisar }: PropsTarjetaTelegram) {
  const { refrescar } = useSesion();
  const [codigo, setCodigo] = useState<CodigoGenerado | null>(null);
  const [ocupado, setOcupado] = useState<'codigo' | 'comprobar' | 'desvincular' | null>(null);
  const [confirmando, setConfirmando] = useState(false);

  const baseId = useId();
  const idBotonDesvincular = baseId + '-desvincular';

  async function pedirCodigo() {
    setOcupado('codigo');
    try {
      const respuesta = await generarCodigoTelegram();
      setCodigo(respuesta);
    } catch (e) {
      avisar({ tipo: 'error', texto: mensajeErrorPerfil(e, []) });
    } finally {
      setOcupado(null);
    }
  }

  async function comprobarVinculacion() {
    setOcupado('comprobar');
    try {
      const u = await refrescar();
      if (u.telegramVinculado) {
        setCodigo(null);
        avisar({ tipo: 'exito', texto: TELEGRAM_VINCULADO });
      } else {
        avisar({ tipo: 'aviso', texto: TELEGRAM_PENDIENTE });
      }
    } catch (e) {
      avisar({ tipo: 'error', texto: mensajeDeError(e) });
    } finally {
      setOcupado(null);
    }
  }

  function cerrarModal() {
    setConfirmando(false);
    document.getElementById(idBotonDesvincular)?.focus();
  }

  async function ejecutarDesvincular() {
    setOcupado('desvincular');
    try {
      await desvincularTelegram();
      try {
        await refrescar();
      } catch {
        // ignorado deliberadamente
      }
      setConfirmando(false);
      avisar({ tipo: 'exito', texto: TELEGRAM_DESVINCULADO });
    } catch (e) {
      setConfirmando(false);
      avisar({ tipo: 'error', texto: mensajeErrorPerfil(e, []) });
    } finally {
      setOcupado(null);
    }
  }

  return (
    <>
      <Tarjeta titulo="Telegram">
        {vinculado ? (
          <>
            <div>
              <Insignia tono="exito">Vinculado</Insignia>
            </div>
            <p>{TELEGRAM_ACTIVO}</p>
            <div>
              <Boton id={idBotonDesvincular} variante="secundario" onClick={() => setConfirmando(true)}>
                Desvincular
              </Boton>
            </div>
          </>
        ) : (
          <>
            <div>
              <Insignia tono="aviso">No vinculado</Insignia>
            </div>
            <Alerta tipo="aviso">{TELEGRAM_OBLIGATORIO}</Alerta>
            <div>
              <Boton
                variante="primario"
                onClick={() => void pedirCodigo()}
                cargando={ocupado === 'codigo'}
                disabled={ocupado !== null}
              >
                {codigo !== null ? 'Generar otro código' : 'Vincular Telegram'}
              </Boton>
            </div>
            {codigo !== null && (
              <>
                <ol className="pasos-telegram">
                  <li>
                    <div className="paso-telegram">
                      <span>Abre el bot de UniProject en Telegram.</span>
                      {esEnlaceTelegram(codigo.enlace) && (
                        <a
                          className="boton boton--secundario"
                          href={codigo.enlace}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          Abrir Telegram
                        </a>
                      )}
                    </div>
                  </li>
                  <li>
                    <div className="paso-telegram">
                      <span>Pulsa «Iniciar». Si el bot no responde, envíale este mensaje:</span>
                      <code className="codigo-telegram">{`/start ${codigo.codigo}`}</code>
                    </div>
                  </li>
                  <li>
                    <div className="paso-telegram">
                      <span>Cuando el bot te confirme la vinculación, vuelve aquí.</span>
                      <Boton
                        variante="secundario"
                        onClick={() => void comprobarVinculacion()}
                        cargando={ocupado === 'comprobar'}
                        disabled={ocupado !== null}
                      >
                        Ya lo vinculé
                      </Boton>
                    </div>
                  </li>
                </ol>
                <p className="texto-ayuda">{textoVence(codigo.venceEn)}</p>
              </>
            )}
          </>
        )}
      </Tarjeta>
      {confirmando && (
        <Modal
          ancho="dialogo"
          titulo="¿Desvincular Telegram?"
          tonoTitulo="peligro"
          ocupado={ocupado === 'desvincular'}
          alCerrar={cerrarModal}
          alEnviar={() => void ejecutarDesvincular()}
          acciones={(cerrar) => (
            <>
              <Boton variante="secundario" onClick={cerrar} disabled={ocupado === 'desvincular'}>
                Cancelar
              </Boton>
              <Boton variante="peligro" type="submit" cargando={ocupado === 'desvincular'}>
                Desvincular
              </Boton>
            </>
          )}
        >
          <p className="modal__texto">{TEXTO_DESVINCULAR}</p>
        </Modal>
      )}
    </>
  );
}
