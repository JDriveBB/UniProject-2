# 06. Comportamiento y flujos dinámicos — UniProject

Este documento rehace los diagramas de estados, secuencia, actividades y casos de uso exigidos en `entrega1.md` sección 2.7 **a partir del comportamiento real verificado en el código fuente**, señalando expresamente las diferencias frente a lo planeado inicialmente.

---

## 6.1. Diagramas de estado

### 6.1.1. Estado de la tarea (`EstadoTarea` y bloqueo por dependencias)

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE : Lider crea tarea con 0 %
    PENDIENTE --> EN_PROGRESO : Encargado o lider avanza 1 a 99 %
    PENDIENTE --> COMPLETADA : Encargado o lider avanza a 100 % sin correccion pendiente
    EN_PROGRESO --> COMPLETADA : Encargado o lider avanza a 100 % sin correccion pendiente
    EN_PROGRESO --> PENDIENTE : Encargado o lider fija avance en 0 %
    COMPLETADA --> EN_PROGRESO : Lider pide correccion (baja a 99 %) o se reduce avance a 1..99 %
    COMPLETADA --> PENDIENTE : Encargado o lider reduce avance a 0 %
```

El enum `EstadoTarea` se deriva de forma determinista de `porcentaje_avance` en [`ServicioAvance.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioAvance.java) (`0` → `PENDIENTE`, `1..99` → `EN_PROGRESO`, `100` → `COMPLETADA`) y se refuerza en PostgreSQL con `tarea_completada_ck`. La solicitud de corrección (`ServicioCorrecciones`) devuelve una tarea `COMPLETADA` a `EN_PROGRESO` con `99` % e impide volver a `100` % (`409 CORRECCION_PENDIENTE`) hasta adjuntar una nueva evidencia. Todas las transiciones anteriores exigen además `EdicionHito.abierta(...) == true` (no estar congelado en `ENVIADO` ni vencido en `EVALUADO`) y no tener prerrequisitos sin completar.

```mermaid
stateDiagram-v2
    [*] --> Desbloqueada : Sin prerrequisitos incompletos
    Desbloqueada --> Bloqueada : RecalculoTareas detecta predecesora no COMPLETADA
    Bloqueada --> Desbloqueada : Todas las predecesoras alcanzan COMPLETADA o se quita la dependencia
```

El bloqueo por dependencias es una bandera ortogonal (`bloqueada_por_dependencia` + `fecha_bloqueo`), recalculada por `MotorDependencias.bloqueadas` en `RecalculoTareas`. Mientras esté bloqueada, `ServicioAvance` rechaza cualquier actualización de avance con `409 TAREA_BLOQUEADA` y los detalles de las predecesoras pendientes.

