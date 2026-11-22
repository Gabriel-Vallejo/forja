// Rango global y mapa por músculo con las marcas reales (RF-30). Solo la sentadilla, la banca y el peso muerto con
// barra tienen tabla de OpenPowerlifting: el resto de ejercicios sale «sin estándar» y una cuenta nueva, sin rango.
import type { Grado } from '../componentes/Emblema';
import { GRUPOS, type Grupo } from '../componentes/cuerpoTrazados';
import type { MarcaEjercicio, Nivel, RangoDeMusculo, RangoMusculo } from '../datos/ejemplo';
import { kgConComa } from './e1rm';
import { estimar, type Levantamiento, rangoDeTop, type SexoEstandar, type TablaEstandar, tablaPara } from './estandares';

export interface Marca {
  idExterno: string;
  nombre: string;
  grupos: readonly string[];
  pesoKg: number;
  repeticiones: number;
  e1rmKg: number;
  levantamiento: Levantamiento | null;
}

export interface RangoGlobal {
  tipo: Nivel | 'forjado';
  grado: Grado;
  top: number;
}

export interface Rangos {
  /** `null` = sin rango: no hay marcas con estándar, o falta el sexo o el peso. */
  global: RangoGlobal | null;
  musculos: Record<Grupo, RangoDeMusculo & { top: number | null }>;
  /** Hay marcas con estándar pero falta el sexo o el peso para compararlas. */
  faltaPerfil: boolean;
}

export const NOMBRES_GRUPO: Record<Grupo, string> = {
  pecho: 'Pecho', hombros: 'Hombros', brazos: 'Bíceps', triceps: 'Tríceps', antebrazos: 'Antebrazos',
  abdomen: 'Abdomen', cuadriceps: 'Cuádriceps', aductores: 'Aductores', gemelos: 'Gemelos', trapecio: 'Trapecio',
  dorsal: 'Dorsal', lumbar: 'Espalda baja', gluteos: 'Glúteos', isquios: 'Isquiotibiales',
};

const esGrupo = (g: string): g is Grupo => (GRUPOS as readonly string[]).includes(g);

/** «80,5 kg × 8»; en dominadas y fondos, el lastre: «+20,0 kg × 5» o «peso corporal × 12». */
function serieDe(m: Marca): string {
  if (m.levantamiento === 'dominada_lastre' || m.levantamiento === 'fondo_lastre') {
    return m.pesoKg > 0 ? `+${kgConComa(m.pesoKg)} kg × ${m.repeticiones}` : `peso corporal × ${m.repeticiones}`;
  }
  return `${kgConComa(m.pesoKg)} kg × ${m.repeticiones}`;
}

export function calcularRangos(
  marcas: readonly Marca[],
  tablas: readonly TablaEstandar[],
  sexo: string | undefined,
  pesoKg: number | null | undefined,
): Rangos {
  const comparable = (sexo === 'hombre' || sexo === 'mujer') && !!pesoKg;
  const estimacion = (m: Marca) => {
    if (!m.levantamiento || !comparable) return null;
    const tabla = tablaPara(tablas, m.levantamiento, sexo as SexoEstandar, pesoKg as number);
    return tabla ? estimar(tabla, m.e1rmKg) : null;
  };
  const conRango = marcas.map((m) => ({ m, e: estimacion(m) }));

  const musculos = Object.fromEntries(
    GRUPOS.map((g) => {
      const suyas = conRango.filter(({ m }) => m.grupos.filter(esGrupo).includes(g));
      const ejercicios: MarcaEjercicio[] = suyas.map(({ m, e }) => ({
        nombre: m.nombre,
        serie: serieDe(m),
        tipo: (e ? e.tipo : 'sin') as RangoMusculo,
        grado: e ? e.grado : 1,
      }));
      const mejor = suyas.map(({ e }) => e).filter((e) => e !== null).sort((a, b) => a.top - b.top)[0];
      return [g, {
        nombre: NOMBRES_GRUPO[g],
        tipo: (mejor ? mejor.tipo : 'sin') as RangoMusculo,
        grado: mejor ? mejor.grado : 1,
        top: mejor ? mejor.top : null,
        ejercicios,
      }];
    }),
  ) as unknown as Rangos['musculos'];

  // El global: la media del «top %» del mejor resultado de cada levantamiento con estándar.
  const porLevantamiento = new Map<Levantamiento, number>();
  for (const { m, e } of conRango) {
    if (!e || !m.levantamiento) continue;
    const previo = porLevantamiento.get(m.levantamiento);
    if (previo === undefined || e.top < previo) porLevantamiento.set(m.levantamiento, e.top);
  }
  const tops = [...porLevantamiento.values()];
  const global = tops.length === 0 ? null : (() => {
    const top = Math.round(tops.reduce((a, b) => a + b, 0) / tops.length);
    return { ...rangoDeTop(top), top };
  })();
  return { global, musculos, faltaPerfil: !comparable && marcas.some((m) => m.levantamiento !== null) };
}
