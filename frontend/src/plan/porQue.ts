import { useQuery } from '@tanstack/react-query';

import { useSesion } from '../sesion/sesion';

/**
 * «Por qué este ejercicio» de ese día del plan. Solo en memoria: puede citar limitaciones o movilidad (ADR 0004), así
 * que no va al almacén sin red y se vuelve a pedir al abrir el detalle (si se retira un consentimiento, deja de salir).
 */
export function usePorQue(plan: string, dia: number, idExterno: string) {
  const { api } = useSesion();
  return useQuery({ queryKey: ['plan', plan, 'porQue', dia, idExterno], queryFn: () => api.porQue(dia, idExterno), gcTime: 0 });
}
