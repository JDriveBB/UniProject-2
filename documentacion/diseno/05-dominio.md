# 05. Modelo del dominio y casos de uso — UniProject

Este documento recoge los diagramas de clases reales de cada módulo de **UniProject** (entidades JPA, enumeraciones, servicios de aplicación y componentes de dominio puro) junto con el diagrama general de casos de uso y su correspondencia con los requerimientos funcionales de `entrega1.md`.

> **Por qué las entidades JPA usan atributos `Long` planos en lugar de asociaciones:** por la decisión arquitectónica DA-08 ([`03-decisiones.md`](03-decisiones.md) y `decisiones-tecnicas-java.md` sección 13), ninguna entidad de `nucleo` declara `@ManyToOne`, `@OneToMany` ni `@ManyToMany`. Cada clave foránea se mapea como un atributo `Long` explícito y las claves compuestas usan `@IdClass` (`MiembroEquipoId`, `MonitorProyectoId`, `DependenciaTareaId`, `CalificacionCriterioId`). Con `spring.jpa.open-in-view: false`, esto evita cargas perezosas imprevistas, consultas N+1 y acoplamiento entre módulos; las consultas compuestas se ejecutan en SQL fijo mediante `JdbcClient`.

---

## 5.1. Módulo `configuracionacademica`

```mermaid
classDiagram
    class RolEquipo {
        <<enumeration>>
        LIDER
        INTEGRANTE
    }

    class Proyecto {
        <<entity>>
        -Long idProyecto
        -Long idDocente
        -String nombre
        -String descripcion
        -LocalDate fechaInicio
        -LocalDate fechaFin
        -Boolean usaMonitores
    }

    class Hito {
        <<entity>>
        -Long idHito
        -Long idProyecto
        -String nombreHito
        -String descripcion
        -Integer orden
        -LocalDate fechaInicio
        -LocalDate fechaLimite
        -Integer diasInactividadAlerta
        -Boolean notificarDocenteInactividad
    }

    class CriterioRubrica {
        <<entity>>
        -Long idCriterio
        -Long idHito
        -String descripcion
        -BigDecimal pesoPorcentaje
        -BigDecimal puntajeMaximo
    }

    class Equipo {
        <<entity>>
        -Long idEquipo
        -Long idProyecto
        -String nombreEquipo
    }

    class MiembroEquipo {
        <<entity>>
        -Long idEquipo
        -Long idUsuario
        -RolEquipo rolEquipo
        -Instant fechaIngreso
    }

    class MonitorProyecto {
        <<entity>>
        -Long idProyecto
        -Long idMonitor
    }

    class ServicioProyectos {
        <<service>>
        +crear(UsuarioAutenticado, SolicitudProyecto) ProyectoVista
        +listar(UsuarioAutenticado) List~ProyectoVista~
        +detalle(UsuarioAutenticado, long) ProyectoDetalle
        +actualizar(UsuarioAutenticado, long, SolicitudProyecto) ProyectoVista
    }

    class ServicioHitos {
        <<service>>
        +crear(UsuarioAutenticado, long, SolicitudHito) HitoVista
        +actualizar(UsuarioAutenticado, long, SolicitudHito) HitoVista
        +crearCriterio(UsuarioAutenticado, long, SolicitudCriterio) CriterioVista
        +actualizarCriterio(UsuarioAutenticado, long, long, SolicitudCriterio) CriterioVista
        +eliminarCriterio(UsuarioAutenticado, long, long) void
    }

    class ServicioEquipos {
        <<service>>
        +crear(UsuarioAutenticado, long, SolicitudEquipo) EquipoVista
        +agregarMiembro(UsuarioAutenticado, long, SolicitudMiembro) EquipoVista
        +invitar(UsuarioAutenticado, long, SolicitudInvitacion) EstudianteProyectoVista
        +incorporarInvitado(long, long, long, long) boolean
        +quitarMiembro(UsuarioAutenticado, long, long) EquipoVista
        +cambiarLider(UsuarioAutenticado, long, SolicitudLider) EquipoVista
    }

    class ServicioCorreosProyecto {
        <<service>>
        +habilitar(UsuarioAutenticado, long, SolicitudCorreos) ResultadoHabilitacion
        +listar(UsuarioAutenticado, long) List~CorreoHabilitadoVista~
        +estudiantes(UsuarioAutenticado, long) List~EstudianteProyectoVista~
        +monitores(UsuarioAutenticado, long) List~MonitorVista~
        +quitarMonitor(UsuarioAutenticado, long, long) void
        +retirarCorreo(UsuarioAutenticado, long, long) void
        +generarEnlace(UsuarioAutenticado, long, long) EnlaceRegistroVista
    }

    class ApoyoConfiguracion {
        +completarHitosEquipo(long) int
        +vistaEquipo(Equipo) EquipoVista
    }

    MiembroEquipo --> RolEquipo
    ServicioProyectos ..> Proyecto
    ServicioProyectos ..> ApoyoConfiguracion
    ServicioHitos ..> Hito
    ServicioHitos ..> CriterioRubrica
    ServicioHitos ..> ApoyoConfiguracion
    ServicioEquipos ..> Equipo
    ServicioEquipos ..> MiembroEquipo
    ServicioEquipos ..> ApoyoConfiguracion
    ServicioCorreosProyecto ..> MonitorProyecto
```

El paquete `configuracionacademica` concentra la administración académica del docente (RF1, RF6, RF14 y habilitación de correos por proyecto). Al crear hitos o equipos, `ApoyoConfiguracion.completarHitosEquipo` genera automáticamente las filas `hito_equipo` faltantes mediante `INSERT ... ON CONFLICT DO NOTHING`.

