// Guardar un JSON que el usuario se lleva (RGPD, art. 20). La implementación sale de archivo.native.ts (móvil:
// archivo temporal y hoja de compartir) o archivo.web.ts (navegador: descarga): Metro y Jest eligen por plataforma.

/** Ofrece `contenido` como archivo `nombre` al usuario. Resuelve cuando el sistema ya lo ha recogido. */
export declare function guardarJson(nombre: string, contenido: string): Promise<void>;

/** Mensaje si el sistema no deja compartir archivos. */
export declare const SIN_COMPARTIR: string;
