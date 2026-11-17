import { useMemo, useRef, useState } from 'react';
import { type AccessibilityActionEvent, Pressable, StyleSheet, View } from 'react-native';
import { GestureDetector, usePanGesture } from 'react-native-gesture-handler';
import Animated, { Easing, useAnimatedStyle, useReducedMotion, useSharedValue, withTiming } from 'react-native-reanimated';
import Svg from 'react-native-svg';

import { type Theme, useTheme } from '../theme';
import { ESPALDA, FRENTE, type Grupo, LIENZO, NOMBRE_GRUPO } from './cuerpoTrazados';
import { type ColoresCuerpo, FiguraCuerpo } from './FiguraCuerpo';
import { Icono } from './Icono';

/** Un toque no cuenta si el dedo se ha arrastrado más que esto (px). */
export const UMBRAL_ARRASTRE = 6;
const UMBRAL_VERTICAL = 12;
const GRADOS_POR_PX = 0.9;
const MEDIA_VUELTA = 180;
const PERSPECTIVA = 1200;
const CURVA = Easing.bezier(0.2, 0.8, 0.2, 1);
const VISTA = `0 0 ${LIENZO.ancho} ${LIENZO.alto}`;
const ANCHO_POR_DEFECTO = 300;
const ICONO_GIRO = 22;

const esEspalda = (grados: number) => Math.round((((grados % 360) + 360) % 360) / MEDIA_VUELTA) % 2 === 1;
const caraMasCercana = (grados: number) => Math.round(grados / MEDIA_VUELTA) * MEDIA_VUELTA;

export interface PropsCuerpo {
  colores?: ColoresCuerpo;
  seleccion?: Grupo | '';
  alTocar?: (grupo: Grupo) => void;
  girable?: boolean;
  ancho?: number;
  etiqueta?: string;
  /** Miniaturas: el lector de pantalla lo salta (el texto de al lado ya lo dice). */
  decorativo?: boolean;
}

/** Cuerpo de frente y espalda con 14 grupos tocables (boceto `Cuerpo.dc.html`). */
export function Cuerpo({
  colores,
  seleccion = '',
  alTocar,
  girable = true,
  ancho = ANCHO_POR_DEFECTO,
  etiqueta = 'Cuerpo',
  decorativo = false,
}: PropsCuerpo) {
  const tema = useTheme();
  const estilos = useMemo(() => crearEstilos(tema), [tema]);
  const alto = ancho * 2;
  const tam = { width: ancho, height: alto };

  if (!girable) {
    return (
      <View style={tam} accessible={!decorativo} accessibilityRole="image" accessibilityLabel={etiqueta}
        accessibilityElementsHidden={decorativo} importantForAccessibility={decorativo ? 'no-hide-descendants' : 'auto'}>
        <Svg viewBox={VISTA} width={ancho} height={alto}>
          <FiguraCuerpo cara={FRENTE} lado="frente" colores={colores} seleccion={seleccion} alTocar={alTocar} />
        </Svg>
      </View>
    );
  }
  return (
    <CuerpoGirable
      {...{ colores, seleccion, alTocar, etiqueta, ancho, alto }}
      estilos={estilos}
      tema={tema}
    />
  );
}

interface PropsGirable extends Required<Pick<PropsCuerpo, 'seleccion' | 'etiqueta' | 'ancho'>> {
  colores?: ColoresCuerpo;
  alTocar?: (grupo: Grupo) => void;
  alto: number;
  estilos: ReturnType<typeof crearEstilos>;
  tema: Theme;
}

