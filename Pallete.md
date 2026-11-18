# Paleta de colores actual — UniProject

- **Fuente:** `web/src/estilos/tema.css` (bloque `:root`)
- **Regla de arquitectura (decisión 25-7, Aclaración 1):** las pantallas usan únicamente las variables definidas en `:root`, nunca colores sueltos; si los valores cambian, solo se edita ese bloque.
- **Origen de los valores:** diseño en Figma (archivo `w8BC6jwbpzPg8XMvKu1EQ4`, pantallas P01a, P01b, P02, P03, P04, P05 y P06) y ajustes documentados en los comentarios de `tema.css`.
- **Fecha:** 2026-11-18

---

## 1. Marca y primario

| Token | Valor | Muestra | Uso | Notas |
| --- | --- | --- | --- | --- |
| `--color-primario` | `#285cdb` | <span style="display:inline-block;width:1em;height:1em;background:#285cdb;border:1px solid #d1d6de"></span> | Botón primario, enlaces, casillas marcadas, pestaña activa, hito seleccionado, nodos/aristas críticos del grafo, indicador de carga y puntos del historial | Valor base de Figma (P01a, P03, P04, P06) |
| `--color-primario-hover` | `#1f4bb8` | <span style="display:inline-block;width:1em;height:1em;background:#1f4bb8;border:1px solid #d1d6de"></span> | Estado `:hover` del botón primario (`.boton--primario`) | Derivado: no está en Figma |
| `--color-texto-sobre-primario` | `#ffffff` | <span style="display:inline-block;width:1em;height:1em;background:#ffffff;border:1px solid #d1d6de"></span> | Texto en botones primarios y de peligro, y marca de verificación de casillas | — |
| `--color-marca-fondo` | `#162850` | <span style="display:inline-block;width:1em;height:1em;background:#162850;border:1px solid #d1d6de"></span> | Fondo del panel de marca en ingreso/registro (P01a/P01b) y de la barra superior (P02, P03, P04, P06) | — |
| `--color-texto-sobre-marca` | `#ffffff` | <span style="display:inline-block;width:1em;height:1em;background:#ffffff;border:1px solid #d1d6de"></span> | Nombre `UniProject`, ítem de navegación activo y nombre del usuario en la barra superior | — |
| `--color-texto-sobre-marca-secundario` | `#c7d6f7` | <span style="display:inline-block;width:1em;height:1em;background:#c7d6f7;border:1px solid #d1d6de"></span> | Lema y descripción en el panel de ingreso, enlaces inactivos de la barra, rol del usuario y acción de salir | — |

---

## 2. Superficies, texto y bordes

| Token | Valor | Muestra | Uso | Notas |
| --- | --- | --- | --- | --- |
| `--color-fondo` | `#f6f8fb` | <span style="display:inline-block;width:1em;height:1em;background:#f6f8fb;border:1px solid #d1d6de"></span> | Fondo general de página (`body`), filas de hitos, tarjetas de equipo, lienzo de ruta crítica, listas de evidencias/comentarios y campos de solo lectura | — |
| `--color-superficie` | `#ffffff` | <span style="display:inline-block;width:1em;height:1em;background:#ffffff;border:1px solid #d1d6de"></span> | Fondo de tarjetas, modales, tablas, controles de formulario, casillas, botón secundario y nodos con holgura | — |
| `--color-borde` | `#d1d6de` | <span style="display:inline-block;width:1em;height:1em;background:#d1d6de;border:1px solid #d1d6de"></span> | Borde de la tarjeta de acceso (P01a/P01b), campos base, casillas de verificación e indicador de carga | — |
| `--color-texto` | `#161b26` | <span style="display:inline-block;width:1em;height:1em;background:#161b26;border:1px solid #d1d6de"></span> | Texto principal, títulos de página y tarjeta, etiquetas, celdas de tabla, entradas de formulario y botón secundario | — |
| `--color-texto-secundario` | `#6b7380` | <span style="display:inline-block;width:1em;height:1em;background:#6b7380;border:1px solid #d1d6de"></span> | Subtítulos, textos de ayuda, fechas, pestañas inactivas, etiquetas de resumen, estado de carga y flechas no críticas del grafo | — |

