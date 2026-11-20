# Guía de la sesión de pruebas final (UniProject)

Guía operativa para la ejecución de pruebas del stack independiente de UniProject (`uniproject-pruebas`).

---

## 1. Para qué es y reglas de oro

Esta sesión valida de forma integral los flujos de negocio, el control de acceso entre roles, las notificaciones y la resiliencia del sistema en un entorno aislado con datos precargados.

- **Datos ficticios y Ley 1581 de 2012:** Toda la sesión utiliza cuentas simuladas bajo el dominio `@demo.uniproject.test`. Queda terminantemente prohibido introducir nombres, correos o teléfonos personales reales.
- **Privacidad en capturas y registros:** En capturas de pantalla, transcripciones y reportes de hallazgos nunca deben figurar identificadores privados de Telegram (`chat_id`, nombres de usuario personales ni números telefónicos).
- **Custodia de credenciales:** Las contraseñas de las cuentas demo y del administrador nunca se registran en el código ni en el repositorio; se generan dinámicamente y residen exclusivamente en el archivo local `%LOCALAPPDATA%\UniProject\sesion-pruebas\credenciales.txt`.

---

## 2. Preparación

1. **Requisitos previos:**
   - Docker Desktop en ejecución.
   - Archivo `.env` presente en la raíz del repositorio con los secretos de infraestructura.
   - Si se van a probar notificaciones por Telegram: configurar `TELEGRAM_BOT_TOKEN` y `TELEGRAM_BOT_USERNAME` en `.env`, y si el agente de desarrollo está corriendo con token, detenerlo previamente: `docker compose -p uniproject stop agente`.
2. **Levantar el stack de pruebas:**
   ```powershell
   .\infra\pruebas\sesion-pruebas.ps1 -Accion Levantar
   ```
3. **Acceso al aplicativo:** Abrir exclusivamente `http://127.0.0.1:18080` en el navegador (nunca usar `localhost`, ya que compartiría y sobreescribiría las cookies de desarrollo).
4. **Perfiles de navegador (Aislamiento de sesiones):** Cada rol o persona debe usar una ventana de incógnito o un perfil de navegador independiente (ej. Perfil 1, Perfil 2, Perfil 3). Si se inicia sesión con dos usuarios en el mismo perfil, la segunda sesión invalidará a la primera.
   - **P1:** Docente (`docente@demo.uniproject.test`) y Administrador (`admin@demo.uniproject.test`).
   - **P2:** Líderes (`diego@demo.uniproject.test`, `ana@demo.uniproject.test`) y Visitante / Registro.
   - **P3:** Integrantes (`felipe@demo.uniproject.test`, `elena@demo.uniproject.test`, `bruno@demo.uniproject.test`) y Monitor (`monitor@demo.uniproject.test`).
5. **Anotar identificadores (IDs):** Al ingresar como docente, anotar desde la URL los IDs del proyecto demo, de los equipos (Alfa y Beta), de los hitos y de al menos una tarea para ejecutar las pruebas de control de acceso por URL.

---

## 3. Credenciales demo

Las contraseñas de todas las cuentas se encuentran en `%LOCALAPPDATA%\UniProject\sesion-pruebas\credenciales.txt` (`DEMO_CONTRASENA` para las cuentas demo y `ADMIN_CONTRASENA` para el administrador).

| Correo | Nombre | Rol | Equipo | Para qué se usa en la sesión |
|---|---|---|---|---|
| `docente@demo.uniproject.test` | Laura Méndez | DOCENTE | — | Gestión del proyecto, alertas, correos, evaluar hitos y reportes PDF. |
| `ana@demo.uniproject.test` | Ana Rojas | ESTUDIANTE (Líder) | Equipo Alfa | Ver hito ya evaluado y calificaciones en equipo con flujo completo. |
| `bruno@demo.uniproject.test` | Bruno Díaz | ESTUDIANTE | Equipo Alfa | Comprobar visibilidad de nota propia e historial en Alfa. |
| `camila@demo.uniproject.test` | Camila Torres | ESTUDIANTE | Equipo Alfa | Verificación de tareas en equipo completado. |
| `diego@demo.uniproject.test` | Diego Pérez | ESTUDIANTE (Líder) | Equipo Beta | Gestión de tablero, ruta crítica, reasignaciones y envío de hito. |
| `elena@demo.uniproject.test` | Elena Gómez | ESTUDIANTE | Equipo Beta | Cuenta con inactividad (6 días); avance y evidencias. |
| `felipe@demo.uniproject.test` | Felipe Ruiz | ESTUDIANTE | Equipo Beta | Solicitud de apoyo, subida de evidencias y correcciones. |
| `monitor@demo.uniproject.test` | Gabriela Soto | MONITOR | — | Visualización transversal de tareas y comentarios de apoyo. |
| `hugo@demo.uniproject.test` | Hugo Lara | ESTUDIANTE | Sin equipo | Cuenta de sacrificio: límites de login, recuperación y supresión. |
| `admin@demo.uniproject.test` | Administrador (pruebas) | ADMINISTRADOR | — | Habilitación de docentes en `/admin/docentes`. |

