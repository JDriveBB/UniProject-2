import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { CorreoHabilitadoVista, EquipoVista, ResultadoHabilitacion, SolicitudCorreos } from '../api/tipos';
import { agregarEstudiante, agregarMonitor, reemplazarEquipo } from './equipos';
import type { ApiEquipos } from './equipos';

const equipo: EquipoVista = { idEquipo: 3, idProyecto: 1, nombreEquipo: 'Equipo Alfa', miembros: [], hitos: [] };

function habilitacion(parcial: Partial<ResultadoHabilitacion>): ResultadoHabilitacion {
  return { habilitados: [], yaHabilitados: [], conOtroRol: [], ...parcial };
}

function apiFalsa(
  resultado: ResultadoHabilitacion,
  alAgregar: (email: string) => Promise<EquipoVista>,
  pedidos: SolicitudCorreos[],
  agregados: string[],
): ApiEquipos {
  return {
    async habilitarCorreos(_idProyecto, solicitud) {
      pedidos.push(solicitud);
      return resultado;
    },
    async agregarMiembro(_idEquipo, email) {
      agregados.push(email);
      return alAgregar(email);
    },
  };
}

describe('agregarEstudiante y agregarMonitor en lote', () => {
  it('resumen de estudiantes con API simulada: mezcla de habilitados nuevos, ya habilitados, otro rol y un fallo al agregar al equipo (los demás continúan)', async () => {
    const pedidos: SolicitudCorreos[] = [];
    const llamadosMiembro: string[] = [];

    const nuevoSinCuenta: CorreoHabilitadoVista = {
      idCorreoHabilitado: 10,
      email: 'nuevo@uninorte.edu.co',
      rol: 'ESTUDIANTE',
      idProyecto: 1,
      fechaHabilitacion: '2026-11-06T10:00:00Z',
      registrado: false,
    };
    const registradoFalla: CorreoHabilitadoVista = {
      idCorreoHabilitado: 11,
      email: 'falla@uninorte.edu.co',
      rol: 'ESTUDIANTE',
      idProyecto: 1,
      fechaHabilitacion: '2026-11-06T10:00:00Z',
      registrado: true,
    };
    const registradoOk: CorreoHabilitadoVista = {
      idCorreoHabilitado: 12,
      email: 'ok@uninorte.edu.co',
      rol: 'ESTUDIANTE',
      idProyecto: 1,
      fechaHabilitacion: '2026-11-06T10:00:00Z',
      registrado: true,
    };

    const respuestaHabilitar = habilitacion({
      habilitados: [nuevoSinCuenta, registradoFalla, registradoOk],
      yaHabilitados: ['ya@uninorte.edu.co'],
      conOtroRol: ['docente@uninorte.edu.co'],
    });

    const equipoActualizado: EquipoVista = {
      ...equipo,
      miembros: [{ idUsuario: 99, nombre: 'OK', rolEquipo: 'INTEGRANTE' }],
    };

    const api = apiFalsa(
      respuestaHabilitar,
      async (email) => {
        if (email === 'falla@uninorte.edu.co') {
          throw new ErrorApi(409, 'YA_TIENE_EQUIPO', 'Ese estudiante ya pertenece a un equipo de este proyecto');
        }
        if (email === 'ya@uninorte.edu.co') {
          throw new ErrorApi(409, 'ESTUDIANTE_NO_REGISTRADO', 'El correo está habilitado pero no tiene cuenta');
        }
        return equipoActualizado;
      },
      pedidos,
      llamadosMiembro,
    );

    const correosEntrada = [
      'nuevo@uninorte.edu.co',
      'falla@uninorte.edu.co',
      'ok@uninorte.edu.co',
      'ya@uninorte.edu.co',
      'docente@uninorte.edu.co',
    ];

    const resumen = await agregarEstudiante(api, 1, 3, correosEntrada);

    expect(pedidos).toEqual([{ emails: correosEntrada, rol: 'ESTUDIANTE' }]);
    expect(llamadosMiembro).toEqual(['falla@uninorte.edu.co', 'ok@uninorte.edu.co', 'ya@uninorte.edu.co']);

    expect(resumen).toEqual({
      agregados: ['ok@uninorte.edu.co'],
      habilitados: ['nuevo@uninorte.edu.co'],
      yaHabilitados: ['ya@uninorte.edu.co'],
      conOtroRol: ['docente@uninorte.edu.co'],
      errores: [
        {
          email: 'falla@uninorte.edu.co',
          motivo: 'Ese estudiante ya pertenece a un equipo de este proyecto',
        },
      ],
      equipo: equipoActualizado,
    });
  });

  it('resumen de monitores con API simulada', async () => {
    const pedidos: SolicitudCorreos[] = [];
    const monAsignado: CorreoHabilitadoVista = {
      idCorreoHabilitado: 21,
      email: 'registrado@uninorte.edu.co',
      rol: 'MONITOR',
      idProyecto: 1,
      fechaHabilitacion: '2026-11-06T10:00:00Z',
      registrado: true,
    };
    const monSinCuenta: CorreoHabilitadoVista = {
      idCorreoHabilitado: 22,
      email: 'pendiente@uninorte.edu.co',
      rol: 'MONITOR',
      idProyecto: 1,
      fechaHabilitacion: '2026-11-06T10:00:00Z',
      registrado: false,
    };

    const respuesta = habilitacion({
      habilitados: [monAsignado, monSinCuenta],
      yaHabilitados: ['ya@uninorte.edu.co'],
      conOtroRol: ['estudiante@uninorte.edu.co'],
    });

    const api: Pick<ApiEquipos, 'habilitarCorreos'> = {
      async habilitarCorreos(_idProyecto, solicitud) {
        pedidos.push(solicitud);
        return respuesta;
      },
    };

    const correos = [
      'registrado@uninorte.edu.co',
      'pendiente@uninorte.edu.co',
      'ya@uninorte.edu.co',
      'estudiante@uninorte.edu.co',
    ];
    const resumen = await agregarMonitor(api, 1, correos);

    expect(pedidos).toEqual([{ emails: correos, rol: 'MONITOR' }]);
    expect(resumen).toEqual({
      asignados: ['registrado@uninorte.edu.co'],
      habilitados: ['pendiente@uninorte.edu.co'],
      yaHabilitados: ['ya@uninorte.edu.co'],
      conOtroRol: ['estudiante@uninorte.edu.co'],
    });
  });

  it('correo ya habilitado que sí tiene cuenta queda en agregados', async () => {
    const pedidos: SolicitudCorreos[] = [];
    const llamadosMiembro: string[] = [];
    const respuesta = habilitacion({
      yaHabilitados: ['ana@uninorte.edu.co'],
    });
    const equipoActualizado: EquipoVista = {
      ...equipo,
      miembros: [{ idUsuario: 10, nombre: 'Ana', rolEquipo: 'INTEGRANTE' }],
    };
    const api = apiFalsa(respuesta, async () => equipoActualizado, pedidos, llamadosMiembro);
    const resumen = await agregarEstudiante(api, 1, 3, ['ana@uninorte.edu.co']);

    expect(llamadosMiembro).toEqual(['ana@uninorte.edu.co']);
    expect(resumen.agregados).toEqual(['ana@uninorte.edu.co']);
    expect(resumen.yaHabilitados).toEqual([]);
    expect(resumen.equipo).toEqual(equipoActualizado);
  });

  it('correo ya habilitado sin cuenta (409 ESTUDIANTE_NO_REGISTRADO) queda en ya habilitados', async () => {
    const pedidos: SolicitudCorreos[] = [];
    const llamadosMiembro: string[] = [];
    const respuesta = habilitacion({
      yaHabilitados: ['sin-cuenta@uninorte.edu.co'],
    });
    const api = apiFalsa(
      respuesta,
      async () => {
        throw new ErrorApi(409, 'ESTUDIANTE_NO_REGISTRADO', 'El correo está habilitado pero no tiene cuenta');
      },
      pedidos,
      llamadosMiembro,
    );
    const resumen = await agregarEstudiante(api, 1, 3, ['sin-cuenta@uninorte.edu.co']);

    expect(llamadosMiembro).toEqual(['sin-cuenta@uninorte.edu.co']);
    expect(resumen.agregados).toEqual([]);
    expect(resumen.yaHabilitados).toEqual(['sin-cuenta@uninorte.edu.co']);
  });

  it('reemplazarEquipo cambia el equipo por id o lo agrega al final', () => {
    const beta: EquipoVista = { ...equipo, idEquipo: 4, nombreEquipo: 'Equipo Beta' };
    const conLider: EquipoVista = { ...equipo, miembros: [{ idUsuario: 9, nombre: 'Ana', rolEquipo: 'LIDER' }] };
    expect(reemplazarEquipo([equipo, beta], conLider)).toEqual([conLider, beta]);
    expect(reemplazarEquipo([equipo], beta)).toEqual([equipo, beta]);
  });
});
