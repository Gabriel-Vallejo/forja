import { useLocalSearchParams } from 'expo-router';

import { Privacidad } from '../../src/pantallas/Privacidad';

/** `?seccion=datos` (desde «Descargar mis datos» en Perfil) baja directamente a esa sección; otro valor se ignora. */
export default function RutaPrivacidad() {
  const { seccion } = useLocalSearchParams<{ seccion?: string }>();
  return <Privacidad irADatos={seccion === 'datos'} />;
}
