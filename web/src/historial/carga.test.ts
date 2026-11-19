import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type {
  EntradaHistorial,
  EquipoVista,
  HitoVista,
  MiembroVista,
  ProyectoDetalle,
  ProyectoVista,
  TableroHito,
  TareaVista,
} from '../api/tipos';
import { cargarHistorial, type ApiHistorial } from './carga';

const Ana: MiembroVista = {
  idUsuario: 10,
  nombre: 'Ana',
  rolEquipo: 'LIDER',
};

const Luis: MiembroVista = {
  idUsuario: 11,
  nombre: 'Luis',
  rolEquipo: 'INTEGRANTE',
};

const Eva: MiembroVista = {
  idUsuario: 20,
  nombre: 'Eva',
  rolEquipo: 'LIDER',
};

function entrada(parcial: Partial<EntradaHistorial> = {}): EntradaHistorial {
  return {
    idLog: 1,
    fechaHora: '2026-09-23T15:00:00Z',
    idUsuario: 10,
    autor: 'Ana',
    accion: 'CREAR',
    entidad: 'tarea',
    idRegistro: 40,
    campo: null,
    valorAnterior: null,
    valorNuevo: null,
    detalle: '',
    ...parcial,
  };
}

function tarea(parcial: Partial<TareaVista> = {}): TareaVista {
  return {
    idTarea: 40,
    idHitoEquipo: 12,
    titulo: 'Requisitos',
    descripcion: null,
    idEncargado: 10,
    encargado: 'Ana',
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

function hitoVista(parcial: Partial<HitoVista> = {}): HitoVista {
  return {
    idHito: 1,
    idProyecto: 3,
    nombreHito: 'Análisis',
    descripcion: null,
    orden: 1,
    fechaInicio: '2026-08-01',
    fechaLimite: '2026-08-29',
    diasInactividadAlerta: 3,
    notificarDocenteInactividad: false,
    criterios: [],
    ...parcial,
  };
}

function tablero(parcial: Partial<TableroHito> = {}): TableroHito {
  return {
    idHitoEquipo: 12,
    idHito: 1,
    nombreHito: 'Análisis',
    fechaLimite: '2026-08-29',
    estado: 'EN_CURSO',
    editable: true,
    idEquipo: 7,
    nombreEquipo: 'Equipo 7',
    idProyecto: 3,
    nombreProyecto: 'Proyecto 3',
    tareas: [tarea()],
    ...parcial,
  };
}

function proyecto(parcial: Partial<ProyectoVista> = {}): ProyectoVista {
  return {
    idProyecto: 3,
    idDocente: 7,
    nombre: 'Proyecto 3',
    descripcion: null,
    fechaInicio: '2026-08-01',
    fechaFin: '2026-11-30',
    usaMonitores: false,
    ...parcial,
  };
}

function equipo(parcial: Partial<EquipoVista> = {}): EquipoVista {
  return {
    idEquipo: 7,
    idProyecto: 3,
    nombreEquipo: 'Equipo 7',
    miembros: [Ana, Luis],
    hitos: [{ idHitoEquipo: 12, idHito: 1, estado: 'EN_CURSO', fechaEnvio: null }],
    ...parcial,
  };
}

function crearApiFalsa(opciones: { errorHistorial?: ErrorApi } = {}) {
  const llamadas: string[] = [];

  const api: ApiHistorial = {
    async listarProyectos() {
      llamadas.push('listarProyectos');
      return [proyecto()];
    },
    async detalleProyecto(idProyecto: number): Promise<ProyectoDetalle> {
      llamadas.push(`detalleProyecto ${idProyecto}`);
      return {
        proyecto: proyecto({ idProyecto }),
        hitos: [hitoVista({ idHito: 1, orden: 1, nombreHito: 'Análisis' })],
        equipos: [
          equipo({
            idEquipo: 7,
            idProyecto,
            miembros: [Ana, Luis],
            hitos: [{ idHitoEquipo: 12, idHito: 1, estado: 'EN_CURSO', fechaEnvio: null }],
          }),
          equipo({ idEquipo: 8, idProyecto, miembros: [Eva], hitos: [] }),
        ],
      };
    },
    async tableroHito(idHitoEquipo: number) {
      llamadas.push(`tableroHito ${idHitoEquipo}`);
      return tablero({ idHitoEquipo });
    },
    async historialEquipo(idEquipo: number, limite: number) {
      llamadas.push(`historialEquipo ${idEquipo} ${limite}`);
      if (opciones.errorHistorial !== undefined) {
        throw opciones.errorHistorial;
      }
      return [entrada({ idLog: 1 }), entrada({ idLog: 2 })];
    },
  };

  return { api, llamadas };
}

describe('carga del historial', () => {
  it('casos de permiso, acceso y llamadas de API', async () => {
    // 1. cargarHistorial(api, 7, 10) -> tipo: 'listo'
    const c1 = crearApiFalsa();
    const r1 = await cargarHistorial(c1.api, 7, 10);
    expect(r1.tipo).toBe('listo');
    if (r1.tipo === 'listo') {
      expect(c1.llamadas).toContain('historialEquipo 7 1000');
      expect(c1.llamadas).toContain('tableroHito 12');
      expect(r1.datos.entradas).toHaveLength(2);
      expect(r1.datos.tableros.map((t) => t.idHitoEquipo)).toEqual([12]);
      expect(r1.datos.hitos.map((h) => h.idHitoEquipo)).toEqual([12]);
      expect(r1.datos.ubicado.equipo.idEquipo).toBe(7);
    }

    // docente (idUsuario 7) también listo
    const cDocente = crearApiFalsa();
    const rDocente = await cargarHistorial(cDocente.api, 7, 7);
    expect(rDocente.tipo).toBe('listo');

    // 2. Eva (20) de otro equipo -> noPermitido
    const c2 = crearApiFalsa();
    const r2 = await cargarHistorial(c2.api, 7, 20);
    expect(r2).toEqual({ tipo: 'noPermitido' });
    expect(c2.llamadas.some((l) => l.startsWith('historialEquipo') || l.startsWith('tableroHito'))).toBe(false);

    // 3. Equipo inexistente (99) -> sinAcceso
    const c3 = crearApiFalsa();
    const r3 = await cargarHistorial(c3.api, 99, 10);
    expect(r3).toEqual({ tipo: 'sinAcceso' });

    // 4. Equipo 8 con hitos vacíos -> listo con tableros [] y sin llamadas a tableroHito
    const c4 = crearApiFalsa();
    const r4 = await cargarHistorial(c4.api, 8, 20);
    expect(r4.tipo).toBe('listo');
    if (r4.tipo === 'listo') {
      expect(r4.datos.tableros).toEqual([]);
      expect(c4.llamadas.some((l) => l.startsWith('tableroHito'))).toBe(false);
    }

    // 5. ErrorApi 403 ACCESO_DENEGADO -> rechaza con ese mismo error
    const error403 = new ErrorApi(403, 'ACCESO_DENEGADO', 'Acceso denegado');
    const c5 = crearApiFalsa({ errorHistorial: error403 });
    await expect(cargarHistorial(c5.api, 7, 10)).rejects.toBe(error403);
  });
});
