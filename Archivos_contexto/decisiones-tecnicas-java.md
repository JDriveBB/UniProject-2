# Decisiones técnicas del backend en Java — UniProject

> Autor: arquitecto. Fecha: 2026-10-29. Estado: **cerrado**. Parte de D1 (Java), D4 (PostgreSQL), D2 (solo avisos), D5 (Telegram obligatorio), D6 (sin correos), D7 (vinculación por endpoint interno, plan §8.8).
> Este documento es la fuente de las instrucciones para los workers. Si algo de aquí choca con el plan, manda este documento. Las versiones se fijan exactas; no se suben sin pasar por `arquitecto`.

## 1. Tabla de decisiones

| # | Decisión | Elección | Versión fijada | Fuente verificada | Motivo |
|---|---|---|---|---|---|
| 1a | Lenguaje / JDK | Java LTS, distribución Eclipse Temurin | **Java 25** (imagen `25.0.5_8`) | Oracle roadmap: 25 es la LTS vigente; 27 (sep-2026) no es LTS; la próxima LTS es 29 (sep-2027). Docker Hub `eclipse-temurin` | Única LTS soportada por todo lo demás (Spring Boot 4.1 admite 17–27; cliente AMQP 5.x lista 25 como soportado). Temurin es gratuito. |
| 1b | Build | **Maven** con Maven Wrapper (`mvnw`) | **Maven 3.9.16** | maven.apache.org/docs/history (3.9.16: 2026-05-13; 3.10.0 salió 2026-09-27; 4.0 sigue en RC) | Multi-módulo declarativo, es lo que usan la documentación de Spring y los tutoriales; el wrapper evita versiones distintas en los tres computadores. 3.9.16 por madurez frente a 3.10.0 (un mes). Gradle se descarta: otro lenguaje de build (Groovy/Kotlin) por aprender. |
| 2 | Framework | **Spring Boot** (padre `spring-boot-starter-parent`) | **4.1.2** (Spring Framework 7.0.10) | docs.spring.io/spring-boot/system-requirements (4.1.2: Java 17–27, Maven ≥ 3.6.3); endoflife.date: soporte OSS de 4.1 hasta 2027-07-31 | Gestiona todas las versiones de abajo con un solo BOM; soporte gratuito cubre el semestre. 4.0 pierde soporte OSS el 2026-12-31; 4.2 aún no es GA. |
| 3 | Mensajería | **Spring AMQP** (`spring-boot-starter-amqp`) sobre AMQP 0.9.1 | spring-rabbit **4.1.2**, `com.rabbitmq:amqp-client` **5.30.0** (gestionadas por Boot) | Tabla de coordenadas de Boot 4.1.2; rabbitmq.com/client-libraries/java-versions (cliente 5.x soporta JDK 25) | Plan §7.9 puesto 1. Confirmaciones del publicador con `CorrelationData`, ack manual y prefetch por propiedades. Ver §3 para el detalle exacto (incluye la trampa nack/reject). |
| 3b | Broker | RabbitMQ 4.3 | imagen **`rabbitmq:4.3.7-management`** | rabbitmq.com/release-information (4.3: soporte comunitario hasta 2027-01-31); Docker Hub | Fijado por el plan (reintento diferido nativo en quorum queues). La 4.4 (2026-10-20) existe pero no se adopta: no aporta nada que el diseño necesite. |
| 4 | Telegram | **TelegramBots** (rubenlagus), módulos `telegrambots-longpolling` + `telegrambots-client`, **sin** el starter de Spring | **10.3.0** (Bot API 10.3) | Maven Central `org.telegram` (10.3.0 = latest); `pom.xml` del repo | Plan §7.7.1. Trae long polling (`TelegramBotsLongPollingApplication`), cliente OkHttp, `SendMessage` con `parseMode=HTML` y `InlineKeyboardButton.url`. El starter `telegrambots-springboot-longpolling-starter` 10.3.0 está compilado contra **Spring Boot 3.5.5**; con Boot 4.1 se registra el bot a mano con un `@Bean` (ver §4). |
| 5a | Acceso a datos (núcleo) | **Spring Data JPA / Hibernate** para el dominio + **`JdbcClient`** (Spring JDBC) para la outbox | hibernate-core **7.4.13.Final**, Spring Data JPA **4.1.2** | Coordenadas de Boot 4.1.2 | JPA ahorra el CRUD de las tablas de §2.6 y encaja con la capa "Repositorios/DAO" de entrega1 §2.5. La outbox se maneja con SQL explícito porque `FOR UPDATE SKIP LOCKED` debe verse tal cual en el código y en la exposición. Ambos comparten la misma transacción (`JpaTransactionManager`). |
| 5b | Acceso a datos (agente) | Solo **`JdbcClient`** (sin JPA) | Spring Framework 7.0.10 | — | El agente tiene una sola tabla (`Evento_Procesado`). |
| 5c | Base de datos | PostgreSQL | imagen **`postgres:18.6`**; driver `org.postgresql:postgresql` **42.7.14** | Docker Hub `postgres` (19 solo en RC1) | D4. 18 es la última estable. |
| 5d | Migraciones | **Flyway** Community (`spring-boot-starter-flyway` + `org.flywaydb:flyway-database-postgresql`) | **12.4.0** (gestionada por Boot) | Coordenadas de Boot 4.1.2; doc de Boot "data-initialization" (exige el módulo de PostgreSQL); Redgate: PostgreSQL 18 verificado | Migraciones en SQL plano, legibles y comparables con el diagrama de BD. Liquibase 5.0 cambió a licencia FSL-1.1 (no es open source según la OSI) y usa changelogs XML/YAML: se descarta. |
| 6 | Programador interno | **`@Scheduled`** de Spring (`@EnableScheduling`) | Spring Framework 7.0.10 | — | Dos revisiones horarias y un sondeo de 2 s en una sola instancia. Lo que se pierde durante una caída se recupera en la siguiente pasada porque las alertas nacen de *cambios de estado* guardados (plan §8.7). Quartz (2.5.2) agrega 11 tablas y un clúster que no se necesita. |
| 7a | Estructura | **Un repositorio, un proyecto Maven multi-módulo, dos aplicaciones** (`nucleo`, `agente-telegram`) + un módulo de contrato compartido | — | Plan §6.1 y §8.9 | El agente es un proceso aparte (es lo que dibuja la arquitectura y lo que permite mostrar que el núcleo sigue respondiendo si el agente cae). El contrato compartido evita que los dos lados describan el evento distinto. |
| 7b | Imagen base | Construcción: **`eclipse-temurin:25-jdk-noble`**; ejecución: **`eclipse-temurin:25-jre-noble`** | Temurin 25.0.5_8 | Docker Hub `eclipse-temurin` | Ubuntu 24.04 (glibc): menos sorpresas que Alpine con OkHttp/DNS. Etiqueta por versión mayor para recibir parches de seguridad. |
| 8 | Validación de eventos | `com.networknt:json-schema-validator` | **3.0.8** (usa Jackson 3, Java 17+) | Maven Central; README del proyecto | Plan §8.4 paso 1. Boot 4.1 usa Jackson 3 (`tools.jackson` 3.1.7), igual que esta librería. |
| 9 | Plantillas del agente | JMustache vía `spring-boot-starter-mustache` | gestionada por Boot | Lista de starters de Boot 4.1.2 | Plan §7.8: seis plantillas en archivos, editables sin tocar Java. |
| 10 | Pruebas | JUnit (vía `spring-boot-starter-test`) + **Testcontainers** con PostgreSQL y RabbitMQ reales | Testcontainers **2.0.5** | Coordenadas de Boot 4.1.2 | Prueban la outbox, el `SKIP LOCKED` y el reintento diferido contra los mismos 18.6 y 4.3.7 de Compose. |