*Correos habilitados sin cuenta registrada:* `ines@demo.uniproject.test` (para probar registro abriendo el enlace de invitación copiado por el docente desde «Correos habilitados sin cuenta») y `jorge@demo.uniproject.test` (para probar retiro de invitación).

---

## 4. El escenario cargado

- **Proyecto:** «Biblioteca digital (demo)».
- **Hitos:**
  - Hito 1: «Requisitos y diagramas» (vence en 3 días, alerta configurada a 3 días, avisa al docente).
  - Hito 2: «Prototipo funcional» (inicia en 4 días).
- **Equipo Alfa:** Hito 1 completo (100 %), entregado y evaluado por la docente con nota provisional.
- **Equipo Beta:** Hito 1 en curso. La tarea «Diagrama de clases» está bloqueada hace 3 días (>48 h, aparece en rojo en el panel del docente), Felipe solicitó apoyo, Elena no registra actividad hace 6 días (genera alerta de inactividad) y existe un comentario pedagógico del monitor.
- **Cuentas sueltas:** Hugo no pertenece a ningún equipo; Inés y Jorge están habilitados pero no tienen cuenta creada aún.

---

## 5. Orden recomendado

1. **Recorrido por rol (sección 6):** Validar flujos regulares de Docente, Líder, Integrante, Monitor, Administrador y Visitante.
2. **Notificaciones (sección 7):** Vinculación de Telegram y verificación de entrega de eventos en tiempo real.
3. **Reportes e historial (sección 8):** Descarga de reporte PDF y control de visibilidad de auditoría y notas.
4. **Intentos para romper la página (sección 10):** Validaciones de borde, inyecciones, límites de texto y concurrencia.
5. **Límites y seguridad (sección 9 - AL FINAL):** Bloqueos por fuerza bruta, recuperación y supresión destructiva de Hugo.
   - *Nota:* Entre pruebas de límites, ejecutar `.\infra\pruebas\sesion-pruebas.ps1 -Accion LimpiarLimites`.

---

## 6. Pruebas por rol

### 6.1 Docente (Laura)

| # | Flujo | Pasos (cuenta y pantalla) | Resultado esperado | Debe negar |
|---|---|---|---|---|
| D1 | Panel del proyecto | Entrar como Laura en `/panel` o `/proyectos/{idProyecto}/panel`. Observar sección «Alertas». | Beta figura con alertas (bloqueo >48 h en «Diagrama de clases» e inactividad de Elena); Alfa sin alertas. | Cambiar avance o evidencias de tareas desde esta vista. |
| D2 | Habilitar y retirar correos | En `/proyectos/{idProyecto}`, sección estudiantes/monitores, pegar correos y pulsar `Agregar`. En «Correos habilitados sin cuenta», pulsar `Retirar` en `jorge@` y confirmar con `Sí, retirar`. | Correos se habilitan en lote; la invitación de Jorge desaparece de pendientes. | Modificar o retirar cuentas ya registradas. |
| D3 | Equipos y roles | En `/proyectos/{idProyecto}`, pulsar `+ Agregar estudiante` en Beta y añadir a Hugo; luego pulsar `Designar líder` en otro miembro y restaurar a Diego; quitar a Gabriela con `Quitar` y reincorporarla con `+ Agregar monitor`. | Los cambios de integrantes, liderazgos y monitores se guardan inmediatamente. | Dejar un equipo sin integrantes o duplicar miembros. |
| D4 | Crear proyecto | Ir a `/proyectos/nuevo`, definir nombre, fechas, 2 hitos y rúbrica; dejar vacía la alerta de inactividad de un hito y pulsar `Guardar proyecto`. | Proyecto creado; el hito sin alerta opera sin avisos periódicos. | Guardar con fecha fin anterior a inicio o campos requeridos vacíos. |
| D5 | Calificación grupal | En `/hitos/{idHitoEquipo}/evaluar` de Alfa, en «Nota grupal» cambiar la nota y pulsar `Cambiar nota grupal`; luego en tabla individual pulsar `Confirmar todas`. | Notas sugeridas recalculadas y confirmadas; cambio registrado en auditoría. | Asignar notas fuera del rango 0.0 a 5.0 (en editar notas ya confirmadas: anotar qué ocurre (la spec no define que se niegue)). |
| D6 | Evaluar hito entregado | Tras el envío de Beta (L6), abrir `/hitos/{idHitoEquipo}/evaluar` de Beta, calificar rúbrica, retroalimentación y pulsar `Registrar evaluación`. | Hito pasa a evaluado con nota provisional; se notifica a los integrantes. | Evaluar un hito que aún esté en progreso (no enviado). |
| D7 | Comentario docente | En `/tareas/{idTarea}`, redactar observación en «Comentarios» y pulsar `Comentar`. | Comentario publicado con insignia `Docente`. | Editar o borrar comentarios emitidos. |
| D8 | Reportes y eliminación | Ir a `/reportes`, seleccionar proyecto y descargar PDF. En `/perfil` intentar eliminar cuenta. | Descarga exitosa de PDF; eliminación de cuenta es rechazada por tener proyectos activos. | Acceder a `/admin/docentes` (403); eliminar cuenta propia con proyectos activos. |

