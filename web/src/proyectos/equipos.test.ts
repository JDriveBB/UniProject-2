import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { CorreoHabilitadoVista, EquipoVista, ResultadoHabilitacion, SolicitudCorreos } from '../api/tipos';
import { agregarEstudiante, agregarMonitor, OTRO_ROL_ESTUDIANTE, PENDIENTE_ESTUDIANTE, reemplazarEquipo } from './equipos';
import type { ApiEquipos } from './equipos';

const equipo: EquipoVista = { idEquipo: 3, idProyecto: 1, nombreEquipo: 'Equipo Alfa', miembros: [], hitos: [] };

function habilitacion(parcial: Partial<ResultadoHabilitacion>): ResultadoHabilitacion {
  return { habilitados: [], yaHabilitados: [], conOtroRol: [], ...parcial };
}

function apiFalsa(resultado: ResultadoHabilitacion, alAgregar: () => Promise<EquipoVista>,
                  pedidos: SolicitudCorreos[], agregados: string[]): ApiEquipos {
  return {
    async habilitarCorreos(_idProyecto, solicitud) {
      pedidos.push(solicitud);
      return resultado;
    },
    async agregarMiembro(_idEquipo, email) {
      agregados.push(email);
      return alAgregar();
    },
  };
}

describe('agregar estudiante y monitor', () => {
  it('estudiante: se habilita y se agrega; sin cuenta queda pendiente; los 409 traen el mensaje del núcleo', async () => {
    const pedidos: SolicitudCorreos[] = [];
    const agregados: string[] = [];
    const listo = apiFalsa(habilitacion({}), async () => equipo, pedidos, agregados);
    expect(await agregarEstudiante(listo, 1, 3, ' ana@uninorte.edu.co ')).toEqual({ tipo: 'agregado', equipo });
    expect(pedidos).toEqual([{ emails: ['ana@uninorte.edu.co'], rol: 'ESTUDIANTE' }]);
    expect(agregados).toEqual(['ana@uninorte.edu.co']);

    const sinCuenta = async (): Promise<EquipoVista> => {
      throw new ErrorApi(409, 'ESTUDIANTE_NO_REGISTRADO', 'Ese estudiante aún no crea su cuenta. Pídele que se registre con ese correo');
    };
    expect(await agregarEstudiante(apiFalsa(habilitacion({}), sinCuenta, [], []), 1, 3, 'ana@uninorte.edu.co')).toEqual({
      tipo: 'pendiente',
      mensaje: PENDIENTE_ESTUDIANTE,
    });

    const conEquipo = async (): Promise<EquipoVista> => {
      throw new ErrorApi(409, 'YA_TIENE_EQUIPO', 'Ese estudiante ya pertenece a un equipo de este proyecto');
    };
    expect(await agregarEstudiante(apiFalsa(habilitacion({}), conEquipo, [], []), 1, 3, 'ana@uninorte.edu.co')).toEqual({
      tipo: 'error',
      mensaje: 'Ese estudiante ya pertenece a un equipo de este proyecto',
    });

    const sinAgregar: string[] = [];
    const otroRol = apiFalsa(habilitacion({ conOtroRol: ['ana@uninorte.edu.co'] }), async () => equipo, [], sinAgregar);
    expect(await agregarEstudiante(otroRol, 1, 3, 'ana@uninorte.edu.co')).toEqual({ tipo: 'error', mensaje: OTRO_ROL_ESTUDIANTE });
    expect(sinAgregar).toEqual([]);
  });

  it('monitor: con cuenta queda agregado; sin cuenta, pendiente; ya habilitado o con otro rol, se avisa', async () => {
    const correo: CorreoHabilitadoVista = {
      idCorreoHabilitado: 1,
      email: 'carlos@uninorte.edu.co',
      rol: 'MONITOR',
      idProyecto: 1,
      fechaHabilitacion: '2026-11-04T15:00:00Z',
      registrado: true,
    };
    const api = (r: ResultadoHabilitacion) => apiFalsa(r, async () => equipo, [], []);
    expect(await agregarMonitor(api(habilitacion({ habilitados: [correo] })), 1, 'carlos@uninorte.edu.co')).toEqual({
      tipo: 'exito',
      texto: 'Monitor agregado.',
    });
    expect(
      await agregarMonitor(api(habilitacion({ habilitados: [{ ...correo, registrado: false }] })), 1, 'carlos@uninorte.edu.co'),
    ).toEqual({ tipo: 'aviso', texto: 'Correo habilitado como monitor. Aparecerá aquí cuando cree su cuenta.' });
    expect(
      await agregarMonitor(api(habilitacion({ yaHabilitados: ['carlos@uninorte.edu.co'] })), 1, 'carlos@uninorte.edu.co'),
    ).toEqual({ tipo: 'aviso', texto: 'Ese correo ya estaba habilitado en este proyecto.' });
    expect(
      await agregarMonitor(api(habilitacion({ conOtroRol: ['carlos@uninorte.edu.co'] })), 1, 'carlos@uninorte.edu.co'),
    ).toEqual({ tipo: 'error', texto: 'Ese correo ya tiene una cuenta que no es de monitor.' });
  });

  it('reemplazarEquipo cambia el equipo por id o lo agrega al final', () => {
    const beta: EquipoVista = { ...equipo, idEquipo: 4, nombreEquipo: 'Equipo Beta' };
    const conLider: EquipoVista = { ...equipo, miembros: [{ idUsuario: 9, nombre: 'Ana', rolEquipo: 'LIDER' }] };
    expect(reemplazarEquipo([equipo, beta], conLider)).toEqual([conLider, beta]);
    expect(reemplazarEquipo([equipo], beta)).toEqual([equipo, beta]);
  });
});
