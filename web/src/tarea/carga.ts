import type {
  ComentarioVista,
  CorreccionVista,
  EvidenciaVista,
  TableroHito,
  TareaDetalle,
} from '../api/tipos';
import {
  hitosDelEquipo,
  ubicarEquipo,
  type ApiUbicacion,
  type EquipoUbicado,
  type HitoDelEquipo,
} from '../tablero/ubicacion';

export interface DatosTarea {
  detalle: TareaDetalle;
  tablero: TableroHito;
  equipo: EquipoUbicado | null;
  hito: HitoDelEquipo | null;
  evidencias: EvidenciaVista[];
  comentarios: ComentarioVista[];
  correcciones: CorreccionVista[];
}

export interface ApiCargaTarea extends ApiUbicacion {
  detalleTarea(idTarea: number): Promise<TareaDetalle>;
  tableroHito(idHitoEquipo: number): Promise<TableroHito>;
  listarEvidencias(idTarea: number): Promise<EvidenciaVista[]>;
  listarComentarios(idTarea: number): Promise<ComentarioVista[]>;
  listarCorrecciones(idTarea: number): Promise<CorreccionVista[]>;
}

export async function cargarTarea(api: ApiCargaTarea, idTarea: number): Promise<DatosTarea> {
  const detalle = await api.detalleTarea(idTarea);

  const [tablero, evidencias, comentarios, correcciones] = await Promise.all([
    api.tableroHito(detalle.tarea.idHitoEquipo),
    api.listarEvidencias(idTarea),
    api.listarComentarios(idTarea),
    api.listarCorrecciones(idTarea),
  ]);

  const equipo = await ubicarEquipo(api, tablero.idEquipo);
  const hito =
    equipo !== null
      ? hitosDelEquipo(equipo).find((h) => h.idHitoEquipo === detalle.tarea.idHitoEquipo) ?? null
      : null;

  const avancesOrdenados = [...detalle.avances].sort((a, b) => b.idRegistro - a.idRegistro);
  const evidenciasOrdenadas = [...evidencias].sort((a, b) => b.idEvidencia - a.idEvidencia);
  const comentariosOrdenados = [...comentarios].sort((a, b) => a.idComentario - b.idComentario);

  return {
    detalle: {
      ...detalle,
      avances: avancesOrdenados,
    },
    tablero,
    equipo,
    hito,
    evidencias: evidenciasOrdenadas,
    comentarios: comentariosOrdenados,
    correcciones: [...correcciones],
  };
}
