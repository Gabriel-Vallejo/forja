import { Redirect, Stack } from 'expo-router';

import { RenovarConsentimiento } from '../../src/pantallas/RenovarConsentimiento';
import { useConsentimientos } from '../../src/privacidad/consultas';
import { porRenovar } from '../../src/privacidad/versiones';
import { useSesion } from '../../src/sesion/sesion';
import { useTheme } from '../../src/theme';

/**
 * Todo lo de aquí dentro necesita sesión: sin ella, a entrar. Si un consentimiento se dio con un texto anterior, se
 * pide de nuevo antes de seguir. Sin conexión no se bloquea nada: el registro de series tiene que funcionar igual.
 */
export default function Dentro() {
  const { estado } = useSesion();
  const tema = useTheme();
  const consentimientos = useConsentimientos(estado === 'dentro');
  if (estado === 'fuera') return <Redirect href="/entrar" />;
  const [pendiente] = consentimientos.data ? porRenovar(consentimientos.data) : [];
  if (pendiente) return <RenovarConsentimiento tipo={pendiente} />;
  return (
    <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: tema.color.background } }}>
      <Stack.Screen name="escala" options={{ presentation: 'modal' }} />
      <Stack.Screen name="rango" options={{ presentation: 'fullScreenModal' }} />
    </Stack>
  );
}
