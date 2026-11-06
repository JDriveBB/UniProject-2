import { describe, expect, it } from 'vitest';
import { permisosTarea } from './permisos';

describe('permisosTarea', () => {
  it('encargado integrante y líder encargado con hito editable', () => {
    expect(
      permisosTarea({
        idUsuario: 10,
        rolEquipo: 'INTEGRANTE',
        idEncargado: 10,
        editable: true,
      })
    ).toEqual({
      avanzar: true,
      adjuntar: true,
      apoyo: true,
      gestionar: false,
      comentar: true,
    });

    expect(
      permisosTarea({
        idUsuario: 10,
        rolEquipo: 'LIDER',
        idEncargado: 10,
        editable: true,
      })
    ).toEqual({
      avanzar: true,
      adjuntar: true,
      apoyo: true,
      gestionar: true,
      comentar: true,
    });
  });

  it('líder no encargado, integrante no encargado, monitor/docente y encargado que ya no es integrante', () => {
    expect(
      permisosTarea({
        idUsuario: 10,
        rolEquipo: 'LIDER',
        idEncargado: 20,
        editable: true,
      })
    ).toEqual({
      avanzar: true,
      adjuntar: true,
      apoyo: false,
      gestionar: true,
      comentar: true,
    });

    expect(
      permisosTarea({
        idUsuario: 10,
        rolEquipo: 'INTEGRANTE',
        idEncargado: 20,
        editable: true,
      })
    ).toEqual({
      avanzar: false,
      adjuntar: false,
      apoyo: false,
      gestionar: false,
      comentar: true,
    });

    expect(
      permisosTarea({
        idUsuario: 10,
        rolEquipo: null,
        idEncargado: 20,
        editable: true,
      })
    ).toEqual({
      avanzar: false,
      adjuntar: false,
      apoyo: false,
      gestionar: false,
      comentar: true,
    });

    expect(
      permisosTarea({
        idUsuario: 10,
        rolEquipo: null,
        idEncargado: 10,
        editable: true,
      })
    ).toEqual({
      avanzar: false,
      adjuntar: false,
      apoyo: false,
      gestionar: false,
      comentar: true,
    });
  });

  it('hito no editable, líder encargado', () => {
    expect(
      permisosTarea({
        idUsuario: 10,
        rolEquipo: 'LIDER',
        idEncargado: 10,
        editable: false,
      })
    ).toEqual({
      avanzar: false,
      adjuntar: false,
      apoyo: false,
      gestionar: false,
      comentar: false,
    });
  });
});
