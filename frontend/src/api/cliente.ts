import type { z } from 'zod';

import type { AlmacenTokens } from '../sesion/almacenTokens';
import { codigoDeProblema, ErrorApi, mensajeDeProblema, RESPUESTA_INESPERADA, SESION_CADUCADA, SIN_CONEXION } from './errores';
import { esquemaDatosSesion } from './esquemas';

export const RUTA_REFRESCO = '/api/v1/auth/refresco';
const NO_AUTORIZADO = 401;
/** El refresco era de una cuenta borrada desde otro dispositivo (ADR 0009 §9). */
const CUENTA_BORRADA = 410;

type Metodo = 'GET' | 'POST' | 'PUT' | 'DELETE';

export interface Peticion<T> {
  ruta: string;
  metodo?: Metodo;
  cuerpo?: unknown;
  /** Sin esquema, la respuesta se descarta (204 y similares). */
  esquema?: z.ZodType<T>;
  /** Por defecto, sí: lleva el token de acceso y renueva la sesión ante un 401. */
  autenticada?: boolean;
}

/** Lo único que el cliente usa de `fetch`; el global encaja y los tests pasan un doble. */
export type Fetch = (url: string, init: RequestInit) => Promise<Response>;

export interface OpcionesCliente {
  urlBase: string;
  almacen: AlmacenTokens;
  fetch?: Fetch;
}

export interface ClienteApi {
  pedir<T = void>(peticion: Peticion<T>): Promise<T>;
  /** Avisa cuando la sesión no se puede renovar. Devuelve la función para dejar de escuchar. */
  alCaducar(oyente: () => void): () => void;
  /**
   * Avisa, antes de cerrar la sesión, de que la cuenta se borró desde otro dispositivo, con su usuario, para borrar lo
   * que este móvil guarda de ella. Devuelve la función para dejar de escuchar.
   */
  alBorrarseLaCuenta(oyente: (usuario: string) => Promise<void>): () => void;
}

export function crearCliente({ urlBase, almacen, fetch: hacerFetch = fetch }: OpcionesCliente): ClienteApi {
  const oyentes = new Set<() => void>();
  const oyentesBorrado = new Set<(usuario: string) => Promise<void>>();
  // Promesa compartida: con varias peticiones en 401 a la vez, se renueva una sola vez. Dos refrescos con el
  // mismo token revocan el dispositivo (ADR 0009 §3).
  let renovando: Promise<void> | null = null;

  async function enviar(peticion: Peticion<unknown>, acceso: string | null): Promise<Response> {
    const cabeceras: Record<string, string> = { Accept: 'application/json' };
    if (acceso) {
      cabeceras.Authorization = `Bearer ${acceso}`;
    }
    if (peticion.cuerpo !== undefined) {
      cabeceras['Content-Type'] = 'application/json';
    }
    try {
      return await hacerFetch(urlBase + peticion.ruta, {
        method: peticion.metodo ?? 'GET',
        headers: cabeceras,
        body: peticion.cuerpo === undefined ? undefined : JSON.stringify(peticion.cuerpo),
      });
    } catch {
      throw new ErrorApi(0, SIN_CONEXION);
    }
  }

  async function leerJson(respuesta: Response): Promise<unknown> {
    try {
      return await respuesta.json();
    } catch {
      return undefined;
    }
  }

  async function validar<T>(respuesta: Response, esquema: z.ZodType<T>): Promise<T> {
    const resultado = esquema.safeParse(await leerJson(respuesta));
    if (!resultado.success) {
      throw new ErrorApi(respuesta.status, RESPUESTA_INESPERADA);
    }
    return resultado.data;
  }

  async function error(respuesta: Response): Promise<ErrorApi> {
    const cuerpo = await leerJson(respuesta);
    return new ErrorApi(respuesta.status, mensajeDeProblema(respuesta.status, cuerpo), codigoDeProblema(cuerpo));
  }

  async function refrescar(): Promise<void> {
    const tokenRefresco = await almacen.leerRefresco();
    if (!tokenRefresco) {
      throw new ErrorApi(NO_AUTORIZADO, SESION_CADUCADA);
    }
    const respuesta = await enviar({ metodo: 'POST', ruta: RUTA_REFRESCO, cuerpo: { tokenRefresco } }, null);
    if (!respuesta.ok) {
      throw await error(respuesta);
    }
    await almacen.guardar(await validar(respuesta, esquemaDatosSesion));
  }

  function renovar(): Promise<void> {
    renovando ??= refrescar().finally(() => {
      renovando = null;
    });
    return renovando;
  }

  /** Si borrar lo local falla, la sesión se cierra igual: la cuenta ya no existe. */
  async function avisarBorrado(): Promise<void> {
    const usuario = await almacen.leerUsuario().catch(() => null);
    if (usuario) await Promise.allSettled([...oyentesBorrado].map((oyente) => oyente(usuario)));
  }

  async function caducar(cuentaBorrada = false): Promise<void> {
    try {
      if (cuentaBorrada) await avisarBorrado();
      await almacen.borrar();
    } finally {
      oyentes.forEach((oyente) => oyente());
    }
  }

  async function renovarTras401(accesoUsado: string | null): Promise<void> {
    // Si otra petición ya renovó mientras esta volvía con 401, basta con reintentar con el token nuevo.
    if (almacen.leerAcceso() !== accesoUsado) {
      return;
    }
    try {
      await renovar();
    } catch (e) {
      // Sin red, el refresco sigue valiendo: no se cierra la sesión.
      if (e instanceof ErrorApi && (e.estado === NO_AUTORIZADO || e.estado === CUENTA_BORRADA)) {
        await caducar(e.estado === CUENTA_BORRADA);
      }
      throw e;
    }
  }

  async function pedir<T>(peticion: Peticion<T>): Promise<T> {
    const autenticada = peticion.autenticada ?? true;
    const acceso = autenticada ? almacen.leerAcceso() : null;
    let respuesta = await enviar(peticion, acceso);
    if (respuesta.status === NO_AUTORIZADO && autenticada) {
      await renovarTras401(acceso);
      respuesta = await enviar(peticion, almacen.leerAcceso());
    }
    if (!respuesta.ok) {
      throw await error(respuesta);
    }
    return peticion.esquema ? validar(respuesta, peticion.esquema) : (undefined as T);
  }

  return {
    pedir,
    alCaducar(oyente) {
      oyentes.add(oyente);
      return () => {
        oyentes.delete(oyente);
      };
    },
    alBorrarseLaCuenta(oyente) {
      oyentesBorrado.add(oyente);
      return () => {
        oyentesBorrado.delete(oyente);
      };
    },
  };
}
