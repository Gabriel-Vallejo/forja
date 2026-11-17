import { useMemo } from 'react';
import { Pressable, StyleSheet, Text } from 'react-native';

import { texto, type Theme, useTheme } from '../theme';

export type VarianteBoton = 'principal' | 'secundario' | 'texto';

interface PropsBoton {
  etiqueta: string;
  onPress: () => void;
  variante?: VarianteBoton;
  deshabilitado?: boolean;
  /** Mientras es verdadero, el botón no responde y dice `etiquetaCargando` (§5: nunca solo un spinner). */
  cargando?: boolean;
  etiquetaCargando?: string;
}

/** Botón de Crimson Ink (docs/design-system.md §5.1). Solo un principal por zona. */
export function Boton({
  etiqueta,
  onPress,
  variante = 'principal',
  deshabilitado = false,
  cargando = false,
  etiquetaCargando = 'Calculando…',
}: PropsBoton) {
  const tema = useTheme();
  const estilos = useMemo(() => crearEstilos(tema), [tema]);
  const inactivo = deshabilitado || cargando;
  const visible = cargando ? etiquetaCargando : etiqueta;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={visible}
      accessibilityState={{ disabled: inactivo, busy: cargando }}
      disabled={inactivo}
      onPress={onPress}
      style={({ pressed }) => [
        estilos.base,
        estilos[variante],
        pressed && estilos[`${variante}Pulsado`],
        deshabilitado && estilos.deshabilitado,
      ]}
    >
      <Text style={[estilos.etiqueta, estilos[`etiqueta_${variante}`]]}>{visible}</Text>
    </Pressable>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    base: {
      minHeight: tema.hit.min,
      minWidth: tema.hit.min,
      borderRadius: tema.radius.sm,
      paddingHorizontal: tema.space.s4,
      alignItems: 'center',
      justifyContent: 'center',
    },
    principal: { backgroundColor: tema.color.primary },
    principalPulsado: { backgroundColor: tema.color.secondary },
    secundario: { borderWidth: tema.border.control, borderColor: tema.color.onSurfaceVariant },
    secundarioPulsado: { backgroundColor: tema.color.surfaceHigh },
    texto: {},
    textoPulsado: { backgroundColor: tema.color.surfaceHigh },
    deshabilitado: { opacity: tema.opacity.deshabilitado },
    etiqueta: { ...texto(tema, 'body'), fontWeight: '700', textAlign: 'center' },
    etiqueta_principal: { color: tema.color.onPrimary },
    etiqueta_secundario: { color: tema.color.onBackground },
    etiqueta_texto: { color: tema.color.onBackground, textDecorationLine: 'underline' },
  });
}
