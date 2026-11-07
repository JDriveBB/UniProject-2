import { useEffect, useId, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router';
import { detalleProyecto, listarProyectos } from '../api/proyectos';
import { tableroHito } from '../api/tareas';
import { historialEquipo } from '../api/trazabilidad';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { Tarjeta } from '../componentes/Tarjeta';
import { fechaIsoBogota } from '../formato';
import { cargarHistorial, type DatosHistorial } from '../historial/carga';
import {
  aplicarFiltros,
  filtrosDeBusqueda,
  opcionesAccion,
  rutaHistorial,
  type Filtros,
} from '../historial/filtros';
import { FiltrosHistorial } from '../historial/FiltrosHistorial';
import { LineaTiempoHistorial } from '../historial/LineaTiempoHistorial';
import {
  agruparPorDia,
  entradasVisibles,
  HISTORIAL_RECORTADO_EQUIPO,
  indiceEquipo,
  NOTAS_RESERVADAS,
  SIN_COINCIDENCIAS,
  SOLO_EQUIPO_Y_DOCENTE,
  TAMANO_PAGINA,
  TITULO_HISTORIAL,
  textoConteo,
  textoMostrarMas,
  type Visor,
} from '../historial/vista';
import { idDeRuta } from '../proyectos/vista';
import { LIMITE_HISTORIAL } from '../reportes/carga';
import { EXPLICACION_HISTORIAL, SIN_CAMBIOS_EQUIPO } from '../reportes/vista';
import { rutas } from '../rutas';
import { useUsuario } from '../sesion/ContextoSesion';
import { mensajeErrorAbrir, SIN_ACCESO_EQUIPO } from '../tablero/ubicacion';

type EstadoCarga =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'noPermitido' }
  | { estado: 'listo'; datos: DatosHistorial };

/** /equipos/:idEquipo/historial: una instancia por equipo (key). */
export function HistorialEquipoDeRuta() {
  const { idEquipo } = useParams();
  return <HistorialEquipo key={idEquipo} idRuta={idEquipo ?? ''} />;
}

