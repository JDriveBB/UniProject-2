import { useCallback, useEffect, useId, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router';
import { ErrorApi } from '../api/cliente';
import {
  agregarCorreosCurso,
  borrarCurso,
  cambiarArchivadoCurso,
  detalleCurso,
  generarEnlaceCurso,
  quitarEstudianteCurso,
  retirarCorreoCurso,
} from '../api/cursos';
import type { CursoDetalle, EstudianteCursoVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { useAvisos } from '../componentes/Avisos';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { ConfirmarQuitar } from '../componentes/ConfirmarQuitar';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { Tarjeta } from '../componentes/Tarjeta';
import { FormularioCurso } from '../cursos/FormularioCurso';
import {
  mensajeErrorCargaCurso,
  preguntaArchivarCurso,
  preguntaBorrarCurso,
  preguntaQuitarDelCurso,
  textoEstudiantes,
} from '../cursos/vista';
import { fechaCorta } from '../formato';
import { CampoCorreos } from '../proyectos/CampoCorreos';
import { CorreosPendientes } from '../proyectos/CorreosPendientes';
import { CAMPOS_CORREO, mensajeErrorApi } from '../proyectos/errores';
import type { Mensaje } from '../proyectos/errores';
import { aplicarResultadoCurso, validas } from '../proyectos/fichas';
import type { Ficha } from '../proyectos/fichas';
import { preguntaRetirarCorreoCurso } from '../proyectos/quitar';
import { idDeRuta } from '../proyectos/vista';
import { rutas } from '../rutas';

type EstadoCarga =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; detalle: CursoDetalle };

function vieneDeCreacionCurso(estado: unknown): boolean {
  return (
    typeof estado === 'object' &&
    estado !== null &&
    'cursoCreado' in estado &&
    (estado as { cursoCreado: unknown }).cursoCreado === true
  );
}

/** /cursos/:idCurso: una instancia por id para no mezclar estados. */
export function DetalleCursoDeRuta() {
  const { idCurso } = useParams();
  return <DetalleCurso key={idCurso} idRuta={idCurso ?? ''} />;
}

/** Detalle de un curso del docente: datos, estudiantes con cuenta y correos pendientes. */
export function DetalleCurso({ idRuta }: { idRuta: string }) {
  const avisar = useAvisos();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const base = useId();
  const id = idDeRuta(idRuta);

  const [carga, setCarga] = useState<EstadoCarga>(() =>
    id === null ? { estado: 'error', mensaje: 'Este curso no existe.' } : { estado: 'cargando' },
  );
  const [mensajeGeneral, setMensajeGeneral] = useState<Mensaje | null>(() =>
    vieneDeCreacionCurso(ubicacion.state)
      ? { tipo: 'exito', texto: 'Curso creado. Ahora agrega a sus estudiantes.' }
      : null,
  );
  const [editando, setEditando] = useState(false);
  const [confirmarAccion, setConfirmarAccion] = useState<'archivar' | 'borrar' | null>(null);
  const [ocupadoAccion, setOcupadoAccion] = useState<'archivar' | 'desarchivar' | 'borrar' | null>(null);

  const [abiertoAgregar, setAbiertoAgregar] = useState(false);
  const [fichas, setFichas] = useState<Ficha[]>([]);
  const [ocupadoEstudiantes, setOcupadoEstudiantes] = useState(false);
  const [mensajeEstudiantes, setMensajeEstudiantes] = useState<Mensaje | null>(null);
  const [porQuitar, setPorQuitar] = useState<EstudianteCursoVista | null>(null);
  const [versionPendientes, setVersionPendientes] = useState(0);

  const volverA = useRef<string | null>(null);
  const focoEnAlerta = useRef(false);
  const enfocarEn = useRef<'campo' | 'agregar' | null>(null);
  const idArea = `${base}-correos`;
  const idAgregar = `${base}-agregar`;
  const idMensajeEstudiantes = `${base}-mensaje-estudiantes`;
  const idTituloEstudiantes = `${base}-estudiantes`;
  const idQuitar = (idUsuario: number) => `${base}-quitar-${idUsuario}`;

  const cargar = useCallback(async () => {
    if (id === null) {
      return;
    }
    try {
      setCarga({ estado: 'listo', detalle: await detalleCurso(id) });
    } catch (e) {
      setCarga({ estado: 'error', mensaje: mensajeErrorCargaCurso(e) });
    }
  }, [id]);

  const recargar = useCallback(() => {
    void cargar();
  }, [cargar]);

  const cargarPendientes = useCallback(async () => {
    if (id === null) {
      return [];
    }
    return (await detalleCurso(id)).pendientes;
  }, [id]);

  const generarEnlace = useCallback(
    (idCorreoHabilitado: number) => {
      if (id === null) {
        return Promise.reject(new Error('Curso inválido'));
      }
      return generarEnlaceCurso(id, idCorreoHabilitado);
    },
    [id],
  );

  const retirarPendiente = useCallback(
    (idCorreoHabilitado: number) => {
      if (id === null) {
        return Promise.reject(new Error('Curso inválido'));
      }
      return retirarCorreoCurso(id, idCorreoHabilitado);
    },
    [id],
  );

  useEffect(() => {
    if (id !== null) {
      void cargar();
    }
  }, [cargar, id]);

  useEffect(() => {
    if (porQuitar === null && volverA.current !== null) {
      (document.getElementById(volverA.current) ?? document.getElementById(idTituloEstudiantes))?.focus();
      volverA.current = null;
    }
  }, [porQuitar, idTituloEstudiantes]);

  useEffect(() => {
    if (mensajeEstudiantes !== null && focoEnAlerta.current) {
      document.getElementById(idMensajeEstudiantes)?.focus();
      focoEnAlerta.current = false;
    }
  }, [mensajeEstudiantes, idMensajeEstudiantes]);

  useEffect(() => {
    if (abiertoAgregar && enfocarEn.current === 'campo') {
      document.getElementById(idArea)?.focus();
      enfocarEn.current = null;
    } else if (!abiertoAgregar && enfocarEn.current === 'agregar') {
      document.getElementById(idAgregar)?.focus();
      enfocarEn.current = null;
    }
  }, [abiertoAgregar, idArea, idAgregar]);

  if (carga.estado === 'cargando') {
    return <Cargando />;
  }
  if (carga.estado === 'error' || id === null) {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Curso" />
        <Alerta tipo="error">{carga.estado === 'error' ? carga.mensaje : 'Este curso no existe.'}</Alerta>
      </div>
    );
  }

  const { curso, estudiantes } = carga.detalle;
  const correosValidos = validas(fichas);
  const hayErrorFormato = fichas.some((f) => f.error !== null);
  const puedeEnviar = correosValidos.length > 0 && !hayErrorFormato;
  const textoBotonAgregar =
    correosValidos.length === 0
      ? 'Agregar estudiantes'
      : correosValidos.length === 1
        ? 'Agregar 1 estudiante'
        : `Agregar ${correosValidos.length} estudiantes`;
  const bloqueadoCabecera = ocupadoAccion !== null || confirmarAccion !== null;
  const bloqueadoEstudiantes = ocupadoEstudiantes || porQuitar !== null;

  async function alArchivar() {
    if (id === null) {
      return;
    }
    setMensajeGeneral(null);
    setOcupadoAccion('archivar');
    try {
      await cambiarArchivadoCurso(id, true);
      setConfirmarAccion(null);
      setAbiertoAgregar(false);
      setFichas([]);
      avisar('Curso archivado.');
      await cargar();
    } catch (e) {
      setMensajeGeneral({ tipo: 'error', texto: mensajeErrorApi(e, []) });
      setConfirmarAccion(null);
    } finally {
      setOcupadoAccion(null);
    }
  }

  async function alDesarchivar() {
    if (id === null) {
      return;
    }
    setMensajeGeneral(null);
    setOcupadoAccion('desarchivar');
    try {
      await cambiarArchivadoCurso(id, false);
      avisar('Curso desarchivado.');
      await cargar();
    } catch (e) {
      setMensajeGeneral({ tipo: 'error', texto: mensajeErrorApi(e, []) });
    } finally {
      setOcupadoAccion(null);
    }
  }

  async function alBorrar() {
    if (id === null) {
      return;
    }
    setMensajeGeneral(null);
    setOcupadoAccion('borrar');
    try {
      await borrarCurso(id);
      avisar('Curso borrado.');
      navegar(rutas.cursos());
    } catch (e) {
      setMensajeGeneral({ tipo: 'error', texto: mensajeErrorApi(e, []) });
      setConfirmarAccion(null);
    } finally {
      setOcupadoAccion(null);
    }
  }

  function abrirAgregar() {
    setMensajeEstudiantes(null);
    enfocarEn.current = 'campo';
    setAbiertoAgregar(true);
  }

  function cerrarAgregar() {
    enfocarEn.current = 'agregar';
    setAbiertoAgregar(false);
    setFichas([]);
  }

  async function alAgregarEstudiantes(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (!puedeEnviar || ocupadoEstudiantes || id === null) {
      return;
    }
    setMensajeEstudiantes(null);
    setOcupadoEstudiantes(true);
    try {
      const r = await agregarCorreosCurso(
        id,
        correosValidos.map((f) => f.correo),
      );
      const aplicado = aplicarResultadoCurso(fichas, r);
      setFichas(aplicado.fichas);
      if (aplicado.fichas.length === 0) {
        setAbiertoAgregar(false);
      }
      setMensajeEstudiantes({
        tipo: aplicado.fichas.length === 0 ? 'exito' : 'aviso',
        texto: aplicado.resumen,
      });
      focoEnAlerta.current = true;
      await cargar();
      setVersionPendientes((v) => v + 1);
    } catch (e) {
      setMensajeEstudiantes({ tipo: 'error', texto: mensajeErrorApi(e, CAMPOS_CORREO) });
      focoEnAlerta.current = true;
    } finally {
      setOcupadoEstudiantes(false);
    }
  }

  async function alConfirmarQuitar(est: EstudianteCursoVista) {
    if (id === null) {
      return;
    }
    setOcupadoEstudiantes(true);
    setMensajeEstudiantes(null);
    try {
      await quitarEstudianteCurso(id, est.idUsuario);
      avisar(`${est.nombre} salió del curso.`);
      volverA.current = idTituloEstudiantes;
      await cargar();
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 404) {
        volverA.current = idTituloEstudiantes;
        await cargar();
      } else {
        volverA.current = idQuitar(est.idUsuario);
        setMensajeEstudiantes({ tipo: 'error', texto: mensajeErrorApi(e, []) });
      }
    } finally {
      setOcupadoEstudiantes(false);
      setPorQuitar(null);
    }
  }

  function alCancelarQuitar(est: EstudianteCursoVista) {
    volverA.current = idQuitar(est.idUsuario);
    setPorQuitar(null);
  }

  return (
    <div className="pagina">
      <EncabezadoPagina
        titulo={curso.nombre}
        descripcion={`${curso.periodo} · ${textoEstudiantes(curso.estudiantes)}`}
      >
        <div className="pagina__acciones">
          {!curso.archivado && (
            <Boton variante="secundario" onClick={() => setEditando(true)} disabled={bloqueadoCabecera}>
              Editar
            </Boton>
          )}
          {curso.archivado ? (
            <Boton
              variante="secundario"
              onClick={() => void alDesarchivar()}
              cargando={ocupadoAccion === 'desarchivar'}
              disabled={bloqueadoCabecera}
            >
              Desarchivar
            </Boton>
          ) : (
            <Boton
              variante="secundario"
              onClick={() => setConfirmarAccion('archivar')}
              disabled={bloqueadoCabecera}
            >
              Archivar
            </Boton>
          )}
          {curso.proyectos.length === 0 && (
            <Boton
              variante="peligro"
              onClick={() => setConfirmarAccion('borrar')}
              disabled={bloqueadoCabecera}
            >
              Borrar
            </Boton>
          )}
        </div>
      </EncabezadoPagina>

      {mensajeGeneral !== null && <Alerta tipo={mensajeGeneral.tipo}>{mensajeGeneral.texto}</Alerta>}

      {confirmarAccion === 'archivar' && (
        <ConfirmarQuitar
          pregunta={preguntaArchivarCurso(curso.nombre)}
          ocupado={ocupadoAccion === 'archivar'}
          textoConfirmar="Sí, archivar"
          textoOcupado="Archivando…"
          alConfirmar={() => void alArchivar()}
          alCancelar={() => setConfirmarAccion(null)}
        />
      )}

      {confirmarAccion === 'borrar' && (
        <ConfirmarQuitar
          pregunta={preguntaBorrarCurso(curso.nombre)}
          ocupado={ocupadoAccion === 'borrar'}
          textoConfirmar="Sí, borrar"
          textoOcupado="Borrando…"
          alConfirmar={() => void alBorrar()}
          alCancelar={() => setConfirmarAccion(null)}
        />
      )}

      {curso.archivado && (
        <Alerta tipo="aviso">Este curso está archivado. Desarchívalo para editarlo o agregar estudiantes.</Alerta>
      )}

      <Tarjeta titulo="Datos del curso">
        <p>{curso.descripcion ?? 'Sin descripción'}</p>
        <p className="texto-secundario">{`Docente: ${curso.nombreDocente}`}</p>
        {curso.proyectos.length === 0 ? (
          <p className="texto-secundario">
            Aún no se usa en ningún proyecto. Asígnalo desde la configuración de un proyecto.
          </p>
        ) : (
          <ul className="curso__proyectos">
            {curso.proyectos.map((p) => (
              <li key={p.idProyecto}>
                <Link className="enlace-tarea" to={rutas.proyecto(p.idProyecto)}>
                  {p.nombre}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Tarjeta>

      <Tarjeta titulo="Estudiantes">
        <div id={idTituloEstudiantes} tabIndex={-1} className="pila">
          {!curso.archivado &&
            (abiertoAgregar ? (
              <form className="pila" onSubmit={(ev) => void alAgregarEstudiantes(ev)} noValidate>
                <CampoCorreos
                  id={idArea}
                  etiqueta="Correos de los estudiantes"
                  fichas={fichas}
                  alCambiar={setFichas}
                  deshabilitado={ocupadoEstudiantes}
                />
                <div className="pagina__acciones">
                  <Boton type="submit" cargando={ocupadoEstudiantes} disabled={!puedeEnviar}>
                    {textoBotonAgregar}
                  </Boton>
                  <Boton variante="secundario" onClick={cerrarAgregar} disabled={ocupadoEstudiantes}>
                    Cancelar
                  </Boton>
                </div>
              </form>
            ) : (
              <div>
                <Boton id={idAgregar} variante="secundario" onClick={abrirAgregar}>
                  + Agregar estudiantes
                </Boton>
              </div>
            ))}

          {mensajeEstudiantes !== null && (
            <Alerta id={idMensajeEstudiantes} tabIndex={-1} tipo={mensajeEstudiantes.tipo}>
              {mensajeEstudiantes.texto}
            </Alerta>
          )}

          {porQuitar !== null && (
            <ConfirmarQuitar
              key={porQuitar.idUsuario}
              pregunta={preguntaQuitarDelCurso(porQuitar.nombre)}
              ocupado={ocupadoEstudiantes}
              alConfirmar={() => void alConfirmarQuitar(porQuitar)}
              alCancelar={() => alCancelarQuitar(porQuitar)}
            />
          )}

          {estudiantes.length === 0 ? (
            <p className="texto-secundario">Aún no hay estudiantes con cuenta en este curso.</p>
          ) : (
            <ul className="equipo__miembros">
              {estudiantes.map((est) => (
                <li key={est.idUsuario} className="equipo__miembro">
                  <span className="curso__estudiante">
                    <span>{est.nombre}</span>
                    <span className="texto-secundario">{est.email}</span>
                    <span className="texto-secundario">{`Desde ${fechaCorta(est.fechaIngreso)}`}</span>
                  </span>
                  <button
                    id={idQuitar(est.idUsuario)}
                    type="button"
                    className="boton-texto boton-texto--peligro"
                    aria-label={`Quitar a ${est.nombre} del curso`}
                    disabled={bloqueadoEstudiantes}
                    onClick={() => setPorQuitar(est)}
                  >
                    Quitar
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </Tarjeta>

      <Tarjeta>
        <CorreosPendientes
          titulo="Correos sin cuenta"
          version={versionPendientes}
          cargar={cargarPendientes}
          generarEnlace={curso.archivado ? null : generarEnlace}
          retirar={retirarPendiente}
          preguntaRetirar={preguntaRetirarCorreoCurso}
          textoVacio="No hay correos pendientes. Aquí aparecen los correos sin cuenta hasta que la persona cree su cuenta con su enlace."
          alCambiarHabilitaciones={recargar}
        />
      </Tarjeta>

      {editando && (
        <FormularioCurso
          inicial={curso}
          alCerrar={() => setEditando(false)}
          alGuardado={() => {
            setEditando(false);
            avisar('Curso actualizado.');
            void cargar();
          }}
        />
      )}
    </div>
  );
}
