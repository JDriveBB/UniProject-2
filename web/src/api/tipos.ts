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

/** POST /api/cuentas/registro. contrasena: 10 a 72 bytes; autorizaTratamientoDatos debe ser true (Ley 1581); codigo: 43 caracteres del enlace de invitación. */
export interface SolicitudRegistro {
  nombre: string;
  email: string;
  contrasena: string;
  autorizaTratamientoDatos: boolean;
  codigo: string;
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
  idCurso: number | null;
  fechaHabilitacion: string;
  registrado: boolean;
}

/** POST /api/proyectos/{id}/correos/{id}/enlace y /api/admin/docentes/{id}/enlace. El código es secreto: no se guarda ni se registra. */
export interface EnlaceRegistro {
  codigo: string;
  venceEn: string;
}

/* ===== Bloque 26b: proyectos, hitos, rúbricas, equipos y bandeja (docs/openapi/openapi.yaml) ===== */

export type RolEquipo = 'LIDER' | 'INTEGRANTE';
export type EstadoHitoEquipo = 'EN_CURSO' | 'ENVIADO' | 'EVALUADO';
export type EstadoEnvioNotificacion = 'EN_COLA' | 'SIN_TELEGRAM';

/** POST /api/proyectos y PUT /api/proyectos/{id} (reemplaza todo). Fechas "aaaa-mm-dd". equiposMixtos solo viaja al crear. */
export interface SolicitudProyecto {
  nombre: string;
  descripcion: string | null;
  fechaInicio: string;
  fechaFin: string;
  usaMonitores: boolean;
  equiposMixtos?: boolean;
}

export interface ProyectoVista {
  idProyecto: number;
  idDocente: number;
  nombre: string;
  descripcion: string | null;
  fechaInicio: string;
  fechaFin: string;
  usaMonitores: boolean;
  equiposMixtos: boolean;
  cierreEquipos: string | null;
  horasRonda: number;
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
  /** Último envío de este hito del equipo (date-time); null si no se ha enviado. */
  fechaEnvio: string | null;
}

export interface EquipoVista {
  idEquipo: number;
  idProyecto: number;
  nombreEquipo: string;
  idCurso: number | null;
  cupo: number | null;
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
  idCurso: number | null;
  cupo: number | null;
}

export interface SolicitudMiembro {
  email: string;
  pasarCupo?: boolean;
}

export interface SolicitudLider {
  idUsuario: number;
}

/** GET /api/proyectos/{id}/estudiantes: estudiantes participantes del proyecto con su curso y equipo. */
export interface EstudianteProyectoVista {
  idUsuario: number;
  email: string;
  nombre: string;
  idCurso: number;
  nombreCurso: string;
  idEquipo: number | null;
}

/** GET /api/proyectos/{id}/estudiantes-disponibles: estudiantes participantes sin equipo en el proyecto, con su curso. */
export interface EstudianteDisponibleVista {
  email: string;
  nombre: string;
  idCurso: number;
  nombreCurso: string;
}

