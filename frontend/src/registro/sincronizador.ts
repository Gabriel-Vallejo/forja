import { ErrorApi } from '../api/errores';
import type { SesionEntrada } from '../api/esquemas';
import type { ApiForja } from '../api/forja';
import { type AlmacenRegistro, borrarDatosDeUsuario, type SerieLocal, type SesionLocal } from './almacen';

// Envío del outbox (ADR 0008 §4). Reenviar es seguro (PUT idempotente), así que ante la duda se reenvía.
// Nunca se registra nada de lo que se envía: las series llevan el dolor, un dato de salud.

/** Las confirmadas se purgan pasados estos días: el servidor ya es la fuente de verdad. */
export const DIAS_RETENCION_CONFIRMADAS = 7;
export const ESPERA_BASE_MS = 30 * 1000;
export const ESPERA_MAXIMA_MS = 15 * 60 * 1000;
const DIA_MS = 24 * 60 * 60 * 1000;

const NO_AUTORIZADO = 401;
const CUENTA_INEXISTENTE = 410;
/** 4xx que no son culpa de la sesión: se reintentan con espera. */
const REINTENTABLES = new Set([408, 425, 429]);

/**
 * Espera antes del intento `intentos`+1: exponencial desde `ESPERA_BASE_MS`, con tope `ESPERA_MAXIMA_MS` y jitter
 * entre la mitad y el total (`azar` en [0, 1]) para que varios móviles no reintenten a la vez.
 */
export function esperaReintento(intentos: number, azar: number): number {
  const techo = Math.min(ESPERA_MAXIMA_MS, ESPERA_BASE_MS * 2 ** Math.max(0, intentos - 1));
  return Math.round(techo / 2 + (techo / 2) * azar);
}

/** La sesión local tal como la espera `PUT /sesiones/{uuid}`: todas las series, en orden, sin campos vacíos. */
export function aSesionEntrada(sesion: SesionLocal, series: readonly SerieLocal[]): SesionEntrada {
  return {
    realizadaEn: sesion.realizadaEn,
    ...(sesion.diaPlan !== null && { diaPlan: sesion.diaPlan }),
    ...(sesion.planVersion !== null && { planVersion: sesion.planVersion }),
    series: series.map((s) => ({
      uuid: s.uuid,
      ejercicio: s.ejercicio,
      orden: s.orden,
      pesoKg: s.pesoKg,
      repeticiones: s.repeticiones,
      ...(s.rir !== null && { rir: s.rir }),
      ...(s.rpe !== null && { rpe: s.rpe }),
      ...(s.dolor !== null && { dolor: s.dolor }),
      calentamiento: s.calentamiento,
      ...(s.corrigeA !== null && { corrigeA: s.corrigeA }),
      anulada: s.anulada,
      realizadaEn: s.realizadaEn,
    })),
  };
}

export interface ResultadoSincronizacion {
  confirmadas: number;
  rechazadas: number;
  /** El servidor dijo que la cuenta ya no existe (410): se ha descartado su outbox. */
  cuentaEliminada: boolean;
  /** Cuándo toca el siguiente reintento de lo que quedó pendiente (ms), o null si no queda nada. */
  proximoIntento: number | null;
}

export interface OpcionesSincronizar {
  /** Ignora la espera del backoff (p. ej., la red acaba de volver). */
  forzar?: boolean;
}

export interface Sincronizador {
  sincronizar(opciones?: OpcionesSincronizar): Promise<ResultadoSincronizacion>;
}

export interface OpcionesSincronizador {
  almacen: AlmacenRegistro;
  api: Pick<ApiForja, 'guardarSesion' | 'usuarioActual'>;
  reloj?: () => number;
  azar?: () => number;
}

type Desenlace = 'confirmada' | 'rechazada' | 'reintentar' | 'parar' | 'cuenta_eliminada';

/**
 * Un solo sincronizador por app: al empezar cada vuelta, lo que esté en `enviando` es de una vuelta que no acabó
 * (la app se cerró a mitad) y vuelve a `pendiente`.
 */
