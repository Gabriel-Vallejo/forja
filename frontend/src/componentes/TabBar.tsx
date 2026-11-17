import { useMemo } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { letra, texto, type Theme, useTheme } from '../theme';
import { Icono, type NombreIcono } from './Icono';

export type IdPestana = 'hoy' | 'entreno' | 'ia' | 'progreso' | 'perfil';

export const PESTANAS: readonly { id: IdPestana; etiqueta: string }[] = [
  { id: 'hoy', etiqueta: 'Hoy' },
  { id: 'entreno', etiqueta: 'Entreno' },
  { id: 'ia', etiqueta: 'Forja IA' },
  { id: 'progreso', etiqueta: 'Progreso' },
  { id: 'perfil', etiqueta: 'Perfil' },
];

const ALTO_PESTANA = 64;
const CIRCULO_IA = 48;
const ELEVACION_IA = -14;

interface PropsTabBar {
  activo: IdPestana;
  alElegir: (id: IdPestana) => void;
}

/** Barra de pestañas del boceto (TabBar.dc.html), con Forja IA elevada en el centro. */
export function TabBar({ activo, alElegir }: PropsTabBar) {
  const tema = useTheme();
  const estilos = useMemo(() => crearEstilos(tema), [tema]);
  const { bottom } = useSafeAreaInsets();

  return (
    <View accessibilityRole="tablist" accessibilityLabel="Secciones" style={[estilos.barra, { paddingBottom: Math.max(tema.space.s3, bottom) }]}>
      {PESTANAS.map(({ id, etiqueta }) => {
        const elegida = id === activo;
        const color = elegida ? tema.color.primary : tema.color.onSurfaceVariant;
        const icono = <Icono nombre={id as NombreIcono} color={color} />;
        return (
          <Pressable
            key={id}
            accessibilityRole="tab"
            accessibilityLabel={etiqueta}
            accessibilityState={{ selected: elegida }}
            onPress={() => alElegir(id)}
            style={[estilos.pestana, id !== 'ia' && elegida && estilos.pestanaElegida]}
          >
            {id === 'ia' ? <View style={estilos.circuloIa}>{icono}</View> : icono}
            <Text style={[estilos.etiqueta, elegida && estilos.etiquetaElegida, { color }]}>{etiqueta}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    barra: {
      flexDirection: 'row',
      alignItems: 'stretch',
      paddingHorizontal: tema.space.s2,
      backgroundColor: tema.color.surfaceLow,
      borderTopWidth: tema.border.fino,
      borderTopColor: tema.color.outline,
    },
    pestana: {
      flex: 1,
      minHeight: ALTO_PESTANA,
      alignItems: 'center',
      justifyContent: 'center',
      gap: tema.space.s1,
      borderTopWidth: tema.border.marca,
      borderTopColor: 'transparent',
    },
    pestanaElegida: { borderTopColor: tema.color.primary },
    circuloIa: {
      width: CIRCULO_IA,
      height: CIRCULO_IA,
      marginTop: ELEVACION_IA,
      borderRadius: CIRCULO_IA / 2,
      backgroundColor: tema.color.surfaceHigh,
      borderWidth: tema.border.control,
      borderColor: tema.color.primary,
      alignItems: 'center',
      justifyContent: 'center',
    },
    etiqueta: { ...texto(tema, 'mini'), ...letra(tema, 'sans', '500') },
    etiquetaElegida: letra(tema, 'sans', '700'),
  });
}
