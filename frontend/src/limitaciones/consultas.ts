// Consultas de TanStack Query de las limitaciones. Son datos de salud (RGPD art. 9): viven solo en la memoria
// de la caché, que `ProveedorSesion` vacía al cerrar sesión; nada va a almacenamiento local.
import { useQuery } from '@tanstack/react-query';

import { useSesion } from '../sesion/sesion';

export const CLAVE_LIMITACIONES = ['limitaciones'] as const;
export const CLAVE_CONSENTIMIENTO = [...CLAVE_LIMITACIONES, 'consentimiento'] as const;
export const CLAVE_RESTRICCIONES = [...CLAVE_LIMITACIONES, 'restricciones'] as const;
export const CLAVE_EFECTO = [...CLAVE_LIMITACIONES, 'efecto'] as const;
const CLAVE_PREGUNTAS = [...CLAVE_LIMITACIONES, 'preguntas'] as const;

export function useConsentimientoLimitaciones() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_CONSENTIMIENTO, queryFn: () => api.tieneConsentimientoLimitaciones() });
}

/** Sin consentimiento el backend contesta 403: `habilitada` evita pedirlas. */
export function useRestricciones(habilitada = true) {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_RESTRICCIONES, queryFn: () => api.restricciones(), enabled: habilitada });
}

export function useEfecto() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_EFECTO, queryFn: () => api.efectoRestricciones() });
}

export function usePreguntasCribado() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_PREGUNTAS, queryFn: () => api.preguntasCribado(), staleTime: Infinity });
}

export interface ResumenLimitaciones {
  valor: string;
  /** Falta configurarlas: se pinta con el color de aviso. */
  pendiente: boolean;
}

/** Lo que dice la fila «Limitaciones» del perfil: el dato real, no un ejemplo. */
export function useResumenLimitaciones(): ResumenLimitaciones {
  const consentimiento = useConsentimientoLimitaciones();
  const restricciones = useRestricciones(consentimiento.data === true);
  if (consentimiento.isPending) return { valor: '…', pendiente: false };
  if (consentimiento.isError) return { valor: 'No disponible', pendiente: false };
  if (!consentimiento.data) return { valor: 'Sin configurar', pendiente: true };
  if (restricciones.isPending) return { valor: '…', pendiente: false };
  if (restricciones.isError) return { valor: 'No disponible', pendiente: false };
  const activas = restricciones.data.filter((r) => r.estado !== 'historico_sin_limitacion').length;
  return { valor: activas === 0 ? 'Ninguna' : activas === 1 ? '1 activa' : `${activas} activas`, pendiente: false };
}
