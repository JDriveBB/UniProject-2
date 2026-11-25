# Aviso de privacidad y política de tratamiento de datos personales de UniProject

> **BORRADOR. No ha sido revisado ni validado por un abogado.** Debe ser revisado por el equipo antes de publicarse. Los textos `[POR DEFINIR: …]` deben completarse o eliminarse antes de mostrar esta página.

Este aviso se rige por la Ley 1581 de 2012 y el Decreto 1377 de 2013 (Colombia). Vigencia: `[POR DEFINIR: fecha de entrada en vigencia]`.

## Resumen

- Guardamos tu nombre, correo institucional, rol y lo que haces en los proyectos (avances, comentarios, evidencias).
- Los usamos solo para gestionar proyectos por hitos, registrar notas y enviarte avisos.
- Si vinculas Telegram, algunos avisos se envían por ese servicio. No compartimos tus datos con ningún otro tercero.
- Puedes conocer, actualizar, rectificar y suprimir tus datos, y revocar tu autorización.
- No tratamos datos sensibles.

## 1. Responsable del tratamiento

- Responsable: el equipo de desarrollo de UniProject, integrado por Juan Benavides, Samuel Lambertino y Santy Baza. La universidad no es la responsable del tratamiento.
- Documento de identidad o NIT: 0.000.000.000 (PROVISIONAL: reemplazar antes del lanzamiento)
- Dirección: Calle 00 # 00-00, Colombia (PROVISIONAL: reemplazar antes del lanzamiento)
- Canal de atención: el perfil de tu cuenta dentro de UniProject (ver sección 7).

## 2. Datos que tratamos

- **Cuenta:** nombre, correo institucional, contraseña (solo se guarda un hash bcrypt, nunca la contraseña), rol en la plataforma (administrador, docente, estudiante o monitor), fecha de registro y fecha en que autorizaste este tratamiento.
- **Telegram (opcional):** el identificador del chat privado que vinculas con tu cuenta.
- **Actividad en proyectos:** pertenencia a equipos, tareas asignadas, registros de avance con su observación, comentarios, solicitudes de corrección, envíos de hitos y las evidencias que subes (archivos PDF, imágenes o enlaces).
- **Evaluación:** notas grupales e individuales y retroalimentación del docente.
- **Notificaciones:** los avisos que recibes en la bandeja de la aplicación.
- **Auditoría:** un registro de acciones (quién, qué, cuándo, valor anterior y nuevo). No incluye contraseñas, tokens, códigos ni identificadores de Telegram.

## 3. Finalidades

Usamos tus datos para: crear y mantener tu cuenta, gestionar proyectos, equipos, tareas y hitos, calcular riesgos y alertas de inactividad, registrar evaluaciones y notas, enviarte avisos y conservar la trazabilidad de los cambios. No los usamos para publicidad ni para elaborar perfiles comerciales.

## 4. Con quién se comparten

- **Telegram:** solo si vinculas tu cuenta. Los mensajes llevan texto de aviso, por ejemplo: nombre de una persona del equipo, título de una tarea, nombre del hito o del proyecto, porcentaje de avance, fechas, y si la nota de un hito es provisional o definitiva (no incluye el valor de la nota ni tu correo). Telegram los trata bajo sus propias condiciones.
- **Dentro de la plataforma:** docentes, líderes y compañeros ven los datos que sus permisos les permiten.
- No usamos analítica, publicidad ni redes de distribución de contenido de terceros. No transferimos datos a otros terceros.

## 5. Dónde se almacenan y por cuánto tiempo

Los datos viven en una base de datos PostgreSQL y las evidencias en un volumen de archivos local del servidor de la aplicación. Los datos de la cuenta (nombre, correo, contraseña y Telegram) se conservan mientras la cuenta esté activa. Si solicitas eliminarla, se anonimizan. Los registros académicos (notas, tareas, entregas y avances), ya anonimizados, se conservan sin plazo para mantener la trazabilidad de la evaluación.

## 6. Medidas de seguridad

- Contraseñas protegidas con bcrypt (costo 12).
- Sesión con cookie y protección contra falsificación de solicitudes (CSRF).
- Las evidencias se descargan únicamente a través de la aplicación, con verificación de permisos.
- El registro de auditoría es de solo inserción: nadie puede modificarlo ni borrarlo desde la aplicación.

Ninguna medida elimina todo riesgo.

## 7. Derechos del titular

Tienes derecho a: conocer, actualizar y rectificar tus datos; solicitar prueba de la autorización; ser informado del uso que se les da; suprimirlos y revocar la autorización cuando no exista un deber legal o contractual de conservarlos; y presentar quejas ante la Superintendencia de Industria y Comercio (SIC) por infracciones a la ley.

