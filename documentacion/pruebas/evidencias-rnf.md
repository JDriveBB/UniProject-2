# Evidencias de requisitos no funcionales (RNF1–RNF5) — UniProject

Este documento consolida la evidencia medida y verificada en código para los cinco requisitos no funcionales de la entrega 1 (`entrega1.md` sección 2.4).

- **Entorno de las mediciones (RNF2, RNF3 y RNF5):** ejecutadas el `2026-11-20` en un PC personal (Windows 11, Docker Desktop) contra el stack de pruebas aislado (`infra/pruebas/sesion-pruebas.ps1 -Accion Levantar`, proyecto `uniproject-pruebas`, `http://127.0.0.1:18080`, datos demo).
- **Medición de RNF1:** ejecutada el `2026-11-18` sobre el mismo stack de pruebas (`http://127.0.0.1:18080`, commit base `e0a6b1e`) y documentada en [`informe-auditoria-final.md`](informe-auditoria-final.md) sección 1, respaldada además por pruebas automáticas en backend y frontend.
- **Verificación de RNF4:** comprobada en la configuración de seguridad y en las pruebas de integración con PostgreSQL real (Testcontainers).

---

## Tabla resumen

| RNF | Meta (`entrega1.md` sección 2.4) | Resultado medido / verificado | Cumple | Evidencia principal |
|---|---|---|---|---|
| **RNF1. Rendimiento** | Ruta crítica de hasta 150 tareas en ≤ 2 s en el cliente | Servidor: **`15–30 ms`**; grafo renderizado en cliente: **`140–176 ms`** | **Sí** | [`informe-auditoria-final.md`](informe-auditoria-final.md) sección 1 |
| **RNF2. Concurrencia** | 500 usuarios simultáneos en evaluación con respuesta ≤ 3 s | 500 VUs (277 s), 56 861 `GET` (205 req/s): **`p95 = 4,4 ms`**, **`p99 = 6,2 ms`**, **máx `74,9 ms`**; 12 fallos de conexión local (`0,02 %`, `0 ms`) | **Sí** (entorno local) | [`rnf2-resumen.txt`](evidencias/rnf2-resumen.txt) |
| **RNF3. Disponibilidad** | Uptime mínimo de 99,5 % en semanas pico (8 y 16) | Caída por `SIGTERM` al PID 1 del núcleo: recuperación automática en **`≈ 7 s`** (`7` de `360` sondeos fallidos en 6 min, `1` reinicio Docker; presupuesto 99,5 % = `50,4 min/semana`) + diseño tolerante a fallos | **Sí** (recuperación medida; ver alcance) | [`rnf3-resultado.json`](evidencias/rnf3-resultado.json) |
| **RNF4. Seguridad** | Contraseñas de todos los actores con hash bcrypt y costo ≥ 12 | `new BCryptPasswordEncoder(12)` verificado en código y con prefijo `$2[aby]$12$` en PostgreSQL real | **Sí** | [`ConfiguracionSeguridad.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/ConfiguracionSeguridad.java#L85-L89) |
| **RNF5. Usabilidad** | Crear tarea con dependencia en ≤ 4 clics desde el tablero del equipo | **`4 clics`** en el tablero de Beta como Diego (`POST .../tareas` `201` + `POST .../dependencias` `200`) en **`1,5 s`** | **Sí** | [`rnf5-resultado.json`](evidencias/rnf5-resultado.json) |

---

## RNF1 — Rendimiento

- **Meta:** «el motor de dependencias debe calcular y renderizar la ruta crítica de proyectos con hasta 150 tareas en un tiempo de respuesta máximo de 2 segundos en el cliente».
- **Cómo está implementado:**
  - En el servidor, [`MotorDependencias.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/dependencias/MotorDependencias.java) ejecuta el cálculo CPM en memoria en tiempo lineal $O(V + E)$ (orden topológico de Kahn y pasadas adelante/atrás) y [`ControladorHitosEquipo.java`](../../nucleo/src/main/java/co/uniproject/nucleo/ejecucion/web/ControladorHitosEquipo.java) lo expone en `GET /api/hitos-equipo/{idHitoEquipo}/ruta-critica`.
  - En el cliente, [`disposicion.ts`](../../web/src/tablero/disposicion.ts) (`disponerRed`) ubica los nodos por niveles en $O(V + E)$ y [`RutaCritica.tsx`](../../web/src/paginas/RutaCritica.tsx) renderiza el grafo en SVG nativo sin librerías externas.
