# 02. Patrones de diseño y principios SOLID — UniProject

Este documento describe únicamente los patrones arquitectónicos y de diseño que están implementados en el código fuente de **UniProject**, indicando qué problema resuelven en el proyecto, cómo se aplicaron, en qué archivos se encuentran y un fragmento real de código (máximo 12 líneas por fragmento). Al final se documenta la aplicación de los cinco principios **SOLID**.

---

## 2.1. Monolito modular con agente externo y contrato compartido

- **Problema que resuelve aquí:** mantener las operaciones académicas (configuración, ejecución, evaluación, trazabilidad, seguridad y outbox) dentro de una misma transacción ACID sin la sobrecarga operativa de múltiples microservicios, mientras que el envío de avisos a Telegram corre en un proceso separado cuya caída o bloqueo no afecta la disponibilidad del núcleo (RNF3).
- **Cómo se aplicó:** el proyecto Maven raíz define tres módulos (`eventos-contrato`, `nucleo`, `agente-telegram`). `nucleo` agrupa los módulos de dominio en paquetes de primer nivel (`configuracionacademica`, `ejecucion`, `evaluacion`, `trazabilidad`, `seguridad`, `notificaciones`, `vinculaciontelegram`, `comun`, `demo`), `agente-telegram` corre como aplicación Spring Boot sin servidor web (`web-application-type: none`) y ambos dependen de `eventos-contrato`, que no depende de Spring.
- **Archivos donde se ve:** [`pom.xml`](../../pom.xml), [`eventos-contrato/pom.xml`](../../eventos-contrato/pom.xml), [`nucleo/src/main/java/co/uniproject/nucleo/NucleoAplicacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/NucleoAplicacion.java), [`agente-telegram/src/main/java/co/uniproject/agente/AgenteAplicacion.java`](../../agente-telegram/src/main/java/co/uniproject/agente/AgenteAplicacion.java).

```xml
    <modules>
        <module>eventos-contrato</module>
        <module>nucleo</module>
        <module>agente-telegram</module>
    </modules>
```

---

## 2.2. Arquitectura en capas

- **Problema que resuelve aquí:** separar la recepción HTTP y serialización JSON de las reglas de negocio, los algoritmos de dominio (CPM e ICE) y las consultas o escrituras en PostgreSQL, cumpliendo la división planteada en `entrega1.md` sección 2.5.
- **Cómo se aplicó:** cada módulo del núcleo separa sus clases en subpaquetes `web/` (controladores `@RestController`), `servicio/` (lógica transaccional y coordinación), subpaquetes de lógica pura sin Spring (`ejecucion/dependencias/`, `evaluacion/calificacion/`, `trazabilidad/reporte/`), `repositorio/` (acceso a datos) y `dominio/` (entidades y enumeraciones).
- **Archivos donde se ve:** [`nucleo/src/main/java/co/uniproject/nucleo/ejecucion/web/ControladorTareas.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/web/ControladorTareas.java), [`nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioTareas.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioTareas.java), [`nucleo/src/main/java/co/uniproject/nucleo/ejecucion/repositorio/TareaRepositorio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/repositorio/TareaRepositorio.java), [`nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/Tarea.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/Tarea.java).

```java
@RestController
@RequestMapping("/api/tareas")
public class ControladorTareas {

    private final ServicioTareas tareas;
    private final ServicioDependencias dependencias;
    private final ServicioAvance avance;

    @PutMapping("/{idTarea}/avance")
    public TareaVista actualizarAvance(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                       @PathVariable("idTarea") long idTarea,
                                       @Valid @RequestBody SolicitudAvance solicitud) {
        return avance.actualizar(usuario, idTarea, solicitud);
    }
}
```

---

## 2.3. Repository (con variante de solo lectura para entidades inmutables)

- **Problema que resuelve aquí:** encapsular las operaciones de persistencia de las entidades del dominio sin repetir código CRUD y garantizar, a nivel de tipos en Java, que tablas de solo inserción como `log_auditoria` no expongan métodos `save(...)` ni `delete(...)` en su repositorio JPA.
- **Cómo se aplicó:** las entidades editables tienen interfaces que extienden `JpaRepository<T, ID>`, mientras que [`LogAuditoriaRepositorio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/repositorio/LogAuditoriaRepositorio.java) extiende la interfaz base `Repository<LogAuditoria, Long>` exponiendo únicamente métodos de consulta `findBy...` sobre una entidad marcada con `@Immutable`.
- **Archivos donde se ve:** [`nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/repositorio/ProyectoRepositorio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/configuracionacademica/repositorio/ProyectoRepositorio.java), [`nucleo/src/main/java/co/uniproject/nucleo/ejecucion/repositorio/TareaRepositorio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/repositorio/TareaRepositorio.java), [`nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/repositorio/LogAuditoriaRepositorio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/repositorio/LogAuditoriaRepositorio.java).

```java
public interface LogAuditoriaRepositorio extends Repository<LogAuditoria, Long> {

    Optional<LogAuditoria> findById(Long id);

    List<LogAuditoria> findByEntidadAfectadaAndIdRegistroAfectadoOrderByFechaHora(
            String entidadAfectada, Long idRegistroAfectado);

    List<LogAuditoria> findByIdUsuarioAccionOrderByFechaHora(Long idUsuarioAccion);
}
```

---

## 2.4. Transactional Outbox con sondeo y confirmaciones del publicador

- **Problema que resuelve aquí:** evitar la pérdida de eventos o la publicación de eventos fantasma cuando un cambio de negocio se guarda en PostgreSQL pero RabbitMQ está caído o la transacción se revierte, sin bloquear las peticiones HTTP de los usuarios (RNF2 y RNF3).
- **Cómo se aplicó:** [`RegistroEventosJdbc.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/RegistroEventosJdbc.java) exige una transacción activa e inserta el evento serializado en `evento_saliente` (y en `notificacion`) usando la misma conexión JDBC de la transacción de negocio. En segundo plano, [`PublicadorOutbox.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/PublicadorOutbox.java) sondea cada 2 segundos las filas pendientes con `FOR UPDATE SKIP LOCKED`, las envía a RabbitMQ con `CorrelationData` y solo marca `publicado_en = now()` tras recibir confirmación (`ack`) del broker.
- **Archivos donde se ve:** [`nucleo/src/main/java/co/uniproject/nucleo/notificaciones/RegistroEventos.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/RegistroEventos.java), [`nucleo/src/main/java/co/uniproject/nucleo/notificaciones/RegistroEventosJdbc.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/RegistroEventosJdbc.java), [`nucleo/src/main/java/co/uniproject/nucleo/notificaciones/PublicadorOutbox.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/PublicadorOutbox.java).

