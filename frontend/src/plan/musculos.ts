import { type ColoresCuerpo } from '../componentes/FiguraCuerpo';
import { ESPALDA, FRENTE, NOMBRE_GRUPO } from '../componentes/cuerpoTrazados';
import type { Musculo } from '../api/esquemas';
import type { Theme } from '../theme';
import type { Dia } from './plan';

/** Cuántos grupos salen como chips en la tarjeta de un día. */
const MAX_CHIPS_DIA = 4;
const MARCA_SECUNDARIO = ' ½';

type Cara = 'frente' | 'espalda';

/** Grupos que más series reciben como primarios en el día, de más a menos. */
export function gruposPrincipales(dia: Dia): string[] {
  const series = new Map<Musculo['grupo'], number>();
  for (const e of dia.ejercicios) {
    for (const m of e.musculos.filter((x) => x.rol === 'primario')) {
      series.set(m.grupo, (series.get(m.grupo) ?? 0) + e.series);
    }
  }
  return [...series.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, MAX_CHIPS_DIA)
    .map(([grupo]) => NOMBRE_GRUPO[grupo]);
}

/** Todos los músculos del día sin repetir; si un grupo es primario en algún ejercicio, queda como primario. */
export function musculosDelDia(dia: Dia): Musculo[] {
  const roles = new Map<Musculo['grupo'], Musculo['rol']>();
  for (const m of dia.ejercicios.flatMap((e) => e.musculos)) {
    if (roles.get(m.grupo) !== 'primario') roles.set(m.grupo, m.rol);
  }
  return [...roles.entries()].map(([grupo, rol]) => ({ grupo, rol }));
}

/** «Pecho» para un primario y «Tríceps ½» para un secundario. */
export const etiquetaMusculo = (m: Musculo) => NOMBRE_GRUPO[m.grupo] + (m.rol === 'secundario' ? MARCA_SECUNDARIO : '');

const gruposDe = (cara: Cara) => (cara === 'frente' ? FRENTE : ESPALDA).grupos.map((g) => g.grupo);

/** La cara del cuerpo donde se ve más trabajo (primario pesa el doble que secundario); en empate, el frente. */
export function caraDelCuerpo(musculos: readonly Musculo[]): Cara {
  const peso = (cara: Cara) =>
    musculos.filter((m) => gruposDe(cara).includes(m.grupo)).reduce((t, m) => t + (m.rol === 'primario' ? 2 : 1), 0);
  return peso('espalda') > peso('frente') ? 'espalda' : 'frente';
}

/** Primarios en `primary`, secundarios en `secondary` y el resto de esa cara en el neutro del tema. */
export function coloresCuerpo(musculos: readonly Musculo[], tema: Theme, cara: Cara): ColoresCuerpo {
  const colores: ColoresCuerpo = Object.fromEntries(gruposDe(cara).map((g) => [g, tema.cuerpo.neutro]));
  const porRol = { secundario: tema.color.secondary, primario: tema.color.primary };
  // Los primarios van después para ganar si un grupo sale con los dos roles.
  for (const [rol, color] of Object.entries(porRol)) {
    for (const m of musculos.filter((x) => x.rol === rol && x.grupo in colores)) colores[m.grupo] = color;
  }
  return colores;
}
