/** 1RM estimado con Epley: peso × (1 + repeticiones / 30), redondeado al kilo (como el boceto). */
const REPETICIONES_EPLEY = 30;

export function e1rmEpley(pesoKg: number, repeticiones: number): number {
  return Math.round(pesoKg * (1 + repeticiones / REPETICIONES_EPLEY));
}

/** «80,5»: un decimal y coma, como se escribe el peso en la app. */
export const kgConComa = (kg: number) => kg.toFixed(1).replace('.', ',');
