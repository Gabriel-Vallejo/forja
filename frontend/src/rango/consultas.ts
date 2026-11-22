import { useQuery } from '@tanstack/react-query';

import { useSesion } from '../sesion/sesion';

export const CLAVE_ESTANDARES = ['estandares'] as const;
const UNA_SEMANA_MS = 7 * 24 * 60 * 60 * 1000;

/** Las tablas cambian una vez al año (volcado de OpenPowerlifting): se piden una vez por semana como mucho. */
export function useEstandares() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_ESTANDARES, queryFn: () => api.estandares(), staleTime: UNA_SEMANA_MS });
}
