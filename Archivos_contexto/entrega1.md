# Entrega 1 — UniProject (texto extraído del PDF)

> Fuente: `Proyecto final diseño 1ra entrega (3).pdf` (17 páginas, versión con cambios del 30/09/2026).
> Las imágenes (prototipos de Figma, diagrama de base de datos, diagramas de actividades y de secuencia) no se pueden extraer como texto; para verlos hay que abrir el PDF o Figma.
> Buscar secciones con Grep por encabezado, p. ej. `^## 2\.4` o `^### Requerimientos funcionales`.

## 2.1. Contexto del problema

### Nombre del proyecto
UniProject: sistema de gestión de proyectos colaborativos universitarios.

### Descripción del proyecto
En los cursos universitarios, buena parte de la nota depende de proyectos grupales que se entregan por hitos durante el semestre. Es frecuente que la carga termine en manos de unos pocos integrantes y que una tarea atrasada frene a las que dependen de ella sin que nadie lo advierta hasta que la entrega está encima. El docente, por su lado, recibe el producto terminado sin saber cómo se repartió el trabajo. Así que califica a todos por igual.

El docente crea el proyecto en UniProject y define sus hitos, cada uno con fecha límite y rúbrica de evaluación. Después, el líder de cada equipo divide el trabajo en tareas y las asigna a sus compañeros. Al crear cada tarea estima cuánto va a durar e indica de qué otras tareas depende. Con esa información el sistema calcula la ruta crítica del proyecto, y si una tarea de esa ruta se atrasa hasta poner en riesgo la fecha del hito, avisa al equipo y al docente.

Cada estudiante registra en sus tareas el porcentaje de avance y adjunta las evidencias de su trabajo (archivos PDF, imágenes o enlaces a repositorios). El líder revisa esas evidencias y, si encuentra errores, puede pedirle al integrante que rehaga la entrega indicando qué debe corregir. Mientras haya tareas marcadas como prerrequisito crítico sin terminar, el equipo no puede enviar el hito a revisión. Una vez enviado, el docente lo califica con la rúbrica. Si la fecha límite del hito no ha vencido, esa nota es provisional y el equipo puede seguir editando sus tareas; cuando la fecha vence, la última nota asignada queda como definitiva. A partir de la calificación grupal, el sistema le sugiere al docente una nota individual para cada integrante, ajustada según el peso de las tareas que cada estudiante completó a tiempo. La nota final la decide el docente.

El docente puede configurar alertas de inactividad, que se disparan cuando un estudiante pasa el tiempo que él defina sin registrar actividad en sus tareas. La alerta siempre le llega al líder del equipo, y el docente decide si también quiere recibirla. Si la institución trabaja con monitores, el docente puede asignarlos al proyecto para que comenten las tareas en curso sin cambiar su estado ni su avance. Las reasignaciones de tareas quedan en un registro de auditoría que nadie puede modificar, y al final del semestre se puede exportar en PDF un reporte con el historial completo del equipo.

Herramientas como Trello o Asana sirven para organizar pendientes, pero no manejan hitos con rúbrica, no bloquean una entrega por dependencias sin cumplir y no convierten el aporte de cada persona en una nota. UniProject está construido alrededor de ese ciclo académico.

### Objetivo general
Diseñar y construir un sistema web para gestionar proyectos académicos grupales organizados por hitos, que controle las dependencias entre tareas, detecte a tiempo los retrasos que comprometen las entregas y mida la contribución individual de cada integrante, de modo que el trabajo se reparta de forma más equitativa y el docente pueda seguir el proceso y no solo el resultado.

### Objetivos específicos
1. Permitir al docente crear proyectos académicos con varios hitos de entrega, cada uno con su fecha límite y su rúbrica de evaluación.
2. Permitir al líder del equipo crear tareas, asignarlas entre los integrantes, definir dependencias entre ellas y reasignarlas cuando haga falta.
3. Calcular y graficar de forma automática la ruta crítica del proyecto a partir de las dependencias y los tiempos estimados, y recalcularla cada vez que estos cambien.
4. Registrar el avance de cada tarea (porcentaje de avance y evidencias), detectar los retrasos en tareas críticas con notificación al equipo y al docente, y generar alertas para el líder del equipo, y de forma opcional para el docente, cuando un estudiante no registre actividad durante el periodo definido.
5. Controlar el flujo de entrega y calificación de cada hito, desde el bloqueo del envío cuando hay prerrequisitos críticos incompletos hasta la nota del docente, que es provisional mientras la fecha límite del hito no haya vencido.
6. Calcular y sugerir al docente la nota individual de cada estudiante a partir de la calificación grupal del hito, con un factor de ajuste basado en el peso de las tareas que completó a tiempo.
7. Restringir las acciones disponibles según el rol de cada usuario y mantener la trazabilidad del proyecto mediante un registro de auditoría, un panel de control para el docente y un reporte exportable en PDF.