### 6.2 Líder (Diego, Beta)

| # | Flujo | Pasos (cuenta y pantalla) | Resultado esperado | Debe negar |
|---|---|---|---|---|
| L1 | Crear tarea | En `/equipos/{idEquipo}/tablero` pulsar `Nueva tarea`, llenar título, descripción, horas, encargado y prerrequisito; pulsar `Guardar` (≤4 clics, RNF5). | Tarea creada con su prerrequisito vinculado en el tablero. | Crear dependencias circulares o entre distintos proyectos. |
| L2 | Ver ruta crítica | En `/equipos/{idEquipo}/tablero`, pulsar `Ver ruta crítica` (lleva a `/hitos/{idHitoEquipo}/ruta-critica`). | Se despliega la secuencia de tareas críticas resaltadas con holgura cero. | Modificar tareas desde la vista de ruta crítica. |
| L3 | Atender apoyo y reasignar | En `/tareas/{idTarea}` con apoyo de Felipe, pulsar `Reasignar tarea`, seleccionar nuevo encargado, opción «Solo una parte (se crea una tarea nueva)» o «Toda la tarea», motivo y pulsar `Reasignar`. | Tarea reasignada o subtarea creada con carga actualizada. | Asignar a integrantes ajenos al equipo. |
| L4 | Desbloqueo de tarea | En `/tareas/{idTarea}` de «Levantar requisitos», registrar avance de 100 % en «Registrar avance». | «Diagrama de clases» se desbloquea automáticamente en el tablero. | Avanzar una tarea mientras mantenga prerrequisitos incompletos. |
| L5 | Pedir corrección | En `/tareas/{idTarea}` con evidencia de un integrante, pulsar `Solicitar corrección`, redactar observaciones y pulsar `Enviar solicitud`. | La tarea regresa a «En progreso» y se notifica al estudiante. | Solicitar corrección sin observaciones obligatorias. |
| L6 | Enviar hito a revisión | En `/equipos/{idEquipo}/tablero`, pulsar `Enviar a revisión` con críticas pendientes (debe advertir/bloquear); completar críticas y pulsar `Enviar a revisión`. | Hito cambia a estado `ENVIADO` y se envía aviso a la docente. | Enviar el hito si existen tareas de la ruta crítica incompletas (RF7). |
| L7 | Avance de tarea ajena | En `/tareas/{idTarea}` asignada a otro miembro de Beta, el líder modifica el porcentaje y pulsa `Registrar avance`. | Avance registrado correctamente (facultad del líder de equipo). | Abrir tablero de Alfa por URL (`/equipos/{idAlfa}/tablero`); evaluar hitos; ver `/panel` o `/reportes`; suprimir su cuenta siendo líder activo. |

### 6.3 Integrante (Felipe/Elena en Beta; Bruno en Alfa)

