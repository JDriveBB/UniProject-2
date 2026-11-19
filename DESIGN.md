---
name: UniProject
description: Gestión de proyectos académicos grupales por hitos, para docentes y estudiantes.
colors:
  primario: "#285cdb"
  primario-hover: "#1f4bb8"
  texto-sobre-primario: "#ffffff"
  marca-fondo: "#162850"
  texto-sobre-marca: "#ffffff"
  texto-sobre-marca-secundario: "#c7d6f7"
  fondo: "#f6f8fb"
  superficie: "#ffffff"
  secundario-hover: "#f0f2f5"
  borde: "#d1d6de"
  borde-tabla: "#dbdee5"
  texto: "#161b26"
  texto-secundario: "#5f6673"
  tabla-encabezado-fondo: "#f0f2f5"
  tabla-encabezado-texto: "#5f6673"
  exito: "#177d45"
  exito-fondo: "#e5f7eb"
  aviso: "#9b6104"
  aviso-fondo: "#fff2d9"
  peligro: "#cc2626"
  peligro-hover: "#a81f1f"
  peligro-fondo: "#fcebeb"
  info: "#285cdb"
  info-fondo: "#e8f0ff"
  neutro: "#696d79"
  neutro-fondo: "#f0f2f5"
  critico: "#7340bf"
  critico-fondo: "#f0e8fc"
  avance: "#285cdb"
  avance-exito: "#1a8c4d"
  avance-pista: "#f0f2f5"
  contador-fondo: "#cc2626"
  texto-sobre-contador: "#ffffff"
  capa-modal: "rgba(23, 28, 41, 0.55)"
typography:
  marca-ingreso:
    fontFamily: "'Inter Variable', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
    fontSize: "2.75rem"
    fontWeight: 700
    lineHeight: 1.1
  titulo-pagina:
    fontFamily: "'Inter Variable', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
    fontSize: "1.75rem"
    fontWeight: 700
    lineHeight: 1.2
  titulo-encabezado:
    fontFamily: "'Inter Variable', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
    fontSize: "1.625rem"
    fontWeight: 700
    lineHeight: 1.2
  titulo-modal:
    fontFamily: "'Inter Variable', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
    fontSize: "1.375rem"
    fontWeight: 700
    lineHeight: 1.2
  titulo:
    fontFamily: "'Inter Variable', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
    fontSize: "1.25rem"
    fontWeight: 600
  titulo-tarjeta:
    fontFamily: "'Inter Variable', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
    fontSize: "1rem"
    fontWeight: 600
  base:
    fontFamily: "'Inter Variable', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.5
  pequeno:
    fontFamily: "'Inter Variable', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 400
  etiqueta:
    fontFamily: "'Inter Variable', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 500
  boton:
    fontFamily: "'Inter Variable', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 600
  tabla-encabezado:
    fontFamily: "'Inter Variable', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 600
  insignia:
    fontFamily: "'Inter Variable', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 500
  codigo:
    fontFamily: "ui-monospace, 'Cascadia Mono', Consolas, monospace"
    fontSize: "0.875rem"
    fontWeight: 400
rounded:
  radio-muestra: "3px"
  radio-casilla: "4px"
  radio: "10px"
  radio-tabla: "12px"
  radio-tarjeta: "14px"
  radio-modal: "16px"
  radio-tarjeta-ingreso: "16px"
  radio-insignia: "999px"
  radio-avance: "999px"
spacing:
  espacio-1: "4px"
  espacio-campo: "6px"
  espacio-2: "8px"
  separacion-botones: "10px"
  espacio-3: "12px"
  espacio-tarjeta: "14px"
  espacio-4: "16px"
  espacio-5: "20px"
  espacio-6: "24px"
  separacion-pestanas: "28px"
  espacio-8: "32px"
  espacio-10: "40px"
  separacion-datos-tarea: "48px"
  espacio-20: "80px"
