# Prueba de carga RNF2 — Módulo de evaluación

Guion reproducible de [k6](https://k6.io/) ejecutado dentro de un contenedor de Docker (`grafana/k6:2.3.0`) para evaluar el requerimiento no funcional **RNF2** (`Archivos_contexto/entrega1.md`):

> **RNF2. Concurrencia:** 500 usuarios simultáneos interactuando en los módulos de evaluación sin que el tiempo de respuesta del servidor supere 3 segundos.

---

## 1. Qué mide

El guion [rnf2-evaluacion.js](rnf2-evaluacion.js) simula usuarios reales consultando la pantalla de evaluación con el escenario `ramping-vus`:

- **Subida:** 1 minuto hasta 500 usuarios virtuales (VUs).
- **Sostenido:** 3 minutos con 500 VUs simultáneos.
- **Bajada:** 30 segundos hasta 0 VUs.

Para respetar los límites de inicio de sesión por correo e IP, `setup()` inicia sesión **una sola vez por cuenta demo** (`docente@demo.uniproject.test` y los estudiantes registrados del equipo con un hito ya en estado `EVALUADO`) y descubre dinámicamente los IDs de proyecto, equipo y `hito_equipo` desde la API.

Durante la carga **solo se envían peticiones `GET` de lectura** con pausas aleatorias de 1 a 3 segundos entre pasos:

- **Recorrido docente (25 % de los VUs):**
  1. `GET /api/proyectos/{idProyecto}/panel` (`panel_docente`) — panel de control del docente.
  2. `GET /api/hitos-equipo/{idHitoEquipo}/tareas` (`entregas_hito`) — entregas del hito.
  3. `GET /api/proyectos/{idProyecto}` (`proyecto_detalle`) — detalle del proyecto con la rúbrica.
  4. `GET /api/hitos-equipo/{idHitoEquipo}/evaluacion` (`evaluacion_hito`) — calificación, criterios y nota individual sugerida.
- **Recorrido estudiante (75 % de los VUs):**
  1. `GET /api/hitos-equipo/{idHitoEquipo}/tareas` (`entregas_hito`).
  2. `GET /api/proyectos/{idProyecto}` (`proyecto_detalle`).
  3. `GET /api/hitos-equipo/{idHitoEquipo}/evaluacion` (`evaluacion_hito`).

---

## 2. Requisitos

1. **Docker Desktop** en ejecución (desde el contenedor se alcanza el servidor en el host mediante `http://host.docker.internal:18080`).
2. **Python 3** disponible en el sistema para ejecutar `correr_carga.py` (sin dependencias externas).
3. **Sesión de pruebas activa** en `http://127.0.0.1:18080`:
   ```powershell
   .\infra\pruebas\sesion-pruebas.ps1 -Accion Levantar
   ```
   Esta sesión genera el archivo `%LOCALAPPDATA%\UniProject\sesion-pruebas\credenciales.txt` con `DEMO_CONTRASENA`. El lanzador lee esa contraseña en memoria y la entrega al contenedor con `-e DEMO_CONTRASENA` sin imprimirla ni escribirla en archivos ni en argumentos de línea de comandos.

---

## 3. Cómo correrlo

Desde la raíz del repositorio:

- **Corrida corta de comprobación** (10 usuarios virtuales, 30 s en total: 5 s subida, 20 s sostenido, 5 s bajada):
  ```powershell
  python infra/carga/correr_carga.py --corta
  ```
- **Corrida completa RNF2** (500 usuarios virtuales, ~4 min 30 s en total):
  ```powershell
  python infra/carga/correr_carga.py
  ```
- **Guardar el resultado en una carpeta específica:**
  ```powershell
  python infra/carga/correr_carga.py --salida C:\ruta\resultados-rnf2
  ```

Si no se indica `--salida`, el lanzador crea una carpeta temporal `uniproject-carga-*` e imprime su ruta al terminar.

Variables de entorno opcionales para ajustar una corrida completa sin editar el guion:
- `BASE` (por defecto `http://host.docker.internal:18080`)
- `VUS_MAX` (por defecto `500`)
- `DURACION_SUBIDA` (por defecto `1m`)
- `DURACION_SOSTENIDA` (por defecto `3m`)
- `DURACION_BAJADA` (por defecto `30s`)

---

## 4. Cómo leer el resumen y umbrales

Al finalizar, `handleSummary` deja dos archivos en la carpeta de salida e imprime el resumen en consola:

- **`resumen.txt`**: reporte legible con el resultado global (`CUMPLE RNF2` / `NO CUMPLE RNF2`), métricas generales, percentiles por endpoint (`min`, `avg`, `med`, `p(90)`, `p(95)`, `p(99)`, `max`), conteo de `checks` y estado de cada umbral.
- **`resumen.json`**: volcado completo en JSON generado por k6 con todas las métricas, sub-métricas por etiqueta y verificaciones.

### Significado de cada umbral

| Umbral | Condición | Qué valida |
|---|---|---|
| `http_req_duration{endpoint:...}` (`p(95)<3000`) | Percentil 95 < 3000 ms | Al menos el 95 % de las respuestas de cada endpoint (`panel_docente`, `entregas_hito`, `proyecto_detalle`, `evaluacion_hito`) respondió en menos de 3 segundos. |
| `http_req_duration{endpoint:...}` (`max<3000`) | Máximo < 3000 ms | Ninguna petición de los endpoints evaluados superó el límite de 3 segundos fijado por RNF2. |
| `http_req_duration{fase:evaluacion}` | `p(95)<3000` y `max<3000` | Mismo criterio agregado sobre todas las consultas del flujo de evaluación (excluye la fase de `setup()`). |
| `http_req_failed` | `rate<0.01` | Menos del 1 % de las peticiones HTTP falló por error de red o código HTTP ≥ 400. |
| `checks` | `rate>0.99` | Más del 99 % de las verificaciones de código HTTP `200` resultó exitoso. |

### Códigos de salida del lanzador

- **`0`:** todos los umbrales pasaron.
- **`99`:** la prueba terminó, pero uno o más umbrales fallaron (consultar `resumen.txt` para ver qué endpoint o métrica incumplió).
- **Otro código distinto de `0`:** error previo o de ejecución (Docker no disponible, sesión de pruebas caída o archivo de credenciales ausente).

---

## 5. Advertencia sobre la medición en un PC personal

Cuando esta prueba se corre en un computador personal, el generador de carga (`k6`) y el servidor (`nucleo`, PostgreSQL y RabbitMQ) compiten por los mismos núcleos de CPU, memoria y disco. Los resultados obtenidos en un PC local son una **evidencia indicativa** útil para detectar regresiones o cuellos de botella evidentes, pero no sustituyen una prueba en infraestructura dedicada con servidor y cliente separados.

---

# Prueba de disponibilidad RNF3 — Recuperación ante caída (`rnf3_disponibilidad.py`)

Guion reproducible en Python (sin dependencias externas) para evaluar la recuperación automática exigida por **RNF3** (`Archivos_contexto/entrega1.md`):

> **RNF3. Disponibilidad:** uptime mínimo de 99,5 % durante las semanas pico (8 y 16).

## 1. Qué hace y qué caída provoca

El guion [rnf3_disponibilidad.py](rnf3_disponibilidad.py):

1. Sondea `http://127.0.0.1:18080/actuator/health` cada **1 segundo** (con timeout de `2 s`) esperando respuesta HTTP `200` con `"UP"`, durante una ventana configurable (`--minutos`, por defecto `6` min).
2. **Provoca una caída real del núcleo:** al alcanzar `--caida-en` segundos desde el inicio (por defecto `60` s), ejecuta `docker exec uniproject-pruebas-nucleo-1 kill -TERM 1`, enviando `SIGTERM` al proceso `java` (PID 1 del contenedor del núcleo).
3. El proceso termina con código distinto de `0` y Docker lo reinicia automáticamente sin intervención humana gracias a la política `restart: unless-stopped` definida en [infra/docker-compose.yml](../docker-compose.yml). No modifica ni borra datos en PostgreSQL ni en la outbox.
4. Al terminar la ventana, consulta `docker inspect --format '{{.RestartCount}}'` sobre `uniproject-pruebas-nucleo-1` y escribe dos archivos en la carpeta de salida (además de imprimir el resumen en JSON):
   - **`rnf3-muestras.json`**: serie temporal de sondeos (`t`, `arriba`) y marca de la caída provocada.
   - **`rnf3-resultado.json`**: duración medida, total de sondeos, sondeos caídos, disponibilidad porcentual en la ventana, segundos sin servicio, contador de reinicios de Docker y el presupuesto semanal equivalente al 99,5 % (`50,4` minutos por semana).

## 2. Cómo correrlo

Requisitos previos: **Docker Desktop** activo, **Python 3** y la sesión de pruebas levantada en `http://127.0.0.1:18080` (`.\infra\pruebas\sesion-pruebas.ps1 -Accion Levantar`).

Desde la raíz del repositorio:

```powershell
python infra/carga/rnf3_disponibilidad.py <carpeta-de-salida> [--minutos 6] [--caida-en 60]
```

Por ejemplo, con los parámetros por defecto (6 minutos, caída provocada al segundo 60):

```powershell
python infra/carga/rnf3_disponibilidad.py C:\ruta\resultados-rnf3
```

## 3. Alcance de la prueba

Esta prueba mide el **tiempo de recuperación automática tras una caída real del proceso del núcleo** en el entorno Docker Compose local (`restart: unless-stopped` y arranque con migraciones Flyway); **no** sustituye una medición continua de semanas reales (semanas 8 y 16) en producción ni evalúa caídas de infraestructura externa.
