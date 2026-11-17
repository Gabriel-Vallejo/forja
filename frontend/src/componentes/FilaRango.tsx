import { useId } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import Svg, { ClipPath, Defs, G, Path, Rect } from 'react-native-svg';

import { type Theme, useTheme } from '../theme';
import { FRENTE, LIENZO } from './cuerpoTrazados';
import { type ColoresCuerpo, FiguraCuerpo } from './FiguraCuerpo';
import { Icono } from './Icono';
import { Texto } from './Texto';

// Hexágono de 60 × 64 con el torso dentro, recortado (boceto Progreso.dc.html, «Por zona»).
const ANCHO_HEX = 60;
const ALTO_HEX = 64;
const BORDE_HEX = 'M30 2 L57 17 L57 47 L30 62 L3 47 L3 17 Z';
// El hueco interior (3 px de margen) en el mismo lienzo, como el `clip-path` del boceto.
const HUECO_HEX = 'M30 3 L57 17.5 L57 46.5 L30 61 L3 46.5 L3 17.5 Z';
const ANCHO_TORSO = 78;
const MARGEN = 3;
const IZQUIERDA_TORSO = -12;
const ESCALA_TORSO = ANCHO_TORSO / LIENZO.ancho;
const CHEVRON = 20;

interface PropsFilaRango {
  nombre: string;
  rango: string;
  /** Color del borde del hexágono y del texto del rango; fondo y borde de la fila. */
  colores: { base: string; texto: string; fondo: string; borde: string };
  musculos: ColoresCuerpo;
  /** Cuánto sube el torso para encuadrar la zona. */
  desplazamiento: number;
  alPulsar: () => void;
}

export function FilaRango({ nombre, rango, colores, musculos, desplazamiento, alPulsar }: PropsFilaRango) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const recorte = `hex${useId().replace(/[^a-zA-Z0-9]/g, '')}`;
  const torso = `translate(${MARGEN + IZQUIERDA_TORSO} ${MARGEN + desplazamiento}) scale(${ESCALA_TORSO})`;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${nombre}: ${rango}`}
      onPress={alPulsar}
      style={({ pressed }) => [
        estilos.fila,
        { backgroundColor: colores.fondo, borderColor: colores.borde },
        pressed && estilos.pulsada,
      ]}
    >
      <View style={estilos.hex}>
        <Svg width={ANCHO_HEX} height={ALTO_HEX} viewBox={`0 0 ${ANCHO_HEX} ${ALTO_HEX}`}>
          <Defs>
            <ClipPath id={recorte}>
              <Path d={HUECO_HEX} />
            </ClipPath>
          </Defs>
          <G clipPath={`url(#${recorte})`}>
            <Rect x={0} y={0} width={ANCHO_HEX} height={ALTO_HEX} fill={tema.color.surfaceHigh} />
            <G transform={torso}>
              <FiguraCuerpo cara={FRENTE} lado="zona" colores={musculos} />
            </G>
          </G>
          <Path d={BORDE_HEX} fill="none" stroke={colores.base} strokeWidth={tema.border.marca} strokeLinejoin="round" />
        </Svg>
      </View>
      <View style={estilos.textos}>
        <Texto estilo="seccion">{nombre}</Texto>
        <Texto estilo="texto" peso="700" color={colores.texto}>{rango}</Texto>
      </View>
      <Icono nombre="chevron" tam={CHEVRON} color={tema.color.onSurfaceVariant} />
    </Pressable>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    fila: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: tema.space.s35,
      paddingVertical: tema.space.s25,
      paddingHorizontal: tema.space.s35,
      borderRadius: tema.radius.md,
      borderWidth: tema.border.fino,
    },
    pulsada: { opacity: tema.opacity.pulsado },
    hex: { width: ANCHO_HEX, height: ALTO_HEX },
    textos: { flex: 1, gap: tema.space.s075 },
  });
}
