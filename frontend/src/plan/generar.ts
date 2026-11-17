import { useMutation, useQueryClient } from '@tanstack/react-query';

import { CODIGO_BANDERA_ROJA, ErrorApi } from '../api/errores';
import { CLAVE_BLOQUEO } from '../limitaciones/consultas';
import { useSesion } from '../sesion/sesion';
import { CLAVE_PLAN_ACTUAL } from './plan';

/** Genera (o regenera) el plan: el backend guarda una versión nueva y el plan actual se vuelve a pedir. */
export function useGenerarPlan() {
  const { api } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: () => api.generarPlan(),
    onSuccess: () => consultas.invalidateQueries({ queryKey: CLAVE_PLAN_ACTUAL }),
    onError: (error) => {
      // El backend tiene un bloqueo que el cliente no conocía: se vuelve a leer para explicarlo.
      if (error instanceof ErrorApi && error.codigo === CODIGO_BANDERA_ROJA) {
        void consultas.invalidateQueries({ queryKey: CLAVE_BLOQUEO });
      }
    },
  });
}
