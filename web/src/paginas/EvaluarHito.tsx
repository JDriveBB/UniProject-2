import { useEffect, useId, useState } from 'react';
import { Link, useParams } from 'react-router';
import { consultarEvaluacion } from '../api/evaluacion';
import { detalleProyecto } from '../api/proyectos';
import { tableroHito } from '../api/tareas';
import type { EvaluacionVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { Insignia } from '../componentes/Insignia';
import { pesosCompletos } from '../evaluacion/calculo';
import {
  cargarEvaluacion,
  criteriosDelHito,
  etiquetaDeHito,
  type ApiCargaEvaluacion,
  type DatosEvaluacion,
} from '../evaluacion/carga';
import { CriteriosEvaluados } from '../evaluacion/CriteriosEvaluados';
import { FormularioEvaluacion } from '../evaluacion/FormularioEvaluacion';
import { ListaEntregas } from '../evaluacion/ListaEntregas';
import { ResumenEvaluacion } from '../evaluacion/ResumenEvaluacion';
import { TablaNotasIndividuales } from '../evaluacion/TablaNotasIndividuales';
import { TarjetaNotaGrupal } from '../evaluacion/TarjetaNotaGrupal';
import {
  AUN_NO_ENVIADO,
  modoDocente,
  RUBRICA_NO_LISTA,
  SIN_ACCESO_EVALUACION,
  textoCaracter,
  textoReevaluacion,
} from '../evaluacion/vista';
import { fechaDia } from '../formato';
import type { Mensaje } from '../proyectos/errores';
import { idDeRuta } from '../proyectos/vista';
import { rutas } from '../rutas';
import { useUsuario } from '../sesion/ContextoSesion';
import { estadoHito } from '../tablero/marcas';
import { mensajeErrorAbrir } from '../tablero/ubicacion';

type EstadoCarga =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; datos: DatosEvaluacion };

const API_EVALUACION: ApiCargaEvaluacion = {
  tableroHito,
  detalleProyecto,
  consultarEvaluacion,
};

export function EvaluarHitoDeRuta() {
  const { idHitoEquipo } = useParams();
  return <EvaluarHito key={idHitoEquipo} idRuta={idHitoEquipo ?? ''} />;
}

