# Documento de diseño — UniProject

Este documento describe la arquitectura real de **UniProject** tal como está construida en el código fuente, los patrones de diseño y principios SOLID aplicados en sus módulos, y el registro de decisiones de arquitectura (ADR) con las justificaciones técnicas y las diferencias respecto a la primera entrega. Sirve como referencia técnica verificable contra el repositorio para la sustentación del proyecto final de Diseño de Software I.

## Índice

- [01. Arquitectura del sistema](01-arquitectura.md): vistas de contexto, contenedores, componentes del núcleo, componentes del agente de Telegram, despliegue en Docker Compose y tácticas para los atributos de calidad (RNF1–RNF5 y Ley 1581).
- [02. Patrones de diseño y principios SOLID](02-patrones.md): catálogo de patrones implementados en el código con su problema, aplicación, archivos y fragmentos reales, seguido del cumplimiento de los cinco principios SOLID.
- [03. Decisiones de arquitectura](03-decisiones.md): registro de decisiones arquitectónicas (DA-01 a DA-12) en formato ADR corto, alternativas descartadas y tabla resumen de diferencias frente a la entrega 1.
