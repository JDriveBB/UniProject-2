/** Caracteres de control: el parser de URL del navegador quita tabuladores y saltos ("/\t/host" queda "//host"). */
const CONTROL = /[\u0000-\u001F\u007F]/;

/**
 * Ruta interna a la que volver tras ingresar (state.desde, lo pone RutaProtegida). Evita redirecciones abiertas con
 * state manipulado (decisión 26a-6): solo "/algo"; nunca "//host", ni "\" en ningún punto (el navegador la trata
 * como "/": "/\host" es "//host"), ni caracteres de control.
 */
export function destinoSeguro(estado: unknown): string {
  if (typeof estado === 'object' && estado !== null && 'desde' in estado) {
    const desde = (estado as { desde: unknown }).desde;
    if (typeof desde === 'string' && desde.startsWith('/') && !desde.startsWith('//') && !desde.includes('\\')
        && !CONTROL.test(desde)) {
      return desde;
    }
  }
  return '/';
}

/** true si Registro mandó a ingresar después de crear la cuenta (state.cuentaCreada, decisión 26a-5). */
export function vieneDeRegistro(estado: unknown): boolean {
  return typeof estado === 'object' && estado !== null && 'cuentaCreada' in estado
    && (estado as { cuentaCreada: unknown }).cuentaCreada === true;
}

/** true si RestablecerContrasena mandó a ingresar tras cambiar la contraseña (state.contrasenaRestablecida, 26a-bis). */
export function vieneDeRestablecer(estado: unknown): boolean {
  return typeof estado === 'object' && estado !== null && 'contrasenaRestablecida' in estado
    && (estado as { contrasenaRestablecida: unknown }).contrasenaRestablecida === true;
}

/** true si Perfil mandó a ingresar tras suprimir la cuenta (state.cuentaEliminada, 26f-3). */
export function vieneDeSupresion(estado: unknown): boolean {
  return typeof estado === 'object' && estado !== null && 'cuentaEliminada' in estado
    && (estado as { cuentaEliminada: unknown }).cuentaEliminada === true;
}