components:
  boton-primario:
    backgroundColor: "{colors.primario}"
    textColor: "{colors.texto-sobre-primario}"
    typography: "{typography.boton}"
    rounded: "{rounded.radio}"
    padding: "10px 18px"
  boton-primario-hover:
    backgroundColor: "{colors.primario-hover}"
    textColor: "{colors.texto-sobre-primario}"
  boton-secundario:
    backgroundColor: "{colors.superficie}"
    textColor: "{colors.texto}"
    typography: "{typography.boton}"
    rounded: "{rounded.radio}"
    padding: "10px 18px"
  boton-secundario-hover:
    backgroundColor: "{colors.secundario-hover}"
    textColor: "{colors.texto}"
  boton-peligro:
    backgroundColor: "{colors.peligro}"
    textColor: "{colors.texto-sobre-primario}"
    typography: "{typography.boton}"
    rounded: "{rounded.radio}"
    padding: "10px 18px"
  boton-peligro-hover:
    backgroundColor: "{colors.peligro-hover}"
    textColor: "{colors.texto-sobre-primario}"
  boton-suave:
    backgroundColor: "{colors.info-fondo}"
    textColor: "{colors.primario}"
    typography: "{typography.boton}"
    rounded: "{rounded.radio}"
    padding: "10px 18px"
  boton-acceso:
    backgroundColor: "{colors.primario}"
    textColor: "{colors.texto-sobre-primario}"
    rounded: "{rounded.radio}"
    padding: "14px 18px"
    width: "100%"
  boton-texto:
    textColor: "{colors.primario}"
    typography: "{typography.pequeno}"
  campo:
    backgroundColor: "{colors.superficie}"
    textColor: "{colors.texto}"
    typography: "{typography.base}"
    rounded: "{rounded.radio}"
    padding: "12px 14px"
  campo-marco:
    backgroundColor: "{colors.superficie}"
    textColor: "{colors.texto}"
    typography: "{typography.base}"
    rounded: "{rounded.radio}"
    padding: "10px 12px"
  tarjeta:
    backgroundColor: "{colors.superficie}"
    rounded: "{rounded.radio-tarjeta}"
    padding: "20px"
  tarjeta-acceso:
    backgroundColor: "{colors.superficie}"
    rounded: "{rounded.radio-tarjeta-ingreso}"
    padding: "40px"
  tabla-encabezado:
    backgroundColor: "{colors.tabla-encabezado-fondo}"
    textColor: "{colors.tabla-encabezado-texto}"
    typography: "{typography.tabla-encabezado}"
    padding: "12px 16px"
  tabla-celda:
    backgroundColor: "{colors.superficie}"
    textColor: "{colors.texto}"
    typography: "{typography.base}"
    padding: "12px 16px"
  insignia-neutro:
    backgroundColor: "{colors.neutro-fondo}"
    textColor: "{colors.neutro}"
    typography: "{typography.insignia}"
    rounded: "{rounded.radio-insignia}"
    padding: "4px 10px"
  insignia-exito:
    backgroundColor: "{colors.exito-fondo}"
    textColor: "{colors.exito}"
    typography: "{typography.insignia}"
    rounded: "{rounded.radio-insignia}"
    padding: "4px 10px"
  insignia-aviso:
    backgroundColor: "{colors.aviso-fondo}"
    textColor: "{colors.aviso}"
    typography: "{typography.insignia}"
    rounded: "{rounded.radio-insignia}"
    padding: "4px 10px"
  insignia-peligro:
    backgroundColor: "{colors.peligro-fondo}"
    textColor: "{colors.peligro}"
    typography: "{typography.insignia}"
    rounded: "{rounded.radio-insignia}"
    padding: "4px 10px"
  insignia-info:
    backgroundColor: "{colors.info-fondo}"
    textColor: "{colors.info}"
    typography: "{typography.insignia}"
    rounded: "{rounded.radio-insignia}"
    padding: "4px 10px"
  insignia-critico:
    backgroundColor: "{colors.critico-fondo}"
    textColor: "{colors.critico}"
    typography: "{typography.insignia}"
    rounded: "{rounded.radio-insignia}"
    padding: "4px 10px"
  alerta-error:
    backgroundColor: "{colors.peligro-fondo}"
    textColor: "{colors.peligro}"
    rounded: "{rounded.radio}"
    padding: "12px 16px"
  alerta-exito:
    backgroundColor: "{colors.exito-fondo}"
    textColor: "{colors.exito}"
    rounded: "{rounded.radio}"
    padding: "12px 16px"
  alerta-aviso:
    backgroundColor: "{colors.aviso-fondo}"
    textColor: "{colors.aviso}"
    rounded: "{rounded.radio}"
    padding: "12px 16px"
  alerta-info:
    backgroundColor: "{colors.info-fondo}"
    textColor: "{colors.info}"
    rounded: "{rounded.radio}"
    padding: "12px 16px"
  barra:
    backgroundColor: "{colors.marca-fondo}"
    textColor: "{colors.texto-sobre-marca}"
    padding: "14px 32px"
  barra-contador:
    backgroundColor: "{colors.contador-fondo}"
    textColor: "{colors.texto-sobre-contador}"
    rounded: "{rounded.radio-insignia}"
    padding: "2px 7px"
  pestana-activa:
    textColor: "{colors.primario}"
    padding: "10px 0"
  modal:
    backgroundColor: "{colors.superficie}"
    textColor: "{colors.texto}"
    rounded: "{rounded.radio-modal}"
    padding: "28px"
    width: "560px"
  avance:
    backgroundColor: "{colors.avance-pista}"
    textColor: "{colors.avance}"
    rounded: "{rounded.radio-avance}"
    height: "6px"
    width: "100px"
