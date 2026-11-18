// Consultas de los tests de movilidad. Se tratan como dato de salud (ADR 0004): solo viven en la memoria de la caché,
// que `ProveedorSesion` vacía al cerrar sesión; nada va a almacenamiento local.
import { type QueryClient, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { TESTS_MOVILIDAD, type TramosMovilidad } from '../api/esquemas';
import { CLAVE_CONSENTIMIENTOS } from '../privacidad/claves';
import { useSesion } from '../sesion/sesion';

export const CLAVE_MOVILIDAD = ['movilidad'] as const;
export const CLAVE_CONSENTIMIENTO_MEDIDAS = [...CLAVE_MOVILIDAD, 'consentimiento'] as const;
const CLAVE_TESTS = [...CLAVE_MOVILIDAD, 'tests'] as const;

export function useConsentimientoMedidas() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_CONSENTIMIENTO_MEDIDAS, queryFn: () => api.tieneConsentimientoMedidas() });
}

/** Sin consentimiento el backend contesta 403: `habilitada` evita pedirlos. */
export function useMovilidad(habilitada = true) {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_TESTS, queryFn: () => api.movilidad(), enabled: habilitada });
}

/** Guarda los tres tramos y deja en caché lo que devuelve el backend. */
export function useGuardarMovilidad() {
  const { api } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: (tramos: TramosMovilidad) => api.guardarMovilidad(tramos),
    onSuccess: (guardada) => consultas.setQueryData(CLAVE_TESTS, guardada),
  });
}

export function useDarConsentimientoMedidas(version: string) {
  const { api } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: () => api.darConsentimientoMedidas(version),
    onSuccess: () => consultas.setQueryData(CLAVE_CONSENTIMIENTO_MEDIDAS, true),
  });
}

/** El backend borra los tests al retirarlo: fuera de la memoria también (y la lista de privacidad se relee). */
export function olvidarMovilidad(consultas: QueryClient): Promise<void> {
  consultas.setQueryData(CLAVE_CONSENTIMIENTO_MEDIDAS, false);
  consultas.removeQueries({ queryKey: CLAVE_TESTS });
  return consultas.invalidateQueries({ queryKey: CLAVE_CONSENTIMIENTOS });
}

export function useRetirarConsentimientoMedidas() {
  const { api } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: () => api.retirarConsentimientoMedidas(),
    onSuccess: () => olvidarMovilidad(consultas),
  });
}

export interface ResumenMovilidad {
  valor: string;
  /** Faltan tests por hacer: se pinta con el color de aviso. */
  pendiente: boolean;
}

/** Lo que dice la fila «Tests de movilidad» del perfil: «Pendientes», «2 de 3» o «Hechos». */
export function useResumenMovilidad(): ResumenMovilidad {
  const consentimiento = useConsentimientoMedidas();
  const movilidad = useMovilidad(consentimiento.data === true);
  if (consentimiento.isPending) return { valor: '…', pendiente: false };
  if (consentimiento.isError) return { valor: 'No disponible', pendiente: false };
  if (!consentimiento.data) return { valor: 'Pendientes', pendiente: true };
  if (movilidad.isPending) return { valor: '…', pendiente: false };
  if (movilidad.isError) return { valor: 'No disponible', pendiente: false };
  const hechos = TESTS_MOVILIDAD.filter((t) => movilidad.data[t] !== undefined).length;
  if (hechos === 0) return { valor: 'Pendientes', pendiente: true };
  if (hechos === TESTS_MOVILIDAD.length) return { valor: 'Hechos', pendiente: false };
  return { valor: `${hechos} de ${TESTS_MOVILIDAD.length}`, pendiente: true };
}
