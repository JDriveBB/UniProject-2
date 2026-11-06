import { useId, useState } from 'react';
import { adjuntarArchivo, adjuntarEnlace } from '../api/tareas';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Modal } from '../componentes/Modal';
import type { Mensaje } from '../proyectos/errores';
import {
  ACEPTA_ARCHIVO,
  CAMPOS_ENLACE,
  validarArchivo,
  validarEnlace,
} from './formularios';
import { mensajeErrorTarea, nombreEvidencia, SIN_PERMISO_EVIDENCIA } from './vista';

export interface PropsModalEvidencia {
  idTarea: number;
  nombre: string;
  nuevaVersion: boolean;
  conCorreccion: boolean;
  alCerrar: () => void;
  alGuardar: (m: Mensaje) => void;
}

export function ModalEvidencia({
  idTarea,
  nombre,
  nuevaVersion,
  conCorreccion,
  alCerrar,
  alGuardar,
}: PropsModalEvidencia) {
  const [tipo, setTipo] = useState<'archivo' | 'enlace'>('archivo');
  const [archivo, setArchivo] = useState<File | null>(null);
  const [url, setUrl] = useState('');
  const [intentado, setIntentado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [errorApi, setErrorApi] = useState<string | null>(null);

  const nombreRadio = useId();
  const idArchivo = useId();
  const idErrorArchivo = idArchivo + '-error';
  const idEnlace = useId();

  const errorArchivo = intentado && tipo === 'archivo' ? validarArchivo(archivo) : undefined;
  const errorEnlace = intentado && tipo === 'enlace' ? validarEnlace(url) : undefined;

  async function enviar() {
    setIntentado(true);
    if (tipo === 'archivo') {
      const err = validarArchivo(archivo);
      if (err !== undefined) {
        document.getElementById(idArchivo)?.focus();
        return;
      }
    } else {
      const err = validarEnlace(url);
      if (err !== undefined) {
        document.getElementById(idEnlace)?.focus();
        return;
      }
    }

    setGuardando(true);
    setErrorApi(null);
    try {
      const ev =
        tipo === 'archivo' && archivo !== null
          ? await adjuntarArchivo(idTarea, archivo)
          : await adjuntarEnlace(idTarea, url.trim());

      const sufijo = conCorreccion ? ' La corrección quedó atendida.' : '';
      alGuardar({
        tipo: 'exito',
        texto: `Evidencia «${nombreEvidencia(ev)}» adjuntada.${sufijo}`,
      });
    } catch (err) {
      setErrorApi(mensajeErrorTarea(err, CAMPOS_ENLACE, SIN_PERMISO_EVIDENCIA));
    } finally {
      setGuardando(false);
    }
  }

  const titulo = nuevaVersion ? 'Adjuntar nueva versión' : 'Adjuntar evidencia';

  return (
    <Modal
      ancho="normal"
      titulo={titulo}
      subtitulo={nombre}
      ocupado={guardando}
      alCerrar={alCerrar}
      alEnviar={() => void enviar()}
      acciones={(cerrar) => (
        <>
          <Boton type="button" variante="secundario" onClick={cerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" variante="primario" cargando={guardando}>
            Adjuntar
          </Boton>
        </>
      )}
    >
      {errorApi !== null && <Alerta tipo="error">{errorApi}</Alerta>}

      <fieldset className="opciones-radio">
        <legend className="campo__etiqueta opciones-radio__titulo">Tipo de evidencia</legend>
        <label className="opcion-radio">
          <input
            type="radio"
            className="opcion-radio__entrada"
            name={nombreRadio}
            value="archivo"
            checked={tipo === 'archivo'}
            disabled={guardando}
            onChange={() => {
              setTipo('archivo');
              setUrl('');
            }}
          />
          <span>Archivo</span>
        </label>
        <label className="opcion-radio">
          <input
            type="radio"
            className="opcion-radio__entrada"
            name={nombreRadio}
            value="enlace"
            checked={tipo === 'enlace'}
            disabled={guardando}
            onChange={() => {
              setTipo('enlace');
              setArchivo(null);
            }}
          />
          <span>Enlace</span>
        </label>
      </fieldset>

      {tipo === 'archivo' ? (
        <div className="campo">
          <label className="campo__etiqueta" htmlFor={idArchivo}>
            Archivo
          </label>
          <input
            id={idArchivo}
            type="file"
            className="campo__entrada"
            accept={ACEPTA_ARCHIVO}
            disabled={guardando}
            aria-invalid={errorArchivo !== undefined}
            aria-describedby={errorArchivo !== undefined ? idErrorArchivo : undefined}
            onChange={(e) => setArchivo(e.target.files?.[0] ?? null)}
          />
          <p className="texto-ayuda modal__ayuda">
            PDF, PNG o JPEG de hasta 10 MB. Si pesa más, súbelo a un repositorio y adjunta el enlace.
          </p>
          {errorArchivo !== undefined && (
            <p id={idErrorArchivo} className="campo__error">
              {errorArchivo}
            </p>
          )}
        </div>
      ) : (
        <Campo
          id={idEnlace}
          etiqueta="Enlace"
          type="url"
          inputMode="url"
          value={url}
          error={errorEnlace}
          disabled={guardando}
          onChange={(e) => setUrl(e.target.value)}
        />
      )}
    </Modal>
  );
}
