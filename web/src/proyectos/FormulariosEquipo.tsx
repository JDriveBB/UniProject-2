import { useId, useState } from 'react';
import type { FormEvent } from 'react';
import { crearEquipo, crearEquiposEnLote } from '../api/proyectos';
import type { CursoResumen, EquipoVista, EstudianteDisponibleVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { useAvisos } from '../componentes/Avisos';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { etiquetaCurso } from '../cursos/vista';
import {
  cupoDeTexto,
  cursoParaEquipo,
  sinEquipoDelCurso,
  textoVistaPrevia,
  validarCupo,
  vistaPreviaLote,
} from './cupos';
import type { CursoParaEquipo } from './cupos';
import { CAMPOS_EQUIPO, mensajeErrorApi } from './errores';
import { MAXIMO_NOMBRE_EQUIPO, validarNombreEquipo } from './validacion';

const CAMPOS_CON_CUPO = [...CAMPOS_EQUIPO, ['cupo', 'Cupo'], ['idCurso', 'Curso']] as const;
const CAMPOS_LOTE = [
  ['prefijo', 'Prefijo de los nombres'],
  ['cantidad', 'Cantidad de equipos'],
  ['cupo', 'Cupo'],
  ['idCurso', 'Curso'],
] as const;

/** Curso del equipo: elegir entre varios, el único o nada (mixto); sin cursos, un aviso. */
function CampoCurso({
  curso,
  elegido,
  alElegir,
  error,
  deshabilitado,
}: {
  curso: CursoParaEquipo;
  elegido: string;
  alElegir: (valor: string) => void;
  error: string | undefined;
  deshabilitado: boolean;
}) {
  const id = useId();
  if (curso.tipo === 'mixto') {
    return null;
  }
  if (curso.tipo === 'sinCursos') {
    return <Alerta tipo="aviso">Asigna primero un curso al proyecto en «Cursos del proyecto».</Alerta>;
  }
  if (curso.tipo === 'unico') {
    return <p className="texto-secundario">{`Curso: ${etiquetaCurso(curso.curso)}`}</p>;
  }
  return (
    <div className="campo">
      <label className="campo__etiqueta" htmlFor={id}>
        Curso del equipo
      </label>
      <select
        id={id}
        className="campo__entrada"
        value={elegido}
        disabled={deshabilitado}
        aria-invalid={error !== undefined}
        aria-describedby={error !== undefined ? id + '-error' : undefined}
        onChange={(ev) => alElegir(ev.target.value)}
      >
        <option value="">Elige un curso</option>
        {curso.cursos.map((c) => (
          <option key={c.idCurso} value={String(c.idCurso)}>
            {etiquetaCurso(c)}
          </option>
        ))}
      </select>
      {error !== undefined && (
        <p id={id + '-error'} className="campo__error">
          {error}
        </p>
      )}
    </div>
  );
}

/** Id del curso con el que se crea: el elegido, el único o null (mixto). */
function idCursoDe(curso: CursoParaEquipo, elegido: string): number | null {
  if (curso.tipo === 'unico') {
    return curso.curso.idCurso;
  }
  return curso.tipo === 'elegir' && elegido !== '' ? Number(elegido) : null;
}

/** Error del curso: solo hace falta elegirlo cuando hay varios. */
function errorCurso(curso: CursoParaEquipo, elegido: string): string | null {
  return curso.tipo === 'elegir' && elegido === '' ? 'Elige el curso del equipo' : null;
}

/** "+ Agregar equipo": nombre, curso y cupo opcional. */
export function FormularioEquipo({
  idProyecto,
  cursos,
  equiposMixtos,
  alCreado,
  alCancelar,
}: {
  idProyecto: number;
  cursos: CursoResumen[];
  equiposMixtos: boolean;
  alCreado: (equipo: EquipoVista) => void;
  alCancelar: () => void;
}) {
  const avisar = useAvisos();
  const curso = cursoParaEquipo(cursos, equiposMixtos);
  const [nombre, setNombre] = useState('');
  const [elegido, setElegido] = useState('');
  const [cupo, setCupo] = useState('');
  const [intentado, setIntentado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const errorNombre = intentado ? (validarNombreEquipo(nombre) ?? undefined) : undefined;
  const errorDelCupo = intentado ? (validarCupo(cupo, false) ?? undefined) : undefined;
  const errorDelCurso = intentado ? (errorCurso(curso, elegido) ?? undefined) : undefined;

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setIntentado(true);
    setError(null);
    if (
      curso.tipo === 'sinCursos' ||
      validarNombreEquipo(nombre) !== null ||
      validarCupo(cupo, false) !== null ||
      errorCurso(curso, elegido) !== null
    ) {
      return;
    }
    setEnviando(true);
    try {
      const equipo = await crearEquipo(idProyecto, {
        nombreEquipo: nombre.trim(),
        idCurso: idCursoDe(curso, elegido),
        cupo: cupoDeTexto(cupo),
      });
      avisar('Equipo creado.');
      alCreado(equipo);
    } catch (e) {
      setError(mensajeErrorApi(e, CAMPOS_CON_CUPO));
      setEnviando(false);
    }
  }

  return (
    <form className="pila" onSubmit={(ev) => void enviar(ev)} noValidate>
      <Campo
        etiqueta="Nombre del equipo"
        placeholder="Equipo Alfa"
        maxLength={MAXIMO_NOMBRE_EQUIPO}
        value={nombre}
        onChange={(ev) => setNombre(ev.target.value)}
        error={errorNombre}
        disabled={enviando}
        required
      />
      <CampoCurso
        curso={curso}
        elegido={elegido}
        alElegir={setElegido}
        error={errorDelCurso}
        deshabilitado={enviando}
      />
      <Campo
        etiqueta="Cupo (opcional)"
        type="number"
        min={1}
        max={100}
        value={cupo}
        onChange={(ev) => setCupo(ev.target.value)}
        error={errorDelCupo}
        disabled={enviando}
      />
      <p className="texto-secundario">
        Vacío: lo armas tú. Con cupo, los estudiantes se inscriben solos hasta la fecha de cierre.
      </p>
      {error !== null && <Alerta tipo="error">{error}</Alerta>}
      <div className="pagina__acciones">
        <Boton type="submit" cargando={enviando} disabled={curso.tipo === 'sinCursos'}>
          Crear equipo
        </Boton>
        <Boton variante="secundario" onClick={alCancelar} disabled={enviando}>
          Cancelar
        </Boton>
      </div>
    </form>
  );
}

/** "+ Crear varios equipos": prefijo, curso, cupo y cantidad opcional, con vista previa en vivo. */
export function FormularioLote({
  idProyecto,
  cursos,
  equiposMixtos,
  disponibles,
  alCreados,
  alCancelar,
}: {
  idProyecto: number;
  cursos: CursoResumen[];
  equiposMixtos: boolean;
  disponibles: EstudianteDisponibleVista[];
  alCreados: (lista: EquipoVista[]) => void;
  alCancelar: () => void;
}) {
  const avisar = useAvisos();
  const curso = cursoParaEquipo(cursos, equiposMixtos);
  const [prefijo, setPrefijo] = useState('Equipo');
  const [elegido, setElegido] = useState('');
  const [cupo, setCupo] = useState('');
  const [cantidad, setCantidad] = useState('');
  const [intentado, setIntentado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const errorDelCurso = intentado ? (errorCurso(curso, elegido) ?? undefined) : undefined;
  const idCurso = idCursoDe(curso, elegido);
  const previa = vistaPreviaLote(prefijo, cantidad, cupo, sinEquipoDelCurso(disponibles, idCurso));

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setIntentado(true);
    setError(null);
    if (curso.tipo === 'sinCursos' || errorCurso(curso, elegido) !== null) {
      return;
    }
    if ('error' in previa) {
      return;
    }
    setEnviando(true);
    try {
      const lista = await crearEquiposEnLote(idProyecto, {
        prefijo: prefijo.trim(),
        cantidad: cantidad.trim() === '' ? null : Number(cantidad.trim()),
        idCurso,
        cupo: Number(cupo.trim()),
      });
      avisar(lista.length === 1 ? 'Se creó 1 equipo.' : `Se crearon ${lista.length} equipos.`);
      alCreados(lista);
    } catch (e) {
      setError(mensajeErrorApi(e, CAMPOS_LOTE));
      setEnviando(false);
    }
  }

  return (
    <form className="pila" onSubmit={(ev) => void enviar(ev)} noValidate>
      <Campo
        etiqueta="Prefijo de los nombres"
        maxLength={90}
        value={prefijo}
        onChange={(ev) => setPrefijo(ev.target.value)}
        disabled={enviando}
        required
      />
      <CampoCurso
        curso={curso}
        elegido={elegido}
        alElegir={setElegido}
        error={errorDelCurso}
        deshabilitado={enviando}
      />
      <Campo
        etiqueta="Cupo de cada equipo"
        type="number"
        min={1}
        max={100}
        value={cupo}
        onChange={(ev) => setCupo(ev.target.value)}
        disabled={enviando}
        required
      />
      <Campo
        etiqueta="Cantidad de equipos (opcional)"
        type="number"
        min={1}
        max={50}
        value={cantidad}
        onChange={(ev) => setCantidad(ev.target.value)}
        disabled={enviando}
      />
      <p className="texto-secundario">Vacía: los necesarios para los estudiantes sin equipo de ese curso.</p>
      {curso.tipo === 'elegir' && elegido === '' ? (
        <p className="texto-secundario" aria-live="polite">
          Elige un curso para calcular.
        </p>
      ) : (
        'cantidad' in previa && (
          <p className="texto-secundario" aria-live="polite">
            {textoVistaPrevia(previa)}
          </p>
        )
      )}
      {'error' in previa && intentado && <Alerta tipo="aviso">{previa.error}</Alerta>}
      {error !== null && <Alerta tipo="error">{error}</Alerta>}
      <div className="pagina__acciones">
        <Boton type="submit" cargando={enviando} disabled={curso.tipo === 'sinCursos'}>
          Crear equipos
        </Boton>
        <Boton variante="secundario" onClick={alCancelar} disabled={enviando}>
          Cancelar
        </Boton>
      </div>
    </form>
  );
}
