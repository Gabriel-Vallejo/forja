// Único sitio con valores de diseño: Crimson Ink (docs/design-system.md §4). Oscuro por defecto.
// Lo marcado [propuesta] no existe en Crimson Ink y lo propone Forja.
const base = {
  // s05…s5 [propuesta]: huecos del boceto aprobado que no están en Crimson Ink
  space: { s05: 2, s075: 3, s1: 4, s15: 6, s2: 8, s25: 10, s3: 12, s35: 14, s4: 16, s45: 18, s5: 20, s6: 24, s12: 48 },
  // xs, mini, burbuja y hoja [propuesta]: radios del boceto
  radius: { celda: 3, xs: 4, sm: 6, mini: 10, md: 12, burbuja: 14, hoja: 20, xl: 112 },
  // Un archivo por peso: con fuentes propias, Android no sintetiza los pesos a partir de `fontWeight`.
  font: {
    sans: { '400': 'Inter_400Regular', '500': 'Inter_500Medium', '700': 'Inter_700Bold', '800': 'Inter_800ExtraBold' },
    mono: { '400': 'JetBrainsMono_400Regular', '700': 'JetBrainsMono_700Bold' },
    kanji: 'NotoSerifJP_900Black',
  },
  // [propuesta] tamaños del kanji de acento del boceto (el `kanji-hero` de Crimson Ink es `xxl`)
  kanji: { s: 16, sm: 22, m: 24, emblema: 28, l: 72, xl: 110, xxl: 120 },
  // [propuesta] Crimson Ink no define movimiento; `giro` es el del cuerpo del boceto
  motion: { fast: 120, base: 200, slow: 320, giro: 480 },
  // [propuesta] objetivo táctil mínimo en dp; `icono` es el botón redondo del boceto
  hit: { icono: 44, min: 48, cta: 56, confirm: 72 },
  // [propuesta] grosor de bordes (§5.1 y §5.5) y opacidad de lo deshabilitado (§5)
  border: { fino: 1, control: 1.5, foco: 2, marca: 3 },
  opacity: { deshabilitado: 0.4, pulsado: 0.8 },
  type: {
    display: { family: 'sans', size: 40, line: 44, weight: '800' },
    title: { family: 'sans', size: 30, line: 34.5, weight: '800' },
    heading: { family: 'sans', size: 20, line: 26, weight: '700' },
    body: { family: 'sans', size: 15, line: 22.5, weight: '400' },
    label: { family: 'sans', size: 12, line: 16.8, weight: '500', letterSpacing: 7, upper: true },
    data: { family: 'mono', size: 13, line: 19.5, weight: '400', tabular: true },
    // [propuesta] Estilos del boceto aprobado (2026-11-17) que Crimson Ink no tiene.
    rango: { family: 'sans', size: 54, line: 54, weight: '800', letterSpacing: -1 },
    hero: { family: 'sans', size: 52, line: 49.4, weight: '800', letterSpacing: -1 },
    rangoMedio: { family: 'sans', size: 34, line: 34, weight: '800' },
    titular: { family: 'sans', size: 32, line: 32, weight: '800' },
    ejercicio: { family: 'sans', size: 26, line: 27.3, weight: '800', letterSpacing: -0.3 },
    pantalla: { family: 'sans', size: 24, line: 28.8, weight: '800' },
    subtitulo: { family: 'sans', size: 22, line: 26.4, weight: '800' },
    destacado: { family: 'sans', size: 18, line: 23.4, weight: '700' },
    marca: { family: 'sans', size: 18, line: 22, weight: '800', letterSpacing: 4 },
    seccion: { family: 'sans', size: 17, line: 22, weight: '700' },
    texto: { family: 'sans', size: 14, line: 21, weight: '400' },
    nota: { family: 'sans', size: 13, line: 19.5, weight: '400' },
    mini: { family: 'sans', size: 12, line: 16.8, weight: '400' },
    micro: { family: 'sans', size: 10, line: 12, weight: '500' },
    etiqueta: { family: 'sans', size: 11, line: 15.4, weight: '500', letterSpacing: 5, upper: true },
    etiquetaEstrecha: { family: 'sans', size: 11, line: 15.4, weight: '500', letterSpacing: 3, upper: true },
    etiquetaDia: { family: 'sans', size: 11, line: 15.4, weight: '500', letterSpacing: 2, upper: true },
    dataMini: { family: 'mono', size: 12, line: 18, weight: '400', tabular: true },
    dataMedio: { family: 'mono', size: 14, line: 21, weight: '400', tabular: true },
    cifraPequena: { family: 'mono', size: 22, line: 26, weight: '700', tabular: true },
    cifraMedia: { family: 'mono', size: 28, line: 32, weight: '700', tabular: true },
    cifra: { family: 'mono', size: 44, line: 48, weight: '700', tabular: true },
    cifraGigante: { family: 'mono', size: 72, line: 72, weight: '700', tabular: true },
  },
  // [propuesta] Paletas del boceto aprobado. Solo existen en oscuro: el tema claro las hereda tal cual.
  // Rangos: base, oscuro y luz son los del emblema; texto, fondo y borde, los de su fila y su panel.
  rango: {
    hierro: { base: '#7a7a84', oscuro: '#4a4a52', luz: '#c4c4cc', texto: '#c4c4cc', fondo: '#18181b', borde: '#3a3a42' },
    bronce: { base: '#b8703f', oscuro: '#7a4424', luz: '#e0a07a', texto: '#e0a07a', fondo: '#1d1611', borde: '#5a3a22' },
    plata: { base: '#c9ccd3', oscuro: '#7e828c', luz: '#f2f3f6', texto: '#f2f3f6', fondo: '#1a1b1e', borde: '#55585f' },
    oro: { base: '#e8b04b', oscuro: '#a0721c', luz: '#f8dc96', texto: '#f8dc96', fondo: '#1f1a10', borde: '#5c4720' },
    acero: { base: '#e5383b', oscuro: '#8c1f22', luz: '#ff9a9c', texto: '#ff9a9c', fondo: '#211315', borde: '#5a1c1f' },
    titanio: { base: '#2fa596', oscuro: '#17665c', luz: '#8ce0d4', texto: '#8ce0d4', fondo: '#111d1b', borde: '#1d5049' },
    obsidiana: { base: '#8f6ad8', oscuro: '#52348c', luz: '#cdb8f5', texto: '#cdb8f5', fondo: '#19151f', borde: '#46356b' },
    diamante: { base: '#cfe9f1', oscuro: '#6f98a8', luz: '#ffffff', texto: '#e4f4f9', fondo: '#151b1d', borde: '#46606a' },
    forjado: { base: '#ff5a36', oscuro: '#a3241a', luz: '#ffd2c4', texto: '#ffd2c4', fondo: '#221315', borde: '#e5383b' },
    sin: { base: '#2a2a30', oscuro: '#2a2a30', luz: '#bdb8b3', texto: '#bdb8b3', fondo: '#18181b', borde: '#2e2e34' },
  },
  // Destacados del rango global (tarjeta de Progreso y Perfil, resultado del rango).
  destaque: {
    titulo: '#ff6b6b',
    fondo: '#221315',
    resultado: '#0f0b0c',
    circulo: '#160f10',
    curva: '#1a1012',
    espadaFilo: '#ece8e4',
  },
  cuerpo: { piel: '#121215', sinColor: '#2a2a30', neutro: '#5a5a62', trazo: '#ece8e4', trazoSeleccion: '#ffffff' },
  regla: { marcaMenor: '#6a6a72' },
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

type Tema = typeof dark | typeof light;
export type Theme = Tema;
export const defaultTheme: Theme = dark; // oscuro por defecto

// shortcut: siempre oscuro; cuando haya ajuste de tema, esto pasa a un contexto.
export function useTheme(): Theme {
  return defaultTheme;
}

export type EstiloTexto = keyof Theme['type'];
export type Familia = 'sans' | 'mono';
type PesoDe<F extends Familia> = keyof Theme['font'][F];

/** La familia de un peso concreto (`fontFamily` ya lleva el peso: no se usa `fontWeight`). */
export function letra<F extends Familia>(tema: Theme, familia: F, peso: PesoDe<F>): { fontFamily: string } {
  return { fontFamily: (tema.font[familia] as Record<string, string>)[peso as string] ?? tema.font.sans['400'] };
}

/** Familia, tamaño, interlineado y espaciado de un estilo de texto, listos para un `StyleSheet`. */
export function texto(tema: Theme, estilo: EstiloTexto) {
  const t = tema.type[estilo];
  return {
    ...letra(tema, t.family, t.weight as never),
    fontSize: t.size,
    lineHeight: t.line,
    ...('letterSpacing' in t ? { letterSpacing: t.letterSpacing } : {}),
    ...('upper' in t ? { textTransform: 'uppercase' as const } : {}),
    ...('tabular' in t ? { fontVariant: ['tabular-nums' as const] } : {}),
  };
}