| # | Flujo | Pasos (cuenta y pantalla) | Resultado esperado | Debe negar |
|---|---|---|---|---|
| I1 | Mis tareas | Entrar como Felipe a `/mis-tareas`. | Lista exclusivamente las tareas asignadas a Felipe con estado e hito. | Mostrar tareas ajenas en esta vista personal. |
| I2 | Registrar avance | En `/tareas/{idTarea}` asignada a Felipe, ingresar porcentaje y observación; pulsar `Registrar avance`. | Porcentaje y observación actualizados en la tarea. | Anotar qué ocurre si la tarea se encuentra bloqueada (la spec no define que se niegue). |
| I3 | Adjuntar evidencias | En `/tareas/{idTarea}`, pulsar `Adjuntar evidencia`, subir PDF/imagen o enlace. Intentar subir archivo >12 MB o `.exe`. | Archivo válido o enlace publicado; rechazo claro ante archivos >12 MB o formatos no permitidos. | Subir archivos mayores a 12 MB o extensiones ejecutables no autorizadas. |
| I4 | Solicitar apoyo | En `/tareas/{idTarea}` asignada, pulsar `Solicitar apoyo`, escribir motivo y pulsar `Enviar solicitud`. | La tarea muestra marca de apoyo solicitado y notifica al líder. | Solicitar apoyo con texto vacío (en tareas completadas: anotar qué ocurre (la spec no define que se niegue)). |
| I5 | Comentar tarea | En `/tareas/{idTarea}` de su equipo, escribir en «Comentarios» y pulsar `Comentar`. | Comentario publicado con su nombre y fecha. | Enviar comentarios vacíos o que excedan 2000 caracteres. |
| I6 | Ver evaluación de Alfa | Entrar como Bruno a `/hitos/{idHitoAlfa}/evaluacion`. | Visualiza rúbrica evaluada, retroalimentación y su calificación provisional. | Modificar notas o rúbricas de evaluación. |
| I7 | Historial restringido | Entrar como Bruno a `/equipos/{idAlfa}/historial`. | Ve la línea de tiempo del equipo, pero en calificaciones solo figura SU nota individual. | Ver notas individuales de compañeros; editar avance/evidencia de tareas ajenas (RF13); crear tareas; ver otro equipo; acceder a `/panel` o `/reportes`. |

### 6.4 Monitor (Gabriela)

| # | Flujo | Pasos (cuenta y pantalla) | Resultado esperado | Debe negar |
|---|---|---|---|---|
| M1 | Ver tareas | Entrar como Gabriela a `/proyectos`, ingresar al tablero de Alfa y de Beta (`/equipos/{id}/tablero`). | Visualiza el tablero, estado de tareas y avance de ambos equipos. | Cambiar porcentajes de avance, estados o subir evidencias. |
| M2 | Comentar tareas | Abrir `/tareas/{idTarea}`, redactar retroalimentación en «Comentarios» y pulsar `Comentar`. | Comentario visible con la insignia `Monitor`. | Evaluar hitos (`/hitos/{id}/evaluar`); acceder a `/panel`, `/reportes` o `/equipos/{id}/historial` (403). |

### 6.5 Administrador

| # | Flujo | Pasos (cuenta y pantalla) | Resultado esperado | Debe negar |
|---|---|---|---|---|
| A1 | Habilitar docente | Ingresar a `/admin/docentes`, en «Habilitar docente» escribir `docente2@demo.uniproject.test`, pulsar `Habilitar` y luego pulsar `Copiar enlace` en su fila. | Correo añadido a la tabla con estado «Pendiente de registro»; al copiar, muestra confirmación con fecha de vencimiento y el enlace de un solo uso seleccionable. | Habilitar formatos de correo inválidos o duplicados; generar enlace para una cuenta ya creada. |
| A2 | Registro y aislamiento | En ventana de incógnito abrir el enlace copiado en A1 (`/registro?codigo=...`), registrar `docente2@` e ingresar. | Acceso concedido; lista de proyectos vacía; sin acceso al proyecto demo ni por URL directa. | Eliminar la cuenta administradora desde `/perfil`; acceder a vistas de docente (`/panel`, `/proyectos/nuevo`); reutilizar el mismo enlace después de crear la cuenta. |

### 6.6 Visitante y Hugo