---

## 3. Estados

| Token | Valor | Muestra | Uso | Notas |
| --- | --- | --- | --- | --- |
| `--color-exito` | `#177d45` | <span style="display:inline-block;width:1em;height:1em;background:#177d45;border:1px solid #d1d6de"></span> | Texto de insignias de éxito (`Completada`, `Nota definitiva`, `Riesgo bajo`, `Suma de pesos: 100 %`), alertas de éxito y título de modal de éxito | En Figma: `#1a8c4d`. Oscurecido por accesibilidad; decisión del usuario 2026-11-04 |
| `--color-exito-fondo` | `#e5f7eb` | <span style="display:inline-block;width:1em;height:1em;background:#e5f7eb;border:1px solid #d1d6de"></span> | Fondo de insignias de éxito (`.insignia--exito`) y alertas de éxito (`.alerta--exito`) | — |
| `--color-aviso` | `#9b6104` | <span style="display:inline-block;width:1em;height:1em;background:#9b6104;border:1px solid #d1d6de"></span> | Texto de insignias de aviso (`Riesgo medio`, contador de predecesoras), alertas de aviso y título de corrección pendiente | En Figma: `#b87305`. Oscurecido por accesibilidad |
| `--color-aviso-fondo` | `#fff2d9` | <span style="display:inline-block;width:1em;height:1em;background:#fff2d9;border:1px solid #d1d6de"></span> | Fondo de insignias de aviso (`.insignia--aviso`), alertas de aviso, bloque de corrección pendiente y avisos en modales | — |
| `--color-peligro` | `#cc2626` | <span style="display:inline-block;width:1em;height:1em;background:#cc2626;border:1px solid #d1d6de"></span> | Texto de insignias de peligro (`Riesgo alto`, alertas de inactividad/bloqueo), errores de validación, botón de peligro y cifra de equipos en alerta | — |
| `--color-peligro-fondo` | `#fcebeb` | <span style="display:inline-block;width:1em;height:1em;background:#fcebeb;border:1px solid #d1d6de"></span> | Fondo de insignias de peligro (`.insignia--peligro`), alertas de error (`.alerta--error`), confirmaciones destructivas y opciones excluidas | — |
| `--color-info` | `#285cdb` | <span style="display:inline-block;width:1em;height:1em;background:#285cdb;border:1px solid #d1d6de"></span> | Texto de insignias informativas (`En progreso`, `Ruta crítica`, `Líder`, chips de predecesoras) y alertas informativas | Mismo valor hex que el color primario |
| `--color-info-fondo` | `#e8f0ff` | <span style="display:inline-block;width:1em;height:1em;background:#e8f0ff;border:1px solid #d1d6de"></span> | Fondo de insignias info (`.insignia--info`), alertas info, botón suave, hito seleccionado, nodo crítico del grafo y notificación no leída | — |
| `--color-neutro` | `#696d79` | <span style="display:inline-block;width:1em;height:1em;background:#696d79;border:1px solid #d1d6de"></span> | Texto de insignias neutras (`Pendiente`, `Bloqueada por dependencia`, monitores) y borde de muestra de tarea con holgura | En Figma: `#737885`. Oscurecido por accesibilidad |
| `--color-neutro-fondo` | `#f0f2f5` | <span style="display:inline-block;width:1em;height:1em;background:#f0f2f5;border:1px solid #d1d6de"></span> | Fondo de insignias neutras (`.insignia--neutro`) | — |
| `--color-critico` | `#7340bf` | <span style="display:inline-block;width:1em;height:1em;background:#7340bf;border:1px solid #d1d6de"></span> | Texto de insignia crítica (`En Revisión` en hitos, `Prerrequisito crítico` en tareas) | — |
| `--color-critico-fondo` | `#f0e8fc` | <span style="display:inline-block;width:1em;height:1em;background:#f0e8fc;border:1px solid #d1d6de"></span> | Fondo de insignia crítica (`.insignia--critico`) | — |

---