---

# Design System: UniProject

## Overview

**Creative North Star: "El cuaderno del curso"**

UniProject es una herramienta de trabajo para docentes y estudiantes que siguen proyectos por hitos. La pantalla se parece a un cuaderno de seguimiento ordenado: fondo gris muy claro, hojas blancas (tarjetas y tablas) con borde fino, un solo azul para lo que se puede hacer y una barra superior azul marino que dice siempre dónde se está. La información es densa (tablas de tareas, notas, historial), así que el sistema prefiere orden y lectura fácil antes que adorno.

El color de estado (éxito, aviso, peligro, información, neutro, crítico) aparece casi solo en insignias y alertas, siempre con texto que dice el estado. Lo que está en riesgo se destaca con color y con palabras, nunca solo con color. Las superficies son planas: la profundidad se da con bordes y con el contraste fondo/superficie, no con sombras.

Paleta y estructura están fijas. Los refinamientos (como P-05) cambian acabado, medidas y comportamiento en celular, pero no cambian colores ni el esquema general de barra superior + contenido centrado.

**Key Characteristics:**
- Un solo azul de acción (`primario`) sobre superficies blancas y fondo gris claro.
- Barra superior y panel de ingreso en azul marino de marca.
- Superficies planas con borde fino; sombra solo en la tarjeta de ingreso.
- Insignias en forma de píldora que nombran el estado con texto.
- Tablas densas que se desplazan dentro de su recuadro, nunca la página.
- Una sola letra, Inter Variable, servida desde el propio sitio.

## Colors

Paleta fría y sobria: un azul de acción, un azul marino de marca, grises neutros y seis pares de estado (texto oscuro sobre fondo pálido).

### Primary
- **Azul de acción** (`primario`): botones principales, enlaces, pestaña activa, casilla marcada, borde del selector abierto y la barra de avance (`avance`). Al pasar el mouse el botón principal baja a **Azul de acción profundo** (`primario-hover`). El texto sobre él es blanco (`texto-sobre-primario`).
- **Azul marino de marca** (`marca-fondo`): solo la barra superior y el panel izquierdo de ingreso y registro. Sobre él va texto blanco (`texto-sobre-marca`) y un azul pálido para lo secundario (`texto-sobre-marca-secundario`): enlaces del menú, rol del usuario, «Salir».

### Neutral
- **Gris papel** (`fondo`): fondo de toda la aplicación; también campos de solo lectura.
- **Blanco hoja** (`superficie`): tarjetas, tablas, modales, campos.
- **Hover secundario** (`secundario-hover`): fondo del botón secundario al pasar el mouse.
- **Borde de campo** (`borde`): borde de campos y casillas en ingreso y registro, y de la tarjeta de ingreso; también borde de botón secundario al pasar el mouse.
- **Borde de tabla** (`borde-tabla`): borde de tarjetas, tablas, filas, modales, pestañas y campos dentro del marco. Es el borde más usado.
- **Tinta** (`texto`): texto principal.
- **Tinta secundaria** (`texto-secundario`): descripciones, ayudas, estados vacíos, pestañas inactivas. Oscurecida a 5:1 o más por accesibilidad.
- **Encabezado de tabla** (`tabla-encabezado-fondo`, `tabla-encabezado-texto`): fila de títulos de las tablas.
- **Capa de modal** (`capa-modal`): velo semitransparente detrás de los diálogos.

### Estados
Cada estado es un par texto + fondo. El texto se oscureció frente a Figma para pasar 4,5:1 sobre su fondo.
- **Éxito** (`exito`, `exito-fondo`): terminado, a tiempo, riesgo bajo.
- **Aviso** (`aviso`, `aviso-fondo`): atención, riesgo medio, pendiente.
- **Peligro** (`peligro`, `peligro-fondo`): error, atrasado, riesgo alto, acciones de borrar o quitar. Al pasar el mouse el botón de peligro baja a `peligro-hover`.
- **Información** (`info`, `info-fondo`): avisos neutros; `info-fondo` también es el fondo del botón suave, de la notificación no leída y de la selección de texto.
- **Neutro** (`neutro`, `neutro-fondo`): sin estado, sin riesgo calculado.
- **Crítico** (`critico`, `critico-fondo`): violeta reservado a «Prerrequisito crítico» y a la ruta crítica.
- **Avance completo** (`avance-exito`) sobre la pista (`avance-pista`): la barra de avance pasa a verde al 100 %.
- **Contador** (`contador-fondo`, `texto-sobre-contador`): número de notificaciones sin leer en la barra superior.

