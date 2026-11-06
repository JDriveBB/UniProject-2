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
  /** null = el hito no tiene alerta de inactividad (bloque 30). */
  diasInactividadAlerta: number | null;
  notificarDocenteInactividad: boolean;
  criterios: CriterioVista[];
}

/** POST /api/proyectos/{id}/hitos y PUT /api/hitos/{id} (reemplaza todo). diasInactividadAlerta: 1 a 60 (null = sin alerta). */
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

/* ===== Bloque 26c: tablero, tareas, dependencias, ruta crítica y envío (docs/openapi/openapi.yaml) ===== */

export type EstadoTarea = 'PENDIENTE' | 'EN_PROGRESO' | 'COMPLETADA';
export type NivelRiesgo = 'BAJO' | 'MEDIO' | 'ALTO';

/** Horas: número JSON (Decimal). Fechas "aaaa-mm-dd"; fechaCompletada y fechaBloqueo, instantes UTC. */
export interface TareaVista {
  idTarea: number;
  idHitoEquipo: number;
  titulo: string;
  descripcion: string | null;
  idEncargado: number | null;
  encargado: string | null;
  idTareaOrigen: number | null;
  estado: EstadoTarea;
  porcentajeAvance: number;
  horasEstimadas: number;
  fechaLimite: string | null;
  fechaLimiteEfectiva: string;
  fechaCompletada: string | null;
  esPrerrequisitoCritico: boolean;
  bloqueada: boolean;
  fechaBloqueo: string | null;
  enRutaCritica: boolean;
  holguraHoras: number | null;
  nivelRiesgo: NivelRiesgo | null;
  prerrequisitos: number[];
}

/** GET /api/hitos-equipo/{id}/tareas. editable: decisión E1 (EN_CURSO sí; ENVIADO no; EVALUADO hasta que vence). */
export interface TableroHito {
  idHitoEquipo: number;
  idHito: number;
  nombreHito: string;
  fechaLimite: string;
  estado: EstadoHitoEquipo;
  editable: boolean;
  idEquipo: number;
  nombreEquipo: string;
  idProyecto: number;
  nombreProyecto: string;
  tareas: TareaVista[];
}

/** POST /api/hitos-equipo/{id}/tareas (solo el líder). horasEstimadas > 0 y ≤ 9999,99 con 2 decimales. */
export interface SolicitudTarea {
  titulo: string;
  descripcion: string | null;
  idEncargado: number | null;
  horasEstimadas: number;
  fechaLimite: string | null;
  esPrerrequisitoCritico: boolean | null;
}

/** POST /api/tareas/{id}/dependencias: de 1 a 50 prerrequisitos. */
export interface SolicitudDependencias {
  prerrequisitos: number[];
}

export interface ResultadoDependencias {
  aceptados: number[];
  ciclicos: number[];
  tarea: TareaVista;
}

export interface NodoRutaCritica {
  idTarea: number;
  titulo: string;
  horasEstimadas: number;
  porcentajeAvance: number;
  critica: boolean;
  inicioTempranoHoras: number;
  finTempranoHoras: number;
  inicioTardioHoras: number;
  finTardioHoras: number;
  holguraHoras: number;
  prerrequisitos: number[];
}

/** GET /api/hitos-equipo/{id}/ruta-critica. nodos en orden topológico (el núcleo los ordena). */
export interface RutaCriticaVista {
  idHitoEquipo: number;
  duracionTotalHoras: number;
  criticas: number[];
  nodos: NodoRutaCritica[];
}

/** POST /api/hitos-equipo/{id}/envio (RF7). */
export interface EnvioVista {
  idEnvio: number;
  idHitoEquipo: number;
  idUsuarioEnvia: number;
  enviadoPor: string;
  fechaEnvio: string;
}

/* Tipos del Bloque 26d (detalle de tarea, evidencias, correcciones, reasignación, apoyo, mis tareas) */

export type TipoEvidencia = 'PDF' | 'IMAGEN' | 'ENLACE';
export type EstadoSolicitudCorreccion = 'PENDIENTE' | 'ATENDIDA';
export type TipoReasignacion = 'TOTAL' | 'PARCIAL';

export interface AvanceVista {
  idRegistro: number;
  idUsuario: number;
  usuario: string;
  porcentajeAvance: number;
  observacion: string | null;
  fechaHora: string;
}

export interface TareaDetalle {
  tarea: TareaVista;
  editable: boolean;
  avances: AvanceVista[];
}

export interface SolicitudAvance {
  porcentajeAvance: number;
  observacion: string | null;
}

export interface SolicitudEnlace {
  url: string;
}

export interface EvidenciaVista {
  idEvidencia: number;
  idTarea: number;
  idUsuario: number;
  autor: string;
  tipoEvidencia: TipoEvidencia;
  enlace: string | null;
  nombreArchivo: string | null;
  descarga: string | null;
  fechaSubida: string;
}

export interface SolicitudComentario {
  contenido: string;
}

export interface ComentarioVista {
  idComentario: number;
  idTarea: number;
  idAutor: number;
  autor: string;
  rolAutor: RolPlataforma;
  contenido: string;
  fechaHora: string;
}

export interface PeticionCorreccion {
  idEvidencia: number;
  observaciones: string;
}

export interface CorreccionVista {
  idSolicitud: number;
  idTarea: number;
  idEvidencia: number;
  idLider: number;
  lider: string;
  idEstudiante: number;
  estudiante: string;
  observaciones: string;
  estado: EstadoSolicitudCorreccion;
  fechaSolicitud: string;
  fechaAtencion: string | null;
}

export interface SolicitudReasignacion {
  idNuevoEncargado: number;
  tipo: TipoReasignacion;
  tituloParcial: string | null;
  horasParcial: number | null;
  motivo: string | null;
}

export interface ResultadoReasignacion {
  tarea: TareaVista;
  tareaNueva: TareaVista | null;
}

export interface SolicitudApoyo {
  mensaje: string;
}

export interface ApoyoVista {
  idSolicitud: number;
  idTarea: number;
  titulo: string;
  idAutor: number;
  autor: string;
  mensaje: string | null;
  fechaHora: string;
}

export interface MiTareaVista {
  idTarea: number;
  titulo: string;
  estado: EstadoTarea;
  porcentajeAvance: number;
  fechaLimite: string;
  bloqueada: boolean;
  nivelRiesgo: NivelRiesgo;
  idHitoEquipo: number;
  nombreHito: string;
  proyecto: string;
}
