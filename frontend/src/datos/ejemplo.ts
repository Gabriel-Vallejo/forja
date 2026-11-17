// shortcut: datos de ejemplo del boceto (rangos por percentil, racha y perfil); en la v0.2 se sustituyen por la API.
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

export const RANGO_GLOBAL: Rango = { tipo: 'acero', grado: 2, top: 26 };

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

const m = (nombre: string, serie: string, tipo: RangoMusculo, grado: Grado = 1): MarcaEjercicio => ({ nombre, serie, tipo, grado });

export const MUSCULOS: Record<Grupo, RangoDeMusculo> = {
  pecho: { nombre: 'Pecho', tipo: 'acero', grado: 3, ejercicios: [m('Press de banca con barra', '80,5 kg × 8', 'acero', 3), m('Press inclinado con mancuernas', '2 × 28 kg × 10', 'oro', 3), m('Fondos con lastre', '+10 kg × 8', 'acero', 1), m('Aperturas en polea', '15 kg × 15', 'sin')] },
  hombros: { nombre: 'Hombros', tipo: 'oro', grado: 2, ejercicios: [m('Press militar con barra', '50 kg × 6', 'oro', 2), m('Elevaciones laterales', '12 kg × 15', 'sin')] },
  brazos: { nombre: 'Bíceps', tipo: 'titanio', grado: 1, ejercicios: [m('Curl con barra EZ', '45 kg × 8', 'titanio', 1), m('Curl inclinado con mancuernas', '2 × 14 kg × 10', 'acero', 3)] },
  triceps: { nombre: 'Tríceps', tipo: 'titanio', grado: 2, ejercicios: [m('Press francés con barra EZ', '40 kg × 10', 'titanio', 2), m('Extensión en polea por encima', '30 kg × 12', 'acero', 2)] },
  antebrazos: { nombre: 'Antebrazos', tipo: 'plata', grado: 3, ejercicios: [m('Curl de muñeca con barra', '35 kg × 15', 'plata', 3)] },
  abdomen: { nombre: 'Abdomen', tipo: 'bronce', grado: 2, ejercicios: [m('Rueda abdominal', '12 reps', 'bronce', 2), m('Elevación de piernas colgado', '10 reps', 'bronce', 1)] },
  cuadriceps: { nombre: 'Cuádriceps', tipo: 'acero', grado: 2, ejercicios: [m('Sentadilla con barra', '120 kg × 5', 'acero', 2), m('Prensa de piernas', '220 kg × 10', 'oro', 3), m('Sentadilla hack', '140 kg × 8', 'acero', 1)] },
  aductores: { nombre: 'Aductores', tipo: 'plata', grado: 1, ejercicios: [m('Aducción en máquina', '60 kg × 12', 'plata', 1)] },
  gemelos: { nombre: 'Gemelos', tipo: 'hierro', grado: 3, ejercicios: [m('Talones de pie en máquina', '80 kg × 12', 'hierro', 3)] },
  trapecio: { nombre: 'Trapecio', tipo: 'oro', grado: 3, ejercicios: [m('Encogimientos con mancuernas', '2 × 40 kg × 12', 'oro', 3)] },
  dorsal: { nombre: 'Dorsal', tipo: 'obsidiana', grado: 1, ejercicios: [m('Dominadas con lastre', '+20 kg × 6', 'obsidiana', 1), m('Jalón al pecho', '80 kg × 10', 'titanio', 3)] },
  lumbar: { nombre: 'Espalda baja', tipo: 'sin', grado: 1, ejercicios: [] },
  gluteos: { nombre: 'Glúteos', tipo: 'acero', grado: 1, ejercicios: [m('Hip thrust con barra', '140 kg × 8', 'acero', 1)] },
  isquios: { nombre: 'Isquiotibiales', tipo: 'bronce', grado: 3, ejercicios: [m('Curl femoral sentado', '50 kg × 12', 'bronce', 3), m('Peso muerto rumano', '90 kg × 8', 'oro', 1)] },
};

