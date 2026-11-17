import type { NivelBloqueo } from '../api/esquemas';

/** Lo que el plan guardado hace con las limitaciones: nada hasta que se regenera (`ServicioRutina.generar`). */
export const NOTA_PLAN = 'Tu plan actual no se modifica solo: las limitaciones cuentan cuando regeneras tu plan.';

export const FINALIDAD = 'Forja elige ejercicios compatibles con las limitaciones que tú indicas.';

/**
 * Qué pasa al regenerar, según `ServicioPlan` y el ADR 0008 §6: cada plan es una versión nueva e inmutable y las
 * sesiones ya registradas siguen apuntando a su versión.
 */
export const CONFIRMAR_REGENERAR =
  'Se crea un plan nuevo con tus limitaciones actuales. Las sesiones que ya has registrado se conservan.';

/** Por qué no se genera el plan y qué hacer, según el nivel que recuerda el backend. Sin diagnóstico. */
export function mensajeBloqueo(nivel: NivelBloqueo | null | undefined): string {
  if (nivel === 'profesional_en_dias') {
    return 'No generamos tu plan por ahora: antes habla con un profesional sanitario. Cuando lo hayas hecho, puedes repetir el cuestionario.';
  }
  return 'No generamos tu plan por ahora: con lo que has contestado, acude hoy a urgencias o llama al 112 antes de entrenar. Cuando un profesional sanitario te lo permita, repite el cuestionario.';
}
