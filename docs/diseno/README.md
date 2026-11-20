# Documento de diseño — UniProject

Este documento describe la arquitectura real de **UniProject** tal como está construida en el código fuente, los patrones de diseño y principios SOLID aplicados en sus módulos, y el registro de decisiones de arquitectura (ADR) con las justificaciones técnicas y las diferencias respecto a la primera entrega. Sirve como referencia técnica verificable contra el repositorio para la sustentación del proyecto final de Diseño de Software I.

## Índice

- [01. Arquitectura del sistema](01-arquitectura.md): vistas de contexto, contenedores, componentes del núcleo, componentes del agente de Telegram, despliegue en Docker Compose y tácticas para los atributos de calidad (RNF1–RNF5 y Ley 1581).
- [02. Patrones de diseño y principios SOLID](02-patrones.md): catálogo de patrones implementados en el código con su problema, aplicación, archivos y fragmentos reales, seguido del cumplimiento de los cinco principios SOLID.
- [03. Decisiones de arquitectura](03-decisiones.md): registro de decisiones arquitectónicas (DA-01 a DA-12) en formato ADR corto, alternativas descartadas y tabla resumen de diferencias frente a la entrega 1.
- [04. Modelo de datos](04-modelo-datos.md): diagramas entidad-relación por grupos del esquema `public` (29 tablas) y del esquema `agente` (`evento_procesado`), restricciones e índices críticos, triggers de auditoría inmutable y diferencias frente al modelo de 19 tablas de la entrega 1.
- [05. Modelo del dominio y casos de uso](05-dominio.md): diagramas de clases por módulo con claves foráneas planas (`Long`), servicios, enumeraciones, contrato de eventos, agente de Telegram y diagrama general de casos de uso (RF1–RF15).
- [06. Comportamiento y flujos dinámicos](06-comportamiento.md): máquinas de estado de tarea e hito de equipo, diagramas de secuencia reales (dependencias, envío de hito y evaluación), diagramas de actividades (entrega/evaluación y cuellos de botella) y los 3 casos de uso obligatorios con sus endpoints reales.
- [07. Matriz de trazabilidad de requisitos](07-trazabilidad.md): resumen de cumplimiento y conteo de la suite de pruebas automáticas, matriz completa de requerimientos funcionales (RF1–RF15) con pantalla, endpoints, servicios y pruebas, matriz de requerimientos no funcionales (RNF1–RNF5) y matriz de restricciones (§2.1 y Ley 1581).