### Fuera del alcance
Integración con plataformas institucionales, chat y videollamadas entre integrantes, aplicaciones móviles nativas y evaluación automática del contenido de los entregables (siempre califica el docente).

### Supuestos
1. Todos los usuarios tienen acceso a internet y a un dispositivo con un navegador web actualizado.
2. Cada equipo tiene un único líder, designado cuando se conforma el equipo.
3. El docente define los hitos, las rúbricas y el periodo de inactividad que dispara las alertas antes de que el equipo empiece a trabajar en cada hito.
4. Los estudiantes registran su porcentaje de avance con honestidad y a tiempo. El sistema guarda lo que se reporta.
5. Los tiempos estimados de las tareas los fija el líder, así que tanto la ruta crítica como el ajuste de la nota individual serán tan precisos como lo sean esas estimaciones.
6. El semestre académico dura 16 semanas y el uso de la plataforma sube en las semanas 8 y 16, que coinciden con las entregas principales.
7. Los entregables pesados (código, bases de datos, videos) se guardan en servicios externos, como repositorios o almacenamiento en la nube, y en el sistema se adjuntan como enlaces.

### Restricciones
1. Proyecto académico: limitado a la duración del semestre, lo hace un equipo de estudiantes y no cuenta con presupuesto, por lo que se usan herramientas gratuitas o con licencia académica.
2. La solución es una aplicación web que se usa desde el navegador. No se desarrollarán aplicaciones móviles nativas.
3. Las contraseñas se almacenan con bcrypt y un factor de costo mínimo de 12.
4. El sistema debe hacer cumplir estas reglas de negocio:
   a. Un estudiante regular no puede modificar el estado ni los adjuntos de una tarea asignada a otro compañero.
   b. Si el proyecto tiene monitores, estos pueden comentar las tareas, pero no pueden cambiar su estado ni su porcentaje de avance.
   c. Un hito no se puede enviar a revisión si tiene tareas marcadas como prerrequisito crítico en estado incompleto.
   d. Las dependencias entre tareas no pueden formar ciclos.
   e. El registro de auditoría no se puede editar ni borrar.
5. Las evidencias de una tarea solo pueden ser archivos PDF, imágenes o enlaces.
6. El manejo de los datos personales de estudiantes y docentes debe cumplir la Ley 1581 de 2012 de protección de datos personales en Colombia.

### Justificación
Cualquiera que haya hecho trabajos en grupo en la universidad ha visto al compañero que casi no aporta y recibe la misma nota que los demás. El docente tampoco tiene cómo notarlo, porque le llega un entregable terminado y muy poca información sobre cómo se hizo. Las herramientas comerciales de gestión de tareas ayudan a llevar pendientes, pero no están pensadas para hitos con rúbrica ni para calcular notas individuales.

UniProject responde a eso desde el diseño. Las alertas de retraso sobre la ruta crítica dan margen para reaccionar antes de que la fecha límite esté perdida, mientras que el registro de avances, evidencias y cambios le permite al docente ver cómo avanzó cada equipo. Y la nota individual sugerida, que ajusta la calificación grupal según las tareas que cada estudiante cumplió a tiempo, le da al docente una base concreta para no calificar a todos por igual.

## 2.2. Actores del sistema

### Identificación de actores
1. **Docente** (actor principal). Profesor del curso. Crea y configura los proyectos, supervisa el trabajo de los equipos y evalúa las entregas.
2. **Estudiante** (actor principal). Integrante de un equipo de proyecto. Ejecuta las tareas que se le asignan y reporta su avance.
3. **Líder de equipo** (actor principal). Estudiante con permisos adicionales para organizar el trabajo de su equipo. Se modela como especialización del actor Estudiante: conserva sus responsabilidades y suma las propias.
4. **Monitor** (actor principal y opcional). Solo participa si la institución trabaja con monitores; el docente lo asigna al configurar el proyecto. Acompaña con retroalimentación, sin intervenir en el estado de las tareas.
5. **Sistema de notificaciones** (actor secundario). Sistema externo que hace llegar a los usuarios los avisos que genera UniProject.

