# Lista previa para la sustentación — UniProject

Lista operativa de verificación para presentar **UniProject** en vivo el **jueves 2026-11-26** (20 minutos) desde el computador de **Santy**, utilizando el entorno aislado de la sesión de pruebas (`infra/pruebas/sesion-pruebas.ps1`, URL `http://127.0.0.1:18080`).

> **Regla de privacidad y credenciales:** las contraseñas demo (`DEMO_CONTRASENA`) y de administración (`ADMIN_CONTRASENA`) **no se escriben** en documentos ni en pantalla compartida. Se consultan únicamente en `%LOCALAPPDATA%\UniProject\sesion-pruebas\credenciales.txt`. Toda la demo usa cuentas ficticias `@demo.uniproject.test`.

---

## 1. El día anterior (miércoles 2026-11-25)

### 1.1. Entorno, variables y bot único de Telegram
- [ ] Confirmar que **Docker Desktop** esté activo y actualizado y que exista `.env` en la raíz del repositorio con todas las variables requeridas (incluidos `TELEGRAM_BOT_TOKEN` y `TELEGRAM_BOT_USERNAME`).
- [ ] Verificar que **solo el computador de Santy** correrá el agente de Telegram el día de la sustentación: **Juan y Samuel deben mantener apagado cualquier proceso o contenedor `agente` que use el mismo token**, para evitar errores `409 Conflict` de Telegram al hacer *long polling*.
- [ ] En el PC de Santy, si el stack de desarrollo (`uniproject`) tiene el servicio `agente` corriendo, detenerlo antes de levantar pruebas:
  ```powershell
  docker compose -p uniproject stop agente
  ```

### 1.2. Levantar datos demo frescos y comprobar estado
- [ ] Desde la raíz del repositorio, reiniciar el stack de pruebas para limpiar cualquier volumen previo y cargar el escenario fresco de `CargaDemo` («Biblioteca digital (demo)» y «App de turnos (demo)»):
  ```powershell
  .\infra\pruebas\sesion-pruebas.ps1 -Accion Reiniciar
  ```
- [ ] Comprobar que todos los servicios queden arriba y saludables:
  ```powershell
  .\infra\pruebas\sesion-pruebas.ps1 -Accion Estado
  ```
  Verificar en la salida:
  - `base-de-datos`, `rabbitmq`, `nucleo` y `agente` en estado `running` (y los dos primeros `healthy`).
  - `Codigo HTTP /actuator/health: 200`.
  - `Resultado carga demo: Datos demo cargados` (o `Los datos demo ya existian`).
  - `TELEGRAM_BOT_TOKEN: no vacio`, `TELEGRAM_BOT_USERNAME: no vacio`, `Agente de desarrollo: no corriendo`.
  - `Archivo credenciales: existe (%LOCALAPPDATA%\UniProject\sesion-pruebas\credenciales.txt)`.
- [ ] Revisar los logs recientes del agente para descartar `409` de Telegram y confirmar `Started AgenteAplicacion`:
  ```powershell
  docker compose -p uniproject-pruebas logs --tail 30 agente
  ```

### 1.3. Ensayo, archivos de prueba y grabación de respaldo
- [ ] Preparar un archivo **PDF pequeño (< 1 MB)** de muestra (p. ej. 1 página sin datos personales) en una carpeta conocida para adjuntarlo como evidencia en el bloque 11–14 (`RF12`).
- [ ] Hacer un ensayo completo cronometrado de los 20 minutos siguiendo `docs/sustentacion/guion.md`.
- [ ] Si durante el ensayo se modificaron tareas, evaluaciones o enlaces, dejar la base limpia al terminar ejecutando de nuevo:
  ```powershell
  .\infra\pruebas\sesion-pruebas.ps1 -Accion Reiniciar
  ```
- [ ] *(Después de aprobar el guion en pruebas)* Grabar el **video de respaldo** del recorrido completo y guardar las capturas de pantalla limpias (sin contraseñas ni `chatId` visibles).

---

## 2. 30 minutos antes de la sustentación (jueves 2026-11-26)

### 2.1. Arranque y verificación del stack
- [ ] Confirmar que **Juan y Samuel no tienen ningún bot encendido** con el token de UniProject.
- [ ] Si el stack de pruebas se detuvo la noche anterior con `-Accion Bajar`, levantarlo (sin borrar datos si ya está limpio, o con `-Accion Reiniciar` si se necesita restablecer):
  ```powershell
  .\infra\pruebas\sesion-pruebas.ps1 -Accion Levantar
  .\infra\pruebas\sesion-pruebas.ps1 -Accion Estado
  ```
- [ ] Revisar de nuevo los logs del agente (`docker compose -p uniproject-pruebas logs --tail 20 agente`) para confirmar ausencia de `409 Conflict` y conexión activa con RabbitMQ.
- [ ] Leer `DEMO_CONTRASENA` y `ADMIN_CONTRASENA` desde `%LOCALAPPDATA%\UniProject\sesion-pruebas\credenciales.txt` **sin abrir ese archivo mientras se comparte pantalla**.

### 2.2. Vincular una cuenta demo al Telegram de quien presenta
- [ ] En la ventana del líder **Diego Pérez** (`diego@demo.uniproject.test`), ir a `/perfil` → tarjeta `Telegram` → pulsar `Vincular Telegram` → abrir el bot con `Abrir Telegram` (o enviar `/start <codigo>`) → pulsar `Ya lo vinculé` y comprobar la insignia `Vinculado`.
  - *Por qué vincular a Diego:* Diego es líder del **Equipo Beta** («Biblioteca digital (demo)») e integrante del **Equipo Gamma** («App de turnos (demo)»). Con esa sola cuenta vinculada recibirá en vivo `tarea.reasignada` (**RF11**), las alertas de `equipo.inactividad` (**RF6**) y `tarea.ruta_critica.riesgo` (**RF4**), y el aviso de `hito.evaluado` (**RF8**) cuando la docente evalúe Gamma.
