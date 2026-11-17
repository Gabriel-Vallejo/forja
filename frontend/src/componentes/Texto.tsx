import type { ReactNode } from 'react';
import { type StyleProp, Text, type TextStyle } from 'react-native';

import { type EstiloTexto, letra, texto, useTheme } from '../theme';

type Peso = '400' | '500' | '700' | '800';

interface PropsTexto {
  estilo?: EstiloTexto;
  /** `secundario` = `onSurfaceVariant`, para lo que acompaña al dato principal. */
  tono?: 'principal' | 'secundario';
  /** Un color del tema que no sea de los dos tonos (rangos, acento). */
  color?: string;
  /** Otro peso de la misma familia del estilo (el mono solo tiene 400 y 700). */
  peso?: Peso;
  centrado?: boolean;
  encabezado?: boolean;
  lineas?: number;
  estiloExtra?: StyleProp<TextStyle>;
  children: ReactNode;
}

/** Texto con un estilo del tema; nada de tamaños ni colores sueltos en las pantallas. */
export function Texto({
  estilo = 'body',
  tono = 'principal',
  color,
  peso,
  centrado = false,
  encabezado = false,
  lineas,
  estiloExtra,
  children,
}: PropsTexto) {
  const tema = useTheme();
  const base = color ?? (tono === 'principal' ? tema.color.onBackground : tema.color.onSurfaceVariant);
  const familia = tema.type[estilo].family;
  return (
    <Text
      accessibilityRole={encabezado ? 'header' : undefined}
      numberOfLines={lineas}
      style={[
        texto(tema, estilo),
        peso ? letra(tema, familia, peso as never) : null,
        { color: base },
        centrado ? { textAlign: 'center' } : null,
        estiloExtra,
      ]}
    >
      {children}
    </Text>
  );
}
