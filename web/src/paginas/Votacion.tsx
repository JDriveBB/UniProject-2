import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import { ErrorApi } from '../api/cliente';
import { detalleProyecto, listarProyectos } from '../api/proyectos';
import type { RondaVista, VotacionVista } from '../api/tipos';
import { abrirRonda, desempatarLider, vistaVotacion, votarLider } from '../api/votacion';
import { Alerta } from '../componentes/Alerta';
import { useAvisos } from '../componentes/Avisos';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { EstadoVacio } from '../componentes/EstadoVacio';
import { Insignia } from '../componentes/Insignia';
import { fechaHora } from '../formato';
import { mensajeErrorApi } from '../proyectos/errores';
import { idDeRuta } from '../proyectos/vista';
import { rutas } from '../rutas';
import { useUsuario } from '../sesion/ContextoSesion';
import { mensajeErrorAbrir, SIN_ACCESO_EQUIPO, ubicarEquipo } from '../tablero/ubicacion';
import {
  empatados,
  estadoRonda,
  puedeEnviarVoto,
  resumenRonda,
  textoBotonVoto,
  textoLiderActual,
  textoVotos,
} from '../votacion/vista';

type Carga =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; vista: VotacionVista };

interface Nombres {
  equipo: string;
  proyecto: string | null;
  idProyecto: number | null;
}

const CAMPOS_ACCION = [
  ['idCandidato', 'Candidato'],
  ['idUsuario', 'Integrante'],
] as const;

/** /equipos/:idEquipo/votacion: una instancia por equipo (key). Destino de los avisos del bloque 50. */
export function VotacionDeRuta() {
  const { idEquipo } = useParams();
  return <Votacion key={idEquipo} idRuta={idEquipo ?? ''} />;
}