| # | Flujo | Pasos (cuenta y pantalla) | Resultado esperado | Debe negar |
|---|---|---|---|---|
| V1 | Ruta protegida | Sin sesión abierta, intentar navegar a `/proyectos`, `/mis-tareas` o `/perfil`. | Redirección automática a la pantalla de `/ingresar`. | Acceder a cualquier pantalla interna o dato sin autenticación. |
| V2 | Registro de usuario | Como docente en `/proyectos/{idProyecto}`, pulsar `Copiar enlace` en `ines@demo.uniproject.test`; en ventana de incógnito abrir ese enlace (`/registro?codigo=...`), probar primero con otro correo y luego registrarse con `ines@demo.uniproject.test`. | Rechazo genérico (`REGISTRO_NO_DISPONIBLE`) si el correo no coincide con la invitación; registro exitoso con redirección a `/ingresar` y aviso de cuenta creada para Inés. | Registrarse con un correo distinto al del enlace o no habilitado previamente. |
| V3 | Usuario sin equipo (Hugo) | Iniciar sesión como Hugo e ingresar a `/mis-tareas`, `/tablero` y `/ruta-critica`. | Vistas limpias con avisos de estado vacío («Sin tareas», «Sin equipo»), sin errores 500. | Realizar acciones de equipo o crear tareas sin pertenecer a un equipo. |
| V4 | Abrir `/registro` sin enlace | Sin sesión abierta, navegar directamente a `/registro` (o a `/registro?codigo=invalido`) o pulsar «Crear cuenta» desde `/ingresar`. | No se muestra el formulario; muestra «Para crear tu cuenta necesitas el enlace de invitación que te envió tu docente.» y el enlace a iniciar sesión. | Mostrar el formulario o permitir enviar el registro sin código válido. |
| V5 | Enlace ya usado o regenerado | 1) Abrir el enlace ya usado por Inés en V2 e intentar crear otra cuenta. 2) Como docente o administrador, generar dos enlaces seguidos para un correo pendiente e intentar registrarse con el primer enlace. | Ambos intentos se rechazan con `409 REGISTRO_NO_DISPONIBLE` y el mensaje genérico del núcleo, sin revelar si el enlace fue usado, venció o fue reemplazado. | Reutilizar un enlace consumido o registrar con un enlace anterior tras regenerarlo. |

---

## 7. Notificaciones (Telegram real)

Si `TELEGRAM_BOT_TOKEN` está vacío en `.env`, el sistema opera exclusivamente con la bandeja web interna (`/notificaciones`).

1. **Vinculación:**
   - En `/perfil` → tarjeta `Telegram` → pulsar `Vincular Telegram`.
   - Se muestra un enlace directo (`Abrir Telegram`) o el comando `/start <codigo>`.
   - Abrir el bot en Telegram, pulsar `Iniciar` o enviar `/start <codigo>`.
   - En la web pulsar `Ya lo vinculé`: la insignia cambia a `Vinculado`.
   - *Reglas:* El código vence en 15 minutos (vigencia configurada); reutilizarlo es rechazado. Un usuario de Telegram solo puede asociarse a una cuenta a la vez; si se intenta asociar a otra cuenta: [anotar resultado]. Para desvincular, usar el botón `Desvincular` en `/perfil`.
   - Vincular al menos: Docente (Laura), Líder (Diego), Integrante (Felipe), y al final Hugo (para recuperación).
2. **Matriz de comprobación de eventos:**

| Evento | Cómo provocarlo | Quién lo recibe | Comprobar |
|---|---|---|---|
| Hito enviado | Diego envía Hito 1 (L6) | Docente | Aviso en Telegram y en `/notificaciones` con enlace al hito. |
| Hito evaluado | Laura evalúa Hito 1 (D6) | Integrantes de Beta | Aviso con nota provisional y retroalimentación. |
| Tarea reasignada | Diego reasigna tarea (L3) | Felipe y nuevo encargado | Aviso con el nombre de la tarea y motivo. |
| Corrección pedida | Diego pide corrección (L5) | Felipe | Aviso indicando que la tarea vuelve a «En progreso». |
| Riesgo e inactividad | Programador cada 5 min (o `-Accion RepetirAlertas`) | Líder, integrante y docente | Alerta de tarea bloqueada e inactividad de Elena. |
| Recuperación clave | Hugo solicita en `/olvide-contrasena` | Hugo | Enlace de restablecimiento enviado por Telegram. |

3. **Verificaciones de seguridad:**
   - Tiempo de llegada: pocos segundos tras la acción.
   - Inyección en textos: Crear una tarea con título `<b>x</b> & <i>`; el mensaje en Telegram y en `/notificaciones` debe mostrarse como texto plano literal, sin interpretar etiquetas HTML.
   - Privacidad: Nunca copiar identificadores privados de Telegram en los reportes de prueba.

