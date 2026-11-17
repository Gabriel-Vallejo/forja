import { StyleSheet, View } from 'react-native';

import { type Theme, useTheme } from '../theme';
import { Texto } from './Texto';

export type TonoVeredicto = 'ok' | 'aviso' | 'urgente';

export interface MotivoVeredicto {
  texto: string;
  /** De dónde sale la regla (la guía que cita el backend). */
  fuente?: string;
}

interface PropsVeredicto {
  tono: TonoVeredicto;
  titulo: string;
  texto?: string;
  motivos?: readonly MotivoVeredicto[];
}

/** Resultado de un cuestionario: título con borde del color de su nivel, nunca solo color (§1.5). */
export function Veredicto({ tono, titulo, texto, motivos = [] }: PropsVeredicto) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const color = { ok: tema.color.success, aviso: tema.color.warning, urgente: tema.color.error }[tono];
  return (
    <View
      accessibilityLiveRegion={tono === 'ok' ? 'polite' : 'assertive'}
      style={[estilos.contenedor, { borderColor: color }]}
    >
      <Texto estilo="heading" encabezado color={color}>{titulo}</Texto>
      {texto ? <Texto>{texto}</Texto> : null}
      {motivos.map((m) => (
        <View key={m.texto} style={estilos.motivo}>
          <Texto>{m.texto}</Texto>
          {m.fuente ? <Texto estilo="nota" tono="secundario">{`Guía: ${m.fuente}`}</Texto> : null}
        </View>
      ))}
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    contenedor: {
      gap: tema.space.s3,
      padding: tema.space.s4,
      borderRadius: tema.radius.md,
      borderWidth: tema.border.foco,
      backgroundColor: tema.color.surfaceHigh,
    },
    motivo: { gap: tema.space.s1 },
  });
}
