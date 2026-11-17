import { Pressable, StyleSheet, View } from 'react-native';

import { type Theme, useTheme } from '../theme';
import { Icono } from './Icono';
import { Texto } from './Texto';

const CHEVRON = 18;

interface PropsFilaEnlace {
  etiqueta: string;
  detalle?: string;
  alPulsar: () => void;
}

/** Fila que lleva a otra pantalla, con su detalle debajo. */
export function FilaEnlace({ etiqueta, detalle, alPulsar }: PropsFilaEnlace) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={etiqueta}
      accessibilityHint={detalle}
      onPress={alPulsar}
      style={({ pressed }) => [estilos.fila, pressed && estilos.pulsada]}
    >
      <View style={estilos.textos}>
        <Texto peso="700">{etiqueta}</Texto>
        {detalle ? <Texto estilo="nota" tono="secundario">{detalle}</Texto> : null}
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
      gap: tema.space.s3,
      minHeight: tema.hit.min,
      padding: tema.space.s35,
      borderRadius: tema.radius.md,
      backgroundColor: tema.color.surface,
    },
    pulsada: { backgroundColor: tema.color.surfaceHigh },
    textos: { flex: 1, gap: tema.space.s05 },
  });
}
