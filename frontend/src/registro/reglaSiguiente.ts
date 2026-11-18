import type { ObjetivoEjercicio } from '../api/esquemas';
import type { SerieLocal } from './almacen';

/**
 * Regla de la siguiente sesión sin red (RF-25, ADR 0008 §5), la misma que `ReglaProgresion` del backend:
 * si en TODAS las series de trabajo llegas a `repeticionesMax` con `rirObjetivo` o menos, la próxima vez la carga
 * sube `incrementoKg`; si no, repites.
 *
 * - RIR: menos RIR es más esfuerzo (más cerca del fallo), así que un RIR menor o igual al objetivo cumple. Sin RIR
 *   anotado no se puede saber y no cumple, igual que en el servidor.
 * - La carga de partida es la más alta de las series de trabajo, como en el servidor.
 * - Además hay que hacer al menos las `series` previstas (el servidor no lo exige; se pidió así para el móvil). Si
 *   difiere, en cuanto sincroniza manda el servidor.
 *
 * `series` son las de una sesión cerrada en su última versión válida (sin anuladas). Devuelve `undefined` en los
 * ejercicios de peso corporal (sin `cargaKg`) o si no hubo series de trabajo de este ejercicio.
 */
export function cargaSiguiente(objetivo: ObjetivoEjercicio, series: readonly SerieLocal[]): number | undefined {
  if (objetivo.cargaKg === undefined) return undefined;
  const trabajo = series.filter((s) => s.ejercicio === objetivo.idExterno && !s.calentamiento);
  if (trabajo.length === 0) return undefined;
  const base = Math.max(...trabajo.map((s) => s.pesoKg));
  const cumplida =
    trabajo.length >= objetivo.series &&
    trabajo.every((s) => s.repeticiones >= objetivo.repeticionesMax && s.rir !== null && s.rir <= objetivo.rirObjetivo);
  return cumplida ? base + objetivo.incrementoKg : base;
}
