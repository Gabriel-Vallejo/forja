import { useMemo } from 'react';
import { Pressable, StyleSheet, Text } from 'react-native';

import { Icono, type NombreIcono } from './Icono';

import { letra, texto, type Theme, useTheme } from '../theme';

export type VarianteBoton = 'principal' | 'secundario' | 'texto';

interface PropsBoton {
  etiqueta: string;
  /** Para el lector de pantalla cuando el texto visible es corto y hace falta contexto («Corregir» → «Corregir serie 2 de Remo»). */
  etiquetaAccesible?: string;
  onPress: () => void;
  variante?: VarianteBoton;
  deshabilitado?: boolean;
  /** Mientras es verdadero, el botón no responde y dice `etiquetaCargando` (§5: nunca solo un spinner). */
  cargando?: boolean;
  etiquetaCargando?: string;
  /** Icono delante de la etiqueta (p. ej. «play» en «Empezar sesión»). */
  icono?: NombreIcono;
  /** `grande`: la llamada principal a pie de pantalla del boceto (56 dp). */
  grande?: boolean;
}

const ICONO = 18;

/** Botón de Crimson Ink (docs/design-system.md §5.1). Solo un principal por zona. */
export function Boton({
  etiqueta,
  etiquetaAccesible,
  onPress,
  variante = 'principal',
  deshabilitado = false,
  cargando = false,
  etiquetaCargando = 'Calculando…',
  icono,
  grande = false,
}: PropsBoton) {
  const tema = useTheme();
  const estilos = useMemo(() => crearEstilos(tema), [tema]);
  const inactivo = deshabilitado || cargando;
  const visible = cargando ? etiquetaCargando : etiqueta;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={cargando ? visible : (etiquetaAccesible ?? visible)}
      accessibilityState={{ disabled: inactivo, busy: cargando }}
      disabled={inactivo}
      onPress={onPress}
      style={({ pressed }) => [
        estilos.base,
        estilos[variante],
        grande && estilos.grande,
        pressed && estilos[`${variante}Pulsado`],
        deshabilitado && estilos.deshabilitado,
      ]}
    >
      {icono ? <Icono nombre={icono} tam={ICONO} color={variante === 'principal' ? tema.color.onPrimary : tema.color.onBackground} /> : null}
      <Text style={[estilos.etiqueta, estilos[`etiqueta_${variante}`]]}>{visible}</Text>
    </Pressable>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    base: {
      minHeight: tema.hit.min,
      minWidth: tema.hit.min,
      borderRadius: tema.radius.sm,
      paddingHorizontal: tema.space.s4,
      flexDirection: 'row',
      gap: tema.space.s2,
      alignItems: 'center',
      justifyContent: 'center',
    },
    grande: { minHeight: tema.hit.cta },
    principal: { backgroundColor: tema.color.primary },
    principalPulsado: { backgroundColor: tema.color.secondary },
    secundario: { borderWidth: tema.border.control, borderColor: tema.color.onSurfaceVariant },
    secundarioPulsado: { backgroundColor: tema.color.surfaceHigh },
    texto: {},
    textoPulsado: { backgroundColor: tema.color.surfaceHigh },
    deshabilitado: { opacity: tema.opacity.deshabilitado },
    etiqueta: { ...texto(tema, 'body'), ...letra(tema, 'sans', '700'), textAlign: 'center' },
    etiqueta_principal: { color: tema.color.onPrimary },
    etiqueta_secundario: { color: tema.color.onBackground },
    etiqueta_texto: { color: tema.color.onBackground, textDecorationLine: 'underline' },
  });
}