## 4. Tablas, avance y contador

| Token | Valor | Muestra | Uso | Notas |
| --- | --- | --- | --- | --- |
| `--color-borde-tabla` | `#dbdee5` | <span style="display:inline-block;width:1em;height:1em;background:#dbdee5;border:1px solid #d1d6de"></span> | Bordes de tablas, tarjetas, modales, botón secundario, entradas dentro del marco, selectores y nodos con holgura | — |
| `--color-tabla-encabezado-fondo` | `#f0f2f5` | <span style="display:inline-block;width:1em;height:1em;background:#f0f2f5;border:1px solid #d1d6de"></span> | Fondo de encabezados de tabla (`.tabla th`) | — |
| `--color-tabla-encabezado-texto` | `#6b7380` | <span style="display:inline-block;width:1em;height:1em;background:#6b7380;border:1px solid #d1d6de"></span> | Texto de encabezados de tabla (`.tabla th`) | — |
| `--color-avance` | `#285cdb` | <span style="display:inline-block;width:1em;height:1em;background:#285cdb;border:1px solid #d1d6de"></span> | Relleno de la barra de avance (`.avance__barra`) cuando el progreso es menor al 100 % | Mismo valor hex que el color primario |
| `--color-avance-exito` | `#1a8c4d` | <span style="display:inline-block;width:1em;height:1em;background:#1a8c4d;border:1px solid #d1d6de"></span> | Relleno de la barra de avance al 100 % (`.avance--completo`) | Usa el verde original de Figma (`#1a8c4d`), no el oscurecido |
| `--color-avance-pista` | `#f0f2f5` | <span style="display:inline-block;width:1em;height:1em;background:#f0f2f5;border:1px solid #d1d6de"></span> | Fondo de la pista de la barra de avance (`.avance__barra`) | — |
| `--color-contador-fondo` | `#cc2626` | <span style="display:inline-block;width:1em;height:1em;background:#cc2626;border:1px solid #d1d6de"></span> | Fondo de la píldora del contador de notificaciones en la barra superior (`.barra__contador`) | — |
| `--color-texto-sobre-contador` | `#ffffff` | <span style="display:inline-block;width:1em;height:1em;background:#ffffff;border:1px solid #d1d6de"></span> | Texto del contador de notificaciones en la barra superior (`.barra__contador`) | — |

---

## 5. Pares de estado (insignias y alertas)

Combinaciones de texto sobre fondo empleadas en insignias (`.insignia--*`) y alertas (`.alerta--*`), según `tema.css` y las notas de Figma (`figma-notas-26b.md` y `figma-notas-26c.md`):

- **Información (`En progreso`, `Ruta crítica`, `Líder`, alertas info):** texto `#285cdb` (`--color-info`) sobre fondo `#e8f0ff` (`--color-info-fondo`). Usado en el estado `En progreso` de hitos y tareas, marca `Ruta crítica`, insignia `Líder`, chips de predecesoras (`T2 · ...`), botón `Evaluar` (`.boton--suave`) y `.alerta--info`.
- **Crítico (`En Revisión`, `Prerrequisito crítico`):** texto `#7340bf` (`--color-critico`) sobre fondo `#f0e8fc` (`--color-critico-fondo`). Usado en el estado de hito `En Revisión` (P02) y en la marca de tarea `Prerrequisito crítico` (P04).
- **Éxito (`Completada`, `Nota definitiva`, `Riesgo bajo`, alertas de éxito):** texto `#177d45` (`--color-exito`; en Figma `#1a8c4d`) sobre fondo `#e5f7eb` (`--color-exito-fondo`). Usado en el estado `Completada` de hitos y tareas, pestaña `Nota definitiva`, marca `Riesgo bajo`, insignia `Suma de pesos: 100 %` (rúbrica P03) y `.alerta--exito`.
- **Neutro (`Pendiente`, `Bloqueada por dependencia`, monitores):** texto `#696d79` (`--color-neutro`; en Figma `#737885`) sobre fondo `#f0f2f5` (`--color-neutro-fondo`). Usado en el estado `Pendiente` de hitos y tareas, marca `Bloqueada por dependencia` e insignias con nombres de monitores (P03).
- **Aviso (`Riesgo medio`, alertas de aviso):** texto `#9b6104` (`--color-aviso`; en Figma `#b87305`) sobre fondo `#fff2d9` (`--color-aviso-fondo`). Usado en la marca `Riesgo medio` (P04), contador de predecesoras en `Bloqueada por` (P05), `.alerta--aviso` y bloque `.correccion-pendiente`.
- **Peligro / Alertas (`Inactivo 5 días`, `Bloqueada > 48 h`, `Riesgo alto`, alertas de error):** texto `#cc2626` (`--color-peligro`) sobre fondo `#fcebeb` (`--color-peligro-fondo`). Usado en las insignias de alerta del panel docente (`Inactivo 5 días`, `Bloqueada > 48 h`), marca `Riesgo alto` (P04), `.alerta--error` y `.aviso-modal--peligro`.

