import type { ReactNode } from 'react';
import { Text } from 'react-native';

import { texto, useTheme } from '../theme';

interface PropsTexto {
  estilo?: 'heading' | 'body' | 'data';
  /** `secundario` = `onSurfaceVariant`, para lo que acompaña al dato principal. */
  tono?: 'principal' | 'secundario';
  encabezado?: boolean;
  children: ReactNode;
}

/** Texto con un estilo del tema; nada de tamaños ni colores sueltos en las pantallas. */
export function Texto({ estilo = 'body', tono = 'principal', encabezado = false, children }: PropsTexto) {
  const tema = useTheme();
  const color = tono === 'principal' ? tema.color.onBackground : tema.color.onSurfaceVariant;
  return (
    <Text accessibilityRole={encabezado ? 'header' : undefined} style={[texto(tema, estilo), { color }]}>
      {children}
    </Text>
  );
}