### Named Rules
**The Solo Variables Rule.** Todo color sale de una variable del bloque `:root` de `tema.css`. Nada de hex, `rgb()` ni nombres de color sueltos en las reglas. Si hace falta un color nuevo, primero se agrega la variable.

**The Paleta Fija Rule.** Los refinamientos conservan esta paleta. No se agregan acentos nuevos ni se cambia el tono de los existentes; solo se oscurecen para cumplir contraste, dejando nota del valor anterior.

**The Estado Con Palabras Rule.** El color de estado nunca va solo: toda insignia, alerta o marca dice su estado con texto.

## Typography

**Display Font:** Inter Variable (con Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif)
**Body Font:** Inter Variable (la misma)
**Label/Mono Font:** ui-monospace, Cascadia Mono, Consolas (solo códigos, como el de vinculación con Telegram)

**Character:** Una sola familia sans, legible a tamaño chico y con cifras tabulares en tablas y avances. La jerarquía sale del peso (400 a 700) y de pocos tamaños, no de familias distintas. La letra se sirve desde el propio sitio (`web/src/fuentes/`, pesos 100–900 en un archivo variable).

### Hierarchy
- **Marca de ingreso** (`marca-ingreso`): el nombre «UniProject» en el panel de ingreso. Baja al tamaño de `titulo-pagina` bajo 960 px.
- **Título de acceso** (`titulo-pagina`): título de la tarjeta de ingreso y registro.
- **Título de pantalla** (`titulo-encabezado`): el h1 de cada pantalla dentro del marco. El valor grande de las tarjetas de resumen usa el mismo tamaño y peso con cifras tabulares.
- **Título de modal** (`titulo-modal`): encabezado de diálogos.
- **Título de sección** (`titulo`): subtítulos de sección; la marca de la barra superior usa este tamaño en negrita.
- **Título de tarjeta** (`titulo-tarjeta`): cabecera de tarjetas.
- **Texto base** (`base`): todo el texto corriente, celdas, enlaces del menú. Interlineado 1,5.
- **Texto pequeño** (`pequeno`): ayudas, errores de campo, texto secundario, botones de texto, etiquetas dentro del marco.
- **Etiqueta** (`etiqueta`): etiquetas de campo en ingreso y registro.
- **Botón** (`boton`): texto de botones.
- **Encabezado de tabla** (`tabla-encabezado`) e **insignia** (`insignia`): el tamaño más chico de uso normal.

### Named Rules
**The Una Sola Letra Rule.** Toda la interfaz usa Inter Variable. No se agrega otra familia para títulos.

**The Cifras Alineadas Rule.** Números en tablas, avances y resúmenes van con `font-variant-numeric: tabular-nums`.

## Layout

La aplicación es un marco: barra superior de ancho completo y debajo el contenido centrado, con ancho máximo de 1440 px (`ancho-maximo-contenido`) y relleno de 28 px arriba/abajo y 32 px a los lados (`relleno-contenido`). Cada pantalla es una columna (`pagina`) con separación de 20 px (`espacio-5`) entre bloques; dentro de las secciones, 12 px. Ingreso y registro usan otro esquema: panel de marca de 620 px a la izquierda y tarjeta centrada a la derecha; bajo 960 px pasa a una sola columna.

El ritmo de espacios es de 4 px (`espacio-1` a `espacio-10`, más `espacio-20` en el panel de ingreso), con algunos espacios con nombre propio: 6 px entre etiqueta y campo, 10 px entre botones, 14 px dentro de tarjetas, 28 px entre pestañas.

**Cortes de pantalla:** 600 px (celular), 960 px (columnas a una sola) y 1360 px (tablas anchas ceden columnas). Las variables CSS no valen dentro de `@media`, por eso los cortes van escritos en número.

### Named Rules
**The Sin Desborde Rule.** Ninguna pantalla desborda en horizontal entre 320 y 1440 px. Se revisa siempre a 1440 y a 390 px.

**The Media Después Rule.** Un bloque `@media` va después de las reglas base que cambia. Tienen la misma especificidad, así que gana el que está más abajo; si el `@media` queda antes, no cambia nada.

**The Tabla Que Se Desplaza Rule.** Una tabla con `table-layout: fixed` lleva un `min-width` igual a la suma de sus columnas fijas más un mínimo para la columna flexible (unos 10–12rem; más si la columna lleva insignias, como los 18rem de Marcas en el tablero). Las celdas usan `overflow-wrap: break-word`, nunca `anywhere`. Si no cabe, la tabla se desplaza dentro de `.tabla-contenedor`, que tiene `position: relative` para que el `<caption>` oculto no ensanche la página. Bajo 1360 px las columnas fijas pueden ceder ancho para que la tabla quepa completa a 1280 px.

