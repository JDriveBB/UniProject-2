import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { mensajeDeError } from '../api/cliente';
import { listarCursos } from '../api/cursos';
import type { CursoVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { EstadoVacio } from '../componentes/EstadoVacio';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { FormularioCurso } from '../cursos/FormularioCurso';
import { listaConY, separarActivos, textoEstudiantes } from '../cursos/vista';
import { rutas } from '../rutas';

type EstadoLista =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; cursos: CursoVista[] };

function TarjetaCurso({ curso }: { curso: CursoVista }) {
  const textoProyectos =
    curso.proyectos.length === 0
      ? 'Sin proyectos'
      : `Proyectos: ${listaConY(curso.proyectos.map((p) => p.nombre))}`;
  return (
    <div className="tarjeta-proyecto">
      <Tarjeta>
        <header className="tarjeta-proyecto__cabecera">
          <div className="tarjeta-proyecto__encabezado">
            <h2 className="tarjeta__titulo tarjeta-proyecto__titulo">{curso.nombre}</h2>
            <p className="texto-secundario tarjeta-proyecto__dato">{curso.periodo}</p>
          </div>
          {curso.archivado && <Insignia>Archivado</Insignia>}
        </header>
        <p className="texto-secundario tarjeta-proyecto__dato">{textoEstudiantes(curso.estudiantes)}</p>
        <p className="texto-secundario tarjeta-proyecto__dato">{textoProyectos}</p>
        <div className="pagina__acciones tarjeta-proyecto__acciones">
          <Link
            className="boton boton--secundario"
            to={rutas.curso(curso.idCurso)}
            aria-label={`Abrir el curso ${curso.nombre}`}
          >
            Abrir
          </Link>
        </div>
      </Tarjeta>
    </div>
  );
}

/** /cursos (solo DOCENTE): listas de estudiantes reutilizables en proyectos. */
export function ListaCursos() {
  const navegar = useNavigate();
  const [lista, setLista] = useState<EstadoLista>({ estado: 'cargando' });
  const [abierto, setAbierto] = useState(false);

  const cargar = useCallback(async () => {
    try {
      setLista({ estado: 'listo', cursos: await listarCursos() });
    } catch (e) {
      setLista({ estado: 'error', mensaje: mensajeDeError(e) });
    }
  }, []);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  function reintentar() {
    setLista({ estado: 'cargando' });
    void cargar();
  }

  const grupos = lista.estado === 'listo' ? separarActivos(lista.cursos) : { activos: [], archivados: [] };

  return (
    <div className="pagina">
      <EncabezadoPagina
        titulo="Cursos"
        descripcion="Tus listas de estudiantes. Asigna un curso a un proyecto para que sus estudiantes participen."
      >
        <div className="pagina__acciones">
          <Boton onClick={() => setAbierto(true)}>Nuevo curso</Boton>
        </div>
      </EncabezadoPagina>
      {lista.estado === 'cargando' && <Cargando />}
      {lista.estado === 'error' && (
        <div className="pila">
          <Alerta tipo="error">{lista.mensaje}</Alerta>
          <div>
            <Boton variante="secundario" onClick={reintentar}>
              Reintentar
            </Boton>
          </div>
        </div>
      )}
      {lista.estado === 'listo' && lista.cursos.length === 0 && (
        <EstadoVacio
          titulo="Aún no tienes cursos"
          accion={<Boton onClick={() => setAbierto(true)}>Nuevo curso</Boton>}
        >
          Crea un curso, pega los correos de tus estudiantes y asígnalo a tus proyectos.
        </EstadoVacio>
      )}
      {lista.estado === 'listo' && grupos.activos.length > 0 && (
        <div className="rejilla-proyectos">
          {grupos.activos.map((c) => (
            <TarjetaCurso key={c.idCurso} curso={c} />
          ))}
        </div>
      )}
      {lista.estado === 'listo' && grupos.archivados.length > 0 && (
        <div className="pila">
          <h2 className="tarjeta__titulo">Archivados</h2>
          <div className="rejilla-proyectos">
            {grupos.archivados.map((c) => (
              <TarjetaCurso key={c.idCurso} curso={c} />
            ))}
          </div>
        </div>
      )}
      {abierto && (
        <FormularioCurso
          inicial={null}
          alCerrar={() => setAbierto(false)}
          alGuardado={(c) => {
            setAbierto(false);
            navegar(rutas.curso(c.idCurso), { state: { cursoCreado: true } });
          }}
        />
      )}
    </div>
  );
}
