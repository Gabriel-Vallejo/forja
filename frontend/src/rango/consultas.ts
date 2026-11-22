import { useQuery } from '@tanstack/react-query';

import { useConsentimientoMedidas } from '../movilidad/consultas';
import { useMedidas, usePerfilDatos } from '../perfil/consultas';
import { useSesion } from '../sesion/sesion';
import { calcularRangos, type Rangos } from './rangos';

export const CLAVE_ESTANDARES = ['estandares'] as const;
const UNA_SEMANA_MS = 7 * 24 * 60 * 60 * 1000;

/** Las tablas cambian una vez al año (volcado de OpenPowerlifting): se piden una vez por semana como mucho. */
export function useEstandares() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_ESTANDARES, queryFn: () => api.estandares(), staleTime: UNA_SEMANA_MS });
}

export const CLAVE_MARCAS = ['progreso', 'marcas'] as const;

/**
 * Rango global y mapa por músculo con las marcas de verdad. Mientras carga, o si algo falla, sin rango: nunca uno
 * inventado.
 */
export function useRangos(): Rangos & { cargando: boolean } {
  const { api } = useSesion();
  const marcas = useQuery({ queryKey: CLAVE_MARCAS, queryFn: () => api.marcas() });
  const estandares = useEstandares();
  const datos = usePerfilDatos();
  const consentimiento = useConsentimientoMedidas();
  const medidas = useMedidas(consentimiento.data === true);
  const rangos = calcularRangos(marcas.data ?? [], estandares.data ?? [], datos.data?.sexo, medidas.data?.pesoKg);
  return { ...rangos, cargando: marcas.isPending || estandares.isPending };
}
