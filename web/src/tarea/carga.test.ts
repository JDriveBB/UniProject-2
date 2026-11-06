import { describe, expect, it } from 'vitest';
import type {
  AvanceVista,
  ComentarioVista,
  CorreccionVista,
  EvidenciaVista,
  ProyectoDetalle,
  ProyectoVista,
  TableroHito,
  TareaDetalle,
  TareaVista,
} from '../api/tipos';
import { cargarTarea, type ApiCargaTarea } from './carga';

function tarea(parcial: Partial<TareaVista> = {}): TareaVista {
  return {
    idTarea: 10,
    idHitoEquipo: 200,
    titulo: 'Tarea 10',
    descripcion: null,
    idEncargado: null,
    encargado: null,
    idTareaOrigen: null,
    horasEstimadas: 1,
    porcentajeAvance: 0,
    estado: 'PENDIENTE',
    fechaLimite: null,
    fechaLimiteEfectiva: '2026-09-30',
    fechaCompletada: null,
    esPrerrequisitoCritico: false,
    bloqueada: false,
    fechaBloqueo: null,
    enRutaCritica: false,
    holguraHoras: null,
    nivelRiesgo: null,
    prerrequisitos: [],
    ...parcial,
  };
}

describe('carga de tarea', () => {
  it('carga datos de la tarea, ordena listas sin mutar y encuentra hito/equipo', async () => {
    const ordenLlamadas: string[] = [];

    const proyecto: ProyectoVista = {
      idProyecto: 1,
      idDocente: 1,
      nombre: 'Proyecto 1',
      descripcion: '',
      fechaInicio: '2026-08-01',
      fechaFin: '2026-12-01',
      usaMonitores: false,
    };

    const proyectoDetalle: ProyectoDetalle = {
      proyecto,
      equipos: [
        {
          idEquipo: 7,
          idProyecto: 1,
          nombreEquipo: 'Equipo 7',
          miembros: [],
          hitos: [
            { idHitoEquipo: 100, idHito: 1, estado: 'EVALUADO' },
            { idHitoEquipo: 200, idHito: 2, estado: 'EN_CURSO' },
          ],
        },
      ],
      hitos: [
        {
          idHito: 1,
          idProyecto: 1,
          nombreHito: 'Hito 1',
          descripcion: '',
          orden: 1,
          fechaInicio: '2026-08-01',
          fechaLimite: '2026-09-01',
          diasInactividadAlerta: 7,
          notificarDocenteInactividad: false,
          criterios: [],
        },
        {
          idHito: 2,
          idProyecto: 1,
          nombreHito: 'Hito 2',
          descripcion: '',
          orden: 2,
          fechaInicio: '2026-09-02',
          fechaLimite: '2026-10-01',
          diasInactividadAlerta: 7,
          notificarDocenteInactividad: false,
          criterios: [],
        },
      ],
    };

    const tablero: TableroHito = {
      idHitoEquipo: 200,
      idHito: 2,
      nombreHito: 'Hito 2',
      fechaLimite: '2026-10-01',
      estado: 'EN_CURSO',
      editable: true,
      idEquipo: 7,
      nombreEquipo: 'Equipo 7',
      idProyecto: 1,
      nombreProyecto: 'Proyecto 1',
      tareas: [],
    };

    const avancesOriginales: AvanceVista[] = [
      { idRegistro: 1, idUsuario: 1, usuario: 'U1', porcentajeAvance: 10, observacion: null, fechaHora: '2026-09-01T00:00:00Z' },
      { idRegistro: 3, idUsuario: 1, usuario: 'U1', porcentajeAvance: 50, observacion: null, fechaHora: '2026-09-03T00:00:00Z' },
      { idRegistro: 2, idUsuario: 1, usuario: 'U1', porcentajeAvance: 30, observacion: null, fechaHora: '2026-09-02T00:00:00Z' },
    ];

    const evidenciasOriginales: EvidenciaVista[] = [
      { idEvidencia: 5, idTarea: 10, idUsuario: 1, autor: 'U1', tipoEvidencia: 'ENLACE', enlace: 'https:' + '//a', nombreArchivo: null, descarga: null, fechaSubida: '2026-09-01T00:00:00Z' },
      { idEvidencia: 9, idTarea: 10, idUsuario: 1, autor: 'U1', tipoEvidencia: 'PDF', enlace: null, nombreArchivo: 'doc.pdf', descarga: '/d/9', fechaSubida: '2026-09-02T00:00:00Z' },
    ];

    const comentariosOriginales: ComentarioVista[] = [
      { idComentario: 4, idTarea: 10, idAutor: 2, autor: 'Docente', rolAutor: 'DOCENTE', contenido: 'Revisar', fechaHora: '2026-09-02T00:00:00Z' },
      { idComentario: 1, idTarea: 10, idAutor: 1, autor: 'Estudiante', rolAutor: 'ESTUDIANTE', contenido: 'Listo', fechaHora: '2026-09-01T00:00:00Z' },
    ];

    const correccionesOriginales: CorreccionVista[] = [];

    const detalle: TareaDetalle = {
      tarea: tarea({ idTarea: 10, idHitoEquipo: 200 }),
      editable: true,
      avances: avancesOriginales,
    };

    const apiFalsa: ApiCargaTarea = {
      async listarProyectos() {
        ordenLlamadas.push('listarProyectos');
        return [proyecto];
      },
      async detalleProyecto(id) {
        ordenLlamadas.push(`detalleProyecto-${id}`);
        return proyectoDetalle;
      },
      async detalleTarea(id) {
        ordenLlamadas.push(`detalleTarea-${id}`);
        return detalle;
      },
      async tableroHito(id) {
        ordenLlamadas.push(`tableroHito-${id}`);
        return tablero;
      },
      async listarEvidencias(id) {
        ordenLlamadas.push(`listarEvidencias-${id}`);
        return evidenciasOriginales;
      },
      async listarComentarios(id) {
        ordenLlamadas.push(`listarComentarios-${id}`);
        return comentariosOriginales;
      },
      async listarCorrecciones(id) {
        ordenLlamadas.push(`listarCorrecciones-${id}`);
        return correccionesOriginales;
      },
    };

    const resultado = await cargarTarea(apiFalsa, 10);

    expect(ordenLlamadas[0]).toBe('detalleTarea-10');
    expect(ordenLlamadas).toContain('tableroHito-200');
    expect(resultado.hito?.orden).toBe(2);
    expect(resultado.detalle.avances.map((a) => a.idRegistro)).toEqual([3, 2, 1]);
    expect(resultado.evidencias.map((e) => e.idEvidencia)).toEqual([9, 5]);
    expect(resultado.comentarios.map((c) => c.idComentario)).toEqual([1, 4]);

    // Verificar que los arreglos originales no fueron mutados
    expect(avancesOriginales.map((a) => a.idRegistro)).toEqual([1, 3, 2]);
    expect(evidenciasOriginales.map((e) => e.idEvidencia)).toEqual([5, 9]);
    expect(comentariosOriginales.map((c) => c.idComentario)).toEqual([4, 1]);

    // Si el equipo no aparece en ningún proyecto -> equipo null y hito null
    const apiSinEquipo: ApiCargaTarea = {
      ...apiFalsa,
      async listarProyectos() {
        return [];
      },
    };

    const resultadoSinEquipo = await cargarTarea(apiSinEquipo, 10);
    expect(resultadoSinEquipo.equipo).toBeNull();
    expect(resultadoSinEquipo.hito).toBeNull();
  });
});
