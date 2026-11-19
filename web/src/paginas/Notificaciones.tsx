import { useEffect, useId, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { ErrorApi, mensajeDeError } from '../api/cliente';
import { avisarCambioBandeja, bandeja, marcarLeida, marcarTodasLeidas } from '../api/notificaciones';
import type { Bandeja, NotificacionVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { Insignia } from '../componentes/Insignia';
import { fechaHora } from '../formato';
import {
  debeMarcarAlAbrir,
  enlaceInterno,
  LIMITE_BANDEJA,
  marcarEnBandeja,
  marcarTodasEnBandeja,
  marcaTipo,
  NO_DISPONIBLE,
  NUEVA,
  SIN_NOTIFICACIONES,
  SOLO_BANDEJA,
  VINCULAR_TELEGRAM,
  TEXTO_LIMITE,
  textoNoLeidas,
  TODAS_LEIDAS,
} from '../notificaciones/vista';
import type { Mensaje } from '../proyectos/errores';

type EstadoCarga =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; datos: Bandeja };

export function Notificaciones() {
  const navegar = useNavigate();
  const [carga, setCarga] = useState<EstadoCarga>({ estado: 'cargando' });
  const [vuelta, setVuelta] = useState(0);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const [foco, setFoco] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState<number | 'todas' | null>(null);

  const idAviso = useId();
  const prefijoFila = useId();

  useEffect(() => {
    let vigente = true;
    bandeja()
      .then((b) => {
        if (vigente) {
          setCarga({ estado: 'listo', datos: b });
        }
      })
      .catch((e: unknown) => {
        if (vigente) {
          setCarga({ estado: 'error', mensaje: mensajeDeError(e) });
        }
      });
    return () => {
      vigente = false;
    };
  }, [vuelta]);

  useEffect(() => {
    if (foco !== null) {
      (document.getElementById(foco) ?? document.getElementById(idAviso))?.focus();
      setFoco(null);
    }
  }, [foco, idAviso]);

  async function abrir(n: NotificacionVista, destino: string) {
    if (ocupado !== null) {
      return;
    }
    if (debeMarcarAlAbrir(n)) {
      setOcupado(n.idNotificacion);
      try {
        await marcarLeida(n.idNotificacion);
      } catch {
        // si falla, se abre igual: el aviso sigue sin leer y se puede marcar después
      }
      avisarCambioBandeja();
    }
    navegar(destino);
  }

  async function marcarUna(n: NotificacionVista) {
    setOcupado(n.idNotificacion);
    try {
      await marcarLeida(n.idNotificacion);
      setCarga((prev) =>
        prev.estado === 'listo'
          ? { estado: 'listo', datos: marcarEnBandeja(prev.datos, n.idNotificacion) }
          : prev
      );
      avisarCambioBandeja();
      setFoco(`${prefijoFila}-${n.idNotificacion}`);
    } catch (e: unknown) {
      if (e instanceof ErrorApi && e.estado === 403) {
        setMensaje({ tipo: 'error', texto: NO_DISPONIBLE });
        setVuelta((v) => v + 1);
      } else {
        setMensaje({ tipo: 'error', texto: mensajeDeError(e) });
      }
      setFoco(idAviso);
    } finally {
      setOcupado(null);
    }
  }

  async function marcarTodas() {
    setOcupado('todas');
    try {
      await marcarTodasLeidas();
      setCarga((prev) =>
        prev.estado === 'listo'
          ? { estado: 'listo', datos: marcarTodasEnBandeja(prev.datos) }
          : prev
      );
      avisarCambioBandeja();
      setMensaje({ tipo: 'exito', texto: TODAS_LEIDAS });
      setFoco(idAviso);
    } catch (e: unknown) {
      setMensaje({ tipo: 'error', texto: mensajeDeError(e) });
      setFoco(idAviso);
    } finally {
      setOcupado(null);
    }
  }

  if (carga.estado === 'cargando') {
    return <Cargando />;
  }

  if (carga.estado === 'error') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Notificaciones" />
        <Alerta tipo="error">{carga.mensaje}</Alerta>
        <div>
          <Boton
            variante="secundario"
            onClick={() => {
              setCarga({ estado: 'cargando' });
              setVuelta((v) => v + 1);
            }}
          >
            Reintentar
          </Boton>
        </div>
      </div>
    );
  }

  const { datos } = carga;

  return (
    <div className="pagina">
      <EncabezadoPagina titulo="Notificaciones" descripcion={textoNoLeidas(datos.noLeidas)}>
        <div className="pagina__acciones">
          <Boton
            variante="secundario"
            onClick={marcarTodas}
            disabled={datos.noLeidas === 0 || ocupado !== null}
            cargando={ocupado === 'todas'}
          >
            Marcar todas como leídas
          </Boton>
        </div>
      </EncabezadoPagina>

      {mensaje !== null && (
        <Alerta tipo={mensaje.tipo} id={idAviso} tabIndex={-1}>
          {mensaje.texto}
        </Alerta>
      )}

      {datos.notificaciones.length === 0 ? (
        <p className="pagina__vacio">{SIN_NOTIFICACIONES}</p>
      ) : (
        <>
          <ul className="lista-notificaciones" aria-label="Notificaciones">
            {datos.notificaciones.map((n) => {
              const marca = marcaTipo(n.tipo);
              const destino = enlaceInterno(n.enlace);
              const clasesFila = ['notificacion', !n.leida && 'notificacion--no-leida']
                .filter(Boolean)
                .join(' ');

              return (
                <li
                  key={n.idNotificacion}
                  id={`${prefijoFila}-${n.idNotificacion}`}
                  tabIndex={-1}
                  className={clasesFila}
                >
                  <div className="notificacion__cuerpo">
                    <div className="notificacion__meta">
                      <Insignia tono={marca.tono}>{marca.texto}</Insignia>
                      {!n.leida && <Insignia tono={NUEVA.tono}>{NUEVA.texto}</Insignia>}
                      <span>{fechaHora(n.fechaCreacion)}</span>
                    </div>
                    <p className="notificacion__mensaje">{n.mensaje}</p>
                    {n.estadoEnvio === 'SIN_TELEGRAM' && (
                      <p className="texto-ayuda">
                        {SOLO_BANDEJA} <Link to="/perfil">{VINCULAR_TELEGRAM}</Link>
                      </p>
                    )}
                  </div>
                  <div className="notificacion__acciones">
                    {destino !== null && (
                      <Link
                        to={destino}
                        className="boton boton--secundario"
                        aria-disabled={ocupado !== null}
                        tabIndex={ocupado !== null ? -1 : undefined}
                        onClick={(e) => {
                          if (ocupado !== null) {
                            e.preventDefault();
                          } else if (!e.ctrlKey && !e.metaKey && !e.shiftKey && !e.altKey && e.button === 0) {
                            e.preventDefault();
                            void abrir(n, destino);
                          }
                        }}
                      >
                        Abrir
                      </Link>
                    )}
                    {!n.leida && (
                      <button
                        type="button"
                        className="boton-texto"
                        disabled={ocupado !== null}
                        onClick={() => void marcarUna(n)}
                      >
                        Marcar como leída
                      </button>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
          {datos.notificaciones.length >= LIMITE_BANDEJA && (
            <p className="texto-ayuda">{TEXTO_LIMITE}</p>
          )}
        </>
      )}
    </div>
  );
}
