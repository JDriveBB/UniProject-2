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

/* ===== Bloque 26b: proyectos, hitos, rúbricas, equipos y bandeja (docs/openapi/openapi.yaml) ===== */

export type RolEquipo = 'LIDER' | 'INTEGRANTE';
export type EstadoHitoEquipo = 'EN_CURSO' | 'ENVIADO' | 'EVALUADO';
export type EstadoEnvioNotificacion = 'EN_COLA' | 'SIN_TELEGRAM';

/** POST /api/proyectos y PUT /api/proyectos/{id} (reemplaza todo). Fechas "aaaa-mm-dd". */
export interface SolicitudProyecto {
  nombre: string;
  descripcion: string | null;
  fechaInicio: string;
  fechaFin: string;
  usaMonitores: boolean;
}

export interface ProyectoVista {
  idProyecto: number;
  idDocente: number;
  nombre: string;
  descripcion: string | null;
  fechaInicio: string;
  fechaFin: string;
  usaMonitores: boolean;
}

export interface CriterioVista {
  idCriterio: number;
  idHito: number;
  descripcion: string;
  pesoPorcentaje: number;
  puntajeMaximo: number;
}

export interface HitoVista {
  idHito: number;
  idProyecto: number;
  nombreHito: string;
  descripcion: string | null;
  orden: number;
  fechaInicio: string;
  fechaLimite: string;
  diasInactividadAlerta: number;
  notificarDocenteInactividad: boolean;
  criterios: CriterioVista[];
}

/** POST /api/proyectos/{id}/hitos y PUT /api/hitos/{id} (reemplaza todo). diasInactividadAlerta: 1 a 60 (null = 3). */
export interface SolicitudHito {
  nombreHito: string;
  descripcion: string | null;
  orden: number;
  fechaInicio: string;
  fechaLimite: string;
  diasInactividadAlerta: number | null;
  notificarDocenteInactividad: boolean | null;
}

/** pesoPorcentaje: mayor que 0 y hasta 100; puntajeMaximo: mayor que 0 (numeric(5,2) en el núcleo). */
export interface SolicitudCriterio {
  descripcion: string;
  pesoPorcentaje: number;
  puntajeMaximo: number;
}

export interface MiembroVista {
  idUsuario: number;
  nombre: string;
  rolEquipo: RolEquipo;
}

export interface HitoEquipoVista {
  idHitoEquipo: number;
  idHito: number;
  estado: EstadoHitoEquipo;
}

export interface EquipoVista {
  idEquipo: number;
  idProyecto: number;
  nombreEquipo: string;
  miembros: MiembroVista[];
  hitos: HitoEquipoVista[];
}

export interface ProyectoDetalle {
  proyecto: ProyectoVista;
  hitos: HitoVista[];
  equipos: EquipoVista[];
}

export interface SolicitudEquipo {
  nombreEquipo: string;
}

export interface SolicitudMiembro {
  email: string;
}

export interface SolicitudLider {
  idUsuario: number;
}

/** POST /api/proyectos/{id}/correos (K2): habilita correos de estudiantes o monitores en el proyecto. */
export interface SolicitudCorreos {
  emails: string[];
  rol: 'ESTUDIANTE' | 'MONITOR';
}

export interface ResultadoHabilitacion {
  habilitados: CorreoHabilitadoVista[];
  yaHabilitados: string[];
  conOtroRol: string[];
}

export interface MonitorVista {
  idUsuario: number;
  nombre: string;
  email: string;
}

export interface NotificacionVista {
  idNotificacion: number;
  tipo: string;
  mensaje: string;
  entidadReferencia: string;
  idReferencia: number;
  enlace: string;
  fechaCreacion: string;
  estadoEnvio: EstadoEnvioNotificacion;
  leida: boolean;
}

export interface Bandeja {
  noLeidas: number;
  notificaciones: NotificacionVista[];
}