---

## 6. Otros valores de color

Variables de `:root` en `web/src/estilos/tema.css` que contienen colores (sombras, anillos de foco y superposición de modal):

| Token | Valor | Muestra | Uso | Notas |
| --- | --- | --- | --- | --- |
| `--sombra` | `0 8px 24px rgba(0, 0, 0, 0.06)` | <span style="display:inline-block;width:1em;height:1em;background:rgba(0, 0, 0, 0.06);border:1px solid #d1d6de"></span> | Sombra de la tarjeta de ingreso y registro (`.tarjeta-acceso`) | Negro al 6 % de opacidad |
| `--foco` | `0 0 0 3px rgba(40, 92, 219, 0.35)` | <span style="display:inline-block;width:1em;height:1em;background:rgba(40, 92, 219, 0.35);border:1px solid #d1d6de"></span> | Anillo de foco (`:focus-visible`) general, en casillas y en `.campo-sufijo` | Basado en el RGB del color primario (`40, 92, 219`) al 35 % |
| `--foco-sobre-marca` | `0 0 0 2px #ffffff` | <span style="display:inline-block;width:1em;height:1em;background:#ffffff;border:1px solid #d1d6de"></span> | Anillo de foco (`:focus-visible`) para elementos dentro de la barra superior (`.barra`) | Blanco sólido sobre fondo de marca |
| `--color-capa-modal` | `rgba(23, 28, 41, 0.55)` | <span style="display:inline-block;width:1em;height:1em;background:rgba(23, 28, 41, 0.55);border:1px solid #d1d6de"></span> | Fondo semitransparente (`.modal::backdrop`) de los diálogos modales (P05, P07b, P07c, P08a, P08b) | Declarada en la sección del bloque 26c de `:root` (línea 122) con `rgba` |

---

## 7. Observaciones

- **Sin modo oscuro:** `prefers-color-scheme` no aparece en `web/src/estilos/tema.css`; solo se define el tema claro en `:root`.
- **Repetición del color primario:** `--color-info` (`#285cdb`) y `--color-avance` (`#285cdb`) repiten el valor de `--color-primario` (`#285cdb`).
- **Verde de avance frente a verde de estado:** `--color-avance-exito` usa el verde de Figma `#1a8c4d` y no el oscurecido de `--color-exito` (`#177d45`).
- **Otros valores hexadecimales compartidos:**
  - `#ffffff` se repite en `--color-texto-sobre-primario`, `--color-superficie`, `--color-texto-sobre-marca` y `--color-texto-sobre-contador`.
  - `#f0f2f5` se repite en `--color-neutro-fondo`, `--color-tabla-encabezado-fondo` y `--color-avance-pista`.
  - `#6b7380` se repite en `--color-texto-secundario` y `--color-tabla-encabezado-texto`.
  - `#cc2626` se repite en `--color-peligro` y `--color-contador-fondo`.
- **Variable `--color-*` en el bloque 26c:** además de las 31 variables `--color-*` con valores hexadecimales de las líneas 5–38, en la línea 122 de `:root` está definida `--color-capa-modal: rgba(23, 28, 41, 0.55)` para la superposición de los modales.
