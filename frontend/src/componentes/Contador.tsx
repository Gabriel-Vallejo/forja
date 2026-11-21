import { Pressable, StyleSheet, TextInput, View } from 'react-native';

import { texto, type Theme, useTheme } from '../theme';
import { Icono } from './Icono';
import { Texto } from './Texto';

/** «82,5» o «82.5» → 82.5; vacío o no numérico → NaN. */
export const aNumero = (t: string) => (t.trim() === '' ? NaN : Number(t.trim().replace(',', '.')));
/** 82.5 → «82,5». */
export const aTexto = (n: number) => String(Math.round(n * 100) / 100).replace('.', ',');

interface PropsContador {
  etiqueta: string;
  /** Para el lector de pantalla: «Press de banca, serie 2». */
  contexto: string;
  valor: string;
  alCambiar: (valor: string) => void;
  paso: number;
  min: number;
  max: number;
  unidad?: string;
  /** Pista bajo la etiqueta: «objetivo 6-10». */
  pista?: string;
  decimales?: boolean;
  /** Texto del campo vacío («elige»): un hueco, no un valor objetivo. */
  sugerencia?: string;
}

/** Campo numérico con −/+ de 48 dp (docs/design-system.md §5.3): se usa con una mano y también se puede teclear. */
export function Contador({ etiqueta, contexto, valor, alCambiar, paso, min, max, unidad, pista, decimales = false, sugerencia }: PropsContador) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const actual = aNumero(valor);
  const mover = (signo: 1 | -1) => {
    const base = Number.isNaN(actual) ? min : actual;
    alCambiar(aTexto(Math.min(max, Math.max(min, base + signo * paso))));
  };
  const boton = (signo: 1 | -1) => {
    const tope = signo === 1 ? actual >= max : actual <= min;
    return (
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`${signo === 1 ? 'Subir' : 'Bajar'} ${etiqueta.toLowerCase()}, ${contexto}`}
        accessibilityState={{ disabled: tope }}
        disabled={tope}
        onPress={() => mover(signo)}
        style={({ pressed }) => [estilos.paso, pressed && estilos.pulsado, tope && estilos.tope]}
      >
        <Icono nombre={signo === 1 ? 'mas' : 'menos'} />
      </Pressable>
    );
  };

  return (
    <View style={estilos.contenedor}>
      <View style={estilos.cabecera}>
        <Texto estilo="etiquetaEstrecha" tono="secundario">{unidad ? `${etiqueta} (${unidad})` : etiqueta}</Texto>
        {pista ? <Texto estilo="mini" tono="secundario">{pista}</Texto> : null}
      </View>
      <View style={estilos.fila}>
        {boton(-1)}
        <TextInput
          accessibilityLabel={`${etiqueta}, ${contexto}`}
          value={valor}
          onChangeText={alCambiar}
          keyboardType={decimales ? 'decimal-pad' : 'number-pad'}
          selectTextOnFocus
          placeholder={sugerencia}
          placeholderTextColor={tema.color.onSurfaceVariant}
          selectionColor={tema.color.primary}
          style={estilos.campo}
        />
        {boton(1)}
      </View>
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    // Lo mínimo para que −, el campo y + no se monten: con menos sitio, el de al lado baja de línea.
    contenedor: { gap: tema.space.s1, flex: 1, minWidth: tema.hit.min * 3 + tema.space.s1 * 2 },
    cabecera: { gap: tema.space.s05 },
    fila: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s1 },
    paso: {
      width: tema.hit.min,
      height: tema.hit.min,
      borderRadius: tema.radius.sm,
      borderWidth: tema.border.control,
      borderColor: tema.color.onSurfaceVariant,
      alignItems: 'center',
      justifyContent: 'center',
    },
    pulsado: { backgroundColor: tema.color.surfaceHigh },
    tope: { opacity: tema.opacity.deshabilitado },
    campo: {
      ...texto(tema, 'cifraPequena'),
      flex: 1,
      minWidth: tema.hit.min,
      minHeight: tema.hit.min,
      textAlign: 'center',
      borderRadius: tema.radius.sm,
      backgroundColor: tema.color.surfaceVariant,
      color: tema.color.onBackground,
    },
  });
}
