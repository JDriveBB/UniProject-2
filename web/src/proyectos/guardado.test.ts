import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { CriterioVista, HitoVista, ProyectoVista, SolicitudCriterio, SolicitudHito } from '../api/tipos';
import { asignarIds } from './borrador';
import type { BorradorProyecto } from './borrador';
import { ejecutarPlan } from './guardado';
import type { ApiGuardado } from './guardado';
import type { Operacion } from './plan';

function apiFalsa(llamadas: string[], fallaEn?: string): ApiGuardado {
  const registrar = (nombre: string) => {
    llamadas.push(nombre);
    if (nombre === fallaEn) {
      throw new ErrorApi(409, 'PESOS_EXCEDEN_100', 'Los pesos de la rúbrica sumarían 110 %; el máximo es 100 %');
    }
  };
  return {
    async crearProyecto() {
      registrar('crearProyecto');
      return { idProyecto: 7 } as ProyectoVista;
    },
    async actualizarProyecto(idProyecto) {
      registrar('actualizarProyecto:' + idProyecto);
      return { idProyecto } as ProyectoVista;
    },
    async crearHito(idProyecto) {
      registrar('crearHito:' + idProyecto);
      return { idHito: 30 } as HitoVista;
    },
    async actualizarHito(idHito) {
      registrar('actualizarHito:' + idHito);
      return { idHito } as HitoVista;
    },
    async crearCriterio(idHito) {
      registrar('crearCriterio:' + idHito);
      return { idCriterio: 40 + llamadas.length } as CriterioVista;
    },
    async actualizarCriterio(idHito, idCriterio) {
      registrar(`actualizarCriterio:${idHito}:${idCriterio}`);
      return { idCriterio } as CriterioVista;
    },
    async eliminarCriterio(idHito, idCriterio) {
      registrar(`eliminarCriterio:${idHito}:${idCriterio}`);
    },
  };
}

const hito: SolicitudHito = {
  nombreHito: 'Análisis',
  descripcion: null,
  orden: 1,
  fechaInicio: '2026-08-01',
  fechaLimite: '2026-08-29',
  diasInactividadAlerta: 3,
  notificarDocenteInactividad: false,
};
const criterio: SolicitudCriterio = { descripcion: 'Claridad', pesoPorcentaje: 50, puntajeMaximo: 5 };
const plan: Operacion[] = [
  {
    tipo: 'crearProyecto',
    solicitud: { nombre: 'P', descripcion: null, fechaInicio: '2026-08-01', fechaFin: '2026-11-30', usaMonitores: false },
  },
  { tipo: 'crearHito', claveHito: 'n1', solicitud: hito },
  { tipo: 'crearCriterio', claveHito: 'n1', idHito: null, claveCriterio: 'n2', solicitud: criterio },
  { tipo: 'crearCriterio', claveHito: 'n1', idHito: null, claveCriterio: 'n3', solicitud: criterio },
];

describe('ejecución del plan de guardado', () => {
  it('ejecuta en orden y usa los ids que crean los pasos anteriores', async () => {
    const llamadas: string[] = [];
    const r = await ejecutarPlan(plan, null, apiFalsa(llamadas));
    expect(llamadas).toEqual(['crearProyecto', 'crearHito:7', 'crearCriterio:30', 'crearCriterio:30']);
    expect(r.idProyecto).toBe(7);
    expect(r.fallo).toBeNull();
    expect([...r.creados.entries()]).toEqual([['n1', 30], ['n2', 43], ['n3', 44]]);
  });

  it('se detiene en el primer error y lo creado se asigna al borrador', async () => {
    const llamadas: string[] = [];
    const r = await ejecutarPlan(plan, null, apiFalsa(llamadas, 'crearCriterio:30'));
    expect(llamadas).toEqual(['crearProyecto', 'crearHito:7', 'crearCriterio:30']);
    expect(r.fallo?.indice).toBe(2);
    expect(r.fallo?.causa).toBeInstanceOf(ErrorApi);
    expect(r.idProyecto).toBe(7);
    const b: BorradorProyecto = {
      nombre: 'P',
      descripcion: '',
      fechaInicio: '2026-08-01',
      fechaFin: '2026-11-30',
      usaMonitores: false,
      hitos: [
        {
          clave: 'n1',
          idHito: null,
          orden: 1,
          nombre: 'Análisis',
          descripcion: null,
          fechaInicio: '2026-08-01',
          fechaLimite: '2026-08-29',
          diasInactividad: '3',
          notificarDocente: false,
          criterios: [{ clave: 'n2', idCriterio: null, descripcion: 'Claridad', peso: '50', puntaje: '5' }],
        },
      ],
    };
    const conIds = asignarIds(b, r.creados);
    expect(conIds.hitos[0].idHito).toBe(30);
    expect(conIds.hitos[0].criterios[0].idCriterio).toBeNull();
  });
});