/** Filas de «Por zona»: qué grupo da el rango, qué se pinta en la miniatura y cuánto se sube el torso. */
export const ZONAS: readonly { nombre: string; clave: Grupo; partes: readonly Grupo[]; desplazamiento: number }[] = [
  { nombre: 'Pecho', clave: 'pecho', partes: ['pecho'], desplazamiento: -18 },
  { nombre: 'Hombros', clave: 'hombros', partes: ['hombros'], desplazamiento: -18 },
  { nombre: 'Brazos', clave: 'brazos', partes: ['brazos', 'antebrazos'], desplazamiento: -18 },
  { nombre: 'Piernas', clave: 'cuadriceps', partes: ['cuadriceps', 'aductores'], desplazamiento: -70 },
  { nombre: 'Core', clave: 'abdomen', partes: ['abdomen'], desplazamiento: -40 },
];

/** Ejercicios con estándar de la calculadora; `desplazamiento` encuadra la miniatura en el músculo. */
export const EJERCICIOS_CALCULADORA: readonly {
  nombre: string;
  musculo: string;
  grupo: Grupo;
  desplazamiento: number;
  desplazamientoGrande: number;
}[] = [
  { nombre: 'Press de banca con barra', musculo: 'Pecho · tríceps · hombro', grupo: 'pecho', desplazamiento: -14, desplazamientoGrande: -22 },
  { nombre: 'Sentadilla con barra', musculo: 'Cuádriceps · glúteos', grupo: 'cuadriceps', desplazamiento: -60, desplazamientoGrande: -96 },
  { nombre: 'Peso muerto convencional', musculo: 'Espalda baja · glúteos · isquios', grupo: 'cuadriceps', desplazamiento: -40, desplazamientoGrande: -64 },
  { nombre: 'Dominadas', musculo: 'Dorsal · bíceps', grupo: 'brazos', desplazamiento: -14, desplazamientoGrande: -22 },
  { nombre: 'Press militar con barra', musculo: 'Hombros · tríceps', grupo: 'hombros', desplazamiento: -14, desplazamientoGrande: -22 },
  { nombre: 'Curl con barra EZ', musculo: 'Bíceps', grupo: 'brazos', desplazamiento: -14, desplazamientoGrande: -22 },
];

export const SERIE_INICIAL = { peso: 80.5, repeticiones: 8 } as const;

/** Resultado del rango tras la calculadora: aún no sale de la curva real. */
export const RESULTADO_RANGO: Rango & { percentil: number } = { tipo: 'acero', grado: 3, top: 31, percentil: 69 };

const SEMANAS_MAPA = 12;
const DIAS_SEMANA = 7;
const DIAS_DE_ENTRENO = [0, 2, 5];

/** Las 12 últimas semanas, por columnas de lunes a domingo: `true` si se entrenó. */
function celdasRacha(): boolean[] {
  const celdas: boolean[] = [];
  for (let semana = 0; semana < SEMANAS_MAPA; semana += 1) {
    for (let dia = 0; dia < DIAS_SEMANA; dia += 1) {
      const hueco = semana === 6 && dia === 5;
      const futuro = semana === SEMANAS_MAPA - 1 && dia > 0;
      celdas.push(DIAS_DE_ENTRENO.includes(dia) && !hueco && !futuro);
    }
  }
  return celdas;
}

export const RACHA = {
  semanas: 4,
  mejor: 9,
  diasEntrenados: 54,
  cumplidos: 96,
  estaSemana: { hechos: 1, objetivo: 3 },
  periodo: 'sep → nov',
  semanasMapa: SEMANAS_MAPA,
  celdas: celdasRacha(),
} as const;

export const PERFIL = {
  sesiones: 54,
  toneladas: 312,
  resumen: 'Fuerza e hipertrofia · desde nov. 2026',
  entreno: [
    { etiqueta: 'Objetivo', valor: 'Fuerza e hipertrofia' },
    { etiqueta: 'Días y minutos', valor: '3 × 60 min', dato: true },
    { etiqueta: 'Material y gimnasio', valor: 'Completo' },
    { etiqueta: 'Tests de movilidad', valor: 'Pendientes', pendiente: true },
  ],
  personaliza: [
    { etiqueta: 'Tema', valor: 'Oscuro' },
    { etiqueta: 'Unidades · descanso', valor: 'kg · 2:00', dato: true },
    { etiqueta: 'Avisos', valor: 'Días de entreno' },
  ],
  privacidad: [
    { etiqueta: 'Consentimientos y dispositivos', valor: '2 de 5 · 2' },
    { etiqueta: 'Descargar mis datos', valor: '' },
  ],
} as const;
