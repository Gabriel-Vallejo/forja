import { StatusBar } from 'expo-status-bar';
import { type ReactNode, useMemo } from 'react';
import { ScrollView, StyleSheet, Text } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { texto, type Theme, useTheme } from '../theme';

interface PropsPantalla {
  titulo?: string;
  children: ReactNode;
}

/** Pantalla con zona segura, fondo del tema y desplazamiento para textos grandes. */
export function Pantalla({ titulo, children }: PropsPantalla) {
  const tema = useTheme();
  const estilos = useMemo(() => crearEstilos(tema), [tema]);

  return (
    <SafeAreaView style={estilos.zonaSegura}>
      <StatusBar style={tema.mode === 'dark' ? 'light' : 'dark'} />
      <ScrollView contentContainerStyle={estilos.contenido} keyboardShouldPersistTaps="handled">
        {titulo ? (
          <Text accessibilityRole="header" style={estilos.titulo}>
            {titulo}
          </Text>
        ) : null}
        {children}
      </ScrollView>
    </SafeAreaView>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    zonaSegura: { flex: 1, backgroundColor: tema.color.background },
    contenido: { padding: tema.space.s4, gap: tema.space.s6 },
    titulo: { ...texto(tema, 'title'), color: tema.color.onBackground },
  });
}