**The Barra En Dos Líneas Rule.** En celular (600 px o menos) la barra superior tiene dos líneas: arriba la marca, la campana (ícono SVG; la palabra «Notificaciones» sigue para lectores de pantalla), el usuario (cortado con puntos suspensivos, máximo 8rem) y «Salir»; abajo los enlaces del menú. Decisión D-08.

## Elevation & Depth

El sistema es plano. La profundidad se da por capas de tono (fondo gris, superficie blanca) y bordes finos de 1 px. La única sombra en reposo es la de la tarjeta de ingreso, que flota sobre el fondo. Los modales se separan con la capa oscura (`capa-modal`), no con sombra.

### Shadow Vocabulary
- **Sombra de ingreso** (`--sombra`: `0 8px 24px rgba(0, 0, 0, 0.06)`): solo la tarjeta de ingreso y registro.
- **Sombra de enlace** (`--sombra-enlace-hover`: `0 2px 8px rgba(22, 40, 80, 0.08)`): solo al pasar el mouse por una tarjeta de resumen que es enlace (junto con el borde `primario`); en reposo no hay sombra.
- **Anillo de foco** (`--foco`: anillo blanco de 2 px y anillo azul `primario` de 2 px por fuera): todo elemento con foco de teclado.
- **Foco sobre marca** (`--foco-sobre-marca`: anillo blanco de 2 px): foco dentro de la barra superior azul marino.

### Named Rules
**The Plano Por Defecto Rule.** Tarjetas, tablas, modales y campos no llevan sombra. Se separan con borde y tono.

**The Foco Visible Rule.** Todo elemento enfocable muestra el anillo de foco con `:focus-visible`; en modo de alto contraste el contorno pasa a `Highlight`. No se quita el foco sin dar otro igual de visible.

## Shapes

Esquinas suaves y consistentes por tamaño: 10 px (`radio`) para botones, campos, alertas y selectores; 12 px (`radio-tabla`) para el recuadro de tablas; 14 px (`radio-tarjeta`) para tarjetas y resúmenes; 16 px (`radio-modal` y `radio-tarjeta-ingreso`) para modales y la tarjeta de ingreso. Insignias, contador y barra de avance (`radio-avance`) son píldoras (`999px`). Las muestras de leyenda van casi rectas (3 px); la casilla, 4 px. Los bordes son de 1 px; los más gruesos tienen un fin: 2 px en la pestaña activa y en nodos críticos de la ruta, 1,5 px en casillas.

## Movimiento

Transiciones sobrias y rápidas para dar respuesta inmediata sin distraer de la lectura.

### Tokens de movimiento (`:root`)
- `--duracion-presion`: `80ms` — hundimiento de botones al presionar (`:active`).
- `--duracion-rapida`: `120ms` — cambios de color, borde o sombra en botones, campos, enlaces, pestañas y tarjetas de resumen que son enlace.
- `--duracion-media`: `180ms` — entrada de diálogos (`.modal[open]` y `::backdrop`) y cambio de estado de notificaciones (`.notificacion`).
- `--curva-salida`: `cubic-bezier(0.2, 0, 0, 1)` — desaceleración suave para la entrada de diálogos.
- `--desplazamiento-entrada`: `8px` — desplazamiento vertical inicial en la entrada del diálogo.
- `--escala-presion`: `0.98` — leve reducción de escala al presionar un botón habilitado.

### Reglas de movimiento
- **Baseline sin polyfills:** se usan propiedades estándar actuales (`scale`, `translate`, `@starting-style`); si un navegador antiguo no soporta `@starting-style`, el diálogo se abre igual sin animación.
- **Duraciones ≤ 200 ms por variable:** ninguna `transition` usa duraciones literales en `ms` o `s`; todas salen de `:root`.
- **Movimiento reducido (`prefers-reduced-motion`):** todo lo que desplaza o cambia de tamaño (`translate`, `scale`) va solo dentro de `@media (prefers-reduced-motion: no-preference)`. Los cambios de color y opacidad permanecen siempre activos.
- **CSS plano y orden de cascada:** no se usa anidamiento de CSS; `@starting-style` va como regla de primer nivel (incluso dentro de `@media (prefers-reduced-motion: no-preference)`), y cada `@media` va después de las reglas base que modifica.

## Components