- **Método y cómo repetirlo:**
  - **Medición en el stack de pruebas (`2026-11-18`):** registrada en [`informe-auditoria-final.md`](informe-auditoria-final.md) sección 1 al consultar y renderizar en navegador un hito con **150 tareas con dependencias** contra el stack de pruebas (`http://127.0.0.1:18080`, commit `e0a6b1e`; no hay un guion separado en `infra/pruebas/` para RNF1). Para observarlo de nuevo: levantar la sesión de pruebas (`.\infra\pruebas\sesion-pruebas.ps1 -Accion Levantar`), poblar un hito con 150 tareas encadenadas y abrir `/hitos/{idHitoEquipo}/ruta-critica` midiendo en DevTools el tiempo de la petición y del renderizado SVG.
  - **Pruebas automáticas reproducibles en el repositorio:**
    ```powershell
    ./mvnw -pl nucleo test -Dtest=MotorDependenciasTest#cientoCincuentaTareasEnMenosDeDosSegundos
    npm --prefix web test -- src/tablero/disposicion.test.ts
    ```
- **Resultado:**
  - Medición sobre stack real ([`informe-auditoria-final.md`](informe-auditoria-final.md) sección 1): **`15–30 ms`** en servidor y **`140–176 ms`** en renderizado del grafo en el cliente (límite: **`2000 ms`**).
  - Pruebas automáticas: [`MotorDependenciasTest.java`](../../nucleo/src/test/java/co/uniproject/nucleo/ejecucion/dependencias/MotorDependenciasTest.java) afirma la ejecución completa de `rutaCritica` y `evaluarRiesgo` sobre 150 tareas en `< 2 s`; [`disposicion.test.ts`](../../web/src/tablero/disposicion.test.ts) dispone 150 tareas en 15 niveles con 1400 dependencias en `< 100 ms`.
- **Veredicto:** **Cumple RNF1**.
- **Evidencia:** [`informe-auditoria-final.md`](informe-auditoria-final.md) sección 1, [`MotorDependenciasTest.java`](../../nucleo/src/test/java/co/uniproject/nucleo/ejecucion/dependencias/MotorDependenciasTest.java) y [`disposicion.test.ts`](../../web/src/tablero/disposicion.test.ts).

---

## RNF2 — Concurrencia

- **Meta:** «500 usuarios simultáneos interactuando en los módulos de evaluación sin que el tiempo de respuesta del servidor supere 3 segundos».
- **Método y cómo repetirlo:**
  - Guion [`rnf2-evaluacion.js`](../../infra/carga/rnf2-evaluacion.js) ejecutado mediante [`correr_carga.py`](../../infra/carga/correr_carga.py) dentro del contenedor `grafana/k6:2.3.0` contra la sesión de pruebas (`http://host.docker.internal:18080`).
  - En `setup()` inicia sesión una única vez por cada cuenta demo participante (`docente@demo.uniproject.test` y los estudiantes registrados del equipo cuyo hito está `EVALUADO`) para respetar los límites de inicio de sesión. Luego ejecuta el escenario `ramping-vus`: **1 min** de subida hasta **500 VUs**, **3 min** sostenidos con 500 VUs y **30 s** de bajada a 0 VUs.
  - Durante la carga, los VUs envían peticiones `GET` de lectura con pausas aleatorias de `1–3 s`: recorrido docente (25 % de los VUs: `panel_docente`, `entregas_hito`, `proyecto_detalle`, `evaluacion_hito`) y recorrido estudiante (75 % de los VUs: `entregas_hito`, `proyecto_detalle`, `evaluacion_hito`).
  - Cómo repetirlo (documentado en [`infra/carga/README.md`](../../infra/carga/README.md)):
    ```powershell
    .\infra\pruebas\sesion-pruebas.ps1 -Accion Levantar
    python infra/carga/correr_carga.py --salida C:\ruta\resultados-rnf2
    ```
