# Sistema de diseño de Forja

Forja usa **Crimson Ink**, el sistema de diseño de Gabriel (negro tinta y carmesí, de Trucha OS). Este documento lo aterriza en la app: tokens, contraste, componentes con sus estados y voz. Los tokens viven en un único `frontend/theme.ts` (§4). Oscuro por defecto.

**Fuente:** [Crimson Ink](https://claude.ai/artifact/8aiPpZtQid9ygULcBXvm91) (`project/tokens.json` y `project/README.md`, leídos el 2026-11-16). Todo valor sin etiqueta sale de ahí. Lo que Crimson Ink no define y propone Forja va marcado **[propuesta]**. Crimson Ink no define movimiento, iconografía de la app, estados de componentes ni colores de estado del tema claro: todo eso es propuesta de este documento.

## 1. Principios

De Crimson Ink:

1. **Un solo acento.** `primary` (carmesí) va en una cosa por zona: la acción principal, el dato clave o el foco. Si todo es carmesí, nada lo es.
2. **La profundidad se marca con `surface-*`, no con sombras.** La única sombra es `glow-primary`, para el foco.
3. **Directo y con cifras.** Lo importante en la primera frase, cada promesa con su dato.
4. **Prohibido:** degradados (salvo dentro del logo), azules o morados de acento, fondos blancos puros en el tema oscuro.
5. **`error`, `warning`, `info` y `success` van siempre con palabra o icono.** `error` se parece a `primary`.

De Forja:

6. **Oscuro por defecto.** El tema claro existe, pero el arranque y la captura de pantallas son en oscuro (CLAUDE.md §5).
7. **Accesibilidad AA.** Texto normal 4,5:1; texto grande (24 px, o 18,66 px en negrita) y elementos de interfaz 3:1. Nada se distingue solo por color (daltonismo): siempre color + forma, patrón, icono o texto. **[propuesta]**
8. **Se usa con una mano en el gimnasio.** Controles de registro grandes y al alcance del pulgar (§5.3). **[propuesta]**
9. **La app explica y avisa.** Cada ejercicio dice por qué está; cada cifra estimada lo dice (§5.7). Forja es de fitness, no sanitaria (§6).

## 2. Tokens de color

Valores exactos de `tokens.json`. Ratios calculados con la fórmula de luminancia relativa WCAG 2.x (sRGB, `L = 0,2126 R + 0,7152 G + 0,0722 B`, ratio = `(L1 + 0,05) / (L2 + 0,05)`).

### 2.1 Paleta por rol

| Rol | Token | Oscuro | Claro | Uso |
|---|---|---|---|---|
| Fondo | `background` | `#0c0c0e` | `#f7f3f0` | Fondo de página |
| Superficie | `surface-lowest` | `#08080a` | `#ffffff` | Pozos, fondo tras modales |
| | `surface-low` | `#111114` | `#f2edea` | Paneles laterales y barras |
| | `surface` | `#151518` | `#ece7e4` | Tarjetas y paneles |
| | `surface-high` | `#1c1c20` | `#e6e1de` | Tarjeta elevada, menú, hover de fila |
| | `surface-highest` | `#242429` | `#e0dbd8` | Popovers, notificaciones |
| | `surface-variant` | `#26262b` | `#e7e1dd` | Campos, pistas de barras, bloques de código |
| Texto | `on-background` | `#ece8e4` | `#1a1a1e` | Texto principal |
| | `on-surface-variant` | `#bdb8b3` | `#4a4a52` | Texto secundario; borde de controles |
| Primario | `primary` | `#e5383b` | `#c1121f` | Acción principal, foco, dato clave |
| | `on-primary` | `#0c0c0e` | `#ffffff` | Texto sobre relleno `primary` |
| | `primary-container` | `#4a1416` | `#ffdad8` | Seleccionado (chip, fila activa); texto en `on-background` |
| Acento secundario | `secondary` | `#ff8a8c` | `#a32a2c` | Hover de `primary`, iconos de segundo nivel |
| Estado | `error` | `#ff6b6b` | hereda oscuro | Error (con icono/texto) |
| | `warning` | `#e8b04b` | hereda oscuro | Aviso |
| | `info` | `#7a9cc6` | hereda oscuro | Información neutra |
| | `success` | `#8fbf7f` | hereda oscuro | Éxito |
| Borde | `outline` | `#4a4a52` | `#8a8580` | Separadores decorativos (no para el borde de un control) |

En claro, Crimson Ink no define los cuatro colores de estado y heredan el valor oscuro, que no pasa AA sobre fondo claro. Forja propone sus variantes claras en §2.3.

### 2.2 Contraste (valores de Crimson Ink)

Leyenda: AA texto = 4,5:1; AA grande/UI = 3:1.

**Tema oscuro**

| Primer plano | Fondo | Ratio | Texto normal | Grande / UI |
|---|---|---|---|---|
| `on-background` `#ece8e4` | `background` `#0c0c0e` | 16,03 | cumple | cumple |
| `on-background` | `surface` `#151518` | 14,95 | cumple | cumple |
| `on-background` | `surface-high` `#1c1c20` | 13,94 | cumple | cumple |
| `on-background` | `surface-highest` `#242429` | 12,67 | cumple | cumple |
| `on-background` | `surface-variant` `#26262b` | 12,35 | cumple | cumple |
| `on-background` | `primary-container` `#4a1416` | 12,28 | cumple | cumple |
| `on-surface-variant` `#bdb8b3` | `background` | 9,93 | cumple | cumple |
| `on-surface-variant` | `surface` | 9,26 | cumple | cumple |
| `on-surface-variant` | `surface-high` | 8,63 | cumple | cumple |
| `on-surface-variant` | `surface-highest` | 7,85 | cumple | cumple |
| `on-surface-variant` | `surface-variant` | 7,65 | cumple | cumple |
| `primary` `#e5383b` | `background` | 4,62 | cumple | cumple |
| `primary` | `surface` | **4,30** | **NO** | cumple |
| `primary` | `surface-high` | **4,01** | **NO** | cumple |
| `primary` | `surface-highest` | **3,65** | **NO** | cumple |
| `primary` | `surface-variant` | **3,56** | **NO** | cumple |
| `primary` | `primary-container` | **3,54** | **NO** | cumple |
| `on-primary` `#0c0c0e` | `primary` (botón) | 4,62 | cumple | cumple |
| `secondary` `#ff8a8c` | `background` | 8,62 | cumple | cumple |
| `secondary` | `surface-high` | 7,49 | cumple | cumple |
| `error` `#ff6b6b` | `background` / `surface` / `surface-highest` | 7,04 / 6,57 / 5,57 | cumple | cumple |
| `warning` `#e8b04b` | `background` / `surface` / `surface-highest` | 10,00 / 9,32 / 7,90 | cumple | cumple |
| `info` `#7a9cc6` | `background` / `surface` / `surface-highest` | 6,88 / 6,42 / 5,44 | cumple | cumple |
| `success` `#8fbf7f` | `background` / `surface` / `surface-highest` | 9,24 / 8,61 / 7,30 | cumple | cumple |
| `error` | `primary-container` | 5,39 | cumple | cumple |
| `outline` `#4a4a52` | `background` | **2,23** | **NO** | **NO** |
| `outline` | `surface` | **2,08** | **NO** | **NO** |
| `on-surface-variant` (borde de control) | `background` | 9,93 | cumple | cumple |
| `primary-container` (sin texto, como forma) | `background` | **1,31** | **NO** | **NO** |

**Tema claro**

| Primer plano | Fondo | Ratio | Texto normal | Grande / UI |
|---|---|---|---|---|
| `on-background` `#1a1a1e` | `background` `#f7f3f0` | 15,72 | cumple | cumple |
| `on-background` | `surface` `#ece7e4` | 14,14 | cumple | cumple |
| `on-background` | `surface-highest` `#e0dbd8` | 12,63 | cumple | cumple |
| `on-background` | `primary-container` `#ffdad8` | 13,44 | cumple | cumple |
| `on-surface-variant` `#4a4a52` | `background` | 7,96 | cumple | cumple |
| `on-surface-variant` | `surface` | 7,15 | cumple | cumple |
| `on-surface-variant` | `surface-highest` | 6,39 | cumple | cumple |
| `primary` `#c1121f` | `background` | 5,64 | cumple | cumple |
| `primary` | `surface` | 5,07 | cumple | cumple |
| `primary` | `surface-high` | 4,80 | cumple | cumple |
| `primary` | `surface-highest` | 4,53 | cumple | cumple |
| `primary` | `primary-container` | 4,82 | cumple | cumple |
| `on-primary` `#ffffff` | `primary` (botón) | 6,22 | cumple | cumple |
| `secondary` `#a32a2c` | `background` / `surface` | 6,51 / 5,85 | cumple | cumple |
| `error` heredado `#ff6b6b` | `background` | **2,52** | **NO** | **NO** |
| `warning` heredado `#e8b04b` | `background` | **1,77** | **NO** | **NO** |
| `info` heredado `#7a9cc6` | `background` | **2,57** | **NO** | **NO** |
| `success` heredado `#8fbf7f` | `background` | **1,92** | **NO** | **NO** |
| `outline` `#8a8580` | `background` | 3,31 | **NO** | cumple |
| `outline` | `surface` | **2,98** | **NO** | **NO** (por 0,02) |
| `primary-container` (como forma) | `background` | **1,17** | **NO** | **NO** |

### 2.3 Pares que NO cumplen AA y qué hacer

| Par | Problema | Regla en Forja |
|---|---|---|
| `primary` texto sobre `surface` (oscuro, 4,30) | Falla texto normal | Texto carmesí normal solo sobre `background`. Sobre `surface-*` solo texto grande/negrita ≥ 18,66 px o iconos y formas (3:1). |
| `primary` sobre `surface-high`, `surface-highest`, `surface-variant`, `primary-container` (oscuro, 3,54-4,01) | Falla texto normal; cumple 3:1 | Igual: nunca texto pequeño carmesí ahí. Los enlaces en tarjetas elevadas van en `on-background` subrayado, o con `secondary` (7,49). |
| `outline` (2,23 en oscuro; 2,98 sobre `surface` en claro) | No llega a 3:1 | Solo separadores decorativos. El borde de un control usa `on-surface-variant`. |
| `primary-container` frente a `background` (1,31) | La forma no se distingue | Un elemento seleccionado lleva además borde `primary` de 1,5 px (4,62) o un icono de «marcado». |
| `error`, `warning`, `info`, `success` en claro | Heredan el valor oscuro: 1,77-2,57 | Usar las variantes claras de abajo. |

**Variantes claras de estado [propuesta]** (no existen en Crimson Ink; valores elegidos y calculados aquí):

| Token | Claro | `background` | `surface` | `surface-highest` | `surface-variant` |
|---|---|---|---|---|---|
| `error` | `#b42318` | 5,96 | 5,36 | 4,79 | 5,08 |
| `warning` | `#8a5a00` | 5,37 | 4,83 | 4,32 **NO** | 4,58 |
| `info` | `#2f5d8f` | 6,17 | 5,55 | 4,96 | 5,26 |
| `success` | `#2e6b2a` | 5,85 | 5,26 | 4,70 | 4,98 |

`warning` claro sobre `surface-highest` da 4,32: no pongas texto de aviso pequeño sobre popovers en claro (o usa `on-background` con icono ámbar).

**Una pega de Crimson Ink que dejo señalada, sin tocarla:** su `README.md` dice que `primary` en oscuro cumple 4,6:1 «sobre `background` o `surface-*`», pero sobre `surface-*` baja a 4,30-3,65. Cierto solo sobre `background`.

## 3. Tipografía, espaciado, radios, sombras y movimiento

### 3.1 Tipografía

Fuentes de Google Fonts: `Inter:wght@400;500;700;800`, `Noto+Serif+JP:wght@500;900`, `JetBrains+Mono:wght@400;700`. En la app (React Native/Expo) se cargan con `expo-font`/`@expo-google-fonts`. Pila de reserva: la de `tokens.json`.

| Familia | Valor | Para qué |
|---|---|---|
| `sans` | `Inter, system-ui, sans-serif` | Interfaz y texto |
| `mono` | `"JetBrains Mono", ui-monospace, monospace` | Datos: pesos, repeticiones, ratios, cifras (con `tabular-nums`) |
| `kanji` | `"Noto Serif JP", serif` | Solo acentos, nunca texto que se lea |

| Estilo | Familia | Tamaño | Interlineado | Peso | Extra | Uso |
|---|---|---|---|---|---|---|
| `display` | sans | 40 px | 1,1 (44 px) | 800 | | Uno por página |
| `title` | sans | 30 px | 1,15 (34,5 px) | 800 | | Título de sección o pantalla |
| `heading` | sans | 20 px | 1,3 (26 px) | 700 | | Subtítulos |
| `body` | sans | 15 px | 1,5 (22,5 px) | 400 | | Texto corrido |
| `label` | sans | 12 px | 1,4 (16,8 px) | 500 | `letterSpacing` 7 px, mayúsculas | Palabras sueltas, en `on-surface-variant` |
| `data` | mono | 13 px | 1,5 (19,5 px) | 400 | `tabular-nums` | Cifras, medidas, comandos |
| `kanji-hero` | kanji | 120 px | 1 | 900 | `primary` + `glow-primary` | Un kanji de acento por pantalla |
| `kanji-stack` | kanji | 30 px | 1,25 | 500 | | Kanji apilados |

Reglas: 12 px solo para `label`; 13 px solo para `data`. En móvil no bajes de 15 px para texto que se lea **[propuesta]**. Respeta el tamaño de fuente del sistema (escalado hasta 200 %) **[propuesta]**.

### 3.2 Espaciado, radios y sombra

| Token | Valor | Uso |
|---|---|---|
| `space-1` | 4 px | Entre icono y texto |
| `space-2` | 8 px | Dentro de chips y entre elementos de una fila |
| `space-4` | 16 px | Relleno de tarjeta y margen lateral mínimo |
| `space-6` | 24 px | Entre bloques de una sección |
| `space-12` | 48 px | Entre secciones |
| `radius-sm` | 6 px | Chips, campos, botones |
| `radius-md` | 12 px | Tarjetas y paneles |
| `radius-xl` | 112 px | Solo el icono de app |
| `glow-primary` | `0 0 30px rgba(229,56,59,0.35)` | Única sombra: kanji de acento y foco |

Foco visible: anillo de 2 px en `primary` con `glow-primary`. En RN (sin `box-shadow`) se aproxima con `shadowColor`/`shadowRadius` en iOS y borde de 2 px en Android **[propuesta]**.

### 3.3 Movimiento **[propuesta]** (Crimson Ink no define movimiento)

| Token | Valor | Uso |
|---|---|---|
| `motion-fast` | 120 ms | Pulsación, cambio de estado de un botón |
| `motion-base` | 200 ms | Aparición de avisos, expandir «por qué» |
| `motion-slow` | 320 ms | Transición de pantalla |
| Curva | `ease-out` para entrar, `ease-in` para salir | |

Reglas: solo se anima `opacity` y `transform`. Con «reducir movimiento» del sistema activado, las transiciones pasan a 0 ms y solo cambia el estado final. Nada parpadea ni rebota; el carmesí no pulsa.

### 3.4 Iconografía

Crimson Ink no define un set de iconos de interfaz; solo dos assets de marca (`trucha-os.svg`, `truchi.svg`; no se recolorean) y el kanji de acento (鱒 la firma; por sección en apps, p. ej. 服 armario, 寸 medidas). Para Forja **[propuesta]**: un único set de iconos de trazo (24 px, trazo 2 px, esquinas redondeadas) en `on-background` o `on-surface-variant`; los de estado en el color del estado. Los kanji de sección de Forja se proponen en su día; no hay ninguno decidido todavía. Truchi (la mascota) solo en estados vacíos, cargas y ayudas, **nunca en avisos de bandera roja**.

## 4. Volcado a `frontend/theme.ts`

Ejemplo del objeto (el archivo no existe todavía; lo crea la fase de frontend). Nombres de tokens en `camelCase`; valores de Crimson Ink salvo lo marcado.

```ts
// frontend/theme.ts: único sitio con valores de diseño (Crimson Ink)
const base = {
  space: { s1: 4, s2: 8, s4: 16, s6: 24, s12: 48 },
  radius: { sm: 6, md: 12, xl: 112 },
  font: {
    sans: 'Inter',
    mono: 'JetBrainsMono',
    kanji: 'NotoSerifJP',
  },
  // [propuesta] Crimson Ink no define movimiento
  motion: { fast: 120, base: 200, slow: 320 },
  // [propuesta] objetivo táctil mínimo en dp; el botón de confirmar serie es mayor
  hit: { min: 48, confirm: 72 },
  type: {
    display: { size: 40, line: 44, weight: '800' },
    title: { size: 30, line: 34.5, weight: '800' },
    heading: { size: 20, line: 26, weight: '700' },
    body: { size: 15, line: 22.5, weight: '400' },
    label: { size: 12, line: 16.8, weight: '500', letterSpacing: 7, upper: true },
    data: { size: 13, line: 19.5, weight: '400', tabular: true },
  },
} as const;

export const dark = {
  ...base,
  mode: 'dark',
  color: {
    background: '#0c0c0e',
    surfaceLowest: '#08080a',
    surfaceLow: '#111114',
    surface: '#151518',
    surfaceHigh: '#1c1c20',
    surfaceHighest: '#242429',
    surfaceVariant: '#26262b',
    onBackground: '#ece8e4',
    onSurfaceVariant: '#bdb8b3',
    primary: '#e5383b',
    onPrimary: '#0c0c0e',
    primaryContainer: '#4a1416',
    secondary: '#ff8a8c',
    outline: '#4a4a52',
    error: '#ff6b6b',
    warning: '#e8b04b',
    info: '#7a9cc6',
    success: '#8fbf7f',
    // [propuesta] mapa muscular
    muscleWeak: '#e8b04b',
    muscleAverage: '#5b7fae',
    muscleStrong: '#dbe8f8',
    muscleNoData: '#26262b',
  },
  shadow: { glowPrimary: { color: '#e5383b', opacity: 0.35, radius: 30 } },
} as const;

export const light = {
  ...base,
  mode: 'light',
  color: {
    background: '#f7f3f0',
    surfaceLowest: '#ffffff',
    surfaceLow: '#f2edea',
    surface: '#ece7e4',
    surfaceHigh: '#e6e1de',
    surfaceHighest: '#e0dbd8',
    surfaceVariant: '#e7e1dd',
    onBackground: '#1a1a1e',
    onSurfaceVariant: '#4a4a52',
    primary: '#c1121f',
    onPrimary: '#ffffff',
    primaryContainer: '#ffdad8',
    secondary: '#a32a2c',
    outline: '#8a8580',
    // [propuesta] Crimson Ink hereda el oscuro, que no pasa AA en claro
    error: '#b42318',
    warning: '#8a5a00',
    info: '#2f5d8f',
    success: '#2e6b2a',
    // [propuesta] mapa muscular
    muscleWeak: '#9a5b00',
    muscleAverage: '#3f6aa0',
    muscleStrong: '#1f3f6e',
    muscleNoData: '#e7e1dd',
  },
  shadow: { glowPrimary: { color: '#c1121f', opacity: 0.35, radius: 30 } },
} as const;

export type Theme = typeof dark;
export const defaultTheme = dark; // oscuro por defecto
```

Reglas de código: ningún componente escribe un hex ni un número de espaciado a mano; todo sale de `useTheme()`. Un test comprueba que cada par texto/fondo de §2.2 cumple su ratio (si cambias un token, el CI avisa) **[propuesta]**. `glowPrimary` en claro usa `primary` claro: Crimson Ink solo define el `rgba` oscuro **[propuesta]**.

## 5. Componentes de Forja

Todos [propuesta] en estructura y estados; usan solo tokens de §2-§3. Estados comunes: **reposo**, **pulsado**, **foco** (anillo 2 px `primary` + `glow-primary`), **deshabilitado** (contenido a 40 % de opacidad, exento de contraste por WCAG, pero con la etiqueta legible y sin color como único aviso), **cargando** (texto «Calculando…», nunca solo un spinner).

### 5.1 Botón

| Variante | Relleno | Texto | Borde |
|---|---|---|---|
| Principal | `primary` | `onPrimary` (4,62 oscuro / 6,22 claro) | ninguno |
| Secundario | transparente | `onBackground` | 1,5 px `onSurfaceVariant` |
| Texto | transparente | `onBackground`, subrayado | ninguno |

Estados: pulsado = `secondary` de relleno (el principal) o `surfaceHigh` (secundario), 120 ms. Altura mínima 48 dp, `radius-sm`, etiqueta `body` en 700 con verbo delante («Guardar serie», «Ver el plan»). Solo un botón principal por zona. Tras una acción, el aviso confirma («Guardar» → «Guardado»).

### 5.2 Tarjeta de ejercicio (con «por qué» y nivel de evidencia)

Es la pieza que Forja no encuentra en la competencia: explicar por qué hay cada ejercicio (`docs/competencia.md`, «Qué se lleva Forja»).

Estructura (fondo `surface`, `radius-md`, relleno `space-4`):

1. Nombre del ejercicio (`heading`) y, a la derecha, la insignia de evidencia.
2. Prescripción en `data`: `4 × 6-8 · RIR 2 · 80 kg`.
3. **Línea «por qué»** (`body`, `onSurfaceVariant`, máx. 2 líneas, expandible): la traza del solver ([ADR 0002](adr/0002-solver-cp-sat.md)). Ej.: «Está porque encaja con tu límite de hombro y cubre tu pecho, que va por debajo de la media. Tus palancas lo favorecen: suele ir bien a gente con tus proporciones; pruébalo.»
4. Pie: fuente (enlace en `onBackground` subrayado) y botones «Más», «Menos», «Reemplazar».

**Insignia de nivel de evidencia** (letra + palabra, nunca solo color; forma `radius-sm`, fondo `surfaceVariant`, borde 1,5 px `onSurfaceVariant`, texto `onBackground`):

| Nivel | Texto visible | Significado (ADR 0007) |
|---|---|---|
| A, B, C, D | «Evidencia A» … «D» | Nivel de evidencia de la fuente de la regla |
| H | «Heurística» | Heurística de entrenadores, no ciencia |
| P | «Propuesta propia» | Regla propia de Forja |

Las insignias no usan colores de estado: la evidencia no es «bien/mal». Tocar la insignia abre la fuente (`docs/referencias.md`). Las reglas por palancas llevan siempre «H» y la frase «suele ir bien a gente con tus proporciones; pruébalo», nunca «mejor/peor» ni «malas palancas».

Estados: reposo, pulsado (`surfaceHigh`), expandido (el «por qué» completo, con la traza), sustituido (tachado + «Reemplazado por …» con «Deshacer»), cargando (esqueleto en `surfaceVariant`).

### 5.3 Fila de registro de serie

Se usa en pleno entreno, sin conexión y con una mano. Una serie por fila, en `data` con `tabular-nums`:

| Campo | Control | Notas |
|---|---|---|
| Peso | Campo numérico + botones −/+ de 48 dp | Paso = carga mínima del equipo del gimnasio; unidad (`kg`) visible |
| Repeticiones | Igual | Rango objetivo como pista: «6-8» |
| RIR | Selector de 5 botones (0, 1, 2, 3, 4+) | Objetivo marcado con un punto |
| Dolor 0-10 | Selector de 11 pasos (deslizador con valores visibles) | Etiqueta «Dolor», opcional; por defecto «sin marcar», no 0 |
| Confirmar | Botón grande | Ver abajo |

**Botón de confirmar:** ancho completo de la fila, **72 dp de alto**, en la mitad inferior de la pantalla, alcanzable con el pulgar; relleno `primary`, texto `onPrimary` «Hecho», con icono de check. Es el único botón principal de la pantalla.

Estados de la fila: **pendiente** (valores objetivo en `onSurfaceVariant`, prellenados desde la progresión doble), **editando** (borde `onSurfaceVariant` y foco), **confirmada** (check + «Serie 2 hecha», fondo `surfaceHigh`, no solo cambio de color; permite «Deshacer» 5 s), **con dolor alto** (dolor ≥ 7 [propuesta de umbral]: icono de aviso + «Anota qué ha pasado» y activa §5.5), **sin conexión** (icono + «Guardada en el móvil»; se sincroniza después, [ADR 0008](adr/0008-sincronizacion-sin-conexion.md)).

Validación: peso y repeticiones numéricos y > 0; si no, mensaje debajo del campo en `error` con icono, no solo borde rojo.

### 5.4 Chip de restricción confirmada y pantalla «esto es lo que he entendido»

**Chip** (`radius-sm`, relleno `primaryContainer`, borde 1,5 px `primary`, texto `onBackground`, icono de check): una limitación que el usuario ha confirmado, p. ej. «Hombro derecho · dolor 4/10 · sin press por encima de la cabeza». Estados: **propuesto por el LLM** (borde discontinuo `onSurfaceVariant`, sin relleno, texto «Por confirmar»), **confirmado** (el de arriba), **editado**, **descartado** (tachado, 5 s para deshacer). Cada chip lleva la cita literal del usuario («tu texto: …»).

**Pantalla «esto es lo que he entendido»** (ADR 0007: el LLM solo transcribe; el usuario confirma):

1. Título (`title`): «Esto es lo que he entendido».
2. Tu texto original, en bloque `surfaceVariant`, sin tocar.
3. Lista de chips por confirmar, cada uno con su cita literal resaltada.
4. Pie: «Forja elige ejercicios compatibles con las limitaciones que tú indicas. No evalúa ni diagnostica.»
5. Un botón principal «Confirmar y armar la rutina» (deshabilitado hasta confirmar o descartar cada chip) y uno secundario «Lo he escrito mal».

Si el LLM no entiende algo, lo dice («No he podido leer esta parte») en vez de rellenar.

### 5.5 Aviso de bandera roja

Salta por reglas (nunca por el LLM). Es un aviso, no un diagnóstico.

- Contenedor `surfaceHigh`, borde completo de 2 px (no una barra lateral) en `error` (7,04 sobre fondo en oscuro; 5,96 con la variante clara), `radius-md`.
- Icono de alerta + título en `heading` («Para y consulta») + cuerpo en `body`, `onBackground`.
- Texto tipo: «Has marcado un síntoma que no debe esperar a la próxima sesión. Para el entreno y habla con un profesional sanitario. Forja no puede valorarlo.» Un botón principal «Entendido» y un enlace a recursos de ayuda.
- Va **siempre** con icono y palabra («Aviso»), nunca solo carmesí/rojo. Sin Truchi ni humor. Bloquea el registro hasta cerrarlo.
- Estados: visible, cerrado (queda anotado en la sesión), reaparece si el síntoma se repite.

### 5.6 Mapa muscular

Estados: **débil**, **en la media**, **fuerte**, **sin datos**. Regla de diseño: **el color nunca va solo**; cada estado combina color, patrón/relleno, icono y etiqueta. Se evita el eje rojo/verde (daltonismo): se usa naranja/ámbar frente a azul, y diferencias de luminosidad.

| Estado | Color (oscuro / claro) | Patrón | Icono | Etiqueta |
|---|---|---|---|---|
| Débil | `#e8b04b` / `#9a5b00` | Rayado diagonal | ▼ | «Débil» |
| En la media | `#5b7fae` / `#3f6aa0` | Relleno liso | = | «En la media» |
| Fuerte | `#dbe8f8` / `#1f3f6e` | Relleno liso con puntos | ▲ | «Fuerte» |
| Sin datos | `surfaceVariant` | Borde discontinuo `onSurfaceVariant`, sin relleno | ? | «Sin datos» |

Contrastes calculados (oscuro, sobre `background` `#0c0c0e`): débil 10,00; media 4,74; fuerte 15,74; sin datos: borde `onSurfaceVariant` 9,93. Claro, sobre `background`: débil 4,92; media 5,03; fuerte 9,54. Todos ≥ 3:1 como forma.

Entre estados vecinos el contraste de color es bajo (p. ej. débil y fuerte en oscuro: 1,57; débil y media en claro: 1,02), **a propósito**: la diferencia la dan el patrón, el icono y la etiqueta, no el tono. Leyenda siempre visible bajo el mapa. Cada músculo es tocable (zona ≥ 48 dp o lista equivalente debajo del mapa, para lectores de pantalla): abre una hoja con el estado, el dato («pecho: percentil estimado 38», con §5.7) y el ejercicio que lo trabaja. Si faltan datos: «Registra 3 sesiones con press para ver este músculo» (N configurable). El mapa no usa «atrofia», «desequilibrio» ni «corregir» (§6).

### 5.7 Aviso de «es una estimación»

Lleva toda cifra derivada (percentil, nivel del modelo, peso objetivo ajustado a palancas, estándar de fuerza, ratios corporales).

- Pastilla `radius-sm`, fondo `surfaceVariant`, icono de información + texto en `body`, `onSurfaceVariant`: «Es una estimación» (cumple 7,65 en oscuro).
- Se pone **junto a la cifra**, no escondida en un pie. Tocarla abre la página de método con un ejemplo trabajado en pocos pasos.
- Cuando hay dos escalas (nivel del modelo y percentil de OpenPowerlifting), cada una va etiquetada y la segunda dice el sesgo: «percentil entre competidores de powerlifting, no entre la población general».
- La cifra se escribe en `data`, con rango o margen si lo hay («≈ 82 kg ±5»), nunca con falsa precisión.
- Los porcentajes de ratios corporales («encaja a ±1 cm») llevan siempre su margen.
- El aviso de heurística («regla de entrenadores, no ciencia») usa el mismo componente con el texto «Heurística de entrenadores» e icono distinto.

## 6. Voz y microcopy

Voz de Gabriel (kit de marca): de tú, directo, con cifras y sin frases de plantilla.

- **Verbo delante.** «Registra tu serie», no «Proceda a registrar».
- **Lo importante, primero; el dato, enseguida.** «Tu pecho va por debajo de la media (percentil 38). Prueba press inclinado.»
- **El botón dice lo que pasa y el aviso lo confirma.** «Guardar» → «Guardado».
- **Cada promesa, con su cifra o su fuente.** Si no hay cifra, no hay promesa.
- **Humor ligero, poco**, solo en estados vacíos y cargas (con Truchi). Nunca en avisos, dolor o banderas rojas.
- **Cierra con un paso siguiente** cuando sirve («Registra 3 sesiones para ver tu mapa»).
- **Nunca:** «Sumérgete», «Desbloquea tu potencial», «No es solo X, es Y», «No dudes en…», superlativos sin cifra («el mejor», «líder»), «soluciones disruptivas», «sinergias».
- Anglicismos técnicos normales (RIR, e1RM, *dashboard*), explicados la primera vez en la ayuda de la pantalla.

### Palabras y frases prohibidas (finalidad de fitness, no sanitaria)

Forja «elige ejercicios compatibles con las limitaciones que tú indicas» ([ADR 0007](adr/0007-motor-de-seleccion-de-ejercicios.md)). **Nunca** escribas, ni en la app, ni en el README, ni en los posts, ni en el App Store:

| Prohibido | Di en su lugar |
|---|---|
| «rehabilita», «rehabilitación» | «elige ejercicios compatibles con lo que indicas» |
| «alivia», «alivia el dolor» | «evita los movimientos que marcas como molestos» |
| «corrige» (un desequilibrio, la postura, la técnica) | «detecta un punto de mejora entre tus datos» |
| «previene lesiones» | «respeta los límites que declaras» |
| «detecta desequilibrios o atrofias» | «tu mapa muestra dónde tienes menos datos o menos fuerza relativa» |
| «seguro para tu lesión», «sin riesgo», «apto para tu lesión» | «compatible con la limitación que has indicado» |

Extensiones de la lista **[propuesta]**, por el mismo motivo: «trata», «cura», «diagnostica», «terapéutico», «tratamiento», «lesión curada», «libre de dopaje» (el escáner de suplementos dice «riesgo por ingredientes declarados» y si tiene sello Informed Sport/NSF).

Otras reglas de copia:

- Las reglas por palancas son **heurísticas de entrenadores, no ciencia**: dilo en la app y en el README. Nunca «unas palancas son malas».
- «Dolor» solo como dato que el usuario registra (escala 0-10), nunca como algo que Forja «trata».
- Datos de salud: junto a cada dato que se pide, «para qué lo usamos» (RGPD, art. 9). Las fotos de cuerpo y de etiquetas se procesan y se borran; solo quedan los ratios. Dilo así.
- Un test de copia (`grep` en CI sobre las cadenas de la app y el README) falla si aparece alguna palabra de la tabla **[propuesta]**.

## 7. Lo que Crimson Ink no cubre (resumen)

Movimiento, iconografía de interfaz, estados de componentes, tamaños táctiles, variantes claras de los cuatro colores de estado, paleta del mapa muscular y valor de `glow-primary` en claro: **[propuesta]** de este documento. Todo lo demás (colores base, tipografía, espaciado, radios, sombra y reglas de uso) sale literalmente de Crimson Ink.