### Buttons
Firmes y compactos, con texto en seminegrita.
- **Shape:** esquinas de 10 px (`radio`), borde de 1 px transparente para que todas las variantes midan igual.
- **Primario:** fondo `primario`, texto blanco, relleno 10 × 18 px. Una acción principal por bloque.
- **Hover / Focus / Active:** transición de `duracion-rapida` en color, borde y sombra. El primario pasa a `primario-hover`; el secundario a fondo `secundario-hover` y borde `borde`; el peligro a `peligro-hover`; el foco usa el anillo de foco. Al presionar (`:active:not(:disabled)`), `scale` baja a `escala-presion` en `duracion-presion` solo bajo `prefers-reduced-motion: no-preference`. Deshabilitado: opacidad 0,6 y cursor «no permitido». Mientras carga dice «Procesando…» y queda deshabilitado.
- **Secundario:** fondo blanco, texto `texto`, borde `borde-tabla`.
- **Peligro:** fondo `peligro`, texto blanco; solo para confirmar algo destructivo.
- **Suave:** fondo `info-fondo`, texto `primario`; al pasar el mouse aparece el borde azul.
- **Acceso:** el primario a todo el ancho y más alto (14 × 18 px), solo en ingreso y registro.
- **Botón de texto:** parece enlace (texto pequeño, peso medio, `primario`; versión `peligro` para «Quitar»); se subraya al pasar el mouse.

### Chips (Insignias)
- **Style:** píldora (`radio-insignia`), relleno 4 × 10 px, texto de 12 px en peso medio, sin borde, sin cortes de línea. Texto del estado sobre su fondo pálido: neutro, éxito, aviso, peligro, información, crítico.
- **State:** el texto siempre dice el estado («Riesgo alto», «Prerrequisito crítico»). Cuando la insignia es un enlace, al pasar el mouse muestra un borde interior de su propio color. En el selector de dependencias la insignia lleva un botón «×» para quitarla.
- **Marcas en tablas:** como máximo una marca por fila, en orden de prioridad: «Bloqueada por dependencia» > «Riesgo alto» > «Riesgo medio» (ninguna si la tarea está completada). «Riesgo bajo» no aparece en tablas: solo se muestra en el detalle de la tarea junto al margen en horas de trabajo.

### Cards / Containers
- **Corner Style:** 14 px (`radio-tarjeta`).
- **Background:** `superficie` sobre `fondo`.
- **Shadow Strategy:** ninguna (ver Elevation & Depth).
- **Border:** 1 px `borde-tabla`.
- **Internal Padding:** 20 px; los hijos se separan con `gap` de 14 px y los párrafos no suman margen.
- Las tarjetas de resumen (ruta crítica, panel) siguen la misma forma; cuando son enlace, al pasar el mouse el borde pasa a `primario` con transición de `duracion-rapida`.
- **Franja compacta del panel (`.resumen-panel`):** 4 datos en una fila (2 bajo 960 px, 1 bajo 360 px) con relleno `espacio-4`, enlace a `#atencion` en equipos en atención y por calificar, y la próxima entrega.
- **«Necesitan tu atención» (`.atencion`):** filas sobre `fondo` con insignia de estado, texto descriptivo y botón de acción a la derecha (debajo del texto bajo 600 px).
- **«Entregas» (`.entregas`):** lista ordenada por hito separada por `borde-tabla`, con fecha y plazo coloreados por urgencia, insignia «Próxima» en el siguiente por vencer y estado enlazado por equipo.
- **Tarjeta de proyecto (`.tarjeta-proyecto`):** rejilla `.rejilla-proyectos` en columnas de 320 px sin transición en el borde (las acciones van en botones al fondo con `margin-top: auto`); cabecera con insignia de estado o hito, descripción a 2 líneas, franja de 3 cifras sobre `fondo` (1 columna bajo 360 px), `BarraAvance` y próxima entrega o hito actual.

### Inputs / Fields
- **Style:** fondo blanco, borde de 1 px, esquinas de 10 px. En ingreso y registro: borde `borde`, relleno 12 × 14 px, etiqueta de 14 px. Dentro del marco: borde `borde-tabla`, relleno 10 × 12 px, etiqueta de 13 px. Etiqueta y campo separados 6 px.
- **Hover / Focus:** transición de `duracion-rapida` en borde y sombra; al pasar el mouse sin foco ni deshabilitado, el borde pasa a `texto-secundario`. Al enfocar usa el anillo de foco. Los campos con sufijo («%») muestran el anillo en todo el grupo.
- **Error / Disabled:** `aria-invalid` pone el borde en `peligro` y el mensaje va debajo en texto pequeño `peligro`. Solo lectura: fondo `fondo`.
- **Casilla:** cuadro de 18 px dibujado (el `input` real sigue accesible), marcada en `primario` con visto blanco.
- **Selectores:** tanto `<select class="campo__entrada">` (`appearance: none`) como el selector múltiple («Bloqueada por») usan el mismo chevron SVG de 12 px en `texto-secundario` (`--icono-chevron`), separado 12 px del borde derecho y con relleno derecho de 32 px para que el texto no lo tape.
- **Campo de fichas (`.campo-fichas`):** contenedor con forma de campo que muestra el anillo de foco con `:focus-within`, lista de fichas en píldora (`.ficha` neutra o `.ficha--error` en peligro, con motivo en texto debajo y botón «✕» ≥ 24 px, 44 px en celular) y entrada de correo sin desborde desde 320 px.

