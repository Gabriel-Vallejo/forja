import { Inter_400Regular, Inter_500Medium, Inter_700Bold, Inter_800ExtraBold } from '@expo-google-fonts/inter';
import { JetBrainsMono_400Regular, JetBrainsMono_700Bold } from '@expo-google-fonts/jetbrains-mono';
import { NotoSerifJP_900Black } from '@expo-google-fonts/noto-serif-jp';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useFonts } from 'expo-font';
import { SplashScreen, Stack } from 'expo-router';
import { useEffect, useState } from 'react';
import { StyleSheet } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';

import { apiForja } from '../src/api/instancia';
import { ProveedorSesion } from '../src/sesion/sesion';
import { useTheme } from '../src/theme';

// La pantalla de arranque se queda hasta que cargan las fuentes (o fallan: entonces, la del sistema).
SplashScreen.preventAutoHideAsync();

const FUENTES = {
  Inter_400Regular,
  Inter_500Medium,
  Inter_700Bold,
  Inter_800ExtraBold,
  JetBrainsMono_400Regular,
  JetBrainsMono_700Bold,
  NotoSerifJP_900Black,
};

export default function Raiz() {
  const tema = useTheme();
  const [consultas] = useState(() => new QueryClient());
  const [cargadas, error] = useFonts(FUENTES);
  const listas = cargadas || error !== null;

  useEffect(() => {
    if (listas) SplashScreen.hideAsync();
  }, [listas]);

  if (!listas) return null;
  return (
    <GestureHandlerRootView style={estilos.raiz}>
      <QueryClientProvider client={consultas}>
        <ProveedorSesion api={apiForja}>
          <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: tema.color.background } }} />
        </ProveedorSesion>
      </QueryClientProvider>
    </GestureHandlerRootView>
  );
}

const estilos = StyleSheet.create({ raiz: { flex: 1 } });