**Fuentes:** [`Proyecto.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/dominio/Proyecto.java), [`Hito.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/dominio/Hito.java), [`CriterioRubrica.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/dominio/CriterioRubrica.java), [`Equipo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/dominio/Equipo.java), [`MiembroEquipo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/dominio/MiembroEquipo.java), [`MiembroEquipoId.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/dominio/MiembroEquipoId.java), [`MonitorProyecto.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/dominio/MonitorProyecto.java), [`MonitorProyectoId.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/dominio/MonitorProyectoId.java), [`RolEquipo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/dominio/RolEquipo.java), [`ServicioProyectos.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/servicio/ServicioProyectos.java), [`ServicioHitos.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/servicio/ServicioHitos.java), [`ServicioEquipos.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/servicio/ServicioEquipos.java), [`ServicioCorreosProyecto.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/servicio/ServicioCorreosProyecto.java), [`ApoyoConfiguracion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/servicio/ApoyoConfiguracion.java).

---

## 5.2. Módulo `ejecucion`

Para mantener cada diagrama legible, el módulo `ejecucion` se muestra en dos vistas: primero el núcleo del tablero, tareas y motor CPM/riesgo; luego el seguimiento de avances, evidencias, correcciones, apoyo y reasignación.

### 5.2.1. Tablero, tareas y motor de dependencias

```mermaid
classDiagram
    class EstadoHitoEquipo {
        <<enumeration>>
        EN_CURSO
        ENVIADO
        EVALUADO
    }

    class EstadoTarea {
        <<enumeration>>
        PENDIENTE
        EN_PROGRESO
        COMPLETADA
    }

    class NivelRiesgo {
        <<enumeration>>
        BAJO
        MEDIO
        ALTO
    }

    class HitoEquipo {
        <<entity>>
        -Long idHitoEquipo
        -Long idHito
        -Long idEquipo
        -EstadoHitoEquipo estado
    }

    class Tarea {
        <<entity>>
        -Long idTarea
        -Long idHitoEquipo
        -Long idEncargado
        -Long idTareaOrigen
        -String titulo
        -EstadoTarea estado
        -Boolean bloqueadaPorDependencia
        -Instant fechaBloqueo
        -Boolean edicionHabilitada
        -Integer porcentajeAvance
        -BigDecimal horasEstimadas
        -LocalDate fechaLimite
        -Boolean esPrerrequisitoCritico
        -Boolean enRutaCritica
        -BigDecimal holguraHoras
        -NivelRiesgo nivelRiesgo
    }

    class DependenciaTarea {
        <<entity>>
        -Long idTareaPredecesora
        -Long idTareaDependiente
    }

    class ServicioTareas {
        <<service>>
        +tablero(UsuarioAutenticado, long) TableroHito
        +detalle(UsuarioAutenticado, long) TareaDetalle
        +mias(UsuarioAutenticado) List~MiTareaVista~
        +crear(UsuarioAutenticado, long, SolicitudTarea) TareaVista
        +editar(UsuarioAutenticado, long, SolicitudTarea) TareaVista
    }

    class ServicioDependencias {
        <<service>>
        +agregar(UsuarioAutenticado, long, SolicitudDependencias) ResultadoDependenciasVista
        +quitar(UsuarioAutenticado, long, long) TareaVista
        +rutaCritica(UsuarioAutenticado, long) RutaCriticaVista
    }

    class MotorDependencias {
        +HORAS_TRABAJO_POR_DIA int
        +ordenTopologico(Collection~TareaPlan~) List~Long~
        +rutaCritica(Collection~TareaPlan~) RutaCritica
        +evaluarRiesgo(Collection~TareaPlan~, Instant, Map~Long, Instant~) List~RiesgoTarea~
        +bloqueadas(Collection~TareaPlan~) Set~Long~
        +agregarPrerrequisitos(Collection~TareaPlan~, long, Set~Long~) ResultadoDependencias
    }

    class RecalculoTareas {
        +recalcular(long) void
        +prerrequisitos(List~Tarea~) Map~Long, Set~
    }

    class EdicionHito {
        +abierta(ContextoHito, Instant) boolean
        +exigirAbierta(ContextoHito, Instant) void
    }

    class BloqueoHito {
        +bloquear(JdbcClient, long) void
        +bloquearPorTarea(JdbcClient, long) long
    }

    HitoEquipo --> EstadoHitoEquipo
    Tarea --> EstadoTarea
    Tarea --> NivelRiesgo
    ServicioTareas ..> Tarea
    ServicioTareas ..> RecalculoTareas
    ServicioTareas ..> EdicionHito
    ServicioTareas ..> BloqueoHito
    ServicioDependencias ..> DependenciaTarea
    ServicioDependencias ..> MotorDependencias
    ServicioDependencias ..> RecalculoTareas
    RecalculoTareas ..> MotorDependencias
```

`MotorDependencias` es una clase utilitaria Java pura (sin Spring ni acceso a BD) que calcula orden topológico de Kahn, ruta crítica (RF3), riesgo considerando `HORAS_TRABAJO_POR_DIA = 4` (RF9), bloqueos y detección de ciclos (restricción 4d). `RecalculoTareas` persiste ese resultado en `tarea` dentro de cada transacción de escritura, serializada por `BloqueoHito` sobre `hito_equipo`.