### Responsabilidades — Docente
1. Crear proyectos académicos y definir sus hitos, cada uno con fecha límite y rúbrica de evaluación.
2. Configurar las alertas de inactividad, el periodo sin actividad que las dispara y si él también quiere recibirlas.
3. Supervisar a los equipos desde el panel de control, donde se resaltan los equipos inactivos y los que tienen tareas bloqueadas por más de 48 horas.
4. Calificar con la rúbrica los entregables de cada hito enviado. Mientras la fecha límite no haya vencido, la nota es provisional y el equipo puede seguir editando; al vencer, queda como definitiva.
5. Revisar la nota individual que sugiere el sistema para cada estudiante y asignar la nota final.
6. Consultar y exportar el reporte de trazabilidad del equipo en PDF.

### Responsabilidades — Estudiante
1. Consultar las tareas que tiene asignadas y sus fechas límite.
2. Actualizar el porcentaje de avance de sus tareas.
3. Adjuntar a sus tareas las evidencias de su trabajo (archivos PDF, imágenes o enlaces a repositorios). Solo puede modificar el estado y los adjuntos de las tareas asignadas a él.
4. Atender las alertas de retraso y, si no puede terminar a tiempo, solicitar apoyo al líder.
5. Enviar el hito a revisión cuando todos los prerrequisitos críticos estén completos.
6. Consultar el resultado de la evaluación de cada hito.
7. Atender las solicitudes de corrección del líder y adjuntar la nueva versión de la entrega.

### Responsabilidades — Líder de equipo (además de las del estudiante)
1. Dividir el trabajo del hito en tareas y estimar la duración de cada una.
2. Asignar las tareas a los integrantes según lo discutido internamente por el grupo.
3. Establecer las dependencias entre tareas y marcar cuáles son prerrequisitos críticos del hito.
4. Analizar la carga del equipo cuando un integrante pide apoyo y reasignar tareas o parte de su alcance cuando haga falta. Cada reasignación queda en el registro de auditoría.
5. Revisar las evidencias que adjuntan los integrantes y, si encuentra errores, solicitar que rehagan la entrega indicando los puntos que deben corregir.

### Responsabilidades — Monitor
1. Consultar las tareas en curso de los equipos.
2. Dejar comentarios de retroalimentación en esas tareas, sin cambiar su estado ni su porcentaje de avance.

### Responsabilidades — Sistema de notificaciones
1. Avisar al equipo y al docente cuando una tarea de la ruta crítica se retrase hasta comprometer la fecha del hito.
2. Enviar las alertas de inactividad al líder del equipo y, si el docente lo eligió, también al docente.
3. Informar al docente cuando un equipo envía un hito a revisión y a los estudiantes cuando el hito ha sido evaluado.
4. Avisar a los integrantes afectados cuando cambian sus asignaciones por una reasignación.
5. Avisar al integrante cuando el líder le solicita rehacer una entrega.

## 2.3. Equipo del proyecto
- Juan Benavides (integrante), rasputin2631
- Samuel Lambertino (integrante), frahklp1
- Santy Baza (integrante), akko98

## 2.4. Requerimientos

