import { type Grado, NOMBRE_RANGO, ROMANO, type TipoRango } from '../componentes/Emblema';
import { type Nivel, type RangoMusculo, TOP_FORJADO, TOPS } from '../datos/ejemplo';

/** «Acero II», o «Sin estándar» si el ejercicio no tiene tabla. */
export function nombreRango(tipo: RangoMusculo | TipoRango, grado: Grado): string {
  if (tipo === 'sin') return 'Sin estándar';
  return tipo === 'forjado' ? NOMBRE_RANGO.forjado : `${NOMBRE_RANGO[tipo]} ${ROMANO[grado]}`;
}

export const nombreRangoMayusculas = (tipo: RangoMusculo | TipoRango, grado: Grado) =>
  tipo === 'sin' ? 'SIN RANGO' : nombreRango(tipo, grado).toUpperCase();

export const topDe = (tipo: Nivel | 'forjado', grado: Grado) => (tipo === 'forjado' ? TOP_FORJADO : TOPS[tipo][grado - 1]);

/** El emblema de un músculo sin rango es el de hierro I, como en el boceto. */
export const tipoEmblema = (tipo: RangoMusculo): TipoRango => (tipo === 'sin' ? 'hierro' : tipo);
