// Tipos y escala de los rangos, y lo que aún es de ejemplo del boceto («Personaliza» del perfil). El rango global y el
// mapa ya salen de las marcas reales (`src/rango/rangos.ts`); la racha y las cifras, de `GET /progreso/resumen`.
// Cada pantalla que los usa enseña la pastilla «Datos de ejemplo». Nada de aquí es de una persona real.
import type { Grupo } from '../componentes/cuerpoTrazados';
import type { Grado, TipoRango } from '../componentes/Emblema';

export type Nivel = Exclude<TipoRango, 'forjado'>;
export type RangoMusculo = Nivel | 'sin';

export interface Rango {
  tipo: TipoRango;
  grado: Grado;
  top: number;
}

/** Top % de cada grado (I, II, III) entre quienes compiten en powerlifting. */
export const TOPS: Record<Nivel, readonly [number, number, number]> = {
  hierro: [100, 92, 85],
  bronce: [78, 72, 66],
  plata: [60, 55, 50],
  oro: [45, 40, 35],
  acero: [30, 26, 22],
  titanio: [19, 16, 13],
  obsidiana: [10, 8, 6],
  diamante: [5, 4, 3],
};
export const TOP_FORJADO = 1;
/** De arriba abajo, como en la escala. */
export const NIVELES_DE_ARRIBA_ABAJO: readonly Nivel[] = [
  'diamante', 'obsidiana', 'titanio', 'acero', 'oro', 'plata', 'bronce', 'hierro',
];

export interface MarcaEjercicio {
  nombre: string;
  serie: string;
  tipo: RangoMusculo;
  grado: Grado;
}

export interface RangoDeMusculo {
  nombre: string;
  tipo: RangoMusculo;
  grado: Grado;
  ejercicios: readonly MarcaEjercicio[];
}

/** Filas de «Por zona»: qué grupo da el rango, qué se pinta en la miniatura y cuánto se sube el torso. */
export const ZONAS: readonly { nombre: string; clave: Grupo; partes: readonly Grupo[]; desplazamiento: number }[] = [
  { nombre: 'Pecho', clave: 'pecho', partes: ['pecho'], desplazamiento: -18 },
  { nombre: 'Hombros', clave: 'hombros', partes: ['hombros'], desplazamiento: -18 },
  { nombre: 'Brazos', clave: 'brazos', partes: ['brazos', 'antebrazos'], desplazamiento: -18 },
  { nombre: 'Piernas', clave: 'cuadriceps', partes: ['cuadriceps', 'aductores'], desplazamiento: -70 },
  { nombre: 'Core', clave: 'abdomen', partes: ['abdomen'], desplazamiento: -40 },
];

export const PERFIL = {
  personaliza: [
    { etiqueta: 'Tema', valor: 'Oscuro' },
    { etiqueta: 'Unidades · descanso', valor: 'kg · 2:00', dato: true },
    { etiqueta: 'Avisos', valor: 'Días de entreno' },
  ],
} as const;