Los ejerces únicamente dentro del sistema, desde tu perfil: puedes ver tus datos, actualizar tu nombre, vincular o desvincular Telegram y eliminar tu cuenta, lo que equivale a revocar la autorización. Al eliminarla, tus datos personales se anonimizan; el registro de auditoría conserva solo identificadores internos.

El sistema atiende desde el perfil, de forma inmediata, lo que permite hacer (ver datos, rectificar el nombre, eliminar la cuenta, cambiar la contraseña, vincular o desvincular Telegram). Cualquier otra consulta o reclamo se tramita por el mismo canal del sistema dentro de los plazos legales: las consultas, en 10 días hábiles prorrogables por 5 más; los reclamos, en 15 días hábiles prorrogables por 8 más. Si un reclamo llega incompleto, tienes 5 días para completarlo.

Si consideras que se vulneraron tus derechos, y solo después de haber presentado el reclamo ante el responsable (art. 16 de la Ley 1581 de 2012), puedes presentar una queja ante la Superintendencia de Industria y Comercio (SIC) mediante el trámite «Quejas Datos Personales» de sus servicios en línea.

## 8. Datos sensibles y menores de edad

No recopilamos datos sensibles (salud, origen étnico, orientación política, datos biométricos, etc.). Si alguna vez se pidieran, serían facultativos y no estarías obligado a entregarlos.

La plataforma puede ser usada por niñas, niños y adolescentes. El art. 7 de la Ley 1581 de 2012 proscribe el tratamiento de datos de menores, salvo los de naturaleza pública; la Corte Constitucional (sentencia C-748 de 2011) lo declaró exequible de forma condicionada: el tratamiento es posible si responde y respeta el interés superior del menor y sus derechos fundamentales. Según el art. 12 del Decreto 1377 de 2013 (compilado en el art. 2.2.2.25.2.9 del Decreto 1074 de 2015), cuando el titular es menor de 18 años la autorización la da su representante legal, después de escuchar al menor según su madurez y autonomía. Solo tratamos los datos de menores que sean necesarios para las finalidades de la sección 3. `[POR DEFINIR: mecanismo de autorización del representante legal, depende del diseño del registro]`

## 9. Autorización

Al marcar la casilla «Autorizo el tratamiento de mis datos personales conforme a la Ley 1581 de 2012» en el registro, aceptas este aviso. Marcar la casilla es una conducta inequívoca de autorización (art. 7 del Decreto 1377 de 2013) y conservamos prueba de ella (art. 8): guardamos la fecha de tu autorización y, una vez implementado, la versión de este aviso que aceptaste. Puedes revocarla en cualquier momento; en ese caso no podremos mantener tu cuenta activa.

## 10. Vigencia y cambios

Este aviso rige desde `[POR DEFINIR: fecha]`. Si lo modificamos de forma sustancial, lo comunicaremos por la aplicación y, si tienes Telegram vinculado, por ese medio. La versión vigente estará siempre en esta página.

---

## Para el usuario (borrar antes de publicar)

Marcadores pendientes: fecha de vigencia (encabezado y sección 10) y mecanismo de autorización del representante legal de menores (sección 8). El documento y la dirección del responsable son ficticios y deben reemplazarse antes del lanzamiento.

Pendientes adicionales:
- El art. 16 exige un canal de consultas y reclamos: decidir si el perfil tendrá un formulario de «consulta o reclamo» (no existe aún).
- Registro Nacional de Bases de Datos: según la SIC (art. 2.2.2.26.1.2 del Decreto 1074 de 2015, modificado por el Decreto 090 de 2018) solo están obligadas las sociedades y entidades sin ánimo de lucro con activos totales superiores a 100.000 UVT y las personas jurídicas de naturaleza pública; un equipo de personas naturales sin personería no estaría obligado. Confirmar el umbral vigente en 2026 antes del lanzamiento.
- Guardar la versión del aviso aceptada junto con la fecha de autorización (no implementado).

Publicar solo cuando existan el bloque 27 (eliminación de cuenta por anonimización), el botón del perfil (26f: ver datos, actualizar nombre, vincular o desvincular Telegram, eliminar cuenta) y el mecanismo de menores. Hasta entonces, las secciones 5, 7 y 8 describirían algo que el sistema aún no hace.

**Nota sobre las fuentes.** Las citas de esta página se contrastaron con Función Pública y la SIC (consulta del 2026-11-04). No se verificaron en texto oficial: la numeración 2.2.2.25.2.4, 2.2.2.25.2.5 y 2.2.2.25.3.1 del Decreto 1074 de 2015, el texto de los arts. 17 y 18 de la Ley 1581 y la vigencia del umbral del Registro Nacional de Bases de Datos. El Gestor Normativo indica además una derogación parcial del Decreto 1377 por el Decreto 1081 de 2015. Un abogado debe revisar el texto antes de publicarlo.
