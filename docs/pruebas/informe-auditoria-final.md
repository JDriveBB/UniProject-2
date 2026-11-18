# Informe de auditoría final — UniProject

Fecha: 2026-11-18 · Base: commit `e0a6b1e` (rama `main`) · Entorno: stack de pruebas aparte (`http://127.0.0.1:18080`, escenario demo).

## 1. Qué se ejecutó
| Prueba | Resultado |
|---|---|
| `mvnw verify` completo (núcleo 325, agente 73, eventos 16) | 414 pruebas, 0 fallos, BUILD SUCCESS (5 min 13 s) |
| Web: `tsc`, `vitest` (172 pruebas / 43 archivos), `oxlint` (react/rules-of-hooks) | verde |
| OpenAPI con `docs/openapi/redocly.yaml` | válido (40 advertencias de estilo: `operation-summary`, `tag-description`) |
| Recorrido en navegador: 7 roles × 25 rutas (175 visitas) | sin pantallas en blanco, errores de React, 5xx ni «Cargando» atascado |
| Acceso cruzado por API (28 intentos: equipo ajeno, hito ajeno, tarea ajena, panel, correos, admin) | todos 403; el monitor ve la evaluación del grupo y **0** notas individuales; la estudiante ve solo la suya |
| Entradas hostiles (JSON roto, 5000 caracteres, vacío, HTML, emoji, ids absurdos, path traversal, CSRF ausente) | 400/403 correctos; **fallos:** carácter nulo → 500; número aceptado como texto |
| RNF1 (150 tareas con dependencias) | servidor 15–30 ms; grafo renderizado 140–176 ms (límite 2 s) |
| RNF2 acotado (500 peticiones, 100 en paralelo, evaluación) | 500 × 200, p95 105 ms, máximo 258 ms (límite 3 s) |
| Auditoría de código backend (66 endpoints) y frontend/contratos (RF/RNF) | ver §2 y §3 |
| No ejecutado | E2E (exige token de Telegram vacío), prueba real del bot (requiere interacción humana), RNF2 con 500 usuarios reales, RNF3 (disponibilidad) |

## 2. Hallazgos y decisión del usuario (2026-11-18)
| # | Gravedad | Hallazgo | Decisión |
|---|---|---|---|
| 1 | ALTA | El registro no comprueba que quien se registra sea dueño del correo habilitado (D6: sin correos); `/api/cuentas/registro` sin límite y con respuestas que permiten enumerar correos habilitados. | Solo límite por IP y mensaje uniforme. La suplantación por correo habilitado queda como riesgo documentado (§4). |
| 2 | MEDIA | `ServicioDependencias`: dos `agregar` simultáneos pueden crear un ciclo; `MotorDependencias` lanza `IllegalStateException` y el hito responde 500. | Corregir: bloquear el hito. |
| 3 | MEDIA | Mismo hueco (sin `bloquear`) en avance, evidencias, tareas, reasignación y correcciones: guardados tras el envío, doble clic crea dos partes. | Corregir. |
| 4 | MEDIA (Ley 1581) | `ServicioSupresion` deja `registro_avance.observacion` y `solicitud_correccion.observaciones` (y su payload en outbox). | Corregir. |
| 5 | MEDIA | Un carácter nulo (`\u0000`) en cualquier texto libre da 500 (PostgreSQL rechaza 0x00). Un nombre enviado como número se acepta. | Corregir. |
| 6 | MEDIA | La regla «un equipo por estudiante en cada proyecto» solo se comprueba en Java; dos altas simultáneas la rompen. | Corregir. |
| 7 | MEDIA (web) | Un 401 a mitad de pantalla desmonta todo y se pierde lo escrito sin aviso. | Corregir. |
| 8 | A VERIFICAR | Límites por `getRemoteAddr()`: con Docker Desktop en Windows todos los clientes podrían verse con la IP de la puerta de enlace; `LOGIN_CORREO` permite bloquear una cuenta conocida durante 1 h. | Documentar; verificar en el despliegue real. |
| 9 | BAJA | Cookie de sesión sin `secure: true` (activar bajo HTTPS); `/interno/**` alcanzable en el puerto público (protegido por token); código de vinculación de Telegram guardado en claro; cada 403 escribe en `log_auditoria` sin límite; `conOtroRol` revela cuentas; editar rúbrica mientras se evalúa; el rol del núcleo es dueño de la BD. | Documentar. |
| 10 | BAJA (web) | Nuevo proyecto con fallo parcial deja la URL en `/proyectos/nuevo`; descarga de evidencias con `<a download>` guarda archivos vacíos ante 401/403; rutas inexistentes muestran la página «Whitelabel» de Spring; detalles de validación con nombres técnicos de campo. | Corregir lo marcado como «web». |
| 11 | BAJA (contrato) | `MiTareaVista.nivelRiesgo` puede ser null y no está marcado nullable; el OpenAPI declara 404 en `POST /api/notificaciones/{id}/leida` pero el servicio responde 403; destino distinto en bandeja y Telegram para `tarea.reasignada`. | Corregir contrato. |
| 12 | INFO | RF10: el panel marca rojo desde 48 h exactas; el requisito dice «más de 48 h». RNF2/RNF3 sin evidencia de pruebas a escala real. | Documentar. |

## 3. Cumplimiento de requisitos (entrega 1)
RF1–RF9 y RF11–RF15 cumplen (pantalla y endpoint). RF10: parcial nominal (48 h exactas). No existe RF16. RNF1 cumple (medido). RNF2: acotado cumple, escala real sin medir. RNF3: sin evidencia. RNF4 cumple (bcrypt, costo 12). RNF5: probable (≈4 clics). Restricciones 4a–4e, 5 y 6 (Ley 1581: consentimiento y supresión) cumplen.

## 4. Riesgos aceptados y pendientes documentados
- Suplantación al registrarse con un correo habilitado aún sin cuenta (hallazgo 1): mitigada solo con límite y mensaje uniforme; la solución completa (código de invitación de un solo uso) queda como trabajo futuro.
- Ítems del hallazgo 8, 9 y 12.
