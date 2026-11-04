/** Tipos de la API (docs/openapi/openapi.yaml). Las pantallas 26a–26f agregan aquí los que usen. */
export type RolPlataforma = 'ADMINISTRADOR' | 'DOCENTE' | 'ESTUDIANTE' | 'MONITOR';

export interface RespuestaError {
  codigo: string;
  mensaje: string;
  detalles: string[];
}

export interface UsuarioSesion {
  idUsuario: number;
  nombre: string;
  email: string;
  rol: RolPlataforma;
  telegramVinculado: boolean;
}

/** POST /api/cuentas/registro. contrasena: 10 a 72 bytes; autorizaTratamientoDatos debe ser true (Ley 1581). */
export interface SolicitudRegistro {
  nombre: string;
  email: string;
  contrasena: string;
  autorizaTratamientoDatos: boolean;
}

/** POST /api/admin/docentes. */
export interface SolicitudHabilitarCorreo {
  email: string;
}

/** fechaHabilitacion: instante UTC (date-time). registrado: el correo ya se usó para crear una cuenta. */
export interface CorreoHabilitadoVista {
  idCorreoHabilitado: number;
  email: string;
  rol: RolPlataforma;
  idProyecto: number | null;
  fechaHabilitacion: string;
  registrado: boolean;
}
