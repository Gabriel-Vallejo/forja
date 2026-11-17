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
  if (nivel === 'parar_urgencias') {
    return 'No generamos tu plan por ahora: por lo que has marcado en el cribado, no entrenes y acude hoy a urgencias o llama al 112. Cuando un profesional sanitario te lo permita, repite el cribado.';
  }
  return 'No generamos tu plan por ahora: con lo que has contestado en el cuestionario, acude hoy a urgencias o llama al 112 antes de entrenar. Cuando un profesional sanitario te lo permita, repite el cuestionario.';
}

/** El botón lleva al cuestionario que puso el bloqueo: el cribado (`parar_urgencias`) o el de dolor. */
export const etiquetaRepetir = (nivel: NivelBloqueo | null | undefined) =>
  nivel === 'parar_urgencias' ? 'Repetir el cribado' : 'Repetir el cuestionario';
