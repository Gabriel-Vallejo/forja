import { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { letra, texto, type Theme, useTheme } from '../theme';

export type TipoAviso = 'error' | 'aviso' | 'info' | 'exito';

const PALABRA: Record<TipoAviso, string> = {
  error: 'Error',
  aviso: 'Aviso',
  info: 'Información',
  exito: 'Hecho',
};

interface PropsAviso {
  tipo: TipoAviso;
  mensaje: string;
}

/** Aviso con palabra y borde del color de estado: nunca solo color (§1.5). */
export function Aviso({ tipo, mensaje }: PropsAviso) {
  const tema = useTheme();
  const estilos = useMemo(() => crearEstilos(tema), [tema]);
  const color = colorDe(tema, tipo);
  const urgente = tipo === 'error' || tipo === 'aviso';

  return (
    <View
      accessible
      accessibilityRole={urgente ? 'alert' : 'text'}
      accessibilityLabel={`${PALABRA[tipo]}: ${mensaje}`}
      accessibilityLiveRegion={urgente ? 'assertive' : 'polite'}
      style={[estilos.contenedor, { borderColor: color }]}
    >
      <Text style={[estilos.palabra, { color }]}>{PALABRA[tipo]}</Text>
      <Text style={estilos.mensaje}>{mensaje}</Text>
    </View>
  );
}

function colorDe(tema: Theme, tipo: TipoAviso): string {
  switch (tipo) {
    case 'error':
      return tema.color.error;
    case 'aviso':
      return tema.color.warning;
    case 'info':
      return tema.color.info;
    case 'exito':
      return tema.color.success;
  }
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    contenedor: {
      gap: tema.space.s1,
      padding: tema.space.s4,
      borderRadius: tema.radius.md,
      borderWidth: tema.border.foco,
      backgroundColor: tema.color.surfaceHigh,
    },
    palabra: { ...texto(tema, 'body'), ...letra(tema, 'sans', '700') },
    mensaje: { ...texto(tema, 'body'), color: tema.color.onBackground },
  });
}
