import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type {
  CorreoHabilitadoVista,
  EquipoVista,
  EstudianteDisponibleVista,
  EstudianteProyectoVista,
  ResultadoHabilitacion,
  SolicitudCorreos,
} from '../api/tipos';
import {
  agregarEstudiante,
  agregarMonitor,
  claveIntegrantes,
  invitadosDe,
  reemplazarEquipo,
  sugerenciasDe,
} from './equipos';
import type { ApiEquipos } from './equipos';

const equipo: EquipoVista = { idEquipo: 3, idProyecto: 1, nombreEquipo: 'Equipo Alfa', miembros: [], hitos: [] };

function habilitacion(parcial: Partial<ResultadoHabilitacion>): ResultadoHabilitacion {
  return { habilitados: [], yaHabilitados: [], conOtroRol: [], ...parcial };
}

function estudiante(parcial: Partial<EstudianteProyectoVista> & { email: string }): EstudianteProyectoVista {
  return { nombre: null, registrado: false, idEquipo: null, idEquipoInvitado: null, ...parcial };
}

function apiFalsa(
  resultado: ResultadoHabilitacion,
  alAgregar: (email: string) => Promise<EquipoVista>,
  pedidos: SolicitudCorreos[],
  agregados: string[],
  invitaciones: string[] = [],
  alInvitar: (email: string) => Promise<void> = async () => {},
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
    async invitarAlEquipo(idEquipo, email) {
      invitaciones.push(email);
      await alInvitar(email);
      return estudiante({ email, idEquipoInvitado: idEquipo });
    },
  };
}