Todo lo anterior es gratuito (Apache 2.0, MIT, EPL, GPLv2+CE o la licencia de PostgreSQL). Prohibido en el proyecto: Lombok (se usan `record`), Liquibase, Quartz, Spring Modulith (su externalización de eventos reemplazaría la tabla `Evento_Saliente` del diseño), el starter de TelegramBots, versiones `-SNAPSHOT`/RC de terceros.

## 2. Estructura del repositorio

```
uniproject/
├── pom.xml                      ← padre: packaging pom, parent spring-boot-starter-parent 4.1.2,
│                                   groupId co.uniproject, version 0.1.0-SNAPSHOT, <java.version>25</java.version>
├── mvnw, mvnw.cmd, .mvn/wrapper/ ← Maven 3.9.16
├── eventos-contrato/            ← jar sin Spring. Paquete co.uniproject.eventos
│   └── src/main/
│       ├── java/…/TipoEvento.java           enum con las 6 claves: tarea.ruta_critica.riesgo, equipo.inactividad,
│       │                                     hito.enviado, hito.evaluado, tarea.reasignada, tarea.correccion
│       ├── java/…/Topologia.java            constantes: uniproject.eventos, notificaciones.telegram, etc. (plan §8.3)
│       ├── java/…/EventoCloud.java          record sobre CloudEvents 1.0 (specversion, id, source, type, time,
│       │                                     datacontenttype, data)
│       ├── java/…/datos/*.java              un record por evento con los campos de plan §5.1
│       └── resources/esquemas/<clave>.schema.json   JSON Schema draft-07 (compatible con AsyncAPI 3.0)
├── nucleo/                      ← Spring Boot app. Paquete co.uniproject.nucleo
│   └── src/main/java/co/uniproject/nucleo/
│       ├── configuracionacademica/   ┐ módulos de entrega1 §2.6; dentro de cada uno:
│       ├── ejecucion/                │ web/ (controladores) · servicio/ · repositorio/ · dominio/
│       ├── evaluacion/               │ (las capas de entrega1 §2.5)
│       ├── trazabilidad/             ┘
│       ├── seguridad/                Spring Security, BCryptPasswordEncoder(12) (RNF4)
│       ├── notificaciones/           RegistroEventos (inserta en la outbox), PublicadorOutbox, RevisionRiesgo,
│       │                             RevisionInactividad
│       ├── vinculaciontelegram/      códigos de un solo uso + endpoint interno (D7)
│       └── comun/
│   └── src/main/resources/db/migration/V1__…sql   (Flyway, esquema public)
├── agente-telegram/             ← Spring Boot app sin web. Paquete co.uniproject.agente
│   └── src/main/java/co/uniproject/agente/
│       ├── consumo/          @RabbitListener, ack/reject
│       ├── destinatarios/    reglas de plan §8.6
│       ├── plantillas/       render JMustache
│       ├── telegram/         cliente de envío, long polling, /start
│       ├── idempotencia/     Evento_Procesado (JdbcClient)
│       └── vinculacion/      RestClient hacia /interno del núcleo
│   └── src/main/resources/plantillas/<clave>[-variante].mustache
│   └── src/main/resources/db/migration/V1__evento_procesado.sql  (Flyway, esquema agente)
├── infra/
│   ├── docker-compose.yml
│   ├── rabbitmq/rabbitmq.conf, rabbitmq/definitions.json, rabbitmq/inicializar.sh, rabbitmq/verificar.sh
│   │                              (topología del plan §8.3; la importa el contenedor rabbitmq-inicializador, ver §6 V2)
│   └── postgres/init/01-roles-y-esquemas.sh   (.sh y no .sql: necesita leer las contraseñas del entorno)
├── docs/asyncapi/asyncapi.yaml  ← referencia con $ref los esquemas de eventos-contrato
└── .env.example                 ← el .env real va en .gitignore
```