**Fuentes:** [`HitoEquipo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/HitoEquipo.java), [`Tarea.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/Tarea.java), [`DependenciaTarea.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/DependenciaTarea.java), [`DependenciaTareaId.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/DependenciaTareaId.java), [`EstadoHitoEquipo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/EstadoHitoEquipo.java), [`EstadoTarea.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/EstadoTarea.java), [`NivelRiesgo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dependencias/NivelRiesgo.java), [`MotorDependencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dependencias/MotorDependencias.java), [`ServicioTareas.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioTareas.java), [`ServicioDependencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioDependencias.java), [`RecalculoTareas.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/RecalculoTareas.java), [`EdicionHito.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/EdicionHito.java), [`BloqueoHito.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/BloqueoHito.java).

### 5.2.2. Avances, evidencias, correcciones, apoyo y reasignación

```mermaid
classDiagram
    class TipoEvidencia {
        <<enumeration>>
        PDF
        IMAGEN
        ENLACE
    }

    class EstadoSolicitudCorreccion {
        <<enumeration>>
        PENDIENTE
        ATENDIDA
    }

    class TipoReasignacion {
        <<enumeration>>
        TOTAL
        PARCIAL
    }

    class RegistroAvance {
        <<entity>>
        -Long idRegistro
        -Long idTarea
        -Long idUsuario
        -Instant fechaHora
        -Integer porcentajeAvance
        -String observacion
    }

    class Evidencia {
        <<entity>>
        -Long idEvidencia
        -Long idTarea
        -Long idUsuario
        -TipoEvidencia tipoEvidencia
        -String url
        -String nombreArchivo
        -Instant fechaSubida
    }

    class ComentarioTarea {
        <<entity>>
        -Long idComentario
        -Long idTarea
        -Long idAutor
        -String contenido
        -Instant fechaHora
    }

    class SolicitudCorreccion {
        <<entity>>
        -Long idSolicitud
        -Long idTarea
        -Long idEvidencia
        -Long idLider
        -Long idEstudiante
        -String observaciones
        -EstadoSolicitudCorreccion estado
        -Instant fechaSolicitud
        -Instant fechaAtencion
    }

    class ServicioAvance {
        <<service>>
        +actualizar(UsuarioAutenticado, long, SolicitudAvance) TareaVista
    }

    class ServicioEvidencias {
        <<service>>
        +adjuntarArchivo(UsuarioAutenticado, long, MultipartFile) EvidenciaVista
        +adjuntarEnlace(UsuarioAutenticado, long, SolicitudEnlace) EvidenciaVista
        +listar(UsuarioAutenticado, long) List~EvidenciaVista~
        +descargar(UsuarioAutenticado, long) ArchivoEvidencia
    }

    class ServicioComentarios {
        <<service>>
        +listar(UsuarioAutenticado, long) List~ComentarioVista~
        +comentar(UsuarioAutenticado, long, SolicitudComentario) ComentarioVista
    }

    class ServicioCorrecciones {
        <<service>>
        +listar(UsuarioAutenticado, long) List~CorreccionVista~
        +solicitar(UsuarioAutenticado, long, PeticionCorreccion) CorreccionVista
    }

    class ServicioReasignacion {
        <<service>>
        +reasignar(UsuarioAutenticado, long, SolicitudReasignacion) ResultadoReasignacion
    }

    class ServicioApoyo {
        <<service>>
        +solicitar(UsuarioAutenticado, long, SolicitudApoyo) ApoyoVista
        +apoyos(UsuarioAutenticado, long) List~ApoyoVista~
        +carga(UsuarioAutenticado, long) List~CargaIntegrante~
    }

    Evidencia --> TipoEvidencia
    SolicitudCorreccion --> EstadoSolicitudCorreccion
    ServicioAvance ..> RegistroAvance
    ServicioEvidencias ..> Evidencia
    ServicioComentarios ..> ComentarioTarea
    ServicioCorrecciones ..> SolicitudCorreccion
    ServicioReasignacion ..> TipoReasignacion
```

Las operaciones de ejecución se reparten en seis servicios especializados. Destaca que `solicitud_apoyo` no tiene entidad JPA: `ServicioApoyo` inserta y consulta directamente con `JdbcClient`. `ServicioEvidencias` delega el almacenamiento físico en `AlmacenEvidencias` y valida tipos reales por firma binaria mediante `TipoArchivo`.

**Fuentes:** [`RegistroAvance.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/RegistroAvance.java), [`Evidencia.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/Evidencia.java), [`ComentarioTarea.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/ComentarioTarea.java), [`SolicitudCorreccion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/SolicitudCorreccion.java), [`TipoEvidencia.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/TipoEvidencia.java), [`EstadoSolicitudCorreccion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/EstadoSolicitudCorreccion.java), [`TipoReasignacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/TipoReasignacion.java), [`ServicioAvance.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioAvance.java), [`ServicioEvidencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioEvidencias.java), [`ServicioComentarios.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioComentarios.java), [`ServicioCorrecciones.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioCorrecciones.java), [`ServicioReasignacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioReasignacion.java), [`ServicioApoyo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioApoyo.java), [`AlmacenEvidencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/AlmacenEvidencias.java), [`TipoArchivo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/TipoArchivo.java).

---

## 5.3. Módulo `evaluacion`

