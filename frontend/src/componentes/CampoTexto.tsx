import { useMemo } from 'react';
import { StyleSheet, Text, TextInput, type TextInputProps, View } from 'react-native';

import { letra, texto, type Theme, useTheme } from '../theme';

interface PropsCampoTexto
  extends Pick<TextInputProps, 'secureTextEntry' | 'keyboardType' | 'autoComplete' | 'textContentType'> {
  etiqueta: string;
  valor: string;
  alCambiar: (valor: string) => void;
  /** Mensaje bajo el campo; con él, el borde cambia y el lector de pantalla lo anuncia. */
  error?: string;
  ayuda?: string;
}

/** Campo con etiqueta visible, ayuda y error con palabra, no solo con color (§5.3). */
export function CampoTexto({ etiqueta, valor, alCambiar, error, ayuda, ...resto }: PropsCampoTexto) {
  const tema = useTheme();
  const estilos = useMemo(() => crearEstilos(tema), [tema]);

  return (
    <View style={estilos.contenedor}>
      <Text style={estilos.etiqueta}>{etiqueta}</Text>
      <TextInput
        accessibilityLabel={etiqueta}
        accessibilityHint={error ?? ayuda}
        value={valor}
        onChangeText={alCambiar}
        autoCapitalize="none"
        autoCorrect={false}
        placeholderTextColor={tema.color.onSurfaceVariant}
        selectionColor={tema.color.primary}
        style={[estilos.campo, error ? estilos.campoConError : null]}
        {...resto}
      />
      {ayuda && !error ? <Text style={estilos.ayuda}>{ayuda}</Text> : null}
      {error ? (
        <Text accessibilityRole="alert" accessibilityLiveRegion="polite" style={estilos.error}>
          {`Error: ${error}`}
        </Text>
      ) : null}
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    contenedor: { gap: tema.space.s2 },
    etiqueta: { ...texto(tema, 'body'), ...letra(tema, 'sans', '700'), color: tema.color.onBackground },
    campo: {
      ...texto(tema, 'body'),
      minHeight: tema.hit.min,
      paddingHorizontal: tema.space.s4,
      borderRadius: tema.radius.sm,
      borderWidth: tema.border.control,
      borderColor: tema.color.onSurfaceVariant,
      backgroundColor: tema.color.surfaceVariant,
      color: tema.color.onBackground,
    },
    campoConError: { borderColor: tema.color.error, borderWidth: tema.border.foco },
    ayuda: { ...texto(tema, 'body'), color: tema.color.onSurfaceVariant },
    error: { ...texto(tema, 'body'), color: tema.color.error },
  });
}
