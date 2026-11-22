// La versión del texto de cada consentimiento que la app enseña. El backend solo acepta la vigente
// (`TipoConsentimiento.versionVigente`): si un texto cambia, se sube allí y aquí a la vez.
import type { Consentimiento, TipoConsentimiento } from '../api/esquemas';

export const VERSIONES_VIGENTES = {
  limitaciones: 'v2',
  medidas_salud: 'v2',
} as const satisfies Partial<Record<TipoConsentimiento, string>>;

export type TipoConTexto = keyof typeof VERSIONES_VIGENTES;

const tieneTexto = (tipo: string): tipo is TipoConTexto => tipo in VERSIONES_VIGENTES;

/**
 * Los consentimientos vivos que se dieron con un texto anterior. El backend los sigue respetando (los datos y el
 * plan no cambian de golpe), pero la app los pide de nuevo con el texto vigente antes de dejar seguir.
 */
export function porRenovar(consentimientos: readonly Consentimiento[]): TipoConTexto[] {
  return consentimientos
    .map((c) => c.tipo)
    .filter(tieneTexto)
    .filter((tipo) => consentimientos.some((c) => c.tipo === tipo && c.versionTexto !== VERSIONES_VIGENTES[tipo]));
}