---

## 8. Reportes e historial

1. **Generación de reporte PDF (Docente):**
   - Como docente, ingresar a `/reportes`, seleccionar «Biblioteca digital (demo)», equipo Alfa y Beta, e hito.
   - Observar el historial desplegado en pantalla y pulsar `Descargar PDF`.
   - Abrir los PDF y comprobar que contienen: historial de cambios con autores, entregas realizadas, rúbrica y calificaciones (RF15).
   - Validar que tildes, caracteres «ñ» y posibles emojis no corrompan el formato del PDF: [anotar resultado].
2. **Control de acceso al reporte:**
   - Un estudiante intenta abrir `/reportes` (bloqueado por menú y router).
   - Un estudiante intenta descargar directamente el PDF por URL (`GET /api/equipos/{idEquipo}/reporte.pdf`): debe responder `403 Forbidden`.
3. **Línea de tiempo `/equipos/{idEquipo}/historial`:**
   - Docente: visualiza el historial completo del equipo y las calificaciones de todos los integrantes.
   - Integrante del equipo: visualiza el historial de actividades y únicamente SU nota individual.
   - Integrante de otro equipo y Monitor: acceso denegado con código `403 Forbidden`.
   - *Nota:* La plataforma no contempla exportación a CSV; la especificación formal requiere exclusivamente reporte en formato PDF.

---

## 9. Límites y seguridad (AL FINAL)

Ejecutar estas pruebas destructivas al finalizar el resto de las comprobaciones.

1. **Límites de inicio de sesión (`application.yml`):**
   - *Límite por correo (10 intentos/hora):* Intentar 10 ingresos fallidos con `hugo@demo.uniproject.test`. En el intento 11 (incluso con la contraseña correcta), el sistema responde «Demasiados intentos. Espera un rato y vuelve a intentarlo».
   - Ejecutar `.\infra\pruebas\sesion-pruebas.ps1 -Accion LimpiarLimites` para desbloquear.
   - *Límite por IP (20 intentos/hora):* Intentar 20 ingresos fallidos con correos inexistentes. A partir del intento 21, todos los inicios de sesión desde esta máquina quedan bloqueados durante 1 hora.
   - *Riesgo conocido:* Registrar como hallazgo de arquitectura que bajo esquemas de red NAT (ej. campus universitario) este límite puede afectar a múltiples usuarios que compartan la misma IP pública. Desbloquear con `-Accion LimpiarLimites`.
2. **Recuperación de contraseña (Hugo con Telegram vinculado):**
   - En `/ingresar`, pulsar «¿Olvidaste tu contraseña?» (lleva a `/olvide-contrasena`).
   - Escribir `hugo@demo.uniproject.test` y pulsar `Enviar enlace`.
   - Abrir el enlace recibido por Telegram en el navegador (`/restablecer-contrasena#codigo=...`).
   - Ingresar nueva contraseña y pulsar `Guardar contraseña`. Comprobar aviso de éxito y que cualquier otra sesión abierta de Hugo en otro navegador se cierra automáticamente.
   - Probar con un correo no registrado: la interfaz muestra exactamente el mismo mensaje genérico para evitar enumeración de usuarios.
   - Límite de recuperación: máximo 3 solicitudes por correo por hora (`maximo-por-correo: 3`). En la 4.ª solicitud: [anotar respuesta del sistema].
   - Reutilización de token: intentar usar el mismo enlace por segunda vez (debe ser rechazado). Actualizar la contraseña de Hugo en `credenciales.txt`.
3. **Supresión de cuenta (Ley 1581) en `/perfil` → «Eliminar cuenta»:**
   - Diego (líder activo con contraseña correcta) → Rechazado por liderar equipo activo.
   - Laura (docente con proyectos activos) → Rechazado por tener proyectos activos.
   - Administrador → Botón no disponible («La cuenta de administrador no se puede eliminar desde la aplicación.»).
   - Hugo: ingresar contraseña incorrecta → muestra «La contraseña no es correcta». Ingresar contraseña correcta y marcar casilla de confirmación → cuenta anonimizada, sesión cerrada automáticamente, no puede volver a ingresar; sus aportes previos figuran como «Usuario anonimizado».
   - Fuerza bruta en perfil: ingresar 11 contraseñas erróneas consecutivas al intentar cambiar contraseña o eliminar cuenta → bloqueo por exceso de intentos.