```mermaid
classDiagram
    class CaracterNota {
        <<enumeration>>
        PROVISIONAL
        DEFINITIVA
    }

    class EnvioHito {
        <<entity>>
        -Long idEnvio
        -Long idHitoEquipo
        -Long idUsuarioEnvia
        -Instant fechaEnvio
    }

    class EvaluacionHito {
        <<entity>>
        -Long idEvaluacion
        -Long idEnvio
        -Long idDocente
        -BigDecimal notaGrupal
        -String retroalimentacionGeneral
        -Boolean esDefinitiva
        -Instant fechaEvaluacion
    }

    class CalificacionCriterio {
        <<entity>>
        -Long idEvaluacion
        -Long idCriterio
        -BigDecimal nota
        -String retroalimentacion
    }

    class NotaIndividual {
        <<entity>>
        -Long idNotaIndividual
        -Long idEvaluacion
        -Long idEstudiante
        -BigDecimal factorAjuste
        -BigDecimal notaSugerida
        -BigDecimal notaFinal
        -Instant fechaConfirmacion
    }

    class ServicioEnvio {
        <<service>>
        +enviar(UsuarioAutenticado, long) EnvioVista
    }

    class ServicioEvaluacion {
        <<service>>
        +evaluar(UsuarioAutenticado, long, SolicitudEvaluacion) EvaluacionVista
        +consultar(UsuarioAutenticado, long) EvaluacionVista
        +cambiarNotaGrupal(UsuarioAutenticado, long, CambioNotaGrupal) EvaluacionVista
        +confirmarNota(UsuarioAutenticado, long, long, ConfirmacionNota) EvaluacionVista
    }

    class LectorEvaluaciones {
        +ultima(long) Optional~Long~
        +hitoEquipoDe(long) Optional~Long~
        +leer(long, long, boolean, Instant) EvaluacionVista
    }

    class CalculadoraIce {
        +ALFA BigDecimal
        +FACTOR_TARDIA BigDecimal
        +NOTA_MAXIMA BigDecimal
        +notaGrupal(Collection~CriterioCalificado~) BigDecimal
        +calcular(BigDecimal, Collection~Long~, Collection~TareaCompletada~) List~NotaSugerida~
        +notaIndividual(BigDecimal, BigDecimal) BigDecimal
    }

    class CaracterEvaluacion {
        +de(boolean, LocalDate, Instant) CaracterNota
    }

    ServicioEnvio ..> EnvioHito
    ServicioEvaluacion ..> EvaluacionHito
    ServicioEvaluacion ..> CalificacionCriterio
    ServicioEvaluacion ..> NotaIndividual
    ServicioEvaluacion ..> LectorEvaluaciones
    ServicioEvaluacion ..> CalculadoraIce
    ServicioEvaluacion ..> CaracterEvaluacion
    LectorEvaluaciones ..> CaracterEvaluacion
    CaracterEvaluacion ..> CaracterNota
```

`evaluacion` separa los motores Java puros (`CalculadoraIce` y `CaracterEvaluacion`) de los servicios transaccionales (`ServicioEnvio` y `ServicioEvaluacion`). El carácter `PROVISIONAL` o `DEFINITIVA` (`CaracterNota`, definido en `eventos-contrato`) se calcula al leer comparando el instante actual con el fin del día de `fecha_limite` en `America/Bogota`.

