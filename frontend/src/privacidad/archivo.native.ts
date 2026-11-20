import { Directory, File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';

export const SIN_COMPARTIR = 'Este dispositivo no deja compartir archivos.';

/** Carpeta propia en la caché: solo guarda la última exportación. */
const CARPETA = 'mis-datos';

/**
 * Escribe el JSON en la caché y abre la hoja de compartir para que el usuario elija dónde guardarlo. No se borra al
 * cerrar la hoja: apps como Gmail o Drive leen el archivo después, al enviar o subir. Son datos de salud, así que cada
 * exportación vacía antes la carpeta (nunca queda más de una) y, si compartir falla, se borra en el momento.
 */
export async function guardarJson(nombre: string, contenido: string): Promise<void> {
  if (!(await Sharing.isAvailableAsync())) throw new Error(SIN_COMPARTIR);
  const carpeta = new Directory(Paths.cache, CARPETA);
  if (carpeta.exists) carpeta.delete();
  carpeta.create();
  const archivo = new File(carpeta, nombre);
  try {
    archivo.create();
    await archivo.write(contenido);
    await Sharing.shareAsync(archivo.uri, { mimeType: 'application/json', dialogTitle: 'Guardar mis datos' });
  } catch (e) {
    carpeta.delete();
    throw e;
  }
}
