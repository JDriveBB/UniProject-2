import type { TableroHito } from '../api/tipos';
import { BarraAvance } from '../componentes/BarraAvance';
import { Insignia } from '../componentes/Insignia';
import { fechaDia } from '../formato';
import { avanceDelHito, estadoHito } from './marcas';

/** Resumen del hito de P04 (decisión 26c-8): estado, fecha límite vigente y avance (promedio simple). */
export function ResumenHito({ tablero }: { tablero: TableroHito }) {
  const estado = estadoHito(tablero.estado);
  return (
    <section className="resumen-hito" aria-label="Resumen del hito">
      <p className="resumen-hito__dato">
        <span className="resumen-hito__etiqueta">Estado del hito:</span>
        <Insignia tono={estado.tono}>{estado.texto}</Insignia>
      </p>
      <p className="resumen-hito__dato">
        <span className="resumen-hito__etiqueta">Fecha límite vigente:</span>
        <span>{fechaDia(tablero.fechaLimite)}</span>
      </p>
      <p className="resumen-hito__dato">
        <span className="resumen-hito__etiqueta">Avance del hito:</span>
        <BarraAvance porcentaje={avanceDelHito(tablero.tareas)} ancho="hito" />
      </p>
    </section>
  );
}
