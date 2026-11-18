// Textos del perfil manual (PRD v0.1): sexo, experiencia, objetivo y la estimación de % de grasa.
// Fitness, no sanitario (ADR 0007): nada de lenguaje de salud o diagnóstico.
import type { Experiencia, ObjetivoPerfil, Sexo } from '../api/esquemas';

export const ETIQUETA_SEXO: Readonly<Record<Sexo, string>> = {
  hombre: 'Hombre',
  mujer: 'Mujer',
  no_indica: 'Prefiero no decirlo',
};

export const AYUDA_SEXO =
  'Solo se usa para la fórmula del % de grasa y, más adelante, para los estándares de fuerza. Sin decirlo, no hay estimación de grasa.';

export const ETIQUETA_EXPERIENCIA: Readonly<Record<Experiencia, string>> = {
  principiante: 'Principiante',
  intermedio: 'Intermedio',
  avanzado: 'Avanzado',
};

/** Una línea por nivel, para que se elija con criterio y no por orgullo. */
export const DESCRIPCION_EXPERIENCIA: Readonly<Record<Experiencia, string>> = {
  principiante: 'Menos de un año entrenando con constancia, o vuelves tras un parón largo.',
  intermedio: 'Entre uno y tres años entrenando con constancia y siguiendo una progresión.',
  avanzado: 'Más de tres años con constancia; ya te cuesta progresar cada semana.',
};

export const ETIQUETA_OBJETIVO: Readonly<Record<ObjetivoPerfil, string>> = {
  fuerza: 'Fuerza',
  hipertrofia: 'Hipertrofia',
  fuerza_hipertrofia: 'Fuerza e hipertrofia',
};

export const NOTA_OBJETIVO = 'Hoy es una preferencia que se guarda en tu perfil: aún no cambia las series ni las repeticiones de tu plan.';

export const NOTA_GRASA =
  'Es una estimación con la fórmula de la US Navy y su margen de error ronda los 4 puntos. Úsala para ver tu tendencia, no como una medición.';

const num = (n: number) => n.toLocaleString('es-ES', { maximumFractionDigits: 1 });

/** «≈ 16,1 % (± 4 puntos)». */
export function textoGrasa(g: { porcentaje: number; margen: number }): string {
  return `≈ ${num(g.porcentaje)} % (± ${num(g.margen)} puntos)`;
}

/** Qué falta para poder estimar el % de grasa, según el sexo guardado. */
export function faltaParaGrasa(sexo: Sexo): string {
  if (sexo === 'no_indica') return 'Para estimar tu % de grasa, indica arriba si eres hombre o mujer y rellena altura, cuello y cintura.';
  if (sexo === 'mujer') return 'Para estimar tu % de grasa necesitas altura, cuello, cintura y cadera.';
  return 'Para estimar tu % de grasa necesitas altura, cuello y cintura.';
}

const SIN_DEFINIR = 'Sin definir';

export const valorObjetivo = (datos: { objetivo?: ObjetivoPerfil }): string => (datos.objetivo ? ETIQUETA_OBJETIVO[datos.objetivo] : SIN_DEFINIR);
export const valorExperiencia = (datos: { experiencia?: Experiencia }): string =>
  datos.experiencia ? ETIQUETA_EXPERIENCIA[datos.experiencia] : SIN_DEFINIR;

/** Bajo «Tu perfil»: «Fuerza e hipertrofia · Intermedio», o una invitación a completarlo. */
export function resumenPerfil(datos: { objetivo?: ObjetivoPerfil; experiencia?: Experiencia }): string {
  const partes = [datos.objetivo && ETIQUETA_OBJETIVO[datos.objetivo], datos.experiencia && ETIQUETA_EXPERIENCIA[datos.experiencia]];
  return partes.filter(Boolean).join(' · ') || 'Completa tu objetivo y tu experiencia';
}
