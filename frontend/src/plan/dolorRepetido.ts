import { useQuery } from '@tanstack/react-query';

import type { DolorRepetido } from '../api/esquemas';
import { useSesion } from '../sesion/sesion';

const SIN_DOLOR: ReadonlyMap<string, DolorRepetido> = new Map();

/**
 * Ejercicios que han dolido de forma repetida (RF-27), por `idExterno`. Es dato de salud: solo en memoria, como «Por
 * qué». Es una pista: si no se puede pedir (sin red), no se avisa y el plan sigue igual.
 */
export function useDolorRepetido(): ReadonlyMap<string, DolorRepetido> {
  const { api } = useSesion();
  const consulta = useQuery({ queryKey: ['dolorRepetido'], queryFn: () => api.dolorRepetido(), gcTime: 0 });
  return consulta.data ? new Map(consulta.data.map((d) => [d.idExterno, d])) : SIN_DOLOR;
}

export const textoDolorRepetido = (d: DolorRepetido) =>
  `Te ha dolido en ${d.sesionesConDolor} de tus ${d.sesionesMiradas} últimas sesiones`;
