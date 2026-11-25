# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Docente (usuario principal del rediseño):** profesor de un curso universitario con varios equipos y poco
  tiempo. Crea el proyecto, define hitos con fecha límite y rúbrica, configura alertas de inactividad, sigue a los
  equipos desde el panel, califica cada hito enviado, revisa la nota individual sugerida y decide la nota final,
  y exporta el reporte de trazabilidad en PDF. Trabaja sobre todo en computador; entra desde el celular cuando le
  llega un aviso por Telegram.
- **Estudiante:** integrante de un equipo. Consulta sus tareas y fechas, registra el porcentaje de avance, adjunta
  evidencias (PDF, imágenes o enlaces), atiende alertas y solicitudes de corrección, envía el hito y consulta la
  evaluación. Usa computador y celular por igual.
- **Líder de equipo:** estudiante con permisos extra. Divide el hito en tareas, estima su duración, las asigna,
  define dependencias y prerrequisitos críticos, reasigna cuando alguien pide apoyo y revisa evidencias. Usa
  computador y celular por igual.
- **Monitor (opcional):** lo asigna el docente; consulta tareas en curso y deja comentarios sin cambiar estado ni
  avance.
- **Administrador:** habilita los correos de los docentes.

## Product Purpose

Gestionar proyectos académicos grupales organizados por hitos para que el trabajo se reparta de forma más
equitativa y el docente pueda seguir el proceso, no solo el resultado: controla dependencias entre tareas, detecta
a tiempo los retrasos en la ruta crítica que comprometen una entrega y mide la contribución individual de cada
integrante. Éxito: el docente ve a tiempo qué equipo necesita intervención y califica con una base concreta en
lugar de poner la misma nota a todos.

## Positioning

Está construido alrededor del ciclo académico, cosa que Trello o Asana no hacen: hitos con fecha límite y rúbrica,
envío bloqueado mientras haya prerrequisitos críticos sin terminar, nota provisional que pasa a definitiva al
vencer la fecha, nota individual sugerida a partir de la calificación grupal y del peso de las tareas que cada
estudiante completó a tiempo, y un registro de auditoría inmutable que sirve de prueba.

## Operating Context

- Se presenta en la sustentación del curso Diseño de Software 1 con los datos de demostración, pero se construye
  como un sistema terminado para uso real con docentes y estudiantes (decisión de Santy, 2026-11-18). Cuentan
  tanto la primera impresión como el uso diario.
- Semestre de 16 semanas; el uso sube en las semanas 8 y 16 (entregas principales).
- Los avisos (retraso en ruta crítica, inactividad, hito enviado o evaluado, reasignación, solicitud de
  corrección) llegan por Telegram y quedan en la bandeja de notificaciones de la web; el aviso lleva a la pantalla
  correspondiente.
- Los entregables pesados viven en servicios externos y se adjuntan como enlaces.
- Al final del semestre se exporta en PDF el reporte con el historial completo del equipo.

## Capabilities and Constraints

- Aplicación web de una sola página en español (Colombia: coma decimal, fechas locales). Sin aplicaciones móviles
  nativas.
- Funciona en las versiones actuales de Chrome, Edge, Safari y Firefox (Baseline), sin polyfills.
- Reglas de negocio que la interfaz debe reflejar: un estudiante no modifica estado ni adjuntos de tareas ajenas;
  el monitor comenta pero no cambia estado ni avance; un hito no se envía con prerrequisitos críticos incompletos;
  las dependencias no forman ciclos; la auditoría no se edita ni se borra.
- Evidencias: solo PDF, imágenes o enlaces.
- Las acciones disponibles dependen del rol; nadie se registra solo (el administrador habilita docentes y el
  docente habilita estudiantes y monitores).
- Ley 1581 de 2012 (datos personales en Colombia): no mostrar ni registrar datos personales sin necesidad; las
  cuentas se suprimen por anonimización.
- Usabilidad (RNF5): crear una tarea con dependencia en 4 clics como máximo desde el panel del equipo. La ruta
  crítica de hasta 150 tareas se dibuja en 2 segundos como máximo (RNF1).

## Brand Commitments

- Nombre: **UniProject**.
- El diseño de Figma (notas en `Archivos_contexto/figma-notas-26b.md` y `-26c.md`) fue una guía, **no** es
  obligatorio: se conservan el nombre y la función de cada pantalla, pero el aspecto puede cambiar libremente
  (decisión de Santy, 2026-11-18). Santy no señaló logo, colores institucionales ni otra marca obligatoria.

## Evidence on Hand

- Datos de demostración (módulo `nucleo/.../demo`): docente Laura Méndez, Equipo Alfa y Equipo Beta, monitora,
  proyecto de ejemplo con hitos, tareas, evidencias y una evaluación.
- Requisitos y casos de uso: `Archivos_contexto/entrega1.md`.
- No hay usuarios reales, testimonios, instituciones clientes ni métricas de uso: no se deben inventar.

## Product Principles

1. **Proceso visible, no solo resultado:** cada pantalla del docente debe dejar ver cómo avanza cada equipo y
   quién aporta.
2. **Avisar a tiempo para poder actuar:** lo que está en riesgo (ruta crítica, inactividad, notas sin confirmar)
   va primero y lleva directo a donde se resuelve.
3. **Justicia con evidencia:** las notas sugeridas, las evaluaciones y la auditoría se explican en lenguaje claro y
   se pueden sostener ante un reclamo.
4. **Cada rol ve lo que puede hacer:** las restricciones de rol y de reglas de negocio se muestran antes de que el
   usuario choque con ellas.

## Accessibility & Inclusion

- Meta: WCAG 2.2 AA (contraste, foco visible también en alto contraste, objetivos táctiles, uso completo con
  teclado y lector de pantalla).
- Docente: computador primero, con acceso cómodo desde el celular al abrir un aviso. Estudiantes y líderes:
  computador y celular por igual. Revisar a 1440 y 390 px sin desborde horizontal.