**Fuentes:** [`EstadoTarea.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/EstadoTarea.java), [`ServicioTareas.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioTareas.java), [`ServicioAvance.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioAvance.java), [`ServicioCorrecciones.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioCorrecciones.java), [`ServicioEvidencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioEvidencias.java), [`RecalculoTareas.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/RecalculoTareas.java), [`EdicionHito.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/EdicionHito.java).

### 6.1.2. Estado del hito del equipo (`EstadoHitoEquipo`)

```mermaid
stateDiagram-v2
    [*] --> EN_CURSO : Se crea hito o equipo (edicion abierta)
    EN_CURSO --> ENVIADO : Integrante envia hito con RF7 cumplido (congela tareas)
    ENVIADO --> EVALUADO : Docente califica rubrica completa (es_definitiva segun fecha limite)
    EVALUADO --> ENVIADO : Integrante reenvia antes de vencer fecha limite (nota provisional)
```

`ENVIADO` congela todas las tareas del `hito_equipo` (`tarea.edicion_habilitada = false` y `EdicionHito.abierta == false`) hasta que el docente evalúa. Si evalúa antes del fin del día de `fecha_limite` en `America/Bogota`, `es_definitiva` queda en `false`, se reactiva `edicion_habilitada = true` y el equipo puede seguir editando y reenviar el hito (`EVALUADO` → `ENVIADO`). Cuando vence `fecha_limite`, el carácter `DEFINITIVA` y el cierre de edición se calculan dinámicamente al leer (`CaracterEvaluacion.de` y `EdicionHito.abierta`) sin necesidad de una transición escrita en BD.

**Fuentes:** [`EstadoHitoEquipo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/EstadoHitoEquipo.java), [`EdicionHito.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/EdicionHito.java), [`ServicioEnvio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEnvio.java), [`ServicioEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEvaluacion.java), [`CaracterEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/calificacion/CaracterEvaluacion.java), [`FechasDominio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/comun/FechasDominio.java).

---

## 6.2. Diagramas de secuencia

### 6.2.1. Establecer dependencias entre tareas

```mermaid
sequenceDiagram
    actor Lider as Lider de equipo
    participant SPA as SPA (ModalDependencias)
    participant Ctrl as ControladorTareas
    participant Srv as ServicioDependencias
    participant Perm as PermisosJdbc, BloqueoHito y EdicionHito
    participant Repo as Repositorios y JdbcClient
    participant Motor as MotorDependencias
    participant Aud as RegistroAuditoriaJdbc
    participant Rec as RecalculoTareas

    Lider->>SPA: Marca prerrequisitos y pulsa Aceptar
    SPA->>Ctrl: POST /api/tareas/{idTarea}/dependencias
    Ctrl->>Srv: agregar(usuario, idTarea, solicitud)
    Srv->>Perm: puedeGestionarTarea(), bloquearPorTarea() y exigirAbierta()
    Perm-->>Srv: OK (candado FOR UPDATE sobre hito_equipo)
    Srv->>Repo: findByIdHitoEquipo() y prerrequisitos()
    Repo-->>Srv: Tareas del mismo hito y dependencias
    Srv->>Motor: agregarPrerrequisitos(planes, idTarea, candidatos)
    Motor-->>Srv: ResultadoDependencias(aceptados, ciclicos)
    loop Por cada id en aceptados
        Srv->>Repo: INSERT INTO dependencia_tarea ON CONFLICT DO NOTHING
        Srv->>Aud: registrar(ESTABLECER_DEPENDENCIA)
    end
    Srv->>Rec: recalcular(idHitoEquipo)
    Rec->>Motor: rutaCritica(), evaluarRiesgo(), bloqueadas()
    Rec->>Repo: Actualiza holgura, riesgo, ruta critica y bloqueo (flush)
    Srv-->>Ctrl: ResultadoDependenciasVista(aceptados, ciclicos, tarea)
    Ctrl-->>SPA: 200 OK JSON
    alt ciclicos no esta vacio
        SPA-->>Lider: Muestra aviso y marca excluidas por ciclo
    else sin ciclos
        SPA-->>Lider: Confirma guardado y ruta critica recalculada
    end
```

Si en `ModalDependencias` el líder desmarcó dependencias previas, la SPA invoca antes `DELETE /api/tareas/{idTarea}/dependencias/{idPrerrequisito}` (`ControladorTareas.quitarDependencia` → `ServicioDependencias.quitar`). Si al crear una tarea desde `ModalNuevaTarea` el líder elige «Bloqueada por» (RNF5 en 4 clics), el frontend llama primero `POST /api/hitos-equipo/{idHitoEquipo}/tareas` y luego `POST /api/tareas/{idTarea}/dependencias`.

> **Diferencia frente a lo planeado en la entrega 1:** `ServicioDependencias.agregar` **no** inserta eventos en la outbox (`evento_saliente`). `RecalculoTareas` deja actualizados `en_ruta_critica`, `holgura_horas`, `nivel_riesgo` y `bloqueada_por_dependencia` en `tarea`; el aviso automático `tarea.ruta_critica.riesgo` lo emite después el programador horario (`RevisionRiesgo` + `EvaluadorRiesgoDominio`) si una tarea crítica entra en riesgo `ALTO`.

**Fuentes:** [`ModalDependencias.tsx`](../../web/src/tarea/ModalDependencias.tsx), [`dependencias.ts`](../../web/src/tarea/dependencias.ts), [`ControladorTareas.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/web/ControladorTareas.java), [`ServicioDependencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioDependencias.java), [`MotorDependencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dependencias/MotorDependencias.java), [`RecalculoTareas.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/RecalculoTareas.java), [`RegistroAuditoriaJdbc.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/auditoria/RegistroAuditoriaJdbc.java).

