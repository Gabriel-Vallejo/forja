import { useRef, useState } from 'react';
import { type AccessibilityActionEvent, type LayoutChangeEvent, StyleSheet, View } from 'react-native';
import { GestureDetector, usePanGesture } from 'react-native-gesture-handler';

import { type Theme, useTheme } from '../theme';

const ALTO = 64;
const ALTO_MARCADOR = 56;
const ANCHO_MARCA = 2;
const ANCHO_MARCADOR = 3;
const MARCA_MAYOR = 36;
const MARCA_MEDIA = 20;
const MARCA_MENOR = 12;
const ANCHO_POR_DEFECTO = 358;

export type TipoMarca = 'mayor' | 'media' | 'menor';

interface PropsRegla {
  etiqueta: string;
  valor: number;
  min: number;
  max: number;
  paso: number;
  /** Separación en px entre dos marcas (un paso). */
  separacion: number;
  marca: (v: number) => TipoMarca;
  /** Texto que oye el lector de pantalla («80,5 kilos»). */
  textoValor: string;
  alCambiar: (v: number) => void;
}

const limitar = (v: number, a: number, b: number) => Math.min(b, Math.max(a, v));

/** Regla que se arrastra en horizontal para elegir un valor (boceto Calculadora.dc.html). */
export function Regla({ etiqueta, valor, min, max, paso, separacion, marca, textoValor, alCambiar }: PropsRegla) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const [ancho, setAncho] = useState(ANCHO_POR_DEFECTO);
  const inicial = useRef(valor);
  const ajustar = (v: number) => alCambiar(limitar(Math.round(v / paso) * paso, min, max));

  const arrastre = usePanGesture({
    runOnJS: true,
    onBegin: () => {
      inicial.current = valor;
    },
    onUpdate: (e) => ajustar(inicial.current - (e.translationX / separacion) * paso),
  });

  const valores: number[] = [];
  for (let v = min; v <= max + paso / 2; v += paso) valores.push(v);
  const desplazamiento = ancho / 2 - separacion / 2 - ((valor - min) / paso) * separacion;
  const alAccion = (e: AccessibilityActionEvent) =>
    ajustar(valor + (e.nativeEvent.actionName === 'increment' ? paso : -paso));
  const alto: Record<TipoMarca, number> = { mayor: MARCA_MAYOR, media: MARCA_MEDIA, menor: MARCA_MENOR };

  return (
    <GestureDetector gesture={arrastre}>
      <View
        accessible
        accessibilityRole="adjustable"
        accessibilityLabel={etiqueta}
        accessibilityValue={{ min, max, now: valor, text: textoValor }}
        accessibilityActions={[{ name: 'increment' }, { name: 'decrement' }]}
        onAccessibilityAction={alAccion}
        onLayout={(e: LayoutChangeEvent) => setAncho(e.nativeEvent.layout.width)}
        style={estilos.regla}
      >
        <View style={[estilos.tira, { transform: [{ translateX: desplazamiento }] }]}>
          {valores.map((v) => {
            const tipo = marca(v);
            return (
              <View key={v} style={[estilos.hueco, { width: separacion }]}>
                <View style={[estilos.marca, { height: alto[tipo] }, tipo === 'mayor' ? estilos.mayor : estilos.menor]} />
              </View>
            );
          })}
        </View>
        <View style={[estilos.marcador, { left: ancho / 2 - ANCHO_MARCADOR / 2 }]} />
      </View>
    </GestureDetector>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    regla: { height: ALTO, alignSelf: 'stretch', overflow: 'hidden' },
    tira: { position: 'absolute', left: 0, bottom: 0, flexDirection: 'row', alignItems: 'flex-end' },
    hueco: { alignItems: 'center' },
    marca: { width: ANCHO_MARCA, borderRadius: ANCHO_MARCA / 2 },
    mayor: { backgroundColor: tema.color.onBackground },
    menor: { backgroundColor: tema.regla.marcaMenor },
    marcador: {
      position: 'absolute',
      bottom: 0,
      width: ANCHO_MARCADOR,
      height: ALTO_MARCADOR,
      borderRadius: ANCHO_MARCA,
      backgroundColor: tema.color.primary,
    },
  });
}