Dependencias por módulo (sin versión: las fija el BOM de Boot salvo las marcadas):

| Módulo | Dependencias |
|---|---|
| `eventos-contrato` | `tools.jackson.core:jackson-databind` (solo anotaciones/records) |
| `nucleo` | `eventos-contrato`, `spring-boot-starter-webmvc`, `-security`, `-validation`, `-data-jpa`, `-jdbc`, `-flyway`, `org.flywaydb:flyway-database-postgresql`, `-amqp`, `org.postgresql:postgresql` (runtime), `-actuator`; test: `spring-boot-starter-test`, `org.testcontainers:testcontainers-postgresql`, `testcontainers-rabbitmq`, `testcontainers-junit-jupiter` |
| `agente-telegram` | `eventos-contrato`, `spring-boot-starter-amqp`, `-jdbc`, `-flyway`, `flyway-database-postgresql`, `-mustache`, `-restclient`, `org.postgresql:postgresql`, `org.telegram:telegrambots-longpolling:10.3.0`, `org.telegram:telegrambots-client:10.3.0`, `com.networknt:json-schema-validator:3.0.8`; test igual que núcleo |

Nombres de tablas: los diagramas usan `Evento_Saliente`; en PostgreSQL se crean **sin comillas y en minúscula** (`evento_saliente`). Regla: nombre del diagrama en minúscula = nombre físico. La entidad Java se llama `EventoSaliente` (Hibernate la mapea solo).

