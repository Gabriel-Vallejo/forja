import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react-native';
import type { ReactElement } from 'react';

import type { ApiForja } from '../api/forja';
import { ProveedorSesion } from '../sesion/sesion';

/** Renderiza con un QueryClient nuevo (sin reintentos) y la sesión sobre la API falsa. */
export function renderizar(ui: ReactElement, api: ApiForja) {
  const consultas = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={consultas}>
      <ProveedorSesion api={api}>{ui}</ProveedorSesion>
    </QueryClientProvider>,
  );
}
