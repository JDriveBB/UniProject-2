import { BarraAvance } from '../componentes/BarraAvance';
import { Boton } from '../componentes/Boton';
import { Insignia } from '../componentes/Insignia';
import { Modal } from '../componentes/Modal';
import type { ResultadoEnvio } from '../tarea/envio';
import { estadoHito } from './marcas';

export type ResultadoDialogo = Exclude<ResultadoEnvio, { tipo: 'error' }>;

export interface PropsDialogoEnvio {
  resultado: ResultadoDialogo;
  etiqueta: string;
  alCerrar: () => void;
}

export function DialogoEnvio({ resultado, etiqueta, alCerrar }: PropsDialogoEnvio) {
  if (resultado.tipo === 'rechazado') {
    return (
      <Modal
        ancho="dialogo"
        titulo="No se puede enviar el hito"
        tonoTitulo="peligro"
        alCerrar={alCerrar}
        acciones={(cerrar) => (
          <Boton type="button" variante="primario" onClick={cerrar}>
            Entendido
          </Boton>
        )}
      >
        <p className="modal__texto">{`${etiqueta} tiene prerrequisitos críticos sin completar:`}</p>
        <ul className="lista-pendientes">
          {resultado.pendientes.map((p) => (
            <li key={p.clave} className="pendiente-envio">
              <span className="pendiente-envio__nombre">{p.nombre}</span>
              {p.porcentaje !== null && <BarraAvance porcentaje={p.porcentaje} ancho="tabla" />}
            </li>
          ))}
        </ul>
        <p className="modal__pie">El hito sigue en estado En progreso y las tareas siguen editables.</p>
      </Modal>
    );
  }

  const marca = estadoHito('ENVIADO');

  return (
    <Modal
      ancho="dialogo"
      titulo="Hito enviado a revisión"
      tonoTitulo="exito"
      alCerrar={alCerrar}
      acciones={(cerrar) => (
        <Boton type="button" variante="primario" onClick={cerrar}>
          Aceptar
        </Boton>
      )}
    >
      <p className="modal__texto">
        {`${etiqueta} quedó en estado En Revisión. Las tareas quedan bloqueadas para edición y el docente fue notificado.`}
      </p>
      <p className="modal__dato">
        <span className="modal__dato-etiqueta">Nuevo estado:</span>
        <Insignia tono={marca.tono}>{marca.texto}</Insignia>
      </p>
    </Modal>
  );
}
