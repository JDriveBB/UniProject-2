import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type {
  CorreoHabilitadoVista,
  EquipoVista,
  EstudianteDisponibleVista,
  ResultadoHabilitacion,
  SolicitudCorreos,
} from '../api/tipos';
import {
  agregarEstudiantes,
  agregarMonitor,
  claveIntegrantes,
  reemplazarEquipo,
  sugerenciasDe,
} from './equipos';
import type { ApiEquipos } from './equipos';

const equipo: EquipoVista = {
  idEquipo: 3,
  idProyecto: 1,
  nombreEquipo: 'Equipo Alfa',
  idCurso: null,
  cupo: null,
  miembros: [],
  hitos: [],
};

function habilitacion(parcial: Partial<ResultadoHabilitacion>): ResultadoHabilitacion {
  return { habilitados: [], yaHabilitados: [], conOtroRol: [], ...parcial };
}

describe('agregarEstudiantes y agregarMonitor en lote', () => {
  it('agrega todos los correos en orden y devuelve el último equipo', async () => {
    const llamados: Array<{ idEquipo: number; email: string; pasarCupo?: boolean }> = [];
    const equipoFinal: EquipoVista = {
      ...equipo,
      miembros: [
        { idUsuario: 1, nombre: 'Ana', rolEquipo: 'INTEGRANTE' },
        { idUsuario: 2, nombre: 'Beto', rolEquipo: 'INTEGRANTE' },
      ],
    };
    const api: Pick<ApiEquipos, 'agregarMiembro'> = {
      async agregarMiembro(idEquipo, email, pasarCupo) {
        llamados.push({ idEquipo, email, pasarCupo });
        return email === 'beto@uninorte.edu.co' ? equipoFinal : equipo;
      },
    };
    const resumen = await agregarEstudiantes(api, 3, ' ana@uninorte.edu.co, beto@uninorte.edu.co ');
    expect(llamados).toEqual([
      { idEquipo: 3, email: 'ana@uninorte.edu.co', pasarCupo: false },
      { idEquipo: 3, email: 'beto@uninorte.edu.co', pasarCupo: false },
    ]);
    expect(resumen).toEqual({
      agregados: ['ana@uninorte.edu.co', 'beto@uninorte.edu.co'],
      cupoLleno: [],
      errores: [],
      equipo: equipoFinal,
    });
  });

  it('separa 409 CUPO_LLENO en cupoLleno, otros 409 en errores y no detiene los demás', async () => {
    const equipoOk: EquipoVista = {
      ...equipo,
      miembros: [{ idUsuario: 9, nombre: 'OK', rolEquipo: 'INTEGRANTE' }],
    };
    const api: Pick<ApiEquipos, 'agregarMiembro'> = {
      async agregarMiembro(_idEquipo, email) {
        if (email === 'lleno@uninorte.edu.co') {
          throw new ErrorApi(409, 'CUPO_LLENO', 'El equipo ya tiene su cupo completo');
        }
        if (email === 'otro@uninorte.edu.co') {
          throw new ErrorApi(409, 'YA_TIENE_EQUIPO', 'Ese estudiante ya pertenece a un equipo de este proyecto');
        }
        return equipoOk;
      },
    };
    const resumen = await agregarEstudiantes(api, 3, [
      'lleno@uninorte.edu.co',
      'otro@uninorte.edu.co',
      'ok@uninorte.edu.co',
    ]);
    expect(resumen).toEqual({
      agregados: ['ok@uninorte.edu.co'],
      cupoLleno: ['lleno@uninorte.edu.co'],
      errores: [
        { email: 'otro@uninorte.edu.co', motivo: 'Ese estudiante ya pertenece a un equipo de este proyecto' },
      ],
      equipo: equipoOk,
    });
  });

  it('con pasarCupo true reenvía true al API y si aun así responde 409 CUPO_LLENO va a errores', async () => {
    const recibidos: Array<boolean | undefined> = [];
    const api: Pick<ApiEquipos, 'agregarMiembro'> = {
      async agregarMiembro(_idEquipo, email, pasarCupo) {
        recibidos.push(pasarCupo);
        if (email === 'falla@u.co') {
          throw new ErrorApi(409, 'CUPO_LLENO', 'Cupo lleno');
        }
        return equipo;
      },
    };
    const resumen = await agregarEstudiantes(api, 3, ['ok@u.co', 'falla@u.co'], true);
    expect(recibidos).toEqual([true, true]);
    expect(resumen.agregados).toEqual(['ok@u.co']);
    expect(resumen.cupoLleno).toEqual([]);
    expect(resumen.errores).toEqual([{ email: 'falla@u.co', motivo: 'Cupo lleno' }]);
  });

  it('lista o texto vacío no hace llamadas al API', async () => {
    let llamadas = 0;
    const api: Pick<ApiEquipos, 'agregarMiembro'> = {
      async agregarMiembro() {
        llamadas += 1;
        return equipo;
      },
    };
    expect(await agregarEstudiantes(api, 3, [])).toEqual({ agregados: [], cupoLleno: [], errores: [] });
    expect(await agregarEstudiantes(api, 3, '   , ; ')).toEqual({ agregados: [], cupoLleno: [], errores: [] });
    expect(llamadas).toBe(0);
  });

  it('resumen de monitores con API simulada', async () => {
    const pedidos: SolicitudCorreos[] = [];
    const monAsignado: CorreoHabilitadoVista = {
      idCorreoHabilitado: 21,
      email: 'registrado@uninorte.edu.co',
      rol: 'MONITOR',
      idProyecto: 1,
      idCurso: null,
      fechaHabilitacion: '2026-11-06T10:00:00Z',
      registrado: true,
    };
    const monSinCuenta: CorreoHabilitadoVista = {
      idCorreoHabilitado: 22,
      email: 'pendiente@uninorte.edu.co',
      rol: 'MONITOR',
      idProyecto: 1,
      idCurso: null,
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

  it('sugerenciasDe filtra por curso cuando el equipo tiene curso, devuelve todos con null y ordena por nombre y correo', () => {
    const disponibles: EstudianteDisponibleVista[] = [
      { email: 'juan@u.co', nombre: 'Juan', idCurso: 2, nombreCurso: 'Curso B' },
      { email: 'ingrid@u.co', nombre: 'Íngrid', idCurso: 1, nombreCurso: 'Curso A' },
      { email: 'hugo@u.co', nombre: 'Hugo', idCurso: 1, nombreCurso: 'Curso A' },
      { email: 'pedro.b@u.co', nombre: 'Pedro', idCurso: 1, nombreCurso: 'Curso A' },
      { email: 'pedro.a@u.co', nombre: 'Pedro', idCurso: 1, nombreCurso: 'Curso A' },
    ];
    expect(sugerenciasDe(disponibles, 1)).toEqual([
      { email: 'hugo@u.co', nombre: 'Hugo', registrado: true },
      { email: 'ingrid@u.co', nombre: 'Íngrid', registrado: true },
      { email: 'pedro.a@u.co', nombre: 'Pedro', registrado: true },
      { email: 'pedro.b@u.co', nombre: 'Pedro', registrado: true },
    ]);
    expect(sugerenciasDe(disponibles, null).map((s) => s.email)).toEqual([
      'hugo@u.co',
      'ingrid@u.co',
      'juan@u.co',
      'pedro.a@u.co',
      'pedro.b@u.co',
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
