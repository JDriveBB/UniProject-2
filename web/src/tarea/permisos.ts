import type { RolEquipo } from '../api/tipos';

export interface ContextoPermisos {
  idUsuario: number;
  rolEquipo: RolEquipo | null;
  idEncargado: number | null;
  editable: boolean;
}

export interface PermisosTarea {
  avanzar: boolean;
  adjuntar: boolean;
  apoyo: boolean;
  gestionar: boolean;
  comentar: boolean;
}

/**
 * Permisos por rol en la tarea (decisión 26d-2).
 * Un encargado que ya no es integrante del equipo no cuenta como encargado.
 * Si el hito no es editable, todo es false.
 */
export function permisosTarea(c: ContextoPermisos): PermisosTarea {
  if (!c.editable) {
    return {
      avanzar: false,
      adjuntar: false,
      apoyo: false,
      gestionar: false,
      comentar: false,
    };
  }

  const esEncargado = c.rolEquipo !== null && c.idEncargado === c.idUsuario;
  const esLider = c.rolEquipo === 'LIDER';

  return {
    avanzar: esEncargado || esLider,
    adjuntar: esEncargado || esLider,
    apoyo: esEncargado,
    gestionar: esLider,
    comentar: true,
  };
}