export function EvaluarHito({ idRuta }: { idRuta: string }) {
  const usuario = useUsuario();
  const idHitoEquipo = idDeRuta(idRuta);

  const [carga, setCarga] = useState<EstadoCarga>(() =>
    idHitoEquipo === null
      ? { estado: 'error', mensaje: SIN_ACCESO_EVALUACION }
      : { estado: 'cargando' }
  );
  const [vuelta, setVuelta] = useState(0);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const [foco, setFoco] = useState<string | null>(null);

  const idAviso = useId();

  useEffect(() => {
    if (idHitoEquipo === null) {
      return;
    }
    let vigente = true;
    cargarEvaluacion(API_EVALUACION, idHitoEquipo)
      .then((datos) => {
        if (vigente) {
          setCarga({ estado: 'listo', datos });
        }
      })
      .catch((e: unknown) => {
        if (vigente) {
          setCarga({ estado: 'error', mensaje: mensajeErrorAbrir(e, SIN_ACCESO_EVALUACION) });
        }
      });
    return () => {
      vigente = false;
    };
  }, [idHitoEquipo, vuelta]);

  useEffect(() => {
    if (foco !== null) {
      (document.getElementById(foco) ?? document.getElementById(idAviso))?.focus();
      setFoco(null);
    }
  }, [foco, idAviso]);

  function hecho(m: Mensaje, recargar: boolean) {
    setMensaje(m);
    setFoco(idAviso);
    if (recargar) {
      setVuelta((v) => v + 1);
    }
  }

  function actualizar(ev: EvaluacionVista, m: Mensaje) {
    setCarga((prev) =>
      prev.estado === 'listo'
        ? { estado: 'listo', datos: { ...prev.datos, evaluacion: ev } }
        : prev
    );
    setMensaje(m);
    setFoco(idAviso);
  }

  if (carga.estado === 'cargando') {
    return <Cargando />;
  }

  if (carga.estado === 'error') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Evaluar hito" />
        <Alerta tipo="error">{carga.mensaje}</Alerta>
        {carga.mensaje !== SIN_ACCESO_EVALUACION && (
          <div>
            <Boton variante="secundario" onClick={() => setVuelta((v) => v + 1)}>
              Reintentar
            </Boton>
          </div>
        )}
      </div>
    );
  }

  const datos = carga.datos;
  const modo = modoDocente(datos, usuario.idUsuario);
  const criterios = criteriosDelHito(datos);

  if (modo === 'sinAcceso') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Evaluar hito" />
        <Alerta tipo="error">{SIN_ACCESO_EVALUACION}</Alerta>
      </div>
    );
  }

  return (
    <div className="pagina">
      <EncabezadoPagina
        titulo="Evaluar hito"
        descripcion={`${etiquetaDeHito(datos)} · ${datos.tablero.nombreEquipo} · ${datos.tablero.nombreProyecto}`}
      >
        <div className="pagina__acciones">
          <Link
            to={rutas.tableroEnHito(datos.tablero.idEquipo, datos.tablero.idHitoEquipo)}
            className="boton boton--secundario"
          >
            Volver al tablero
          </Link>
        </div>
      </EncabezadoPagina>

      <div className="insignias-tarea">
        <Insignia tono={estadoHito(datos.tablero.estado).tono}>
          {estadoHito(datos.tablero.estado).texto}
        </Insignia>
        <span className="texto-secundario">
          {`Fecha límite: ${fechaDia(datos.tablero.fechaLimite)}`}
        </span>
      </div>

      <div id={idAviso} tabIndex={-1}>
        {mensaje !== null && <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>}
      </div>

      {modo === 'noEnviado' && (
        <>
          <Alerta tipo="info">{AUN_NO_ENVIADO}</Alerta>
          <ListaEntregas tareas={datos.tablero.tareas} />
        </>
      )}

      {modo === 'evaluar' && (
        <>
          {datos.evaluacion !== null && (
            <Alerta tipo="info">{textoReevaluacion(datos.evaluacion)}</Alerta>
          )}
          {!pesosCompletos(criterios) ? (
            <>
              <Alerta tipo="aviso">{RUBRICA_NO_LISTA}</Alerta>
              <div>
                <Link
                  to={rutas.proyecto(datos.tablero.idProyecto)}
                  className="boton boton--secundario"
                >
                  Configurar proyecto
                </Link>
              </div>
              <ListaEntregas tareas={datos.tablero.tareas} />
            </>
          ) : (
            <div className="detalle-tarea">
              <FormularioEvaluacion
                idHitoEquipo={datos.tablero.idHitoEquipo}
                criterios={criterios}
                anterior={datos.evaluacion}
                fechaLimite={datos.tablero.fechaLimite}
                alHecho={hecho}
              />
              <div className="detalle-tarea__columna">
                <ListaEntregas tareas={datos.tablero.tareas} />
              </div>
            </div>
          )}
        </>
      )}

      {modo === 'evaluado' && datos.evaluacion !== null && (
        <>
          <ResumenEvaluacion
            evaluacion={datos.evaluacion}
            vista="docente"
            notaPropia={null}
          />
          <Alerta tipo="info">
            {textoCaracter(datos.evaluacion.caracter, datos.tablero.fechaLimite)}
          </Alerta>
          <div className="detalle-tarea">
            <div className="detalle-tarea__columna">
              <CriteriosEvaluados evaluacion={datos.evaluacion} />
              <TablaNotasIndividuales
                key={`${datos.evaluacion.idEvaluacion}-${datos.evaluacion.notaGrupal}`}
                evaluacion={datos.evaluacion}
                alActualizar={actualizar}
                alHecho={hecho}
              />
            </div>
            <div className="detalle-tarea__columna">
              <TarjetaNotaGrupal
                key={`${datos.evaluacion.idEvaluacion}-${datos.evaluacion.notaGrupal}`}
                evaluacion={datos.evaluacion}
                alActualizar={actualizar}
                alHecho={hecho}
              />
              <ListaEntregas tareas={datos.tablero.tareas} />
            </div>
          </div>
        </>
      )}
    </div>
  );
}
