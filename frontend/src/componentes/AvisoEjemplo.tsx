import { StyleSheet, View } from 'react-native';

import { type Theme, useTheme } from '../theme';
import { Icono } from './Icono';
import { Texto } from './Texto';

const ICONO = 14;

/** Pastilla de «es una estimación» (design-system §5.7), aquí para avisar de que los datos son de ejemplo. */
export function AvisoEjemplo({ texto = 'Datos de ejemplo' }: { texto?: string }) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  return (
    <View accessible accessibilityRole="text" accessibilityLabel={texto} style={estilos.pastilla}>
      <Icono nombre="info" tam={ICONO} color={tema.color.info} />
      <Texto estilo="mini" tono="secundario">{texto}</Texto>
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    pastilla: {
      flexDirection: 'row',
      alignItems: 'center',
      alignSelf: 'flex-start',
      gap: tema.space.s15,
      paddingHorizontal: tema.space.s2,
      paddingVertical: tema.space.s1,
      borderRadius: tema.radius.sm,
      backgroundColor: tema.color.surfaceVariant,
    },
  });
}
