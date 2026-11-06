import { useCallback, useEffect, useId, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router';
import { mensajeDeError } from '../api/cliente';
import { detalleProyecto, listarProyectos } from '../api/proyectos';
import { enviarHito, tableroHito } from '../api/tareas';
import type { TableroHito } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { ConfirmarQuitar } from '../componentes/ConfirmarQuitar';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import type { Mensaje } from '../proyectos/errores';
import { idDeRuta } from '../proyectos/vista';
import { rutas } from '../rutas';
import { useUsuario } from '../sesion/ContextoSesion';
import { LeyendaMarcas } from '../tablero/LeyendaMarcas';
import { codigosDeTareas } from '../tablero/marcas';
import { ModalNuevaTarea } from '../tablero/ModalNuevaTarea';
import { PestanasHitos } from '../tablero/PestanasHitos';
import { ResumenHito } from '../tablero/ResumenHito';
import { TablaTareas } from '../tablero/TablaTareas';
import {
  etiquetaHito,
  hitoElegido,
  hitosDelEquipo,
  mensajeErrorAbrir,
  rolEnEquipo,
  SIN_ACCESO_EQUIPO,
  ubicarEquipo,
} from '../tablero/ubicacion';
import type { ApiUbicacion, EquipoUbicado, HitoDelEquipo } from '../tablero/ubicacion';

const API_UBICACION: ApiUbicacion = { listarProyectos, detalleProyecto };

type Ubicacion =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; equipo: EquipoUbicado; hitos: HitoDelEquipo[] };

/** Tablero de un hito, guardado con su id: así no se muestra el de otro hito mientras carga el nuevo. */
interface Contenido {
  idHitoEquipo: number;
  tablero: TableroHito | null;
  error: string | null;
}

/** /equipos/:idEquipo/tablero: una instancia por equipo (key). */
export function TableroDeRuta() {
  const { idEquipo } = useParams();
  return <TableroEquipo key={idEquipo} idRuta={idEquipo ?? ''} />;
}

/**
 * P04 Tablero del equipo (Figma nodo 6:2) con P05 Nueva tarea como modal (nodo 6:200). Hito por ?hito= o el actual
 * (26c-2). Botones según la nota de Figma (26c-7): «Nueva tarea» solo el líder y «Ver ruta crítica» cualquier integrante (decisión 26c-1); «Enviar a
 * revisión» cualquier integrante; docente y monitor, solo lectura. El servidor responde 403 de todas formas.
 */
