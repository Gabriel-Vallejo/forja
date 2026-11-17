import { StyleSheet, View } from 'react-native';

import { type Theme, useTheme } from '../theme';
import { Boton } from './Boton';
import { Chips } from './Chips';
import { Cuerpo } from './Cuerpo';
import type { ColoresCuerpo } from './FiguraCuerpo';
import { Texto } from './Texto';

export const ANCHO_TARJETA = 330;
const ALTO_TARJETA = 392;
const ANCHO_CUERPO = 88;
const CUERPO_ARRIBA = 22;
const CUERPO_DERECHA = 12;

interface PropsTarjetaSesion {
  /** Pastilla de arriba («Hoy · lunes»); solo en la sesión de hoy. */
  cuando?: string;
  etiqueta: string;
  titulo: string;
  datos?: string;
  musculos?: readonly string[];
  colores?: ColoresCuerpo;
  /** Cara del cuerpo que se dibuja. */
  cara?: 'frente' | 'espalda';
  /** Sin él, la tarjeta es la de un día siguiente: apagada y sin botón. */
  alEmpezar?: () => void;
}

/** Tarjeta de la semana en Hoy (boceto Hoy.dc.html). */
export function TarjetaSesion({ cuando, etiqueta, titulo, datos, musculos = [], colores, cara, alEmpezar }: PropsTarjetaSesion) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const hoy = alEmpezar !== undefined;

  return (
    <View accessibilityLabel={`${etiqueta}: ${titulo}`} style={[estilos.tarjeta, !hoy && estilos.apagada]}>
      {cuando ? (
        <View style={estilos.cuando}>
          <Texto estilo="etiquetaDia">{cuando}</Texto>
        </View>
      ) : null}
      {hoy ? (
        <View style={estilos.cuerpo}>
          <Cuerpo ancho={ANCHO_CUERPO} girable={false} colores={colores} cara={cara} decorativo />
        </View>
      ) : null}
      <View style={estilos.pie}>
        <Texto estilo="etiqueta" tono="secundario">{etiqueta}</Texto>
        <Texto estilo="hero" tono={hoy ? 'principal' : 'secundario'}>{titulo}</Texto>
        {datos ? <Texto estilo="data" tono="secundario">{datos}</Texto> : null}
        <Chips textos={musculos} />
        {alEmpezar ? <Boton etiqueta="Empezar sesión" icono="play" grande onPress={alEmpezar} /> : null}
      </View>
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    tarjeta: {
      width: ANCHO_TARJETA,
      height: ALTO_TARJETA,
      padding: tema.space.s45,
      borderRadius: tema.radius.md,
      backgroundColor: tema.color.surface,
      overflow: 'hidden',
      justifyContent: 'flex-end',
    },
    apagada: { backgroundColor: tema.color.surfaceLow },
    cuando: {
      position: 'absolute',
      top: tema.space.s45,
      left: tema.space.s45,
      paddingHorizontal: tema.space.s2,
      paddingVertical: tema.space.s1,
      borderRadius: tema.radius.sm,
      backgroundColor: tema.color.surfaceVariant,
    },
    cuerpo: { position: 'absolute', top: CUERPO_ARRIBA, right: CUERPO_DERECHA },
    pie: { gap: tema.space.s25 },
  });
}
