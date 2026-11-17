import { View } from 'react-native';
import Svg, { G, Path, Rect, Text as TextoSvg } from 'react-native-svg';

import { useTheme } from '../theme';

// Emblema de rango del boceto aprobado (Emblema.dc.html): la forma va por nivel; llamas y corona, por grado.

export type TipoRango = 'hierro' | 'bronce' | 'plata' | 'oro' | 'acero' | 'titanio' | 'obsidiana' | 'diamante' | 'forjado';
export type Grado = 1 | 2 | 3;

export const TIPOS_RANGO: readonly TipoRango[] = [
  'hierro', 'bronce', 'plata', 'oro', 'acero', 'titanio', 'obsidiana', 'diamante', 'forjado',
];

export const NOMBRE_RANGO: Record<TipoRango, string> = {
  hierro: 'Hierro',
  bronce: 'Bronce',
  plata: 'Plata',
  oro: 'Oro',
  acero: 'Acero',
  titanio: 'Titanio',
  obsidiana: 'Obsidiana',
  diamante: 'Diamante',
  forjado: 'Forjado',
};

export const ROMANO: Record<Grado, string> = { 1: 'I', 2: 'II', 3: 'III' };

const HEX = 'M50 16 L82 34 L82 72 L50 90 L18 72 L18 34 Z';
const FORMA: Record<TipoRango, string> = {
  hierro: 'M50 16 L86 80 L14 80 Z',
  bronce: 'M27 22 H73 V52 C73 69 63 81 50 88 C37 81 27 69 27 52 Z',
  plata: 'M50 14 L86 54 L50 94 L14 54 Z',
  oro: 'M50 16 L86 42 L72 86 L28 86 L14 42 Z',
  acero: HEX,
  titanio: 'M37 17 L63 17 L83 37 L83 71 L63 91 L37 91 L17 71 L17 37 Z',
  obsidiana: 'M50 10 L61 40 L90 38 L68 58 L78 90 L50 72 L22 90 L32 58 L10 38 L39 40 Z',
  diamante: 'M30 22 L70 22 L86 42 L50 92 L14 42 Z',
  forjado: 'M50 14 A40 40 0 1 0 50.01 14 Z',
};

const LLAMA_GRANDE = 'M10 80 C -4 62, 0 38, 14 24 C 10 42, 16 56, 24 66 Z';
const LLAMA = 'M16 72 C 6 60, 8 46, 18 36 C 16 48, 22 56, 26 62 Z';
const ESTRELLA = 'M50 43 L53 50.6 L61 51 L54.8 56.1 L56.8 64 L50 59.6 L43.2 64 L45.2 56.1 L39 51 L47 50.6 Z';
const CORONA = 'M38 14 L42 2 L46 13 Z M45 12 L50 -4 L55 12 Z M54 13 L58 2 L62 14 Z';
const ESPEJO = 'translate(100 0) scale(-1 1)';
const CENTRO = 'translate(50 54) scale(%s) translate(-50 -54)';
const ESCALA_INTERIOR = '0.66';
const ESCALA_BRILLO = '0.84';
const TAM_POR_DEFECTO = 120;

export function etiquetaEmblema(tipo: TipoRango, grado: Grado): string {
  return tipo === 'forjado' ? 'Emblema Forjado' : `Emblema ${NOMBRE_RANGO[tipo]} ${ROMANO[grado]}`;
}

interface PropsEmblema {
  tipo: TipoRango;
  grado?: Grado;
  tam?: number;
}

export function Emblema({ tipo, grado = 3, tam = TAM_POR_DEFECTO }: PropsEmblema) {
  const tema = useTheme();
  const tinta = tema.color.background;
  const { base, oscuro, luz } = tema.rango[tipo];
  const forjado = tipo === 'forjado';
  const forma = FORMA[tipo];

  return (
    <View accessible accessibilityRole="image" accessibilityLabel={etiquetaEmblema(tipo, grado)} style={{ width: tam, height: tam }}>
      <Svg width={tam} height={tam} viewBox="-8 -8 116 116">
        {tipo === 'diamante' || forjado ? (
          <>
            {[-40, 40].map((angulo) => (
              <G key={angulo} transform={`rotate(${angulo} 50 54)`}>
                <Rect x={47} y={-2} width={6} height={96} rx={3} fill={tema.destaque.espadaFilo} stroke={tinta} strokeWidth={1.5} />
                <Rect x={43} y={80} width={14} height={5} rx={2} fill={tema.rango.acero.oscuro} />
              </G>
            ))}
          </>
        ) : null}
        {grado >= 3 || forjado ? <Llamas d={LLAMA_GRANDE} relleno={oscuro} tinta={tinta} /> : null}
        {grado >= 2 || forjado ? <Llamas d={LLAMA} relleno={luz} tinta={tinta} /> : null}
        <Path d={forma} fill={base} stroke={tinta} strokeWidth={3} strokeLinejoin="round" />
        <Path d={forma} fill={oscuro} transform={CENTRO.replace('%s', ESCALA_INTERIOR)} />
        <Path d={forma} fill="none" stroke={luz} strokeWidth={2.5} strokeLinejoin="round" opacity={0.9}
          transform={CENTRO.replace('%s', ESCALA_BRILLO)} />
        {forjado ? (
          <TextoSvg x={50} y={64} textAnchor="middle" fontFamily={tema.font.kanji} fontSize={tema.kanji.emblema} fill={luz}>
            鍛
          </TextoSvg>
        ) : (
          <Path d={ESTRELLA} fill={luz} stroke={tinta} strokeWidth={1.2} strokeLinejoin="round" />
        )}
        {grado >= 3 || forjado ? <Path d={CORONA} fill={luz} stroke={tinta} strokeWidth={1.5} strokeLinejoin="round" /> : null}
      </Svg>
    </View>
  );
}

function Llamas({ d, relleno, tinta }: { d: string; relleno: string; tinta: string }) {
  return (
    <>
      <Path d={d} fill={relleno} stroke={tinta} strokeWidth={2} strokeLinejoin="round" />
      <Path d={d} fill={relleno} stroke={tinta} strokeWidth={2} strokeLinejoin="round" transform={ESPEJO} />
    </>
  );
}
