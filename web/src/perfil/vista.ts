import { ErrorApi } from '../api/cliente';
import type { DatosPersonales, RolPlataforma } from '../api/tipos';
import { fechaHora } from '../formato';
import { mensajeErrorApi, type Campos } from '../proyectos/errores';

export const ID_TELEGRAM = 'telegram';
export const SIN_PERMISO_PERFIL = 'No puedes hacer este cambio en tu cuenta.';

export const CAMPOS_NOMBRE: Campos = [['nombre', 'Nombre completo']];
export const CAMPOS_CONTRASENA: Campos = [
  ['contrasenaActual', 'Contraseña actual'],
  ['contrasenaNueva', 'Nueva contraseña'],
];
export const CAMPOS_SUPRESION: Campos = [['contrasena', 'Contraseña actual']];

export const NOMBRE_ACTUALIZADO = 'Nombre actualizado.';
export const CONTRASENA_ACTUALIZADA = 'Contraseña actualizada. Cerramos tus otras sesiones abiertas.';
export const SIN_TELEGRAM_RECUPERAR = 'Sin Telegram vinculado no podrás recuperar tu contraseña si la olvidas.';
export const TELEGRAM_OBLIGATORIO =
  'Telegram es obligatorio en UniProject: por ahí te llegan los avisos de tus proyectos y el enlace para recuperar tu contraseña.';
export const TELEGRAM_ACTIVO = 'Recibes los avisos de UniProject por Telegram.';
export const TELEGRAM_VINCULADO = 'Telegram vinculado. Desde ahora recibirás los avisos por Telegram.';
export const TELEGRAM_PENDIENTE =
  'Aún no vemos la vinculación. Abre el enlace, pulsa «Iniciar» en Telegram y vuelve a intentarlo.';
export const TELEGRAM_DESVINCULADO = 'Desvinculaste Telegram. Los avisos seguirán llegando a tu bandeja.';
export const TEXTO_DESVINCULAR =
  'Dejarás de recibir los avisos por Telegram (seguirán en tu bandeja) y no podrás recuperar tu contraseña si la olvidas. Telegram es obligatorio en UniProject: vuelve a vincularlo pronto.';
export const TEXTO_ELIMINAR =
  'Puedes revocar tu autorización de tratamiento de datos y eliminar tu cuenta (Ley 1581 de 2012). No se puede deshacer.';

export const SE_BORRA: readonly string[] = [
  'Tu nombre, tu correo y tu contraseña (se reemplazan por valores anónimos).',
  'Tu vinculación con Telegram y tus notificaciones.',
  'El texto de tus comentarios y de tus solicitudes de apoyo.',
  'Tu nombre en los avisos y en el historial de cambios.',
  'Los nombres de los archivos que subiste como evidencia.',
  'Las invitaciones pendientes con tu correo.',
];

export const SE_CONSERVA: readonly string[] = [
  'Tus tareas, avances y evidencias, sin tu nombre: son parte del trabajo de tu equipo.',
  'Tus notas y evaluaciones, sin tu nombre.',
];

export const REGLAS_ELIMINAR =
  'Si eres docente con proyectos activos, o líder de un equipo en un proyecto activo, primero hay que cerrar el proyecto o pasar el liderazgo.';
export const ADMIN_NO_ELIMINA = 'La cuenta de administrador no se puede eliminar desde la aplicación.';
export const TEXTO_CONFIRMAR_ELIMINAR =
  'Se anonimizarán tus datos personales y se cerrarán todas tus sesiones. No se puede deshacer: para volver a usar UniProject necesitarás que te habiliten de nuevo.';

export function esEnlaceTelegram(enlace: string): boolean {
  try {
    const u = new URL(enlace);
    return u.protocol === 'https:' && u.hostname === 't.me';
  } catch {
    return false;
  }
}

export function textoVence(venceEn: string): string {
  return `El código vence el ${fechaHora(venceEn)}.`;
}

export function textoAutorizacion(d: Pick<DatosPersonales, 'autorizaTratamientoDatos' | 'fechaAutorizacion'>): string {
  if (!d.autorizaTratamientoDatos) {
    return 'No autorizada';
  }
  if (d.fechaAutorizacion !== null) {
    return `Autorizada el ${fechaHora(d.fechaAutorizacion)}`;
  }
  return 'Autorizada';
}

export function puedeEliminarse(rol: RolPlataforma): boolean {
  return rol !== 'ADMINISTRADOR';
}

export function mensajeErrorPerfil(error: unknown, campos: Campos): string {
  if (error instanceof ErrorApi && error.estado === 403 && error.codigo === 'ACCESO_DENEGADO') {
    return SIN_PERMISO_PERFIL;
  }
  return mensajeErrorApi(error, campos);
}