### Tables
- Recuadro `.tabla-contenedor`: borde `borde-tabla`, esquinas de 12 px, fondo blanco, `position: relative` y desplazamiento horizontal propio.
- Encabezado: fondo `tabla-encabezado-fondo`, texto de 12 px en seminegrita `tabla-encabezado-texto`, relleno 12 × 16 px.
- Filas: texto base, relleno 12 × 16 px, separador de 1 px `borde-tabla`, la última sin borde; cifras tabulares.
- **Ruta crítica en el tablero:** las filas de la ruta crítica llevan una franja interior violeta (`critico`) de 4 px en el borde izquierdo de la primera celda (y «(ruta crítica)» oculto a la vista para lectores de pantalla). La leyenda de franja y marcas va en un `<details>` cerrado por defecto («¿Qué significan las marcas?»).
- Cada tabla tiene un `<caption>` solo para lectores de pantalla y un texto para cuando no hay filas.
- Ancho y corte de palabras según **The Tabla Que Se Desplaza Rule**.

### Alerts
- Relleno 12 × 16 px, esquinas de 10 px, texto del estado sobre su fondo pálido (error, éxito, aviso, información). El aviso de sesión vencida queda pegado arriba al desplazarse.

### Avisos breves
- Confirmaciones de éxito cuya información ya se refleja en la pantalla; errores, advertencias, evaluaciones con notas e instrucciones («Proyecto creado. Ahora puedes…») permanecen como alertas en la página.
- Contenedor `.avisos` fijo abajo a la derecha con `espacio-5` de margen (abajo al centro con 16 px a cada lado bajo 600 px) y `z-index` por encima del contenido y de la barra; sin `popover`. Existe desde la carga dentro del marco con `role="region"`, `aria-label="Avisos"` y una zona interna `aria-live="polite"`.
- Máximo 3 visibles (si entra otro, sale el más viejo) y textos repetidos seguidos se juntan sin apilarse. No aparecen mientras haya un `<dialog open>` (esperan a que se cierre) y nunca roban el foco al mostrarse.
- Tarjeta `.aviso`: fondo `superficie`, borde `borde-tabla`, franja izquierda `exito`, `radio-tarjeta`, sombra `--sombra`, texto que escala hasta 200 % sin cortarse ni desbordar a 320 px, y botón «Cerrar» enfocable (`aria-label="Cerrar aviso"`). Cierre automático a los 4 s, pausado mientras tenga foco o el mouse encima; al cerrarse con teclado, el foco pasa al siguiente aviso o vuelve a `main`.
- Entrada con `@starting-style` en `duracion-media` y `curva-salida`: solo opacidad por defecto, y desplazamiento (`desplazamiento-entrada`) únicamente bajo `@media (prefers-reduced-motion: no-preference)`.

### Navigation
- **Barra superior:** fondo `marca-fondo`, relleno 14 × 32 px. Marca a la izquierda (20 px, negrita, blanca); enlaces en `texto-sobre-marca-secundario`, que pasan a blanco al pasar el mouse (`duracion-rapida`); el activo en blanco y seminegrita. A la derecha: notificaciones con contador rojo en píldora, nombre y rol del usuario, y «Salir». En celular, ver **The Barra En Dos Líneas Rule**.
- **Pestañas de hitos:** sin fondo, línea inferior de 1 px `borde-tabla`; la activa en `primario`, seminegrita y borde inferior de 2 px, con transición de `duracion-rapida`. Si no caben, se desplazan en horizontal.

### Modal
- `<dialog>` de 560 px (580 px para tareas, 520 px para diálogos de confirmación), sin pasar del ancho de pantalla menos 16 px por lado; relleno 28 px, esquinas de 16 px (`radio-modal`), borde `borde-tabla`, capa `capa-modal` detrás. Acciones abajo a la derecha, separadas 10 px.
- Entrada animada con `@starting-style` en `duracion-media`: fundido (`opacity`) y capa `::backdrop` (`background-color`), más desplazamiento vertical (`desplazamiento-entrada`) solo si `prefers-reduced-motion: no-preference`. Se desmonta al cerrar.

