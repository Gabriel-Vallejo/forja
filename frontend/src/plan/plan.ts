import { useQuery } from '@tanstack/react-query';

import type { ObjetivoEjercicio, PlanActual } from '../api/esquemas';
import { useSesion } from '../sesion/sesion';

export const CLAVE_PLAN_ACTUAL = ['plan', 'actual'] as const;

/** El plan actual del backend (`GET /planes/actual`); `null` si aún no hay. */
export function usePlanActual() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_PLAN_ACTUAL, queryFn: () => api.planActual() });
}

export type Dia = PlanActual['dias'][number];

const pesoCorporal = (e: ObjetivoEjercicio) => e.incrementoKg === 0;

export function prescripcion(e: ObjetivoEjercicio): string {
  const base = `${e.series} × ${e.repeticionesMin}-${e.repeticionesMax} · RIR ${e.rirObjetivo}`;
  if (pesoCorporal(e)) return `${base} · tu peso`;
  return e.cargaKg === undefined ? base : `${base} · ${e.cargaKg} kg`;
}

export const seriesDe = (dia: Dia) => dia.ejercicios.reduce((total, e) => total + e.series, 0);

// Acoplado al texto de `Motivos.sinSesiones` del backend: la API no dice si ya hay sesiones del ejercicio.
const PREFIJO_SIN_SESIONES = 'Todavía no hay sesiones de este ejercicio';

/** Primera vez con el ejercicio: su motivo es el genérico de elegir carga, que el plan dice una sola vez arriba. */
export const sinSesiones = (e: ObjetivoEjercicio) => e.motivo.startsWith(PREFIJO_SIN_SESIONES);

export const AVISO_PRIMERA_SEMANA =
  'Primera semana: todavía no hay sesiones de estos ejercicios. Elige en cada uno una carga con la que te queden las repeticiones en reserva (RIR) que marca.';

export const NOMBRE_ESTADO: Record<ObjetivoEjercicio['estado'], string> = {
  acumulando: 'Acumulando',
  subir_carga: 'Toca subir',
  estancado: 'Estancado',
  deload: 'Deload',
};

/** «optimo» → el mejor plan que encuentra el solver; «factible» → uno bueno. */
export const calidadPlan = (estado: string) => (estado === 'optimo' ? 'El mejor plan' : 'Un buen plan');

const DIAS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];
export const diaDeLaSemana = (fecha: Date) => DIAS[fecha.getDay()] ?? '';

const dosCifras = (n: number) => String(n).padStart(2, '0');
export const numeroDeOrden = (indice: number) => dosCifras(indice + 1);