**Fuentes:** [`EnvioHito.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/dominio/EnvioHito.java), [`EvaluacionHito.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/dominio/EvaluacionHito.java), [`CalificacionCriterio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/dominio/CalificacionCriterio.java), [`CalificacionCriterioId.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/dominio/CalificacionCriterioId.java), [`NotaIndividual.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/dominio/NotaIndividual.java), [`ServicioEnvio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEnvio.java), [`ServicioEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEvaluacion.java), [`LectorEvaluaciones.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/LectorEvaluaciones.java), [`CalculadoraIce.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/calificacion/CalculadoraIce.java), [`CaracterEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/calificacion/CaracterEvaluacion.java), [`CaracterNota.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/datos/CaracterNota.java).

---

## 5.4. Módulo `trazabilidad`

```mermaid
classDiagram
    class AccionAuditoria {
        <<enumeration>>
        LOGIN_EXITOSO
        ACCESO_DENEGADO
        ESTABLECER_DEPENDENCIA
        REASIGNAR_TAREA
        ENVIAR_HITO
        EVALUAR_HITO
        CONFIRMAR_NOTA
        EXPORTAR_REPORTE
        SUPRIMIR_CUENTA
    }

    class LogAuditoria {
        <<entity>>
        -Long idLog
        -Long idUsuarioAccion
        -String entidadAfectada
        -Long idRegistroAfectado
        -AccionAuditoria accion
        -String campoModificado
        -String valorAnterior
        -String valorNuevo
        -Instant fechaHora
    }

    class EntradaAuditoria {
        <<record>>
        +Long idUsuarioAccion
        +String entidadAfectada
        +Long idRegistroAfectado
        +AccionAuditoria accion
        +String campoModificado
        +String valorAnterior
        +String valorNuevo
    }

    class RegistroAuditoria {
        <<interface>>
        +registrar(EntradaAuditoria) void
        +registrarTexto(EntradaAuditoria) void
    }

    class RegistroAuditoriaJdbc {
        +registrar(EntradaAuditoria) void
        +registrarTexto(EntradaAuditoria) void
    }

    class LogAuditoriaRepositorio {
        <<interface>>
        +findById(Long) Optional~LogAuditoria~
        +findByEntidadAfectadaAndIdRegistroAfectadoOrderByFechaHora(String, Long) List~LogAuditoria~
        +findByIdUsuarioAccionOrderByFechaHora(Long) List~LogAuditoria~
    }

    class ServicioPanel {
        <<service>>
        +panel(UsuarioAutenticado, long) PanelProyecto
    }

    class ServicioHistorial {
        <<service>>
        +consultar(UsuarioAutenticado, long, int) List~EntradaHistorial~
    }

    class ServicioReporte {
        <<service>>
        +generar(UsuarioAutenticado, long) byte[]
    }

    class GeneradorReportePdf {
        +generar(ReporteTrazabilidad) byte[]
    }

    LogAuditoria --> AccionAuditoria
    RegistroAuditoria <|.. RegistroAuditoriaJdbc
    RegistroAuditoria ..> EntradaAuditoria
    LogAuditoriaRepositorio ..> LogAuditoria
    ServicioReporte ..> ServicioHistorial
    ServicioReporte ..> GeneradorReportePdf
    ServicioHistorial ..> LogAuditoria
```

`LogAuditoria` está anotada con `@Immutable` y `LogAuditoriaRepositorio` extiende `Repository` (sin `save` ni `delete`), mientras las inserciones pasan por `RegistroAuditoriaJdbc` (separando los textos libres en `texto_auditoria` mediante `registrarTexto`). Nota: `AccionAuditoria` define 28 constantes en el código; el diagrama muestra una selección representativa para mantener la legibilidad.

**Fuentes:** [`LogAuditoria.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/dominio/LogAuditoria.java), [`AccionAuditoria.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/auditoria/AccionAuditoria.java), [`EntradaAuditoria.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/auditoria/EntradaAuditoria.java), [`RegistroAuditoria.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/auditoria/RegistroAuditoria.java), [`RegistroAuditoriaJdbc.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/auditoria/RegistroAuditoriaJdbc.java), [`LogAuditoriaRepositorio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/repositorio/LogAuditoriaRepositorio.java), [`ServicioPanel.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/servicio/ServicioPanel.java), [`ServicioHistorial.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/servicio/ServicioHistorial.java), [`ServicioReporte.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/servicio/ServicioReporte.java), [`GeneradorReportePdf.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/reporte/GeneradorReportePdf.java).

---

## 5.5. Módulo `seguridad`

```mermaid
classDiagram
    class RolPlataforma {
        <<enumeration>>
        ADMINISTRADOR
        DOCENTE
        ESTUDIANTE
        MONITOR
    }

    class Usuario {
        <<entity>>
        -Long idUsuario
        -String nombre
        -String email
        -String passwordHash
        -RolPlataforma rolPlataforma
        -Boolean autorizaTratamientoDatos
        -Instant fechaAutorizacion
        -Instant fechaRegistro
        -Long telegramChatId
        -Instant fechaAnonimizacion
    }

    class CorreoHabilitado {
        <<entity>>
        -Long idCorreoHabilitado
        -Long idProyecto
        -String email
        -RolPlataforma rolPlataforma
        -Long idHabilitadoPor
        -Instant fechaHabilitacion
        -Long idUsuarioRegistrado
        -Instant fechaUso
        -Long idEquipoInvitado
        -byte[] enlaceSelector
        -byte[] enlaceVerificadorHash
        -Instant enlaceVenceEn
    }

    class Permisos {
        <<interface>>
        +esDocenteDelProyecto(long, long) boolean
        +esMonitorDelProyecto(long, long) boolean
        +puedeGestionarTarea(long, long) boolean
        +puedeActualizarAvance(long, long) boolean
        +puedeEnviarHito(long, long) boolean
        +puedeEvaluarHito(long, long) boolean
        +exigir(boolean) void
    }

    class PermisosJdbc {
        +esDocenteDelProyecto(long, long) boolean
        +puedeGestionarTarea(long, long) boolean
        +puedeActualizarAvance(long, long) boolean
    }

    class ServicioCuentas {
        <<service>>
        +autenticar(String, String) Optional~UsuarioAutenticado~
        +registrar(SolicitudRegistro) UsuarioSesion
        +habilitarDocente(long, String) CorreoHabilitadoVista
        +docentesHabilitados() List~CorreoHabilitadoVista~
        +sesion(long) UsuarioSesion
        +crearAdministradorInicial(String, String, String) boolean
    }

    class EnlacesRegistro {
        <<service>>
        +generarParaProyecto(long, long, long) EnlaceRegistroVista
        +generarParaDocente(long, long) EnlaceRegistroVista
    }

    class ServicioContrasena {
        <<service>>
        +solicitarRecuperacion(String) void
        +emitirCodigo(UUID, long) EmisionCodigo
        +restablecer(String, String) boolean
        +cambiar(long, String, String) int
    }

    class ServicioPerfil {
        <<service>>
        +datos(long) DatosPersonales
        +cambiarNombre(long, String) UsuarioSesion
    }

    class ServicioSupresion {
        <<service>>
        +suprimir(long, String) void
    }

    class LimitadorRecuperacion {
        +exigir(Tipo, String) void
        +devolver(Tipo, String) void
        +reiniciar(Tipo, String) void
    }

    Usuario --> RolPlataforma
    CorreoHabilitado --> RolPlataforma
    Permisos <|.. PermisosJdbc
    ServicioCuentas ..> Usuario
    ServicioCuentas ..> CorreoHabilitado
    EnlacesRegistro ..> CorreoHabilitado
    ServicioPerfil ..> Usuario
```

`seguridad` centraliza el control de acceso (`Permisos`/`PermisosJdbc`, con 17 métodos de verificación sobre SQL `EXISTS` que devuelven 403 si el ID no existe), la gestión de cuentas por habilitación y enlace (`ServicioCuentas`, `EnlacesRegistro`), el cambio y recuperación de contraseñas (`ServicioContrasena`, `LimitadorRecuperacion`) y los derechos de acceso, rectificación y supresión de la Ley 1581 (`ServicioPerfil`, `ServicioSupresion`).

**Fuentes:** [`Usuario.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/dominio/Usuario.java), [`CorreoHabilitado.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/dominio/CorreoHabilitado.java), [`RolPlataforma.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/dominio/RolPlataforma.java), [`Permisos.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/permisos/Permisos.java), [`PermisosJdbc.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/permisos/PermisosJdbc.java), [`ServicioCuentas.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/cuentas/ServicioCuentas.java), [`EnlacesRegistro.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/cuentas/EnlacesRegistro.java), [`ServicioContrasena.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/cuentas/ServicioContrasena.java), [`ServicioPerfil.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/cuentas/ServicioPerfil.java), [`ServicioSupresion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/cuentas/ServicioSupresion.java), [`LimitadorRecuperacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/cuentas/LimitadorRecuperacion.java).