export function crearSincronizador({ almacen, api, reloj = Date.now, azar = Math.random }: OpcionesSincronizador): Sincronizador {
  let enCurso: Promise<ResultadoSincronizacion> | null = null;
  let otraVuelta = false;
  let forzarOtra = false;

  async function enviar(sesion: SesionLocal): Promise<Desenlace> {
    const series = await almacen.listarSeries(sesion.uuid);
    await almacen.actualizarSesion(sesion.uuid, { estado: 'enviando' });
    try {
      await api.guardarSesion(sesion.uuid, aSesionEntrada(sesion, series));
    } catch (e) {
      return fallo(sesion, e);
    }
    // Si se añadió una serie mientras volaba el PUT, el servidor no la tiene: queda pendiente.
    if ((await almacen.listarSeries(sesion.uuid)).length !== series.length) {
      await almacen.actualizarSesion(sesion.uuid, { estado: 'pendiente', proximoIntento: null });
      return 'reintentar';
    }
    await almacen.actualizarSesion(sesion.uuid, { estado: 'confirmada', confirmadaEn: reloj(), proximoIntento: null, ultimoError: null });
    return 'confirmada';
  }

  async function fallo(sesion: SesionLocal, e: unknown): Promise<Desenlace> {
    const estado = e instanceof ErrorApi ? e.estado : 0;
    if (estado === NO_AUTORIZADO) {
      // El cliente ya intentó renovar el token y no pudo: se espera a que vuelva a entrar, sin gastar intentos.
      await almacen.actualizarSesion(sesion.uuid, { estado: 'pendiente' });
      return 'parar';
    }
    if (estado === CUENTA_INEXISTENTE) {
      await borrarDatosDeUsuario(almacen, sesion.usuario);
      return 'cuenta_eliminada';
    }
    const mensaje = e instanceof Error ? e.message : 'No se ha podido enviar.';
    if (estado >= 400 && estado < 500 && !REINTENTABLES.has(estado)) {
      await almacen.actualizarSesion(sesion.uuid, { estado: 'rechazada', ultimoError: mensaje, proximoIntento: null });
      return 'rechazada';
    }
    // Sin red, 5xx, 408/429 o una respuesta rara: reenviar es seguro.
    const intentos = sesion.intentos + 1;
    await almacen.actualizarSesion(sesion.uuid, {
      estado: 'pendiente',
      intentos,
      proximoIntento: reloj() + esperaReintento(intentos, azar()),
      ultimoError: mensaje,
    });
    return 'parar';
  }

  async function preparar(): Promise<void> {
    for (const s of await almacen.listarSesiones({ estados: ['enviando'] })) {
      await almacen.actualizarSesion(s.uuid, { estado: 'pendiente' });
    }
    const limite = reloj() - DIAS_RETENCION_CONFIRMADAS * DIA_MS;
    for (const s of await almacen.listarSesiones({ estados: ['confirmada'] })) {
      if (s.confirmadaEn !== null && s.confirmadaEn < limite) await almacen.borrarSesion(s.uuid);
    }
  }

  async function vuelta(forzar: boolean): Promise<ResultadoSincronizacion> {
    const resultado: ResultadoSincronizacion = { confirmadas: 0, rechazadas: 0, cuentaEliminada: false, proximoIntento: null };
    await preparar();
    const usuario = await api.usuarioActual();
    if (!usuario) return resultado;
    const pendientes = await almacen.listarSesiones({ usuario, estados: ['pendiente'] });
    for (const sesion of pendientes) {
      if (!forzar && sesion.proximoIntento !== null && sesion.proximoIntento > reloj()) continue;
      // Se vuelve a mirar antes de cada envío: si alguien cambió de cuenta, lo de la anterior no sale con su token.
      if ((await api.usuarioActual()) !== usuario) break;
      const desenlace = await enviar(sesion);
      if (desenlace === 'confirmada') resultado.confirmadas++;
      if (desenlace === 'rechazada') resultado.rechazadas++;
      if (desenlace === 'cuenta_eliminada') resultado.cuentaEliminada = true;
      if (desenlace === 'parar' || desenlace === 'cuenta_eliminada') break;
    }
    const restantes = await almacen.listarSesiones({ usuario, estados: ['pendiente'] });
    const proximos = restantes.map((s) => s.proximoIntento ?? reloj());
    resultado.proximoIntento = proximos.length ? Math.min(...proximos) : null;
    return resultado;
  }

  async function bucle(forzar: boolean): Promise<ResultadoSincronizacion> {
    let resultado = await vuelta(forzar);
    while (otraVuelta) {
      const f = forzarOtra;
      otraVuelta = false;
      forzarOtra = false;
      const siguiente = await vuelta(f);
      resultado = {
        confirmadas: resultado.confirmadas + siguiente.confirmadas,
        rechazadas: resultado.rechazadas + siguiente.rechazadas,
        cuentaEliminada: resultado.cuentaEliminada || siguiente.cuentaEliminada,
        proximoIntento: siguiente.proximoIntento,
      };
    }
    return resultado;
  }

  return {
    sincronizar({ forzar = false } = {}) {
      if (enCurso) {
        // Lo que se pide durante un envío se hace en otra vuelta al acabar, no en paralelo.
        otraVuelta = true;
        forzarOtra ||= forzar;
        return enCurso;
      }
      enCurso = bucle(forzar).finally(() => {
        enCurso = null;
      });
      return enCurso;
    },
  };
}
