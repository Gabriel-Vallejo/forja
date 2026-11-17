import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { texto, type Theme, useTheme } from '../theme';
import { AvisoEjemplo } from './AvisoEjemplo';
import { Texto } from './Texto';

interface PropsCabecera {
  /** Semanas de racha; sin ellas no sale la insignia. */
  racha?: number;
  alPulsarRacha?: () => void;
  /** La racha aún es de ejemplo: la pastilla lo dice al lado. */
  ejemplo?: boolean;
  derecha?: ReactNode;
}

const INTERLINEADO_KANJI = 1.4;

/** Marca 鍛 FORJA con la racha a la derecha (cabecera de Hoy, Progreso y Perfil). */
export function Cabecera({ racha, alPulsarRacha, ejemplo = false, derecha }: PropsCabecera) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  return (
    <View style={estilos.fila}>
      <View accessible accessibilityRole="header" accessibilityLabel="Forja" style={estilos.marca}>
        <Text style={estilos.kanji}>鍛</Text>
        <Texto estilo="marca">FORJA</Texto>
      </View>
      <View style={estilos.derecha}>
        {ejemplo ? <AvisoEjemplo /> : null}
        {racha !== undefined ? (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`Racha de ${racha} semanas`}
            onPress={alPulsarRacha}
            style={({ pressed }) => [estilos.racha, pressed && estilos.pulsado]}
          >
            <Text style={estilos.kanjiRacha}>炎</Text>
            <Texto estilo="data" peso="700">{racha}</Texto>
          </Pressable>
        ) : null}
        {derecha}
      </View>
    </View>
  );
}

function crearEstilos(tema: Theme) {
  const kanji = { fontFamily: tema.font.kanji, color: tema.color.primary };
  return StyleSheet.create({
    fila: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
    marca: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s2 },
    kanji: { ...kanji, fontSize: tema.kanji.m, lineHeight: tema.kanji.m * INTERLINEADO_KANJI },
    derecha: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s2 },
    racha: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: tema.space.s15,
      minHeight: tema.hit.icono,
      paddingHorizontal: tema.space.s25,
      borderRadius: tema.radius.sm,
      backgroundColor: tema.color.surface,
    },
    pulsado: { backgroundColor: tema.color.surfaceHigh },
    kanjiRacha: { ...kanji, ...texto(tema, 'data'), fontFamily: tema.font.kanji, fontSize: tema.kanji.s },
  });
}
