import { useCallback, useEffect, useState } from 'react';
import {
  generarEnlaceCorreo,
  listarCorreos,
  listarCursosProyecto,
  listarEstudiantesDisponibles,
  retirarCorreo,
  vistaInscripcion,
} from '../api/proyectos';
import type { CursoResumen, EquipoVista, EstudianteDisponibleVista, InscripcionVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Casilla } from '../componentes/Casilla';
import { Tarjeta } from '../componentes/Tarjeta';
import { BloqueInscripcion } from './BloqueInscripcion';
import { CorreosPendientes } from './CorreosPendientes';
import { claveIntegrantes } from './equipos';
import { FilaMonitores } from './FilaMonitores';
import { FormularioEquipo, FormularioLote } from './FormulariosEquipo';
import { preguntaRetirarCorreo } from './quitar';
import { SinEquipo } from './SinEquipo';
import { TarjetaEquipo } from './TarjetaEquipo';

const NOTA =
  'Los estudiantes entran al proyecto por sus cursos. En los equipos con cupo se inscriben solos hasta la fecha ' +
  'de cierre; al cerrar, cada equipo vota a su líder. Si la institución trabaja con monitores, agrégalos aquí.';

interface PropsEquipos {
  /** null mientras el proyecto no se ha guardado por primera vez. */
  idProyecto: number | null;
  equipos: EquipoVista[];
  editable: boolean;
  /** Casilla del borrador (se guarda con "Guardar proyecto"). */
  usaMonitores: boolean;
  /** Valor guardado: habilitar monitores lo exige (409 PROYECTO_SIN_MONITORES). */
  usaMonitoresGuardado: boolean;
  /** Sube al cambiar cursos del proyecto para recargar disponibles. */
  versionCursos: number;
  /** El proyecto mezcla estudiantes de varios cursos en un mismo equipo (no se pide curso al crear equipos). */
  equiposMixtos: boolean;
  alCambiarUsaMonitores: (valor: boolean) => void;
  alActualizarEquipo: (equipo: EquipoVista) => void;
  /** Vuelve a pedir los equipos del proyecto (tras asignar o repartir). */
  alRecargarEquipos: () => void;
}

function RejillaEquipos({
  idProyecto,
  equipos,
  disponibles,
  cursos,
  horasRonda,
  editable,
  alActualizarEquipo,
}: {
  idProyecto: number;
  equipos: EquipoVista[];
  disponibles: EstudianteDisponibleVista[];
  cursos: CursoResumen[];
  horasRonda: number | undefined;
  editable: boolean;
  alActualizarEquipo: (equipo: EquipoVista) => void;
}) {
  if (equipos.length === 0) {
    return <p className="texto-secundario">Aún no hay equipos.</p>;
  }
  return (
    <div className="rejilla-equipos">
      {equipos.map((e) => (
        <TarjetaEquipo
          key={e.idEquipo}
          idProyecto={idProyecto}
          equipo={e}
          disponibles={disponibles}
          cursos={cursos}
          horasRonda={horasRonda}
          editable={editable}
          alActualizar={alActualizarEquipo}
        />
      ))}
    </div>
  );
}

