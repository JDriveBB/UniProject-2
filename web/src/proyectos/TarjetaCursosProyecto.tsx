import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router';
import { ErrorApi } from '../api/cliente';
import { detalleCurso, listarCursos } from '../api/cursos';
import {
  asignarCursosProyecto,
  elegirCursoEstudiante,
  listarCursosProyecto,
  listarEstudiantes,
} from '../api/proyectos';
import type {
  CursoResumen,
  CursoVista,
  EstudianteRepetidoVista,
  ResultadoCursosProyecto,
} from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { useAvisos } from '../componentes/Avisos';
import { Boton } from '../componentes/Boton';
import { Casilla } from '../componentes/Casilla';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import {
  etiquetaCurso,
  listaConY,
  mismaSeleccion,
  opcionesCursos,
  repetidosDelProyecto,
  textoConfirmarCursos,
} from '../cursos/vista';
import { rutas } from '../rutas';
import { mensajeErrorApi } from './errores';

type EstadoCarga =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo' };

/** Tarjeta de cursos asignados al proyecto; guarda al momento con vista previa y confirmación. */
export function TarjetaCursosProyecto({
  idProyecto,
  alCambiarCursos,
}: {
  idProyecto: number | null;
  alCambiarCursos: () => void;
}) {
  const avisar = useAvisos();
  const [carga, setCarga] = useState<EstadoCarga>(() =>
    idProyecto === null ? { estado: 'listo' } : { estado: 'cargando' },
  );
  const [asignados, setAsignados] = useState<CursoResumen[]>([]);
  const [mios, setMios] = useState<CursoVista[]>([]);
  const [repetidos, setRepetidos] = useState<EstudianteRepetidoVista[]>([]);
  const [editando, setEditando] = useState(false);
  const [seleccion, setSeleccion] = useState<number[]>([]);
  const [previa, setPrevia] = useState<ResultadoCursosProyecto | null>(null);
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errorRepetido, setErrorRepetido] = useState<string | null>(null);
  const [cambiandoUsuario, setCambiandoUsuario] = useState<number | null>(null);

  const cargar = useCallback(async (id: number) => {
    try {
      const [cursosAsignados, misCursos] = await Promise.all([listarCursosProyecto(id), listarCursos()]);
      setAsignados(cursosAsignados);
      setMios(misCursos);
      if (cursosAsignados.length >= 2) {
        try {
          const [detalles, estudiantes] = await Promise.all([
            Promise.all(cursosAsignados.map((c) => detalleCurso(c.idCurso))),
            listarEstudiantes(id),
          ]);
          setRepetidos(repetidosDelProyecto(cursosAsignados, detalles, estudiantes));
        } catch {
          setRepetidos([]);
        }
      } else {
        setRepetidos([]);
      }
      setCarga({ estado: 'listo' });
    } catch (e) {
      setCarga({ estado: 'error', mensaje: mensajeErrorApi(e, []) });
    }
  }, []);

  useEffect(() => {
    if (idProyecto !== null) {
      void cargar(idProyecto);
    }
  }, [cargar, idProyecto]);

  if (idProyecto === null) {
    return (
      <Tarjeta titulo="Cursos del proyecto">
        <p className="texto-secundario">Guarda el proyecto para asignarle cursos.</p>
      </Tarjeta>
    );
  }

  const opciones = opcionesCursos(mios, asignados);
  const hayActivos = mios.some((c) => !c.archivado);
  const idsAsignados = asignados.map((c) => c.idCurso);

  function abrirEdicion() {
    setError(null);
    setPrevia(null);
    setSeleccion(idsAsignados);
    setEditando(true);
  }

  function cancelarEdicion() {
    setError(null);
    setPrevia(null);
    setEditando(false);
  }

  function alternarCurso(idCurso: number, marcado: boolean) {
    setSeleccion((actual) => (marcado ? [...actual, idCurso] : actual.filter((x) => x !== idCurso)));
  }

  function terminarGuardado(r: ResultadoCursosProyecto) {
    setAsignados(r.cursos);
    setRepetidos(r.repetidos);
    setPrevia(null);
    setEditando(false);
    setError(null);
    avisar('Cursos del proyecto guardados.');
    alCambiarCursos();
  }

  async function guardar(confirmar: boolean) {
    if (idProyecto === null) {
      return;
    }
    setError(null);
    setOcupado(true);
    try {
      const idsCursos = [...seleccion].sort((a, b) => a - b);
      const r = await asignarCursosProyecto(idProyecto, { idsCursos, confirmar });
      if (r.aplicado) {
        terminarGuardado(r);
      } else {
        setPrevia(r);
      }
    } catch (e) {
      setError(mensajeErrorApi(e, [['idsCursos', 'Cursos']]));
      setPrevia(null);
      if (e instanceof ErrorApi && e.estado === 409) {
        await cargar(idProyecto);
      }
    } finally {
      setOcupado(false);
    }
  }

  async function cambiarCursoEstudiante(rep: EstudianteRepetidoVista, nuevoIdCurso: number) {
    if (idProyecto === null || nuevoIdCurso === rep.idCursoQueCuenta) {
      return;
    }
    setErrorRepetido(null);
    setCambiandoUsuario(rep.idUsuario);
    try {
      await elegirCursoEstudiante(idProyecto, rep.idUsuario, nuevoIdCurso);
      setRepetidos((actual) =>
        actual.map((item) => (item.idUsuario === rep.idUsuario ? { ...item, idCursoQueCuenta: nuevoIdCurso } : item)),
      );
      const cursoNuevo = rep.cursos.find((c) => c.idCurso === nuevoIdCurso);
      const etiqueta = cursoNuevo !== undefined ? etiquetaCurso(cursoNuevo) : String(nuevoIdCurso);
      avisar(`${rep.nombre} ahora cuenta con ${etiqueta}.`);
      alCambiarCursos();
    } catch (e) {
      setErrorRepetido(mensajeErrorApi(e, [['idCurso', 'Curso']]));
    } finally {
      setCambiandoUsuario(null);
    }
  }

  return (
    <Tarjeta titulo="Cursos del proyecto">
      {carga.estado === 'cargando' && <p className="texto-secundario">Cargando…</p>}
      {carga.estado === 'error' && <Alerta tipo="error">{carga.mensaje}</Alerta>}
      {carga.estado === 'listo' && (
        <div className="pila">
          {error !== null && <Alerta tipo="error">{error}</Alerta>}

          {!editando && (
            <>
              {asignados.length === 0 ? (
                <div className="pila">
                  <p className="texto-secundario">
                    Este proyecto aún no tiene cursos. Asigna al menos uno para que sus estudiantes participen.
                  </p>
                  {!hayActivos && (
                    <p>
                      <Link className="enlace-tarea" to={rutas.cursos()}>
                        Crea un curso en Cursos
                      </Link>
                    </p>
                  )}
                </div>
              ) : (
                <div className="curso__insignias">
                  {asignados.map((c) => (
                    <Insignia key={c.idCurso}>{`${etiquetaCurso(c)}${c.archivado ? ' · archivado' : ''}`}</Insignia>
                  ))}
                </div>
              )}
              <div>
                <Boton variante="secundario" onClick={abrirEdicion}>
                  Cambiar cursos
                </Boton>
              </div>
            </>
          )}

          {editando && previa === null && (
            <div className="pila">
              <fieldset className="curso__selector">
                <legend className="tarjeta__subtitulo">Elige los cursos</legend>
                {opciones.length === 0 ? (
                  <p className="texto-secundario">
                    No tienes cursos activos.{' '}
                    <Link className="enlace-tarea" to={rutas.cursos()}>
                      Crea un curso en Cursos
                    </Link>
                    .
                  </p>
                ) : (
                  <div className="pila">
                    {opciones.map((c) => (
                      <Casilla
                        key={c.idCurso}
                        checked={seleccion.includes(c.idCurso)}
                        disabled={ocupado}
                        onChange={(e) => alternarCurso(c.idCurso, e.target.checked)}
                      >
                        {`${etiquetaCurso(c)}${c.archivado ? ' (archivado: si lo quitas no podrás volver a asignarlo)' : ''}`}
                      </Casilla>
                    ))}
                  </div>
                )}
              </fieldset>
              <div className="pagina__acciones">
                <Boton
                  onClick={() => void guardar(false)}
                  cargando={ocupado}
                  disabled={mismaSeleccion(seleccion, idsAsignados)}
                >
                  Guardar cursos
                </Boton>
                <Boton variante="secundario" onClick={cancelarEdicion} disabled={ocupado}>
                  Cancelar
                </Boton>
              </div>
            </div>
          )}

          {editando && previa !== null && (
            <div className="pila">
              <Alerta tipo="aviso">{textoConfirmarCursos(previa)}</Alerta>
              {previa.repetidos.length > 0 && (
                <ul className="curso__proyectos">
                  {previa.repetidos.map((rep) => {
                    const cuenta = rep.cursos.find((c) => c.idCurso === rep.idCursoQueCuenta);
                    const etiquetaCuenta = cuenta !== undefined ? etiquetaCurso(cuenta) : String(rep.idCursoQueCuenta);
                    return (
                      <li key={rep.idUsuario}>
                        {`${rep.nombre} — ${listaConY(rep.cursos.map(etiquetaCurso))}; cuenta con ${etiquetaCuenta}`}
                      </li>
                    );
                  })}
                </ul>
              )}
              <div className="pagina__acciones">
                <Boton onClick={() => void guardar(true)} cargando={ocupado}>
                  Confirmar
                </Boton>
                <Boton variante="secundario" onClick={() => setPrevia(null)} disabled={ocupado}>
                  Volver
                </Boton>
              </div>
            </div>
          )}

          {!editando && repetidos.length > 0 && (
            <div className="pila">
              <h3 className="tarjeta__subtitulo">Estudiantes en más de un curso</h3>
              <p className="texto-secundario">
                Cada uno cuenta una sola vez. Elige con qué curso cuenta en este proyecto.
              </p>
              {errorRepetido !== null && <Alerta tipo="error">{errorRepetido}</Alerta>}
              <ul className="equipo__miembros">
                {repetidos.map((rep) => (
                  <li key={rep.idUsuario} className="curso__repetido">
                    <span className="curso__estudiante">
                      <span>{rep.nombre}</span>
                      <span className="texto-secundario">{rep.email}</span>
                    </span>
                    <select
                      className="campo__entrada"
                      aria-label={`Curso con que cuenta ${rep.nombre}`}
                      value={rep.idCursoQueCuenta}
                      disabled={cambiandoUsuario !== null}
                      onChange={(e) => void cambiarCursoEstudiante(rep, Number(e.target.value))}
                    >
                      {rep.cursos.map((c) => (
                        <option key={c.idCurso} value={c.idCurso}>
                          {etiquetaCurso(c)}
                        </option>
                      ))}
                    </select>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </Tarjeta>
  );
}
