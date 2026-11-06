import type { ProyectoVista, UsuarioSesion } from '../api/tipos';

export type ModoVista = 'edicion' | 'lectura';

/**
 * Decisión 26b-6: edita solo el docente dueño (la regla de Permisos.esDocenteDelProyecto); estudiantes, monitores y
 * otros docentes ven en solo lectura. proyecto null = proyecto nuevo. El servidor igual responde 403.
 */
export function modoDeVista(usuario: Pick<UsuarioSesion, 'idUsuario' | 'rol'>,
                            proyecto: Pick<ProyectoVista, 'idDocente'> | null): ModoVista {
  if (usuario.rol !== 'DOCENTE') {
    return 'lectura';
  }
  return proyecto === null || proyecto.idDocente === usuario.idUsuario ? 'edicion' : 'lectura';
}

/** Hasta 15 cifras: siempre un entero seguro de JavaScript. */
const ID = /^[1-9]\d{0,14}$/;

/** ":idProyecto" de la URL → número; null si no es un entero positivo (p. ej. "/proyectos/abc"). */
export function idDeRuta(texto: string | undefined): number | null {
  return texto !== undefined && ID.test(texto) ? Number(texto) : null;
}

/** true si se llega desde "Nuevo proyecto" recién creado (state.proyectoCreado; solo un booleano, 26a-5). */
export function vieneDeCreacion(estado: unknown): boolean {
  return typeof estado === 'object' && estado !== null && 'proyectoCreado' in estado
    && (estado as { proyectoCreado: unknown }).proyectoCreado === true;
}