## 3. Mensajería: cómo se implementa exactamente

**Topología:** se declara solo en `infra/rabbitmq/definitions.json` (plan §8.3). El código **no** declara exchanges ni colas (no crear beans `Queue`, `Exchange`, `Binding` ni `Declarables`).

**Publicador de la outbox (núcleo)** — `application.yml`:
```yaml
spring.rabbitmq.publisher-confirm-type: correlated
spring.task.scheduling.pool.size: 3
```
Método `@Scheduled(fixedDelay = 2000)` + `@Transactional`:
1. `SELECT id, tipo, carga::text FROM evento_saliente WHERE publicado_en IS NULL ORDER BY creado_en LIMIT 50 FOR UPDATE SKIP LOCKED` (con `JdbcClient`).
2. Por fila: `rabbitTemplate.send("uniproject.eventos", tipo, mensaje, new CorrelationData(id))`. Propiedades: `contentType=application/cloudevents+json`, `messageId=id`, `type=tipo`, `deliveryMode=PERSISTENT`.
3. Espera `correlationData.getFuture().get(5, SECONDS)`. Si `ack` → `UPDATE … SET publicado_en = now()`. Si `nack`, tiempo agotado o `AmqpException` → `UPDATE … SET intentos = intentos + 1` y **se corta el lote** (RabbitMQ probablemente está caído; se reintenta en 2 s).
4. Índice: `CREATE INDEX … ON evento_saliente (creado_en) WHERE publicado_en IS NULL`.

La inserción en la outbox la hace `RegistroEventos.registrar(...)` con `JdbcClient` **dentro** del método `@Transactional` del servicio de negocio (misma conexión, misma transacción). Columnas: `id uuid`, `tipo text`, `carga jsonb`, `creado_en timestamptz`, `publicado_en timestamptz null`, `intentos int default 0`.

**Consumidor (agente)** — `application.yml`:
```yaml
spring.main.web-application-type: none
spring.rabbitmq.listener.type: simple
spring.rabbitmq.listener.simple.acknowledge-mode: manual
spring.rabbitmq.listener.simple.prefetch: 10
spring.rabbitmq.listener.simple.concurrency: 1
spring.rabbitmq.listener.simple.max-concurrency: 1
spring.rabbitmq.listener.simple.retry.enabled: false
```
Firma: `@RabbitListener(queues = Topologia.COLA_TELEGRAM) void recibir(Message m, Channel canal, @Header(AmqpHeaders.DELIVERY_TAG) long tag)`.

Regla obligatoria: el método **nunca lanza excepción** (atrapa `Exception`) y termina con exactamente una de estas llamadas:

| Resultado (plan §8.4 y §8.5) | Llamada |
|---|---|
| Enviado a todos, o duplicado ya registrado | `canal.basicAck(tag, false)` |
| Error temporal (429, red, 5xx) | `canal.basicReject(tag, true)` → reintento diferido 10 s, 20 s, … hasta 5 min |
| Evento inválido o plantilla rota | `canal.basicReject(tag, false)` → `uniproject.fallidos` |

**Prohibido `basicNack`.** En RabbitMQ 4.3 `basic.nack` no incrementa `delivery-count`, así que la espera no crece y `delivery-limit` nunca enviaría el mensaje a fallidos. Por eso también se usa modo `manual`: en modo `auto`, Spring AMQP rechaza con `basicNack(…, multiple=true, …)` cuando hay varias entregas pendientes (verificado en `BlockingQueueConsumer.rollbackOnExceptionIfNecessary`).

