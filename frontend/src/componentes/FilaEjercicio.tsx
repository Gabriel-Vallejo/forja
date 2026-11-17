import { Pressable, StyleSheet, View } from 'react-native';

import { type Theme, useTheme } from '../theme';
import { Icono } from './Icono';
import { Texto } from './Texto';

const LADO_NUMERO = 40;
const CHEVRON = 18;

interface PropsFilaEjercicio {
  numero: string;
  nombre: string;
  prescripcion: string;
  /** Línea de abajo (el porqué o qué toca); sin ella la fila queda en dos líneas. */
  nota?: string;
  /** Primera fila de la lista: sin separador encima. */
  primera?: boolean;
  alPulsar: () => void;
}

/** Fila numerada del plan (boceto Plan.dc.html). */
export function FilaEjercicio({ numero, nombre, prescripcion, nota, primera = false, alPulsar }: PropsFilaEjercicio) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${numero}. ${nombre}. ${prescripcion}${nota ? `. ${nota}` : ''}`}
      accessibilityHint="Abre el detalle del ejercicio"
      onPress={alPulsar}
      style={({ pressed }) => [estilos.fila, !primera && estilos.separada, pressed && estilos.pulsada]}
    >
      <View style={estilos.numero}>
        <Texto estilo="dataMedio" peso="700" tono="secundario">{numero}</Texto>
      </View>
      <View style={estilos.textos}>
        <Texto estilo="body" peso="700">{nombre}</Texto>
        <Texto estilo="dataMini" tono="secundario">{prescripcion}</Texto>
        {nota ? <Texto estilo="mini" tono="secundario">{nota}</Texto> : null}
      </View>
      <Icono nombre="chevron" tam={CHEVRON} color={tema.color.onSurfaceVariant} />
    </Pressable>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    fila: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: tema.space.s3,
      paddingVertical: tema.space.s3,
      paddingHorizontal: tema.space.s35,
    },
    separada: { borderTopWidth: tema.border.fino, borderTopColor: tema.color.surfaceVariant },
    pulsada: { backgroundColor: tema.color.surfaceHigh },
    numero: {
      width: LADO_NUMERO,
      height: LADO_NUMERO,
      borderRadius: tema.radius.sm,
      backgroundColor: tema.color.surfaceHigh,
      alignItems: 'center',
      justifyContent: 'center',
    },
    textos: { flex: 1, gap: tema.space.s075 },
  });
}
