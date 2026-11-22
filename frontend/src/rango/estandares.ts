// Percentil y rango de una marca frente a las tablas de OpenPowerlifting (RF-30). Se calcula en el móvil: las tablas
// son públicas y el peso y las marcas no viajan al servidor para esto.
import type { Grado } from '../componentes/Emblema';
import { type Nivel, NIVELES_DE_ARRIBA_ABAJO, TOP_FORJADO, TOPS } from '../datos/ejemplo';

export const LEVANTAMIENTOS = ['sentadilla', 'banca', 'peso_muerto', 'dominada_lastre', 'fondo_lastre'] as const;
export type Levantamiento = (typeof LEVANTAMIENTOS)[number];
export type SexoEstandar = 'hombre' | 'mujer';

export interface TablaEstandar {
  levantamiento: Levantamiento;
  /** `kg_lastre`: dominada y fondo (OpenStreetlifting), kg añadidos al peso corporal; `kg_1rm`: carga total. */
  metrica: 'kg_1rm' | 'kg_lastre';
  sexo: SexoEstandar;
  pesoDesdeKg: number;
  /** `null` en la franja de más peso, que no tiene techo. */
  pesoHastaKg: number | null;
  /** Personas en el grupo. */
  muestra: number;
  /** kg en los percentiles 1 a 99, en orden. */
  kg: readonly number[];
}

export interface Estimacion {
  /** Cuántos de cada 100 levantan menos: 0 a 99. */
  percentil: number;
  /** Lo contrario, como lo dice la escala: «top 26 %». Nunca baja de 1. */
  top: number;
  tipo: Nivel | 'forjado';
  grado: Grado;
  tabla: TablaEstandar;
}

/** La tabla del sexo y la franja de peso de la persona, si hay. */
export function tablaPara(
  tablas: readonly TablaEstandar[],
  levantamiento: Levantamiento,
  sexo: SexoEstandar,
  pesoKg: number,
): TablaEstandar | undefined {
  return tablas.find(
    (t) =>
      t.levantamiento === levantamiento &&
      t.sexo === sexo &&
      pesoKg >= t.pesoDesdeKg &&
      (t.pesoHastaKg === null || pesoKg < t.pesoHastaKg),
  );
}

/** Cuántos cortes de percentil quedan por debajo o igual a la marca. */
export function percentilDe(e1rmKg: number, cortes: readonly number[]): number {
  return cortes.filter((kg) => kg <= e1rmKg).length;
}

/** Todos los escalones de la escala, del más alto (menor % top) al más bajo. */
const ESCALONES: readonly { tipo: Nivel; grado: Grado; top: number }[] = NIVELES_DE_ARRIBA_ABAJO.flatMap((tipo) =>
  TOPS[tipo].map((top, i) => ({ tipo, grado: (i + 1) as Grado, top })),
).sort((a, b) => a.top - b.top);

/** El escalón de la escala para un «top X %»: el más alto cuyo umbral aún se alcanza. */
export function rangoDeTop(top: number): { tipo: Nivel | 'forjado'; grado: Grado } {
  if (top <= TOP_FORJADO) return { tipo: 'forjado', grado: 1 };
  const escalon = ESCALONES.find((e) => e.top >= top) ?? { tipo: 'hierro' as const, grado: 1 as const };
  return { tipo: escalon.tipo, grado: escalon.grado };
}

export function estimar(tabla: TablaEstandar, e1rmKg: number): Estimacion {
  const percentil = percentilDe(e1rmKg, tabla.kg);
  const top = Math.max(TOP_FORJADO, 100 - percentil);
  return { percentil, top, ...rangoDeTop(top), tabla };
}