### 6.2.2. Envío de hito para revisión académica

```mermaid
sequenceDiagram
    actor Est as Estudiante (miembro)
    participant SPA as SPA (Tablero / envio.ts)
    participant Ctrl as ControladorEnvioEvaluacion
    participant Srv as ServicioEnvio
    participant BD as PermisosJdbc y JdbcClient
    participant Aud as RegistroAuditoriaJdbc
    participant Out as RegistroEventosJdbc
    participant Pub as PublicadorOutbox
    participant MQ as RabbitMQ
    participant Ag as ConsumidorNotificaciones
    participant TG as API de Telegram

    Est->>SPA: Confirma Enviar a revision
    SPA->>Ctrl: POST /api/hitos-equipo/{idHitoEquipo}/envio
    Ctrl->>Srv: enviar(usuario, idHitoEquipo)
    Srv->>BD: puedeEnviarHito(), hito_equipo FOR UPDATE y EdicionHito.abierta()
    Srv->>BD: Consulta tareas con es_prerrequisito_critico y no COMPLETADA
    alt Hay prerrequisitos criticos incompletos (RF7)
        Srv-->>Ctrl: ReglaNegocioException(PRERREQUISITOS_INCOMPLETOS)
        Ctrl-->>SPA: 409 Conflict (RespuestaError con titulos)
        SPA-->>Est: DialogoEnvio P08a lista tareas faltantes
    else Todos los prerrequisitos criticos completos
        Srv->>BD: INSERT envio_hito, hito_equipo=ENVIADO, tarea.edicion_habilitada=false
        Srv->>Aud: registrar(ENVIAR_HITO)
        Srv->>Out: registrar(HitoEnviado) en evento_saliente y notificacion
        Srv-->>Ctrl: EnvioVista (201 Created)
        Ctrl-->>SPA: 201 Created
        SPA-->>Est: DialogoEnvio P08b confirma envio y bloqueo
        Pub->>BD: Sondeo cada 2 s con FOR UPDATE SKIP LOCKED
        Pub->>MQ: Publica CloudEvent (hito.enviado) + confirmacion
        MQ->>Ag: Entrega mensaje de notificaciones.telegram
        Ag->>Ag: Valida JSON Schema, comprueba agente.evento_procesado y renderiza Mustache
        Ag->>TG: sendMessage HTML al chat del docente
        TG-->>Ag: 200 OK
        Ag->>MQ: basicAck
    end
```

A diferencia de la entrega 1 (donde `SistemaNotificaciones` aparecía como un actor síncrono y se contemplaba correo electrónico), cualquier integrante del equipo puede disparar el envío, la entrega queda auditada y el aviso al docente queda persistido en la bandeja interna (`notificacion`) y en `evento_saliente` dentro de la misma transacción, despachándose de forma asíncrona por RabbitMQ y `agente-telegram`.

**Fuentes:** [`Tablero.tsx`](../../web/src/paginas/Tablero.tsx), [`envio.ts`](../../web/src/tarea/envio.ts), [`ControladorEnvioEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/web/ControladorEnvioEvaluacion.java), [`ServicioEnvio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEnvio.java), [`RegistroEventosJdbc.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/RegistroEventosJdbc.java), [`PublicadorOutbox.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/PublicadorOutbox.java), [`ConsumidorNotificaciones.java`](../../agente-telegram/src/main/java/co/uniproject/agente/consumo/ConsumidorNotificaciones.java), [`EnviadorTelegramOkHttp.java`](../../agente-telegram/src/main/java/co/uniproject/agente/telegram/EnviadorTelegramOkHttp.java).

### 6.2.3. Evaluar hito