4. **Protección CSRF:**
   - Con sesión abierta, abrir DevTools (F12) → pestaña Consola y ejecutar una petición POST sin la cabecera `X-XSRF-TOKEN`:
     ```javascript
     fetch('/api/notificaciones/leidas', { method: 'POST' }).then(r => console.log('HTTP', r.status));
     ```
   - Debe retornar código `403 Forbidden`. Con la cookie enviada en la cabecera `X-XSRF-TOKEN`, responde `204 No Content`.

---

## 10. Intentos para romper la página

| Intento | Dónde | Qué esperar | Si pasa otra cosa |
|---|---|---|---|
| Título de tarea (150 vs 151 caracteres) | Modal «Nueva tarea» / Detalle | 150 caracteres guarda correctamente; 151 es rechazado por validación (`@Size(max=150)`). | Fallo de validación o corte silencioso. |
| Descripción extensa (2000 vs 2001) | Tarea, proyecto o hito | 2000 caracteres guarda bien; 2001 es rechazado (`@Size(max=2000)`). | Error 500 en backend. |
| Mensaje de apoyo / motivo (500 vs 501) | Modales de apoyo y reasignación | 500 caracteres admitidos; 501 rechazados (`@Size(max=500)`). | Se trunca sin aviso. |
| Observaciones corrección (1000 vs 1001) | Modal solicitar corrección | 1000 caracteres admitidos; 1001 rechazados (`@Size(max=1000)`). | Error no controlado. |
| Comentario de tarea (2000 vs 2001) | Tarjeta comentarios | 2000 caracteres admitidos; 2001 rechazados (`@Size(max=2000)`). | Mensaje de error no visible. |
| Nombre de equipo (100 vs 101) | Tarjeta equipo en proyecto | 100 caracteres admitidos; 101 rechazados (`@Size(max=100)`). | Desborde de layout. |
| Retroalimentación docente (4000 vs 4001) | Pantalla evaluar hito | 4000 caracteres admitidos; 4001 rechazados (`@Size(max=4000)`). | Excepción JPA no controlada. |
| Contraseña de registro (<10, >72) | `/registro` | Menos de 10 o más de 72 caracteres es rechazado (`@Size(min=10, max=72)`). | Permite contraseñas débiles o largas. |
| 150 emojis en título | Modal «Nueva tarea» | Cada emoji puede ocupar 2 o más caracteres en Java/UTF-16: [anotar si rechaza antes de 150]. | Error 500 por longitud en BD. |
| Caracteres especiales y scripts | Títulos, nombres y comentarios | Introducir `' " ; -- \ ñ 漢字 RTL` y `<script>alert(1)</script>`. Se muestra como texto plano sin ejecutar. | Ejecución de script (XSS) o rotura visual. |
| Doble clic rápido en acciones | «Nueva tarea», «Enviar a revisión», «Evaluar» | Los botones se deshabilitan inmediatamente durante el envío; se procesa una única acción. | Duplicación de registros o doble envío. |
| Lote con 60 correos complejos | Habilitar correos en lote | Pegar correos con mayúsculas, espacios, duplicados y uno `@anonimo.invalid`. Resumen claro de aceptados/rechazados. | Fallo total sin indicar cuáles sirvieron. |
| Lote con 201 correos | Habilitar correos en lote | Rechazo inmediato por superar el límite máximo de 200 correos (`@Size(max=200)`). | Sobrecarga de base de datos. |
| Fechas imposibles | Configurar proyecto / hito | Fin anterior a inicio, 31 de febrero o año 9999 son rechazados con aviso en campo. | Acepta fechas ilógicas. |
| Dependencia cíclica | Modal «Nueva tarea» / Dependencias | Intentar que A dependa de B y B dependa de A: el sistema detecta el ciclo y lo rechaza. | Bloqueo o bucle infinito en cálculo. |
| Horas estimadas inválidas | Modal «Nueva tarea» / Reasignar | Ingresar 0 horas, números negativos o 10000 horas: rechazado (`@DecimalMin`, `@Digits(integer=4)`). | Acepta horas negativas o absurdas. |
| Porcentaje de avance fuera de rango | «Registrar avance» | Ingresar -1 % o 101 %: validación impide el envío (`@Min(0)`, `@Max(100)`). | Guarda valores fuera de escala. |
| Permisos cruzados por URL | Barra de direcciones | Intentar abrir con ID anotado el tablero o reporte de otro equipo: página de «Sin acceso». | Fuga de información confidencial. |
| Sesión expirada | Formulario abierto en pantalla | Borrar cookie de sesión en DevTools y pulsar guardar: redirige al ingreso sin pantalla en blanco. | Pantalla blanca sin mensaje de sesión. |
| Concurrencia en avance | Dos pestañas con misma tarea | Modificar avance con valores distintos en ambas: [anotar cuál prevalece y si avisa conflicto]. | Datos corruptos o desincronizados. |
| Botón Atrás tras salir | Navegador | Pulsar botón Atrás tras cerrar sesión: no muestra datos cacheados ni permite acciones. | Permite operar sin sesión válida. |
| Recarga en ruta profunda | F5 en `/equipos/1/tablero` | La pantalla recarga exactamente en la misma vista, sin dar error 404 del servidor. | Error 404 Not Found en recarga. |
| Ventana estrecha (375px) | DevTools modo responsivo | La interfaz se adapta ordenadamente en columna única sin desbordamientos críticos. | Elementos solapados o inalcanzables. |

