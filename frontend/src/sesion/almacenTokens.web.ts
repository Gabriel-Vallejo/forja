import type { AlmacenTokens } from './almacenTokens';

// El llavero del sistema (Tauri) llega en otro bloque. Nada de localStorage mientras tanto (ADR 0009).
export const MENSAJE_NO_DISPONIBLE = 'El inicio de sesión en la versión de PC no está disponible todavía.';

function noDisponible(): never {
  throw new Error(MENSAJE_NO_DISPONIBLE);
}

export const almacenTokens: AlmacenTokens = {
  leerAcceso: noDisponible,
  leerRefresco: async () => noDisponible(),
  leerDispositivo: async () => noDisponible(),
  guardar: async () => noDisponible(),
  borrar: async () => noDisponible(),
};