export function Votacion({ idRuta }: { idRuta: string }) {
  const usuario = useUsuario();
  const avisar = useAvisos();
  const id = idDeRuta(idRuta);
  const [carga, setCarga] = useState<Carga>(() =>
    id === null ? { estado: 'error', mensaje: SIN_ACCESO_EQUIPO } : { estado: 'cargando' },
  );
  const [nombres, setNombres] = useState<Nombres>({ equipo: 'Equipo', proyecto: null, idProyecto: null });
  const [seleccion, setSeleccion] = useState<number | null | undefined>(undefined);
  const [desempate, setDesempate] = useState<number | null>(null);
  const [ocupado, setOcupado] = useState(false);
  const [mensaje, setMensaje] = useState<string | null>(null);

  const cargar = useCallback(async (idEquipo: number) => {
    try {
      setCarga({ estado: 'listo', vista: await vistaVotacion(idEquipo) });
    } catch (e) {
      setCarga({ estado: 'error', mensaje: mensajeErrorAbrir(e, SIN_ACCESO_EQUIPO) });
    }
  }, []);

  useEffect(() => {
    if (id === null) {
      return;
    }
    let vigente = true;
    void cargar(id);
    ubicarEquipo({ listarProyectos, detalleProyecto }, id)
      .then((u) => {
        if (vigente && u !== null) {
          setNombres({
            equipo: u.equipo.nombreEquipo,
            proyecto: u.proyecto.nombre,
            idProyecto: u.proyecto.idProyecto,
          });
        }
      })
      .catch(() => {
        // la página sigue con el nombre genérico
      });
    return () => {
      vigente = false;
    };
  }, [id, cargar]);

  if (carga.estado === 'cargando') {
    return <Cargando />;
  }
  if (carga.estado === 'error' || id === null) {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Votación de líder" />
        <Alerta tipo="error">{carga.estado === 'error' ? carga.mensaje : SIN_ACCESO_EQUIPO}</Alerta>
      </div>
    );
  }

  const idEquipo: number = id;
  const v = carga.vista;
  const ronda = v.ronda;
  const estado = estadoRonda(ronda);
  const resumen = resumenRonda(v);
  const esEstudiante = usuario.rol === 'ESTUDIANTE';
  const descripcion = nombres.proyecto === null ? nombres.equipo : `${nombres.equipo} · ${nombres.proyecto}`;

  /** Ejecuta una acción sobre la votación: guarda la vista nueva; tras un 409 recarga la actual. */
  async function ejecutar(accion: () => Promise<VotacionVista>, textoAviso: string) {
    setOcupado(true);
    setMensaje(null);
    try {
      setCarga({ estado: 'listo', vista: await accion() });
      setSeleccion(undefined);
      setDesempate(null);
      avisar(textoAviso);
    } catch (e) {
      setMensaje(mensajeErrorApi(e, CAMPOS_ACCION));
      if (e instanceof ErrorApi && e.estado === 409) {
        await cargar(idEquipo);
      }
    } finally {
      setOcupado(false);
    }
  }

  async function actualizar() {
    setOcupado(true);
    setMensaje(null);
    await cargar(idEquipo);
    setOcupado(false);
  }

  function votar(r: RondaVista) {
    const elegido = seleccion === undefined ? r.miVoto : seleccion;
    if (elegido !== null && puedeEnviarVoto(elegido, r.miVoto)) {
      void ejecutar(() => votarLider(idEquipo, elegido), 'Tu voto quedó registrado.');
    }
  }

  function elegirLider(r: RondaVista) {
    const candidato = empatados(r).find((c) => c.idUsuario === desempate);
    if (candidato !== undefined) {
      void ejecutar(
        () => desempatarLider(idEquipo, candidato.idUsuario),
        `${candidato.nombre} es el nuevo líder.`,
      );
    }
  }

  return (
    <div className="pagina">
      <EncabezadoPagina titulo="Votación de líder" descripcion={descripcion}>
        <div className="pagina__acciones">
          {esEstudiante ? (
            <Link className="boton boton--secundario" to={rutas.tableroEquipo(idEquipo)}>
              Ir al tablero
            </Link>
          ) : (
            nombres.idProyecto !== null && (
              <Link className="boton boton--secundario" to={rutas.proyecto(nombres.idProyecto)}>
                Volver al proyecto
              </Link>
            )
          )}
        </div>
      </EncabezadoPagina>

      <p>{textoLiderActual(v)}</p>
      {mensaje !== null && <Alerta tipo="error">{mensaje}</Alerta>}

      {estado === 'sin' && (
        <EstadoVacio titulo="Aún no hay votaciones">
          La votación se abre cuando cierra la inscripción a equipos.
        </EstadoVacio>
      )}

      {ronda !== null && estado === 'abierta' && (
        <>
          <Alerta tipo="info">
            {`Votación abierta hasta el ${fechaHora(ronda.cierraEn)}. Votaron ${ronda.votosEmitidos} de ${ronda.votantes}. El voto es secreto y puedes cambiarlo mientras siga abierta.`}
          </Alerta>
          {ronda.puedoVotar ? (
            <fieldset className="opciones-radio">
              <legend className="campo__etiqueta opciones-radio__titulo">Tu voto</legend>
              {ronda.candidatos.map((c) => (
                <label key={c.idUsuario} className="opcion-radio">
                  <input
                    type="radio"
                    className="opcion-radio__entrada"
                    name="voto-lider"
                    value={c.idUsuario}
                    checked={(seleccion === undefined ? ronda.miVoto : seleccion) === c.idUsuario}
                    disabled={ocupado}
                    onChange={() => setSeleccion(c.idUsuario)}
                  />
                  <span>{c.idUsuario === usuario.idUsuario ? `${c.nombre} (tú)` : c.nombre}</span>
                </label>
              ))}
              <div>
                <Boton
                  cargando={ocupado}
                  disabled={!puedeEnviarVoto(seleccion === undefined ? ronda.miVoto : seleccion, ronda.miVoto)}
                  onClick={() => votar(ronda)}
                >
                  {textoBotonVoto(ronda.miVoto)}
                </Boton>
              </div>
            </fieldset>
          ) : (
            <div className="pila">
              <ul className="lista-simple">
                {ronda.candidatos.map((c) => (
                  <li key={c.idUsuario}>{c.nombre}</li>
                ))}
              </ul>
              <p className="texto-secundario">Solo los integrantes del equipo votan.</p>
            </div>
          )}
        </>
      )}

      {ronda !== null && estado === 'vencida' && (
        <>
          <Alerta tipo="aviso">La votación terminó. El resultado aparecerá en unos minutos.</Alerta>
          <div>
            <Boton variante="secundario" disabled={ocupado} onClick={() => void actualizar()}>
              Actualizar
            </Boton>
          </div>
        </>
      )}

      {ronda !== null && estado === 'cerrada' && resumen !== null && (
        <div className="pila">
          <p>{resumen.texto}</p>
          {ronda.candidatos.some((c) => c.votos !== null) && (
            <ul className="lista-simple">
              {ronda.candidatos.map((c) => (
                <li key={c.idUsuario} className="equipo__miembro">
                  <span>{`${c.nombre}: ${textoVotos(c.votos)}`}</span>
                  {c.empatado && <Insignia tono="aviso">Empate</Insignia>}
                </li>
              ))}
            </ul>
          )}
          {v.puedeDesempatar && (
            <fieldset className="opciones-radio">
              <legend className="campo__etiqueta opciones-radio__titulo">Elige al líder entre los empatados</legend>
              {empatados(ronda).map((c) => (
                <label key={c.idUsuario} className="opcion-radio">
                  <input
                    type="radio"
                    className="opcion-radio__entrada"
                    name="desempate-lider"
                    value={c.idUsuario}
                    checked={desempate === c.idUsuario}
                    disabled={ocupado}
                    onChange={() => setDesempate(c.idUsuario)}
                  />
                  <span>{c.nombre}</span>
                </label>
              ))}
              <div>
                <Boton cargando={ocupado} disabled={desempate === null} onClick={() => elegirLider(ronda)}>
                  Elegir líder
                </Boton>
              </div>
            </fieldset>
          )}
        </div>
      )}

      {v.puedeAbrir && (
        <div>
          <Boton
            variante="secundario"
            disabled={ocupado}
            onClick={() => void ejecutar(() => abrirRonda(idEquipo), 'Se abrió la votación.')}
          >
            {estado === 'sin' ? 'Abrir votación' : 'Abrir otra votación'}
          </Boton>
        </div>
      )}
    </div>
  );
}