Reintento diferido y cola de fallidos: **no se programan en Java**. Son la política `delayed-retry-type=failed`, `delayed-retry-min=10000`, `delayed-retry-max=300000`, `delivery-limit=20`, `dead-letter-exchange=uniproject.fallidos` del plan §8.3, dentro de `definitions.json`.

Idempotencia: `INSERT INTO agente.evento_procesado (evento_id, usuario_id, enviado_en) VALUES (…) ON CONFLICT DO NOTHING` después de cada envío exitoso; antes de enviar a un destinatario se consulta si la pareja ya existe (plan §8.4, variante por destinatario).

## 4. Telegram: cómo se implementa exactamente

- Envío: `OkHttpTelegramClient(token)`; `SendMessage` con `parseMode = ParseMode.HTML` y `replyMarkup` = `InlineKeyboardMarkup` con un solo `InlineKeyboardButton` de tipo `url` (texto del botón según plan §8.6; URL = `UNIPROJECT_URL_BASE` + ruta).
- Escape: JMustache con escapador propio que solo reemplaza `&`→`&amp;`, `<`→`&lt;`, `>`→`&gt;`, `"`→`&quot;` (las cuatro entidades que admite el HTML de Telegram).
- Errores: `TelegramApiRequestException.getErrorCode()` 429 o ≥ 500 → temporal; 403 → permanente para ese destinatario (y aviso al núcleo, abajo); 400 → permanente para ese destinatario. Cualquier otra `TelegramApiException` (red) → temporal.
- Recepción: bean `TelegramBotsLongPollingApplication` que en el arranque llama `registerBot(token, consumidor)` y se cierra en `@PreDestroy`. El consumidor implementa `LongPollingSingleThreadUpdateConsumer`. Solo se procesan updates de tipo `message` en chat privado.
- `/start <código>` (código: 24 bytes de `SecureRandom` en Base64 URL sin relleno = 32 caracteres, dentro del límite de 64 y del alfabeto `A-Za-z0-9_-` del enlace profundo) → `RestClient` `POST http://nucleo:8080/interno/telegram/vinculaciones` con cuerpo `{"codigo": "...", "chatId": 123}` y cabecera `X-UniProject-Token-Interno`. Respuestas del núcleo: `204` vinculado · `404` código inexistente · `410` código usado o vencido. El bot contesta un texto fijo para cada caso.
- 403 al enviar → `POST /interno/telegram/bloqueos` con `{"chatId": 123}`; el núcleo pone `telegram_chat_id` en nulo (plan §8.5).
- Cualquier otro mensaje → texto fijo: "Este bot solo envía avisos de UniProject" (D2).
- `/interno/**` lo protege Spring Security con un filtro que compara la cabecera contra `UNIPROJECT_TOKEN_INTERNO` (comparación de tiempo constante); no usa la sesión de usuarios.

## 5. Docker Compose (plan §8.9)

| Servicio | Imagen | Nota |
|---|---|---|
| `rabbitmq` | `rabbitmq:4.3.7-management` | Puertos 5672 y 15672; monta `rabbitmq.conf` y `definitions.json` |
| `base-de-datos` | `postgres:18.6` | Se renombra con guion (antes `base_de_datos`): el guion bajo no es válido en nombres de host y `java.net.URI` lo rechaza. Monta `postgres/init/` |
| `nucleo` | Dockerfile multi-etapa: `eclipse-temurin:25-jdk-noble` → `./mvnw -pl nucleo -am package -DskipTests` → `eclipse-temurin:25-jre-noble` | Puerto 8080 |
| `agente` | Igual, con `-pl agente-telegram` | Sin puertos publicados |

Base de datos `uniproject`, dueña el rol `uniproject_nucleo` (tablas en `public`). Esquema `agente` con `AUTHORIZATION uniproject_agente`; ese rol no recibe permisos sobre `public` (mínimo privilegio, Ley 1581). Cada aplicación corre su propio Flyway sobre su esquema (`spring.flyway.schemas`).

