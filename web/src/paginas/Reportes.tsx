import { useEffect, useId, useState } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { mensajeDeError } from '../api/cliente';
import { consultarEvaluacion } from '../api/evaluacion';
import { detalleProyecto, listarProyectos } from '../api/proyectos';
import { tableroHito } from '../api/tareas';
import type { ProyectoDetalle, ProyectoVista } from '../api/tipos';
import { historialEquipo, reporteEquipo } from '../api/trazabilidad';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { Tarjeta } from '../componentes/Tarjeta';
import { fechaIsoBogota } from '../formato';
import { mensajeErrorCarga, type Mensaje } from '../proyectos/errores';
import { ResumenTrazabilidad } from '../reportes/ResumenTrazabilidad';
import { SelectorReporte } from '../reportes/SelectorReporte';
import { TablaHistorial } from '../reportes/TablaHistorial';
import { cargarTrazabilidad, LIMITE_HISTORIAL, type DatosTrazabilidad } from '../reportes/carga';
import {
  descargarReporte,
  guardarEnDescargas,
  mensajeErrorDescarga,
  nombreArchivoReporte,
  textoDescargado,
} from '../reportes/descarga';
import {
  elegirEquipo,
  elegirHito,
  elegirProyecto,
  proyectosDelDocente,
  rutaReportes,
  type Seleccion,
} from '../reportes/seleccion';
import {
  DESCRIPCION_REPORTES,
  EXPLICACION_HISTORIAL,
  filtrarPorHito,
  HISTORIAL_DEL_HITO,
  HISTORIAL_RECORTADO,
  resumenEquipo,
  resumenHito,
  SIN_CAMBIOS_EQUIPO,
  SIN_CAMBIOS_HITO,
  SIN_EQUIPOS,
  SIN_PROYECTOS,
} from '../reportes/vista';
import { useUsuario } from '../sesion/ContextoSesion';
import { etiquetaHito, hitosDelEquipo, mensajeErrorAbrir, SIN_ACCESO_EQUIPO } from '../tablero/ubicacion';

type EstadoProyectos =
  { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'listo'; lista: ProyectoVista[] };

interface Detalle {
  idProyecto: number;
  datos: ProyectoDetalle | null;
  error: string | null;
}

interface Contenido {
  idEquipo: number;
  idHitoEquipo: number | null;
  datos: DatosTrazabilidad | null;
  error: string | null;
}

