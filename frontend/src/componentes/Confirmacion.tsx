import { StyleSheet, View } from 'react-native';

import { Aviso } from './Aviso';
import { Boton } from './Boton';
import { type Theme, useTheme } from '../theme';

interface PropsConfirmacion {
  mensaje: string;
  etiquetaConfirmar: string;
  etiquetaCargando: string;
  cargando?: boolean;
  /** Error del intento de confirmar, si lo hubo. */
  error?: string;
  alConfirmar: () => void;
  alCancelar: () => void;
}

/** Pregunta en la propia pantalla antes de algo que no se deshace (no depende de `Alert`, que en web no existe). */
export function Confirmacion({ mensaje, etiquetaConfirmar, etiquetaCargando, cargando = false, error, alConfirmar, alCancelar }: PropsConfirmacion) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  return (
    <View style={estilos.contenedor}>
      <Aviso tipo="aviso" mensaje={mensaje} />
      {error ? <Aviso tipo="error" mensaje={error} /> : null}
      <Boton etiqueta={etiquetaConfirmar} etiquetaCargando={etiquetaCargando} cargando={cargando} onPress={alConfirmar} />
      <Boton variante="secundario" etiqueta="Cancelar" deshabilitado={cargando} onPress={alCancelar} />
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({ contenedor: { gap: tema.space.s2 } });
}
