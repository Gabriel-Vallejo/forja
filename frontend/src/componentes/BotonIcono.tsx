import { Pressable, StyleSheet } from 'react-native';

import { type Theme, useTheme } from '../theme';
import { Icono, type NombreIcono } from './Icono';

interface PropsBotonIcono {
  icono: NombreIcono;
  etiqueta: string;
  onPress: () => void;
  /** Con fondo redondo (cerrar del resultado del rango) o sin él (atrás). */
  relleno?: boolean;
}

/** Botón de solo icono de 44 × 44 con su etiqueta para el lector de pantalla. */
export function BotonIcono({ icono, etiqueta, onPress, relleno = false }: PropsBotonIcono) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={etiqueta}
      onPress={onPress}
      style={({ pressed }) => [estilos.base, relleno && estilos.relleno, pressed && estilos.pulsado]}
    >
      <Icono nombre={icono} />
    </Pressable>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    base: {
      width: tema.hit.icono,
      height: tema.hit.icono,
      borderRadius: tema.hit.icono / 2,
      alignItems: 'center',
      justifyContent: 'center',
    },
    relleno: { backgroundColor: tema.color.surfaceHigh },
    pulsado: { backgroundColor: tema.color.surfaceHighest },
  });
}
