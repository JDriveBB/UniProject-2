import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  crearEquipo,
  generarEnlaceCorreo,
  listarCorreos,
  listarEstudiantesDisponibles,
  retirarCorreo,
} from '../api/proyectos';
import type { EquipoVista, EstudianteDisponibleVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { useAvisos } from '../componentes/Avisos';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Casilla } from '../componentes/Casilla';
import { Tarjeta } from '../componentes/Tarjeta';
import { CorreosPendientes } from './CorreosPendientes';
import { claveIntegrantes } from './equipos';
import { CAMPOS_EQUIPO, mensajeErrorApi } from './errores';
import type { Mensaje } from './errores';
import { FilaMonitores } from './FilaMonitores';
import { preguntaRetirarCorreo } from './quitar';
import { TarjetaEquipo } from './TarjetaEquipo';
import { MAXIMO_NOMBRE_EQUIPO, validarNombreEquipo } from './validacion';

const NOTA =
  'Todos los usuarios se registran como estudiantes. El docente designa al líder de cada equipo y, si la ' +
  'institución trabaja con monitores, a los monitores del proyecto.';

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
  alCambiarUsaMonitores: (valor: boolean) => void;
  alActualizarEquipo: (equipo: EquipoVista) => void;
}

function RejillaEquipos({
  equipos,
  disponibles,
  editable,
  alActualizarEquipo,
}: {
  equipos: EquipoVista[];
  disponibles: EstudianteDisponibleVista[];
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
          equipo={e}
          disponibles={disponibles}
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
  alCambiarUsaMonitores,
  alActualizarEquipo,
}: PropsEquipos) {
  const avisar = useAvisos();
  const [versionCorreos, setVersionCorreos] = useState(0);
  const [disponibles, setDisponibles] = useState<EstudianteDisponibleVista[]>([]);
  const integrantes = claveIntegrantes(equipos);
  const [abierto, setAbierto] = useState(false);
  const [nombre, setNombre] = useState('');
  const [intentado, setIntentado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const errorNombre = intentado ? (validarNombreEquipo(nombre) ?? undefined) : undefined;

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
  }, [editable, idProyecto, integrantes, versionCursos]);

  function cerrar() {
    setAbierto(false);
    setNombre('');
    setIntentado(false);
  }

  async function alCrear(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setIntentado(true);
    setMensaje(null);
    if (idProyecto === null || validarNombreEquipo(nombre) !== null) {
      return;
    }
    setEnviando(true);
    try {
      alActualizarEquipo(await crearEquipo(idProyecto, { nombreEquipo: nombre.trim(), idCurso: null, cupo: null }));
      cerrar();
      avisar('Equipo creado.');
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeErrorApi(e, CAMPOS_EQUIPO) });
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Tarjeta titulo="Equipos y monitores">
      {idProyecto === null ? (
        <p className="texto-secundario">Guarda el proyecto para crear equipos y agregar estudiantes.</p>
      ) : (
        <RejillaEquipos
          equipos={equipos}
          disponibles={disponibles}
          editable={editable}
          alActualizarEquipo={alActualizarEquipo}
        />
      )}
      {editable &&
        idProyecto !== null &&
        (abierto ? (
          <form className="formulario-en-linea" onSubmit={(ev) => void alCrear(ev)} noValidate>
            <Campo
              etiqueta="Nombre del equipo"
              placeholder="Equipo Alfa"
              maxLength={MAXIMO_NOMBRE_EQUIPO}
              value={nombre}
              onChange={(ev) => setNombre(ev.target.value)}
              error={errorNombre}
              required
            />
            <Boton type="submit" cargando={enviando}>
              Crear equipo
            </Boton>
            <Boton variante="secundario" onClick={cerrar} disabled={enviando}>
              Cancelar
            </Boton>
          </form>
        ) : (
          <div>
            <Boton variante="secundario" onClick={() => setAbierto(true)}>
              + Agregar equipo
            </Boton>
          </div>
        ))}
      {mensaje !== null && <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>}
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
