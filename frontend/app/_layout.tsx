import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import { useState } from 'react';

import { apiForja } from '../src/api/instancia';
import { ProveedorSesion } from '../src/sesion/sesion';
import { useTheme } from '../src/theme';

export default function Raiz() {
  const tema = useTheme();
  const [consultas] = useState(() => new QueryClient());

  return (
    <QueryClientProvider client={consultas}>
      <ProveedorSesion api={apiForja}>
        <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: tema.color.background } }} />
      </ProveedorSesion>
    </QueryClientProvider>
  );
}
