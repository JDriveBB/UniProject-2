import { useId, useState, type FormEvent } from 'react';
import { comentarTarea } from '../api/tareas';
import type { ComentarioVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { fechaCorta } from '../formato';
import { etiquetaRol } from '../marco/navegacion';
import type { Mensaje } from '../proyectos/errores';
import { CAMPOS_COMENTARIO, validarTexto } from './formularios';
import { mensajeErrorTarea, SIN_PERMISO_COMENTAR } from './vista';

export interface PropsTarjetaComentarios {
  idTarea: number;
  comentarios: ComentarioVista[];
  puedeComentar: boolean;
  alHecho: (m: Mensaje) => void;
}

export function TarjetaComentarios({
  idTarea,
  comentarios,
  puedeComentar,
  alHecho,
}: PropsTarjetaComentarios) {
  const [texto, setTexto] = useState('');
  const [intentado, setIntentado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [errorApi, setErrorApi] = useState<string | null>(null);

  const idTextarea = useId();
  const idError = idTextarea + '-error';

  const errorTexto = intentado ? validarTexto(texto, 2000, 'Escribe el comentario.') : undefined;

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setIntentado(true);
    const err = validarTexto(texto, 2000, 'Escribe el comentario.');
    if (err !== undefined) {
      document.getElementById(idTextarea)?.focus();
      return;
    }

    setGuardando(true);
    setErrorApi(null);
    try {
      await comentarTarea(idTarea, texto.trim());
      setTexto('');
      setIntentado(false);
      alHecho({ tipo: 'exito', texto: 'Comentario publicado.' });
    } catch (errApi) {
      setErrorApi(mensajeErrorTarea(errApi, CAMPOS_COMENTARIO, SIN_PERMISO_COMENTAR));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Tarjeta titulo="Comentarios">
      {comentarios.length === 0 ? (
        <p className="pagina__vacio">Aún no hay comentarios.</p>
      ) : (
        <ul className="lista-comentarios">
          {comentarios.map((c) => (
            <li key={c.idComentario} className="comentario">
              <p className="comentario__cabecera">
                <span className="comentario__autor">{c.autor}</span>
                <Insignia tono="neutro">{etiquetaRol(c.rolAutor)}</Insignia>
                <span className="comentario__fecha">{fechaCorta(c.fechaHora)}</span>
              </p>
              <p className="comentario__texto">{c.contenido}</p>
            </li>
          ))}
        </ul>
      )}

      {puedeComentar && (
        <form className="formulario-comentario" noValidate onSubmit={(e) => void enviar(e)}>
          {errorApi !== null && <Alerta tipo="error">{errorApi}</Alerta>}
          <div>
            <textarea
              id={idTextarea}
              className="campo__entrada campo__entrada--area campo__entrada--comentario"
              aria-label="Nuevo comentario"
              placeholder="Escribe un comentario…"
              value={texto}
              disabled={guardando}
              aria-invalid={errorTexto !== undefined}
              aria-describedby={errorTexto !== undefined ? idError : undefined}
              onChange={(e) => setTexto(e.target.value)}
            />
            {errorTexto !== undefined && (
              <p id={idError} className="campo__error">
                {errorTexto}
              </p>
            )}
          </div>
          <div className="formulario-comentario__acciones">
            <Boton type="submit" variante="primario" cargando={guardando}>
              Comentar
            </Boton>
          </div>
        </form>
      )}
    </Tarjeta>
  );
}
