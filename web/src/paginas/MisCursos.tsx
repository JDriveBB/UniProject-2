import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router';
import { mensajeDeError } from '../api/cliente';
import { companerosCurso, listarCursos } from '../api/cursos';
import { vistaInscripcion } from '../api/proyectos';
import type { CompaneroVista, CursoVista, InscripcionVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { EstadoVacio } from '../componentes/EstadoVacio';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { estadoProyectoEstudiante, proyectosDeMisCursos } from '../cursos/vista';
import { fechaHora } from '../formato';
import { rutas } from '../rutas';

type EstadoCarga =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | {
      estado: 'listo';
      cursos: CursoVista[];
      inscripciones: Map<number, InscripcionVista>;
      companeros: Map<number, CompaneroVista[] | null>;
    };

/** /mis-cursos (solo ESTUDIANTE): cursos, proyectos con estado de inscripción y compañeros (solo nombres). */
export function MisCursos() {
  const [carga, setCarga] = useState<EstadoCarga>({ estado: 'cargando' });

  const cargar = useCallback(async () => {
    let cursos: CursoVista[];
    try {
      cursos = await listarCursos();
    } catch (e) {
      setCarga({ estado: 'error', mensaje: mensajeDeError(e) });
      return;
    }
    const proyectos = proyectosDeMisCursos(cursos);
    const [resInscripciones, resCompaneros] = await Promise.all([
      Promise.allSettled(proyectos.map((p) => vistaInscripcion(p.idProyecto))),
      Promise.allSettled(cursos.map((c) => companerosCurso(c.idCurso))),
    ]);
    const inscripciones = new Map<number, InscripcionVista>();
    proyectos.forEach((p, i) => {
      const r = resInscripciones[i];
      if (r.status === 'fulfilled') {
        inscripciones.set(p.idProyecto, r.value);
      }
    });
    const companeros = new Map<number, CompaneroVista[] | null>();
    cursos.forEach((c, i) => {
      const r = resCompaneros[i];
      companeros.set(c.idCurso, r.status === 'fulfilled' ? r.value : null);
    });
    setCarga({ estado: 'listo', cursos, inscripciones, companeros });
  }, []);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  function reintentar() {
    setCarga({ estado: 'cargando' });
    void cargar();
  }

  return (
    <div className="pagina">
      <EncabezadoPagina
        titulo="Mis cursos"
        descripcion="Tus cursos, tus compañeros y los proyectos en los que participas."
      />
      {carga.estado === 'cargando' && <Cargando />}
      {carga.estado === 'error' && (
        <div className="pila">
          <Alerta tipo="error">{carga.mensaje}</Alerta>
          <div>
            <Boton variante="secundario" onClick={reintentar}>
              Reintentar
            </Boton>
          </div>
        </div>
      )}
      {carga.estado === 'listo' && carga.cursos.length === 0 && (
        <EstadoVacio titulo="Aún no estás en ningún curso">
          Cuando tu docente te agregue a un curso, lo verás aquí con sus proyectos.
        </EstadoVacio>
      )}
      {carga.estado === 'listo' && carga.cursos.length > 0 && (
        <div className="pila">
          {carga.cursos.map((c) => {
            const listaCompaneros = carga.companeros.get(c.idCurso) ?? null;
            return (
              <Tarjeta key={c.idCurso}>
                <header className="tarjeta-proyecto__cabecera">
                  <div className="tarjeta-proyecto__encabezado">
                    <h2 className="tarjeta__titulo tarjeta-proyecto__titulo">{c.nombre}</h2>
                    <p className="texto-secundario">{`${c.periodo} · Docente: ${c.nombreDocente}`}</p>
                  </div>
                  {c.archivado && <Insignia>Archivado</Insignia>}
                </header>
                {c.descripcion !== null && c.descripcion !== '' && <p>{c.descripcion}</p>}

                <h3 className="tarjeta__subtitulo">Proyectos</h3>
                {c.proyectos.length === 0 ? (
                  <p className="texto-secundario">Este curso aún no tiene proyectos.</p>
                ) : (
                  <ul className="curso__proyectos">
                    {c.proyectos.map((p) => {
                      const estado = estadoProyectoEstudiante(carga.inscripciones.get(p.idProyecto));
                      return (
                        <li key={p.idProyecto} className="curso__proyecto">
                          <div className="pila">
                            <Link className="enlace-tarea" to={rutas.proyecto(p.idProyecto)}>
                              {p.nombre}
                            </Link>
                            {estado.tipo === 'conEquipo' && (
                              <>
                                <p className="texto-secundario">{`Tu equipo: ${estado.nombreEquipo}`}</p>
                                <div className="pagina__acciones">
                                  <Link
                                    className="boton boton--primario"
                                    to={rutas.tableroEquipo(estado.idEquipo)}
                                    aria-label={`Abrir tablero de ${p.nombre}`}
                                  >
                                    Abrir tablero
                                  </Link>
                                  <Link
                                    className="boton boton--secundario"
                                    to={rutas.votacionEquipo(estado.idEquipo)}
                                    aria-label={`Votación de líder de ${p.nombre}`}
                                  >
                                    Votación de líder
                                  </Link>
                                </div>
                              </>
                            )}
                            {estado.tipo === 'abierta' && (
                              <>
                                <p className="texto-secundario">
                                  {`La inscripción a equipos está abierta hasta el ${fechaHora(estado.cierre)}.`}
                                </p>
                                <div className="pagina__acciones">
                                  <Link
                                    className="boton boton--primario"
                                    to={rutas.inscripcionProyecto(p.idProyecto)}
                                    aria-label={`Elegir equipo en ${p.nombre}`}
                                  >
                                    Elegir equipo
                                  </Link>
                                </div>
                              </>
                            )}
                            {estado.tipo === 'cerrada' && (
                              <>
                                <p className="texto-secundario">Aún no tienes equipo. Tu docente te asignará uno.</p>
                                <div className="pagina__acciones">
                                  <Link
                                    className="boton boton--secundario"
                                    to={rutas.inscripcionProyecto(p.idProyecto)}
                                    aria-label={`Ver equipos de ${p.nombre}`}
                                  >
                                    Ver equipos
                                  </Link>
                                </div>
                              </>
                            )}
                            {estado.tipo === 'sinFecha' && (
                              <>
                                <p className="texto-secundario">
                                  Aún no tienes equipo. La inscripción a equipos todavía no está abierta.
                                </p>
                                <div className="pagina__acciones">
                                  <Link
                                    className="boton boton--secundario"
                                    to={rutas.inscripcionProyecto(p.idProyecto)}
                                    aria-label={`Ver equipos de ${p.nombre}`}
                                  >
                                    Ver equipos
                                  </Link>
                                </div>
                              </>
                            )}
                            {estado.tipo === 'desconocido' && (
                              <div className="pagina__acciones">
                                <Link
                                  className="boton boton--secundario"
                                  to={rutas.proyecto(p.idProyecto)}
                                  aria-label={`Ver proyecto ${p.nombre}`}
                                >
                                  Ver proyecto
                                </Link>
                              </div>
                            )}
                          </div>
                        </li>
                      );
                    })}
                  </ul>
                )}

                <details className="curso__companeros">
                  <summary className="leyenda-marcas__resumen">
                    {listaCompaneros === null ? 'Compañeros' : `Compañeros (${listaCompaneros.length})`}
                  </summary>
                  {listaCompaneros === null ? (
                    <p className="texto-secundario">No se pudieron cargar los compañeros.</p>
                  ) : listaCompaneros.length === 0 ? (
                    <p className="texto-secundario">Sin compañeros registrados.</p>
                  ) : (
                    <ul className="curso__lista-companeros">
                      {listaCompaneros.map((comp, idx) => (
                        <li key={`${comp.nombre}-${idx}`}>{comp.nombre}</li>
                      ))}
                    </ul>
                  )}
                </details>
              </Tarjeta>
            );
          })}
        </div>
      )}
    </div>
  );
}
