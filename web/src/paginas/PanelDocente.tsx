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
import { EstadoVacio } from '../componentes/EstadoVacio';
import { fechaIsoBogota } from '../formato';
import { cargarPanel, type DatosPanel } from '../panel/carga';
import {
  atencionGeneral,
  cargarPanelesPropios,
  proyectosDelPanel,
  resumenGeneral,
  ultimaActualizacion,
  type PanelDeProyecto,
} from '../panel/general';
import { ResumenPanel } from '../panel/ResumenPanel';
import { TarjetaAlertas } from '../panel/TarjetaAlertas';
import { TarjetaAtencion } from '../panel/TarjetaAtencion';
import { TarjetaEntregas } from '../panel/TarjetaEntregas';
import { TarjetaEquiposPanel } from '../panel/TarjetaEquiposPanel';
import { TarjetaProyectosPanel } from '../panel/TarjetaProyectosPanel';
import {
  alertasPorEquipo,
  atencionDelDia,
  entregasDelProyecto,
  estadoBotonPanel,
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
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'unico'; idProyecto: number }
  | { estado: 'vacio' }
  | { estado: 'listo'; paneles: PanelDeProyecto[] };

export function ElegirPanel() {
  const usuario = useUsuario();
  const [estado, setEstado] = useState<EstadoElegir>({ estado: 'cargando' });
  const [vuelta, setVuelta] = useState(0);
  const [recargando, setRecargando] = useState(false);

  useEffect(() => {
    let vigente = true;
    async function cargar() {
      let lista: ProyectoVista[];
      try {
        lista = await listarProyectos();
      } catch (e) {
        if (vigente) {
          setEstado({ estado: 'error', mensaje: mensajeDeError(e) });
          setRecargando(false);
        }
        return;
      }
      if (!vigente) {
        return;
      }
      const propios = proyectosPropios(lista, usuario.idUsuario);
      if (propios.length === 1) {
        setEstado({ estado: 'unico', idProyecto: propios[0].idProyecto });
        setRecargando(false);
        return;
      }
      if (propios.length === 0) {
        setEstado({ estado: 'vacio' });
        setRecargando(false);
        return;
      }

      const paneles = await cargarPanelesPropios({ panelProyecto, detalleProyecto }, propios);
      if (!vigente) {
        return;
      }
      if (paneles.every((p) => p.datos === null)) {
        setEstado({ estado: 'error', mensaje: 'No se pudieron cargar tus proyectos.' });
      } else {
        setEstado({ estado: 'listo', paneles });
      }
      setRecargando(false);
    }
    void cargar();
    return () => {
      vigente = false;
    };
  }, [usuario.idUsuario, vuelta]);

  function actualizar() {
    if (recargando) {
      return;
    }
    setRecargando(true);
    setVuelta((v) => v + 1);
  }

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

  if (estado.estado === 'unico') {
    return <Navigate to={rutas.panelProyecto(estado.idProyecto)} replace />;
  }

  if (estado.estado === 'vacio') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Panel del docente" />
        <EstadoVacio
          titulo="Aún no tienes proyectos"
          accion={
            <Link className="boton boton--primario" to={rutas.proyectoNuevo()}>
              Nuevo proyecto
            </Link>
          }
        >
          Crea el primero con «Nuevo proyecto».
        </EstadoVacio>
      </div>
    );
  }

  const { paneles } = estado;
  const hoy = fechaIsoBogota(new Date());
  const ultima = ultimaActualizacion(paneles);
  const descripcion =
    ultima === null ? `${paneles.length} proyectos` : `${paneles.length} proyectos · ${textoActualizado(ultima)}`;
  const fallidos = paneles.filter((p) => p.datos === null).map((p) => p.proyecto.nombre);
  const botonActualizar = estadoBotonPanel('actualizar', recargando);

  return (
    <div className="pagina">
      <EncabezadoPagina titulo="Panel del docente" descripcion={descripcion}>
        <div className="pagina__acciones">
          <Boton variante="secundario" disabled={botonActualizar.deshabilitado} onClick={actualizar}>
            {botonActualizar.texto}
          </Boton>
        </div>
      </EncabezadoPagina>
      {fallidos.length > 0 && (
        <Alerta tipo="aviso">{`No se pudieron cargar los datos de ${fallidos.join(', ')}. Lo demás está al día.`}</Alerta>
      )}
      <ResumenPanel resumen={resumenGeneral(paneles, hoy)} />
      <div className="detalle-tarea">
        <div className="detalle-tarea__columna">
          <div id="atencion">
            <TarjetaAtencion elementos={atencionGeneral(paneles, hoy)} />
          </div>
        </div>
        <div className="detalle-tarea__columna">
          <TarjetaProyectosPanel proyectos={proyectosDelPanel(paneles, hoy)} />
        </div>
      </div>
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
  const atenciones = atencionDelDia(panel, detalle, hoy);
  const entregas = entregasDelProyecto(detalle, hoy);
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
      <ResumenPanel resumen={resumenPanel(panel, pendientes.length, entregas)} />
      <div className="detalle-tarea">
        <div className="detalle-tarea__columna">
          <div id="atencion">
            <TarjetaAtencion elementos={atenciones} />
          </div>
          <div id="equipos">
            <TarjetaEquiposPanel equipos={panel.equipos} />
          </div>
        </div>
        <div className="detalle-tarea__columna">
          <TarjetaEntregas entregas={entregas} />
          <TarjetaAlertas alertas={alertasPorEquipo(panel)} />
        </div>
      </div>
    </div>
  );
}