```mermaid
sequenceDiagram
    actor Doc as Docente
    participant SPA as SPA (EvaluarHito)
    participant Ctrl as ControladorEnvioEvaluacion
    participant Srv as ServicioEvaluacion
    participant Ice as CalculadoraIce y CaracterEvaluacion
    participant BD as PermisosJdbc y JdbcClient
    participant Aud as RegistroAuditoriaJdbc
    participant Out as RegistroEventosJdbc
    participant Ag as Outbox, RabbitMQ y Agente
    actor Est as Integrantes del equipo

    Doc->>SPA: Califica criterios, nota grupal opcional y confirma
    SPA->>Ctrl: POST /api/hitos-equipo/{idHitoEquipo}/evaluacion
    Ctrl->>Srv: evaluar(usuario, idHitoEquipo, solicitud)
    Srv->>BD: puedeEvaluarHito(), hito_equipo FOR UPDATE, valida ENVIADO y rubrica 100 %
    Srv->>Ice: notaGrupal(criterios)
    Ice-->>Srv: notaCalculada
    Srv->>Ice: calcular(grupal, miembros, tareasCompletadas)
    Ice-->>Srv: ICE y notaSugerida por estudiante
    Srv->>BD: Inserta evaluacion_hito, calificacion_criterio, nota_individual
    Srv->>BD: hito_equipo=EVALUADO y edicion_habilitada=!esDefinitiva
    Srv->>Aud: registrar(EVALUAR_HITO) y si difiere nota_grupal
    Srv->>Out: registrar(HitoEvaluado con CaracterNota sin notas numericas)
    Srv-->>Ctrl: EvaluacionVista (201 Created)
    Ctrl-->>SPA: 201 Created con rubrica, ICE y sugeridas
    Ag--)Est: Notificacion asincrona en bandeja y Telegram

    opt Ajuste posterior o confirmacion de notas individuales
        Doc->>Ctrl: PUT /api/evaluaciones/{idEvaluacion}/nota-grupal
        Ctrl->>Srv: cambiarNotaGrupal() recalcula nota_sugerida si no hay confirmadas
        Doc->>Ctrl: PUT /api/evaluaciones/{idEvaluacion}/notas/{idEstudiante}
        Ctrl->>Srv: confirmarNota() fija nota_final y audita CONFIRMAR_NOTA
    end
```

Si la evaluación se registra antes de vencer la fecha límite en `America/Bogota`, `es_definitiva` es `false` y el sistema vuelve a habilitar la edición de tareas (`edicion_habilitada = true`). Si la fecha límite ya venció, `es_definitiva` queda en `true` y las tareas permanecen cerradas. En `HitoEvaluado` nunca viajan notas numéricas hacia RabbitMQ ni Telegram, únicamente `PROVISIONAL` o `DEFINITIVA` (Ley 1581).

**Fuentes:** [`EvaluarHito.tsx`](../../web/src/paginas/EvaluarHito.tsx), [`FormularioEvaluacion.tsx`](../../web/src/evaluacion/FormularioEvaluacion.tsx), [`ControladorEnvioEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/web/ControladorEnvioEvaluacion.java), [`ServicioEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEvaluacion.java), [`CalculadoraIce.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/calificacion/CalculadoraIce.java), [`CaracterEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/calificacion/CaracterEvaluacion.java), [`TarjetaNotaGrupal.tsx`](../../web/src/evaluacion/TarjetaNotaGrupal.tsx), [`TablaNotasIndividuales.tsx`](../../web/src/evaluacion/TablaNotasIndividuales.tsx).

---

## 6.3. Diagramas de actividades

### 6.3.1. Flujo de entrega y evaluación

```mermaid
flowchart TD
    I(["Inicio: hito_equipo en EN_CURSO"]) --> T["Estudiantes avanzan tareas y adjuntan evidencias"]
    T --> D1{"¿Lider solicita correccion RF5?"}
    D1 -- "Si" --> COR["Tarea pasa a EN_PROGRESO 99 % y avisa tarea.correccion"]
    COR --> T
    D1 -- "No" --> ENV["Integrante solicita POST /envio"]
    ENV --> D2{"¿Prerrequisitos criticos en COMPLETADA RF7?"}
    D2 -- "No" --> ERR["409 PRERREQUISITOS_INCOMPLETOS con titulos"]
    ERR --> T
    D2 -- "Si" --> CONG["Congela tareas y pasa hito a ENVIADO; avisa hito.enviado"]
    CONG --> EVAL["Docente califica rubrica POST /evaluacion; CalculadoraIce sugiere notas"]
    EVAL --> D3{"¿Ahora antes de venceEn fecha_limite?"}
    D3 -- "Si" --> PROV["Nota PROVISIONAL y edicion_habilitada=true; permite reenviar"]
    PROV --> T
    D3 -- "No" --> DEF["Nota DEFINITIVA y edicion_habilitada=false"]
    DEF --> CONF["Docente ajusta grupal si aplica y confirma nota_final por estudiante"]
    CONF --> F(["Fin: evaluacion definitiva y trazabilidad cerrada"])
```

