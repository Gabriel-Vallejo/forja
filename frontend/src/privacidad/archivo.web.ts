export const SIN_COMPARTIR = 'Este navegador no deja descargar archivos.';

/** Descarga el JSON con un enlace temporal; no queda nada en el almacenamiento del navegador. */
export async function guardarJson(nombre: string, contenido: string): Promise<void> {
  if (typeof document === 'undefined') throw new Error(SIN_COMPARTIR);
  const url = URL.createObjectURL(new Blob([contenido], { type: 'application/json' }));
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  // Revocar en el mismo tick puede cortar la descarga en algunos navegadores.
  setTimeout(() => URL.revokeObjectURL(url), 0);
}
