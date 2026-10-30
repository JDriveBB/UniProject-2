# Diagrama de base de datos vigente (19 tablas)

Fuente: `UniProject 1-entrega/actualizaciones de entrega/Diagrama_BD_UniProject.svg` (generado con Graphviz, modificado el 2026-10-29 23:33). El usuario lo indicó el 2026-10-30 como el diagrama más actual; extraído como texto. Nombres de tabla del diagrama; en PostgreSQL van en minúscula (`Evento_Saliente` → `evento_saliente`, regla K1). PK = clave primaria, FK = clave foránea. El SVG no indica los tipos de dato ni a qué tabla apunta cada FK: se deducen del nombre y deben fijarse en la migración.

| Tabla | Columnas |
|---|---|
| Usuario | id_usuario PK · nombre · email · password_hash · rol_plataforma · autoriza_tratamiento_datos · fecha_autorizacion · fecha_registro |
| Proyecto | id_proyecto PK · id_docente FK · nombre · descripcion · fecha_inicio · fecha_fin · usa_monitores |
| Monitor_Proyecto | id_proyecto PK,FK · id_monitor PK,FK |
| Equipo | id_equipo PK · id_proyecto FK · nombre_equipo |
| Miembro_Equipo | id_equipo PK,FK · id_usuario PK,FK · rol_equipo · fecha_ingreso |
| Hito | id_hito PK · id_proyecto FK · nombre_hito · descripcion · orden · fecha_inicio · fecha_limite · dias_inactividad_alerta · notificar_docente_inactividad |
| Criterio_Rubrica | id_criterio PK · id_hito FK · descripcion · peso_porcentaje · puntaje_maximo |
| Hito_Equipo | id_hito_equipo PK · id_hito FK · id_equipo FK · estado |
| Envio_Hito | id_envio PK · id_hito_equipo FK · id_usuario_envia FK · fecha_envio |
| Tarea | id_tarea PK · id_hito_equipo FK · id_encargado FK · id_tarea_origen FK · titulo · descripcion · estado · fecha_cambio_estado · bloqueada_por_dependencia · fecha_bloqueo · edicion_habilitada · porcentaje_avance · horas_estimadas · fecha_limite · fecha_completada · es_prerrequisito_critico · en_ruta_critica · holgura_horas · nivel_riesgo · peso |
| Dependencia_Tarea | id_tarea_predecesora PK,FK · id_tarea_dependiente PK,FK |
| Registro_Avance | id_registro PK · id_tarea FK · id_usuario FK · fecha_hora · porcentaje_avance · observacion |
| Evidencia | id_evidencia PK · id_tarea FK · id_usuario FK · tipo_evidencia · url · nombre_archivo · fecha_subida |
| Comentario_Tarea | id_comentario PK · id_tarea FK · id_autor FK · contenido · fecha_hora |
| Solicitud_Correccion | id_solicitud PK · id_tarea FK · id_evidencia FK · id_lider FK · id_estudiante FK · observaciones · estado · fecha_solicitud · fecha_atencion |
| Evaluacion_Hito | id_evaluacion PK · id_envio FK · id_docente FK · nota_grupal · retroalimentacion_general · es_definitiva · fecha_evaluacion |
| Calificacion_Criterio | id_evaluacion PK,FK · id_criterio PK,FK · nota · retroalimentacion |
| Nota_Individual | id_nota_individual PK · id_evaluacion FK · id_estudiante FK · factor_ajuste · nota_sugerida · nota_final · fecha_confirmacion |
| Notificacion | id_notificacion PK · id_destinatario FK · tipo · mensaje · entidad_referencia · id_referencia · fecha_creacion · estado_envio · leida |
| Log_Auditoria | id_log PK · id_usuario_accion FK · entidad_afectada · id_registro_afectado · accion · campo_modificado · valor_anterior · valor_nuevo · fecha_hora |

## Diferencias conocidas con lo ya implementado (notificaciones)
- `Usuario`: el diagrama no tiene `telegram_chat_id`; la migración V1 del núcleo ya lo agrega (y la PK `id_usuario` coincide).
- Tablas de notificaciones que NO están en el diagrama y ya existen en código: `evento_saliente`, `codigo_vinculacion`, `estado_riesgo_tarea`, `alerta_inactividad` (V1/V2 del núcleo) y `agente.evento_procesado` (esquema `agente`). Se añaden al diagrama al final (tarea del `documentador`).
- `Notificacion` (bandeja interna, prototipo P10) existe en el diagrama y todavía no en código.
- Alta de cuentas por lista del docente (decisión K2, 2026-10-30): requiere una tabla nueva que el diagrama no tiene (correos institucionales habilitados por proyecto). Es una adición a este diagrama.