*Qué podría romper la página:* Monitorear con DevTools abierto la memoria y red en escenarios de: grafos de ruta crítica con alta densidad de tareas, carga de archivos pesados al tope de 12 MB, renderizado de rúbricas extensas, lotes masivos de correos y caducidad de sesión durante la edición.

---

## 11. Cómo anotar un fallo

1. **Herramientas de diagnóstico:** Mantener abiertas las pestañas **Consola** y **Red** de las herramientas de desarrollador (F12).
2. **Datos obligatorios:** Registrar el método HTTP, código de respuesta (400, 403, 404, 500), mensaje devuelto y URL del endpoint.
3. **Extracción de logs:**
   ```powershell
   docker compose -p uniproject-pruebas logs --tail 50 nucleo
   docker compose -p uniproject-pruebas logs --tail 50 agente
   ```
   *Revisión previa:* Verificar que el fragmento de log no contenga contraseñas ni tokens antes de adjuntarlo.
4. **Evidencias gráficas:** Tomar captura de pantalla asegurando que no se visualicen datos personales ni de Telegram.
5. **Niveles de severidad:**
   - **Bloqueante:** Impide continuar la sesión o deja la aplicación inaccesible sin alternativa posible.
   - **Alta:** Falla en control de acceso, permisos cruzados, visibilidad de datos ajenos o pérdida de información.
   - **Media:** Falla funcional relevante para la cual existe una vía alternativa o paso de recuperación.
   - **Baja:** Inconsistencia funcional menor que no interrumpe el flujo normal del trabajo.
   - **Cosmética:** Defecto puramente estético, desalineación visual o error ortográfico.

---

## 12. Plantilla de registro de hallazgos

Guardar los hallazgos encontrados durante la sesión en el archivo `docs/pruebas/hallazgos-AAAA-MM-DD.md` utilizando el siguiente formato de tabla:

| ID | Fecha y hora | Quién | Cuenta/rol | Pantalla o URL | Pasos | Esperado | Obtenido | Severidad | Evidencia (archivo/captura) | Consola/HTTP | Estado |
|---|---|---|---|---|---|---|---|---|---|---|---|
| H-01 | 2026-11-06 15:30 | Ana | `diego@` (Líder) | `/tareas/12` | 1. Clic en «Registrar avance»<br>2. Ingresar 105 % | Error de validación impidiendo el envío | Permitió guardar y mostró 105 % | Alta | `captura-h01.png` | POST 200 OK | Abierto |

---

## 13. Cierre

1. **Detener el stack de pruebas:**
   - *Conservando datos para continuar más tarde:*
     ```powershell
     .\infra\pruebas\sesion-pruebas.ps1 -Accion Bajar
     ```
   - *Eliminando volúmenes y credenciales generadas:*
     ```powershell
     .\infra\pruebas\sesion-pruebas.ps1 -Accion Bajar -BorrarDatos
     ```
2. **Restaurar entorno de desarrollo:**
   - Si se van a ejecutar las pruebas E2E de desarrollo, asegurarse de volver a dejar `TELEGRAM_BOT_TOKEN` vacío en `.env` (la suite E2E exige token vacío).
   - Si se había detenido el servicio `agente` de desarrollo en el paso de preparación, volver a iniciarlo:
     ```powershell
     docker compose -p uniproject start agente
     ```
