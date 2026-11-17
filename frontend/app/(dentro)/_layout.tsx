import { Redirect, Stack } from 'expo-router';

import { useSesion } from '../../src/sesion/sesion';
import { useTheme } from '../../src/theme';

/** Todo lo de aquí dentro necesita sesión: sin ella, a entrar. */
export default function Dentro() {
  const { estado } = useSesion();
  const tema = useTheme();
  if (estado === 'fuera') return <Redirect href="/entrar" />;
  return (
    <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: tema.color.background } }}>
      <Stack.Screen name="escala" options={{ presentation: 'modal' }} />
      <Stack.Screen name="rango" options={{ presentation: 'fullScreenModal' }} />
    </Stack>
  );
}
