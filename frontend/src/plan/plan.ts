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

export const pesoCorporal = (e: ObjetivoEjercicio) => e.incrementoKg === 0;

/** Kilos con coma decimal: «82,5». */
export const kilos = (kg: number) => String(kg).replace('.', ',');

/** «4 × 6-10 · RIR 2 · 80 kg»; `cargaSiguiente` (la calculada sin red tras la última sesión) manda sobre la del plan. */
export function prescripcion(e: ObjetivoEjercicio, cargaSiguiente?: number): string {
  const base = `${e.series} × ${e.repeticionesMin}-${e.repeticionesMax} · RIR ${e.rirObjetivo}`;
  if (pesoCorporal(e)) return `${base} · tu peso`;
  const carga = cargaSiguiente ?? e.cargaKg;
  return carga === undefined ? base : `${base} · ${kilos(carga)} kg`;
}

export const seriesDe = (dia: Dia) => dia.ejercicios.reduce((total, e) => total + e.series, 0);

/** Algún ejercicio sin sesiones: su motivo es el genérico de elegir carga, que el plan dice una sola vez arriba. */
export const esPrimeraSemana = (plan: PlanActual) => plan.dias.some((d) => d.ejercicios.some((e) => e.primeraSesion));

/** El día de hoy primero y, detrás, los demás en el orden del ciclo. */
export function diasDesdeHoy(plan: PlanActual): Dia[] {
  const posicion = Math.max(0, plan.dias.findIndex((d) => d.dia === plan.diaDeHoy));
  return [...plan.dias.slice(posicion), ...plan.dias.slice(0, posicion)];
}

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

const MESES = ['ene.', 'feb.', 'mar.', 'abr.', 'may.', 'jun.', 'jul.', 'ago.', 'sep.', 'oct.', 'nov.', 'dic.'];
/** «18 nov.» a partir de una fecha ISO o de milisegundos. */
export const fechaCorta = (fecha: string | number) => {
  const f = new Date(fecha);
  return `${f.getDate()} ${MESES[f.getMonth()] ?? ''}`;
};