Variables de `.env.example`: `POSTGRES_PASSWORD`, `NUCLEO_DB_PASSWORD`, `AGENTE_DB_PASSWORD`, `RABBITMQ_ADMIN_USER`, `RABBITMQ_ADMIN_PASSWORD`, `RABBITMQ_NUCLEO_PASSWORD`, `RABBITMQ_AGENTE_PASSWORD`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`, `UNIPROJECT_TOKEN_INTERNO`, `UNIPROJECT_URL_BASE`, `UNIPROJECT_TELEGRAM_BOTON_ENLACE` (V1), `TZ=America/Bogota`. Servicio adicional: `rabbitmq-inicializador` (`curlimages/curl:8.22.0`, de un solo uso). Detalle completo en `instrucciones-agy/bloque-01-infraestructura.md`.

Programador: `@Scheduled(cron = "0 0 * * * *", zone = "America/Bogota")` para riesgo e inactividad; `fixedDelay = 2000` para la outbox. Una sola instancia de `nucleo`.

## 6. Verificaciones abiertas (no bloquean el arranque del código)

> Resolución por defecto (2026-10-29, bloque 1): **V1** se asume rechazo → `UNIPROJECT_TELEGRAM_BOTON_ENLACE=false` mientras la URL sea localhost; prueba real en el bloque del agente. **V2** verificada: un nodo vacío que importa definiciones al arrancar **no** crea vhost ni usuario por defecto (rabbitmq.com/docs/definitions). Se descarta la importación al arrancar y los usuarios con `password_hash` en el repo: el administrador sale de `rabbitmq.conf` (`default_user = $(RABBITMQ_ADMIN_USER)`) y el contenedor `rabbitmq-inicializador` importa `definitions.json` por la API HTTP y crea `nucleo`/`agente` con las contraseñas del `.env` (alternativa: `RABBITMQ_DEFAULT_USER/PASS`). **V3**: sin fijar versiones; prueba `CompatibilidadTelegramBotsTest`; si falla, `<jackson-2-bom.version>2.17.2</jackson-2-bom.version>` y/o `okhttp:4.12.0` directo en `agente-telegram`.

| # | Qué falta confirmar | Quién | Si falla |
|---|---|---|---|
| V1 | Si Telegram acepta un botón `url` que apunte a `http://localhost:8080/...` (no está documentado; podría responder 400) | `ejecutor` con el bot real | Propiedad `uniproject.telegram.boton-enlace=false`: se envía el aviso sin botón y con la ruta en texto |
| V2 | Si RabbitMQ 4.3 sigue creando el usuario por defecto cuando carga `definitions.json` al arrancar | `investigador` | Declarar los usuarios `nucleo` y `agente` dentro de `definitions.json` (permiso de escritura solo en `uniproject.eventos` para `nucleo`; lectura solo en `notificaciones.telegram` para `agente`) |
| V3 | Que `telegrambots-meta` 10.3.0 (compilado con Jackson 2.17) funcione con el Jackson 2.21.7 que impone Boot | prueba de integración del agente | Fijar `jackson-bom` 2.x a la versión que pida TelegramBots |

## 7. Opciones descartadas (para defender en la exposición)

- **Gradle**: más rápido, pero suma un DSL nuevo; el volumen del proyecto no lo aprovecha.
- **Cliente AMQP nativo / cliente AMQP 1.0 de RabbitMQ**: más control, pero confirmaciones, conversión y contenedor de consumidores se escriben a mano. Spring AMQP ya los trae (plan §7.9, puesto 2).
- **java-telegram-bot-api (pengrad) 10.3.0**: válida y al día; se prefiere TelegramBots por separar long polling, cliente y modelo en módulos y porque es la más usada en Java. Cambiar de una a otra solo toca el paquete `telegram/`.
- **Solo JPA** (con `@Lock` + pista de *lock timeout* `-2` para obtener `SKIP LOCKED`): funciona, pero esconde la consulta que hay que explicar. **Solo JDBC**: obliga a escribir a mano el CRUD de todo el dominio.
- **Quartz**: persistencia de trabajos y clúster que una sola instancia no necesita.
- **Spring Modulith** (registro de publicaciones de eventos): sustituiría la outbox diseñada (`Evento_Saliente`, sondeo con confirmaciones) y rompería la consistencia con los diagramas.
- **Hilos virtuales** (`spring.threads.virtual.enabled`): se dejan apagados por ahora; se evalúan si una prueba de carga del RNF2 lo pide.

