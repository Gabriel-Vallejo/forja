import { StyleSheet, View } from 'react-native';

import type { Musculo } from '../api/esquemas';
import { coloresCuerpo } from '../plan/musculos';
import { useTheme } from '../theme';
import { Cuerpo } from './Cuerpo';

/** Alto de la figura entera respecto a su ancho (el lienzo del cuerpo es el doble de alto que de ancho). */
const PROPORCION = 2;

interface PropsCuerpoDoble {
  musculos: readonly Musculo[];
  /** Ancho de cada figura. */
  ancho: number;
  /** Hueco entre el frente y la espalda. */
  hueco?: number;
}

/** Frente y espalda lado a lado, cada una con los músculos de su cara resaltados: así no se esconde ninguno. */
export function CuerpoDoble({ musculos, ancho, hueco = 4 }: PropsCuerpoDoble) {
  const tema = useTheme();
  return (
    <View style={[estilos.fila, { gap: hueco, height: ancho * PROPORCION }]}>
      <Cuerpo ancho={ancho} girable={false} cara="frente" colores={coloresCuerpo(musculos, tema, 'frente')} decorativo />
      <Cuerpo ancho={ancho} girable={false} cara="espalda" colores={coloresCuerpo(musculos, tema, 'espalda')} decorativo />
    </View>
  );
}

const estilos = StyleSheet.create({ fila: { flexDirection: 'row' } });
