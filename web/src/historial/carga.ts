import type { EntradaHistorial, TableroHito } from '../api/tipos';
import { LIMITE_HISTORIAL } from '../reportes/carga';
import { hitosDelEquipo, ubicarEquipo } from '../tablero/ubicacion';
import type { ApiUbicacion, EquipoUbicado, HitoDelEquipo } from '../tablero/ubicacion';
import { puedeVerHistorial } from './vista';

export interface ApiHistorial extends ApiUbicacion {
  tableroHito(idHitoEquipo: number): Promise<TableroHito>;
  historialEquipo(idEquipo: number, limite: number): Promise<EntradaHistorial[]>;
}

export interface DatosHistorial {
  ubicado: EquipoUbicado;
  hitos: HitoDelEquipo[];
  tableros: TableroHito[];
  entradas: EntradaHistorial[];
}

export type ResultadoHistorial =
  | { tipo: 'sinAcceso' }
  | { tipo: 'noPermitido' }
  | { tipo: 'listo'; datos: DatosHistorial };

export async function cargarHistorial(
  api: ApiHistorial,
  idEquipo: number,
  idUsuario: number
): Promise<ResultadoHistorial> {
  const ubicado = await ubicarEquipo(api, idEquipo);
  if (ubicado === null) {
    return { tipo: 'sinAcceso' };
  }
  if (!puedeVerHistorial(ubicado, idUsuario)) {
    return { tipo: 'noPermitido' };
  }
  const hitos = hitosDelEquipo(ubicado);
  const [entradas, tableros] = await Promise.all([
    api.historialEquipo(idEquipo, LIMITE_HISTORIAL),
    Promise.all(hitos.map((h) => api.tableroHito(h.idHitoEquipo))),
  ]);
  return {
    tipo: 'listo',
    datos: { ubicado, hitos, tableros, entradas },
  };
}