---

## 5.6. Módulos `notificaciones` y `vinculaciontelegram`

```mermaid
classDiagram
    class EstadoEnvioNotificacion {
        <<enumeration>>
        EN_COLA
        SIN_TELEGRAM
    }

    class Notificacion {
        <<entity>>
        -Long idNotificacion
        -Long idDestinatario
        -String tipo
        -String mensaje
        -String entidadReferencia
        -Long idReferencia
        -Instant fechaCreacion
        -EstadoEnvioNotificacion estadoEnvio
        -Boolean leida
    }

    class RegistroEventos {
        <<interface>>
        +registrar(DatosEvento) UUID
    }

    class RegistroEventosJdbc {
        +registrar(DatosEvento) UUID
    }

    class PublicadorOutbox {
        +publicarLote() ResultadoLote
        +publicarProgramado() void
    }

    class RegistroNotificaciones {
        <<interface>>
        +registrar(EventoCloud) int
    }

    class RegistroNotificacionesJdbc {
        +registrar(EventoCloud) int
    }

    class ServicioBandeja {
        <<service>>
        +bandeja(long) Bandeja
        +marcarLeida(long, long) void
        +marcarTodasLeidas(long) int
    }

    class RevisionRiesgo {
        +revisar() int
    }

    class RevisionInactividad {
        +revisar() int
    }

    class EvaluadorRiesgoDominio {
        +evaluarTareasRutaCritica() List~EvaluacionRiesgo~
    }

    class DetectorInactividadDominio {
        +buscarInactivos(Instant) List~CasoInactividad~
    }

    class ServicioVinculacion {
        <<service>>
        +generarCodigo(long) CodigoGenerado
        +confirmar(String, long) ResultadoConfirmacion
        +desvincular(long) void
        +desvincularPorChat(long) int
    }

    class ServicioPerfilTelegram {
        <<service>>
        +generarCodigo(long) CodigoGenerado
        +desvincular(long) void
    }

    Notificacion --> EstadoEnvioNotificacion
    RegistroEventos <|.. RegistroEventosJdbc
    RegistroNotificaciones <|.. RegistroNotificacionesJdbc
    RegistroEventosJdbc ..> RegistroNotificaciones
    RegistroNotificacionesJdbc ..> Notificacion
    RevisionRiesgo ..> EvaluadorRiesgoDominio
    RevisionRiesgo ..> RegistroEventos
    RevisionInactividad ..> DetectorInactividadDominio
    RevisionInactividad ..> RegistroEventos
    ServicioPerfilTelegram ..> ServicioVinculacion
```

Ambos módulos completan la capa de notificaciones del núcleo: `RegistroEventosJdbc` persiste de forma atómica en `evento_saliente` y `notificacion`, `PublicadorOutbox` sondea con `FOR UPDATE SKIP LOCKED`, las revisiones horarias evalúan riesgo e inactividad mediante `EvaluadorRiesgoDominio` y `DetectorInactividadDominio`, y `ServicioVinculacion` / `ServicioPerfilTelegram` administran los enlaces profundos con el bot.