### Requerimientos funcionales
- **RF1.** Permitir al docente crear proyectos académicos configurando múltiples hitos de entrega y vinculando rúbricas de evaluación a cada uno.
- **RF2.** Permitir al estudiante con rol de "Líder" crear y asignar tareas a los miembros del equipo, estableciendo dependencias jerárquicas (p. ej., la tarea A bloquea la tarea B).
- **RF3.** Calcular y graficar automáticamente la ruta crítica del proyecto basándose en las dependencias y los tiempos estimados de cada tarea.
- **RF4.** Enviar notificaciones automáticas al equipo y al docente cuando una tarea de la ruta crítica sufra un retraso que comprometa la fecha límite del hito.
- **RF5.** Permitir al líder revisar las evidencias que un integrante adjuntó en una tarea y, si encuentra errores, solicitarle que rehaga la entrega indicando los puntos que debe corregir. La tarea vuelve al estado "En progreso" hasta que el integrante adjunte la nueva versión.
- **RF6.** Permitir al docente configurar alertas automáticas de inactividad que se disparen si un estudiante no registra actividad en sus tareas asignadas (cambios de estado o evidencias) durante un periodo definido. La alerta siempre se envía al líder del equipo, y el docente decide si también la recibe.
- **RF7.** Bloquear la opción de enviar el hito a revisión del docente si existen tareas marcadas como "prerrequisito crítico" en estado incompleto.
- **RF8.** Permitir al docente calificar el hito enviado, asignando una nota y una retroalimentación a cada criterio de la rúbrica. Si la fecha límite del hito no ha vencido, la nota queda como provisional y los estudiantes pueden seguir editando sus tareas. Cuando la fecha vence, la última nota asignada queda como definitiva.
- **RF9.** Identificar tareas con alto riesgo de retraso a partir de su porcentaje de avance, tiempo restante, duración estimada y dependencias.
- **RF10.** Generar un panel de control (dashboard) para el docente, resaltando en rojo los equipos inactivos o aquellos con tareas bloqueadas por más de 48 horas.
- **RF11.** Permitir la reasignación de tareas entre los miembros del equipo, generando automáticamente un registro inmutable (log) de auditoría del cambio.
- **RF12.** Permitir a los estudiantes adjuntar evidencias (archivos PDF, imágenes o enlaces de repositorios) y actualizar el porcentaje de avance en el detalle de cada tarea.
- **RF13.** Restringir los permisos de modo que un estudiante regular no pueda modificar el estado ni los adjuntos de una tarea asignada a otro compañero.
- **RF14.** Si la institución trabaja con monitores, permitir al docente asignarlos a un proyecto. El monitor puede agregar comentarios de retroalimentación en las tareas en curso sin alterar su estado.
- **RF15.** Exportar un reporte de trazabilidad en PDF que consolide el historial de cambios, entregas y calificaciones del equipo durante todo el semestre.

### Requerimientos no funcionales
- **RNF1. Rendimiento:** el motor de dependencias debe calcular y renderizar la ruta crítica de proyectos con hasta 150 tareas en un tiempo de respuesta máximo de 2 segundos en el cliente.
- **RNF2. Concurrencia:** 500 usuarios simultáneos interactuando en los módulos de evaluación sin que el tiempo de respuesta del servidor supere 3 segundos.
- **RNF3. Disponibilidad:** uptime mínimo de 99,5 % durante las semanas pico (8 y 16).
- **RNF4. Seguridad:** contraseñas de todos los actores con hash bcrypt y factor de costo mínimo de 12.
- **RNF5. Usabilidad:** el flujo para crear una nueva tarea con dependencia debe requerir un máximo de 4 clics desde el panel principal del equipo.

## 2.5. Estructura del sistema
- **Capa de Presentación (Frontend):** interfaz con la que interactúan docentes y estudiantes. Captura interacciones, muestra tableros de avance y renderiza visualmente dependencias y bloqueos.
- **Capa de Enrutamiento y Control (API Gateway / Controladores):** punto de entrada de peticiones. Intercepta las solicitudes, verifica tokens de autenticación/roles y enruta la petición al servicio adecuado.
- **Capa de Lógica de Negocio (Servicios Core):** motor central. Procesa los algoritmos pesados, calcula rutas críticas, valida que las tareas bloqueadas no se ejecuten y calcula el Índice de Contribución Efectiva.
- **Capa de Acceso a Datos (Repositorios/DAO):** intermediario con la base de datos. Traduce objetos de negocio a consultas SQL y garantiza transacciones seguras (ACID).
- **Gestor Asíncrono de Notificaciones:** componente paralelo. Despacha correos electrónicos y alertas internas sin detener el tiempo de respuesta de la interfaz.
- **Relación entre componentes:** la Presentación se comunica exclusivamente por HTTP con Enrutamiento y Control. Esta delega las reglas a la Lógica de Negocio, que se apoya en Acceso a Datos para leer/escribir. Si un proceso crítico finaliza (p. ej., retraso de tarea crítica), la Lógica de Negocio dispara un evento al Gestor Asíncrono de Notificaciones.

