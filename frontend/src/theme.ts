// Único sitio con valores de diseño: Crimson Ink (docs/design-system.md §4). Oscuro por defecto.
// Lo marcado [propuesta] no existe en Crimson Ink y lo propone Forja.
const base = {
  space: { s1: 4, s2: 8, s4: 16, s6: 24, s12: 48 },
  radius: { sm: 6, md: 12, xl: 112 },
  // Las fuentes se cargan en otro bloque; hasta entonces el texto usa la del sistema.
  font: {
    sans: 'Inter',
    mono: 'JetBrainsMono',
    kanji: 'NotoSerifJP',
  },
  // [propuesta] Crimson Ink no define movimiento
  motion: { fast: 120, base: 200, slow: 320 },
  // [propuesta] objetivo táctil mínimo en dp; el botón de confirmar serie es mayor
  hit: { min: 48, confirm: 72 },
  // [propuesta] grosor de bordes (§5.1 y §5.5) y opacidad de lo deshabilitado (§5)
  border: { control: 1.5, foco: 2 },
  opacity: { deshabilitado: 0.4 },
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

type Tema = typeof dark | typeof light;
export type Theme = Tema;
export const defaultTheme: Theme = dark; // oscuro por defecto

// shortcut: siempre oscuro; cuando haya ajuste de tema, esto pasa a un contexto.
export function useTheme(): Theme {
  return defaultTheme;
}

type EstiloTexto = keyof Theme['type'];

/** Tamaño, interlineado y peso de un estilo de texto, listos para un `StyleSheet`. */
export function texto(tema: Theme, estilo: EstiloTexto) {
  const t = tema.type[estilo];
  return {
    fontSize: t.size,
    lineHeight: t.line,
    fontWeight: t.weight,
    ...('tabular' in t ? { fontVariant: ['tabular-nums' as const] } : {}),
  };
}