Este diagrama refleja las tres reglas reales que gobiernan el flujo: (1) una solicitud de corrección (`ServicioCorrecciones`) bloquea completar al 100 % hasta subir nueva evidencia, (2) mientras el hito esté `EVALUADO` antes del vencimiento, `EdicionHito.abierta` sigue siendo `true` y el equipo puede editar y volver a enviar (`ServicioEnvio`), y (3) al vencer el día en `America/Bogota`, `CaracterEvaluacion` pasa la nota a `DEFINITIVA` de forma automática en lectura.

**Fuentes:** [`ServicioAvance.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioAvance.java), [`ServicioCorrecciones.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioCorrecciones.java), [`ServicioEnvio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEnvio.java), [`ServicioEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEvaluacion.java), [`EdicionHito.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/EdicionHito.java), [`CaracterEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/calificacion/CaracterEvaluacion.java).

### 6.3.2. Resolución de cuellos de botella

```mermaid
flowchart TD
    INI(["Monitoreo continuo y deteccion"]) --> CRON["Programador horario cada hora America/Bogota"]
    INI --> PANEL["Docente consulta panel RF10"]
    INI --> APOYO["Encargado pide apoyo POST /apoyo"]

    CRON --> R1{"¿Tarea critica en riesgo ALTO RF9?"}
    R1 -- "Si y antes false o sin fila" --> EVR["Actualiza estado_riesgo_tarea y emite tarea.ruta_critica.riesgo"]
    CRON --> R2{"¿Inactivo segun hito y no avisado RF6?"}
    R2 -- "Si" --> EVI["Actualiza alerta_inactividad y emite equipo.inactividad a lider y docente opcional"]

    PANEL --> P1["Marca equipo en rojo si hay inactivos o bloqueo mayor a 48 h"]
    APOYO --> SA["Guarda solicitud_apoyo y audita SOLICITAR_APOYO"]

    EVR --> REV["Lider revisa carga e historial de apoyos"]
    EVI --> REV
    P1 --> REV
    SA --> REV

    REV --> D{"¿Como reasigna el lider RF11?"}
    D -- "TOTAL" --> RT["Cambia id_encargado y traslada correccion pendiente"]
    D -- "PARCIAL" --> RP["Resta horas, crea tarea hija con id_tarea_origen y copia dependencias"]
    RT --> REC["Audita REASIGNAR_TAREA, texto opcional, recalcula CPM y emite tarea.reasignada"]
    RP --> REC
    REC --> FIN(["Notificacion en bandeja y Telegram + tablero actualizado"])
```

A diferencia de la entrega 1, el flujo real distingue con precisión qué dispara notificaciones y qué queda solo en plataforma: `RevisionRiesgo` y `RevisionInactividad` emiten eventos a la outbox solo al cambiar de estado (evitando alertas repetidas cada hora); `ServicioApoyo` guarda la solicitud en `solicitud_apoyo` y `log_auditoria` sin enviar evento a RabbitMQ; y `ServicioReasignacion` ofrece división parcial con herencia automática de dependencias predecesoras y sucesoras antes de recalcular el grafo.

