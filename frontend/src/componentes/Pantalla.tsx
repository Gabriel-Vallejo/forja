import { StatusBar } from 'expo-status-bar';
import { type ReactNode, useMemo } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { type Edge, SafeAreaView } from 'react-native-safe-area-context';

import { texto, type Theme, useTheme } from '../theme';

type Hueco = keyof Theme['space'];

interface PropsPantalla {
  titulo?: string;
  /** Separación entre bloques (por defecto `s6`, la de Crimson Ink). */
  hueco?: Hueco;
  /** Sin desplazamiento: para pantallas que caben enteras y llevan un botón abajo del todo. */
  fija?: boolean;
  /** Las pestañas ya dejan su hueco abajo: sus pantallas solo piden el de arriba. */
  bordes?: readonly Edge[];
  fondo?: string;
  pie?: ReactNode;
  children: ReactNode;
}

const TODOS: readonly Edge[] = ['top', 'bottom', 'left', 'right'];

/** Pantalla con zona segura, fondo del tema y desplazamiento para textos grandes. */
export function Pantalla({ titulo, hueco = 's6', fija = false, bordes = TODOS, fondo, pie, children }: PropsPantalla) {
  const tema = useTheme();
  const estilos = useMemo(() => crearEstilos(tema), [tema]);
  const separacion = { gap: tema.space[hueco] };
  const cabecera = titulo ? (
    <Text accessibilityRole="header" style={estilos.titulo}>
      {titulo}
    </Text>
  ) : null;

  return (
    <SafeAreaView edges={bordes} style={[estilos.zonaSegura, fondo ? { backgroundColor: fondo } : null]}>
      <StatusBar style={tema.mode === 'dark' ? 'light' : 'dark'} />
      {fija ? (
        <View style={[estilos.contenido, estilos.fija, separacion]}>
          {cabecera}
          {children}
        </View>
      ) : (
        <ScrollView contentContainerStyle={[estilos.contenido, separacion]} keyboardShouldPersistTaps="handled">
          {cabecera}
          {children}
        </ScrollView>
      )}
      {pie ? <View style={estilos.pie}>{pie}</View> : null}
    </SafeAreaView>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    zonaSegura: { flex: 1, backgroundColor: tema.color.background },
    contenido: { padding: tema.space.s4 },
    fija: { flex: 1 },
    pie: { paddingHorizontal: tema.space.s4, paddingBottom: tema.space.s4, gap: tema.space.s2 },
    titulo: { ...texto(tema, 'title'), color: tema.color.onBackground },
  });
}
