import { Boton } from '../componentes/Boton';
import { Tarjeta } from '../componentes/Tarjeta';
import { rangoFechas } from '../formato';
import type { BorradorHito } from './borrador';
import { hitoTieneErrores } from './validacion';
import type { ErroresBorrador } from './validacion';

/** P03 izquierda 2: un botón por hito ("1. Análisis" y su rango); el elegido en azul. */
export function TarjetaHitos({ hitos, seleccion, errores, editable, alSeleccionar, alAgregar }: {
  hitos: BorradorHito[];
  seleccion: string | null;
  errores: ErroresBorrador;
  editable: boolean;
  alSeleccionar: (clave: string) => void;
  alAgregar: () => void;
}) {
  return (
    <Tarjeta titulo="Hitos">
      {hitos.length === 0 ? (
        <p className="texto-secundario">
          {editable ? 'Aún no hay hitos. Agrega el primero.' : 'Este proyecto aún no tiene hitos.'}
        </p>
      ) : (
        <ul className="lista-hitos">
          {hitos.map((h) => {
            const elegido = h.clave === seleccion;
            const conErrores = hitoTieneErrores(h, errores);
            const clases = ['fila-hito', elegido ? 'fila-hito--seleccionada' : '', conErrores ? 'fila-hito--error' : '']
              .filter(Boolean)
              .join(' ');
            return (
              <li key={h.clave}>
                <button type="button" className={clases} aria-pressed={elegido} onClick={() => alSeleccionar(h.clave)}>
                  <span className="fila-hito__nombre">
                    {h.orden}. {h.nombre.trim() === '' ? 'Hito sin nombre' : h.nombre.trim()}
                  </span>
                  <span className="fila-hito__fechas">{rangoFechas(h.fechaInicio, h.fechaLimite)}</span>
                  {conErrores && <span className="solo-lectores"> (tiene errores)</span>}
                </button>
              </li>
            );
          })}
        </ul>
      )}
      {editable && (
        <div>
          <Boton variante="secundario" onClick={alAgregar}>
            + Agregar hito
          </Boton>
        </div>
      )}
    </Tarjeta>
  );
}
