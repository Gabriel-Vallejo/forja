import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react-native';
import type { ReactElement, ReactNode } from 'react';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import type { ApiForja } from '../api/forja';
import type { AlmacenRegistro } from '../registro/almacen';
import { ProveedorSincronizacion } from '../registro/sincronizacion';
import { ProveedorSesion } from '../sesion/sesion';
import { apiFalsa } from './dobles';

const METRICAS = { frame: { x: 0, y: 0, width: 390, height: 844 }, insets: { top: 0, left: 0, right: 0, bottom: 0 } };

/** Raíz de gestos y zona segura, como en `app/_layout.tsx`. */
export function Marco({ children }: { children: ReactNode }) {
  return (
    <GestureHandlerRootView>
      <SafeAreaProvider initialMetrics={METRICAS}>{children}</SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

const sinRed = () => () => undefined;

/**
 * Renderiza con un QueryClient nuevo (sin reintentos ni temporizador de limpieza, que deja Jest colgado), la sesión
 * sobre la API falsa y el registro local sobre `almacen` (sin él, como en web: sin outbox).
 */
export function renderizar(ui: ReactElement, api: ApiForja = apiFalsa(), almacen: AlmacenRegistro | null = null) {
  const abrirAlmacen = async () => almacen;
  const consultas = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false, gcTime: Infinity } } });
  return render(
    <Marco>
      <QueryClientProvider client={consultas}>
        <ProveedorSesion api={api}>
          <ProveedorSincronizacion abrirAlmacen={abrirAlmacen} escucharRed={sinRed}>
            {ui}
          </ProveedorSincronizacion>
        </ProveedorSesion>
      </QueryClientProvider>
    </Marco>,
  );
}