- **Resultado (medición del `2026-11-20`):**
  - Escenario ejecutado: **500 usuarios virtuales** (subida 1 min, 3 min sostenidos, bajada 30 s; **`277 s`** en total).
  - Carga procesada: **`56 861` peticiones `GET`** del módulo de evaluación (**`205` peticiones por segundo**).
  - Latencia global (`http_req_duration{fase:evaluacion}`): **`p95 = 4,4 ms`**, **`p99 = 6,2 ms`**, **máximo `74,9 ms`** (umbral RNF2: `3000 ms`).
  - Por endpoint: el máximo más alto registrado fue **`74,9 ms`** en `GET /api/hitos-equipo/{id}/tareas`.
  - Peticiones fallidas: **`12` de `56 861`** (**`0,02 %`**, umbral `< 1 %`), todas con duración **`0 ms`** — errores de conexión del cliente en la misma máquina, no respuestas lentas ni errores del servidor.
  - Veredicto de k6: **`CUMPLE RNF2`**.
- **Limitaciones:** medido en un PC personal (Windows 11, Docker Desktop), donde el contenedor generador (`k6`) y los contenedores del sistema (`nucleo`, `base-de-datos`, `rabbitmq`) compiten por la misma CPU, memoria y pila de red local. Es una evidencia indicativa de que la arquitectura no presenta bloqueos ni consultas N+1 bajo 500 usuarios virtuales, pero no sustituye una prueba distribuida con generador y servidor en máquinas separadas.
- **Veredicto:** **Cumple RNF2**.
- **Evidencia:** [`evidencias/rnf2-resumen.txt`](evidencias/rnf2-resumen.txt) y [`infra/carga/README.md`](../../infra/carga/README.md).

---

## RNF3 — Disponibilidad