## 2.6. División por módulos
- **Módulo de Configuración Académica:** permite a los docentes parametrizar el entorno de trabajo, instanciar proyectos, definir el cronograma de hitos obligatorios e integrar las rúbricas de evaluación base para cada entrega.
- **Módulo de Ejecución y Dependencias:** centraliza el trabajo de los estudiantes. Gestiona creación y asignación de tareas, controla los porcentajes de avance, permite al líder revisar evidencias y pedir correcciones, y aplica las reglas del motor de dependencias (bloquea tareas cuyo prerrequisito no esté al 100 %).
- **Módulo de Evaluación y Calificación Algorítmica:** congela la edición de tareas al enviar un hito, habilita la interfaz de corrección/retroalimentación para el docente y calcula automáticamente las notas individuales distribuyendo la calificación grupal según el peso del trabajo finalizado por cada estudiante.
- **Módulo de Trazabilidad y Seguridad (Logs):** registra de forma inmutable (solo lectura) cada acción crítica: reasignaciones por cuellos de botella, solicitudes de ayuda, cambios de estado y control de accesos. Es la fuente de datos verificable para el motor de calificaciones.

## 2.7. Artefactos obligatorios

### Caso de uso: Establecer dependencias
- **Actores:** Líder.
- **Descripción:** permite al líder establecer relaciones de dependencia entre tareas que bloqueen otras hasta estar terminadas.
- **Precondición:** el líder tiene la sesión iniciada, un proyecto asignado y existen al menos dos tareas.
- **Flujo principal:**
  1. El líder entra al menú del proyecto y selecciona una tarea.
  2. Selecciona la opción de establecer dependencias.
  3. Selecciona la o las tareas prerrequisito.
  4. El sistema guarda la relación y recalcula la ruta crítica.
  5. Fin exitoso.
- **Flujo alterno — Dependencia circular:** si en el paso 3 alguna tarea o sus prerrequisitos recursivos dependen de la tarea actual: se notifica al líder la dependencia circular, se excluyen las tareas causantes y se continúa en el paso 4.
- **Postcondición (éxito):** la tarea seleccionada queda inhabilitada hasta que se completen sus dependencias.

### Caso de uso: Enviar hito
- **Actores:** Estudiante, Sistema de notificaciones.
- **Descripción:** proceso para enviar un hito a revisión del docente.
- **Precondición:** el estudiante tiene la sesión iniciada y hay al menos un hito disponible dentro de un proyecto.
- **Flujo principal:**
  1. El estudiante selecciona enviar el hito a revisión.
  2. El sistema valida la compleción de todos los prerrequisitos críticos.
  3. El sistema notifica mediante un diálogo el envío exitoso.
  4. El sistema notifica al docente.
  5. Fin exitoso.
- **Flujo alterno — Tareas críticas incompletas:** si en el paso 2 hay prerrequisitos críticos sin completar, se notifica al estudiante mediante un diálogo las tareas faltantes y el caso de uso termina.
- **Postcondiciones:** éxito: las entregas de las tareas del hito quedan bloqueadas y el docente tiene la entrega en su bandeja. Falla: no se realiza ningún envío y el hito sigue editable.

### Caso de uso: Evaluar hito
- **Actores:** Docente, Sistema de notificaciones.
- **Descripción:** evaluación de un hito por el docente y registro de nota y retroalimentación.
- **Precondición:** el docente inició sesión y tiene en su bandeja al menos un hito entregado por calificar.
- **Flujo principal:**
  1. El docente selecciona el hito por calificar.
  2. Revisa las entregas de las diferentes tareas.
  3. Asigna nota y retroalimentación a cada elemento de la rúbrica.
  4. El sistema guarda las notas.
  5. El sistema permite la edición de los entregables.
  6. El sistema notifica a los estudiantes.
  7. Fin exitoso.
- **Flujo alterno — Fin del plazo:** si en el paso 5 ya pasó la fecha límite, no se habilita la edición posterior, la calificación se registra como final y se continúa en el paso 6.
- **Postcondición (éxito):** los estudiantes fueron notificados del resultado temporal o final del hito.

### Artefactos gráficos (solo en el PDF / Figma)
- Prototipos (Figma; la pantalla P10 es la bandeja de notificaciones).
- Diagrama de base de datos (modelo relacional de 19 tablas, entre ellas `Notificacion` y `Log_Auditoria`).
- Diagramas de actividades: "Flujo entrega y evaluación" y "Resolución de cuellos de botella".
- Diagramas de secuencia: "Envío de hito para revisión académica" y "Establecer dependencias entre tareas".
