// Ajustes del plan (PRD v0.1): vista previa, aplicar y preferencias guardadas. Preferencias de entreno, no datos de
// salud; la caché solo las tiene en memoria.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { CODIGO_BANDERA_ROJA, ErrorApi } from '../api/errores';
import type { Ajuste, PreferenciasGuardadas, PreferenciasPlan } from '../api/esquemas';
import { CLAVE_BLOQUEO } from '../limitaciones/consultas';
import { useSesion } from '../sesion/sesion';
import { CLAVE_PLAN_ACTUAL } from './plan';

const CLAVE_PREFERENCIAS = ['plan', 'preferencias'] as const;
const CLAVE_VISTA_PREVIA = ['plan', 'vista-previa'] as const;

export type TipoAjuste = Ajuste['tipo'];

/** Los músculos con meta de volumen del solver (`musculos-con-minimo`), los que se pueden enfocar. */
export const MUSCULOS_ENFOCABLES = [
  { codigo: 'pecho', nombre: 'Pecho' },
  { codigo: 'dorsales', nombre: 'Dorsales' },
  { codigo: 'espalda_media', nombre: 'Espalda media' },
  { codigo: 'hombros', nombre: 'Hombros' },
  { codigo: 'cuadriceps', nombre: 'Cuádriceps' },
  { codigo: 'isquiotibiales', nombre: 'Isquiotibiales' },
  { codigo: 'gluteos', nombre: 'Glúteos' },
  { codigo: 'biceps', nombre: 'Bíceps' },
  { codigo: 'triceps', nombre: 'Tríceps' },
  { codigo: 'gemelos', nombre: 'Gemelos' },
] as const;
export const MAXIMO_ENFOQUE = 2;

/** Lo que dice la vista previa del ajuste que se está viendo. */
export const TITULO_AJUSTE: Record<string, string> = {
  mas: 'Una serie más',
  menos: 'Una serie menos',
  no_recomendar: 'No recomendar',
  reemplazar: 'Reemplazar',
  variedad: 'Más variedad',
  enfocar: 'Enfocar músculos',
};

/**
 * Resolver con el ajuste tarda unos segundos y gasta un intento: se pide una vez por ajuste y no se repite sola
 * (ni al volver a la pantalla ni al fallar).
 */
export function useVistaPrevia(ajuste: Ajuste) {
  const { api } = useSesion();
  return useQuery({
    queryKey: [...CLAVE_VISTA_PREVIA, ajuste],
    queryFn: () => api.vistaPrevia(ajuste),
    staleTime: Infinity,
    gcTime: 0,
    retry: false,
    refetchOnWindowFocus: false,
  });
}

/** Aplica el ajuste: el plan actual y las preferencias se vuelven a pedir. */
export function useAplicarAjuste() {
  const { api } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: (ajuste: Ajuste & { version: string }) => api.aplicarAjuste(ajuste),
    onSuccess: async () => {
      await Promise.all([
        consultas.invalidateQueries({ queryKey: CLAVE_PLAN_ACTUAL }),
        consultas.invalidateQueries({ queryKey: CLAVE_PREFERENCIAS }),
      ]);
    },
    onError: (error) => {
      if (error instanceof ErrorApi && error.codigo === CODIGO_BANDERA_ROJA) {
        void consultas.invalidateQueries({ queryKey: CLAVE_BLOQUEO });
      }
    },
  });
}

export function usePreferencias() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_PREFERENCIAS, queryFn: () => api.preferencias() });
}

export function useGuardarPreferencias() {
  const { api } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: (preferencias: PreferenciasPlan) => api.guardarPreferencias(preferencias),
    onSuccess: (guardadas) => consultas.setQueryData(CLAVE_PREFERENCIAS, guardadas),
  });
}

/** Las guardadas con códigos, para mandarlas de vuelta sin una. */
export function aCodigos(p: PreferenciasGuardadas): PreferenciasPlan {
  return {
    exclusiones: p.exclusiones.map((e) => e.codigo),
    prioritarios: p.prioritarios.map((m) => m.codigo),
    series: Object.fromEntries(p.series.map((s) => [s.idExterno, s.series])),
    variedad: p.variedad,
  };
}

/** «12» o «10,5» series. */
export const numeroSeries = (n: number) => String(n).replace('.', ',');