describe('agregarEstudiante y agregarMonitor en lote', () => {
  it('resumen de estudiantes con API simulada: agrega a los que tienen cuenta, invita a los que no (también al ya habilitado sin cuenta), otro rol y un fallo al agregar (los demás continúan)', async () => {
    const pedidos: SolicitudCorreos[] = [];
    const llamadosMiembro: string[] = [];
    const llamadosInvitar: string[] = [];

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
      llamadosInvitar,
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
    expect(llamadosInvitar).toEqual(['nuevo@uninorte.edu.co', 'ya@uninorte.edu.co']);

    expect(resumen).toEqual({
      agregados: ['ok@uninorte.edu.co'],
      invitados: ['nuevo@uninorte.edu.co', 'ya@uninorte.edu.co'],
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
    expect(resumen.equipo).toEqual(equipoActualizado);
  });

  it('correo ya habilitado sin cuenta (409 ESTUDIANTE_NO_REGISTRADO) queda invitado al equipo', async () => {
    const pedidos: SolicitudCorreos[] = [];
    const llamadosMiembro: string[] = [];
    const llamadosInvitar: string[] = [];
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
      llamadosInvitar,
    );
    const resumen = await agregarEstudiante(api, 1, 3, ['sin-cuenta@uninorte.edu.co']);

    expect(llamadosMiembro).toEqual(['sin-cuenta@uninorte.edu.co']);
    expect(llamadosInvitar).toEqual(['sin-cuenta@uninorte.edu.co']);
    expect(resumen.agregados).toEqual([]);
    expect(resumen.invitados).toEqual(['sin-cuenta@uninorte.edu.co']);
    expect(resumen.errores).toEqual([]);
  });

  it('si la invitación falla (ya invitado a otro equipo) queda en errores con el motivo del API', async () => {
    const sinCuenta: CorreoHabilitadoVista = {
      idCorreoHabilitado: 30,
      email: 'ines@uninorte.edu.co',
      rol: 'ESTUDIANTE',
      idProyecto: 1,
      fechaHabilitacion: '2026-11-06T10:00:00Z',
      registrado: false,
    };
    const api = apiFalsa(
      habilitacion({ habilitados: [sinCuenta] }),
      async () => equipo,
      [],
      [],
      [],
      async () => {
        throw new ErrorApi(409, 'YA_INVITADO', 'Ese estudiante ya está invitado a otro equipo de este proyecto');
      },
    );
    const resumen = await agregarEstudiante(api, 1, 3, ['ines@uninorte.edu.co']);

    expect(resumen.invitados).toEqual([]);
    expect(resumen.errores).toEqual([
      { email: 'ines@uninorte.edu.co', motivo: 'Ese estudiante ya está invitado a otro equipo de este proyecto' },
    ]);
    expect(resumen.equipo).toBeUndefined();
  });

  it('sugerenciasDe deja solo a los que no tienen equipo ni invitación; invitadosDe, los invitados de un equipo', () => {
    const lista = [
      estudiante({ email: 'ana@u.co', nombre: 'Ana', registrado: true, idEquipo: 3 }),
      estudiante({ email: 'beto@u.co', nombre: 'Beto', registrado: true }),
      estudiante({ email: 'ines@u.co', idEquipoInvitado: 3 }),
      estudiante({ email: 'juan@u.co', idEquipoInvitado: 4 }),
      estudiante({ email: 'luz@u.co' }),
    ];
    expect(sugerenciasDe(lista)).toEqual([
      { email: 'beto@u.co', nombre: 'Beto', registrado: true },
      { email: 'luz@u.co', nombre: null, registrado: false },
    ]);
    expect(invitadosDe(lista, 3)).toEqual(['ines@u.co']);
    expect(invitadosDe(lista, 9)).toEqual([]);
  });

  it('sugerenciasDe mezcla disponibles sin repetir, ordena con tildes y usa correo sin nombre', () => {
    const estudiantes = [
      estudiante({ email: 'ana@u.co', nombre: 'Ana', registrado: true, idEquipo: 3 }),
      estudiante({ email: 'diego@u.co', idEquipoInvitado: 3 }),
      estudiante({ email: 'camila@u.co', nombre: 'Camila Habilitada', registrado: true }),
      estudiante({ email: 'elena@u.co' }),
    ];
    const disponibles: EstudianteDisponibleVista[] = [
      { email: 'CAMILA@U.CO', nombre: 'Camila Disponible' },
      { email: 'ANA@U.CO', nombre: 'Ana Con Equipo' },
      { email: 'juan@u.co', nombre: 'Juan' },
      { email: 'ingrid@u.co', nombre: 'Íngrid' },
      { email: 'hugo@u.co', nombre: 'Hugo' },
      { email: 'pedro.b@u.co', nombre: 'Pedro' },
      { email: 'pedro.a@u.co', nombre: 'Pedro' },
    ];
    expect(sugerenciasDe(estudiantes, disponibles)).toEqual([
      { email: 'camila@u.co', nombre: 'Camila Habilitada', registrado: true },
      { email: 'elena@u.co', nombre: null, registrado: false },
      { email: 'hugo@u.co', nombre: 'Hugo', registrado: true },
      { email: 'ingrid@u.co', nombre: 'Íngrid', registrado: true },
      { email: 'juan@u.co', nombre: 'Juan', registrado: true },
      { email: 'pedro.a@u.co', nombre: 'Pedro', registrado: true },
      { email: 'pedro.b@u.co', nombre: 'Pedro', registrado: true },
    ]);
  });

  it('claveIntegrantes cambia al agregar o quitar integrantes, no al cambiar el líder', () => {
    const conAna: EquipoVista = { ...equipo, miembros: [{ idUsuario: 9, nombre: 'Ana', rolEquipo: 'INTEGRANTE' }] };
    const anaLider: EquipoVista = { ...equipo, miembros: [{ idUsuario: 9, nombre: 'Ana', rolEquipo: 'LIDER' }] };
    expect(claveIntegrantes([equipo])).not.toBe(claveIntegrantes([conAna]));
    expect(claveIntegrantes([conAna])).toBe(claveIntegrantes([anaLider]));
  });

  it('reemplazarEquipo cambia el equipo por id o lo agrega al final', () => {
    const beta: EquipoVista = { ...equipo, idEquipo: 4, nombreEquipo: 'Equipo Beta' };
    const conLider: EquipoVista = { ...equipo, miembros: [{ idUsuario: 9, nombre: 'Ana', rolEquipo: 'LIDER' }] };
    expect(reemplazarEquipo([equipo, beta], conLider)).toEqual([conLider, beta]);
    expect(reemplazarEquipo([equipo], beta)).toEqual([equipo, beta]);
  });
});