function CuerpoGirable({ colores, seleccion, alTocar, etiqueta, ancho, alto, estilos, tema }: PropsGirable) {
  const rot = useSharedValue(0);
  const actual = useRef(0);
  const inicio = useRef(0);
  const movido = useRef(false);
  const [espalda, setEspalda] = useState(false);
  const sinMovimiento = useReducedMotion();

  function irA(grados: number) {
    actual.current = grados;
    rot.value = withTiming(grados, { duration: sinMovimiento ? 0 : tema.motion.giro, easing: CURVA });
    setEspalda(esEspalda(grados));
  }

  const giro = usePanGesture({
    runOnJS: true,
    activeOffsetX: [-UMBRAL_ARRASTRE, UMBRAL_ARRASTRE],
    failOffsetY: [-UMBRAL_VERTICAL, UMBRAL_VERTICAL],
    onBegin: () => {
      movido.current = false;
      inicio.current = actual.current;
    },
    onUpdate: (e) => {
      if (Math.abs(e.translationX) > UMBRAL_ARRASTRE) movido.current = true;
      actual.current = inicio.current + e.translationX * GRADOS_POR_PX;
      rot.value = actual.current;
    },
    onDeactivate: () => irA(caraMasCercana(actual.current)),
  });

  const estiloFrente = useAnimatedStyle(() => ({
    transform: [{ perspective: PERSPECTIVA }, { rotateY: `${rot.value}deg` }],
  }));
  const estiloEspalda = useAnimatedStyle(() => ({
    transform: [{ perspective: PERSPECTIVA }, { rotateY: `${rot.value + MEDIA_VUELTA}deg` }],
  }));

  const tocar = alTocar
    ? (grupo: Grupo) => {
        if (!movido.current) alTocar(grupo);
      }
    : undefined;

  // Lista equivalente para lectores de pantalla: una acción por grupo de la cara visible.
  const grupos = (espalda ? ESPALDA : FRENTE).grupos;
  const acciones = alTocar ? grupos.map(({ grupo }) => ({ name: grupo, label: NOMBRE_GRUPO[grupo] })) : undefined;
  const alAccion = (e: AccessibilityActionEvent) => {
    const g = grupos.find(({ grupo }) => grupo === e.nativeEvent.actionName);
    if (g && alTocar) alTocar(g.grupo);
  };

  return (
    <View style={{ width: ancho, height: alto }}>
      <GestureDetector gesture={giro}>
        <View
          style={StyleSheet.absoluteFill}
          accessible
          accessibilityRole="image"
          accessibilityLabel={`${etiqueta} · ${espalda ? 'espalda' : 'frente'}`}
          accessibilityHint={alTocar ? 'Usa las acciones para elegir un músculo' : undefined}
          accessibilityActions={acciones}
          onAccessibilityAction={alAccion}
        >
          <Animated.View style={[estilos.cara, estiloFrente]}>
            <Svg viewBox={VISTA} width={ancho} height={alto}>
              <FiguraCuerpo cara={FRENTE} lado="frente" colores={colores} seleccion={seleccion} alTocar={tocar} />
            </Svg>
          </Animated.View>
          <Animated.View style={[estilos.cara, estiloEspalda]}>
            <Svg viewBox={VISTA} width={ancho} height={alto}>
              <FiguraCuerpo cara={ESPALDA} lado="espalda" colores={colores} seleccion={seleccion} alTocar={tocar} />
            </Svg>
          </Animated.View>
        </View>
      </GestureDetector>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={espalda ? 'Ver de frente' : 'Ver la espalda'}
        onPress={() => irA(caraMasCercana(actual.current) + MEDIA_VUELTA)}
        style={({ pressed }) => [estilos.girar, pressed && estilos.girarPulsado]}
      >
        <Icono nombre="girar" tam={ICONO_GIRO} />
      </Pressable>
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    cara: { ...StyleSheet.absoluteFill, backfaceVisibility: 'hidden' },
    girar: {
      position: 'absolute',
      right: 0,
      top: 0,
      width: tema.hit.icono,
      height: tema.hit.icono,
      borderRadius: tema.hit.icono / 2,
      backgroundColor: tema.color.surfaceHighest,
      alignItems: 'center',
      justifyContent: 'center',
    },
    girarPulsado: { backgroundColor: tema.color.surfaceVariant },
  });
}