**Fuentes:** [`Notificacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/dominio/Notificacion.java), [`EstadoEnvioNotificacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/dominio/EstadoEnvioNotificacion.java), [`RegistroEventos.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/RegistroEventos.java), [`RegistroEventosJdbc.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/RegistroEventosJdbc.java), [`PublicadorOutbox.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/PublicadorOutbox.java), [`RegistroNotificaciones.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/bandeja/RegistroNotificaciones.java), [`RegistroNotificacionesJdbc.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/bandeja/RegistroNotificacionesJdbc.java), [`ServicioBandeja.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/bandeja/ServicioBandeja.java), [`RevisionRiesgo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/RevisionRiesgo.java), [`RevisionInactividad.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/RevisionInactividad.java), [`EvaluadorRiesgoDominio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/EvaluadorRiesgoDominio.java), [`DetectorInactividadDominio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/DetectorInactividadDominio.java), [`ServicioVinculacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/vinculaciontelegram/ServicioVinculacion.java), [`ServicioPerfilTelegram.java`](../../nucleo/src/main/java/co/uniproject/nucleo/vinculaciontelegram/ServicioPerfilTelegram.java).

---

## 5.7. Módulos `eventos-contrato` y `agente-telegram`

```mermaid
classDiagram
    class TipoEvento {
        <<enumeration>>
        TAREA_RUTA_CRITICA_RIESGO
        EQUIPO_INACTIVIDAD
        HITO_ENVIADO
        HITO_EVALUADO
        TAREA_REASIGNADA
        TAREA_CORRECCION
        CUENTA_RECUPERACION_SOLICITADA
        CUENTA_CONTRASENA_CAMBIADA
        +clave() String
        +rutaEsquema() String
    }

    class EventoCloud {
        <<record>>
        +String specversion
        +UUID id
        +String source
        +String type
        +Instant time
        +String datacontenttype
        +DatosEvento data
        +nuevo(DatosEvento, Clock) EventoCloud
    }

    class DatosEvento {
        <<interface>>
    }

    class CodecEventos {
        +mapeador() JsonMapper
        +serializar(EventoCloud) String
        +leer(byte[]) EventoCloud
    }

    class DestinatariosEvento {
        +de(DatosEvento) List~Destino~
    }

    class ConsumidorNotificaciones {
        +recibir(Message, Channel, long) void
    }

    class ReglasDestinatariosPorTipo {
        +destinatarios(EventoCloud) List~Destinatario~
    }

    class RenderizadorMustache {
        +renderizar(EventoCloud, Variante) Aviso
        +renderizarRecuperacion(EventoCloud, String) Aviso
    }

    class EnviadorTelegramOkHttp {
        +enviar(long, Aviso) ResultadoEnvio
    }

    class ReceptorTelegram {
        +consume(Update) void
    }

    class RegistroProcesadosJdbc {
        +yaProcesado(UUID, long) boolean
        +registrar(UUID, long) void
    }

    class ClienteNucleoRest {
        +vincular(String, long) ResultadoVinculacion
        +informarBloqueo(long) void
        +pedirCodigoRecuperacion(UUID, long) CodigoRecuperacion
    }

    EventoCloud --> DatosEvento
    CodecEventos ..> EventoCloud
    CodecEventos ..> TipoEvento
    ConsumidorNotificaciones ..> CodecEventos
    ConsumidorNotificaciones ..> ReglasDestinatariosPorTipo
    ConsumidorNotificaciones ..> RenderizadorMustache
    ConsumidorNotificaciones ..> EnviadorTelegramOkHttp
    ConsumidorNotificaciones ..> RegistroProcesadosJdbc
    ConsumidorNotificaciones ..> ClienteNucleoRest
    ReglasDestinatariosPorTipo ..> DestinatariosEvento
    ReceptorTelegram ..> ClienteNucleoRest
```

`DatosEvento` es una interfaz sellada (`sealed interface`) implementada por los 8 records de evento (`TareaRutaCriticaRiesgo`, `EquipoInactividad`, `HitoEnviado`, `HitoEvaluado`, `TareaReasignada`, `TareaCorreccion`, `RecuperacionSolicitada`, `ContrasenaCambiada`). En `agente-telegram`, el consumidor coordina validación JSON Schema, reglas de destinatarios, idempotencia JDBC, renderizado Mustache, envío por Bot API y llamadas internas al núcleo sin tocar el esquema `public`.

**Fuentes:** [`EventoCloud.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/EventoCloud.java), [`TipoEvento.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/TipoEvento.java), [`DatosEvento.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/datos/DatosEvento.java), [`CodecEventos.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/CodecEventos.java), [`DestinatariosEvento.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/DestinatariosEvento.java), [`ConsumidorNotificaciones.java`](../../agente-telegram/src/main/java/co/uniproject/agente/consumo/ConsumidorNotificaciones.java), [`ReglasDestinatariosPorTipo.java`](../../agente-telegram/src/main/java/co/uniproject/agente/destinatarios/ReglasDestinatariosPorTipo.java), [`RenderizadorMustache.java`](../../agente-telegram/src/main/java/co/uniproject/agente/plantillas/RenderizadorMustache.java), [`EnviadorTelegramOkHttp.java`](../../agente-telegram/src/main/java/co/uniproject/agente/telegram/EnviadorTelegramOkHttp.java), [`ReceptorTelegram.java`](../../agente-telegram/src/main/java/co/uniproject/agente/telegram/ReceptorTelegram.java), [`ClienteNucleoRest.java`](../../agente-telegram/src/main/java/co/uniproject/agente/vinculacion/ClienteNucleoRest.java).

---

## 5.8. Casos de uso del sistema (RF1–RF15)

```mermaid
flowchart LR
    subgraph Actores["Actores"]
        ADM["Administrador"]
        DOC["Docente"]
        LID["Lider de equipo"]
        EST["Estudiante"]
        MON["Monitor"]
        SYS["Programador horario y Bot"]
    end

    subgraph SEG["seguridad"]
        CU01(["CU-01 Habilitar docentes, registro, recuperacion y Ley 1581"])
    end

    subgraph CONF["configuracionacademica"]
        CU02(["CU-02 Crear proyectos, hitos, rubricas y alertas - RF1, RF6"])
        CU03(["CU-03 Habilitar correos, equipos y monitores - RF14"])
    end

    subgraph EJEC["ejecucion"]
        CU04(["CU-04 Crear tareas y dependencias sin ciclos - RF2, RF7"])
        CU05(["CU-05 Calcular y graficar ruta critica - RF3"])
        CU06(["CU-06 Registrar avance y evidencias - RF12, RF13"])
        CU07(["CU-07 Pedir correccion de evidencias - RF5"])
        CU08(["CU-08 Pedir apoyo y reasignar tareas - RF11"])
        CU09(["CU-09 Comentar tareas en curso - RF14"])
    end

    subgraph EVAL["evaluacion"]
        CU10(["CU-10 Enviar hito con validacion critica - RF7"])
        CU11(["CU-11 Evaluar rubrica, ICE y confirmar notas - RF8"])
    end

    subgraph TRAZ["trazabilidad"]
        CU12(["CU-12 Supervisar panel con alertas - RF10"])
        CU13(["CU-13 Consultar historial y exportar PDF - RF15"])
    end

    subgraph NOTIF["notificaciones y Telegram"]
        CU14(["CU-14 Detectar riesgo e inactividad y avisar - RF4, RF6, RF9"])
        CU15(["CU-15 Bandeja web y vinculacion Telegram"])
    end

    LID -. "hereda de" .-> EST
    ADM --> CU01
    DOC --> CU01
    EST --> CU01
    MON --> CU01
    DOC --> CU02
    DOC --> CU03
    LID --> CU04
    LID --> CU07
    LID --> CU08
    EST --> CU05
    DOC --> CU05
    MON --> CU05
    EST --> CU06
    EST --> CU08
    EST --> CU09
    DOC --> CU09
    MON --> CU09
    EST --> CU10
    DOC --> CU11
    EST --> CU11
    DOC --> CU12
    DOC --> CU13
    EST --> CU13
    SYS --> CU14
    EST --> CU15
    DOC --> CU15
    MON --> CU15
    SYS --> CU15
```

