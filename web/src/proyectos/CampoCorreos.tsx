import { useId, useRef, useState } from 'react';
import type { ChangeEvent, ClipboardEvent, FocusEvent, KeyboardEvent, MouseEvent } from 'react';
import { MAXIMO_CORREOS_LOTE } from './correos';
import { agregarTexto, filtrarSugerencias, quitar } from './fichas';
import type { Ficha, Sugerencia } from './fichas';

const AYUDA_POR_DEFECTO = 'Escribe un correo y pulsa Enter. También puedes pegar una lista.';

export interface PropsCampoCorreos {
  id?: string;
  etiqueta: string;
  fichas: Ficha[];
  alCambiar: (fichas: Ficha[]) => void;
  ayuda?: string;
  deshabilitado?: boolean;
  /** D-11, T-046: estudiantes sin equipo en el proyecto; se eligen con un clic (filtrados por lo escrito). */
  sugerencias?: Sugerencia[];
}

/**
 * Campo de correos con fichas tipo "Para:" de Gmail: valida al confirmar cada ficha, admite pegar
 * listas separadas por espacios, comas, punto y coma o saltos de línea, y asocia errores al input. Con
 * sugerencias, muestra debajo botones (con desplazamiento si son muchos) que agregan ese correo como ficha.
 */
