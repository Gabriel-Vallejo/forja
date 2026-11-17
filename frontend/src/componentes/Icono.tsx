import Svg, { Circle, Path, Rect } from 'react-native-svg';

import { useTheme } from '../theme';

// Iconos de trazo del boceto aprobado (24 × 24, trazo 2, extremos redondeados).
type Pieza = { d: string } | { circulo: readonly [number, number, number] } | { rect: readonly [number, number] };

const ICONOS = {
  atras: [{ d: 'M15 5l-7 7 7 7' }],
  flecha: [{ d: 'M5 12h14M13 6l6 6-6 6' }],
  chevron: [{ d: 'M9 6l6 6-6 6' }],
  cerrar: [{ d: 'M6 6l12 12M18 6L6 18' }],
  girar: [{ d: 'M20 12a8 8 0 1 1-3-6.2' }, { d: 'M20 4v5h-5' }],
  mas: [{ d: 'M12 5v14M5 12h14' }],
  check: [{ d: 'M5 12l5 5 9-10' }],
  enviar: [{ d: 'M4 12l16-8-6 16-2-7z' }],
  info: [{ circulo: [12, 12, 9] }, { d: 'M12 11v5M12 8h.01' }],
  ajustes: [
    { circulo: [12, 12, 3] },
    { d: 'M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1' },
  ],
  papelera: [{ d: 'M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3' }],
  plantillas: [{ rect: [4, 4] }, { rect: [13, 4] }, { rect: [4, 13] }, { rect: [13, 13] }],
  ajustar: [{ d: 'M4 7h10M4 12h16M4 17h7' }, { circulo: [17, 7, 2] }, { circulo: [14, 17, 2] }],
  hoy: [{ d: 'M3 11l9-8 9 8v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z' }],
  entreno: [{ d: 'M6 6v12M18 6v12M3 9v6M21 9v6M6 12h12' }],
  ia: [{ d: 'M4 5h16v11H10l-5 4v-4H4z' }, { d: 'M8 10.5h8' }],
  progreso: [{ d: 'M4 20h16M5 16l4-5 4 3 6-8' }],
  perfil: [{ circulo: [12, 8, 4] }, { d: 'M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6' }],
} as const satisfies Record<string, readonly Pieza[]>;

const LADO_PLANTILLA = 7;
const VISTA = '0 0 24 24';
const TAM_POR_DEFECTO = 24;
const TRAZO = 2;

export type NombreIcono = keyof typeof ICONOS | 'play';

interface PropsIcono {
  nombre: NombreIcono;
  tam?: number;
  color?: string;
  trazo?: number;
}

/** Icono decorativo: quien lo usa pone la etiqueta accesible en el control. */
export function Icono({ nombre, tam = TAM_POR_DEFECTO, color, trazo = TRAZO }: PropsIcono) {
  const tema = useTheme();
  const c = color ?? tema.color.onBackground;
  if (nombre === 'play') {
    return (
      <Svg width={tam} height={tam} viewBox={VISTA} accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
        <Path d="M8 5v14l11-7z" fill={c} />
      </Svg>
    );
  }
  const piezas: readonly Pieza[] = ICONOS[nombre];
  return (
    <Svg
      width={tam}
      height={tam}
      viewBox={VISTA}
      fill="none"
      stroke={c}
      strokeWidth={trazo}
      strokeLinecap="round"
      strokeLinejoin="round"
      accessibilityElementsHidden
      importantForAccessibility="no-hide-descendants"
    >
      {piezas.map((p, i) => {
        if ('d' in p) return <Path key={i} d={p.d} />;
        if ('circulo' in p) return <Circle key={i} cx={p.circulo[0]} cy={p.circulo[1]} r={p.circulo[2]} />;
        return <Rect key={i} x={p.rect[0]} y={p.rect[1]} width={LADO_PLANTILLA} height={LADO_PLANTILLA} rx={1} />;
      })}
    </Svg>
  );
}
