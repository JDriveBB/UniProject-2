import { useEffect, useId, useState } from 'react';
import { Link, useParams } from 'react-router';
import { detalleProyecto, listarProyectos } from '../api/proyectos';
import { detalleTarea, listarComentarios, listarCorrecciones, listarEvidencias, tableroHito } from '../api/tareas';
import { Alerta } from '../componentes/Alerta';
import { useAvisos } from '../componentes/Avisos';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { Insignia } from '../componentes/Insignia';
import type { Mensaje } from '../proyectos/errores';
import { idDeRuta } from '../proyectos/vista';
import { rutas } from '../rutas';
import { useUsuario } from '../sesion/ContextoSesion';
import { codigosDeTareas, nombreConCodigo } from '../tablero/marcas';
import { etiquetaHito, mensajeErrorAbrir, rolEnEquipo } from '../tablero/ubicacion';
import { cargarTarea, type ApiCargaTarea, type DatosTarea } from '../tarea/carga';
import { ModalApoyo } from '../tarea/ModalApoyo';
import { ModalCorreccion } from '../tarea/ModalCorreccion';
import { ModalDependencias } from '../tarea/ModalDependencias';
import { ModalEvidencia } from '../tarea/ModalEvidencia';
import { ModalReasignar } from '../tarea/ModalReasignar';
import { permisosTarea } from '../tarea/permisos';
import { TarjetaAcciones, type AccionTarea } from '../tarea/TarjetaAcciones';
import { TarjetaAvance } from '../tarea/TarjetaAvance';
import { TarjetaComentarios } from '../tarea/TarjetaComentarios';
import { TarjetaEvidencias } from '../tarea/TarjetaEvidencias';
import { TarjetaHistorial } from '../tarea/TarjetaHistorial';
import { TarjetaInformacion } from '../tarea/TarjetaInformacion';
import {
  bloqueoCorreccion,
  correccionPendiente,
  insigniasDeTarea,
  prerrequisitosDe,
  SIN_ACCESO_TAREA,
  textoSoloLectura,
} from '../tarea/vista';

const API_CARGA: ApiCargaTarea = {
  listarProyectos,
  detalleProyecto,
  detalleTarea,
  tableroHito,
  listarEvidencias,
  listarComentarios,
  listarCorrecciones,
};

type TipoModal = 'dependencias' | 'evidencia' | 'version' | AccionTarea;

type EstadoCarga =
  { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'listo'; datos: DatosTarea };

export function DetalleTareaDeRuta() {
  const { idTarea } = useParams();
  return <DetalleTarea key={idTarea} idRuta={idTarea ?? ''} />;
}

