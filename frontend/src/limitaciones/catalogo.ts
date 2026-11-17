// Códigos y nombres de las limitaciones, iguales a los enums del backend (es.forja.restricciones.modelo).
// Si el backend añade uno, `esquemas.ts` rechaza la respuesta y esta lista es lo único que hay que tocar.

export const REGIONES = {
  columna_lumbar: 'Columna lumbar',
  columna_cervical: 'Columna cervical',
  hombro: 'Hombro',
  codo: 'Codo',
  muneca: 'Muñeca',
  cadera: 'Cadera',
  rodilla: 'Rodilla',
  tobillo: 'Tobillo',
} as const;
export type CodigoRegion = keyof typeof REGIONES;

export const LADOS = {
  izquierdo: 'Izquierdo',
  derecho: 'Derecho',
  ambos: 'Ambos',
  no_aplica: 'No aplica',
} as const;
export type CodigoLado = keyof typeof LADOS;

export const PATRONES = {
  sentadilla: 'Sentadilla',
  bisagra: 'Bisagra de cadera',
  zancada: 'Zancada',
  empuje_horizontal: 'Empuje horizontal',
  empuje_vertical: 'Empuje vertical',
  traccion_horizontal: 'Tracción horizontal',
  traccion_vertical: 'Tracción vertical',
  aislamiento_brazo: 'Aislamiento de brazo',
  aislamiento_pierna: 'Aislamiento de pierna',
  core: 'Core',
  acarreo: 'Acarreo',
  pliometria: 'Pliometría',
  movilidad: 'Movilidad',
  cardio: 'Cardio',
} as const;
export type CodigoPatron = keyof typeof PATRONES;

export const TIPOS = {
  evitar: 'Evitar',
  limitar_rango: 'Limitar rango',
  limitar_carga: 'Limitar carga',
  sin_carga_axial: 'Sin carga axial',
  sin_impacto: 'Sin impacto',
  sin_agarre_pesado: 'Sin agarre pesado',
  sin_por_encima_cabeza: 'Sin trabajo por encima de la cabeza',
  tempo_controlado: 'Tempo controlado',
} as const;
export type CodigoTipo = keyof typeof TIPOS;

export const ESTADOS = {
  actual: 'Ahora mismo',
  ultimos_12_meses: 'En los últimos 12 meses',
  historico_sin_limitacion: 'Antigua, ya sin limitación',
} as const;
export type CodigoEstado = keyof typeof ESTADOS;

export const ORIGENES = {
  usuario: 'La indico yo',
  profesional: 'Me la indicó un profesional',
} as const;
export type CodigoOrigen = keyof typeof ORIGENES;

/** Las claves de un catálogo como tupla no vacía, que es lo que pide `z.enum`. */
export function codigos<K extends string>(catalogo: Readonly<Record<K, string>>): [K, ...K[]] {
  return Object.keys(catalogo) as [K, ...K[]];
}

/** Las opciones de un catálogo para el selector, en el orden en que se declaró. */
export function opcionesDe<K extends string>(catalogo: Readonly<Record<K, string>>): { valor: K; etiqueta: string }[] {
  return codigos(catalogo).map((valor) => ({ valor, etiqueta: catalogo[valor] }));
}
