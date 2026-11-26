import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { CorreoHabilitadoVista, EquipoVista, MiembroVista } from '../api/tipos';
import {
  bloqueoParaQuitar,
  ES_LIDER_AHORA,
  pendientesDe,
  preguntaQuitarIntegrante,
  preguntaQuitarMonitor,
  preguntaRetirarCorreo,
  preguntaRetirarCorreoCurso,
  quitarIntegrante,
  quitarMonitorDelProyecto,
  retirarHabilitacion,
  sePuedeQuitar,
  textoIntegranteQuitado,
  YA_NO_ES_MIEMBRO,
  YA_NO_ES_MONITOR,
} from './quitar';

const equipo: EquipoVista = {
  idEquipo: 3,
  idProyecto: 1,
  nombreEquipo: 'Equipo Alfa',
  idCurso: null,
  cupo: null,
  miembros: [],
  hitos: [],
};

describe('quitar integrantes y monitores', () => {
  it('al líder no se le ofrece quitar; las preguntas dicen que se conservan tareas e historial', () => {
    expect(sePuedeQuitar({ idUsuario: 9, nombre: 'Ana', rolEquipo: 'LIDER' })).toBe(false);
    expect(sePuedeQuitar({ idUsuario: 8, nombre: 'Luis', rolEquipo: 'INTEGRANTE' })).toBe(true);
    expect(preguntaQuitarIntegrante('Luis')).toBe(
      '¿Quitar a Luis del equipo? Sus tareas y su historial se conservan, y el líder puede reasignar sus tareas.',
    );
    expect(preguntaQuitarMonitor('Carlos Ruiz')).toBe(
      '¿Quitar a Carlos Ruiz de los monitores del proyecto? Su historial se conserva y puedes volver a agregarlo con su correo.',
    );
    expect(textoIntegranteQuitado('Luis')).toBe('Luis salió del equipo. Sus tareas y su historial se conservan.');
  });

  it('integrante: devuelve el equipo; 404 y 403 con mensajes claros', async () => {
    const pedidos: string[] = [];
    const bien = {
      async quitarMiembro(idEquipo: number, idUsuario: number) {
        pedidos.push(`${idEquipo}:${idUsuario}`);
        return equipo;
      },
    };
    expect(await quitarIntegrante(bien, 3, 8)).toEqual({ tipo: 'quitado', equipo });
    expect(pedidos).toEqual(['3:8']);
    const falla = (error: ErrorApi) => ({
      async quitarMiembro(): Promise<EquipoVista> {
        throw error;
      },
    });
    expect(await quitarIntegrante(falla(new ErrorApi(404, 'NO_ENCONTRADO', 'Esa persona no es miembro del equipo')), 3, 8))
      .toEqual({ tipo: 'error', mensaje: YA_NO_ES_MIEMBRO });
    expect(await quitarIntegrante(falla(new ErrorApi(403, 'ACCESO_DENEGADO', 'Acceso denegado')), 3, 8)).toEqual({
      tipo: 'error',
      mensaje: 'Solo el docente del proyecto puede cambiarlo',
    });
  });

  it('monitor: éxito, aviso si ya no estaba y error sin conexión', async () => {
    const bien = {
      async quitarMonitor() {
        return undefined;
      },
    };
    expect(await quitarMonitorDelProyecto(bien, 1, 12, 'Carlos Ruiz')).toEqual({
      tipo: 'exito',
      texto: 'Carlos Ruiz ya no es monitor de este proyecto.',
    });
    const falla = (error: ErrorApi) => ({
      async quitarMonitor(): Promise<void> {
        throw error;
      },
    });
    expect(
      await quitarMonitorDelProyecto(falla(new ErrorApi(404, 'NO_ENCONTRADO', 'Ese monitor no está asignado al proyecto')), 1, 12, 'Carlos Ruiz'),
    ).toEqual({ tipo: 'aviso', texto: YA_NO_ES_MONITOR });
    expect(
      await quitarMonitorDelProyecto(
        falla(new ErrorApi(0, 'SIN_CONEXION', 'No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo')),
        1,
        12,
        'Carlos Ruiz',
      ),
    ).toEqual({ tipo: 'error', texto: 'No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo' });
  });

  it('al confirmar se revisa la lista actual: no se quita a quien ahora es líder ni a quien ya salió', () => {
    const miembros: MiembroVista[] = [
      { idUsuario: 9, nombre: 'Ana', rolEquipo: 'LIDER' },
      { idUsuario: 8, nombre: 'Luis', rolEquipo: 'INTEGRANTE' },
    ];
    expect(bloqueoParaQuitar(miembros, 8)).toBeNull();
    expect(bloqueoParaQuitar(miembros, 9)).toBe(ES_LIDER_AHORA);
    expect(bloqueoParaQuitar(miembros, 7)).toBe(YA_NO_ES_MIEMBRO);
    expect(ES_LIDER_AHORA).toBe('Esa persona es ahora el líder del equipo. Designa a otro líder antes de quitarla.');
  });

  it('retirar un correo pendiente', async () => {
    const llamadas: number[] = [];
    const falsa = async (idCorreoHabilitado: number) => {
      llamadas.push(idCorreoHabilitado);
    };
    expect(await retirarHabilitacion(falsa, { idCorreoHabilitado: 40, email: 'ana@uninorte.edu.co' })).toEqual({
      mensaje: { tipo: 'exito', texto: 'Se retiró la habilitación de ana@uninorte.edu.co.' },
      recargar: true,
    });
    expect(llamadas).toEqual([40]);

    const falla = (error: ErrorApi) => async (): Promise<void> => {
      throw error;
    };

    const msg409 = 'Esa persona ya creó su cuenta. Quítala de su equipo o de los monitores del proyecto';
    expect(
      await retirarHabilitacion(falla(new ErrorApi(409, 'HABILITACION_USADA', msg409)), {
        idCorreoHabilitado: 40,
        email: 'ana@uninorte.edu.co',
      }),
    ).toEqual({
      mensaje: { tipo: 'error', texto: msg409 },
      recargar: true,
    });

    expect(
      await retirarHabilitacion(falla(new ErrorApi(404, 'NO_ENCONTRADO', 'No encontrado')), {
        idCorreoHabilitado: 40,
        email: 'ana@uninorte.edu.co',
      }),
    ).toEqual({
      mensaje: null,
      recargar: true,
    });

    expect(
      await retirarHabilitacion(falla(new ErrorApi(403, 'ACCESO_DENEGADO', 'Acceso denegado')), {
        idCorreoHabilitado: 40,
        email: 'ana@uninorte.edu.co',
      }),
    ).toEqual({
      mensaje: { tipo: 'error', texto: 'Solo el docente del proyecto puede cambiarlo' },
      recargar: false,
    });

    const correos: CorreoHabilitadoVista[] = [
      { idCorreoHabilitado: 1, email: 'a@u.edu.co', rol: 'ESTUDIANTE', idProyecto: null, idCurso: 1, fechaHabilitacion: '2026-11-01T00:00:00Z', registrado: false },
      { idCorreoHabilitado: 2, email: 'b@u.edu.co', rol: 'ESTUDIANTE', idProyecto: null, idCurso: 1, fechaHabilitacion: '2026-11-01T00:00:00Z', registrado: true },
      { idCorreoHabilitado: 3, email: 'c@u.edu.co', rol: 'MONITOR', idProyecto: 1, idCurso: null, fechaHabilitacion: '2026-11-01T00:00:00Z', registrado: false },
    ];
    expect(pendientesDe(correos)).toEqual([correos[0], correos[2]]);

    expect(preguntaRetirarCorreo('ana@uninorte.edu.co')).toBe(
      '¿Retirar la habilitación de ana@uninorte.edu.co? Esa persona ya no podrá crear su cuenta para este proyecto. Puedes volver a habilitarla cuando quieras.',
    );
    expect(preguntaRetirarCorreoCurso('ana@uninorte.edu.co')).toBe(
      '¿Retirar la habilitación de ana@uninorte.edu.co? Esa persona ya no podrá crear su cuenta con este curso. Puedes volver a agregar su correo cuando quieras.',
    );
  });

  it('retirarHabilitacion pide recargar en éxito (204), 404 y 409, pero no en 403 u otros errores', async () => {
    const apiExito = async () => undefined;
    const api404 = async (): Promise<void> => {
      throw new ErrorApi(404, 'NO_ENCONTRADO', 'No encontrado');
    };
    const api409 = async (): Promise<void> => {
      throw new ErrorApi(409, 'HABILITACION_USADA', 'Usada');
    };
    const api403 = async (): Promise<void> => {
      throw new ErrorApi(403, 'ACCESO_DENEGADO', 'Denegado');
    };

    const c = { idCorreoHabilitado: 10, email: 'x@u.edu.co' };
    expect((await retirarHabilitacion(apiExito, c)).recargar).toBe(true);
    expect((await retirarHabilitacion(api404, c)).recargar).toBe(true);
    expect((await retirarHabilitacion(api409, c)).recargar).toBe(true);
    expect((await retirarHabilitacion(api403, c)).recargar).toBe(false);
  });
});
