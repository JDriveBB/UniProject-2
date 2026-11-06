import { useState } from 'react';
import type { FormEvent } from 'react';
import { crearEquipo } from '../api/proyectos';
import type { EquipoVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Casilla } from '../componentes/Casilla';
import { Tarjeta } from '../componentes/Tarjeta';
import { CAMPOS_EQUIPO, mensajeErrorApi } from './errores';
import type { Mensaje } from './errores';
import { FilaMonitores } from './FilaMonitores';
import { TarjetaEquipo } from './TarjetaEquipo';
import { MAXIMO_NOMBRE_EQUIPO, validarNombreEquipo } from './validacion';

const NOTA = 'Todos los usuarios se registran como estudiantes. El docente designa al líder de cada equipo y, si la '
  + 'institución trabaja con monitores, a los monitores del proyecto.';

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

function RejillaEquipos({ idProyecto, equipos, editable, alActualizarEquipo }: {
  idProyecto: number;
  equipos: EquipoVista[];
  editable: boolean;
  alActualizarEquipo: (equipo: EquipoVista) => void;
}) {
  if (equipos.length === 0) {
    return <p className="texto-secundario">Aún no hay equipos.</p>;
  }
  return (
    <div className="rejilla-equipos">
      {equipos.map((e) => (
        <TarjetaEquipo key={e.idEquipo} idProyecto={idProyecto} equipo={e} editable={editable} alActualizar={alActualizarEquipo} />
      ))}
    </div>
  );
}

/** P03 derecha 2: equipos en dos columnas, casilla de monitores, fila de monitores y nota. */
export function TarjetaEquipos({
  idProyecto,
  equipos,
  editable,
  usaMonitores,
  usaMonitoresGuardado,
  alCambiarUsaMonitores,
  alActualizarEquipo,
}: PropsEquipos) {
  const [abierto, setAbierto] = useState(false);
  const [nombre, setNombre] = useState('');
  const [intentado, setIntentado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const errorNombre = intentado ? (validarNombreEquipo(nombre) ?? undefined) : undefined;

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
      setMensaje({ tipo: 'exito', texto: 'Equipo creado.' });
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
        <RejillaEquipos idProyecto={idProyecto} equipos={equipos} editable={editable} alActualizarEquipo={alActualizarEquipo} />
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
          <FilaMonitores idProyecto={idProyecto} />
        ) : (
          <p className="texto-secundario">Guarda el proyecto para habilitar monitores.</p>
        ))}
      {editable && <p className="texto-secundario">{NOTA}</p>}
    </Tarjeta>
  );
}
