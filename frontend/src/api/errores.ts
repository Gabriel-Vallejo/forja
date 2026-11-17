import { esquemaProblema } from './esquemas';

/** Error de la API con un mensaje que se puede enseñar tal cual. `estado` 0 = sin conexión. */
export class ErrorApi extends Error {
  constructor(
    readonly estado: number,
    mensaje: string,
  ) {
    super(mensaje);
    this.name = 'ErrorApi';
  }
}

export const SIN_CONEXION = 'No hay conexión con el servidor. Comprueba la red e inténtalo otra vez.';
export const SESION_CADUCADA = 'Tu sesión ha caducado. Vuelve a entrar.';
export const RESPUESTA_INESPERADA = 'La respuesta del servidor no tiene el formato esperado. Inténtalo más tarde.';

const POR_ESTADO: Record<number, string> = {
  401: SESION_CADUCADA,
  403: 'No tienes permiso para hacer esto.',
  404: 'No hemos encontrado lo que buscas.',
  429: 'Demasiados intentos seguidos. Espera un poco y vuelve a probar.',
};
const ERROR_SERVIDOR = 'Algo ha fallado en el servidor. Inténtalo más tarde.';
const ERROR_GENERICO = 'No se ha podido completar la petición.';
const ESTADO_ERROR_SERVIDOR = 500;

/** Convierte un `ProblemDetail` (o lo que llegue) en un mensaje para el usuario. */
export function mensajeDeProblema(estado: number, cuerpo: unknown): string {
  const problema = esquemaProblema.safeParse(cuerpo);
  if (problema.success) {
    const { errores, detail } = problema.data;
    if (errores && errores.length > 0) {
      return errores.map((e) => e.mensaje).join(' ');
    }
    if (detail) {
      return detail;
    }
  }
  if (estado >= ESTADO_ERROR_SERVIDOR) {
    return ERROR_SERVIDOR;
  }
  return POR_ESTADO[estado] ?? ERROR_GENERICO;
}

/** Mensaje para el usuario de cualquier error que llegue a la interfaz. */
export function mensajeDeError(error: unknown): string {
  return error instanceof Error ? error.message : ERROR_GENERICO;
}
