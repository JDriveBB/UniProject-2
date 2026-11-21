import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { crearEquipo, detalleProyecto, listarEstudiantes, listarEstudiantesDisponibles } from '../api/proyectos';
import type { EquipoVista, EstudianteDisponibleVista, EstudianteProyectoVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { useAvisos } from '../componentes/Avisos';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Casilla } from '../componentes/Casilla';
import { Tarjeta } from '../componentes/Tarjeta';
import { CAMPOS_EQUIPO, mensajeErrorApi } from './errores';
import type { Mensaje } from './errores';
import { CorreosPendientes } from './CorreosPendientes';
import { claveIntegrantes } from './equipos';
import { FilaMonitores } from './FilaMonitores';
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
  alCambiarUsaMonitores: (valor: boolean) => void;
  alActualizarEquipo: (equipo: EquipoVista) => void;
}

function RejillaEquipos({
  idProyecto,
  equipos,
  estudiantes,
  disponibles,
  editable,
  alActualizarEquipo,
  alHabilitarCorreo,
}: {
  idProyecto: number;
  equipos: EquipoVista[];
  estudiantes: EstudianteProyectoVista[];
  disponibles: EstudianteDisponibleVista[];
  editable: boolean;
  alActualizarEquipo: (equipo: EquipoVista) => void;
  alHabilitarCorreo: () => void;
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
          estudiantes={estudiantes}
          disponibles={disponibles}
          editable={editable}
          alActualizar={alActualizarEquipo}
          alHabilitarCorreo={alHabilitarCorreo}
        />
      ))}
    </div>
  );
}

/** P03 derecha 2: equipos en dos columnas, casilla de monitores, fila de monitores y nota y los correos habilitados sin cuenta (bloque 30). */
export function TarjetaEquipos({
  idProyecto,
  equipos,
  editable,
  usaMonitores,
  usaMonitoresGuardado,
  alCambiarUsaMonitores,
  alActualizarEquipo,
}: PropsEquipos) {
  const avisar = useAvisos();
  const [versionCorreos, setVersionCorreos] = useState(0);
  const [estudiantes, setEstudiantes] = useState<EstudianteProyectoVista[]>([]);
  const [disponibles, setDisponibles] = useState<EstudianteDisponibleVista[]>([]);
  const integrantes = claveIntegrantes(equipos);
  const [abierto, setAbierto] = useState(false);
  const [nombre, setNombre] = useState('');
  const [intentado, setIntentado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const errorNombre = intentado ? (validarNombreEquipo(nombre) ?? undefined) : undefined;

  function correoHabilitado() {
    setVersionCorreos((v) => v + 1);
  }

  useEffect(() => {
    if (versionCorreos === 0 || idProyecto === null) {
      return;
    }
    let vigente = true;
    detalleProyecto(idProyecto)
      .then((detalle) => {
        if (vigente) {
          for (const e of detalle.equipos) {
            alActualizarEquipo(e);
          }
        }
      })
      .catch(() => {
        // no bloquear si falla la recarga secundaria
      });
    return () => {
      vigente = false;
    };
  }, [idProyecto, versionCorreos, alActualizarEquipo]);

  /* D-11, D-12 y T-046: estudiantes del proyecto (sugerencias e invitados) y cuentas disponibles sin equipo una vez,
     y de nuevo al habilitar, invitar, retirar, agregar o quitar integrantes. Solo el docente dueño puede pedirlos. */
  useEffect(() => {
    if (!editable || idProyecto === null) {
      return;
    }
    let vigente = true;
    void Promise.allSettled([listarEstudiantes(idProyecto), listarEstudiantesDisponibles(idProyecto)]).then(
      ([resEstudiantes, resDisponibles]) => {
        if (!vigente) {
          return;
        }
        if (resEstudiantes.status === 'fulfilled') {
          setEstudiantes(resEstudiantes.value);
        }
        if (resDisponibles.status === 'fulfilled') {
          setDisponibles(resDisponibles.value);
        }
      },
    );
    return () => {
      vigente = false;
    };
  }, [editable, idProyecto, versionCorreos, integrantes]);

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
      alActualizarEquipo(await crearEquipo(idProyecto, nombre.trim()));
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
          idProyecto={idProyecto}
          equipos={equipos}
          estudiantes={estudiantes}
          disponibles={disponibles}
          editable={editable}
          alActualizarEquipo={alActualizarEquipo}
          alHabilitarCorreo={correoHabilitado}
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
          <FilaMonitores idProyecto={idProyecto} version={versionCorreos} alHabilitarCorreo={correoHabilitado} />
        ) : (
          <p className="texto-secundario">Guarda el proyecto para habilitar monitores.</p>
        ))}
      {editable && idProyecto !== null && (
        <CorreosPendientes
          idProyecto={idProyecto}
          version={versionCorreos}
          alCambiarHabilitaciones={correoHabilitado}
        />
      )}
      {editable && <p className="texto-secundario">{NOTA}</p>}
    </Tarjeta>
  );
}