- [ ] **Inmediatamente después de vincular Telegram**, reiniciar las marcas de envío de alertas periódicas para que el programador (configurado cada `5 min` en `sesion-pruebas.ps1`) envíe las alertas de riesgo e inactividad en la próxima pasada múltiplo de 5 minutos ya con el `chatId` activo:
  ```powershell
  .\infra\pruebas\sesion-pruebas.ps1 -Accion RepetirAlertas
  ```
- [ ] Abrir Telegram Desktop/Web en una ventana aparte con los demás chats silenciados o archivados para no exponer conversaciones personales al proyectar.

### 2.3. Ventanas o perfiles de navegador (aislamiento de sesión)
Abrir **cuatro perfiles de navegador independientes o ventanas con sesión aislada** apuntando siempre a `http://127.0.0.1:18080` (nunca `localhost`, para no mezclar cookies). Dos inicios de sesión en el mismo perfil invalidan la sesión anterior:

| Ventana / Perfil | Rol y persona | Correo demo | Ruta inicial recomendada |
|---|---|---|---|
| **Ventana 1** | Docente — Laura Méndez | `docente@demo.uniproject.test` | `/panel` (tener vistos los IDs de proyectos, Alfa, Beta y Gamma) |
| **Ventana 2** | Líder — Diego Pérez | `diego@demo.uniproject.test` | `/equipos/{idBeta}/tablero` (Beta, Hito 1) |
| **Ventana 3** | Estudiante — Felipe Ruiz | `felipe@demo.uniproject.test` | `/mis-tareas` |
| **Ventana 4** | Administrador (pruebas) | `admin@demo.uniproject.test` | `/admin/docentes` (apoyo para preguntas de alta inicial) |

### 2.4. Documentos y pestañas de apoyo listas
- [ ] **PDF del diseño:** `docs/diseno/UniProject-diseno.pdf` abierto en el visor en §1.2 (*Vista de contenedores*) y §1.4 (*Agente de Telegram*), y listo para saltar a §7 (*Matriz de trazabilidad*).
- [ ] **README en GitHub:** pestaña del repositorio abierta mostrando la insignia de CI `Calidad` en verde (y el flujo `.github/workflows/calidad.yml`).
- [ ] **Evidencias de RNF y auditoría:** pestañas o editor listos con:
  - `docs/pruebas/evidencias/rnf2-resumen.txt` (prueba de carga k6, 500 VUs).
  - `docs/pruebas/evidencias-rnf.md` (RNF1–RNF5) y `docs/diseno/07-trazabilidad.md` §7.1 (conteo de 611 pruebas automáticas).
  - `docs/pruebas/evidencias/rnf3-resultado.json` y `rnf5-resultado.json` (+ capturas `rnf5-0-tablero.png` a `rnf5-5-creada.png`).
- [ ] **Archivo PDF pequeño (< 1 MB)** ubicado a la mano en el escritorio/carpeta de pruebas para el bloque 11–14.

---

## 3. Plan B ante fallos en vivo

| Incidente probable | Qué hacer en el acto (sin improvisar ni perder tiempo) |
|---|---|
| **Falla internet o la Bot API de Telegram (o aparece `409`)** | No reiniciar el stack durante la presentación. Abrir `/notificaciones` (bandeja interna P10) en la ventana de Diego/Laura para mostrar que la misma notificación se guardó en PostgreSQL en la transacción del negocio, explicar la tolerancia a fallos con outbox + RabbitMQ (`docs/diseno/01-arquitectura.md` §1.4) y mostrar el video/captura de respaldo del bot enviando el mensaje. |
| **No ha coincidido el múltiplo de 5 min para alertas periódicas (`RF4`/`RF6`)** | Mostrar la notificación instantánea de `tarea.reasignada` (**RF11**, entregada por la outbox cada `2 s`) y luego la de `hito.evaluado` (**RF8**), explicando que el programador horario corre en producción cada hora y cada 5 min en pruebas; verificar ambos avisos en `/notificaciones`. |
| **Falla internet para abrir GitHub y la insignia de CI** | Mostrar la captura local de GitHub Actions en verde junto con `docs/diseno/07-trazabilidad.md` §7.1 (611 pruebas) y `docs/pruebas/informe-auditoria-final.md` §1. |
| **Falla Docker o se congela una pantalla web** | Cambiar de inmediato al **video de respaldo** pregrabado y a las capturas en `docs/pruebas/evidencias/`, manteniendo el ritmo de narración con `UniProject-diseno.pdf`. |
| **Se cierra sesión por error en una ventana o se cruzan cookies** | Verificar que las cuatro cuentas estén en perfiles distintos y siempre en `http://127.0.0.1:18080`; volver a iniciar sesión leyendo el archivo `credenciales.txt` en la pantalla no proyectada. |

---

## 4. Cierre posterior a la sustentación

- [ ] Detener el stack de pruebas conservando o limpiando los datos:
  ```powershell
  .\infra\pruebas\sesion-pruebas.ps1 -Accion Bajar
  ```
- [ ] Si se quiere eliminar los volúmenes de pruebas y `credenciales.txt`:
  ```powershell
  .\infra\pruebas\sesion-pruebas.ps1 -Accion Bajar -BorrarDatos
  ```