export function CampoCorreos({
  id,
  etiqueta,
  fichas,
  alCambiar,
  ayuda = AYUDA_POR_DEFECTO,
  deshabilitado = false,
  sugerencias = [],
}: PropsCampoCorreos) {
  const generado = useId();
  const idCampo = id ?? generado;
  const idAyuda = `${idCampo}-ayuda`;
  const idAviso = `${idCampo}-aviso`;
  const idErrores = `${idCampo}-errores`;
  const idSugerencias = `${idCampo}-sugerencias`;

  const [texto, setTexto] = useState('');
  const [avisoLimite, setAvisoLimite] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listaRef = useRef<HTMLUListElement>(null);
  const sugerenciasRef = useRef<HTMLDivElement>(null);
  const visibles = filtrarSugerencias(sugerencias, fichas, texto);

  const fichasConProblema = fichas.filter((f) => f.error !== null || f.resultado !== undefined);
  const tieneErrores = fichasConProblema.length > 0;
  const avisoVisible = fichas.length < MAXIMO_CORREOS_LOTE ? null : avisoLimite;
  const descritos = [
    ayuda !== '' ? idAyuda : null,
    avisoVisible !== null ? idAviso : null,
    tieneErrores ? idErrores : null,
  ]
    .filter(Boolean)
    .join(' ');

  function confirmarTexto(crudo: string) {
    const resultado = agregarTexto(fichas, crudo);
    setAvisoLimite(resultado.aviso);
    setTexto('');
    if (resultado.fichas !== fichas) {
      alCambiar(resultado.fichas);
    }
  }

  function quitarFicha(correo: string) {
    setAvisoLimite(null);
    alCambiar(quitar(fichas, correo));
    inputRef.current?.focus();
  }

  function alCambiarTexto(evento: ChangeEvent<HTMLInputElement>) {
    const valor = evento.target.value;
    if (/[\s,;]/.test(valor)) {
      confirmarTexto(valor);
      return;
    }
    setTexto(valor);
  }

  function alPegar(evento: ClipboardEvent<HTMLInputElement>) {
    const pegado = evento.clipboardData.getData('text/plain');
    if (pegado === '') {
      return;
    }
    if (/[\s,;]/.test(pegado)) {
      evento.preventDefault();
      const combinado = texto.trim() === '' ? pegado : `${texto} ${pegado}`;
      confirmarTexto(combinado);
    }
  }

  function alTecla(evento: KeyboardEvent<HTMLInputElement>) {
    if (deshabilitado) {
      return;
    }
    if (evento.key === 'Enter') {
      if (texto.trim() !== '') {
        evento.preventDefault();
        confirmarTexto(texto);
      }
      return;
    }
    if (evento.key === ',' || evento.key === ' ' || evento.key === ';') {
      evento.preventDefault();
      if (texto.trim() !== '') {
        confirmarTexto(texto);
      }
      return;
    }
    if (evento.key === 'Backspace' && texto === '' && fichas.length > 0) {
      evento.preventDefault();
      quitarFicha(fichas[fichas.length - 1].correo);
    }
  }

  /* Ir con Tab a una sugerencia no convierte en ficha lo escrito para buscarla. */
  function alPerderFoco(evento: FocusEvent<HTMLInputElement>) {
    if (evento.relatedTarget instanceof Node && sugerenciasRef.current?.contains(evento.relatedTarget)) {
      return;
    }
    if (texto.trim() !== '') {
      confirmarTexto(texto);
    }
  }

  function elegirSugerencia(email: string) {
    const resultado = agregarTexto(fichas, email);
    setAvisoLimite(resultado.aviso);
    setTexto('');
    if (resultado.fichas !== fichas) {
      alCambiar(resultado.fichas);
    }
    inputRef.current?.focus();
  }

  function alPulsarContenedor(evento: MouseEvent<HTMLDivElement>) {
    if (deshabilitado) {
      return;
    }
    if (evento.target === evento.currentTarget || evento.target === listaRef.current) {
      evento.preventDefault();
      inputRef.current?.focus();
    }
  }

  const clasesContenedor = [
    'campo-fichas',
    tieneErrores || avisoVisible !== null ? 'campo-fichas--error' : '',
    deshabilitado ? 'campo-fichas--deshabilitado' : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className="campo">
      <label className="campo__etiqueta" htmlFor={idCampo}>
        {etiqueta}
      </label>
      <div className={clasesContenedor} onMouseDown={alPulsarContenedor}>
        {fichas.length > 0 && (
          <ul ref={listaRef} className="campo-fichas__lista" aria-label={etiqueta}>
            {fichas.map((f) => {
              const problema = f.error !== null || f.resultado !== undefined;
              return (
                <li key={f.correo} className={problema ? 'ficha ficha--error' : 'ficha'}>
                  {problema && <span className="solo-lectores">Error: </span>}
                  <span className="ficha__correo">{f.correo}</span>
                  <button
                    type="button"
                    className="ficha__quitar"
                    aria-label={`Quitar ${f.correo}`}
                    disabled={deshabilitado}
                    onMouseDown={(ev) => ev.preventDefault()}
                    onClick={() => quitarFicha(f.correo)}
                  >
                    <span aria-hidden="true">✕</span>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
        <input
          ref={inputRef}
          id={idCampo}
          className="campo-fichas__entrada"
          type="email"
          multiple={false}
          autoComplete="off"
          placeholder={fichas.length === 0 ? 'nombre@uninorte.edu.co' : undefined}
          value={texto}
          disabled={deshabilitado}
          aria-invalid={tieneErrores || avisoVisible !== null}
          aria-describedby={descritos.length > 0 ? descritos : undefined}
          onChange={alCambiarTexto}
          onKeyDown={alTecla}
          onPaste={alPegar}
          onBlur={alPerderFoco}
        />
      </div>
      {ayuda !== '' && (
        <p id={idAyuda} className="texto-secundario campo-fichas__ayuda">
          {ayuda}
        </p>
      )}
      {avisoVisible !== null && (
        <p id={idAviso} className="campo__error" role="status">
          {avisoVisible}
        </p>
      )}
      {tieneErrores && (
        <ul id={idErrores} className="campo-fichas__errores">
          {fichasConProblema.map((f) => (
            <li key={f.correo} className="campo__error">
              {`${f.correo}: ${f.error ?? f.resultado}`}
            </li>
          ))}
        </ul>
      )}
      {visibles.length > 0 && (
        <div ref={sugerenciasRef} className="sugerencias" role="group" aria-labelledby={idSugerencias}>
          <p id={idSugerencias} className="sugerencias__titulo">
            Sugerencias
          </p>
          <ul className="sugerencias__lista">
            {visibles.map((s) => (
              <li key={s.email}>
                <button
                  type="button"
                  className="sugerencia"
                  aria-label={`Agregar ${s.nombre ?? s.email}${s.registrado ? '' : ', sin cuenta'}`}
                  disabled={deshabilitado}
                  onMouseDown={(ev) => ev.preventDefault()}
                  onClick={() => elegirSugerencia(s.email)}
                >
                  <span aria-hidden="true">+</span>
                  <span className="sugerencia__nombre">{s.nombre ?? s.email}</span>
                  {!s.registrado && <span className="sugerencia__nota">· sin cuenta</span>}
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