export function Reportes() {
  const usuario = useUsuario();
  const navigate = useNavigate();
  const { search } = useLocation();

  const [proyectos, setProyectos] = useState<EstadoProyectos>({ estado: 'cargando' });
  const [detalle, setDetalle] = useState<Detalle | null>(null);
  const [contenido, setContenido] = useState<Contenido | null>(null);
  const [vuelta, setVuelta] = useState(0);
  const [vueltaEquipo, setVueltaEquipo] = useState(0);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const [foco, setFoco] = useState<string | null>(null);
  const [descargando, setDescargando] = useState(false);

  const idAviso = useId();
  const idAyuda = useId();
  const idTitulo = useId();

  const params = new URLSearchParams(search);
  const proyecto = proyectos.estado === 'listo' ? elegirProyecto(proyectos.lista, params.get('proyecto')) : null;
  const idProyecto = proyecto?.idProyecto ?? null;

  const datosProyecto =
    detalle !== null && idProyecto !== null && detalle.idProyecto === idProyecto ? detalle.datos : null;

  const equipo = datosProyecto !== null ? elegirEquipo(datosProyecto.equipos, params.get('equipo')) : null;
  const idEquipo = equipo?.idEquipo ?? null;

  const hitos =
    datosProyecto !== null && equipo !== null
      ? hitosDelEquipo({ proyecto: datosProyecto.proyecto, hitos: datosProyecto.hitos, equipo })
      : [];
  const hito = elegirHito(hitos, params.get('hito'));
  const idHitoEquipo = hito?.idHitoEquipo ?? null;

  const contenidoVigente =
    contenido !== null &&
    idEquipo !== null &&
    contenido.idEquipo === idEquipo &&
    contenido.idHitoEquipo === idHitoEquipo
      ? contenido
      : null;

  useEffect(() => {
    let vigente = true;
    listarProyectos()
      .then((lista) => {
        if (vigente) {
          setProyectos({
            estado: 'listo',
            lista: proyectosDelDocente(lista, usuario.idUsuario),
          });
        }
      })
      .catch((e: unknown) => {
        if (vigente) {
          setProyectos({ estado: 'error', mensaje: mensajeDeError(e) });
        }
      });
    return () => {
      vigente = false;
    };
  }, [usuario.idUsuario, vuelta]);

  useEffect(() => {
    if (idProyecto === null) {
      return;
    }
    let vigente = true;
    detalleProyecto(idProyecto)
      .then((datos) => {
        if (vigente) {
          setDetalle({ idProyecto, datos, error: null });
        }
      })
      .catch((e: unknown) => {
        if (vigente) {
          setDetalle({ idProyecto, datos: null, error: mensajeErrorCarga(e) });
        }
      });
    return () => {
      vigente = false;
    };
  }, [idProyecto, vuelta]);

  useEffect(() => {
    if (idEquipo === null) {
      return;
    }
    let vigente = true;
    cargarTrazabilidad({ historialEquipo, tableroHito, consultarEvaluacion }, idEquipo, idHitoEquipo)
      .then((datos) => {
        if (vigente) {
          setContenido({ idEquipo, idHitoEquipo, datos, error: null });
        }
      })
      .catch((e: unknown) => {
        if (vigente) {
          setContenido({
            idEquipo,
            idHitoEquipo,
            datos: null,
            error: mensajeErrorAbrir(e, SIN_ACCESO_EQUIPO),
          });
        }
      });
    return () => {
      vigente = false;
    };
  }, [idEquipo, idHitoEquipo, vuelta, vueltaEquipo]);

  useEffect(() => {
    if (foco !== null) {
      (document.getElementById(foco) ?? document.getElementById(idAviso))?.focus();
      setFoco(null);
    }
  }, [foco, idAviso]);

  function cambiar(sel: Seleccion) {
    setMensaje(null);
    navigate(rutaReportes(sel), { replace: true });
  }

  function reintentar() {
    setProyectos({ estado: 'cargando' });
    setDetalle(null);
    setContenido(null);
    setMensaje(null);
    setVuelta((v) => v + 1);
  }

  function descargar() {
    if (equipo === null || descargando) {
      return;
    }
    setDescargando(true);
    const nombre = nombreArchivoReporte(equipo.nombreEquipo, equipo.idEquipo, fechaIsoBogota(new Date()));
    descargarReporte({ reporteEquipo }, equipo.idEquipo, nombre, guardarEnDescargas)
      .then(() => {
        setMensaje({ tipo: 'exito', texto: textoDescargado(nombre) });
        setVueltaEquipo((v) => v + 1);
      })
      .catch((e: unknown) => {
        setMensaje({ tipo: 'error', texto: mensajeErrorDescarga(e) });
      })
      .finally(() => {
        setDescargando(false);
        setFoco(idAviso);
      });
  }

  if (proyectos.estado === 'cargando') {
    return <Cargando />;
  }

  if (proyectos.estado === 'error') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Reportes" />
        <Alerta tipo="error">{proyectos.mensaje}</Alerta>
        <div>
          <Boton variante="secundario" onClick={reintentar}>
            Reintentar
          </Boton>
        </div>
      </div>
    );
  }

  return (
    <div className="pagina">
      <EncabezadoPagina titulo="Reportes" descripcion={DESCRIPCION_REPORTES}>
        {equipo !== null && (
          <div className="pagina__acciones">
            <Boton
              variante="primario"
              cargando={descargando}
              disabled={descargando}
              aria-describedby={idAyuda}
              onClick={descargar}
            >
              Descargar PDF
            </Boton>
          </div>
        )}
      </EncabezadoPagina>

      <div id={idAviso} tabIndex={-1}>
        {mensaje !== null && <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>}
      </div>

      {proyectos.lista.length === 0 ? (
        <p className="pagina__vacio">{SIN_PROYECTOS}</p>
      ) : (
        <>
          {proyecto !== null && (
            <SelectorReporte
              proyectos={proyectos.lista}
              idProyecto={proyecto.idProyecto}
              equipos={datosProyecto?.equipos ?? null}
              idEquipo={idEquipo}
              hitos={hitos}
              idHitoEquipo={idHitoEquipo}
              deshabilitado={descargando}
              idAyuda={idAyuda}
              alCambiar={cambiar}
            />
          )}

          {datosProyecto === null ? (
            detalle !== null && idProyecto !== null && detalle.idProyecto === idProyecto && detalle.error !== null ? (
              <>
                <Alerta tipo="error">{detalle.error}</Alerta>
                <div>
                  <Boton variante="secundario" onClick={reintentar}>
                    Reintentar
                  </Boton>
                </div>
              </>
            ) : (
              <Cargando />
            )
          ) : datosProyecto.equipos.length === 0 ? (
            <p className="pagina__vacio">{SIN_EQUIPOS}</p>
          ) : equipo !== null ? (
            contenidoVigente === null ? (
              <Cargando />
            ) : contenidoVigente.error !== null ? (
              <>
                <Alerta tipo="error">{contenidoVigente.error}</Alerta>
                {contenidoVigente.error !== SIN_ACCESO_EQUIPO && (
                  <div>
                    <Boton variante="secundario" onClick={reintentar}>
                      Reintentar
                    </Boton>
                  </div>
                )}
              </>
            ) : contenidoVigente.datos !== null ? (
              <>
                <section className="pagina__seccion" aria-labelledby={idTitulo}>
                  <h2 className="pagina__subtitulo" id={idTitulo}>
                    {hito === null
                      ? `Resumen de ${equipo.nombreEquipo}`
                      : `${etiquetaHito(hito.orden, hito.nombreHito)} · ${equipo.nombreEquipo}`}
                  </h2>
                  <ResumenTrazabilidad
                    datos={
                      hito !== null && contenidoVigente.datos.tablero !== null
                        ? resumenHito(
                            contenidoVigente.datos.tablero,
                            contenidoVigente.datos.tablero.tareas,
                            contenidoVigente.datos.evaluacion,
                          )
                        : resumenEquipo(equipo, hitos, contenidoVigente.datos.historial)
                    }
                  />
                </section>

                <Tarjeta titulo="Historial de cambios">
                  <p className="texto-secundario">{EXPLICACION_HISTORIAL}</p>
                  {hito !== null && <p className="texto-secundario">{HISTORIAL_DEL_HITO}</p>}
                  {contenidoVigente.datos.historial.length === LIMITE_HISTORIAL && (
                    <p className="texto-ayuda">{HISTORIAL_RECORTADO}</p>
                  )}
                  <TablaHistorial
                    entradas={
                      hito !== null && contenidoVigente.datos.tablero !== null
                        ? filtrarPorHito(
                            contenidoVigente.datos.historial,
                            hito.idHitoEquipo,
                            contenidoVigente.datos.tablero.tareas,
                          )
                        : contenidoVigente.datos.historial
                    }
                    nombres={new Map(equipo.miembros.map((m) => [m.idUsuario, m.nombre] as const))}
                    visor={{ idUsuario: usuario.idUsuario, esDocente: true }}
                    vacio={hito !== null ? SIN_CAMBIOS_HITO : SIN_CAMBIOS_EQUIPO}
                  />
                </Tarjeta>
              </>
            ) : null
          ) : null}
        </>
      )}
    </div>
  );
}