/** P03 derecha 2: equipos en dos columnas, casilla de monitores, fila de monitores y monitores sin cuenta. */
export function TarjetaEquipos({
  idProyecto,
  equipos,
  editable,
  usaMonitores,
  usaMonitoresGuardado,
  versionCursos,
  equiposMixtos,
  alCambiarUsaMonitores,
  alActualizarEquipo,
  alRecargarEquipos,
}: PropsEquipos) {
  const [versionCorreos, setVersionCorreos] = useState(0);
  const [disponibles, setDisponibles] = useState<EstudianteDisponibleVista[]>([]);
  const [versionDisponibles, setVersionDisponibles] = useState(0);
  const [cursos, setCursos] = useState<CursoResumen[] | null>(null);
  const [errorCursos, setErrorCursos] = useState(false);
  const [inscripcion, setInscripcion] = useState<InscripcionVista | null>(null);
  const integrantes = claveIntegrantes(equipos);
  const [formulario, setFormulario] = useState<'equipo' | 'lote' | null>(null);

  const correoHabilitado = useCallback(() => {
    setVersionCorreos((v) => v + 1);
  }, []);

  const cargarMonitoresPendientes = useCallback(async () => {
    if (idProyecto === null) {
      return [];
    }
    const correos = await listarCorreos(idProyecto);
    return correos.filter((c) => c.rol === 'MONITOR');
  }, [idProyecto]);

  const generarEnlaceMonitor = useCallback(
    (idCorreoHabilitado: number) => {
      if (idProyecto === null) {
        return Promise.reject(new Error('Proyecto sin guardar'));
      }
      return generarEnlaceCorreo(idProyecto, idCorreoHabilitado);
    },
    [idProyecto],
  );

  const retirarMonitorPendiente = useCallback(
    (idCorreoHabilitado: number) => {
      if (idProyecto === null) {
        return Promise.reject(new Error('Proyecto sin guardar'));
      }
      return retirarCorreo(idProyecto, idCorreoHabilitado);
    },
    [idProyecto],
  );

  useEffect(() => {
    if (!editable || idProyecto === null) {
      return;
    }
    let vigente = true;
    listarCursosProyecto(idProyecto)
      .then((lista) => {
        if (vigente) {
          setCursos(lista);
          setErrorCursos(false);
        }
      })
      .catch(() => {
        if (vigente) {
          setErrorCursos(true);
        }
      });
    listarEstudiantesDisponibles(idProyecto)
      .then((lista) => {
        if (vigente) {
          setDisponibles(lista);
        }
      })
      .catch(() => {
        // no bloquear si falla la carga de sugerencias
      });
    return () => {
      vigente = false;
    };
  }, [editable, idProyecto, integrantes, versionCursos, versionDisponibles]);

  const cargarInscripcion = useCallback(() => {
    if (idProyecto === null) {
      return () => undefined;
    }
    let vigente = true;
    vistaInscripcion(idProyecto)
      .then((v) => {
        if (vigente) {
          setInscripcion(v);
        }
      })
      .catch(() => {
        // la inscripción no se muestra si no carga
      });
    return () => {
      vigente = false;
    };
  }, [idProyecto]);

  useEffect(() => {
    if (!editable) {
      return;
    }
    return cargarInscripcion();
  }, [editable, cargarInscripcion]);

  function alCambioSinEquipo() {
    alRecargarEquipos();
    setVersionDisponibles((v) => v + 1);
  }

  return (
    <Tarjeta titulo="Equipos y monitores">
      {idProyecto === null ? (
        <p className="texto-secundario">Guarda el proyecto para crear equipos y agregar estudiantes.</p>
      ) : (
        <>
          {editable && (
            <BloqueInscripcion idProyecto={idProyecto} inscripcion={inscripcion} alGuardado={setInscripcion} />
          )}
          <RejillaEquipos
            idProyecto={idProyecto}
            equipos={equipos}
            disponibles={disponibles}
            cursos={cursos ?? []}
            horasRonda={inscripcion?.horasRonda}
            editable={editable}
            alActualizarEquipo={alActualizarEquipo}
          />
        </>
      )}
      {editable && idProyecto !== null && errorCursos && cursos === null && (
        <Alerta tipo="error">
          No se pudieron cargar los cursos del proyecto. Recarga la página para crear equipos.
        </Alerta>
      )}
      {editable &&
        idProyecto !== null &&
        (formulario === 'equipo' ? (
          <FormularioEquipo
            idProyecto={idProyecto}
            cursos={cursos ?? []}
            equiposMixtos={equiposMixtos}
            alCreado={(e) => {
              alActualizarEquipo(e);
              setFormulario(null);
            }}
            alCancelar={() => setFormulario(null)}
          />
        ) : formulario === 'lote' ? (
          <FormularioLote
            idProyecto={idProyecto}
            cursos={cursos ?? []}
            equiposMixtos={equiposMixtos}
            disponibles={disponibles}
            alCreados={(lista) => {
              lista.forEach(alActualizarEquipo);
              setFormulario(null);
            }}
            alCancelar={() => setFormulario(null)}
          />
        ) : (
          <div className="pagina__acciones">
            <Boton variante="secundario" disabled={cursos === null} onClick={() => setFormulario('equipo')}>
              + Agregar equipo
            </Boton>
            <Boton variante="secundario" disabled={cursos === null} onClick={() => setFormulario('lote')}>
              + Crear varios equipos
            </Boton>
          </div>
        ))}
      {editable && idProyecto !== null && (
        <SinEquipo
          idProyecto={idProyecto}
          disponibles={disponibles}
          equipos={equipos}
          cursos={cursos ?? []}
          alCambio={alCambioSinEquipo}
        />
      )}
      {editable && (
        <Casilla checked={usaMonitores} onChange={(ev) => alCambiarUsaMonitores(ev.target.checked)}>
          La institución trabaja con monitores en este proyecto
        </Casilla>
      )}
      {editable &&
        usaMonitores &&
        (usaMonitoresGuardado && idProyecto !== null ? (
          <>
            <FilaMonitores idProyecto={idProyecto} version={versionCorreos} alHabilitarCorreo={correoHabilitado} />
            <CorreosPendientes
              titulo="Monitores sin cuenta"
              version={versionCorreos}
              cargar={cargarMonitoresPendientes}
              generarEnlace={generarEnlaceMonitor}
              retirar={retirarMonitorPendiente}
              preguntaRetirar={preguntaRetirarCorreo}
              alCambiarHabilitaciones={correoHabilitado}
            />
          </>
        ) : (
          <p className="texto-secundario">Guarda el proyecto para habilitar monitores.</p>
        ))}
      {editable && <p className="texto-secundario">{NOTA}</p>}
    </Tarjeta>
  );
}