```java
    @Override
    public UUID registrar(DatosEvento datos) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("RegistroEventos.registrar exige una transacción activa");
        }
        EventoCloud evento = EventoCloud.nuevo(datos, reloj);
        String json = codec.serializar(evento);
        jdbc.sql("INSERT INTO evento_saliente (id, tipo, carga) VALUES (:id, :tipo, CAST(:carga AS jsonb))")
                .param("id", evento.id()).param("tipo", evento.type()).param("carga", json).update();
        bandeja.registrar(evento);
        return evento.id();
    }
```

---

## 2.5. Publicación/suscripción con contrato (CloudEvents 1.0 + JSON Schema)

- **Problema que resuelve aquí:** desacoplar al núcleo de los consumidores de eventos asegurando al mismo tiempo que ningún cambio rompa en silencio el formato de los mensajes entre aplicaciones independientes.
- **Cómo se aplicó:** cada evento viaja en formato estructurado **CloudEvents 1.0** ([`EventoCloud.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/EventoCloud.java)) hacia el exchange topic `uniproject.eventos` con claves jerárquicas definidas en [`TipoEvento.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/TipoEvento.java) y documentadas en [`docs/asyncapi/asyncapi.yaml`](../../docs/asyncapi/asyncapi.yaml). La clase [`CodecEventos.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/CodecEventos.java) carga los 8 esquemas JSON Schema (draft-07) y valida tanto al serializar en el núcleo como al deserializar en el agente.
- **Archivos donde se ve:** [`eventos-contrato/src/main/java/co/uniproject/eventos/EventoCloud.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/EventoCloud.java), [`eventos-contrato/src/main/java/co/uniproject/eventos/TipoEvento.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/TipoEvento.java), [`eventos-contrato/src/main/java/co/uniproject/eventos/CodecEventos.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/CodecEventos.java), [`eventos-contrato/src/main/resources/esquemas/tarea.reasignada.schema.json`](../../eventos-contrato/src/main/resources/esquemas/tarea.reasignada.schema.json), [`docs/asyncapi/asyncapi.yaml`](../../docs/asyncapi/asyncapi.yaml).

```java
    public String serializar(EventoCloud evento) {
        TipoEvento tipo = TipoEvento.desdeClave(evento.type())
                .orElseThrow(() -> new EventoInvalidoException("type desconocido: " + evento.type()));
        String json = mapeador.writeValueAsString(evento);
        validar(tipo, mapeador.readTree(json));
        return json;
    }

    private void validar(TipoEvento tipo, JsonNode nodo) {
        List<com.networknt.schema.Error> errores = esquemas.get(tipo).validate(nodo);
        if (!errores.isEmpty()) {
            throw new EventoInvalidoException("El evento " + tipo.clave() + " no cumple su esquema: " + errores);
        }
    }
```

---

## 2.6. Consumidor idempotente (por evento y destinatario)

- **Problema que resuelve aquí:** la outbox transaccional y los reintentos diferidos de RabbitMQ garantizan entrega *al menos una vez* (*at-least-once*). Además, un evento con varios destinatarios (como `hito.evaluado` o `tarea.ruta_critica.riesgo`) puede fallar temporalmente en el tercer destinatario tras haber enviado el aviso a los dos primeros.
- **Cómo se aplicó:** en lugar de registrar solo el `evento_id`, la tabla `agente.evento_procesado` tiene clave primaria compuesta `(evento_id, usuario_id)` ([`agente-telegram/src/main/resources/db/migration/V1__evento_procesado.sql`](../../agente-telegram/src/main/resources/db/migration/V1__evento_procesado.sql)). Antes de enviar a cada destinatario, [`ConsumidorNotificaciones.java`](../../agente-telegram/src/main/java/co/uniproject/agente/consumo/ConsumidorNotificaciones.java) consulta `registro.yaProcesado(...)` y, tras enviar o descartar permanentemente ese chat, ejecuta `INSERT ... ON CONFLICT DO NOTHING` mediante [`RegistroProcesadosJdbc.java`](../../agente-telegram/src/main/java/co/uniproject/agente/idempotencia/RegistroProcesadosJdbc.java).
- **Archivos donde se ve:** [`agente-telegram/src/main/java/co/uniproject/agente/idempotencia/RegistroProcesados.java`](../../agente-telegram/src/main/java/co/uniproject/agente/idempotencia/RegistroProcesados.java), [`agente-telegram/src/main/java/co/uniproject/agente/idempotencia/RegistroProcesadosJdbc.java`](../../agente-telegram/src/main/java/co/uniproject/agente/idempotencia/RegistroProcesadosJdbc.java), [`agente-telegram/src/main/java/co/uniproject/agente/consumo/ConsumidorNotificaciones.java`](../../agente-telegram/src/main/java/co/uniproject/agente/consumo/ConsumidorNotificaciones.java).

```java
    @Override
    public boolean yaProcesado(UUID eventoId, long usuarioId) {
        return Boolean.TRUE.equals(
                jdbc.sql("SELECT EXISTS (SELECT 1 FROM evento_procesado WHERE evento_id = :eventoId AND usuario_id = :usuarioId)")
                        .param("eventoId", eventoId).param("usuarioId", usuarioId).query(Boolean.class).single());
    }

    @Override
    public void registrar(UUID eventoId, long usuarioId) {
        jdbc.sql("INSERT INTO evento_procesado (evento_id, usuario_id) VALUES (:eventoId, :usuarioId) ON CONFLICT DO NOTHING")
                .param("eventoId", eventoId).param("usuarioId", usuarioId).update();
    }
```

---

## 2.7. Motor de plantillas con variantes y escapado a medida

- **Problema que resuelve aquí:** separar la redacción HTML de los avisos de Telegram de la lógica de consumo de eventos, soportando eventos donde distintos destinatarios reciben textos y botones diferentes para el mismo hecho (como quien deja y quien recibe una tarea en `tarea.reasignada`) y escapando únicamente las cuatro entidades admitidas por el parser HTML de Telegram.
- **Cómo se aplicó:** [`RenderizadorMustache.java`](../../agente-telegram/src/main/java/co/uniproject/agente/plantillas/RenderizadorMustache.java) implementa [`RenderizadorAvisos.java`](../../agente-telegram/src/main/java/co/uniproject/agente/plantillas/RenderizadorAvisos.java), precompila los 9 archivos `.mustache` con un `Escapers.simple` para `&`, `<`, `>`, `"`, selecciona la plantilla combinando `evento.type() + variante.sufijo()` ([`Variante.java`](../../agente-telegram/src/main/java/co/uniproject/agente/destinatarios/Variante.java)) y enriquece el modelo según el tipo sellado `DatosEvento`.
- **Archivos donde se ve:** [`agente-telegram/src/main/java/co/uniproject/agente/plantillas/RenderizadorMustache.java`](../../agente-telegram/src/main/java/co/uniproject/agente/plantillas/RenderizadorMustache.java), [`agente-telegram/src/main/java/co/uniproject/agente/destinatarios/Variante.java`](../../agente-telegram/src/main/java/co/uniproject/agente/destinatarios/Variante.java), [`agente-telegram/src/main/resources/plantillas/tarea.reasignada-nuevo.mustache`](../../agente-telegram/src/main/resources/plantillas/tarea.reasignada-nuevo.mustache), [`agente-telegram/src/main/resources/plantillas/hito.evaluado.mustache`](../../agente-telegram/src/main/resources/plantillas/hito.evaluado.mustache).

```java
    public RenderizadorMustache(CodecEventos codec) {
        this.codec = codec;
        Mustache.Compiler compilador = Mustache.compiler()
                .withEscaper(Escapers.simple(new String[][] {
                        {"&", "&amp;"}, {"<", "&lt;"}, {">", "&gt;"}, {"\"", "&quot;"}}))
                .nullValue("");
```

---

## 2.8. DTO inmutable (`record`) y jerarquía algebraica sellada (`sealed interface`)

- **Problema que resuelve aquí:** transportar datos entre capas, hacia la API REST y a través de RabbitMQ sin mutabilidad accidental, sin código repetitivo ni dependencia de Lombok, permitiendo que el compilador de Java 25 verifique la exhaustividad de todos los tipos de evento en expresiones `switch`.
- **Cómo se aplicó:** todas las solicitudes HTTP, vistas de respuesta, errores (`RespuestaError`), entradas de auditoría (`EntradaAuditoria`) y cargas de eventos son `record` de Java. La interfaz [`DatosEvento.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/datos/DatosEvento.java) es una `sealed interface` que enumera con `permits` los 8 records de eventos, lo que permite pattern matching exhaustivo sin rama `default` en [`TipoEvento.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/TipoEvento.java), [`DestinatariosEvento.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/DestinatariosEvento.java) y [`MensajesBandeja.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/bandeja/MensajesBandeja.java). además, records que envuelven secretos como [`CodigoRecuperacionEmitido.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/interno/CodigoRecuperacionEmitido.java) sobrescriben `toString()` para enmascarar el valor.
- **Archivos donde se ve:** [`eventos-contrato/src/main/java/co/uniproject/eventos/datos/DatosEvento.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/datos/DatosEvento.java), [`eventos-contrato/src/main/java/co/uniproject/eventos/TipoEvento.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/TipoEvento.java), [`eventos-contrato/src/main/java/co/uniproject/eventos/DestinatariosEvento.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/DestinatariosEvento.java), [`nucleo/src/main/java/co/uniproject/nucleo/comun/web/RespuestaError.java`](../../nucleo/src/main/java/co/uniproject/nucleo/comun/web/RespuestaError.java).

```java
public sealed interface DatosEvento
        permits TareaRutaCriticaRiesgo, EquipoInactividad, HitoEnviado, HitoEvaluado,
                TareaReasignada, TareaCorreccion, RecuperacionSolicitada, ContrasenaCambiada {
}
```

---

## 2.9. Máquina de estados de tareas e hitos

- **Problema que resuelve aquí:** gobernar las transiciones válidas del ciclo de vida de una tarea (`PENDIENTE` ↔ `EN_PROGRESO` ↔ `COMPLETADA`) y del hito de un equipo (`EN_CURSO` → `ENVIADO` → `EVALUADO` con edición condicional antes de la fecha límite), evitando estados incoherentes como tareas al 100 % que sigan pendientes o hitos editados mientras están en revisión del docente (RF5, RF7, RF8).
- **Cómo se aplicó:**
  - En tareas, [`ServicioAvance.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioAvance.java) deriva determinísticamente el [`EstadoTarea.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/EstadoTarea.java) del porcentaje (`0` → `PENDIENTE`, `1..99` → `EN_PROGRESO`, `100` → `COMPLETADA`), bloquea el avance si hay prerrequisitos incompletos o una corrección pendiente, y [`ServicioCorrecciones.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioCorrecciones.java) devuelve una tarea `COMPLETADA` a `EN_PROGRESO` (99 %) cuando el líder pide rehacer una entrega (RF5). La base de datos refuerza el invariante con `CONSTRAINT tarea_completada_ck CHECK ((estado = 'COMPLETADA') = (porcentaje_avance = 100) AND (estado = 'COMPLETADA') = (fecha_completada IS NOT NULL))`.
  - En hitos, [`EdicionHito.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/EdicionHito.java) centraliza en un solo método cuándo está abierta la edición según [`EstadoHitoEquipo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dominio/EstadoHitoEquipo.java) y la fecha límite en Bogotá, y [`CaracterEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/calificacion/CaracterEvaluacion.java) determina si la nota es `PROVISIONAL` o `DEFINITIVA`.
- **Archivos donde se ve:** [`nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/EdicionHito.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/EdicionHito.java), [`nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioAvance.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioAvance.java), [`nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioCorrecciones.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioCorrecciones.java), [`nucleo/src/main/java/co/uniproject/nucleo/evaluacion/calificacion/CaracterEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/calificacion/CaracterEvaluacion.java).

```java
    /** EN_CURSO: sí. ENVIADO: no (congelado para revisión). EVALUADO: solo antes de que venza (nota provisional). */
    public static boolean abierta(ContextoHito hito, Instant ahora) {
        return switch (hito.estado()) {
            case EN_CURSO -> true;
            case ENVIADO -> false;
            case EVALUADO -> ahora.isBefore(FechasDominio.venceEn(hito.fechaLimite()));
        };
    }
```

---

## 2.10. Auditoría inmutable por trigger y anexo de texto libre

- **Problema que resuelve aquí:** cumplir simultáneamente la restricción 4e de la entrega 1 («el registro de auditoría no se puede editar ni borrar», ni siquiera por el propio rol dueño de la tabla en PostgreSQL) y el derecho de supresión de la Ley 1581 de 2012 sobre textos libres escritos por un titular que elimina su cuenta.
- **Cómo se aplicó:** en [`nucleo/src/main/resources/db/migration/V3__modelo_dominio.sql`](../../nucleo/src/main/resources/db/migration/V3__modelo_dominio.sql) se creó la función PL/pgSQL `log_auditoria_solo_insercion()` asociada a dos triggers (`BEFORE UPDATE OR DELETE` y `BEFORE TRUNCATE`) sobre `log_auditoria` que lanzan excepción ante cualquier intento de modificación o borrado. En Java, [`RegistroAuditoriaJdbc.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/auditoria/RegistroAuditoriaJdbc.java) guarda en `log_auditoria` únicamente hechos estructurados (IDs, fechas, números y códigos de acción), mientras que los textos libres escritos por personas (`registrarTexto`) se almacenan en la tabla anexa `texto_auditoria`, que sí puede borrarse cuando el autor suprime su cuenta sin alterar `log_auditoria`.
- **Archivos donde se ve:** [`nucleo/src/main/resources/db/migration/V3__modelo_dominio.sql`](../../nucleo/src/main/resources/db/migration/V3__modelo_dominio.sql), [`nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/dominio/LogAuditoria.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/dominio/LogAuditoria.java), [`nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/auditoria/RegistroAuditoria.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/auditoria/RegistroAuditoria.java), [`nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/auditoria/RegistroAuditoriaJdbc.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/auditoria/RegistroAuditoriaJdbc.java).

```sql
CREATE FUNCTION log_auditoria_solo_insercion() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'log_auditoria es de solo inserción (restricción 4e)'
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER log_auditoria_sin_cambios
    BEFORE UPDATE OR DELETE ON log_auditoria
    FOR EACH ROW EXECUTE FUNCTION log_auditoria_solo_insercion();
```

---

## 2.11. Facade / Servicio de aplicación transaccional

- **Problema que resuelve aquí:** una operación de negocio como evaluar un hito, reasignar una tarea o suprimir una cuenta exige coordinar de manera atómica verificaciones de permisos, bloqueos pesimistas, cálculos algorítmicos, escritura en múltiples tablas, auditoría y publicación en la outbox/bandeja.
- **Cómo se aplicó:** los servicios `@Service` (como [`ServicioEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEvaluacion.java), [`ServicioReasignacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioReasignacion.java) o [`ServicioSupresion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/cuentas/ServicioSupresion.java)) actúan como fachadas transaccionales (`@Transactional`) que ofrecen un método único por caso de uso al controlador web y orquestan los componentes internos (`Permisos`, `BloqueoHito`, `CalculadoraIce` / `RecalculoTareas`, `RegistroAuditoria` y `RegistroEventos`). Incluso el almacenamiento en disco de archivos ([`AlmacenEvidencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/AlmacenEvidencias.java)) se sincroniza con la transacción mediante `TransactionSynchronizationManager` para borrar archivos huérfanos si la transacción hace rollback.
- **Archivos donde se ve:** [`nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEnvio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEnvio.java), [`nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEvaluacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/evaluacion/servicio/ServicioEvaluacion.java), [`nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioReasignacion.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/ServicioReasignacion.java), [`nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/AlmacenEvidencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/AlmacenEvidencias.java).

```java
        jdbc.sql("UPDATE hito_equipo SET estado = 'ENVIADO' WHERE id_hito_equipo = :he")
                .param("he", idHitoEquipo).update();
        jdbc.sql("UPDATE tarea SET edicion_habilitada = false WHERE id_hito_equipo = :he")
                .param("he", idHitoEquipo).update();
        auditoria.registrar(EntradaAuditoria.cambio(id, "hito_equipo", idHitoEquipo, AccionAuditoria.ENVIAR_HITO,
                "estado", hito.estado().name(), "ENVIADO"));
        Map<Long, Persona> personas = consultas.personas(List.of(id, hito.idDocente()));
        String nombre = personas.get(id).nombre();
        eventos.registrar(new HitoEnviado(new Referencia(hito.idProyecto(), hito.nombreProyecto()),
                new Referencia(hito.idEquipo(), hito.nombreEquipo()), new Referencia(idHitoEquipo, hito.nombreHito()),
                ahora, new Referencia(id, nombre), personas.get(hito.idDocente())));
```

---

## 2.12. Inyección de dependencias por constructor y Puertos/Adaptadores

- **Problema que resuelve aquí:** permitir que la lógica de negocio, el programador de alertas y el consumidor del agente se prueben de forma determinista (incluyendo el paso del tiempo con `Clock` fijo y dobles de prueba sin levantar red ni Telegram real) y mantener explícitas las dependencias de cada clase.
- **Cómo se aplicó:** en todo el proyecto (Java y cliente TypeScript [`web/src/api/cliente.ts`](../../web/src/api/cliente.ts)) las dependencias son campos `private final` inyectados exclusivamente por constructor (sin `@Autowired` en campos). Las fronteras de infraestructura se definen mediante interfaces de puerto (`Permisos`, `RegistroAuditoria`, `RegistroEventos`, `RegistroNotificaciones`, `EvaluadorRiesgo`, `DetectorInactividad`, `EstadoAlertas`, `EnviadorTelegram`, `ClienteNucleo`, `RegistroProcesados`, `RenderizadorAvisos`) implementadas por adaptadores concretos (`*Jdbc`, `*Dominio`, `EnviadorTelegramOkHttp`, `ClienteNucleoRest`).
- **Archivos donde se ve:** [`nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/RevisionRiesgo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/RevisionRiesgo.java), [`agente-telegram/src/main/java/co/uniproject/agente/consumo/ConsumidorNotificaciones.java`](../../agente-telegram/src/main/java/co/uniproject/agente/consumo/ConsumidorNotificaciones.java), [`web/src/api/cliente.ts`](../../web/src/api/cliente.ts).

```java
    private final EvaluadorRiesgo evaluador;
    private final EstadoAlertas estado;
    private final RegistroEventos registro;
    private final TransactionTemplate tx;
    private final Clock reloj;

    public RevisionRiesgo(EvaluadorRiesgo evaluador, EstadoAlertas estado,
                          RegistroEventos registro, TransactionTemplate tx, Clock reloj) {
        this.evaluador = evaluador;
        this.estado = estado;
        this.registro = registro;
        this.tx = tx;
        this.reloj = reloj;
    }
```

---

## 2.13. Principios SOLID en el código de UniProject

| Principio | Cómo se cumple en UniProject | Ejemplo real verificable en el código |
|---|---|---|
| **S — Responsabilidad Única (SRP)** | Los algoritmos matemáticos y de grafos están separados de la persistencia y del transporte HTTP. Cada clase tiene una única razón para cambiar: `MotorDependencias` solo calcula orden topológico, CPM, holguras, ciclos y bloqueos en memoria; `RecalculoTareas` solo vuelca ese resultado en las filas de `tarea`; `GeneradorReportePdf` solo maqueta el documento PDF con PDFBox a partir del record `ReporteTrazabilidad` que consulta `ServicioReporte`. | [`nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dependencias/MotorDependencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dependencias/MotorDependencias.java), [`nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/RecalculoTareas.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/servicio/RecalculoTareas.java), [`nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/reporte/GeneradorReportePdf.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/reporte/GeneradorReportePdf.java) |
| **O — Abierto/Cerrado (OCP)** | El pipeline de publicación (`RegistroEventosJdbc` → `evento_saliente` → `PublicadorOutbox` → `uniproject.eventos`) y el motor de validación (`CodecEventos`) están cerrados a modificación cuando se agregan nuevos tipos de evento. Cuando en el bloque 28 se añadieron `cuenta.recuperacion.solicitada` y `cuenta.contrasena.cambiada`, `PublicadorOutbox` y `CodecEventos` no cambiaron su lógica: `CodecEventos` itera sobre `TipoEvento.values()` cargando automáticamente cada `esquemas/<clave>.schema.json`. | [`eventos-contrato/src/main/java/co/uniproject/eventos/CodecEventos.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/CodecEventos.java), [`eventos-contrato/src/main/java/co/uniproject/eventos/TipoEvento.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/TipoEvento.java), [`nucleo/src/main/java/co/uniproject/nucleo/notificaciones/PublicadorOutbox.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/PublicadorOutbox.java) |
| **L — Sustitución de Liskov (LSP)** | Los contratos de las interfaces de frontera especifican explícitamente sus postcondiciones (por ejemplo, «nunca lanza excepción: todo error se traduce al enum de resultado»). Tanto `EnviadorTelegramOkHttp` como `ClienteNucleoRest` atrapan `TelegramApiException` y `RuntimeException` y devuelven siempre un valor válido de `ResultadoEnvio` o `ResultadoVinculacion` / `CodigoRecuperacion`, por lo que `ConsumidorNotificaciones` y `ReceptorTelegram` pueden sustituirlos por cualquier otra implementación sin alterar el flujo de confirmaciones AMQP. | [`agente-telegram/src/main/java/co/uniproject/agente/telegram/EnviadorTelegram.java`](../../agente-telegram/src/main/java/co/uniproject/agente/telegram/EnviadorTelegram.java), [`agente-telegram/src/main/java/co/uniproject/agente/telegram/EnviadorTelegramOkHttp.java`](../../agente-telegram/src/main/java/co/uniproject/agente/telegram/EnviadorTelegramOkHttp.java), [`agente-telegram/src/main/java/co/uniproject/agente/vinculacion/ClienteNucleo.java`](../../agente-telegram/src/main/java/co/uniproject/agente/vinculacion/ClienteNucleo.java), [`agente-telegram/src/main/java/co/uniproject/agente/vinculacion/ClienteNucleoRest.java`](../../agente-telegram/src/main/java/co/uniproject/agente/vinculacion/ClienteNucleoRest.java) |
| **I — Segregación de Interfaces (ISP)** | Las interfaces se dividen por cliente en lugar de crear contratos monolíticos: `LogAuditoriaRepositorio` extiende `Repository` (solo lectura) en vez de `JpaRepository` para no exponer operaciones de borrado o actualización; el programador separa `EvaluadorRiesgo`, `DetectorInactividad` y `EstadoAlertas` (de modo que `ServicioPanel` en `trazabilidad` solo depende de `DetectorInactividad`); y en los eventos se distingue `Persona(id, nombre, chatId)` (quien recibe avisos) de `Referencia(id, nombre)` (quien solo es mencionado, sin exponer su `chatId`). | [`nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/repositorio/LogAuditoriaRepositorio.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/repositorio/LogAuditoriaRepositorio.java), [`nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/DetectorInactividad.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/DetectorInactividad.java), [`nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/servicio/ServicioPanel.java`](../../nucleo/src/main/java/co/uniproject/nucleo/trazabilidad/servicio/ServicioPanel.java), [`eventos-contrato/src/main/java/co/uniproject/eventos/datos/Persona.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/datos/Persona.java), [`eventos-contrato/src/main/java/co/uniproject/eventos/datos/Referencia.java`](../../eventos-contrato/src/main/java/co/uniproject/eventos/datos/Referencia.java) |
| **D — Inversión de Dependencias (DIP)** | Los servicios de alto nivel y orquestadores (`ServicioProyectos`, `ServicioEvaluacion`, `RevisionRiesgo`, `RevisionInactividad`, `ConsumidorNotificaciones`) dependen de abstracciones (`Permisos`, `RegistroAuditoria`, `RegistroEventos`, `EvaluadorRiesgo`, `DetectorInactividad`, `EnviadorTelegram`, `RenderizadorAvisos`, `RegistroProcesados`, `ClienteNucleo`) y nunca de las clases concretas de JDBC, OkHttp o JMustache. | [`nucleo/src/main/java/co/uniproject/nucleo/seguridad/permisos/Permisos.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/permisos/Permisos.java), [`nucleo/src/main/java/co/uniproject/nucleo/notificaciones/RegistroEventos.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/RegistroEventos.java), [`nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/RevisionInactividad.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/programador/RevisionInactividad.java), [`agente-telegram/src/main/java/co/uniproject/agente/consumo/ConsumidorNotificaciones.java`](../../agente-telegram/src/main/java/co/uniproject/agente/consumo/ConsumidorNotificaciones.java) |
