import { StyleSheet, View } from 'react-native';

import type { Nivel } from '../api/esquemas';
import { type Theme, useTheme } from '../theme';
import { Texto } from './Texto';

// Letra y palabra, nunca solo color: la evidencia no es «bien» ni «mal» (design-system §5.2).
const VISIBLE: Record<Nivel, string> = {
  A: 'Evidencia A',
  B: 'Evidencia B',
  C: 'Evidencia C',
  D: 'Evidencia D',
  H: 'Heurística',
  P: 'Propuesta propia',
};

const LEYENDA: Record<Nivel, string> = {
  A: 'evidencia A de la fuente',
  B: 'evidencia B de la fuente',
  C: 'evidencia C de la fuente',
  D: 'evidencia D de la fuente',
  H: 'heurística de entrenadores, no ciencia',
  P: 'propuesta propia de Forja',
};

/** Insignia del nivel de una regla (ADR 0007), con su leyenda para el lector de pantalla. */
export function InsigniaNivel({ nivel }: { nivel: Nivel }) {
  const estilos = crearEstilos(useTheme());
  return (
    <View style={estilos.insignia} accessible accessibilityLabel={`Nivel ${nivel}: ${LEYENDA[nivel]}`}>
      <Texto estilo="mini">{VISIBLE[nivel]}</Texto>
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    insignia: {
      paddingHorizontal: tema.space.s15,
      paddingVertical: tema.space.s05,
      borderRadius: tema.radius.sm,
      backgroundColor: tema.color.surfaceVariant,
      borderWidth: tema.border.control,
      borderColor: tema.color.onSurfaceVariant,
    },
  });
}
