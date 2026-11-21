import { Pressable, StyleSheet, View } from 'react-native';

import { type Theme, useTheme } from '../theme';
import { Texto } from './Texto';

export interface OpcionSeleccion<T extends string> {
  valor: T;
  etiqueta: string;
}

interface PropsSeleccion<T extends string> {
  etiqueta: string;
  opciones: readonly OpcionSeleccion<T>[];
  valor: T | undefined;
  /** Con `opcional`, volver a pulsar la elegida la deselecciona y llega `undefined`. */
  alCambiar: (valor: T | undefined) => void;
  opcional?: boolean;
  ayuda?: string;
  error?: string;
}

interface PropsFichaOpcion {
  etiqueta: string;
  rol: 'radio' | 'checkbox';
  marcada: boolean;
  alPulsar: () => void;
  /** Para lectores de pantalla, si la etiqueta visible no basta («8,5» → «RPE 8,5»). */
  etiquetaAccesible?: string;
}

/** Ficha pulsable de una opción: objetivo táctil mínimo del tema y estado con borde y peso, no solo con color. */
export function FichaOpcion({ etiqueta, rol, marcada, alPulsar, etiquetaAccesible = etiqueta }: PropsFichaOpcion) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  return (
    <Pressable
      accessibilityRole={rol}
      accessibilityLabel={etiquetaAccesible}
      accessibilityState={{ selected: marcada, checked: marcada }}
      onPress={alPulsar}
      style={[estilos.opcion, marcada && estilos.elegida]}
    >
      <Texto estilo="texto" peso={marcada ? '700' : '400'} color={marcada ? tema.color.primary : tema.color.onBackground}>
        {etiqueta}
      </Texto>
    </Pressable>
  );
}

/** Elección única con etiqueta visible y error con palabra, no solo con color (§5.3). */
export function Seleccion<T extends string>({ etiqueta, opciones, valor, alCambiar, opcional = false, ayuda, error }: PropsSeleccion<T>) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  return (
    <View accessibilityRole="radiogroup" accessibilityLabel={etiqueta} style={estilos.contenedor}>
      <Texto peso="700">{etiqueta}</Texto>
      {ayuda && !error ? <Texto estilo="nota" tono="secundario">{ayuda}</Texto> : null}
      <View style={estilos.opciones}>
        {opciones.map((o) => {
          const elegida = o.valor === valor;
          return (
            <FichaOpcion
              key={o.valor}
              etiqueta={o.etiqueta}
              rol="radio"
              marcada={elegida}
              alPulsar={() => alCambiar(elegida && opcional ? undefined : o.valor)}
            />
          );
        })}
      </View>
      {error ? (
        <View accessibilityRole="alert" accessibilityLiveRegion="polite">
          <Texto estilo="texto" color={tema.color.error}>{`Error: ${error}`}</Texto>
        </View>
      ) : null}
    </View>
  );
}

/** Fila de fichas que salta de línea: también la usa el cuestionario de dolor para sus zonas. */
export const opcionesFila = (tema: Theme) => ({ flexDirection: 'row', flexWrap: 'wrap', gap: tema.space.s2 }) as const;

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    contenedor: { gap: tema.space.s2 },
    opciones: opcionesFila(tema),
    opcion: {
      minHeight: tema.hit.min,
      minWidth: tema.hit.min,
      paddingHorizontal: tema.space.s35,
      alignItems: 'center',
      justifyContent: 'center',
      borderRadius: tema.radius.sm,
      borderWidth: tema.border.control,
      borderColor: tema.color.outline,
      backgroundColor: tema.color.surface,
    },
    elegida: { borderColor: tema.color.primary, backgroundColor: tema.color.surfaceHigh },
  });
}
