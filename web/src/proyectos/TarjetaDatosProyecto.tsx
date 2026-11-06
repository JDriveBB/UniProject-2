import { Campo } from '../componentes/Campo';
import { CampoArea } from '../componentes/CampoArea';
import { Tarjeta } from '../componentes/Tarjeta';
import type { BorradorProyecto } from './borrador';
import { MAXIMO_DESCRIPCION, MAXIMO_NOMBRE_PROYECTO } from './validacion';
import type { ErroresProyecto } from './validacion';

export type CambiosProyecto = Partial<Pick<BorradorProyecto, 'nombre' | 'descripcion' | 'fechaInicio' | 'fechaFin'>>;

/** P03 izquierda 1: nombre, descripción (80 px) y las dos fechas en una fila. */
export function TarjetaDatosProyecto({ borrador, errores, editable, alCambiar }: {
  borrador: BorradorProyecto;
  errores: ErroresProyecto;
  editable: boolean;
  alCambiar: (cambios: CambiosProyecto) => void;
}) {
  return (
    <Tarjeta titulo="Datos del proyecto">
      <Campo
        etiqueta="Nombre del proyecto"
        value={borrador.nombre}
        maxLength={MAXIMO_NOMBRE_PROYECTO}
        readOnly={!editable}
        onChange={(e) => alCambiar({ nombre: e.target.value })}
        error={errores.nombre}
        required
      />
      <CampoArea
        etiqueta="Descripción"
        value={borrador.descripcion}
        maxLength={MAXIMO_DESCRIPCION}
        readOnly={!editable}
        onChange={(e) => alCambiar({ descripcion: e.target.value })}
        error={errores.descripcion}
      />
      <div className="fila-campos">
        <Campo
          etiqueta="Fecha de inicio"
          type="date"
          value={borrador.fechaInicio}
          readOnly={!editable}
          onChange={(e) => alCambiar({ fechaInicio: e.target.value })}
          error={errores.fechaInicio}
          required
        />
        <Campo
          etiqueta="Fecha de fin"
          type="date"
          value={borrador.fechaFin}
          readOnly={!editable}
          onChange={(e) => alCambiar({ fechaFin: e.target.value })}
          error={errores.fechaFin}
          required
        />
      </div>
    </Tarjeta>
  );
}
