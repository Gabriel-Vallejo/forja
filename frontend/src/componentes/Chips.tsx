import { StyleSheet, View } from 'react-native';

import { type Theme, useTheme } from '../theme';
import { Texto } from './Texto';

/** Fila de etiquetas pequeñas (músculos de un día o de un ejercicio). */
export function Chips({ textos }: { textos: readonly string[] }) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  if (textos.length === 0) return null;
  return (
    <View style={estilos.chips}>
      {textos.map((t) => (
        <View key={t} style={estilos.chip}>
          <Texto estilo="mini">{t}</Texto>
        </View>
      ))}
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    chips: { flexDirection: 'row', flexWrap: 'wrap', gap: tema.space.s15 },
    chip: {
      paddingHorizontal: tema.space.s2,
      paddingVertical: tema.space.s075,
      borderRadius: tema.radius.sm,
      borderWidth: tema.border.fino,
      borderColor: tema.color.outline,
      backgroundColor: tema.color.surfaceLow,
    },
  });
}
