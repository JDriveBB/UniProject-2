import { useEffect, useId, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router';
import { consultarEvaluacion } from '../api/evaluacion';
import { detalleProyecto } from '../api/proyectos';
import { tableroHito } from '../api/tareas';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { Insignia } from '../componentes/Insignia';
import {
  cargarEvaluacion,
  etiquetaDeHito,
  type ApiCargaEvaluacion,
  type DatosEvaluacion,
} from '../evaluacion/carga';
import { CriteriosEvaluados } from '../evaluacion/CriteriosEvaluados';
import { ResumenEvaluacion } from '../evaluacion/ResumenEvaluacion';
import {
  REENVIADO_ESTUDIANTE,
  SIN_ACCESO_EVALUACION,
  SIN_EVALUACION,
  textoCaracter,
} from '../evaluacion/vista';
import { fechaDia } from '../formato';
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

export function EvaluacionHitoDeRuta() {
  const { idHitoEquipo } = useParams();
  const usuario = useUsuario();
  const id = idDeRuta(idHitoEquipo ?? '');

  if (usuario.rol === 'DOCENTE' && id !== null) {
    return <Navigate to={rutas.evaluarHito(id)} replace />;
  }
  return <EvaluacionHito key={idHitoEquipo} idRuta={idHitoEquipo ?? ''} />;
}

export function EvaluacionHito({ idRuta }: { idRuta: string }) {
  const usuario = useUsuario();
  const idHitoEquipo = idDeRuta(idRuta);

  const [carga, setCarga] = useState<EstadoCarga>(() =>
    idHitoEquipo === null
      ? { estado: 'error', mensaje: SIN_ACCESO_EVALUACION }
      : { estado: 'cargando' }
  );
  const [vuelta, setVuelta] = useState(0);
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

  if (carga.estado === 'cargando') {
    return <Cargando />;
  }

  if (carga.estado === 'error') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Resultado de la evaluación" />
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
  const evaluacion = datos.evaluacion;
  const vista = usuario.rol === 'ESTUDIANTE' ? 'estudiante' : 'monitor';
  const notaPropia =
    evaluacion !== null
      ? evaluacion.notas.find((n) => n.idEstudiante === usuario.idUsuario) ?? null
      : null;

  return (
    <div className="pagina">
      <EncabezadoPagina
        titulo="Resultado de la evaluación"
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

      <div id={idAviso} tabIndex={-1} />

      {evaluacion === null ? (
        <p className="pagina__vacio">{SIN_EVALUACION}</p>
      ) : (
        <>
          {datos.tablero.estado === 'ENVIADO' && (
            <Alerta tipo="info">{REENVIADO_ESTUDIANTE}</Alerta>
          )}
          <ResumenEvaluacion
            evaluacion={evaluacion}
            vista={vista}
            notaPropia={notaPropia}
          />
          <Alerta tipo="info">
            {textoCaracter(evaluacion.caracter, datos.tablero.fechaLimite)}
          </Alerta>
          <CriteriosEvaluados evaluacion={evaluacion} />
        </>
      )}
    </div>
  );
}