El diagrama refleja los cinco actores humanos reales (incluyendo `ADMINISTRADOR`, añadido frente a `entrega1.md` sección 2.2) más el programador horario y el bot de Telegram como actor secundario. El **Líder de equipo** hereda los permisos de **Estudiante** y suma las operaciones de gestión sobre tareas (`puedeGestionarTarea`, `puedeGestionarHitoEquipo`).

### Trazabilidad rápida de requerimientos funcionales (RF1–RF15)

| RF | Implementación principal en el código | Diferencia relevante frente a la entrega 1 |
|---|---|---|
| **RF1** | [`ControladorProyectos.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/web/ControladorProyectos.java), [`ControladorHitos.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/web/ControladorHitos.java) | La suma exacta de `peso_porcentaje = 100` se exige al evaluar el hito (`ServicioEvaluacion`), permitiendo construir criterios gradualmente sin pasar de 100 % (`ServicioHitos`). |
| **RF2** | [`ControladorTareas.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/web/ControladorTareas.java), [`ServicioDependencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioDependencias.java) | Las dependencias solo se permiten entre tareas del mismo `hito_equipo`; el filtro cíclico evalúa candidatos uno a uno. |
| **RF3** | [`MotorDependencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dependencias/MotorDependencias.java), [`RutaCritica.tsx`](../../web/src/paginas/RutaCritica.tsx) | Calcula CPM en tiempo lineal y persiste `en_ruta_critica` y `holgura_horas` en `tarea` tras cada cambio (`RecalculoTareas`). |
| **RF4** | [`RevisionRiesgo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/RevisionRiesgo.java), [`ConsumidorNotificaciones.java`](../../agente-telegram/src/main/java/co/uniproject/agente/consumo/ConsumidorNotificaciones.java) | Avisos por bandeja interna P10 (`notificacion`) y Telegram vía outbox + RabbitMQ (sin correos electrónicos). |
| **RF5** | [`ServicioCorrecciones.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioCorrecciones.java), [`ServicioEvidencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioEvidencias.java) | Al pedir corrección sobre una tarea `COMPLETADA`, vuelve a `EN_PROGRESO` con `99` % y bloquea volver a `100` % hasta adjuntar nueva evidencia. |
| **RF6** | [`DetectorInactividadDominio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/DetectorInactividadDominio.java), [`V7__alerta_inactividad_opcional.sql`](../../nucleo/src/main/resources/db/migration/V7__alerta_inactividad_opcional.sql) | Desde `V7`, la alerta es opcional por hito (`dias_inactividad_alerta = NULL` la desactiva). |
| **RF7** | [`ServicioEnvio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEnvio.java), [`ControladorEnvioEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/web/ControladorEnvioEvaluacion.java) | Cualquier miembro del equipo puede enviar el hito (no solo el líder); devuelve `409 PRERREQUISITOS_INCOMPLETOS` con los títulos pendientes. |
| **RF8** | [`ServicioEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEvaluacion.java), [`CalculadoraIce.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/calificacion/CalculadoraIce.java) | El paso a `DEFINITIVA` al vencer se evalúa dinámicamente al leer (`CaracterEvaluacion`) sin job programado; permite ajustar `nota_grupal` y confirmar `nota_final`. |
| **RF9** | [`MotorDependencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dependencias/MotorDependencias.java), [`EvaluadorRiesgoDominio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/EvaluadorRiesgoDominio.java) | Convierte días de calendario a tiempo de trabajo útil (`HORAS_TRABAJO_POR_DIA = 4`). |
| **RF10** | [`ServicioPanel.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/servicio/ServicioPanel.java), [`PanelDocente.tsx`](../../web/src/paginas/PanelDocente.tsx) | Reutiliza `DetectorInactividad` para inactivos y consulta tareas con `bloqueada_por_dependencia` mayor a 48 horas. |
| **RF11** | [`ServicioReasignacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioReasignacion.java) | Soporta reasignación `TOTAL` y `PARCIAL` (creando subtarea con `id_tarea_origen` y dependencias heredadas) con auditoría inmutable y evento `tarea.reasignada`. |
| **RF12** | [`ServicioAvance.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioAvance.java), [`ControladorEvidencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/web/ControladorEvidencias.java) | Valida PDF, PNG y JPEG por bytes mágicos (`TipoArchivo`, hasta 10 MB) o URL `http://`/`https://`. |
| **RF13** | [`PermisosJdbc.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/permisos/PermisosJdbc.java) | Unifica permisos en consultas SQL `EXISTS`; los IDs inexistentes responden `403` auditado en lugar de `404`. |
| **RF14** | [`ServicioComentarios.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioComentarios.java), [`ServicioCorreosProyecto.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/servicio/ServicioCorreosProyecto.java) | Los monitores se habilitan por proyecto cuando `usa_monitores = true`; pueden comentar en hitos abiertos sin alterar avance, estado ni ver notas individuales o historial. |
| **RF15** | [`ServicioReporte.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/servicio/ServicioReporte.java), [`GeneradorReportePdf.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/reporte/GeneradorReportePdf.java) | Genera el PDF en memoria con Apache PDFBox 3.0.8 sin almacenarlo en disco y audita `EXPORTAR_REPORTE`. |
