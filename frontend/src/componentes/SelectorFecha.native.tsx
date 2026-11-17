import DateTimePicker, { type DateTimePickerEvent } from '@react-native-community/datetimepicker';
import { useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { texto, type Theme, useTheme } from '../theme';
import type { PropsSelectorFecha } from './SelectorFecha';

const MESES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
];
/** Donde abre la ruleta si aún no hay fecha: más cerca de un adulto que de hoy. */
const FECHA_INICIAL = new Date(2000, 0, 1);
const FECHA_MINIMA = new Date(1900, 0, 1);
const TEXTO_SIN_FECHA = 'Elige tu fecha';

const dosCifras = (n: number) => String(n).padStart(2, '0');

function aTexto(fecha: Date): string {
  return `${fecha.getFullYear()}-${dosCifras(fecha.getMonth() + 1)}-${dosCifras(fecha.getDate())}`;
}

function aFecha(valor: string): Date | null {
  const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(valor);
  return partes ? new Date(Number(partes[1]), Number(partes[2]) - 1, Number(partes[3])) : null;
}

function legible(fecha: Date | null): string {
  return fecha ? `${fecha.getDate()} de ${MESES[fecha.getMonth()]} de ${fecha.getFullYear()}` : TEXTO_SIN_FECHA;
}

/** Fecha con la ruleta nativa de día, mes y año (spinner de Android); el valor sigue siendo `AAAA-MM-DD`. */
export function SelectorFecha({ etiqueta, valor, alCambiar, error, ayuda }: PropsSelectorFecha) {
  const tema = useTheme();
  const estilos = useMemo(() => crearEstilos(tema), [tema]);
  const [abierto, setAbierto] = useState(false);
  const fecha = aFecha(valor);

  function alElegir(evento: DateTimePickerEvent, elegida?: Date) {
    setAbierto(false);
    if (evento.type === 'set' && elegida) {
      alCambiar(aTexto(elegida));
    }
  }

  return (
    <View style={estilos.contenedor}>
      <Text style={estilos.etiqueta}>{etiqueta}</Text>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`${etiqueta}: ${legible(fecha)}`}
        accessibilityHint={error ?? 'Abre la ruleta de día, mes y año'}
        onPress={() => setAbierto(true)}
        style={[estilos.campo, error ? estilos.campoConError : null]}
      >
        <Text style={fecha ? estilos.valor : estilos.vacio}>{legible(fecha)}</Text>
      </Pressable>
      {abierto ? (
        <DateTimePicker
          mode="date"
          display="spinner"
          value={fecha ?? FECHA_INICIAL}
          minimumDate={FECHA_MINIMA}
          maximumDate={new Date()}
          onChange={alElegir}
        />
      ) : null}
      {ayuda && !error ? <Text style={estilos.ayuda}>{ayuda}</Text> : null}
      {error ? (
        <Text accessibilityRole="alert" accessibilityLiveRegion="polite" style={estilos.error}>
          {`Error: ${error}`}
        </Text>
      ) : null}
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    contenedor: { gap: tema.space.s2 },
    etiqueta: { ...texto(tema, 'body'), fontWeight: '700', color: tema.color.onBackground },
    campo: {
      minHeight: tema.hit.min,
      justifyContent: 'center',
      paddingHorizontal: tema.space.s4,
      borderRadius: tema.radius.sm,
      borderWidth: tema.border.control,
      borderColor: tema.color.onSurfaceVariant,
      backgroundColor: tema.color.surfaceVariant,
    },
    campoConError: { borderColor: tema.color.error, borderWidth: tema.border.foco },
    valor: { ...texto(tema, 'body'), color: tema.color.onBackground },
    vacio: { ...texto(tema, 'body'), color: tema.color.onSurfaceVariant },
    ayuda: { ...texto(tema, 'body'), color: tema.color.onSurfaceVariant },
    error: { ...texto(tema, 'body'), color: tema.color.error },
  });
}