- **Meta:** «uptime mínimo de 99,5 % durante las semanas pico (8 y 16)». Un 99,5 % semanal equivale a un presupuesto máximo de caída de **`50,4 minutos`** por semana (`7 × 24 × 60 × 0,005`).
- **Medidas de diseño verificadas en el código:**
  - **Política de reinicio y sondeos de salud en Docker Compose ([`infra/docker-compose.yml`](../../infra/docker-compose.yml)):** todos los servicios persistentes (`base-de-datos`, `rabbitmq`, `nucleo` y `agente`) tienen **`restart: unless-stopped`**. `base-de-datos` define `healthcheck` con `pg_isready`, `rabbitmq` con `rabbitmq-diagnostics -q check_running && rabbitmq-diagnostics -q check_port_listener 15672`, y el núcleo expone `/actuator/health` (`management.endpoints.web.exposure.include: health` en [`nucleo/src/main/resources/application.yml`](../../nucleo/src/main/resources/application.yml), permitido sin autenticación en [`ConfiguracionSeguridad.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/ConfiguracionSeguridad.java#L57)).
  - **Outbox transaccional con reintentos ante caída de RabbitMQ:** [`RegistroEventosJdbc.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/RegistroEventosJdbc.java) guarda cada evento en `evento_saliente` (y en la bandeja `notificacion`) dentro de la misma transacción del negocio. [`PublicadorOutbox.java`](../../nucleo/src/main/java/co/uniproject/nucleo/notificaciones/PublicadorOutbox.java) sondea cada 2 s con `FOR UPDATE SKIP LOCKED`; si RabbitMQ cae o no confirma (`nack`, `TimeoutException` o `AmqpException`), incrementa `intentos`, corta el lote y reintenta en la siguiente pasada sin perder eventos ni afectar las respuestas HTTP (verificado en [`PublicadorOutboxTest.java`](../../nucleo/src/test/java/co/uniproject/nucleo/notificaciones/PublicadorOutboxTest.java)).
  - **Agente de Telegram en proceso separado:** el servicio `agente` ([`agente-telegram/src/main/resources/application.yml`](../../agente-telegram/src/main/resources/application.yml), `web-application-type: none`) corre en su propio contenedor. Si Telegram o el agente fallan o se reinician, la aplicación web y la bandeja interna siguen operando; la quorum queue `notificaciones.telegram` retiene los mensajes con reintento diferido nativo (`10 s` a `5 min`, `delivery-limit: 20` en [`definitions.json`](../../infra/rabbitmq/definitions.json)) y [`ConsumidorNotificaciones.java`](../../agente-telegram/src/main/java/co/uniproject/agente/consumo/ConsumidorNotificaciones.java) evita duplicados mediante idempotencia por `(evento_id, usuario_id)`.
  - **Migraciones Flyway automáticas al arrancar:** `nucleo` (`spring.flyway.schemas: public`) y `agente-telegram` (`spring.flyway.schemas: agente`) validan y aplican sus migraciones SQL al iniciar antes de quedar listos, manteniendo `spring.jpa.hibernate.ddl-auto: validate` en el núcleo.
- **Método y cómo repetirlo:**
  - El guion [`rnf3_disponibilidad.py`](../../infra/carga/rnf3_disponibilidad.py) sondea `http://127.0.0.1:18080/actuator/health` cada `1 s` durante `6 min` (`--minutos 6`). Al cumplirse `60 s` (`--caida-en 60`) ejecuta `docker exec uniproject-pruebas-nucleo-1 kill -TERM 1` para matar el proceso `java` (PID 1 del contenedor del núcleo) y mide cuánto tarda Docker en reiniciarlo automáticamente y volver a recibir HTTP `200` con `"UP"`.
  - Cómo repetirlo:
    ```powershell
    .\infra\pruebas\sesion-pruebas.ps1 -Accion Levantar
    python infra/carga/rnf3_disponibilidad.py C:\ruta\resultados-rnf3 --minutos 6 --caida-en 60
    ```
- **Resultado (medición del `2026-11-20`):**
  - Sondeo continuo cada `1 s` durante **`6 min`** (**`360` sondeos**).
  - Al provocar la caída (`SIGTERM` al PID 1 a los `60 s`), Docker reinició el contenedor solo (`restart: unless-stopped`, **`1` reinicio** registrado) y `/actuator/health` volvió a responder en **`≈ 7 s`** (**`7` de `360` sondeos fallidos**).
  - En comparación, el presupuesto semanal permitido por el 99,5 % es de **`50,4 min`** de caída.
- **Limitaciones:** esta prueba evalúa la **recuperación automática ante la caída del proceso del núcleo** dentro de una ventana de 6 minutos en Docker Desktop local; **no** es una medición continua durante las semanas 8 y 16 de un semestre real en producción ni cubre fallos de hardware o de conectividad externa prolongados.
- **Veredicto:** **Cumple RNF3** (recuperación automática verificada y tolerancia a fallos comprobada).
- **Evidencia:** [`evidencias/rnf3-resultado.json`](evidencias/rnf3-resultado.json) y [`infra/carga/rnf3_disponibilidad.py`](../../infra/carga/rnf3_disponibilidad.py).

---

## RNF4 — Seguridad

- **Meta:** «contraseñas de todos los actores con hash bcrypt y factor de costo mínimo de 12».
- **Cómo está implementado y verificado:**
  - [`ConfiguracionSeguridad.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/ConfiguracionSeguridad.java#L85-L89) declara el `PasswordEncoder` único de la aplicación como `new BCryptPasswordEncoder(12)`.
  - Todos los flujos de credenciales (alta de cuentas, autenticación con `hashFicticio` de igual costo, creación del administrador inicial, cambio/restablecimiento de contraseña y cuentas demo) pasan por ese codificador.
  - La prueba de integración [`SeguridadIntegracionTest.java`](../../nucleo/src/test/java/co/uniproject/nucleo/seguridad/SeguridadIntegracionTest.java#L330-L350) (`contrasenasConBcrypt12YAuditoriaSinSecretos`) consulta `usuario.password_hash` directamente en PostgreSQL (Testcontainers) y comprueba el patrón `^\$2[aby]\$12\$[./A-Za-z0-9]{53}$` (bcrypt con costo `12`). Además, [`ServicioCuentasRegistroTest.java`](../../nucleo/src/test/java/co/uniproject/nucleo/seguridad/cuentas/ServicioCuentasRegistroTest.java) (`rechazosIgualesYConUnBcrypt`) verifica que cada intento de registro ejecuta exactamente un hash bcrypt.
- **Cómo repetirlo:**
  ```powershell
  ./mvnw -pl nucleo test -Dtest=SeguridadIntegracionTest#contrasenasConBcrypt12YAuditoriaSinSecretos
  ```
- **Resultado:** `BCryptPasswordEncoder(12)` configurado en producción y verificado contra PostgreSQL en la suite de integración (incluida en `python scripts/verificar.py`).
- **Veredicto:** **Cumple RNF4**.
- **Evidencia:** [`ConfiguracionSeguridad.java`](../../nucleo/src/main/java/co/uniproject/nucleo/seguridad/ConfiguracionSeguridad.java#L85-L89), [`SeguridadIntegracionTest.java`](../../nucleo/src/test/java/co/uniproject/nucleo/seguridad/SeguridadIntegracionTest.java#L330-L350) y [`ServicioCuentasRegistroTest.java`](../../nucleo/src/test/java/co/uniproject/nucleo/seguridad/cuentas/ServicioCuentasRegistroTest.java).

---

## RNF5 — Usabilidad

- **Meta:** «el flujo para crear una nueva tarea con dependencia debe requerir un máximo de 4 clics desde el panel principal del equipo».
- **Cómo está implementado:** desde el tablero del equipo (`/equipos/{idEquipo}/tablero`, [`Tablero.tsx`](../../web/src/paginas/Tablero.tsx)), [`ModalNuevaTarea.tsx`](../../web/src/tablero/ModalNuevaTarea.tsx) abre como diálogo modal con el foco puesto automáticamente en el campo «Título» (y `1` hora estimada por defecto en [`nuevaTarea.ts`](../../web/src/tablero/nuevaTarea.ts)), integrando el selector [`SelectorPrerrequisitos.tsx`](../../web/src/tablero/SelectorPrerrequisitos.tsx) para elegir dependencias en el mismo formulario y encadenar la creación y el registro de dependencias al guardar.
- **Método y cómo repetirlo:**
  - Medido el `2026-11-20` con un navegador automatizado (**Puppeteer**) autenticado como el líder **Diego** (`diego@demo.uniproject.test`) sobre el tablero del **Equipo Beta** (`http://127.0.0.1:18080`):
    1. **Clic 1:** pulsar **«Nueva tarea»** (el campo Título recibe el foco automáticamente y se escribe el título).
    2. **Clic 2:** pulsar **«Bloqueada por»** para desplegar la lista de tareas del hito.
    3. **Clic 3:** seleccionar la **primera tarea de la lista** como prerrequisito.
    4. **Clic 4:** pulsar **«Guardar»**.
  - Reproducible manualmente siguiendo el caso `L1` de [`guia-sesion-pruebas.md`](guia-sesion-pruebas.md) sobre `.\infra\pruebas\sesion-pruebas.ps1 -Accion Levantar`, y cubierto a nivel unitario por [`nuevaTarea.test.ts`](../../web/src/tablero/nuevaTarea.test.ts).
- **Resultado (medición del `2026-11-20`):**
  - Número de clics desde el tablero del equipo: **`4 clics`**.
  - Peticiones al servidor verificadas: **`POST /api/hitos-equipo/{id}/tareas` → `201`** y **`POST /api/tareas/{id}/dependencias` → `200`**.
  - Tiempo total del flujo: **`1,5 s`**.
- **Veredicto:** **Cumple RNF5**.
- **Evidencia:** [`evidencias/rnf5-resultado.json`](evidencias/rnf5-resultado.json) y capturas del recorrido:
  - [`evidencias/rnf5-0-tablero.png`](evidencias/rnf5-0-tablero.png) — tablero inicial del Equipo Beta
  - [`evidencias/rnf5-1-nueva-tarea.png`](evidencias/rnf5-1-nueva-tarea.png) — clic 1: modal «Nueva tarea» con foco en Título
  - [`evidencias/rnf5-2-bloqueada-por.png`](evidencias/rnf5-2-bloqueada-por.png) — clic 2: desplegable «Bloqueada por» abierto
  - [`evidencias/rnf5-3-primera-tarea-de-la-lista.png`](evidencias/rnf5-3-primera-tarea-de-la-lista.png) — clic 3: primera tarea seleccionada
  - [`evidencias/rnf5-4-guardar.png`](evidencias/rnf5-4-guardar.png) — clic 4: pulsación de «Guardar»
  - [`evidencias/rnf5-5-creada.png`](evidencias/rnf5-5-creada.png) — tarea creada en el tablero