**Fuentes:** [`RevisionRiesgo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/RevisionRiesgo.java), [`EvaluadorRiesgoDominio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/EvaluadorRiesgoDominio.java), [`RevisionInactividad.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/RevisionInactividad.java), [`DetectorInactividadDominio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/DetectorInactividadDominio.java), [`ServicioPanel.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/servicio/ServicioPanel.java), [`ServicioApoyo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioApoyo.java), [`ServicioReasignacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioReasignacion.java).

---

## 6.4. Casos de uso detallados (reescritos según el comportamiento real)

### Caso de uso 1: Establecer dependencias

| Elemento | Especificación real en el código |
|---|---|
| **Actor principal** | Líder de equipo (`miembro_equipo.rol_equipo = 'LIDER'`). |
| **Descripción** | Permite al líder definir qué tareas del mismo `hito_equipo` deben completarse antes de iniciar otra, rechazando ciclos y recalculando la ruta crítica, holguras, riesgo y bloqueos. |
| **Precondición** | Sesión activa como líder del equipo; hito de equipo editable (`EdicionHito.abierta == true`); existen tareas en el mismo `hito_equipo`. |
| **Flujo principal** | 1. El líder abre el detalle de una tarea (`/tareas/{idTarea}`) y pulsa «Establecer dependencias» (`ModalDependencias`), o bien selecciona «Bloqueada por» al crearla (`ModalNuevaTarea`).<br>2. La SPA carga las tareas del hito (`GET /api/hitos-equipo/{idHitoEquipo}/tareas`).<br>3. Si se quitaron dependencias previas, la SPA envía `DELETE /api/tareas/{idTarea}/dependencias/{idPrerrequisito}`.<br>4. Para las nuevas dependencias seleccionadas (máximo 50), la SPA envía `POST /api/tareas/{idTarea}/dependencias` (`ControladorTareas.agregarDependencias` → `ServicioDependencias.agregar`).<br>5. El servicio valida permiso de líder (`PermisosJdbc.puedeGestionarTarea`), toma candado `BloqueoHito.bloquearPorTarea`, verifica que los candidatos pertenezcan al mismo `hito_equipo` y llama a `MotorDependencias.agregarPrerrequisitos`.<br>6. Inserta en `dependencia_tarea` los candidatos aceptados, registra cada alta en `log_auditoria` (`ESTABLECER_DEPENDENCIA`) y ejecuta `RecalculoTareas.recalcular` (actualiza `en_ruta_critica`, `holgura_horas`, `nivel_riesgo`, `bloqueada_por_dependencia` y `fecha_bloqueo`).<br>7. La SPA muestra confirmación (`200 OK` con `ResultadoDependenciasVista`). |
| **Flujos alternos** | - **FA-1 (Dependencia circular):** si uno o más candidatos son la misma tarea o alcanzan a `idTarea` en el grafo (`MotorDependencias.alcanza`), se devuelven en el arreglo `ciclicos` de la respuesta `200 OK`, se guardan los demás en `aceptados` y `ModalDependencias` resalta en rojo las excluidas.<br>- **FA-2 (Prerrequisito de otro hito):** `400 PRERREQUISITO_DE_OTRO_HITO`.<br>- **FA-3 (Hito congelado o vencido):** `409 HITO_NO_EDITABLE`.<br>- **FA-4 (No es líder o tarea inexistente):** `403 ACCESO_DENEGADO` auditado. |
| **Postcondición** | Las relaciones válidas persisten en `dependencia_tarea`; si alguna predecesora no está en `COMPLETADA`, la tarea dependiente queda con `bloqueada_por_dependencia = true` (lo que impide avanzar porcentaje en `ServicioAvance`) y el tablero refleja la ruta crítica recalculada. |
| **Diferencias con `entrega1.md`** | (1) Se incorporó el flujo rápido de 4 clics desde `ModalNuevaTarea` (encadenando `POST /api/hitos-equipo/{idHitoEquipo}/tareas` y `POST /api/tareas/{idTarea}/dependencias`, RNF5); (2) además de recalcular la ruta crítica, recalcula holguras, riesgo y fechas de bloqueo para el panel RF10; (3) no emite eventos a la outbox en el acto. |

**Fuentes:** [`ModalDependencias.tsx`](../../web/src/tarea/ModalDependencias.tsx), [`ModalNuevaTarea.tsx`](../../web/src/tablero/ModalNuevaTarea.tsx), [`ControladorTareas.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/web/ControladorTareas.java), [`ServicioDependencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioDependencias.java), [`MotorDependencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dependencias/MotorDependencias.java).

---

### Caso de uso 2: Enviar hito

