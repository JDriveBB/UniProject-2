import type { TareaVista } from '../api/tipos';
import { BarraAvance } from '../componentes/BarraAvance';
import { Boton } from '../componentes/Boton';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { decimalEsCo, fechaDia } from '../formato';
import type { Prerrequisito } from './vista';

export interface PropsTarjetaInformacion {
  tarea: TareaVista;
  prerrequisitos: Prerrequisito[];
  puedeGestionar: boolean;
  idBoton: string;
  alEstablecer: () => void;
}

export function TarjetaInformacion({
  tarea,
  prerrequisitos,
  puedeGestionar,
  idBoton,
  alEstablecer,
}: PropsTarjetaInformacion) {
  return (
    <Tarjeta titulo="Información">
      <dl className="datos-tarea">
        <div className="dato-tarea">
          <dt className="dato-tarea__etiqueta">Encargado</dt>
          <dd className="dato-tarea__valor">{tarea.encargado ?? 'Sin asignar'}</dd>
        </div>
        <div className="dato-tarea">
          <dt className="dato-tarea__etiqueta">Horas estimadas</dt>
          <dd className="dato-tarea__valor">{`${decimalEsCo(tarea.horasEstimadas)} h`}</dd>
        </div>
        <div className="dato-tarea">
          <dt className="dato-tarea__etiqueta">Avance</dt>
          <dd className="dato-tarea__valor">
            <BarraAvance porcentaje={tarea.porcentajeAvance} ancho="detalle" />
          </dd>
        </div>
        <div className="dato-tarea">
          <dt className="dato-tarea__etiqueta">Fecha límite</dt>
          <dd className="dato-tarea__valor">{fechaDia(tarea.fechaLimiteEfectiva)}</dd>
        </div>
      </dl>
      {tarea.descripcion !== null && tarea.descripcion !== '' && (
        <p className="dato-tarea__descripcion">{tarea.descripcion}</p>
      )}
      <div className="caja-dependencias">
        <div className="caja-dependencias__lista">
          <span className="caja-dependencias__titulo">Bloqueada por:</span>
          {prerrequisitos.length > 0 ? (
            prerrequisitos.map((p) => (
              <Insignia key={p.idTarea} tono={p.tono}>
                {p.texto}
              </Insignia>
            ))
          ) : (
            <span className="texto-secundario">Ninguna tarea.</span>
          )}
        </div>
        {puedeGestionar && (
          <Boton id={idBoton} variante="suave" onClick={alEstablecer}>
            Establecer dependencias
          </Boton>
        )}
      </div>
    </Tarjeta>
  );
}