### Barra de avance
- `<progress>` de 6 px de alto, esquinas en píldora (`radio-avance`, `999px`), pista `avance-pista`, relleno `avance`; verde (`avance-exito`) al 100 %. Siempre acompañada del porcentaje en texto pequeño con cifras tabulares.

### Estado vacío
- Bloque centrado en `superficie` con borde punteado `borde-tabla`, radio `radio-tarjeta`, relleno generoso (`espacio-8` × `espacio-5`; `espacio-6` × `espacio-4` bajo 600 px), título en seminegrita (`titulo-tarjeta`), texto en `texto-secundario` y acción opcional debajo.

### Ayuda a pedido
- Botón «?» de `44 × 44` px (círculo interior `22` px con borde `borde`, activo en `primario` sobre `info-fondo`) con `aria-expanded`, que revela debajo un panel en `superficie` con borde `borde-tabla`, radio `radio`, sombra `--sombra` y transición de opacidad en `duracion-rapida`; cierra con `Escape` o clic fuera.

## Celular

Ajustes para pantallas angostas (600 px o menos) manteniendo la paleta y la estructura general sin desborde horizontal entre 320 y 1440 px:

- **Tablas a tarjetas (`@container (width < 600px)`):** `.tabla-contenedor` define `container-type: inline-size`. Bajo 600 px de contenedor se oculta `thead` (`display: none`), se anulan `min-width` y anchos fijos de columnas, cada fila pasa a tarjeta (`superficie`, borde `borde-tabla`, `radio-tarjeta`, relleno `espacio-4`, separación `espacio-3`), la celda principal (`<th scope="row">`) queda arriba en seminegrita y las demás celdas muestran «etiqueta | valor» con `::before` (`tamano-pequeno`, `texto-secundario`, sin repetir si ya tienen etiqueta accesible propia) y `overflow-wrap: anywhere` en los valores. Las tablas vacías (`tabla__vacia`) conservan su aspecto original.
- **Panel 2×2 (`.resumen-panel`):** se mantiene en 2 columnas hasta 360 px y pasa a 1 columna solo bajo 360 px.
- **Objetivos táctiles de 44 px en la barra superior:** bajo 600 px la barra conserva las 2 filas y todos sus elementos tocables (enlaces de navegación, campana, usuario/perfil y «Salir») tienen un área táctil mínima de 44 × 44 px mediante relleno, sin cambiar el tamaño de letra y sin desborde a 320 px.

## Do's and Don'ts

### Do:
- **Do** usar solo variables de `:root` para colores, espacios, radios, tamaños y movimiento; si falta una, agregarla allí primero.
- **Do** mantener el texto secundario en 5:1 o más y el texto de estado en 4,5:1 o más sobre su fondo.
- **Do** dejar el foco visible con el anillo `--foco` (o `--foco-sobre-marca` en la barra superior).
- **Do** dar objetivos táctiles de 24 px como mínimo a enlaces, botones de texto, casillas y elementos de la barra, y de 44 px con puntero táctil (`pointer: coarse`), incluidos botones y campos.
- **Do** escribir el estado con palabras en cada insignia y alerta.
- **Do** darle a cada tabla de columnas fijas un `min-width` (columnas fijas + 10–12rem para la flexible), `overflow-wrap: break-word` en las celdas y desplazamiento dentro de `.tabla-contenedor`.
- **Do** colocar cada `@media` después de las reglas base que cambia.
- **Do** revisar cada pantalla a 1440 y 390 px y comprobar que nada desborda entre 320 y 1440 px.
- **Do** usar en celular (600 px o menos) la barra superior en dos líneas, con la campana SVG y la palabra «Notificaciones» oculta solo a la vista.
- **Do** respetar `prefers-reduced-motion`: el indicador de carga deja de girar, y los desplazamientos o cambios de tamaño (`translate`, `scale`) solo se aplican bajo `prefers-reduced-motion: no-preference`.

### Don't:
- **Don't** escribir colores sueltos (hex, `rgb()`) fuera de `:root`.
- **Don't** cambiar la paleta ni la estructura de barra + contenido al refinar una pantalla.
- **Don't** usar `overflow-wrap: anywhere` en celdas de tablas: parte las palabras en cualquier letra.
- **Don't** dejar que una tabla ensanche la página; el desplazamiento va dentro de su recuadro.
- **Don't** poner sombras en tarjetas, tablas o modales; la única sombra en reposo es la de la tarjeta de ingreso.
- **Don't** usar el violeta `critico` para algo que no sea ruta crítica o prerrequisito crítico.
- **Don't** mostrar un estado solo con color.
- **Don't** agregar otra familia tipográfica.