| Elemento | Especificación real en el código |
|---|---|
| **Actores** | Estudiante (cualquier miembro del equipo, líder o integrante), Sistema de notificaciones asíncrono (`PublicadorOutbox`, RabbitMQ y `agente-telegram`). |
| **Descripción** | Envía el `hito_equipo` a revisión del docente, congela la edición de sus tareas, audita el envío y notifica al docente en la bandeja web y por Telegram. |
| **Precondición** | Sesión activa de un miembro del equipo (`PermisosJdbc.puedeEnviarHito`); `hito_equipo` en estado `EN_CURSO` o bien en `EVALUADO` antes de vencer la fecha límite (`EdicionHito.abierta == true`). |
| **Flujo principal** | 1. En `/equipos/{idEquipo}/tablero`, el estudiante pulsa «Enviar a revisión» y confirma en `ConfirmarEnvio`.<br>2. La SPA invoca `POST /api/hitos-equipo/{idHitoEquipo}/envio` (`ControladorEnvioEvaluacion.enviar` → `ServicioEnvio.enviar`).<br>3. El servicio verifica permiso de integrante, adquiere `BloqueoHito.bloquear(hito_equipo)` y confirma `EdicionHito.abierta`.<br>4. Consulta las tareas del `hito_equipo` con `es_prerrequisito_critico = true AND estado <> 'COMPLETADA'`.<br>5. Al no haber pendientes, inserta fila en `envio_hito`, actualiza `hito_equipo.estado = 'ENVIADO'` y `tarea.edicion_habilitada = false`, y audita `ENVIAR_HITO`.<br>6. Invoca `RegistroEventosJdbc.registrar(HitoEnviado)` dentro de la misma transacción: guarda el CloudEvent en `evento_saliente` y registra la notificación en `notificacion` para el docente.<br>7. Devuelve `201 Created` (`EnvioVista`); la SPA muestra `DialogoEnvio` (P08b).<br>8. De forma asíncrona, `PublicadorOutbox` publica `hito.enviado` a `uniproject.eventos` y `agente-telegram` entrega el aviso HTML al chat vinculado del docente. |
| **Flujos alternos** | - **FA-1 (Prerrequisitos críticos incompletos — RF7):** en el paso 4, si existen tareas críticas sin completar, lanza `409 PRERREQUISITOS_INCOMPLETOS` con la lista de títulos en `RespuestaError.detalles`; `DialogoEnvio` (P08a) los lista al estudiante y no se modifica el hito.<br>- **FA-2 (Hito ya enviado o cerrado):** en el paso 3, responde `409 HITO_NO_ENVIABLE`.<br>- **FA-3 (No es miembro del equipo):** `403 ACCESO_DENEGADO` auditado. |
| **Postcondición** | En éxito: existe registro en `envio_hito`, `hito_equipo` pasa a `ENVIADO`, las tareas quedan bloqueadas para edición y el docente tiene la alerta en `/notificaciones` (y Telegram si está vinculado). En fallo: no hay envío y el hito conserva su estado previo. |
| **Diferencias con `entrega1.md`** | (1) La SPA incluye confirmación modal previa (`ConfirmarEnvio`); (2) también admite reenvío cuando el hito fue evaluado con nota provisional y sigue antes de `fecha_limite`; (3) el aviso al docente dejó de ser por correo electrónico y funciona mediante bandeja P10 + outbox + RabbitMQ + Telegram. |

