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
  /** Último envío de este hito del equipo (date-time); null si no se ha enviado. Opcional solo para datos armados a mano en pruebas. */
  fechaEnvio?: string | null;
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

/** POST /api/equipos/{id}/invitaciones (D-12): correo habilitado de un estudiante sin cuenta. */
export interface SolicitudInvitacion {
  email: string;
}

/** GET /api/proyectos/{id}/estudiantes (D-11, D-12): correo de estudiante habilitado, con o sin cuenta. */
export interface EstudianteProyectoVista {
  email: string;
  /** null si aún no tiene cuenta. */
  nombre: string | null;
  registrado: boolean;
  /** Equipo del proyecto donde es integrante, o null. */
  idEquipo: number | null;
  /** Equipo al que entrará cuando cree su cuenta, o null. */
  idEquipoInvitado: number | null;
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
  nivelRiesgo: NivelRiesgo | null;
  idHitoEquipo: number;
  nombreHito: string;
  proyecto: string;
}

/* Tipos del Bloque 26e (evaluación del hito, notas individuales) */

export type CaracterNota = 'PROVISIONAL' | 'DEFINITIVA';

export interface CalificacionSolicitada {
  idCriterio: number;
  nota: number;
  retroalimentacion: string | null;
}

export interface SolicitudEvaluacion {
  criterios: CalificacionSolicitada[];
  notaGrupal: number | null;
  retroalimentacion: string | null;
}

export interface CambioNotaGrupal {
  notaGrupal: number;
}

export interface ConfirmacionNota {
  notaFinal: number;
}

export interface CriterioEvaluadoVista {
  idCriterio: number;
  descripcion: string;
  pesoPorcentaje: number;
  puntajeMaximo: number;
  nota: number;
  retroalimentacion: string | null;
}

export interface NotaIndividualVista {
  idEstudiante: number;
  nombre: string;
  factorAjuste: number | null;
  notaSugerida: number | null;
  notaFinal: number | null;
  fechaConfirmacion: string | null;
}

export interface EvaluacionVista {
  idEvaluacion: number;
  idHitoEquipo: number;
  idEnvio: number;
  fechaEnvio: string;
  fechaEvaluacion: string;
  notaGrupal: number;
  notaCalculada: number | null;
  retroalimentacion: string | null;
  caracter: CaracterNota;
  criterios: CriterioEvaluadoVista[];
  notas: NotaIndividualVista[];
}

/** POST /api/cuentas/recuperacion: siempre 202 sin cuerpo (bloque 28). */
export interface SolicitudRecuperacion {
  email: string;
}

/** El código es secreto: no se guarda ni se registra. */
export interface SolicitudRestablecer {
  codigo: string;
  contrasenaNueva: string;
}

/* Tipos del Bloque 26f-1 (panel del docente, RF10) */
export interface IntegranteInactivo {
  idUsuario: number;
  nombre: string;
  ultimaActividad: string;
  diasSinActividad: number;
}

export interface TareaBloqueada {
  idTarea: number;
  titulo: string;
  fechaBloqueo: string;
  horasBloqueada: number;
}

export interface PanelEquipo {
  idEquipo: number;
  nombreEquipo: string;
  enRojo: boolean;
  tareasTotales: number;
  tareasCompletadas: number;
  tareasRiesgoAlto: number;
  /** Promedio simple de todas las tareas; se conserva por compatibilidad. La web usa avanceHitoActual (D-12). */
  avancePromedio: number;
  integrantesInactivos: IntegranteInactivo[];
  tareasBloqueadas: TareaBloqueada[];
  /* D-12: opcionales solo para los datos armados a mano en pruebas; el núcleo siempre los manda. */
  /** Primer hito del equipo, por orden, en EN_CURSO; null si no hay. */
  hitoActual?: HitoActualPanel | null;
  /** Avance del hito en curso ponderado por horas estimadas (0..100); null sin hito en curso o sin tareas. */
  avanceHitoActual?: number | null;
  /** El equipo tiene hitos y ninguno está EN_CURSO. */
  todoEnviado?: boolean;
}

export interface HitoActualPanel {
  idHitoEquipo: number;
  orden: number;
  nombreHito: string;
}

export interface PanelProyecto {
  idProyecto: number;
  nombre: string;
  generadoEn: string;
  equipos: PanelEquipo[];
}

/* Tipos del Bloque 26f-3 (perfil: datos, Telegram, contraseña, supresión) */
export interface DatosPersonales {
  idUsuario: number;
  nombre: string;
  email: string;
  rol: RolPlataforma;
  telegramVinculado: boolean;
  autorizaTratamientoDatos: boolean;
  fechaAutorizacion: string | null;
  fechaRegistro: string;
}

export interface SolicitudNombre {
  nombre: string;
}

export interface SolicitudCambioContrasena {
  contrasenaActual: string;
  contrasenaNueva: string;
}

export interface SolicitudSupresion {
  contrasena: string;
}

export interface CodigoGenerado {
  codigo: string;
  venceEn: string;
  enlace: string;
}

/* Tipos del Bloque 26p (trazabilidad del equipo) */
export interface EntradaHistorial {
  idLog: number;
  fechaHora: string;
  idUsuario: number | null;
  autor: string;
  accion: string;
  entidad: string;
  idRegistro: number | null;
  campo: string | null;
  valorAnterior: string | null;
  valorNuevo: string | null;
  detalle: string;
}
