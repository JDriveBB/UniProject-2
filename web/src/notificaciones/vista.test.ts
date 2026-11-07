import { describe, expect, it } from 'vitest';
import type { Bandeja, NotificacionVista } from '../api/tipos';
import {
  debeMarcarAlAbrir,
  enlaceInterno,
  marcarEnBandeja,
  marcarTodasEnBandeja,
  marcaTipo,
  textoNoLeidas,
} from './vista';

function notificacion(parcial: Partial<NotificacionVista> = {}): NotificacionVista {
  return {
    idNotificacion: 1,
    tipo: 'hito.enviado',
    mensaje: 'Mensaje de prueba',
    entidadReferencia: 'HITO_EQUIPO',
    idReferencia: 1,
    enlace: '/hitos/1/evaluar',
    estadoEnvio: 'EN_COLA',
    leida: false,
    fechaCreacion: '2026-09-23T15:00:00Z',
    ...parcial,
  };
}

describe('notificaciones/vista', () => {
  it('marcaTipo y textoNoLeidas', () => {
    expect(marcaTipo('hito.enviado')).toEqual({ texto: 'Hito enviado', tono: 'critico' });
    expect(marcaTipo('hito.evaluado')).toEqual({ texto: 'Hito evaluado', tono: 'exito' });
    expect(marcaTipo('tarea.ruta_critica.riesgo')).toEqual({
      texto: 'Riesgo en ruta crítica',
      tono: 'peligro',
    });
    expect(marcaTipo('equipo.inactividad')).toEqual({ texto: 'Inactividad', tono: 'aviso' });
    expect(marcaTipo('tarea.reasignada')).toEqual({ texto: 'Tarea reasignada', tono: 'info' });
    expect(marcaTipo('tarea.correccion')).toEqual({ texto: 'Corrección solicitada', tono: 'aviso' });
    expect(marcaTipo('otro.tipo')).toEqual({ texto: 'Aviso', tono: 'neutro' });
    expect(marcaTipo('constructor')).toEqual({ texto: 'Aviso', tono: 'neutro' });

    expect(textoNoLeidas(0)).toBe('No tienes notificaciones sin leer.');
    expect(textoNoLeidas(1)).toBe('Tienes 1 notificación sin leer.');
    expect(textoNoLeidas(7)).toBe('Tienes 7 notificaciones sin leer.');
  });

  it('enlaceInterno acepta solo rutas internas seguras', () => {
    expect(enlaceInterno('/tareas/5')).toBe('/tareas/5');
    expect(enlaceInterno('/hitos/3/evaluar')).toBe('/hitos/3/evaluar');
    expect(enlaceInterno('//otro.example/x')).toBeNull();
    expect(enlaceInterno('javascript:alert(1)')).toBeNull();
    expect(enlaceInterno('')).toBeNull();
    expect(enlaceInterno('/a\\b')).toBeNull();
  });

  it('marcarEnBandeja y marcarTodasEnBandeja no mutan la bandeja original', () => {
    const n1 = notificacion({ idNotificacion: 1, leida: false });
    const n2 = notificacion({ idNotificacion: 2, leida: true });
    const n3 = notificacion({ idNotificacion: 3, leida: false });
    const b: Bandeja = {
      noLeidas: 3,
      notificaciones: [n1, n2, n3],
    };

    const res1 = marcarEnBandeja(b, 1);
    expect(res1.noLeidas).toBe(2);
    expect(res1.notificaciones[0]?.leida).toBe(true);
    expect(res1.notificaciones[2]?.leida).toBe(false);
    expect(b.noLeidas).toBe(3);
    expect(n1.leida).toBe(false);

    const res2 = marcarEnBandeja(b, 2);
    expect(res2.noLeidas).toBe(3);

    const res99 = marcarEnBandeja(b, 99);
    expect(res99.noLeidas).toBe(3);

    const resTodas = marcarTodasEnBandeja(b);
    expect(resTodas.noLeidas).toBe(0);
    expect(resTodas.notificaciones.every((n) => n.leida)).toBe(true);
    expect(b.noLeidas).toBe(3);
    expect(n1.leida).toBe(false);

    const bCero: Bandeja = {
      noLeidas: 0,
      notificaciones: [notificacion({ idNotificacion: 1, leida: false })],
    };
    const resCero = marcarEnBandeja(bCero, 1);
    expect(resCero.noLeidas).toBe(0);
  });

  it('debeMarcarAlAbrir decide si abrir debe marcar como leída según el aviso', () => {
    expect(debeMarcarAlAbrir({ leida: false })).toBe(true);
    expect(debeMarcarAlAbrir({ leida: true })).toBe(false);
  });
});
