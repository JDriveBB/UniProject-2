import { useState } from 'react';
import { api } from '../api/cliente';
import type { CorreccionVista, EvidenciaVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { fechaCorta } from '../formato';
import { guardarEnDescargas } from '../reportes/descarga';
import { descargarEvidencia, mensajeErrorEvidencia } from './descargaEvidencia';
import {
  TIPO_EVIDENCIA,
  esEnlaceWeb,
  nombreDeEvidencia,
  nombreEvidencia,
  textoCorreccion,
} from './vista';

export interface PropsTarjetaEvidencias {
  evidencias: EvidenciaVista[];
  correccion: CorreccionVista | null;
  esElEncargado: boolean;
  puedeAdjuntar: boolean;
  idAdjuntar: string;
  idVersion: string;
  alAdjuntar: (nuevaVersion: boolean) => void;
}

export function TarjetaEvidencias({
  evidencias,
  correccion,
  esElEncargado,
  puedeAdjuntar,
  idAdjuntar,
  idVersion,
  alAdjuntar,
}: PropsTarjetaEvidencias) {
  const [error, setError] = useState<string | null>(null);
  const [descargando, setDescargando] = useState<number | null>(null);

  async function alDescargar(id: number, ruta: string, nombre: string) {
    setError(null);
    setDescargando(id);
    try {
      await descargarEvidencia(api, ruta, nombre, guardarEnDescargas);
    } catch (err) {
      setError(mensajeErrorEvidencia(err));
    } finally {
      setDescargando(null);
    }
  }

  const infoCorreccion =
    correccion !== null
      ? textoCorreccion(correccion, nombreDeEvidencia(evidencias, correccion.idEvidencia), esElEncargado)
      : null;

  return (
    <Tarjeta titulo="Evidencias">
      {error !== null && <Alerta tipo="error">{error}</Alerta>}
      {correccion !== null && infoCorreccion !== null && (
        <div className="correccion-pendiente">
          <p className="correccion-pendiente__titulo">{infoCorreccion.titulo}</p>
          <p className="correccion-pendiente__texto">{infoCorreccion.texto}</p>
          {puedeAdjuntar && (
            <Boton id={idVersion} variante="primario" onClick={() => alAdjuntar(true)}>
              Adjuntar nueva versión
            </Boton>
          )}
        </div>
      )}

      {evidencias.length === 0 ? (
        <p className="pagina__vacio">Aún no hay evidencias.</p>
      ) : (
        <ul className="lista-evidencias">
          {evidencias.map((e) => {
            const marca = TIPO_EVIDENCIA[e.tipoEvidencia];
            const nombre = nombreEvidencia(e);

            let contenidoNombre = <span>{nombre}</span>;
            if (e.tipoEvidencia === 'ENLACE' && e.enlace !== null && esEnlaceWeb(e.enlace)) {
              contenidoNombre = (
                <a
                  className="evidencia__enlace"
                  href={e.enlace}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {nombre}
                </a>
              );
            } else if (e.tipoEvidencia !== 'ENLACE' && e.descarga !== null) {
              const ruta = e.descarga;
              contenidoNombre = (
                <button
                  type="button"
                  className="evidencia__enlace evidencia__descargar"
                  onClick={() => void alDescargar(e.idEvidencia, ruta, nombre)}
                  disabled={descargando === e.idEvidencia}
                >
                  {nombre}
                </button>
              );
            }

            return (
              <li key={e.idEvidencia} className="evidencia">
                <span className="evidencia__nombre">
                  <Insignia tono={marca.tono}>{marca.texto}</Insignia>
                  {contenidoNombre}
                </span>
                <span className="evidencia__autor">{`${e.autor} · ${fechaCorta(e.fechaSubida)}`}</span>
              </li>
            );
          })}
        </ul>
      )}

      {puedeAdjuntar && (
        <div className="pie-evidencias">
          <Boton id={idAdjuntar} variante="secundario" onClick={() => alAdjuntar(false)}>
            Adjuntar evidencia
          </Boton>
          <p className="texto-secundario">Solo se aceptan archivos PDF, imágenes o enlaces.</p>
        </div>
      )}
    </Tarjeta>
  );
}