function TableroEquipo({ idRuta }: { idRuta: string }) {
  const usuario = useUsuario();
  const { search } = useLocation();
  const idEquipo = idDeRuta(idRuta);
  const [ubicacion, setUbicacion] = useState<Ubicacion>(() =>
    idEquipo === null ? { estado: 'error', mensaje: SIN_ACCESO_EQUIPO } : { estado: 'cargando' },
  );
  const [contenido, setContenido] = useState<Contenido | null>(null);
  const [vuelta, setVuelta] = useState(0);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const [modalAbierto, setModalAbierto] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [foco, setFoco] = useState<string | null>(null);
  const idNueva = useId();
  const idEnviar = useId();
  const idMensaje = useId();

  const ubicar = useCallback(async (id: number) => {
    try {
      const u = await ubicarEquipo(API_UBICACION, id);
      setUbicacion(u === null
        ? { estado: 'error', mensaje: SIN_ACCESO_EQUIPO }
        : { estado: 'listo', equipo: u, hitos: hitosDelEquipo(u) });
    } catch (e) {
      setUbicacion({ estado: 'error', mensaje: mensajeErrorAbrir(e, SIN_ACCESO_EQUIPO) });
    }
  }, []);

  useEffect(() => {
    if (idEquipo !== null) {
      void ubicar(idEquipo);
    }
  }, [ubicar, idEquipo]);

  const hito = ubicacion.estado === 'listo'
    ? hitoElegido(ubicacion.hitos, new URLSearchParams(search).get('hito'))
    : null;
  const idHitoEquipo = hito === null ? null : hito.idHitoEquipo;

  useEffect(() => {
    if (idHitoEquipo === null) {
      return;
    }
    let vigente = true;
    tableroHito(idHitoEquipo)
      .then((tablero) => {
        if (vigente) {
          setContenido({ idHitoEquipo, tablero, error: null });
        }
      })
      .catch((e: unknown) => {
        if (vigente) {
          setContenido({ idHitoEquipo, tablero: null, error: mensajeErrorAbrir(e, SIN_ACCESO_EQUIPO) });
        }
      });
    return () => {
      vigente = false;
    };
  }, [idHitoEquipo, vuelta]);

  useEffect(() => {
    if (foco !== null) {
      (document.getElementById(foco) ?? document.getElementById(idMensaje))?.focus();
      setFoco(null);
    }
  }, [foco, idMensaje]);

  if (ubicacion.estado === 'cargando') {
    return <Cargando />;
  }
  if (ubicacion.estado === 'error') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Tablero del equipo" />
        <Alerta tipo="error">{ubicacion.mensaje}</Alerta>
      </div>
    );
  }
  const { equipo: ubicado, hitos } = ubicacion;
  const descripcion = `${ubicado.equipo.nombreEquipo} · ${ubicado.proyecto.nombre}`;
  if (hito === null) {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Tablero del equipo" descripcion={descripcion} />
        <p className="pagina__vacio">El proyecto aún no tiene hitos.</p>
      </div>
    );
  }
  const rol = rolEnEquipo(ubicado.equipo, usuario.idUsuario);
  const actual = contenido !== null && contenido.idHitoEquipo === hito.idHitoEquipo ? contenido : null;
  const tablero = actual === null ? null : actual.tablero;
  const editable = tablero !== null && tablero.editable;
  const verRuta = rol !== null;
  const puedeCrear = rol === 'LIDER' && editable;
  const puedeEnviar = rol !== null && editable;
  const codigos = tablero === null ? new Map<number, string>() : codigosDeTareas(tablero.tareas);

  function reintentar() {
    setContenido(null);
    setVuelta((v) => v + 1);
  }

  function elegirHito() {
    setMensaje(null);
    setConfirmando(false);
  }

  async function enviar(idHito: number) {
    setEnviando(true);
    try {
      await enviarHito(idHito);
      setMensaje({ tipo: 'exito', texto: 'Hito enviado a revisión.' });
      if (idEquipo !== null) {
        void ubicar(idEquipo);
      }
      setVuelta((v) => v + 1);
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeDeError(e) });
    }
    setEnviando(false);
    setConfirmando(false);
    setFoco(idMensaje);
  }

  return (
    <div className="pagina">
      <EncabezadoPagina titulo="Tablero del equipo" descripcion={descripcion}>
        {(verRuta || puedeCrear || puedeEnviar) && (
          <div className="pagina__acciones">
            {verRuta && (
              <Link className="boton boton--secundario" to={rutas.rutaCritica(hito.idHitoEquipo)}>
                Ver ruta crítica
              </Link>
            )}
            {puedeCrear && (
              <Boton
                id={idNueva}
                variante="secundario"
                disabled={confirmando || enviando}
                onClick={() => setModalAbierto(true)}
              >
                Nueva tarea
              </Boton>
            )}
            {puedeEnviar && (
              <Boton
                id={idEnviar}
                disabled={confirmando || enviando}
                onClick={() => {
                  setMensaje(null);
                  setConfirmando(true);
                }}
              >
                Enviar a revisión
              </Boton>
            )}
          </div>
        )}
      </EncabezadoPagina>
      {mensaje !== null && (
        <div id={idMensaje} tabIndex={-1}>
          <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>
        </div>
      )}
      {confirmando && (
        <ConfirmarQuitar
          pregunta={`¿Enviar «${etiquetaHito(hito.orden, hito.nombreHito)}» a revisión? Mientras esté en revisión, sus tareas no se podrán cambiar.`}
          textoConfirmar="Sí, enviar"
          textoOcupado="Enviando…"
          ocupado={enviando}
          alConfirmar={() => void enviar(hito.idHitoEquipo)}
          alCancelar={() => {
            setConfirmando(false);
            setFoco(idEnviar);
          }}
        />
      )}
      <PestanasHitos idEquipo={ubicado.equipo.idEquipo} hitos={hitos} activo={hito.idHitoEquipo} alElegir={elegirHito} />
      {actual === null && <Cargando />}
      {actual !== null && actual.error !== null && (
        <div className="pila">
          <Alerta tipo="error">{actual.error}</Alerta>
          <div>
            <Boton variante="secundario" onClick={reintentar}>
              Reintentar
            </Boton>
          </div>
        </div>
      )}
      {tablero !== null && (
        <>
          <ResumenHito tablero={tablero} />
          <LeyendaMarcas />
          <TablaTareas tareas={tablero.tareas} codigos={codigos} puedeCrear={puedeCrear} />
        </>
      )}
      {modalAbierto && tablero !== null && (
        <ModalNuevaTarea
          tablero={tablero}
          hito={hito}
          miembros={ubicado.equipo.miembros}
          codigos={codigos}
          alCerrar={() => {
            setModalAbierto(false);
            setFoco(idNueva);
          }}
          alCrear={(m) => {
            setModalAbierto(false);
            setMensaje(m);
            setVuelta((v) => v + 1);
            setFoco(idMensaje);
          }}
        />
      )}
    </div>
  );
}
