import { useQuery } from '@tanstack/react-query';

import { useSesion } from '../sesion/sesion';

export const CLAVE_RESUMEN_PROGRESO = ['progreso', 'resumen'] as const;

/** Racha semanal y cifras del perfil, de las sesiones ya enviadas (lo pendiente del móvil aún no cuenta). */
export function useResumenProgreso() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_RESUMEN_PROGRESO, queryFn: () => api.resumenProgreso() });
}

const MIL = 1000;

/** «850 kg» o, desde una tonelada, «12,4 t». */
export function formatoCarga(kg: number): { valor: string; unidad: string } {
  if (kg < MIL) return { valor: String(Math.round(kg)), unidad: 'kg' };
  const toneladas = kg / MIL;
  const redondeo = toneladas < 100 ? Math.round(toneladas * 10) / 10 : Math.round(toneladas);
  return { valor: String(redondeo).replace('.', ','), unidad: 't' };
}