/** POST /api/proyectos/{id}/correos: habilita correos de monitores en el proyecto. */
export interface SolicitudCorreos {
  emails: string[];
  rol: 'MONITOR';
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
  /** Primer hito del equipo, por orden, en EN_CURSO; null si no hay. */
  hitoActual: HitoActualPanel | null;
  /** Avance del hito en curso ponderado por horas estimadas (0..100); null sin hito en curso o sin tareas. */
  avanceHitoActual: number | null;
  /** El equipo tiene hitos y ninguno está EN_CURSO. */
  todoEnviado: boolean;
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

/* ===== Bloque 51: cursos, equipos con cupo, inscripción y votación ===== */

/** POST /api/cursos y PUT /api/cursos/{id}. */
export interface SolicitudCurso {
  nombre: string;
  periodo: string;
  descripcion: string | null;
}

/** PUT /api/cursos/{id}/archivado. */
export interface SolicitudArchivado {
  archivado: boolean;
}

/** POST /api/cursos/{id}/correos. */
export interface SolicitudCorreosCurso {
  emails: string[];
}

/** Resumen de un curso en listados y asignaciones de proyecto. */
export interface CursoResumen {
  idCurso: number;
  nombre: string;
  periodo: string;
  archivado: boolean;
}

/** Resumen de un proyecto dentro de un curso. */
export interface ProyectoResumen {
  idProyecto: number;
  nombre: string;
}

/** GET /api/cursos y respuestas de POST/PUT /api/cursos. */
export interface CursoVista {
  idCurso: number;
  nombre: string;
  periodo: string;
  descripcion: string | null;
  archivado: boolean;
  nombreDocente: string;
  estudiantes: number;
  proyectos: ProyectoResumen[];
}

/** Estudiante con cuenta inscrito en un curso (GET /api/cursos/{id}). */
export interface EstudianteCursoVista {
  idUsuario: number;
  nombre: string;
  email: string;
  fechaIngreso: string;
}

/** GET /api/cursos/{id}. */
export interface CursoDetalle {
  curso: CursoVista;
  estudiantes: EstudianteCursoVista[];
  pendientes: CorreoHabilitadoVista[];
}

/** GET /api/cursos/{id}/companeros (solo nombre, Ley 1581). */
export interface CompaneroVista {
  nombre: string;
}

/** POST /api/cursos/{id}/correos. */
export interface ResultadoCorreosCurso {
  agregados: string[];
  habilitados: CorreoHabilitadoVista[];
  yaEnCurso: string[];
  conOtroRol: string[];
}

/** PUT /api/proyectos/{id}/cursos. */
export interface SolicitudCursosProyecto {
  idsCursos: number[];
  confirmar: boolean;
}

/** Estudiante que aparece en varios cursos asignados a un mismo proyecto. */
export interface EstudianteRepetidoVista {
  idUsuario: number;
  nombre: string;
  email: string;
  cursos: CursoResumen[];
  idCursoQueCuenta: number;
}

/** Respuesta de PUT /api/proyectos/{id}/cursos. */
export interface ResultadoCursosProyecto {
  aplicado: boolean;
  cursos: CursoResumen[];
  repetidos: EstudianteRepetidoVista[];
}

/** PUT /api/proyectos/{id}/estudiantes/{idUsuario}/curso. */
export interface SolicitudCursoEstudiante {
  idCurso: number;
}

/** POST /api/proyectos/{id}/equipos/lote. */
export interface SolicitudEquiposLote {
  prefijo: string;
  cantidad: number | null;
  idCurso: number | null;
  cupo: number;
}

/** PUT /api/equipos/{id}/cupo. */
export interface SolicitudCupo {
  cupo: number | null;
}

/** PUT /api/proyectos/{id}/inscripcion. */
export interface SolicitudInscripcion {
  cierreEquipos: string | null;
  horasRonda: number;
}

/** Equipo mostrado en la pantalla de inscripción del proyecto. */
export interface EquipoInscripcionVista {
  idEquipo: number;
  nombreEquipo: string;
  idCurso: number | null;
  nombreCurso: string | null;
  cupo: number | null;
  integrantes: number;
  nombres: string[];
  puedeUnirse: boolean;
}

/** GET y PUT /api/proyectos/{id}/inscripcion. */
export interface InscripcionVista {
  cierreEquipos: string | null;
  horasRonda: number;
  abierta: boolean;
  idCursoQueCuenta: number | null;
  idMiEquipo: number | null;
  equipos: EquipoInscripcionVista[];
}

/** Asignación realizada por el reparto automático de equipos. */
export interface AsignacionRepartoVista {
  idUsuario: number;
  nombre: string;
  idEquipo: number;
  nombreEquipo: string;
}

/** POST /api/proyectos/{id}/equipos/reparto. */
export interface ResultadoReparto {
  asignados: AsignacionRepartoVista[];
  sinCupo: EstudianteDisponibleVista[];
}

export type OrigenRonda = 'PROGRAMADA' | 'DEMANDA';
export type ResultadoRonda = 'LIDER_ELEGIDO' | 'EMPATE' | 'SIN_VOTOS' | 'CANCELADA';

/** Candidato de una ronda de votación; votos es null mientras la ronda no cierre y en CANCELADA. */
export interface CandidatoVista {
  idUsuario: number;
  nombre: string;
  votos: number | null;
  empatado: boolean;
}

/** Ronda de votación de líder de un equipo. */
export interface RondaVista {
  idRonda: number;
  origen: OrigenRonda;
  abreEn: string;
  cierraEn: string;
  cerradaEn: string | null;
  abierta: boolean;
  resultado: ResultadoRonda | null;
  idLiderElegido: number | null;
  nombreLiderElegido: string | null;
  votosEmitidos: number;
  votantes: number;
  miVoto: number | null;
  puedoVotar: boolean;
  candidatos: CandidatoVista[];
}

/** GET/POST /api/equipos/{id}/ronda y PUT voto/desempate. */
export interface VotacionVista {
  idEquipo: number;
  idLiderActual: number | null;
  nombreLiderActual: string | null;
  ronda: RondaVista | null;
  puedeAbrir: boolean;
  puedeDesempatar: boolean;
}

/** PUT /api/equipos/{id}/ronda/voto. */
export interface SolicitudVoto {
  idCandidato: number;
}

/** PUT /api/equipos/{id}/ronda/desempate. */
export interface SolicitudDesempate {
  idUsuario: number;
}