export function DetalleTarea({ idRuta }: { idRuta: string }) {
  const usuario = useUsuario();
  const avisar = useAvisos();
  const idTarea = idDeRuta(idRuta);

  const [carga, setCarga] = useState<EstadoCarga>(() =>
    idTarea === null ? { estado: 'error', mensaje: SIN_ACCESO_TAREA } : { estado: 'cargando' },
  );
  const [vuelta, setVuelta] = useState(0);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const [modal, setModal] = useState<TipoModal | null>(null);
  const [foco, setFoco] = useState<string | null>(null);
  const [botonModal, setBotonModal] = useState<string | null>(null);

  const idAviso = useId();
  const idBotonDependencias = useId();
  const idBotonAdjuntar = useId();
  const idBotonVersion = useId();
  const idBotonApoyo = useId();
  const idBotonReasignar = useId();
  const idBotonCorreccion = useId();

  const idsAcciones: Record<AccionTarea, string> = {
    apoyo: idBotonApoyo,
    reasignar: idBotonReasignar,
    correccion: idBotonCorreccion,
  };

  useEffect(() => {
    if (idTarea === null) {
      return;
    }
    let vigente = true;
    cargarTarea(API_CARGA, idTarea)
      .then((datos) => {
        if (vigente) {
          setCarga({ estado: 'listo', datos });
        }
      })
      .catch((e) => {
        if (vigente) {
          setCarga({ estado: 'error', mensaje: mensajeErrorAbrir(e, SIN_ACCESO_TAREA) });
        }
      });
    return () => {
      vigente = false;
    };
  }, [idTarea, vuelta]);

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
        <EncabezadoPagina titulo="Tarea" />
        <Alerta tipo="error">{carga.mensaje}</Alerta>
        {carga.mensaje !== SIN_ACCESO_TAREA && (
          <div className="pagina__acciones">
            <Boton variante="secundario" onClick={() => setVuelta((v) => v + 1)}>
              Reintentar
            </Boton>
          </div>
        )}
      </div>
    );
  }

  const { detalle, tablero, equipo, hito, evidencias, comentarios, correcciones } = carga.datos;
  const tarea = detalle.tarea;
  const codigos = codigosDeTareas(tablero.tareas);
  const nombre = nombreConCodigo(codigos, tarea);
  const etiqueta = hito !== null ? etiquetaHito(hito.orden, hito.nombreHito) : tablero.nombreHito;
  const rol = equipo !== null ? rolEnEquipo(equipo.equipo, usuario.idUsuario) : null;
  const permisos = permisosTarea({
    idUsuario: usuario.idUsuario,
    rolEquipo: rol,
    idEncargado: tarea.idEncargado,
    editable: detalle.editable,
  });
  const pendiente = correccionPendiente(correcciones);
  const esElEncargado = rol !== null && tarea.idEncargado === usuario.idUsuario;
  const miembros = equipo?.equipo.miembros ?? [];

  function abrir(m: TipoModal, idBoton: string) {
    setModal(m);
    setBotonModal(idBoton);
  }

  function cerrar() {
    setModal(null);
    setFoco(botonModal);
    setBotonModal(null);
  }

  function hecho(m: Mensaje) {
    const veniaDeModal = modal !== null;
    setModal(null);
    setVuelta((v) => v + 1);
    if (m.tipo === 'exito') {
      setMensaje(null);
      avisar(m.texto);
      if (veniaDeModal) {
        setFoco(botonModal);
      }
      setBotonModal(null);
      return;
    }
    setMensaje(m);
    setFoco(idAviso);
    setBotonModal(null);
  }

  return (
    <div className="pagina">
      <EncabezadoPagina titulo={nombre} descripcion={`${etiqueta} · ${tablero.nombreEquipo}`}>
        <div className="pagina__acciones">
          <Link className="boton boton--secundario" to={rutas.tableroEnHito(tablero.idEquipo, tablero.idHitoEquipo)}>
            Volver al tablero
          </Link>
        </div>
      </EncabezadoPagina>

      <div className="insignias-tarea">
        {insigniasDeTarea(tarea, pendiente !== null).map((m) => (
          <Insignia key={m.texto} tono={m.tono}>
            {m.texto}
          </Insignia>
        ))}
      </div>

      {!detalle.editable && <Alerta tipo="info">{textoSoloLectura(tablero.estado)}</Alerta>}

      {mensaje !== null && (
        <div id={idAviso} tabIndex={-1}>
          <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>
        </div>
      )}

      <div className="detalle-tarea">
        <div className="detalle-tarea__columna">
          <TarjetaInformacion
            tarea={tarea}
            prerrequisitos={prerrequisitosDe(tarea, tablero.tareas, codigos)}
            puedeGestionar={permisos.gestionar}
            idBoton={idBotonDependencias}
            alEstablecer={() => abrir('dependencias', idBotonDependencias)}
          />
          {permisos.avanzar && <TarjetaAvance tarea={tarea} alHecho={hecho} />}
          <TarjetaEvidencias
            evidencias={evidencias}
            correccion={pendiente}
            esElEncargado={esElEncargado}
            puedeAdjuntar={permisos.adjuntar}
            idAdjuntar={idBotonAdjuntar}
            idVersion={idBotonVersion}
            alAdjuntar={(nuevaVersion) =>
              abrir(nuevaVersion ? 'version' : 'evidencia', nuevaVersion ? idBotonVersion : idBotonAdjuntar)
            }
          />
          <TarjetaHistorial avances={detalle.avances} />
        </div>

        <div className="detalle-tarea__columna">
          <TarjetaAcciones
            tarea={tarea}
            permisos={permisos}
            bloqueoCorreccion={bloqueoCorreccion(tarea, evidencias, pendiente)}
            ids={idsAcciones}
            alAbrir={(accion) => abrir(accion, idsAcciones[accion])}
          />
          <TarjetaComentarios
            idTarea={tarea.idTarea}
            comentarios={comentarios}
            puedeComentar={permisos.comentar}
            alHecho={hecho}
          />
        </div>
      </div>

      {modal === 'dependencias' && (
        <ModalDependencias
          tarea={tarea}
          tareas={tablero.tareas}
          codigos={codigos}
          alCerrar={cerrar}
          alGuardar={hecho}
        />
      )}

      {(modal === 'evidencia' || modal === 'version') && (
        <ModalEvidencia
          idTarea={tarea.idTarea}
          nombre={nombre}
          nuevaVersion={modal === 'version'}
          conCorreccion={pendiente !== null}
          alCerrar={cerrar}
          alGuardar={hecho}
        />
      )}

      {modal === 'correccion' && (
        <ModalCorreccion tarea={tarea} nombre={nombre} evidencias={evidencias} alCerrar={cerrar} alGuardar={hecho} />
      )}

      {modal === 'reasignar' && (
        <ModalReasignar tarea={tarea} nombre={nombre} miembros={miembros} alCerrar={cerrar} alGuardar={hecho} />
      )}

      {modal === 'apoyo' && <ModalApoyo idTarea={tarea.idTarea} nombre={nombre} alCerrar={cerrar} alGuardar={hecho} />}
    </div>
  );
}
