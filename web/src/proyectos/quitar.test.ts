import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { EquipoVista, MiembroVista } from '../api/tipos';
import {
  bloqueoParaQuitar,
  ES_LIDER_AHORA,
  preguntaQuitarIntegrante,
  preguntaQuitarMonitor,
  quitarIntegrante,
  quitarMonitorDelProyecto,
  sePuedeQuitar,
  textoIntegranteQuitado,
  YA_NO_ES_MIEMBRO,
  YA_NO_ES_MONITOR,
} from './quitar';

const equipo: EquipoVista = { idEquipo: 3, idProyecto: 1, nombreEquipo: 'Equipo Alfa', miembros: [], hitos: [] };

describe('quitar integrantes y monitores', () => {
  it('al líder no se le ofrece quitar; las preguntas dicen que se conservan tareas e historial', () => {
    expect(sePuedeQuitar({ idUsuario: 9, nombre: 'Ana', rolEquipo: 'LIDER' })).toBe(false);
    expect(sePuedeQuitar({ idUsuario: 8, nombre: 'Luis', rolEquipo: 'INTEGRANTE' })).toBe(true);
    expect(preguntaQuitarIntegrante('Luis')).toBe(
      '¿Quitar a Luis del equipo? Sus tareas y su historial se conservan, y el líder puede reasignar sus tareas.',
    );
    expect(preguntaQuitarMonitor('Carlos Ruiz')).toBe(
      '¿Quitar a Carlos Ruiz de los monitores del proyecto? Su historial se conserva, pero con ese correo no se '
        + 'podrá volver a agregar desde aquí.',
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
});