export function HistorialEquipo({ idRuta }: { idRuta: string }) {
  const usuario = useUsuario();
  const navigate = useNavigate();
  const { search } = useLocation();
  const idEquipo = idDeRuta(idRuta);

  const [carga, setCarga] = useState<EstadoCarga>(() =>
    idEquipo === null ? { estado: 'error', mensaje: SIN_ACCESO_EQUIPO } : { estado: 'cargando' }
  );
  const [vuelta, setVuelta] = useState(0);
  const [cantidad, setCantidad] = useState(TAMANO_PAGINA);
  const [foco, setFoco] = useState<string | null>(null);
  const prefijoId = useId();

  useEffect(() => {
    if (idEquipo === null) {
      return;
    }
    let vigente = true;
    cargarHistorial({ listarProyectos, detalleProyecto, tableroHito, historialEquipo }, idEquipo, usuario.idUsuario)
      .then((resultado) => {
        if (!vigente) {
          return;
        }
        if (resultado.tipo === 'sinAcceso') {
          setCarga({ estado: 'error', mensaje: SIN_ACCESO_EQUIPO });
        } else if (resultado.tipo === 'noPermitido') {
          setCarga({ estado: 'noPermitido' });
        } else {
          setCarga({ estado: 'listo', datos: resultado.datos });
        }
      })
      .catch((e: unknown) => {
        if (vigente) {
          setCarga({ estado: 'error', mensaje: mensajeErrorAbrir(e, SIN_ACCESO_EQUIPO) });
        }
      });
    return () => {
      vigente = false;
    };
  }, [idEquipo, vuelta, usuario.idUsuario]);

  useEffect(() => {
    if (foco !== null) {
      document.getElementById(foco)?.focus();
      setFoco(null);
    }
  }, [foco]);

  function reintentar() {
    setCarga({ estado: 'cargando' });
    setVuelta((v) => v + 1);
  }

  function cambiar(f: Filtros) {
    setCantidad(TAMANO_PAGINA);
    if (idEquipo !== null) {
      navigate(rutaHistorial(idEquipo, f), { replace: true });
    }
  }

  if (carga.estado === 'cargando') {
    return <Cargando />;
  }

  if (carga.estado === 'error') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo={TITULO_HISTORIAL} />
        <Alerta tipo="error">{carga.mensaje}</Alerta>
        {carga.mensaje !== SIN_ACCESO_EQUIPO && (
          <div>
            <Boton variante="secundario" onClick={reintentar}>
              Reintentar
            </Boton>
          </div>
        )}
      </div>
    );
  }

  if (carga.estado === 'noPermitido') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo={TITULO_HISTORIAL} />
        <Alerta tipo="aviso">{SOLO_EQUIPO_Y_DOCENTE}</Alerta>
      </div>
    );
  }

  const { datos } = carga;
  const { equipo, proyecto } = datos.ubicado;

  const visor: Visor = {
    idUsuario: usuario.idUsuario,
    esDocente: datos.ubicado.proyecto.idDocente === usuario.idUsuario,
  };

  const indice = indiceEquipo(equipo, datos.hitos, datos.tableros);
  const visibles = entradasVisibles(datos.entradas, visor);
  const acciones = opcionesAccion(visibles);
  const filtros = filtrosDeBusqueda(search, acciones.map((o) => o.valor), datos.hitos);
  const coinciden = aplicarFiltros(visibles, filtros, indice.tareasPorHito);
  const mostradas = coinciden.slice(0, cantidad);
  const hoy = fechaIsoBogota(new Date());

  function mostrarMas() {
    const esa = coinciden[cantidad];
    setCantidad((c) => c + TAMANO_PAGINA);
    if (esa !== undefined) {
      setFoco(`${prefijoId}-${esa.idLog}`);
    }
  }

  return (
    <div className="pagina">
      <EncabezadoPagina
        titulo={TITULO_HISTORIAL}
        descripcion={`${equipo.nombreEquipo} · ${proyecto.nombre}`}
      >
        <div className="pagina__acciones">
          <Link className="boton boton--secundario" to={rutas.tableroEquipo(equipo.idEquipo)}>
            Volver al tablero
          </Link>
        </div>
      </EncabezadoPagina>

      <p className="texto-secundario">{EXPLICACION_HISTORIAL}</p>
      {!visor.esDocente && <p className="texto-secundario">{NOTAS_RESERVADAS}</p>}

      <FiltrosHistorial
        filtros={filtros}
        acciones={acciones}
        hitos={datos.hitos}
        alCambiar={cambiar}
      />

      <Tarjeta titulo="Línea de tiempo">
        {datos.entradas.length === LIMITE_HISTORIAL && (
          <p className="texto-ayuda">{HISTORIAL_RECORTADO_EQUIPO}</p>
        )}
        {visibles.length === 0 ? (
          <p className="pagina__vacio">{SIN_CAMBIOS_EQUIPO}</p>
        ) : coinciden.length === 0 ? (
          <p className="pagina__vacio">{SIN_COINCIDENCIAS}</p>
        ) : (
          <>
            <p className="texto-secundario" aria-live="polite">
              {textoConteo(mostradas.length, coinciden.length, visibles.length)}
            </p>
            <LineaTiempoHistorial
              grupos={agruparPorDia(mostradas)}
              hoy={hoy}
              indice={indice}
              visor={visor}
              prefijoId={prefijoId}
            />
            {coinciden.length > mostradas.length && (
              <div>
                <Boton
                  variante="secundario"
                  onClick={mostrarMas}
                >
                  {textoMostrarMas(coinciden.length - mostradas.length)}
                </Boton>
              </div>
            )}
          </>
        )}
      </Tarjeta>
    </div>
  );
}
