import { useEffect, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router';
import { mensajeDeError } from '../api/cliente';
import { panelProyecto } from '../api/panel';
import { detalleProyecto, listarProyectos } from '../api/proyectos';
import type { ProyectoVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { fechaIsoBogota, rangoFechas } from '../formato';
import { cargarPanel, type DatosPanel } from '../panel/carga';
import { ResumenPanel } from '../panel/ResumenPanel';
import { TarjetaAlertas } from '../panel/TarjetaAlertas';
import { TarjetaEquiposPanel } from '../panel/TarjetaEquiposPanel';
import { TarjetaHitosPanel } from '../panel/TarjetaHitosPanel';
import { TarjetaPorEvaluar } from '../panel/TarjetaPorEvaluar';
import {
  alertasPorEquipo,
  estadoBotonPanel,
  matrizHitos,
  porEvaluar,
  proyectosPropios,
  resumenPanel,
  SIN_ACCESO_PANEL,
  textoActualizado,
} from '../panel/vista';
import { idDeRuta } from '../proyectos/vista';
import { rutas } from '../rutas';
import { useUsuario } from '../sesion/ContextoSesion';
import { mensajeErrorAbrir } from '../tablero/ubicacion';

type EstadoElegir =
  { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'listo'; proyectos: ProyectoVista[] };

export function ElegirPanel() {
  const usuario = useUsuario();
  const [estado, setEstado] = useState<EstadoElegir>({ estado: 'cargando' });
  const [vuelta, setVuelta] = useState(0);

  useEffect(() => {
    let vigente = true;
    listarProyectos()
      .then((lista) => {
        if (vigente) {
          setEstado({ estado: 'listo', proyectos: proyectosPropios(lista, usuario.idUsuario) });
        }
      })
      .catch((e: unknown) => {
        if (vigente) {
          setEstado({ estado: 'error', mensaje: mensajeDeError(e) });
        }
      });
    return () => {
      vigente = false;
    };
  }, [usuario.idUsuario, vuelta]);

  function reintentar() {
    setEstado({ estado: 'cargando' });
    setVuelta((v) => v + 1);
  }

  if (estado.estado === 'cargando') {
    return <Cargando />;
  }

  if (estado.estado === 'error') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Panel del docente" />
        <div className="pila">
          <Alerta tipo="error">{estado.mensaje}</Alerta>
          <div>
            <Boton variante="secundario" onClick={reintentar}>
              Reintentar
            </Boton>
          </div>
        </div>
      </div>
    );
  }

  const { proyectos } = estado;
  if (proyectos.length === 1) {
    return <Navigate to={rutas.panelProyecto(proyectos[0].idProyecto)} replace />;
  }

  if (proyectos.length === 0) {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Panel del docente" />
        <p className="pagina__vacio">Aún no tienes proyectos. Crea el primero con «Nuevo proyecto».</p>
        <div className="pagina__acciones">
          <Link className="boton boton--primario" to={rutas.proyectoNuevo()}>
            Nuevo proyecto
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="pagina">
      <EncabezadoPagina titulo="Panel del docente" descripcion="Tienes varios proyectos. Elige uno." />
      <ul className="lista-simple">
        {proyectos.map((p) => (
          <li key={p.idProyecto}>
            <Link className="enlace-fuerte" to={rutas.panelProyecto(p.idProyecto)}>
              {p.nombre}
            </Link>
            <span className="texto-secundario">{` · ${rangoFechas(p.fechaInicio, p.fechaFin)}`}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function PanelProyectoDeRuta() {
  const { idProyecto } = useParams();
  return <PanelProyecto key={idProyecto} idRuta={idProyecto ?? ''} />;
}

type EstadoCarga =
  { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'listo'; datos: DatosPanel };

export function PanelProyecto({ idRuta }: { idRuta: string }) {
  const id = idDeRuta(idRuta);

  const [carga, setCarga] = useState<EstadoCarga>(() =>
    id === null ? { estado: 'error', mensaje: SIN_ACCESO_PANEL } : { estado: 'cargando' },
  );
  const [vuelta, setVuelta] = useState(0);
  const [recargando, setRecargando] = useState(false);

  useEffect(() => {
    if (id === null) {
      return;
    }
    let vigente = true;
    cargarPanel({ panelProyecto, detalleProyecto }, id)
      .then((datos) => {
        if (vigente) {
          setCarga({ estado: 'listo', datos });
        }
      })
      .catch((e: unknown) => {
        if (vigente) {
          setCarga({ estado: 'error', mensaje: mensajeErrorAbrir(e, SIN_ACCESO_PANEL) });
        }
      })
      .finally(() => {
        if (vigente) {
          setRecargando(false);
        }
      });
    return () => {
      vigente = false;
    };
  }, [id, vuelta]);

  function actualizar() {
    if (recargando) {
      return;
    }
    setRecargando(true);
    setVuelta((v) => v + 1);
  }

  function reintentar() {
    if (recargando) {
      return;
    }
    setRecargando(true);
    setVuelta((v) => v + 1);
  }

  if (carga.estado === 'cargando') {
    return <Cargando />;
  }

  if (carga.estado === 'error') {
    const boton = estadoBotonPanel('reintentar', recargando);
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Panel del docente" />
        <div className="pila">
          <Alerta tipo="error">{carga.mensaje}</Alerta>
          {carga.mensaje !== SIN_ACCESO_PANEL && (
            <div>
              <Boton variante="secundario" disabled={boton.deshabilitado} onClick={reintentar}>
                {boton.texto}
              </Boton>
            </div>
          )}
        </div>
      </div>
    );
  }

  const { panel, detalle } = carga.datos;
  const hoy = fechaIsoBogota(new Date());
  const pendientes = porEvaluar(detalle);
  const botonActualizar = estadoBotonPanel('actualizar', recargando);

  return (
    <div className="pagina">
      <EncabezadoPagina
        titulo="Panel del docente"
        descripcion={`${panel.nombre} · ${textoActualizado(panel.generadoEn)}`}
      >
        <div className="pagina__acciones">
          <Boton variante="secundario" disabled={botonActualizar.deshabilitado} onClick={actualizar}>
            {botonActualizar.texto}
          </Boton>
          <Link className="boton boton--secundario" to={rutas.proyecto(panel.idProyecto)}>
            Configurar proyecto
          </Link>
        </div>
      </EncabezadoPagina>
      <ResumenPanel resumen={resumenPanel(panel, pendientes.length)} />
      <div className="detalle-tarea">
        <div className="detalle-tarea__columna">
          <div id="equipos">
            <TarjetaEquiposPanel equipos={panel.equipos} />
          </div>
          <TarjetaHitosPanel matriz={matrizHitos(detalle, hoy)} />
        </div>
        <div className="detalle-tarea__columna">
          <div id="por-evaluar">
            <TarjetaPorEvaluar pendientes={pendientes} />
          </div>
          <TarjetaAlertas alertas={alertasPorEquipo(panel)} />
        </div>
      </div>
    </div>
  );
}
