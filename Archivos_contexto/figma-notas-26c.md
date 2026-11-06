# Notas de Figma para el bloque 26c (extraídas, 2026-11-05)

Archivo `w8BC6jwbpzPg8XMvKu1EQ4`. El arquitecto (subagente) no tiene las herramientas MCP de Figma ni puede renderizar el PDF de la entrega 1, así que esta hoja es la lista de lo que hace falta. Formato igual a `figma-notas-26b.md`: valores literales (px, hex, textos exactos con mayúsculas y tildes), por nodo. Lo que ya está en `figma-notas-26b.md` (barra superior, encabezado de página, tarjeta, campo, botones primario y secundario, insignias de estado y alerta) no se repite: solo anota si P04/P05/P06 lo cambian.

Ya conocido de P04 (26b): barra del estudiante `Tablero` (activo) · `Mis tareas` · `Ruta crítica`; usuario `Juan Benavides` / `Líder de equipo`. El tema (bloque 25) ya tomó de P04: tabla (`--color-borde-tabla`, encabezado #f0f2f5 / 12 px #6b7380), barra de avance (#285cdb, éxito #1a8c4d, pista #f0f2f5, alto 6, radio 3) e insignia crítica (#f0e8fc / #7340bf).

## P04 Tablero (nodo 6:2)
1. Título y subtítulo exactos de la página; botones del encabezado (texto, variante, orden). ¿Hay `Nueva tarea`? ¿`Ver ruta crítica`? ¿`Enviar hito`?
2. ¿Hay selector o pestañas de hito? Cómo se ve (texto, posición) y qué muestra del hito (nombre, fecha límite, estado).
3. Disposición del contenido: ¿tabla de tareas, columnas tipo kanban (por estado) o tarjetas? Anchos de columnas o de paneles, separaciones.
4. Si es tabla: encabezados exactos en orden y anchos; qué va en cada celda (título, encargado, estado, avance con barra y %, horas, fecha límite, riesgo, "crítica", "bloqueada", prerrequisitos). Si es kanban: títulos de columna, contador por columna, contenido de cada tarjeta de tarea.
5. Textos e insignias de estado de tarea (`Pendiente` / `En progreso` / `Completada`), de riesgo (`Bajo`/`Medio`/`Alto` u otro texto) y de ruta crítica/bloqueo; colores de cada una.
6. Barra de avance: ancho, si muestra el % al lado y con qué tamaño/color; cuándo usa el verde.
7. ¿Hay paneles laterales (carga del equipo por integrante, alertas, solicitudes de apoyo, resumen)? Título, contenido y medidas.
8. ¿La fila o tarjeta de una tarea es un enlace (a P07 detalle)? ¿Algún resaltado de fila (crítica, bloqueada, en riesgo)?
9. Notas o leyendas al pie (texto exacto).

## P05 Nueva tarea (nodo 6:200)
1. ¿Página propia o modal/panel sobre el tablero? Ancho, fondo de la capa si es modal, radio, relleno.
2. Título y subtítulo exactos.
3. Campos en orden, con etiqueta exacta, tipo de control (texto, área, número, fecha, desplegable, botones de opción, casillas) y ancho: título, descripción, encargado, horas estimadas, fecha límite, "prerrequisito crítico", dependencias.
4. Dependencias: ¿lista de casillas con las tareas del hito, buscador, chips? Qué muestra cada opción (título, encargado, estado).
5. Encargado: ¿desplegable o botones? (importa para RNF5: un desplegable nativo cuesta 2 clics).
6. Textos de ayuda o notas (exactos), p. ej. sobre la ruta crítica o el bloqueo.
7. Botones (texto, variante, orden, posición).

## P06 Ruta crítica (nodo 6:273)
1. Título y subtítulo exactos; botones del encabezado.
2. Tipo de gráfico: ¿red de nodos con flechas (columnas por nivel), diagrama de Gantt (barras en el tiempo) u otro? Captura o descripción de la geometría: tamaño de nodo/barra, separación horizontal y vertical, grosor y color de flechas o líneas, eje o escala (horas, días, fechas) y sus marcas.
3. Contenido de cada nodo o barra (título, horas, holgura, avance, encargado) y estilos: tarea crítica vs. no crítica (colores de fondo, borde, texto), completada, bloqueada.
4. Leyenda (textos y muestras de color).
5. Paneles o tarjetas adicionales (duración total, lista de tareas críticas, holguras, tabla de tiempos ES/EF/LS/LF): títulos, columnas y medidas.
6. Nota al pie (texto exacto).

## Valores extraídos (Figma, 2026-11-05; extraídos por la sesión principal con get_design_context)
Convenciones comunes: página = barra marino (#162850, px 32 py 14) + contenido (px 32 py 28, gap 20, fondo #f6f8fb). Encabezado de página: título Bold 26 #161b26, subtítulo Regular 14 #6b7380, gap 4; botones a la derecha gap 10. Botón secundario: blanco, borde #dbdee5, radio 8, px 18 py 10, SemiBold 14 #161b26. Primario: #285cdb, texto blanco. Insignia: px 10 py 4, radio 999, Medium 12. Tarjeta: blanca, borde #dbdee5, radio 12.
Barra superior: en P04 «Tablero» activo (SemiBold blanco); en P06 «Ruta crítica» activo; «Mis tareas» y los inactivos Regular #c7d6f7. «Notificaciones» + círculo rojo #cc2626 con «3» (Bold 11 blanco, px 7 py 2). Usuario «Juan Benavides» / «Líder de equipo» (ya cubierto en 26b).

### P04 Tablero (6:2)
1. Título «Tablero del equipo»; subtítulo «Equipo 1 · Alfa · Diseño de Software I». Botones en orden: «Ver ruta crítica» (secundario), «Nueva tarea» (secundario), «Enviar a revisión» (primario).
2. Pestañas de hitos (gap 28, sin fondo): «Hito 1 · Análisis» + insignia éxito (#e5f7eb/#1a8c4d) «Nota definitiva»; «Hito 2 · Diseño» ACTIVA (SemiBold 15 #285cdb, borde inferior 2 #285cdb); «Hito 3 · Implementación». Inactivas Regular 15 #6b7380, py 10.
3. Resumen del hito (tarjeta radio 12, p 16, gap 32, una fila): «Estado del hito:» (Medium 14) + insignia «En progreso» (#e8f0ff/#285cdb); «Fecha límite vigente:» Medium + «26/09/2026» Regular 14; «Avance del hito:» + barra 160×6 (pista #f0f2f5, relleno #285cdb, radio 3) + «48 %» (Medium 13).
4. Leyenda «Marcas:» (Medium 13 #6b7380) + insignias: «Ruta crítica» (#e8f0ff/#285cdb), «Prerrequisito crítico» (#f0e8fc/#7340bf), «Bloqueada por dependencia» (#f0f2f5/#737885), «Riesgo alto» (#fcebeb/#cc2626), «Riesgo medio» (#fff2d9/#b87305), «Riesgo bajo» (#e5f7eb/#1a8c4d). Gap 8.
5. Tabla (borde #dbdee5, radio 10, ancho 1376): encabezado #f0f2f5, px 16 py 10, SemiBold 12 #6b7380; columnas y anchos: ID 60 · Tarea 300 · Encargado 170 · Estado 150 · Avance 170 · Fecha límite 130 · Marcas 364 (resto). Filas: borde superior #dbdee5, px 16 py 12, texto Regular 14 #161b26. Estado = insignia: «Completada» (#e5f7eb/#1a8c4d), «En progreso» (#e8f0ff/#285cdb), «Pendiente» (#f0f2f5/#737885). Avance = barra 100×6 + % Medium 13 (relleno verde #1a8c4d al 100 %, azul si no; 0 % muestra 1 px). Fecha dd/mm/aaaa. Marcas: flex-wrap gap 6; orden mostrado: Bloqueada por dependencia, Ruta crítica, Prerrequisito crítico, Riesgo.
6. Filas de ejemplo: T1 Diagrama de casos de uso / Samuel Lambertino / Completada 100 % / 18/09/2026 / Ruta crítica+Prerrequisito crítico; T2 Diagramas de secuencia / Juan Benavides / En progreso 60 % / 23/09/2026 / Ruta crítica+Prerrequisito crítico+Riesgo alto; T3 Modelo relacional / Santy Baza / En progreso 80 % / 22/09/2026 / Riesgo bajo; T4 Prototipos en Figma / Santy Baza / Pendiente 0 % / 25/09/2026 / Bloqueada+Ruta crítica+Prerrequisito+Riesgo medio; T5 Diapositivas de la exposición / Samuel Lambertino / Pendiente 0 % / 26/09/2026 / Bloqueada+Ruta crítica.
7. No hay paneles laterales ni kanban. Nota al pie (fondo #fff2d9, radio 8, p 12): título «Nota del prototipo» SemiBold 12 #b87305; texto 13 #161b26: «Los botones Nueva tarea y Ver ruta crítica solo los ve el líder. Enviar a revisión lo ve cualquier integrante. El monitor ve este tablero en modo lectura. Al hacer clic en una tarea se abre su detalle (P07).» (La nota es del prototipo: NO se implementa como texto en pantalla; sí sus reglas de visibilidad.)

### P05 Nueva tarea (6:200) — ES MODAL sobre el tablero
1. Capa: rgba(23,28,41,0.55) a pantalla completa, contenido centrado, min-alto 836. Modal: blanco, borde #dbdee5, radio 12, p 28, gap 16, ancho 560 (campos 504).
2. Título «Nueva tarea» Bold 22; subtítulo «Hito 2 · Diseño · Equipo 1 · Alfa» Regular 14 #6b7380.
3. Campos (etiqueta Medium 13 #161b26, gap 6; control blanco borde #dbdee5 radio 8 px 12 py 10, texto 14): «Título» (texto); «Descripción» (área, alto 70); «Encargado» (desplegable con «▾» #6b7380; en el ejemplo «Santy Baza»); en una fila gap 20 dos campos de 242: «Horas estimadas» (número) y «Fecha límite» (selector de fecha con «▾»); casilla «Prerrequisito crítico del hito» (cuadro 18 #285cdb radio 4, gap 10); «Bloqueada por» + insignia aviso con el número de predecesoras; selector múltiple (borde #285cdb, radio 8, px 10 py 8, gap 8, wrap) con chips azules «T2 · Diagramas de secuencia  ×» y «T3 · Modelo relacional  ×» (#e8f0ff/#285cdb) + «▾».
4. Botones a la derecha gap 10: «Cancelar» (secundario) y «Guardar» (primario).
5. Los numeritos 2/3/4 en insignia aviso (#fff2d9/#b87305) son anotaciones del prototipo de los clics del RNF 5: NO se implementan. Tarjeta lateral «Recorrido del RNF 5 (4 clics)» (fondo #fff2d9, 320 de ancho): también del prototipo, NO se implementa. Recorrido: 1 «Nueva tarea» en el tablero; 2 clic en «Bloqueada por»; 3 clic en la tarea predecesora; 4 «Guardar» (escribir no cuenta). Implica encargado y fecha ya con valor por defecto (o no obligatorios) y selector de predecesoras propio (no <select> nativo) con lista abierta en un clic.

### P06 Ruta crítica (6:273)
1. Título «Ruta crítica · Hito 2 · Diseño»; subtítulo «Equipo 1 · Alfa · se recalcula al cambiar dependencias, estimaciones o fechas». Un botón secundario «Volver al tablero».
2. Tres tarjetas resumen en fila gap 20 (445 de ancho, p 20, gap 4): etiqueta Medium 13 #6b7380 + valor Bold 26: «Duración de la ruta crítica» «44 h» (#285cdb); «Fecha límite vigente» «26/09/2026» (#161b26); «Tareas en la ruta crítica» «4 de 5».
3. Tarjeta «Grafo de dependencias» (SemiBold 16; p 20, gap 14, ancho 1376): lienzo #f6f8fb, alto 420, radio 8 (ancho 1336). RED DE NODOS con flechas (no Gantt), columnas por nivel: x=60, 380, 700, 1020 (paso 320), filas y=50, 170, 290 (paso 120). Nodo: ancho 212+2·14 de relleno = 240, p 14, gap 4, radio 10; título SemiBold 14, detalle Regular 13 #6b7380 «16 h · holgura 0 h». Crítico: fondo #e8f0ff, borde 2 #285cdb, título #285cdb. Con holgura: blanco, borde 1 #dbdee5, título #161b26. Flechas desde el borde derecho de la predecesora (x nodo+240) al borde izquierdo del sucesor (80 de separación horizontal), con punta; las que cambian de fila suben/bajan (curva o codo). Datos: T1 «Casos de uso» 16 h h0 (60,170); T2 «Diagramas de secuencia» 12 h h0 (380,50); T3 «Modelo relacional» 8 h holgura 4 h (380,290, no crítico); T4 «Prototipos en Figma» 10 h h0 (700,170); T5 «Diapositivas» 6 h h0 (1020,170). Aristas T1→T2, T1→T3, T2→T4, T3→T4, T4→T5. (44 h = 16+12+10+6.)
4. Leyenda (gap 24): cuadro 14 #285cdb radio 3 «Ruta crítica (holgura 0 h)»; cuadro 14 blanco con borde 1,5 #737885 «Tarea con holgura» (Regular 13 #161b26).
5. Nota del prototipo (no se implementa como texto): flecha de predecesora a dependiente; datos coinciden con el tablero; el cálculo debe tardar ≤ 2 s para 150 tareas (RNF 1).