**Fuentes:** [`Tablero.tsx`](../../web/src/paginas/Tablero.tsx), [`envio.ts`](../../web/src/tarea/envio.ts), [`ControladorEnvioEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/web/ControladorEnvioEvaluacion.java), [`ServicioEnvio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEnvio.java), [`EdicionHito.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/EdicionHito.java).

---

### Caso de uso 3: Evaluar hito

| Elemento | Especificación real en el código |
|---|---|
| **Actores** | Docente (dueño del proyecto), Sistema de notificaciones asíncrono (`PublicadorOutbox`, RabbitMQ y `agente-telegram`). |
| **Descripción** | Permite al docente revisar las entregas, calificar cada criterio de la rúbrica, fijar u omitir una nota grupal manual, generar las notas individuales sugeridas por ICE y notificar a los integrantes. |
| **Precondición** | Sesión activa del docente dueño del proyecto (`PermisosJdbc.puedeEvaluarHito`); `hito_equipo.estado == 'ENVIADO'`; los pesos de `criterio_rubrica` del hito suman exactamente `100`; el equipo tiene al menos un integrante. |
| **Flujo principal** | 1. El docente entra a `/hitos/{idHitoEquipo}/evaluar` (`EvaluarHito.tsx`), que consulta `GET /api/hitos-equipo/{idHitoEquipo}/tareas`, `GET /api/proyectos/{idProyecto}` y (si hubo evaluación previa) `GET /api/hitos-equipo/{idHitoEquipo}/evaluacion`.<br>2. Revisa las entregas por tarea y diligencia nota y retroalimentación por cada criterio (más nota grupal y retroalimentación general opcionales) en `FormularioEvaluacion`.<br>3. Al confirmar, la SPA envía `POST /api/hitos-equipo/{idHitoEquipo}/evaluacion` (`ControladorEnvioEvaluacion.evaluar` → `ServicioEvaluacion.evaluar`).<br>4. El servicio bloquea `hito_equipo`, valida estado `ENVIADO`, suma de pesos `= 100` y notas dentro de `0..puntaje_maximo`.<br>5. `CalculadoraIce.notaGrupal` calcula la nota base de la rúbrica; toma `solicitud.notaGrupal()` si se indicó o la calculada si faltaba. Calcula el ICE y `nota_sugerida` de cada integrante usando las tareas `COMPLETADA` a tiempo (`f = 1`) o tarde (`f = 0,5`).<br>6. Determina `es_definitiva = !ahora.isBefore(FechasDominio.venceEn(fechaLimite))`, inserta `evaluacion_hito`, `calificacion_criterio` y `nota_individual`, pasa `hito_equipo` a `EVALUADO` y ajusta `tarea.edicion_habilitada = !es_definitiva`.<br>7. Registra auditoría (`EVALUAR_HITO` y `ACTUALIZAR nota_grupal` si difiere de la calculada) y emite `HitoEvaluado` (solo con carácter `PROVISIONAL` o `DEFINITIVA`, sin notas numéricas) en `evento_saliente` + `notificacion`.<br>8. Opcionalmente, el docente ajusta la nota grupal (`PUT /api/evaluaciones/{idEvaluacion}/nota-grupal`) mientras no haya notas confirmadas, y confirma la nota final por integrante (`PUT /api/evaluaciones/{idEvaluacion}/notas/{idEstudiante}`). |
| **Flujos alternos** | - **FA-1 (Fin del plazo):** si `ahora >= FechasDominio.venceEn(fechaLimite)` en `America/Bogota`, `es_definitiva` se guarda como `true`, `tarea.edicion_habilitada` queda en `false` y el aviso sale con carácter `DEFINITIVA`.<br>- **FA-2 (Rúbrica incompleta o fuera de rango):** `409 PESOS_NO_SUMAN_100`, `400 RUBRICA_INCOMPLETA` o `400 NOTA_FUERA_DE_RANGO`.<br>- **FA-3 (Hito no enviado o equipo vacío):** `409 HITO_NO_ENVIADO` o `409 EQUIPO_SIN_INTEGRANTES`.<br>- **FA-4 (Cambio de nota grupal tras confirmar individuales):** `PUT /nota-grupal` responde `409 NOTAS_CONFIRMADAS`. |
| **Postcondición** | La evaluación y el desglose quedan guardados, cada integrante recibe el aviso (`notificacion` y Telegram si vinculó chat) con el carácter de la nota, y solo el docente ve los ICE y notas sugeridas de todo el equipo (cada estudiante ve exclusivamente su propia fila en `GET /api/hitos-equipo/{idHitoEquipo}/evaluacion`). |
| **Diferencias con `entrega1.md`** | (1) La calificación grupal y la confirmación de notas individuales son endpoints independientes (`POST /evaluacion`, `PUT /nota-grupal` y `PUT /notas/{idEstudiante}`); (2) el paso de `PROVISIONAL` a `DEFINITIVA` al vencer `fecha_limite` se calcula al leer (`CaracterEvaluacion`) sin proceso en lote; (3) la notificación externa omite calificaciones numéricas por cumplimiento de la Ley 1581. |

**Fuentes:** [`EvaluarHito.tsx`](../../web/src/paginas/EvaluarHito.tsx), [`ControladorEnvioEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/web/ControladorEnvioEvaluacion.java), [`ServicioEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEvaluacion.java), [`LectorEvaluaciones.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/LectorEvaluaciones.java), [`CalculadoraIce.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/calificacion/CalculadoraIce.java), [`CaracterEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/calificacion/CaracterEvaluacion.java).