## 8. Impactos

- **Plan**: §7.7.1 y §7.9 quedan concretados (TelegramBots 10.3.0, Spring AMQP 4.1.2); §8.1 queda con `FOR UPDATE SKIP LOCKED` confirmado (PostgreSQL 18.6); §8.9 queda con imágenes fijadas y el servicio renombrado a `base-de-datos`; §8.10 el AsyncAPI se escribe a mano (no hay equivalente de FastStream en esta pila).
- **Entrega 1**: sin cambios (D3). El diagrama de despliegue o de componentes, si muestra nombres de contenedores, debe usar `base-de-datos`.
- **Agentes**: `dev-backend` (núcleo, outbox, programador, endpoint interno), `dev-notificaciones` (agente completo), `configurador` (§5 y V2), `contratos` (esquemas draft-07 en `eventos-contrato`, AsyncAPI, plantillas `.mustache`), `ejecutor` (wrapper, V1), `revisor` (cotejar §3: modo `manual`, sin `basicNack`, sin declaraciones de topología en código).

## 9. Decisiones de la planificación por olas (2026-10-29)

Detalle y reparto en `instrucciones-agy/plan-de-olas.md` (bloques 2–10). Resumen de lo que se fijó:

| Tema | Decisión | Motivo |
|---|---|---|
| Campos de `data` | camelCase, igual que los `record` (el ejemplo snake_case de plan §8.2 era ilustrativo) | Cero anotaciones de Jackson; mismo estilo que el endpoint interno (`chatId`) |
| Tiempos | `Instant` (ISO con `Z`) y `LocalDate`; el agente formatea en `America/Bogota` | Ida y vuelta exacta con Jackson 3 sin configurar features |
| Personas en el evento | `Persona(id, nombre, chatId)` solo para quien puede recibir; las mencionadas van como `Referencia(id, nombre)` | Mínimo de datos personales (Ley 1581) |
| URL del botón | El evento no lleva URL; el agente arma `url-base + RutasWeb.*` | Una sola tabla de rutas compartida con el frontend |
| Validación | `CodecEventos` (en `eventos-contrato`, networknt 3.0.8, draft-07) valida al serializar en el núcleo y al leer en el agente; esquemas sin `additionalProperties: false` | Estricto al enviar, tolerante al recibir |
| Transacciones en esta fase | `TransactionTemplate` inyectado, sin `@Transactional`; `RegistroEventos` exige transacción activa | Las pruebas sin Spring cubren la atomicidad; compatible con `@Transactional` del dominio |
| Programador | Puertos `EvaluadorRiesgo`/`DetectorInactividad` (hoy vacíos) + tablas `estado_riesgo_tarea` y `alerta_inactividad` (V2, sin FK hasta el dominio) | Plan §8.7: alertar en el cambio de estado sin depender aún del dominio |
| Vinculación | Vencimiento calculado por PostgreSQL; nuevo código invalida los anteriores; gana el último vínculo de un chat; bloqueos siempre 204 | Un solo reloj; columna `UNIQUE`; idempotencia |
| Seguridad | Dos `SecurityFilterChain`: `/interno/**` (token en tiempo constante, sin sesión) y el resto autenticado con HTTP Basic provisional | Al declarar una cadena propia, la de Boot desaparece; sin la segunda quedaría todo abierto |
| Errores de Telegram | 400 con `can't parse entities` o `BUTTON_URL_INVALID` → el evento va a fallidos; otro 400 → se omite ese destinatario; 401/404 y token vacío → temporal | Un mensaje mal formado no se arregla reintentando; un token mal puesto sí, al corregir el `.env` |
| Paralelismo | Un worktree por paquete en `C:\uniproject-olas\`; archivos disjuntos; pruebas de la ola 1 sin `@SpringBootTest` | Siete compilaciones en la misma carpeta se pisan `target/` y ven código ajeno a medio escribir |
| AsyncAPI | Se valida con `asyncapi/cli:6.2.1` (Docker Hub, 2026-10-12) | Versión fijada, no `latest` |
